package com.regnosys.rosetta.ir.expr.adapter;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.annotations.RAnnotationRef;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.CardMod;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.ROnlyExistsElement;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRExprKind;
import com.regnosys.rosetta.ir.expr.IRListConstruct;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRLibraryApply;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRMetaItemNav;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRRecordReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierItemNav;
import com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav;
import com.regnosys.rosetta.ir.expr.IRSwitchOp;
import com.regnosys.rosetta.ir.expr.IRDefaultOp;
import com.regnosys.rosetta.ir.expr.IRMembershipOp;
import com.regnosys.rosetta.ir.expr.IRCollectOp;
import com.regnosys.rosetta.ir.expr.IRWithMetaOp;
import com.regnosys.rosetta.ir.expr.IRJoinOp;
import com.regnosys.rosetta.ir.expr.IRMetaParamRef;
import com.regnosys.rosetta.ir.expr.IROutputAliasNav;
import com.regnosys.rosetta.ir.expr.IROutputRef;
import com.regnosys.rosetta.ir.expr.IRRuleInputNav;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
import com.regnosys.rosetta.ir.expr.IRConditionInstance;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural-faithfulness unit test for {@link ExpressionToIRAdapter} over the Wave-0
 * leaf primitives. Builds each leaf AST node directly and asserts the lowered
 * {@link IRExpr} carries the right kind, payload, structural identity and children — the
 * {@code ast → ir} half of Wave 0 (the byte-emission half is gated by
 * {@code Path2ByteIdentityTest} FUNCTION rows).
 *
 * <p>A minimal real {@link RWorkspace} (built from the rune-dsl builtins only) supplies
 * the {@code type}/{@code cardinality} lookups the adapter performs; we assert only the
 * workspace-independent structure here, so the (memoization-{@code MISSING}) type facts
 * for these hand-built nodes do not matter. Skips cleanly if the sibling rune-dsl
 * builtins are not on disk.
 */
class ExpressionToIRAdapterTest {

    /** Vendored rune-dsl builtins, sibling module (mirrors Path2ByteIdentityTest). */
    private static final Path BUILTINS_ROOT =
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model");

    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();

    private static RWorkspace cachedWorkspace;

    private static RWorkspace workspace() throws IOException {
        if (cachedWorkspace == null) {
            List<RModel> models = new ArrayList<>();
            try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
                for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                    models.add(AstBuilder.buildFromFile(p));
                }
            }
            cachedWorkspace = RWorkspace.build(models).workspace();
        }
        return cachedWorkspace;
    }

    @Test
    void scalarLiteralsLowerToIRLiteral() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();

        RIntLiteral intLit = new RIntLiteral();
        intLit.setValue(42);
        IRLiteral asInt = assertInstanceOf(IRLiteral.class, adapt(intLit, ws));
        assertEquals(IRExprKind.LITERAL, asInt.kind());
        assertEquals(IRLiteral.LiteralKind.INT, asInt.literalKind());
        assertEquals(BigInteger.valueOf(42), asInt.value());
        assertEquals(Optionality.PRESENT, asInt.optionality());
        assertTrue(asInt.children().isEmpty());
        assertEquals(List.of(), asInt.nodeId().path(), "a seam-root leaf has the empty NodeId path");

        RNumberLiteral numLit = new RNumberLiteral();
        numLit.setValue(new BigDecimal("1.5"));
        IRLiteral asNum = assertInstanceOf(IRLiteral.class, adapt(numLit, ws));
        assertEquals(IRLiteral.LiteralKind.NUMBER, asNum.literalKind());
        assertEquals(new BigDecimal("1.5"), asNum.value());

        RStringLiteral strLit = new RStringLiteral();
        strLit.setValue("hello");
        IRLiteral asStr = assertInstanceOf(IRLiteral.class, adapt(strLit, ws));
        assertEquals(IRLiteral.LiteralKind.STRING, asStr.literalKind());
        assertEquals("hello", asStr.value());

        RBooleanLiteral boolLit = new RBooleanLiteral();
        boolLit.setValue(true);
        IRLiteral asBool = assertInstanceOf(IRLiteral.class, adapt(boolLit, ws));
        assertEquals(IRLiteral.LiteralKind.BOOLEAN, asBool.literalKind());
        assertEquals(Boolean.TRUE, asBool.value());
    }

    @Test
    void emptyLiteralLowersToOptionalAbsentValue() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IREmptyLiteral empty = assertInstanceOf(IREmptyLiteral.class, adapt(new REmptyLiteral(), workspace()));
        assertEquals(IRExprKind.EMPTY_LITERAL, empty.kind());
        assertEquals(IREmptyLiteral.EmptySource.USER_EMPTY, empty.source());
        assertEquals(Optionality.OPTIONAL, empty.optionality());
        assertTrue(empty.children().isEmpty());
    }

    @Test
    void implicitVariableCarriesSyntheticFlag() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RImplicitVariable userItem = new RImplicitVariable();
        userItem.setSynthetic(false);
        IRVariable asUser = assertInstanceOf(IRVariable.class, adapt(userItem, ws));
        assertEquals(IRVariable.VariableKind.USER_ITEM, asUser.variableKind());
        assertEquals("item", asUser.name());

        RImplicitVariable syntheticItem = new RImplicitVariable();
        syntheticItem.setSynthetic(true);
        IRVariable asSynthetic = assertInstanceOf(IRVariable.class, adapt(syntheticItem, ws));
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, asSynthetic.variableKind());
    }

    @Test
    void superCallLowersToSuperReference() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRReference ref = assertInstanceOf(IRReference.class, adapt(new RSuperCall(), workspace()));
        assertEquals(IRExprKind.REFERENCE, ref.kind());
        assertEquals(IRReference.ReferenceKind.SUPER, ref.referenceKind());
        assertEquals("", ref.target());
    }

    @Test
    void listOfLeavesLowersWithChildNodeIds() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RListLiteral list = new RListLiteral();
        list.elements().add(intLiteral(1));
        list.elements().add(intLiteral(2));

        IRListConstruct asList = assertInstanceOf(IRListConstruct.class, adapt(list, ws));
        assertEquals(IRExprKind.LIST_CONSTRUCT, asList.kind());
        assertEquals(2, asList.elements().size());
        assertEquals(2, asList.children().size());
        // Child NodeIds are the structural child-index paths off the list root.
        assertEquals(List.of(0), asList.elements().get(0).nodeId().path());
        assertEquals(List.of(1), asList.elements().get(1).nodeId().path());
        assertEquals(IRLiteral.LiteralKind.INT,
                assertInstanceOf(IRLiteral.class, asList.elements().get(0)).literalKind());
    }

    @Test
    void unsupportedAndPartiallyUnsupportedNodesReturnEmpty() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A bare symbol reference is a Wave-1+ concern → deferred to legacy fallback.
        RSymbolReference symbol = new RSymbolReference();
        symbol.setName("someParam");
        assertTrue(adapter.adapt(symbol, ws).isEmpty(), "bare symbol reference is not Wave-0-expressible");

        // A list is expressible only if EVERY element is; one unsupported element ⇒ empty.
        RListLiteral mixed = new RListLiteral();
        mixed.elements().add(intLiteral(1));
        RSymbolReference unsupportedElement = new RSymbolReference();
        unsupportedElement.setName("x");
        mixed.elements().add(unsupportedElement);
        assertTrue(adapter.adapt(mixed, ws).isEmpty(),
                "a list with a non-Wave-0 element is not fully expressible");
    }

    @Test
    void scalarFunctionParameterLowersToIRVariablePARAM() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RAttribute param = scalarParam("foo");
        functionWith(ws, param);                 // wires parent + attaches to the workspace
        RSymbolReference ref = paramRef("foo", param, ws);

        IRVariable var = assertInstanceOf(IRVariable.class, adapt(ref, ws));
        assertEquals(IRExprKind.VARIABLE, var.kind());
        assertEquals(IRVariable.VariableKind.PARAM, var.variableKind());
        assertEquals("foo", var.name());
        assertEquals(Optionality.PRESENT, var.optionality(), "a (1..1) parameter is present");
        assertEquals(ExpressionCardinality.SINGLE, var.cardinality(), "a (1..1) parameter is single");
        assertTrue(var.children().isEmpty());
    }

    @Test
    void nonParameterOrCallSymbolReferencesReturnEmpty() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // An attribute with no enclosing function (e.g. a data-type field) is not a parameter.
        // #528 arm-5 RETENSE, re-tensed at the fix commit: the L-113 conversion mints the
        // shallow IRImplicitAttrNav for the bare-attr arm's census families (here
        // noFilterExtractBinder) — but only for an attribute the attribute channel can TYPE
        // (the mint's #514-pattern guard, the Copilot #528 R1 catch: the serialized channel
        // assumes a non-null type). This engine-state fixture carries no typeCall, so the
        // mint declines and the ORIGINAL #497-era invariant returns in its strongest form:
        // the reference neither takes the PARAM path nor mints an untypeable node
        // (prove-or-decline). The typed mint positives live in the parsed-model retenses.
        RAttribute orphan = scalarParam("orphan");
        assertTrue(adapter.adapt(paramRef("orphan", orphan, ws), ws).isEmpty(),
                "an untypeable non-parameter attribute reference declines — the mint's type"
                        + " guard (never a param variable, never a null-typed mint)");

        // A reference carrying arguments whose symbol is an ATTRIBUTE (not a resolved RFunction) is not a
        // function call — deferred (adaptApply claims only a call whose symbol resolves to a plain RFunction).
        RAttribute called = scalarParam("foo");
        functionWith(ws, called);
        RSymbolReference call = paramRef("foo", called, ws);
        call.args().add(intLiteral(1));
        assertTrue(adapter.adapt(call, ws).isEmpty(), "an args-present attribute reference is not a function call");
    }

    @Test
    void functionCallLowersToIRApply_multiOutputLowers_multiParamLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `myCallee(x)` — a plain RFunction (scalar param + scalar output) called with a scalar-param arg lowers to
        // IRApply{callee = IRReference{FUNCTION}, args = [IRVariable PARAM]}; the emitter renders
        // MapperS.of(myCallee.evaluate(x)). The callee takes child slot 0, the first arg child slot 1.
        RAttribute x = scalarParam("x");
        functionWith(ws, x);
        RFunction callee = calleeFunction(ws, "MyCallee", List.of(scalarParam("p")), scalarParam("out"));
        RSymbolReference call = callRef("MyCallee", callee, ws, paramRef("x", x, ws));
        IRApply apply = assertInstanceOf(IRApply.class, adapt(call, ws));
        assertEquals(IRExprKind.APPLY, apply.kind());
        IRReference calleeRef = assertInstanceOf(IRReference.class, apply.callee(), "the callee is an IRReference");
        assertEquals(IRReference.ReferenceKind.FUNCTION, calleeRef.referenceKind());
        assertEquals("MyCallee", calleeRef.target(), "the callee carries the function's SIMPLE name");
        assertEquals(List.of(0), calleeRef.nodeId().path(), "the callee takes child slot 0");
        assertEquals(1, apply.args().size());
        IRVariable arg = assertInstanceOf(IRVariable.class, apply.args().get(0), "the scalar-param arg lowers");
        assertEquals(IRVariable.VariableKind.PARAM, arg.variableKind());
        assertEquals(List.of(1), arg.nodeId().path(), "the first arg takes child slot 1");

        // A LITERAL arg also lowers (`myCallee2(42)`).
        RFunction calleeLit = calleeFunction(ws, "MyCallee2", List.of(scalarParam("p")), scalarParam("out"));
        IRApply applyLit = assertInstanceOf(IRApply.class,
                adapt(callRef("MyCallee2", calleeLit, ws, intLiteral(42)), ws));
        assertInstanceOf(IRLiteral.class, applyLit.args().get(0), "the literal arg lowers");

        // A two-scalar-arg call lowers (`myCallee3(x, 7)`) — all args scalar/literal, the emitter comma-joins them.
        RFunction callee3 = calleeFunction(ws, "MyCallee3",
                List.of(scalarParam("p1"), scalarParam("p2")), scalarParam("out"));
        IRApply apply3 = assertInstanceOf(IRApply.class,
                adapt(callRef("MyCallee3", callee3, ws, paramRef("x", x, ws), intLiteral(7)), ws));
        assertEquals(2, apply3.args().size(), "both scalar args lower");

        // A MULTI-output callee LOWERS (the emitter renders MapperC.<Item>of(recv.evaluate(args)); the IRApply
        // carries MULTI cardinality, the wrap + <Item> witness are emitter decisions).
        RFunction multiOut = calleeFunction(ws, "MultiOut", List.of(scalarParam("p")), multiParam("out"));
        IRApply multiApply = assertInstanceOf(IRApply.class,
                adapt(callRef("MultiOut", multiOut, ws, paramRef("x", x, ws)), ws),
                "a multi-output call now lowers");
        assertEquals(ExpressionCardinality.MULTI, multiApply.cardinality(),
                "the IRApply carries MULTI cardinality so the emitter wraps MapperC.<Item>of");

        // A MULTI-param callee now LOWERS (the multi-PARAM lever): the adapter no longer declines on a multi param;
        // the arg unwraps with .getMulti() instead of .get(), an emitter accessor decision (CallParamMultiResolver)
        // kept off the neutral node.
        RFunction multiParamCallee = calleeFunction(ws, "MultiParam", List.of(multiParam("p")), scalarParam("out"));
        IRApply multiParamApply = assertInstanceOf(IRApply.class,
                adapt(callRef("MultiParam", multiParamCallee, ws, paramRef("x", x, ws)), ws),
                "a multi-param call now lowers");
        assertEquals(1, multiParamApply.args().size(), "the arg lowers; the .getMulti() unwrap is an emitter decision");
    }

    @Test
    void functionCallWithAliasArgLowers_navArgStillDefers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // Slice 2 (alias args — the biggest call-arg lever, firing-ceiling ~2557). `AliasCallee(myAlias)` where
        // myAlias is an enclosing-function shortcut → IRApply whose arg[0] is an IRReference{ALIAS}; the emitter
        // renders aliasName(inputs).get() (the .get() fall-through over the alias Mapper).
        RShortcut shortcut = new RShortcut();
        shortcut.setName("myAlias");
        RFunction enclosing = new RFunction();
        enclosing.setName("MyFunc");
        enclosing.shortcuts().add(shortcut);
        shortcut.setParent(enclosing);
        enclosing.attachToWorkspace(ws);
        shortcut.attachToWorkspace(ws);

        RFunction callee = calleeFunction(ws, "AliasCallee", List.of(scalarParam("p")), scalarParam("out"));
        IRApply apply = assertInstanceOf(IRApply.class, adapt(callRef("AliasCallee", callee, ws, aliasRef(shortcut, ws)), ws));
        assertEquals(1, apply.args().size());
        IRReference aliasIr = assertInstanceOf(IRReference.class, apply.args().get(0),
                "the alias arg lowers to IRReference{ALIAS}");
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasIr.referenceKind());
        assertEquals("myAlias", aliasIr.target());
        assertEquals(List.of(1), aliasIr.nodeId().path(), "the alias arg takes child slot 1 (the callee is slot 0)");

        // A mixed alias + literal call lowers (both args admissible).
        RFunction callee2 = calleeFunction(ws, "AliasCallee2",
                List.of(scalarParam("p1"), scalarParam("p2")), scalarParam("out"));
        IRApply apply2 = assertInstanceOf(IRApply.class,
                adapt(callRef("AliasCallee2", callee2, ws, aliasRef(shortcut, ws), intLiteral(7)), ws));
        assertEquals(2, apply2.args().size(), "both the alias and the literal arg lower");
        assertInstanceOf(IRReference.class, apply2.args().get(0), "arg[0] is the alias");
        assertInstanceOf(IRLiteral.class, apply2.args().get(1), "arg[1] is the literal");

        // RETENSED #527 (the missing-hop admission): a HAND-BUILT navigation arg — its hop
        // never seen by the type fixed point, reading the NON-NULL MISSING sentinel — now
        // ADMITS on the PARAM root (the typeMissing.param face retired to the compiler's
        // argNavMissingHop whole-legacy serve; the reason-mirror pin asserts the composed
        // trigger shape, and the #491 belt pin argNavRootTypeBeltDeclinesMetaAndMissingRoots
        // keeps the META decline live).
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        IRApply missingNavApply = assertInstanceOf(IRApply.class,
                adapt(callRef("AliasCallee", callee, ws,
                        featureCall("foo", foo, "bar", scalarParam("bar"), ws)), ws),
                "a hand-built (MISSING-typed) navigation arg now claims — the #527 admission");
        assertInstanceOf(FieldAccess.class, missingNavApply.args().get(0),
                "the admitted arg is the FieldAccess chain (the serve leg's trigger shape)");
    }

    @Test
    void functionCallWithMultiParamArgLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // The multi-param-ARG sub-slice (L-048): a bare MULTI parameter passed AS a call arg now lowers. `MultiArg(xs)`
        // where xs is a multi enclosing-function input and the callee param is multi → IRApply whose arg[0] is an
        // IRVariable{PARAM, MULTI}; the emitter renders MapperC.<Item>of(xs) and strips it structurally to the bare
        // evaluate slot (byte-identical to legacy's bare multi-param arg, asMulti inert at the strip).
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        RFunction callee = calleeFunction(ws, "MultiArg", List.of(multiParam("p")), scalarParam("out"));
        IRApply apply = assertInstanceOf(IRApply.class,
                adapt(callRef("MultiArg", callee, ws, paramRef("xs", xs, ws)), ws), "a multi-param arg now lowers");
        assertEquals(1, apply.args().size());
        IRVariable argIr = assertInstanceOf(IRVariable.class, apply.args().get(0),
                "the multi-param arg lowers to an IRVariable");
        assertEquals(IRVariable.VariableKind.PARAM, argIr.variableKind());
        assertEquals(ExpressionCardinality.MULTI, argIr.cardinality(), "the arg carries MULTI cardinality");
    }

    @Test
    void multiCardinalityParameterLowersToMultiVariable() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A multi ((0..*)/(1..*)) function-parameter reference now lowers (the emitter renders it
        // MapperC.<Item>of(name)); the IRVariable carries MULTI cardinality (from the DECLARED
        // cardinality, matching legacy gm.isMulti), keeping it out of the scalar-only comparison/
        // equality/existence gates.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        IRVariable var = assertInstanceOf(IRVariable.class, adapt(paramRef("xs", xs, ws), ws));
        assertEquals(IRVariable.VariableKind.PARAM, var.variableKind());
        assertEquals(ExpressionCardinality.MULTI, var.cardinality(), "a multi param lowers with MULTI cardinality");
        assertEquals("xs", var.name());
    }

    @Test
    void bareEnumValueReferenceLowersToIRReferenceENUMVALUE() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);

        RSymbolReference ref = new RSymbolReference();
        ref.setName("Cash");
        ref.setResolvedSymbol(ev);
        ref.attachToWorkspace(ws);

        IRReference asRef = assertInstanceOf(IRReference.class, adapt(ref, ws));
        assertEquals(IRExprKind.REFERENCE, asRef.kind());
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, asRef.referenceKind());
        assertEquals("Cash", asRef.target(),
                "the adapter carries the RAW value name; convertValue is the emitter's Java concern");
        assertEquals(Optionality.PRESENT, asRef.optionality());
        assertTrue(asRef.children().isEmpty());
    }

    @Test
    void qualifiedEnumValueRefLowersToIRReferenceENUMVALUE_disguisesDefer() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A genuine qualified enum-value reference `FooEnum -> Cash` (enumeration + value both resolved,
        // no disguise channel) lowers to IRReference{ENUM_VALUE} carrying the RAW value name (the emitter
        // does convertValue + the import); same node as the bare arm.
        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);

        REnumValueRef genuine = enumValueRef("FooEnum", "Cash", en, ev, ws);
        IRReference asRef = assertInstanceOf(IRReference.class, adapt(genuine, ws));
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, asRef.referenceKind());
        assertEquals("Cash", asRef.target(), "the adapter carries the RAW value name");
        assertTrue(asRef.children().isEmpty());

        // DISGUISE 1 — enumeration ABSENT (a bare/unresolved or disguised-nav REnumValueRef): defer to legacy.
        REnumValueRef noEnum = new REnumValueRef();
        noEnum.setEnumName("foo");
        noEnum.setValueName("bar");
        noEnum.attachToWorkspace(ws);
        assertTrue(adapter.adapt(noEnum, ws).isEmpty(),
                "an REnumValueRef with no resolved enumeration defers (a disguised navigation, not a constant)");

        // DISGUISE 2 — the PRECEDENCE guard: enumeration present BUT a higher-precedence resolved channel
        // (resolvedAttributeChain) is also set. ExpressionTypeComputer.computeEnumValueRef checks the chain
        // BEFORE enumeration(), so the node types to a NON-enum — it must NOT lower to ENUM_VALUE.
        REnumValueRef disguised = enumValueRef("FooEnum", "Cash", en, ev, ws);
        disguised.setResolvedAttributeChain(
                new REnumValueRef.AttributeChain(scalarParam("a"), scalarParam("b")));
        assertTrue(adapter.adapt(disguised, ws).isEmpty(),
                "a node with a higher-precedence resolved channel defers, mirroring computeEnumValueRef's precedence");
    }

    @Test
    void disguisedInputNavArmDefersUnresolvableHeadsWithComposedReasons() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // #478 — the disguised-input-nav arm's DEFER gates + the composed mirror tokens. The arm's
        // LOWERS side is population-locked, not unit-locked: the builtins workspace carries no data
        // types, so a bare fixture cannot resolve a declared-type feature — the #478 nav-gate
        // witness's `inputFeatureNav:headInput:lowers` bucket must read ZERO on the post-teach
        // probed re-read (every equivalent-lowerable blocker is arm-lowered before the probe can
        // see it), and the flag-on byte ring proves the renders.
        RShortcut shortcut = new RShortcut();
        shortcut.setName("s");
        RFunction fn = new RFunction();
        fn.setName("NavFn");
        fn.shortcuts().add(shortcut);
        shortcut.setParent(fn);
        RAttribute tradeInput = new RAttribute();
        tradeInput.setName("trade");
        fn.inputs().add(tradeInput);
        tradeInput.setParent(fn);
        fn.attachToWorkspace(ws);
        tradeInput.attachToWorkspace(ws);

        // A SHORTCUT head: resolveNameInFunction nulls it (legacy's own contract), so the
        // equivalent carries an unresolved receiver and no wired feature — the arm defers and the
        // alias-receiver nav stays on legacy's own machinery (the L-032 class).
        REnumValueRef aliasHead = new REnumValueRef();
        aliasHead.setEnumName("s");
        aliasHead.setValueName("x");
        aliasHead.setResolvedInputFeature(scalarParam("x"));
        aliasHead.setParent(fn);
        aliasHead.attachToWorkspace(ws);
        assertTrue(adapter.adapt(aliasHead, ws).isEmpty(),
                "an alias-head disguise must defer (the receiver stays on legacy's alias machinery)");
        assertEquals(Optional.of("inputFeatureNav.featureUnresolved.aliasHead.bodyMissing"),
                adapter.declineReason(aliasHead, ws),
                "the mirror names the equivalent's own first failing gate, dot-suffixed; #492 the"
                        + " head decode reads the shortcut collision and the bare fixture's"
                        + " body-less shortcut lands the bodyMissing face");

        // An INPUT head whose declared type resolves no feature (no typeCall in the bare fixture):
        // the receiver resolves (the name-matched input) but the equivalent's feature gate declines
        // — the same composed family reached through the headInput class.
        REnumValueRef inputHead = new REnumValueRef();
        inputHead.setEnumName("trade");
        inputHead.setValueName("leg");
        inputHead.setResolvedInputFeature(scalarParam("leg"));
        inputHead.setParent(fn);
        inputHead.attachToWorkspace(ws);
        assertTrue(adapter.adapt(inputHead, ws).isEmpty(),
                "an input head with no resolvable declared-type feature must defer");
        assertEquals(Optional.of("inputFeatureNav.featureUnresolved.headAttr.declMiss"),
                adapter.declineReason(inputHead, ws));

        // A partially-built node (null valueName, resolvedInputFeature present): the arm's entry
        // gate defers and the mirror restates it as a NAMED token instead of NPE-ing through
        // resolveNameInFunction's equals (the Seat-1 OBS-1 / Copilot R2 convergence class).
        REnumValueRef nameless = new REnumValueRef();
        nameless.setEnumName("trade");
        nameless.setResolvedInputFeature(scalarParam("x"));
        nameless.setParent(fn);
        nameless.attachToWorkspace(ws);
        assertTrue(adapter.adapt(nameless, ws).isEmpty(),
                "a null-valueName disguise must defer at the arm's entry gate");
        assertEquals(Optional.of("inputFeatureNav.nameMissing"),
                adapter.declineReason(nameless, ws),
                "the mirror names the entry gate — a partially-built node never NPEs declineReason");
    }

    @Test
    void functionCallWithEnumValueArgLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // Slice (a) — a qualified enum-value argument `EnumCallee(FooEnum -> Cash)` lowers the whole IRApply,
        // arg[0] = IRReference{ENUM_VALUE}; the emitter renders MapperS.of(enumCallee.evaluate(FooEnum.CASH))
        // (the dotted-enum constant passes through unwrapForEvaluateArg unchanged — no MapperS wrap, no .get()).
        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);

        RFunction callee = calleeFunction(ws, "EnumCallee", List.of(scalarParam("p")), scalarParam("out"));
        IRApply apply = assertInstanceOf(IRApply.class,
                adapt(callRef("EnumCallee", callee, ws, enumValueRef("FooEnum", "Cash", en, ev, ws)), ws));
        assertEquals(1, apply.args().size());
        IRReference enumIr = assertInstanceOf(IRReference.class, apply.args().get(0),
                "the enum-value arg lowers to IRReference{ENUM_VALUE}");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, enumIr.referenceKind());
        assertEquals("Cash", enumIr.target());
        assertEquals(List.of(1), enumIr.nodeId().path(), "the enum arg takes child slot 1 (the callee is slot 0)");
    }

    /** Builds a genuine (enumeration + value resolved) qualified enum-value reference attached to {@code ws}. */
    private static REnumValueRef enumValueRef(String enumName, String valueName,
                                              REnumeration en, REnumValue ev, RWorkspace ws) {
        REnumValueRef enr = new REnumValueRef();
        enr.setEnumName(enumName);
        enr.setValueName(valueName);
        enr.setResolvedEnum(en);
        enr.setResolvedValue(ev);
        enr.attachToWorkspace(ws);
        return enr;
    }

    @Test
    void bareRuleReferenceLowersToIRApplyRULE() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A bare no-arg reference to an RRule (the `output = SomeRule` delegating shape) lowers to an
        // argument-less IRApply whose callee is IRReference{RULE} carrying the rule's simple name. The
        // context gate (top-level + from-type + not-in-lambda) and the <Name>Rule receiver are
        // compiler/emitter decisions (the L-029 split), so the adapter claim is purely structural.
        RRule rule = new RRule();
        rule.setName("AssetClass");
        rule.attachToWorkspace(ws);
        RSymbolReference ref = new RSymbolReference();
        ref.setName("AssetClass");
        ref.setResolvedSymbol(rule);
        ref.attachToWorkspace(ws);

        IRApply apply = assertInstanceOf(IRApply.class, adapt(ref, ws));
        assertEquals(IRExprKind.APPLY, apply.kind());
        assertTrue(apply.args().isEmpty(), "a bare rule delegation carries no explicit args");
        IRReference callee = assertInstanceOf(IRReference.class, apply.callee(), "the callee is an IRReference");
        assertEquals(IRReference.ReferenceKind.RULE, callee.referenceKind());
        assertEquals("AssetClass", callee.target(), "the callee carries the rule's SIMPLE name");
        assertEquals(List.of(0), callee.nodeId().path(), "the rule callee takes child slot 0");
    }

    @Test
    void aliasReferenceLowersToIRReferenceALIAS() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A reference whose resolved symbol is an enclosing-function RShortcut (legacy isAliasReference
        // arm 1) lowers to a neutral IRReference{ALIAS} carrying the alias name; the `aliasName(inputs)`
        // Java render (input threading + dependency-collision disambiguation) is the emitter's concern.
        RShortcut shortcut = new RShortcut();
        shortcut.setName("myAlias");
        RFunction func = new RFunction();
        func.setName("MyFunc");
        func.shortcuts().add(shortcut);
        shortcut.setParent(func);
        func.attachToWorkspace(ws);
        shortcut.attachToWorkspace(ws);
        RSymbolReference ref = new RSymbolReference();
        ref.setName("myAlias");
        ref.setResolvedSymbol(shortcut);
        ref.attachToWorkspace(ws);

        IRReference asRef = assertInstanceOf(IRReference.class, adapt(ref, ws));
        assertEquals(IRExprKind.REFERENCE, asRef.kind());
        assertEquals(IRReference.ReferenceKind.ALIAS, asRef.referenceKind());
        assertEquals("myAlias", asRef.target(), "the adapter carries the neutral alias name");
        assertTrue(asRef.children().isEmpty());

        // An alias reference carrying arguments is malformed (legacy throws) — declined by the args gate.
        RSymbolReference aliasWithArgs = new RSymbolReference();
        aliasWithArgs.setName("myAlias");
        aliasWithArgs.setResolvedSymbol(shortcut);
        aliasWithArgs.args().add(intLiteral(1));
        aliasWithArgs.attachToWorkspace(ws);
        assertTrue(adapter.adapt(aliasWithArgs, ws).isEmpty(), "an alias reference with args defers");
    }

    @Test
    void aliasArithmeticOperandLowersToBinaryOp() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `daysInPeriod / 365`: an alias/shortcut operand now composes in arithmetic (alongside the numeric
        // literal) — the day-count shape. The adapter stays neutral (claims an alias + literal pair); the
        // numeric classification (recursing the shortcut body) and the aliasName(inputs) render are the
        // compiler/emitter's.
        RShortcut shortcut = new RShortcut();
        shortcut.setName("daysInPeriod");
        RFunction func = new RFunction();
        func.setName("YearFraction");
        func.shortcuts().add(shortcut);
        shortcut.setParent(func);
        func.attachToWorkspace(ws);
        shortcut.attachToWorkspace(ws);

        RSymbolReference aliasRef = new RSymbolReference();
        aliasRef.setName("daysInPeriod");
        aliasRef.setResolvedSymbol(shortcut);
        aliasRef.attachToWorkspace(ws);

        RArithmeticExpr aliasOverLiteral = new RArithmeticExpr();
        aliasOverLiteral.setOp(ArithOp.DIVIDE);
        aliasOverLiteral.setLeft(aliasRef);
        aliasOverLiteral.setRight(intLiteral(365));
        aliasOverLiteral.attachToWorkspace(ws);

        BinaryOp div = assertInstanceOf(BinaryOp.class, adapt(aliasOverLiteral, ws));
        assertEquals(BinaryOp.BinOp.DIV, div.op());
        IRReference alias = assertInstanceOf(IRReference.class, div.left(), "the alias operand composes");
        assertEquals(IRReference.ReferenceKind.ALIAS, alias.referenceKind());
        assertEquals("daysInPeriod", alias.target());
        assertInstanceOf(IRLiteral.class, div.right(), "the numeric literal operand composes");
    }

    @Test
    void nestedArithmeticOperandLowersToBinaryOp() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // The COMPOUND day-count shape (ACT_ACT_ISDA `set result:`):
        //   (daysInNonLeapPeriod / 365) + (daysInLeapYearPeriod / 366)
        // — an outer ADD whose BOTH operands are nested arithmetic (alias / literal divides). The adapter recurses
        // all-or-nothing: each inner DIVIDE lowers (alias + numeric-literal operands, L-035), so the outer ADD now
        // admits the two nested BinaryOps as operands. The adapter stays neutral; the nested result witnesses, the
        // outer JOIN, and the re-render are the emitter's.
        RFunction func = new RFunction();
        func.setName("YearFraction");
        RShortcut nonLeap = new RShortcut();
        nonLeap.setName("daysInNonLeapPeriod");
        RShortcut leap = new RShortcut();
        leap.setName("daysInLeapYearPeriod");
        func.shortcuts().add(nonLeap);
        func.shortcuts().add(leap);
        nonLeap.setParent(func);
        leap.setParent(func);
        func.attachToWorkspace(ws);
        nonLeap.attachToWorkspace(ws);
        leap.attachToWorkspace(ws);

        BinaryOp add = assertInstanceOf(BinaryOp.class,
                adapt(arithmetic(ArithOp.PLUS,
                        aliasOverLiteral(ArithOp.DIVIDE, nonLeap, 365, ws),
                        aliasOverLiteral(ArithOp.DIVIDE, leap, 366, ws), ws), ws));
        assertEquals(BinaryOp.BinOp.ADD, add.op());

        BinaryOp leftDiv = assertInstanceOf(BinaryOp.class, add.left(), "the LHS nested divide composes");
        assertEquals(BinaryOp.BinOp.DIV, leftDiv.op());
        assertEquals("daysInNonLeapPeriod",
                assertInstanceOf(IRReference.class, leftDiv.left()).target());
        assertInstanceOf(IRLiteral.class, leftDiv.right());

        BinaryOp rightDiv = assertInstanceOf(BinaryOp.class, add.right(), "the RHS nested divide composes");
        assertEquals(BinaryOp.BinOp.DIV, rightDiv.op());
        assertEquals("daysInLeapYearPeriod",
                assertInstanceOf(IRReference.class, rightDiv.left()).target());
        assertInstanceOf(IRLiteral.class, rightDiv.right());

        // Structural NodeIds: the outer operands carry child(0)/child(1); the nested divides' own operands recurse
        // one level deeper (child(0).child(0) etc.), proving the all-or-nothing recursion threads the id path.
        assertEquals(List.of(0), add.left().nodeId().path());
        assertEquals(List.of(1), add.right().nodeId().path());
        assertEquals(List.of(0, 0), leftDiv.left().nodeId().path());
    }

    @Test
    void parameterComparisonLowersToBinaryOp() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        functionWith(ws, n1, n2);
        RComparisonExpr cmp = new RComparisonExpr();
        cmp.setOp(CompOp.GTE);
        cmp.setLeft(paramRef("n1", n1, ws));
        cmp.setRight(paramRef("n2", n2, ws));
        cmp.attachToWorkspace(ws);

        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(cmp, ws));
        assertEquals(IRExprKind.BINARY_OP, bin.kind());
        assertEquals(BinaryOp.BinOp.GTE, bin.op());
        assertEquals(2, bin.children().size());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, bin.left()).variableKind());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, bin.right()).variableKind());
        // operands carry the structural child-index NodeIds (left=child(0), right=child(1)).
        assertEquals(List.of(0), bin.left().nodeId().path());
        assertEquals(List.of(1), bin.right().nodeId().path());
    }

    @Test
    void comparisonWithNumericLiteralLowers_twoLiteralsAndModifierDefer() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `n < 0` — a numeric literal operand now LOWERS when its SIBLING is a scalar operand (the emitter
        // threads the sibling's resolved numeric type into the literal's render; the JOIN/coercion is the
        // emitter's, so the adapter does not look at the operand types here).
        RAttribute n = scalarParam("n");
        functionWith(ws, n);
        RComparisonExpr withLiteral = new RComparisonExpr();
        withLiteral.setOp(CompOp.LT);
        withLiteral.setLeft(paramRef("n", n, ws));
        withLiteral.setRight(intLiteral(0));
        withLiteral.attachToWorkspace(ws);
        BinaryOp ltBin = assertInstanceOf(BinaryOp.class, adapt(withLiteral, ws));
        assertEquals(BinaryOp.BinOp.LT, ltBin.op());
        assertInstanceOf(IRVariable.class, ltBin.left());
        assertInstanceOf(IRLiteral.class, ltBin.right(), "the numeric literal operand composes");

        // Two numeric literals DEFER (`1 < 2` — no resolved sibling to type the literal).
        RComparisonExpr litLit = new RComparisonExpr();
        litLit.setOp(CompOp.LT);
        litLit.setLeft(intLiteral(1));
        litLit.setRight(intLiteral(2));
        litLit.attachToWorkspace(ws);
        assertTrue(adapter.adapt(litLit, ws).isEmpty(), "two numeric literals defer (no resolved sibling)");

        // An explicit all/any cardinality modifier defers (not modelled yet).
        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        RComparisonExpr withMod = new RComparisonExpr();
        withMod.setOp(CompOp.GT);
        withMod.setMod(CardMod.ALL);
        withMod.setLeft(paramRef("a", a, ws));
        withMod.setRight(paramRef("b", b, ws));
        withMod.attachToWorkspace(ws);
        // RECUT at #503 (the pin-recut law): the modified comparison now LOWERS to the
        // DISTINCT shallow IRAllAnyCompare (the arm-A1 claim — legacy renders the whole
        // modified comparison through its own operator family, oracle-served at the root).
        IRAllAnyCompare modLowered = assertInstanceOf(IRAllAnyCompare.class,
                adapter.adapt(withMod, ws).orElseThrow(() -> new AssertionError(
                        "an explicit all/any modifier must lower since the #503 arm")));
        assertEquals("GT", modLowered.op(), "the operator fact");
        assertEquals("ALL", modLowered.modifier(), "the modifier fact");
    }

    @Test
    void parameterAndLiteralArithmeticLowersToBinaryOpUnaryDefers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `a + b` over two scalar params lowers to BinaryOp{ADD}; each operator maps to its BinOp. The adapter
        // gate is param/literal (neutral) — the numeric witness classification + join/coercion decision are the
        // emitter's, so the adapter does not look at the operand types here.
        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        BinaryOp add = assertInstanceOf(BinaryOp.class, adapt(arithmetic(ArithOp.PLUS, "a", a, "b", b, ws), ws));
        assertEquals(IRExprKind.BINARY_OP, add.kind());
        assertEquals(BinaryOp.BinOp.ADD, add.op());
        assertEquals(2, add.children().size());
        assertEquals(IRVariable.VariableKind.PARAM, assertInstanceOf(IRVariable.class, add.left()).variableKind());
        assertEquals(IRVariable.VariableKind.PARAM, assertInstanceOf(IRVariable.class, add.right()).variableKind());
        assertEquals(BinaryOp.BinOp.SUB,
                assertInstanceOf(BinaryOp.class, adapt(arithmetic(ArithOp.MINUS, "a", a, "b", b, ws), ws)).op());
        assertEquals(BinaryOp.BinOp.MUL,
                assertInstanceOf(BinaryOp.class, adapt(arithmetic(ArithOp.MULTIPLY, "a", a, "b", b, ws), ws)).op());
        assertEquals(BinaryOp.BinOp.DIV,
                assertInstanceOf(BinaryOp.class, adapt(arithmetic(ArithOp.DIVIDE, "a", a, "b", b, ws), ws)).op());

        // A numeric LITERAL operand now lowers (`a * 2`): the adapter claims a param/literal pair (the
        // numeric-literal operand re-renders against the join expected type in the emitter, byte-safely).
        RArithmeticExpr paramTimesLiteral = new RArithmeticExpr();
        paramTimesLiteral.setOp(ArithOp.MULTIPLY);
        paramTimesLiteral.setLeft(paramRef("a", a, ws));
        paramTimesLiteral.setRight(intLiteral(2));
        paramTimesLiteral.attachToWorkspace(ws);
        BinaryOp mul = assertInstanceOf(BinaryOp.class, adapt(paramTimesLiteral, ws));
        assertEquals(BinaryOp.BinOp.MUL, mul.op());
        assertInstanceOf(IRVariable.class, mul.left());
        assertInstanceOf(IRLiteral.class, mul.right(), "the numeric literal operand composes");

        // `2 / b`: a literal LHS lowers too.
        RArithmeticExpr literalOverParam = new RArithmeticExpr();
        literalOverParam.setOp(ArithOp.DIVIDE);
        literalOverParam.setLeft(intLiteral(2));
        literalOverParam.setRight(paramRef("b", b, ws));
        literalOverParam.attachToWorkspace(ws);
        BinaryOp div = assertInstanceOf(BinaryOp.class, adapt(literalOverParam, ws));
        assertEquals(BinaryOp.BinOp.DIV, div.op());
        assertInstanceOf(IRLiteral.class, div.left(), "the numeric literal LHS composes");
        assertInstanceOf(IRVariable.class, div.right());

        // The unary prefix form (rawLeft == null — negate / pass-through) still defers.
        RArithmeticExpr unary = new RArithmeticExpr();
        unary.setOp(ArithOp.MINUS);
        unary.setRight(paramRef("a", a, ws));
        unary.attachToWorkspace(ws);
        assertTrue(adapter.adapt(unary, ws).isEmpty(), "a unary +/- arithmetic expression defers");
    }

    @Test
    void parameterEqualityLowersToBinaryOp() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `=` over two scalar parameters lowers to BinaryOp{EQ}; the emitter maps EQ → areEqual.
        RAttribute s1 = scalarParam("s1");
        RAttribute s2 = scalarParam("s2");
        functionWith(ws, s1, s2);
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(paramRef("s1", s1, ws));
        eq.setRight(paramRef("s2", s2, ws));
        eq.attachToWorkspace(ws);

        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(eq, ws));
        assertEquals(IRExprKind.BINARY_OP, bin.kind());
        assertEquals(BinaryOp.BinOp.EQ, bin.op());
        assertEquals(2, bin.children().size());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, bin.left()).variableKind());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, bin.right()).variableKind());
        assertEquals(List.of(0), bin.left().nodeId().path());
        assertEquals(List.of(1), bin.right().nodeId().path());

        // `<>` lowers to BinaryOp{NEQ}; the emitter maps NEQ → notEqual + the Any cardinality default.
        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        REqualityExpr neq = new REqualityExpr();
        neq.setOp(EqOp.NEQ);
        neq.setLeft(paramRef("a", a, ws));
        neq.setRight(paramRef("b", b, ws));
        neq.attachToWorkspace(ws);
        assertEquals(BinaryOp.BinOp.NEQ, assertInstanceOf(BinaryOp.class, adapt(neq, ws)).op());
    }

    @Test
    void equalityWithLiteralOrEnumOperandLowers_twoLiteralsAndModifierDefer() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `flag = True` — a BOOLEAN literal operand now LOWERS (the literal already renders the wrapped
        // MapperS.of(true); no numeric coercion is threaded, unlike a number literal). The RHS is the
        // boolean IRLiteral.
        RAttribute flag = scalarParam("flag");
        functionWith(ws, flag);
        REqualityExpr boolEq = new REqualityExpr();
        boolEq.setOp(EqOp.EQ);
        boolEq.setLeft(paramRef("flag", flag, ws));
        boolEq.setRight(boolLiteral(true));
        boolEq.attachToWorkspace(ws);
        BinaryOp boolBin = assertInstanceOf(BinaryOp.class, adapt(boolEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, boolBin.op());
        assertInstanceOf(IRVariable.class, boolBin.left());
        IRLiteral boolRhs = assertInstanceOf(IRLiteral.class, boolBin.right(), "the boolean literal operand composes");
        assertEquals(IRLiteral.LiteralKind.BOOLEAN, boolRhs.literalKind());

        // `code = "FOO"` — a STRING literal operand also LOWERS (the literal renders MapperS.of("FOO") with no
        // coercion, like the boolean; only a NUMERIC literal threads an expected-type coercion and still defers).
        RAttribute code = scalarParam("code");
        functionWith(ws, code);
        RStringLiteral strLit = new RStringLiteral();
        strLit.setValue("FOO");
        REqualityExpr strEq = new REqualityExpr();
        strEq.setOp(EqOp.EQ);
        strEq.setLeft(paramRef("code", code, ws));
        strEq.setRight(strLit);
        strEq.attachToWorkspace(ws);
        BinaryOp strBin = assertInstanceOf(BinaryOp.class, adapt(strEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, strBin.op());
        IRLiteral strRhs = assertInstanceOf(IRLiteral.class, strBin.right(), "the string literal operand composes");
        assertEquals(IRLiteral.LiteralKind.STRING, strRhs.literalKind());

        // `status = FooEnum.Cash` — a bare ENUM-value operand now LOWERS (the emitter wraps the dotted
        // constant MapperS.of(FooEnum.Cash)). The RHS is the IRReference{ENUM_VALUE}.
        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);
        RSymbolReference enumRef = new RSymbolReference();
        enumRef.setName("Cash");
        enumRef.setResolvedSymbol(ev);
        enumRef.attachToWorkspace(ws);
        RAttribute status = scalarParam("status");
        functionWith(ws, status);
        REqualityExpr enumEq = new REqualityExpr();
        enumEq.setOp(EqOp.EQ);
        enumEq.setLeft(paramRef("status", status, ws));
        enumEq.setRight(enumRef);
        enumEq.attachToWorkspace(ws);
        BinaryOp enumBin = assertInstanceOf(BinaryOp.class, adapt(enumEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, enumBin.op());
        IRReference enumRhs = assertInstanceOf(IRReference.class, enumBin.right(), "the enum-value operand composes");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, enumRhs.referenceKind());

        // `n = 0` — a NUMERIC literal operand now LOWERS when its SIBLING is a scalar operand (the emitter
        // threads the sibling's resolved numeric type into the literal's render — bare MapperS.of(0) for an int
        // sibling, MapperS.of(BigDecimal.valueOf(0)) for a number sibling; the JOIN/coercion is the emitter's).
        RAttribute n = scalarParam("n");
        functionWith(ws, n);
        REqualityExpr numEq = new REqualityExpr();
        numEq.setOp(EqOp.EQ);
        numEq.setLeft(paramRef("n", n, ws));
        numEq.setRight(intLiteral(0));
        numEq.attachToWorkspace(ws);
        BinaryOp numBin = assertInstanceOf(BinaryOp.class, adapt(numEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, numBin.op());
        IRLiteral numRhs = assertInstanceOf(IRLiteral.class, numBin.right(), "the numeric literal operand composes");
        assertEquals(IRLiteral.LiteralKind.INT, numRhs.literalKind());

        // Two numeric literals DEFER (`1 = 2` — no resolved sibling to type the literal; the emitter cannot
        // determine the coercion, so the adapter declines and legacy renders both bare).
        REqualityExpr litLit = new REqualityExpr();
        litLit.setOp(EqOp.EQ);
        litLit.setLeft(intLiteral(1));
        litLit.setRight(intLiteral(2));
        litLit.attachToWorkspace(ws);
        assertTrue(adapter.adapt(litLit, ws).isEmpty(), "two numeric literals defer (no resolved sibling)");

        // An explicit all/any cardinality modifier defers (not modelled yet).
        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        REqualityExpr withMod = new REqualityExpr();
        withMod.setOp(EqOp.NEQ);
        withMod.setMod(CardMod.ANY);
        withMod.setLeft(paramRef("a", a, ws));
        withMod.setRight(paramRef("b", b, ws));
        withMod.attachToWorkspace(ws);
        // RECUT at #503 (the pin-recut law): the modified equality now LOWERS to the DISTINCT
        // shallow IRAllAnyCompare (the arm-A1 claim; the reason mirror reads empty ⟺ lowers).
        IRAllAnyCompare modLowered = assertInstanceOf(IRAllAnyCompare.class,
                adapter.adapt(withMod, ws).orElseThrow(() -> new AssertionError(
                        "an explicit all/any modifier must lower since the #503 arm")));
        assertEquals("NEQ", modLowered.op(), "the operator fact");
        assertEquals("ANY", modLowered.modifier(), "the modifier fact");
        assertTrue(adapter.declineReason(withMod, ws).isEmpty(),
                "empty ⟺ lowers: the twin's mod exit claims");
    }

    @Test
    void parameterLogicalComposesStructuralOperands() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // (n1 > n2) and (n3 >= n4): both operands lower to a comparison BinaryOp (a ComparisonResult),
        // so the logical composes them into BinaryOp{AND}.
        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        RAttribute n3 = scalarParam("n3");
        RAttribute n4 = scalarParam("n4");
        functionWith(ws, n1, n2, n3, n4);
        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(comparison(CompOp.GT, "n1", n1, "n2", n2, ws));
        and.setRight(comparison(CompOp.GTE, "n3", n3, "n4", n4, ws));
        and.attachToWorkspace(ws);

        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(and, ws));
        assertEquals(IRExprKind.BINARY_OP, bin.kind());
        assertEquals(BinaryOp.BinOp.AND, bin.op());
        assertEquals(2, bin.children().size());
        assertEquals(BinaryOp.BinOp.GT, assertInstanceOf(BinaryOp.class, bin.left()).op());
        assertEquals(BinaryOp.BinOp.GTE, assertInstanceOf(BinaryOp.class, bin.right()).op());
        // operands carry the structural child-index NodeIds (left=child(0), right=child(1)).
        assertEquals(List.of(0), bin.left().nodeId().path());
        assertEquals(List.of(1), bin.right().nodeId().path());
    }

    @Test
    void logicalWithNonComparisonResultOperandDefers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // Bare boolean-param operands lower to IRVariable{PARAM}, NOT a ComparisonResult-producing
        // BinaryOp — legacy renders MapperS.of(flag).andNullSafe(...), a non-compiling/waivered form.
        RAttribute flag1 = scalarParam("flag1");
        RAttribute flag2 = scalarParam("flag2");
        functionWith(ws, flag1, flag2);
        RLogicalExpr bare = new RLogicalExpr();
        bare.setOp(LogOp.OR);
        bare.setLeft(paramRef("flag1", flag1, ws));
        bare.setRight(paramRef("flag2", flag2, ws));
        bare.attachToWorkspace(ws);
        assertTrue(adapter.adapt(bare, ws).isEmpty(), "bare-param logical operands are not ComparisonResults — defer");

        // All-or-nothing: a comparison operand AND a (non-ComparisonResult) literal still defers.
        RAttribute n1 = scalarParam("n1");
        RAttribute n2 = scalarParam("n2");
        functionWith(ws, n1, n2);
        RLogicalExpr mixed = new RLogicalExpr();
        mixed.setOp(LogOp.AND);
        mixed.setLeft(comparison(CompOp.LT, "n1", n1, "n2", n2, ws));
        mixed.setRight(boolLiteral(true));
        mixed.attachToWorkspace(ws);
        assertTrue(adapter.adapt(mixed, ws).isEmpty(), "a non-ComparisonResult operand defers the whole logical");
    }

    @Test
    void scalarParameterExistenceLowersToExistence() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `foo exists` over a scalar parameter lowers to Existence{EXISTS, null, IRVariable{PARAM}}.
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        Existence ex = assertInstanceOf(Existence.class, adapt(existence(ExistenceOp.EXISTS, "foo", foo, ws), ws));
        assertEquals(IRExprKind.EXISTENCE, ex.kind());
        assertEquals(Existence.ExistOp.EXISTS, ex.op());
        assertNull(ex.modifier(), "a bare `exists` carries no single/multiple modifier");
        assertEquals(1, ex.children().size());
        assertEquals(IRVariable.VariableKind.PARAM, assertInstanceOf(IRVariable.class, ex.arg()).variableKind());
        assertEquals(List.of(0), ex.arg().nodeId().path(), "the operand carries the structural child-index NodeId");

        // `bar is absent` lowers to Existence{ABSENT} (the emitter maps it to notExists).
        RAttribute bar = scalarParam("bar");
        functionWith(ws, bar);
        assertEquals(Existence.ExistOp.ABSENT,
                assertInstanceOf(Existence.class, adapt(existence(ExistenceOp.ABSENT, "bar", bar, ws), ws)).op());
    }

    @Test
    void flatListOpsAndCountLowerWhenReceiverLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `xs distinct` over a multi parameter lowers to IRListOp{DISTINCT, child=IRVariable{PARAM,MULTI}} — the
        // receiver subtree lowers, so the flat list op is admitted (the verbatim-oracle render is emitter-side, L-050).
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        IRListOp distinct = assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.DISTINCT, "xs", xs, ws), ws));
        assertEquals(IRExprKind.LIST_OP, distinct.kind());
        assertEquals(IRListOp.Kind.DISTINCT, distinct.op());
        assertEquals(1, distinct.children().size());
        assertEquals(IRVariable.VariableKind.PARAM, assertInstanceOf(IRVariable.class, distinct.child()).variableKind());
        assertEquals(List.of(0), distinct.child().nodeId().path(), "the receiver carries the structural child-index NodeId");

        // flatten / first / last / reverse likewise lower (the flat-op set).
        RAttribute ys = multiParam("ys");
        functionWith(ws, ys);
        assertEquals(IRListOp.Kind.FLATTEN,
                assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.FLATTEN, "ys", ys, ws), ws)).op());
        assertEquals(IRListOp.Kind.FIRST,
                assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.FIRST, "ys", ys, ws), ws)).op());
        assertEquals(IRListOp.Kind.REVERSE,
                assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.REVERSE, "ys", ys, ws), ws)).op());

        // `xs count` lowers to IRListOp{COUNT} (the flat postfix sibling on a distinct AST node).
        assertEquals(IRListOp.Kind.COUNT,
                assertInstanceOf(IRListOp.class, adapt(countOf("xs", xs, ws), ws)).op());

        // ONLY_ELEMENT joins the taught flat set at #498 (the CollectionHandler oracle renders the
        // inline `.get()` collapse + its selfUnwrapping consumer-marker verbatim); SUM joins at
        // #519 — the deeper-diff render is exactly why it is oracle-only on every route (a
        // top-level sum keeps the standing CollectionOpRenderer slot, an interior sum is an
        // oracle leaf via the SUM-scoped containsOracleLeaf leg).
        assertEquals(IRListOp.Kind.ONLY_ELEMENT,
                assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.ONLY_ELEMENT, "xs", xs, ws), ws)).op());
        assertEquals(IRListOp.Kind.SUM,
                assertInstanceOf(IRListOp.class, adapt(listOp(ListOp.SUM, "xs", xs, ws), ws)).op());
    }

    @Test
    void countLowersAsAComparisonAndEqualityOperand() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `xs count = 1` — a count list-op is admitted as an EQUALITY operand against a numeric literal (L-051):
        // the count lowers to IRListOp{COUNT} (its receiver subtree lowers) and serves as the resolved Integer
        // operand that licenses the int literal sibling. The MapperS.of(<chain>.resultCount()) wrap + the Integer
        // witness are the emitter's (the L-029 split); the adapter just claims the operand pair.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        REqualityExpr countEq = new REqualityExpr();
        countEq.setOp(EqOp.EQ);
        countEq.setLeft(countOf("xs", xs, ws));
        countEq.setRight(intLiteral(1));
        countEq.attachToWorkspace(ws);
        BinaryOp eqBin = assertInstanceOf(BinaryOp.class, adapt(countEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, eqBin.op());
        assertEquals(IRListOp.Kind.COUNT, assertInstanceOf(IRListOp.class, eqBin.left()).op(),
                "the count operand lowers to IRListOp{COUNT}");
        assertInstanceOf(IRLiteral.class, eqBin.right(), "the numeric literal sibling composes");

        // `xs count >= 1` — likewise admitted as a COMPARISON operand.
        RComparisonExpr countCmp = new RComparisonExpr();
        countCmp.setOp(CompOp.GTE);
        countCmp.setLeft(countOf("xs", xs, ws));
        countCmp.setRight(intLiteral(1));
        countCmp.attachToWorkspace(ws);
        BinaryOp gteBin = assertInstanceOf(BinaryOp.class, adapt(countCmp, ws));
        assertEquals(BinaryOp.BinOp.GTE, gteBin.op());
        assertEquals(IRListOp.Kind.COUNT, assertInstanceOf(IRListOp.class, gteBin.left()).op());

        // RECUT at #511 (the pin-recut law — the lock's own named class taught): a non-COUNT
        // list-op operand (`xs distinct = ...`) now ADMITS — the "later increment" this lock
        // recorded landed as the #511 operand admission, and the numeric-literal sibling is
        // licensed by the non-COUNT list-op operand (the widened licensing set). The render
        // routes the claim root through the oracle-root equality serve (the containsOracleLeaf
        // non-COUNT shape leg — byte-identical BY IDENTITY), while the COUNT pairs above keep
        // their standing NATIVE resultCount composes via the twin COUNT carve-outs.
        REqualityExpr distinctEq = new REqualityExpr();
        distinctEq.setOp(EqOp.EQ);
        distinctEq.setLeft(listOp(ListOp.DISTINCT, "xs", xs, ws));
        distinctEq.setRight(intLiteral(1));
        distinctEq.attachToWorkspace(ws);
        BinaryOp distinctBin = assertInstanceOf(BinaryOp.class, adapt(distinctEq, ws));
        assertEquals(IRListOp.Kind.DISTINCT,
                assertInstanceOf(IRListOp.class, distinctBin.left()).op(),
                "the non-COUNT operand lowers since the #511 admission");
        assertInstanceOf(IRLiteral.class, distinctBin.right(),
                "the literal licenses off the non-COUNT list-op sibling");
    }

    @Test
    void multiOperandExistenceLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `xs exists` where `xs` is a multi ((0..*)) parameter now lowers — existence is
        // operand-cardinality-agnostic (legacy `ExistenceHandler` wraps the operand's render in the same
        // `exists(...)` call regardless of cardinality), so a multi operand (rendering MapperC.<Item>of(xs))
        // is byte-safe. The Existence carries the multi IRVariable{PARAM} operand verbatim.
        RAttribute xs = multiParam("xs");
        functionWith(ws, xs);
        Existence ex = assertInstanceOf(Existence.class, adapt(existence(ExistenceOp.EXISTS, "xs", xs, ws), ws));
        assertEquals(Existence.ExistOp.EXISTS, ex.op());
        IRVariable operand = assertInstanceOf(IRVariable.class, ex.arg(), "the operand is the multi xs parameter");
        assertEquals(ExpressionCardinality.MULTI, operand.cardinality(), "a multi existence operand now lowers");

        // `trade -> legs exists` where `legs` is multi: a multi NAVIGATION operand also lowers.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs");
        RExistenceExpr navExists = new RExistenceExpr();
        navExists.setOp(ExistenceOp.ABSENT);
        navExists.setArgument(hop(paramRef("trade", trade, ws), "legs", legs, ws));
        navExists.attachToWorkspace(ws);
        Existence navEx = assertInstanceOf(Existence.class, adapt(navExists, ws));
        assertEquals(Existence.ExistOp.ABSENT, navEx.op());
        FieldAccess navArg = assertInstanceOf(FieldAccess.class, navEx.arg(), "the operand is the multi legs navigation");
        assertEquals(ExpressionCardinality.MULTI, navArg.cardinality(), "a multi navigation existence operand lowers");
    }

    @Test
    void callOperandExistenceLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `MyCallee(x) exists` — a function CALL operand now lowers (L-105 call-as-existence-operand): the call
        // lowers to IRApply via adaptApply, admitted by isExistenceOperand, and the emitter wraps it in the same
        // cardinality-agnostic `exists(...)` call (exists(MapperS.of(MyCallee.evaluate(x)))). The Existence carries
        // the FUNCTION-callee IRApply operand verbatim — the direct generalization of the call-as-nav-base slice.
        RAttribute x = scalarParam("x");
        functionWith(ws, x);
        RFunction callee = calleeFunction(ws, "MyCallee", List.of(scalarParam("p")), scalarParam("out"));
        RExistenceExpr callExists = new RExistenceExpr();
        callExists.setOp(ExistenceOp.EXISTS);
        callExists.setArgument(callRef("MyCallee", callee, ws, paramRef("x", x, ws)));
        callExists.attachToWorkspace(ws);
        Existence ex = assertInstanceOf(Existence.class, adapt(callExists, ws),
                "a function-call existence operand now lowers");
        assertEquals(Existence.ExistOp.EXISTS, ex.op());
        assertNull(ex.modifier(), "a bare `exists` carries no single/multiple modifier");
        IRApply operand = assertInstanceOf(IRApply.class, ex.arg(), "the operand is the lowered function call");
        assertEquals(IRReference.ReferenceKind.FUNCTION,
                assertInstanceOf(IRReference.class, operand.callee()).referenceKind(),
                "the existence operand is a FUNCTION-callee IRApply");
    }

    @Test
    void callOperandEqualityLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `MyCallee(x) = baz` — a SINGLE-output function CALL operand now lowers as a scalar equality operand (L-106
        // call-as-cmp/eq-operand): the call lowers to a SINGLE IRApply via adaptApply, admitted by isScalarOperand,
        // and the emitter composes areEqual(MapperS.of(MyCallee.evaluate(x)), MapperS.of(baz), All). The BinaryOp
        // carries the FUNCTION-callee IRApply as its left operand. (This slice trades a §4.2-proxy dip for real
        // coverage — the binary-absorption trap — shipped on Nick's coverage-over-proxy steer.)
        RAttribute x = scalarParam("x");
        RAttribute baz = scalarParam("baz");
        functionWith(ws, x, baz);
        RFunction callee = calleeFunction(ws, "MyCallee", List.of(scalarParam("p")), scalarParam("out"));
        REqualityExpr callEq = new REqualityExpr();
        callEq.setOp(EqOp.EQ);
        callEq.setLeft(callRef("MyCallee", callee, ws, paramRef("x", x, ws)));
        callEq.setRight(paramRef("baz", baz, ws));
        callEq.attachToWorkspace(ws);
        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(callEq, ws),
                "a function-call equality operand now lowers");
        assertEquals(BinaryOp.BinOp.EQ, bin.op());
        IRApply operand = assertInstanceOf(IRApply.class, bin.left(), "the left operand is the lowered function call");
        assertEquals(ExpressionCardinality.SINGLE, operand.cardinality(),
                "only a SINGLE-output call is a scalar operand");
        assertEquals(IRReference.ReferenceKind.FUNCTION,
                assertInstanceOf(IRReference.class, operand.callee()).referenceKind(),
                "the equality operand is a FUNCTION-callee IRApply");
    }

    @Test
    void nestedCallArgLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `Outer(Inner(x))` — a NESTED function-call argument now lowers (L-107 call-as-nested-arg): the inner call
        // Inner(x) lowers to an IRApply (its own arg is a simple param), admitted by isSimpleCallArg, and emitApply
        // renders it recursively (MapperS.of(Inner.evaluate(x))) which unwrapForEvaluateArg strips into Outer's
        // evaluate slot — byte-identical via the same oracle the top-level call uses, no emitter change. The outer
        // IRApply carries the inner IRApply as arg[0].
        RAttribute x = scalarParam("x");
        functionWith(ws, x);
        RFunction inner = calleeFunction(ws, "Inner", List.of(scalarParam("p")), scalarParam("innerOut"));
        RFunction outer = calleeFunction(ws, "Outer", List.of(scalarParam("q")), scalarParam("outerOut"));
        IRApply apply = assertInstanceOf(IRApply.class,
                adapt(callRef("Outer", outer, ws, callRef("Inner", inner, ws, paramRef("x", x, ws))), ws),
                "a nested-call argument now lowers");
        assertEquals(1, apply.args().size());
        IRApply nestedArg = assertInstanceOf(IRApply.class, apply.args().get(0),
                "the arg is the inner function call");
        assertEquals(IRReference.ReferenceKind.FUNCTION,
                assertInstanceOf(IRReference.class, nestedArg.callee()).referenceKind(),
                "the nested arg is a FUNCTION-callee IRApply");
        assertEquals(List.of(1), nestedArg.nodeId().path(), "the nested arg takes child slot 1 (the callee is slot 0)");
    }

    @Test
    void existenceWithElidedArgumentDefers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A null argument (a `then`/`filter`-elided implicit operand). The #519 elided-implicit
        // synthesis types such an operand from the nearest enclosing lambda's own pipe source —
        // but THIS hand-built node has no enclosing binder at all (the elidedOperand.noEnclosing
        // stop), so the claim still defers: the synthesis is prove-or-decline, never a guessed
        // binding. (The #520 Seat-1 OBS-2 recut of the pre-#519 "later increment" note.)
        RExistenceExpr elided = new RExistenceExpr();
        elided.setOp(ExistenceOp.EXISTS);
        elided.attachToWorkspace(ws);
        assertTrue(adapter.adapt(elided, ws).isEmpty(),
                "a binder-less elided existence operand defers (the noEnclosing stop)");
    }

    @Test
    void existenceComposesThroughLogical() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `foo exists and bar exists`: both operands lower to an Existence (a ComparisonResult), so
        // the logical composes them — proving producesComparisonResult admits Existence, not just BinaryOp.
        RAttribute foo = scalarParam("foo");
        RAttribute bar = scalarParam("bar");
        functionWith(ws, foo, bar);
        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(existence(ExistenceOp.EXISTS, "foo", foo, ws));
        and.setRight(existence(ExistenceOp.EXISTS, "bar", bar, ws));
        and.attachToWorkspace(ws);

        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(and, ws));
        assertEquals(BinaryOp.BinOp.AND, bin.op());
        assertInstanceOf(Existence.class, bin.left());
        assertInstanceOf(Existence.class, bin.right());
    }

    @Test
    void scalarParameterNavigationLowersToFieldAccess() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `foo -> bar`: a scalar-param receiver and a single non-meta resolved feature → FieldAccess.
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        RAttribute bar = scalarParam("bar"); // the navigated feature (single, non-meta)
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(paramRef("foo", foo, ws));
        fc.setFeatureName("bar");
        fc.setResolvedFeature(bar);
        fc.attachToWorkspace(ws);

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(fc, ws));
        assertEquals(IRExprKind.FIELD_ACCESS, fa.kind());
        assertEquals("bar", fa.feature());
        assertEquals(1, fa.children().size());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, fa.receiver()).variableKind());
        assertEquals(List.of(0), fa.receiver().nodeId().path(), "the receiver carries the child-index NodeId");
    }

    @Test
    void navigationWithUnresolvedFeatureOrNonParamReceiverDefers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // An UNRESOLVED feature (a disguised enum/choice nav or a built-in record feature) defers.
        RAttribute p1 = scalarParam("foo");
        functionWith(ws, p1);
        RFeatureCall unresolved = new RFeatureCall();
        unresolved.setReceiver(paramRef("foo", p1, ws));
        unresolved.setFeatureName("bar");
        unresolved.attachToWorkspace(ws);
        assertTrue(adapter.adapt(unresolved, ws).isEmpty(), "an unresolved feature defers");

        // (A MULTI feature off a SINGLE receiver lowers as mapC — see
        // singleMultiHopOffScalarParamLowersToMultiFieldAccess; a chain through a multi hop lowers too,
        // per-hop map/mapC — see chainThroughMultiHopLowers.)

        // A non scalar-parameter receiver (here a literal) defers.
        RFeatureCall litReceiver = new RFeatureCall();
        litReceiver.setReceiver(intLiteral(1));
        litReceiver.setFeatureName("bar");
        litReceiver.setResolvedFeature(scalarParam("bar"));
        litReceiver.attachToWorkspace(ws);
        assertTrue(adapter.adapt(litReceiver, ws).isEmpty(), "a non-scalar-param receiver defers");
    }

    @Test
    void navigationResultComposesAsScalarOperand() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RAttribute foo = scalarParam("foo");
        RAttribute baz = scalarParam("baz");
        functionWith(ws, foo, baz);
        RAttribute bar = scalarParam("bar"); // the navigated feature (single, non-meta)

        // `foo -> bar exists`: the existence gate now accepts a single FieldAccess operand.
        RExistenceExpr navExists = new RExistenceExpr();
        navExists.setOp(ExistenceOp.EXISTS);
        navExists.setArgument(featureCall("foo", foo, "bar", bar, ws));
        navExists.attachToWorkspace(ws);
        Existence ex = assertInstanceOf(Existence.class, adapt(navExists, ws));
        assertInstanceOf(FieldAccess.class, ex.arg(), "existence now composes a navigation operand");

        // `foo -> bar = baz`: equality now accepts a FieldAccess operand alongside a scalar param.
        REqualityExpr navEq = new REqualityExpr();
        navEq.setOp(EqOp.EQ);
        navEq.setLeft(featureCall("foo", foo, "bar", bar, ws));
        navEq.setRight(paramRef("baz", baz, ws));
        navEq.attachToWorkspace(ws);
        BinaryOp bin = assertInstanceOf(BinaryOp.class, adapt(navEq, ws));
        assertEquals(BinaryOp.BinOp.EQ, bin.op());
        assertInstanceOf(FieldAccess.class, bin.left(), "the navigation LHS composes as a scalar operand");
        assertInstanceOf(IRVariable.class, bin.right());
    }

    @Test
    void scalarParameterNavigationChainLowersToNestedFieldAccess() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `a -> b -> c`: a 2-hop chain off a scalar-param base. The outer hop's receiver is the inner
        // single non-meta navigation, so the broadened receiver gate composes the chain by recursion —
        // FieldAccess{ c, receiver = FieldAccess{ b, receiver = IRVariable{PARAM a} } }. Every hop is a
        // single non-meta resolved feature, so the multi/meta/unresolved deferrals never fire.
        RAttribute a = scalarParam("a");
        functionWith(ws, a);
        RAttribute b = scalarParam("b"); // inner feature (single, non-meta)
        RAttribute c = scalarParam("c"); // outer feature (single, non-meta)
        RFeatureCall outer = hop(featureCall("a", a, "b", b, ws), "c", c, ws); // (a -> b) -> c

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(outer, ws));
        assertEquals(IRExprKind.FIELD_ACCESS, fa.kind());
        assertEquals("c", fa.feature());
        assertEquals(1, fa.children().size());
        FieldAccess inner = assertInstanceOf(FieldAccess.class, fa.receiver(),
                "the chain receiver is itself a navigation hop");
        assertEquals("b", inner.feature());
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, inner.receiver()).variableKind());
        // Structural child-index NodeIds nest along the chain: receiver=child(0), base=child(0).child(0).
        assertEquals(List.of(0), fa.receiver().nodeId().path());
        assertEquals(List.of(0, 0), inner.receiver().nodeId().path());
    }

    @Test
    void navigationChainOptionalityIsTheAbsorbingMonoid() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // Absorbing monoid (design §6): any OPTIONAL hop makes the whole chain OPTIONAL. The
        // intermediate feature `b` is (0..1), so `a -> b` is optional and `a -> b -> c` stays optional
        // even though the final feature `c` is present — the receiver's optionality propagates through
        // the later hop (which the per-feature optionality alone would miss).
        RAttribute a = scalarParam("a");
        functionWith(ws, a);
        RAttribute optB = optionalParam("b"); // (0..1) — optional single intermediate feature
        RAttribute c = scalarParam("c");       // (1..1) — present final feature
        RFeatureCall outer = hop(featureCall("a", a, "b", optB, ws), "c", c, ws); // a -> b -> c

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(outer, ws));
        assertEquals(Optionality.OPTIONAL, fa.optionality(),
                "an optional intermediate hop makes the whole chain optional (absorbing monoid)");
        assertEquals(Optionality.OPTIONAL,
                assertInstanceOf(FieldAccess.class, fa.receiver()).optionality(),
                "the optional intermediate hop is itself optional");
    }

    @Test
    void singleMultiHopOffScalarParamLowersToMultiFieldAccess() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `trade -> legs` where `legs` is (0..*): a single MULTI feature off a single scalar-param
        // receiver lowers to a FieldAccess — the (receiver SINGLE, feature MULTI) cell the emitter
        // renders MapperS.of(trade).<Leg>mapC("getLegs", ...). The result is itself MULTI, so it stays
        // out of the single-only receiver/operand gates (chain-through-multi is a later increment).
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs"); // multi feature (0..*)
        FieldAccess fa = assertInstanceOf(FieldAccess.class,
                adapt(hop(paramRef("trade", trade, ws), "legs", legs, ws), ws));
        assertEquals("legs", fa.feature());
        assertEquals(ExpressionCardinality.MULTI, fa.cardinality(),
                "navigating a multi feature off a single receiver yields a multi result");
        assertEquals(IRVariable.VariableKind.PARAM,
                assertInstanceOf(IRVariable.class, fa.receiver()).variableKind());
    }

    @Test
    void chainThroughMultiHopLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `trade -> legs -> rate` (legs multi, rate single): a chain THROUGH a multi hop now lowers. Each
        // FieldAccess carries its hop's OWN featureCardinality — `legs` MULTI (the emitter renders mapC),
        // `rate` SINGLE (renders map off the MapperC) — while the accumulated `cardinality` is MULTI for
        // the whole chain (the monotone multi overlay), keeping the multi result out of the scalar-operand gate.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute legs = multiParam("legs");
        RAttribute rate = scalarParam("rate");
        RFeatureCall outer = hop(hop(paramRef("trade", trade, ws), "legs", legs, ws), "rate", rate, ws);

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(outer, ws));
        assertEquals("rate", fa.feature());
        assertEquals(ExpressionCardinality.SINGLE, fa.featureCardinality(), "the rate hop is single-stepped");
        assertEquals(ExpressionCardinality.MULTI, fa.cardinality(), "the whole chain is multi (monotone overlay)");
        FieldAccess inner = assertInstanceOf(FieldAccess.class, fa.receiver(), "the receiver is the multi legs hop");
        assertEquals("legs", inner.feature());
        assertEquals(ExpressionCardinality.MULTI, inner.featureCardinality(), "the legs hop is multi-stepped");
    }

    @Test
    void multiParamBaseNavigationLowers() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `legs -> rate` where `legs` is a multi ((0..*)) PARAMETER — the navigation BASE is a multi
        // parameter (it lowers to an IRVariable{PARAM, MULTI}, NOT a FieldAccess), which the broadened
        // isNavigableReceiver now admits. `rate` is a single non-meta feature, so its FieldAccess carries
        // featureCardinality SINGLE (the emitter renders `map` off the MapperC) while the accumulated
        // cardinality is MULTI (the receiver is multi — the monotone overlay), keeping the whole nav out of
        // the scalar-operand gate. The sole difference from chainThroughMultiHopLowers is the receiver kind:
        // an IRVariable{PARAM, MULTI} leaf vs a multi FieldAccess hop.
        RAttribute legs = multiParam("legs");
        functionWith(ws, legs);
        RAttribute rate = scalarParam("rate"); // single non-meta navigated feature

        FieldAccess fa = assertInstanceOf(FieldAccess.class,
                adapt(hop(paramRef("legs", legs, ws), "rate", rate, ws), ws));
        assertEquals("rate", fa.feature());
        assertEquals(ExpressionCardinality.SINGLE, fa.featureCardinality(), "the rate hop is single-stepped");
        assertEquals(ExpressionCardinality.MULTI, fa.cardinality(),
                "the whole nav is multi off a multi-parameter base (monotone overlay)");
        IRVariable base = assertInstanceOf(IRVariable.class, fa.receiver(),
                "the navigation base is the multi legs parameter, not a FieldAccess");
        assertEquals(IRVariable.VariableKind.PARAM, base.variableKind());
        assertEquals(ExpressionCardinality.MULTI, base.cardinality(), "the base receiver is a multi parameter");
    }

    // === Conditional (if-then-else) — step-12 task-1 ==========================
    //
    // STRUCTURAL cases (hand-built literal branches, mirroring scalarLiteralsLowerToIRLiteral): pure node
    // shape/kind/children-nullness facts that don't depend on real type/cardinality inference.
    //
    // LINKED cases (real functionWith/scalarParam/multiParam/featureCall receivers): D3 (type) and D4
    // (cardinality) need a REAL resolved feature — a hand-built literal branch would trivially agree on
    // type/cardinality without ever exercising the branch-join computation this arm is responsible for.

    @Test
    void genuineElseConditionalLowersToIRConditional() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if true then 1 else 2` — a genuine (non-empty) else lowers all three slots.
        RConditionalExpr cond = conditional(boolLiteral(true), intLiteral(1), intLiteral(2), ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertEquals(IRExprKind.CONDITIONAL, ir.kind());
        assertInstanceOf(IRLiteral.class, ir.condition(), "the condition child lowers");
        assertInstanceOf(IRLiteral.class, ir.thenBranch(), "the then-branch child lowers");
        assertInstanceOf(IRLiteral.class, ir.elseBranch(), "a genuine else-branch child lowers");
    }

    @Test
    void noElseConditionalHasNullElseBranchAndOptionalOptionality() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if true then 1` (else omitted entirely) — elseBranch is never set.
        RConditionalExpr cond = conditional(boolLiteral(true), intLiteral(1), null, ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertNull(ir.elseBranch(), "an absent else means the Java ifThenElseResult local stays null");
        assertEquals(Optionality.OPTIONAL, ir.optionality(),
                "no genuine else ⇒ OPTIONAL (D5): a false/absent condition yields no value");
    }

    @Test
    void emptyElseBranchIsTreatedAsNoElse() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if true then 1 else empty` — a value-less else is NOT genuine (hasGenuineElse, D2):
        // structurally identical to the no-else case, matching DefaultElseRule's injected default.
        RConditionalExpr cond = conditional(boolLiteral(true), intLiteral(1), new REmptyLiteral(), ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertNull(ir.elseBranch(), "an empty else is not genuine (D2) — treated exactly like no-else");
    }

    @Test
    void nestedElseIfLowersElseBranchToNestedIRConditional() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if true then 1 else (if false then 2)` — an else-if chain: the adapter recurses through
        // the SAME arm for the nested conditional (D6).
        RConditionalExpr inner = conditional(boolLiteral(false), intLiteral(2), null, ws);
        RConditionalExpr outer = conditional(boolLiteral(true), intLiteral(1), inner, ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(outer, ws));
        assertInstanceOf(IRConditional.class, ir.elseBranch(), "a nested else-if recurses through the same arm");
        // D5 else-branch disjunct: the else here is a no-else (hence OPTIONAL) inner conditional while the then is a
        // PRESENT literal, so the outer absorbs to OPTIONAL solely via `elseIr.optionality()==OPTIONAL` — witnessing
        // the middle disjunct that the extreme no-else / all-present tests do not reach.
        assertEquals(Optionality.OPTIONAL, ir.optionality(),
                "a genuine-else conditional whose else-branch is OPTIONAL absorbs to OPTIONAL (D5 else-disjunct)");
    }

    @Test
    void conditionalDeclinesWhenAnyChildDeclines() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // A bare unresolved symbol reference is a Wave-1+ concern (unsupportedAndPartiallyUnsupportedNodesReturnEmpty)
        // — it never adapts, so it's a convenient non-adapting child to place in each slot in turn (D1).
        RSymbolReference declinedThen = new RSymbolReference();
        declinedThen.setName("someParam");
        assertTrue(adapter.adapt(conditional(boolLiteral(true), declinedThen, null, ws), ws).isEmpty(),
                "a non-adapting then-branch sinks the whole conditional");

        RSymbolReference declinedCondition = new RSymbolReference();
        declinedCondition.setName("someParam");
        assertTrue(adapter.adapt(conditional(declinedCondition, intLiteral(1), null, ws), ws).isEmpty(),
                "a non-adapting condition sinks the whole conditional");

        RSymbolReference declinedElse = new RSymbolReference();
        declinedElse.setName("someParam");
        assertTrue(adapter.adapt(conditional(boolLiteral(true), intLiteral(1), declinedElse, ws), ws).isEmpty(),
                "a non-adapting genuine else-branch sinks the whole conditional");
    }

    @Test
    void noElseConditionalTypeEqualsThenBranchType() {
        // D3 needs a NON-MISSING inferred type to have teeth. Unlike cardinality (RWorkspace.getCardinality
        // → CardinalityComputer.compute, a live recursive computation — hence hand-built AST works fine for
        // the D4 tests below), RWorkspace.getInferredType is a PURE CACHE LOOKUP populated only by the
        // fixed-point pass RWorkspace.build(models) runs over the models given to it
        // (TypeInferenceEngine.getInferredType: `inferredTypes.getOrDefault(expr, MISSING)`) — a hand-built,
        // never-registered node (as functionWith/featureCall build) can only ever resolve to MISSING here. So
        // this test parses a tiny real snippet into its OWN workspace instead (mirrors SmokeTypeInferenceTest),
        // which puts the then-branch through the real fixed-point walk and gives it a genuine type.
        //
        // The then-branch is a bare parameter reference (`foo`), not a `trade -> foo` navigation: the grammar's
        // `expression ARROW validID` production parses EVERY bare `identifier -> identifier` (including a
        // lowercase receiver like `trade`) as an REnumValueRef — genuine-vs-disguised is a LINKING-time
        // distinction (see qualifiedEnumValueRefLowersToIRReferenceENUMVALUE_disguisesDefer's "DISGUISE" cases)
        // — and adaptEnumValueRef declines the disguised (non-enum) case, which would sink this whole
        // conditional (D1). A bare parameter reference has no such ambiguity and still exercises a REAL,
        // non-MISSING resolved type (D3 cares about the branch-join, not the then-branch's own shape).
        String source = """
                namespace "test.step12"

                func MyFunc:
                    inputs:
                        foo int (1..1)
                        active boolean (1..1)
                    output:
                        result int (0..1)
                    set result:
                        if active then foo
                """;
        RModel model = AstBuilder.buildFromString(source, "step12-d3.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RConditionalExpr cond = AstWalker.findFirst(ws.files().get(0), RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConditionalExpr"));
        RSymbolReference thenBranch = assertInstanceOf(RSymbolReference.class, cond.thenBranch());

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        RNumberType thenType = assertInstanceOf(RNumberType.class, ws.getInferredType(thenBranch).type(),
                "foo resolves to a real (non-MISSING) type through the parsed+linked workspace");
        assertEquals(thenType, ir.type().type(),
                "a no-else conditional's type equals the then-branch's type (D3: join(then, NOTHING) == then)");
    }

    @Test
    void noElseConditionalWithMultiThenBranchHasMultiCardinality() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if trade->active then trade->legs` (legs MULTI, no else) — CardinalityComputer stamps EVERY
        // RConditionalExpr SINGLE (conservative), so this proves the arm computes the branch join itself
        // (D4) instead of reading card(expr, ws).
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute active = scalarParam("active");
        RAttribute legs = multiParam("legs");

        RConditionalExpr cond = conditional(
                featureCall("trade", trade, "active", active, ws),
                featureCall("trade", trade, "legs", legs, ws), null, ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertEquals(ExpressionCardinality.MULTI, ir.cardinality(),
                "a MULTI then-branch drives the conditional MULTI (D4 then-MULTI disjunct)");
    }

    @Test
    void genuineElseConditionalWithMultiElseBranchHasMultiCardinality() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if trade->active then trade->foo else trade->legs` (foo SINGLE, legs MULTI) — the else-branch
        // disjunct of D4's join, unobservable from a Python-side test (the emitter declines this mixed
        // shape per D10); the adapter's cardinality FACT is still the accurate join.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute active = scalarParam("active");
        RAttribute foo = scalarParam("foo");
        RAttribute legs = multiParam("legs");

        RConditionalExpr cond = conditional(
                featureCall("trade", trade, "active", active, ws),
                featureCall("trade", trade, "foo", foo, ws),
                featureCall("trade", trade, "legs", legs, ws), ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertEquals(ExpressionCardinality.MULTI, ir.cardinality(),
                "a MULTI else-branch also drives the conditional MULTI (D4 else-MULTI disjunct)");
    }

    @Test
    void genuineElseConditionalWithBothSingleBranchesHasSingleCardinalityAndPresentOptionality() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // `if trade->active then trade->foo else trade->bar` — both branches SINGLE, genuine else.
        RAttribute trade = scalarParam("trade");
        functionWith(ws, trade);
        RAttribute active = scalarParam("active");
        RAttribute foo = scalarParam("foo");
        RAttribute bar = scalarParam("bar");

        RConditionalExpr cond = conditional(
                featureCall("trade", trade, "active", active, ws),
                featureCall("trade", trade, "foo", foo, ws),
                featureCall("trade", trade, "bar", bar, ws), ws);

        IRConditional ir = assertInstanceOf(IRConditional.class, adapt(cond, ws));
        assertEquals(ExpressionCardinality.SINGLE, ir.cardinality(), "both branches SINGLE ⇒ SINGLE (D4)");
        assertEquals(Optionality.PRESENT, ir.optionality(),
                "a genuine else with both branches PRESENT ⇒ PRESENT (D5)");
    }

    // ---------------------------------------------------------------------------------
    // The #477 decline-reason channel — the per-ARM refinement decode locks
    // ---------------------------------------------------------------------------------

    @Test
    void declineReasonEmptyForLoweringNodeAndNamesEnumChannels() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);

        // empty ⟺ lowers: a genuine enum-value ref lowers, so it carries NO reason.
        assertTrue(adapter.declineReason(enumValueRef("FooEnum", "Cash", en, ev, ws), ws).isEmpty(),
                "a lowering node must carry NO decline reason (empty ⟺ lowers)");

        // The disguise channels, in computeEnumValueRef's own resolution precedence.
        REnumValueRef chained = enumValueRef("FooEnum", "Cash", en, ev, ws);
        chained.setResolvedAttributeChain(
                new REnumValueRef.AttributeChain(scalarParam("a"), scalarParam("b")));
        // #480: the channel lowers through the implicit-item CHAIN arm now, so a DECLINER
        // composes the arm's own first failing gate onto the channel token (here: no enclosing
        // lambda ⇒ the filter/extract binder gate names the exit).
        assertEquals(Optional.of("attributeChain.noFilterExtractBinder"),
                adapter.declineReason(chained, ws),
                "the attribute-chain channel composes the arm's own gate onto its token");

        REnumValueRef inputNav = new REnumValueRef();
        inputNav.setEnumName("head");
        inputNav.setValueName("feature");
        inputNav.setResolvedInputFeature(scalarParam("feature"));
        inputNav.attachToWorkspace(ws);
        // #478: the channel lowers through its legacy-equivalent nav now, so a DECLINER composes the
        // equivalent's own first failing gate onto the channel token (here: no enclosing function ⇒
        // the equivalent's head/feature never resolve ⇒ its feature gate names the exit).
        assertEquals(Optional.of("inputFeatureNav.featureUnresolved.headUnresolved"),
                adapter.declineReason(inputNav, ws),
                "the L-111 channel composes the equivalent's own gate onto its token (#492: no"
                        + " enclosing function, no collision — the headUnresolved face)");

        REnumValueRef bare = new REnumValueRef();
        bare.setEnumName("foo");
        bare.setValueName("bar");
        bare.attachToWorkspace(ws);
        // #507: the record arm's twin composes its first failing gate onto the channel token
        // (the #486 refine-in-place precedent); a head resolving nowhere reads headMiss —
        // #514: the non-closure-param residue spelled `.other` by the in-place cp split.
        assertEquals(Optional.of("noResolutionChannel.headMiss.other"),
                adapter.declineReason(bare, ws),
                "an unresolved disguise lands in the no-channel bucket, gate-composed");
    }

    @Test
    void declineReasonNamesSymbolCallAndFeatureCallGates() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // attrOutsideFunction — a resolved attribute with no enclosing RFunction (a data-type
        // field). #480: the bare-attr arm runs on this leg, so the token composes the arm's own
        // first failing gate (no enclosing lambda ⇒ the filter/extract binder gate).
        // #528 arm-5 RETENSE, re-tensed at the fix commit: the three census families MINT
        // since the L-113 conversion — for a TYPED attribute (the mint's #514-pattern guard,
        // the Copilot #528 R1 catch). This typeless engine-state fixture declines instead,
        // and the mirror names the untypeable residue's own dot-refined face IN LOCKSTEP
        // (twin-exact with the mint site's guards BY CALL — a mirror reading UNATTRIBUTED
        // while the mint declines would be twin drift).
        assertEquals(Optional.of("attrOutsideFunction.noFilterExtractBinder.untyped"),
                adapter.declineReason(paramRef("x", scalarParam("x"), ws), ws),
                "the untypeable mint-family residue keeps a face — dot-refined in place");
        assertTrue(adapter.adapt(paramRef("x", scalarParam("x"), ws), ws).isEmpty(),
                "the twin: the mint's type guard declines exactly what the mirror names");

        // symbolUnresolved / bareFunctionReference — the two !(RAttribute) sub-classes. #486:
        // the flat token refined IN PLACE to symbolUnresolved.<facet> — an unattached ref has
        // no ancestors and matches no root element, so it reads the absent face (the finer
        // constructions are pinned by declineReasonDecodesSymbolUnresolvedFacets).
        RSymbolReference unresolved = new RSymbolReference();
        unresolved.setName("mystery");
        unresolved.attachToWorkspace(ws);
        assertEquals(Optional.of("symbolUnresolved.absent"), adapter.declineReason(unresolved, ws));

        RFunction pointFreeCallee =
                calleeFunction(ws, "IsFXOption", List.of(scalarParam("p")), scalarParam("out"));
        RSymbolReference pointFree = new RSymbolReference();
        pointFree.setName("IsFXOption");
        pointFree.setResolvedSymbol(pointFreeCallee);
        pointFree.attachToWorkspace(ws);
        // #492: the L-109 deferral RESOLVED — the point-free shape LOWERS to its own kind (NOT an
        // empty-args IRApply: the IRApply-keyed gates stay byte-frozen), carrying the callee name
        // + the OUTPUT's channels; a claimed node never re-spells its old token.
        Optional<IRExpr> pointFreeLowered = adapter.adapt(pointFree, ws);
        assertTrue(pointFreeLowered.isPresent(), "the point-free shape must lower (#492)");
        IRPointFreeApply pfa = assertInstanceOf(IRPointFreeApply.class, pointFreeLowered.get());
        assertEquals("IsFXOption", pfa.callee());
        assertTrue(adapter.declineReason(pointFree, ws).isEmpty(),
                "a claimed point-free node never re-spells bareFunctionReference");
        // The ONE decline face: a bare-F whose name collides with an enclosing shortcut — legacy
        // isAliasReference wins over the bare-function arm, so it renders as an ALIAS CALL.
        RFunction collisionFn = new RFunction();
        collisionFn.setName("CollisionFn");
        RShortcut collidingShortcut = new RShortcut();
        collidingShortcut.setName("IsFXOption");
        collisionFn.shortcuts().add(collidingShortcut);
        collidingShortcut.setParent(collisionFn);
        collisionFn.attachToWorkspace(ws);
        RSymbolReference collidingPointFree = new RSymbolReference();
        collidingPointFree.setName("IsFXOption");
        collidingPointFree.setResolvedSymbol(pointFreeCallee);
        collidingPointFree.setParent(collisionFn);
        collidingPointFree.attachToWorkspace(ws);
        assertTrue(adapter.adapt(collidingPointFree, ws).isEmpty(),
                "a shortcut-colliding bare-F must defer (legacy renders an alias invocation)");
        assertEquals(Optional.of("bareFunctionReference.aliasCollision"),
                adapter.declineReason(collidingPointFree, ws));

        // calleeMetaOutput — RECUT at #502 to the taught behavior (the #501 pin-recut law):
        // a FUNCTION call whose callee output carries [metadata] now LOWERS to the DISTINCT
        // shallow IRMetaOutputApply (the arm claims; empty ⟺ lowers).
        RAttribute metaOut = scalarParam("metaOut");
        RAnnotationRef metaAnnotation = new RAnnotationRef();
        metaAnnotation.setAnnotationName("metadata");
        metaOut.annotationRefs().add(metaAnnotation);
        RFunction metaCallee = calleeFunction(ws, "MetaCallee", List.of(scalarParam("r")), metaOut);
        RSymbolReference metaOutCall = callRef("MetaCallee", metaCallee, ws, intLiteral(1));
        assertInstanceOf(IRMetaOutputApply.class,
                adapter.adapt(metaOutCall, ws).orElseThrow(() -> new AssertionError(
                        "a meta-output FUNCTION call must lower since the #502 arm")),
                "the call lowers to the shallow meta-output kind");
        assertTrue(adapter.declineReason(metaOutCall, ws).isEmpty(),
                "empty ⟺ lowers: the twin's FUNCTION-slice restatement claims it");

        // argNav — a call whose argument lowers to a FieldAccess navigation (needs .get():
        // deferred). #488: the flat token refined IN PLACE to argNav.[meta.|typeMissing.]
        // <root>.<depth>[.multi] — the two-step run-first disclosure: the first cut
        // predicted typeMissing. here and the run read the CLEAN spelling, because
        // getInferredType returns the NON-NULL MISSING sentinel for a hand-built hop the
        // fixed point never saw (getOrDefault(expr, MISSING) — nothing computes on
        // demand) and the null-only check could not see it; the Seat-1 MF-1 recut made
        // the detector LIVE (isMissing()). RETENSED at #527 (the missing-hop admission):
        // a PURE missing-typed read on a PARAM-rooted chain now ADMITS — the exact shape
        // that once disclosed the typeMissing. face is the compiler-side argNavMissingHop
        // probe's trigger (an IRApply carrying a FieldAccess arg whose hop reads the
        // MISSING sentinel — the claim serves whole-legacy at the claim root), so the pin
        // asserts the composed lowering carries that trigger shape; the decline boundary
        // moved to the META flag and the non-param roots (the drift locks below and the
        // #491 belt pin argNavRootTypeBeltDeclinesMetaAndMissingRoots).
        RAttribute foo = scalarParam("foo");
        functionWith(ws, foo);
        RFeatureCall navArg = featureCall("foo", foo, "bar", scalarParam("bar"), ws);
        RFunction navCallee =
                calleeFunction(ws, "NavCallee", List.of(scalarParam("q")), scalarParam("navOut"));
        RSymbolReference missingHopCall = callRef("NavCallee", navCallee, ws, navArg);
        assertEquals(Optional.empty(), adapter.declineReason(missingHopCall, ws),
                "the #527 missing-hop admission: a pure missing-typed param-rooted chain claims");
        IRExpr missingHopLowered = adapter.adapt(missingHopCall, ws).orElseThrow(
                () -> new AssertionError("the admitted missing-hop call must lower"));
        IRApply missingHopApply = assertInstanceOf(IRApply.class, missingHopLowered,
                "the call lowers to an apply carrying the chain arg");
        FieldAccess missingHopArg = assertInstanceOf(FieldAccess.class,
                missingHopApply.args().get(0), "the admitted arg is the FieldAccess chain");
        assertTrue(missingHopArg.type() == null || missingHopArg.type().isMissing(),
                "the hop keeps the MISSING sentinel — the compiler probe's trigger shape");

        // metaFeature — recut at #499 (the meta arm): a meta nav over a LOWERING receiver now
        // CLAIMS (the arm lowers it to IRMetaAccess — the identity witness below), so the
        // composed residue face `metaFeature.recvBlocked` is pinned on the receiver-BLOCKED
        // twin: the same meta feature navigated off an UNRESOLVED bare name (nothing binds
        // "mystery", so the receiver subtree cannot lower — the belt-kept class).
        RAttribute metaBar = scalarParam("bar");
        RAnnotationRef metaFeatureAnnotation = new RAnnotationRef();
        metaFeatureAnnotation.setAnnotationName("metadata");
        metaBar.annotationRefs().add(metaFeatureAnnotation);
        assertEquals(Optional.empty(),
                adapter.declineReason(featureCall("foo", foo, "bar", metaBar, ws), ws),
                "#499: a meta nav whose receiver lowers is CLAIMED by the meta arm (the mirror"
                        + " reads UNATTRIBUTED → empty)");
        RFeatureCall recvBlockedMetaNav = new RFeatureCall();
        RSymbolReference mysteryRecv = new RSymbolReference();
        mysteryRecv.setName("mystery");
        mysteryRecv.attachToWorkspace(ws);
        recvBlockedMetaNav.setReceiver(mysteryRecv);
        recvBlockedMetaNav.setFeatureName("bar");
        recvBlockedMetaNav.setResolvedFeature(metaBar);
        recvBlockedMetaNav.attachToWorkspace(ws);
        assertEquals(Optional.of("metaFeature.recvBlocked"),
                adapter.declineReason(recvBlockedMetaNav, ws),
                "#499: the belt-kept residue face — a meta nav whose receiver does not lower");

        RFeatureCall recordFeature = new RFeatureCall();
        recordFeature.setReceiver(paramRef("foo", foo, ws));
        recordFeature.setFeatureName("day");
        recordFeature.attachToWorkspace(ws);
        assertEquals(Optional.of("featureUnresolved.headAttr.declMiss"),
                adapter.declineReason(recordFeature, ws));

        // receiverAlias — the alias receiver LOWERS (IRReference{ALIAS}) but is not a navigable base.
        RShortcut shortcut = new RShortcut();
        shortcut.setName("economicTerms");
        RFeatureCall aliasNav = new RFeatureCall();
        aliasNav.setReceiver(aliasRef(shortcut, ws));
        aliasNav.setFeatureName("payout");
        aliasNav.setResolvedFeature(scalarParam("payout"));
        aliasNav.attachToWorkspace(ws);
        assertEquals(Optional.of("receiverAlias.bodyMissing"), adapter.declineReason(aliasNav, ws),
                "an alias receiver is named by its lowered form, not folded into not-expressible"
                        + " (#492: the body-less fixture shortcut lands the bodyMissing face)");
    }

    @Test
    void declineReasonDecodesSymbolUnresolvedFacets() {
        // #486 — the symbolUnresolved DECODE witness: the flat reason token refined IN PLACE to
        // symbolUnresolved.[synthetic.]<facet> — the MECHANISM split first (a ref whose parent
        // does not child-link it is an UP-only parented render-time synthesis — legacy
        // ReferenceHandler.synthesizeFeatureCall's disguised-nav receiver shape, the
        // decode-proven dominant population; the parse-time complement reads unprefixed), then
        // the head-name ladder (WHAT the name matches, in the resolution machinery's own
        // precedence). Each pin builds the facet's construction; the hand-parented probe refs
        // are orphans BY CONSTRUCTION (setParent only — the exact synthesized-receiver shape),
        // so they pin the synthetic. faces; the child-linked sibling pin locks the plain path.
        // The parsed model gives the workspace a REAL name index (the global.* faces need true
        // exact-name hits — findByName's fuzzy fallback must not count) and a REAL extract
        // lambda with a provable element source (the itemAttr / lambda.* faces read the arms'
        // own structural oracles). The closureParam / fnScopeName faces are pinned too — at
        // corpus they read on SYNTHESIZED heads (a closure-param/input head the synthesis
        // resolver cannot bind), while a PARSE-TIME hit would mean linker drift.
        String source = """
                namespace "test.step486"

                enum Colour:
                    RED
                    BLUE

                type Leg:
                    rate number (1..1)

                type Trade:
                    legs Leg (0..*)
                    colour Colour (1..1)

                func Helper:
                    inputs:
                        t Trade (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        t -> colour = Colour -> RED

                func MyFunc:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (0..*)
                    alias myAlias: trade -> legs
                    set result:
                        trade -> legs extract rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step486-symbol-unresolved.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction myFunc = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "MyFunc".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no MyFunc"));
        RExtractExpr extract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction lambda = assertInstanceOf(RInlineFunction.class, extract.body());

        // absent — no ancestors, no scope, no root element anywhere carries the name.
        RSymbolReference absent = new RSymbolReference();
        absent.setName("mystery");
        absent.attachToWorkspace(ws);
        assertEquals(Optional.of("symbolUnresolved.absent"), adapter.declineReason(absent, ws));

        // nameMissing / dottedName — contract-impossible drift detectors.
        RSymbolReference nameless = new RSymbolReference();
        nameless.attachToWorkspace(ws);
        assertEquals(Optional.of("symbolUnresolved.nameMissing"),
                adapter.declineReason(nameless, ws));
        RSymbolReference dotted = new RSymbolReference();
        dotted.setName("a.b");
        dotted.attachToWorkspace(ws);
        assertEquals(Optional.of("symbolUnresolved.dottedName"),
                adapter.declineReason(dotted, ws));

        // onlyExists — the synthesized only-exists receiver class (checked FIRST after the
        // name guards). The parentless element means no enclosing-function face could fire
        // here anyway — the pin proves the ancestor CLASSIFIES, not the ladder's precedence
        // (the Seat-1 #486 OBS-5 comment recut).
        ROnlyExistsElement onlyExistsElement = new ROnlyExistsElement();
        RSymbolReference underOnlyExists = new RSymbolReference();
        underOnlyExists.setName("myAlias");
        underOnlyExists.setParent(onlyExistsElement);
        assertEquals(Optional.of("symbolUnresolved.synthetic.onlyExists"),
                adapter.declineReason(underOnlyExists, ws));

        // aliasName — RECUT at #502 to the taught behavior (the #501 pin-recut law): the
        // name-match fallback (the arm-1 deferred edge, RESOLVED) now CLAIMS the head — it
        // lowers to the standing IRReference{ALIAS} and the reason reads empty ⟺ lowers.
        RSymbolReference aliasName = new RSymbolReference();
        aliasName.setName("myAlias");
        aliasName.setParent(myFunc);
        IRReference aliasNameLowered = assertInstanceOf(IRReference.class,
                adapter.adapt(aliasName, ws).orElseThrow(() -> new AssertionError(
                        "a name matching an enclosing shortcut must lower since the #502 arm")));
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasNameLowered.referenceKind());
        assertTrue(adapter.declineReason(aliasName, ws).isEmpty(),
                "empty ⟺ lowers: the twin's restated name-match claims it");

        // closureParam — RECUT at #502 to the taught behavior: the synthetic head naming an
        // enclosing binder's declared param now CLAIMS (the resolve-by-name arm); the
        // fnScopeName drift detector below keeps its face.
        RInlineFunction namedLambda = new RInlineFunction();
        namedLambda.paramNames().add("p");
        RSymbolReference closureParam = new RSymbolReference();
        closureParam.setName("p");
        closureParam.setParent(namedLambda);
        IRClosureParam closureLowered = assertInstanceOf(IRClosureParam.class,
                adapter.adapt(closureParam, ws).orElseThrow(() -> new AssertionError(
                        "a head naming an enclosing closure param must lower since the #502"
                                + " arm")));
        assertEquals("p", closureLowered.paramName());
        assertTrue(adapter.declineReason(closureParam, ws).isEmpty(),
                "empty ⟺ lowers: the twin's restated param-match claims it");
        RSymbolReference fnScopeName = new RSymbolReference();
        fnScopeName.setName("trade");
        fnScopeName.setParent(myFunc);
        assertEquals(Optional.of("symbolUnresolved.synthetic.fnScopeName"),
                adapter.declineReason(fnScopeName, ws));

        // itemAttr — RECUT at #524 to the taught behavior (the #502 pin-recut law): the
        // Cat-9-shaped residue (an UNRESOLVED bare name that IS a member of the extract
        // binder's provable element type) now CLAIMS through the EMPTY-symbol bare-item
        // admission — legacy's own handle(RSymbolReference) gate lists the empty symbol
        // in the same ladder — and lowers to the bound twin's FieldAccess over the
        // synthetic item; the reason reads empty ⟺ lowers.
        RSymbolReference itemAttr = new RSymbolReference();
        itemAttr.setName("rate");
        itemAttr.setParent(lambda);
        FieldAccess itemAttrLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(itemAttr, ws).orElseThrow(() -> new AssertionError(
                        "an unresolved member name inside the binder must lower since the"
                                + " #524 empty-symbol admission")));
        assertEquals("rate", itemAttrLowered.feature());
        assertTrue(adapter.declineReason(itemAttr, ws).isEmpty(),
                "empty ⟺ lowers: the twin's empty-symbol leg claims it");

        // lambda.global.type — in-lambda, element type proves, the name is NOT a member but IS
        // a global data type's name (the #451 choice-option pattern's reading).
        RSymbolReference lambdaGlobalType = new RSymbolReference();
        lambdaGlobalType.setName("Trade");
        lambdaGlobalType.setParent(lambda);
        assertEquals(Optional.of("symbolUnresolved.synthetic.lambda.global.type"),
                adapter.declineReason(lambdaGlobalType, ws));

        // lambda.absent — in-lambda, element type proves, the name matches nothing anywhere.
        RSymbolReference lambdaAbsent = new RSymbolReference();
        lambdaAbsent.setName("nonesuch");
        lambdaAbsent.setParent(lambda);
        assertEquals(Optional.of("symbolUnresolved.synthetic.lambda.absent"),
                adapter.declineReason(lambdaAbsent, ws));

        // global.function / global.enum — the visibility-gap faces (exact-name root hits).
        RSymbolReference globalFunction = new RSymbolReference();
        globalFunction.setName("Helper");
        globalFunction.setParent(myFunc);
        assertEquals(Optional.of("symbolUnresolved.synthetic.global.function"),
                adapter.declineReason(globalFunction, ws));
        RSymbolReference globalEnum = new RSymbolReference();
        globalEnum.setName("Colour");
        globalEnum.setParent(myFunc);
        assertEquals(Optional.of("symbolUnresolved.synthetic.global.enum"),
                adapter.declineReason(globalEnum, ws));

        // siblingEnumValue — the #458 Arm B mirror's decode, CLAIMED at #508 (the arm-C
        // node-local requalify's null-symbol ladder-fidelity leg — legacy
        // tryBareEnumComparand's own unresolved shape): the equality SIBLING (the parsed,
        // cache-typed `t -> colour` nav — a name->name shape parses as the DISGUISED-nav
        // REnumValueRef, the #478 class) types to the enumeration carrying the bare name, so
        // the bare operand now LOWERS to the requalified enum constant (the #504 mint —
        // IRReference{ENUM_VALUE} carrying the SIBLING's enum type; the pre-#508 decline pin
        // recut to the acceptance witness, the #486 refine-in-place precedent). The fresh
        // equality only POINTS at the parsed nav (no parsed-node mutation — field sets).
        REnumValueRef colourNav = AstWalker.findAll(ws.files().get(0), REnumValueRef.class).stream()
                .filter(nav -> "colour".equals(nav.valueName())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no colour nav"));
        REqualityExpr freshEquality = new REqualityExpr();
        freshEquality.setLeft(colourNav);
        RSymbolReference siblingEnumValue = new RSymbolReference();
        siblingEnumValue.setName("RED");
        freshEquality.setRight(siblingEnumValue);
        siblingEnumValue.setParent(freshEquality);
        assertEquals(Optional.empty(), adapter.declineReason(siblingEnumValue, ws),
                "the #508 arm-C eq-seat requalify claims — declineReason reads empty");
        IRExpr requalified = adapter.adapt(siblingEnumValue, ws).orElseThrow(
                () -> new AssertionError("the #508 arm-C eq-seat requalify must lower"));
        IRReference requalifiedRef = assertInstanceOf(IRReference.class, requalified);
        assertEquals("RED", requalifiedRef.target());
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, requalifiedRef.referenceKind());
        // itemAttrMeta needs a [metadata]-annotated member on a PARSED provable element type —
        // deliberately unpinned here (the corpus probe reads the face; a fixture would hand-mutate
        // parsed attributes, which the frozen-tree contract forbids).
    }

    @Test
    void argNavRevivalAdmitsTheDecodedFacets() {
        // #488 — the argNav DECODE witness: the flat reason token refined IN PLACE to
        // argNav.[meta.|typeMissing.]<root>.<depth>[.multi] at its single seat (argToken).
        // Every argNav
        // occurrence is a navigation the nav arms ALREADY LOWER (the token is minted off the
        // lowered FieldAccess — adapt returned non-empty), so the facet decodes the
        // argument-position admission an L-042-class arm would compose: the ROOT names the
        // render family (param — the MapperS.of(name) root; item/synItem — the explicit/elided
        // filter-extract implicit, the lambda-binder renders), the DEPTH sizes the chain
        // (hop1/hop2/deep ≥3 — the same chain machinery at any depth), .multi marks the
        // accumulated-MULTI MapperC sub-population, and the meta./typeMissing. prefixes are
        // drift detectors (the nav arms decline meta features — metaFeature/itemMetaSourced —
        // so a meta-typed hop can never have lowered; typeMissing. reads the type engine's
        // NON-NULL MISSING sentinel — getInferredType is getOrDefault(expr, MISSING), so a
        // node the fixed point never saw carries MISSING, not null; the Seat-1 MF-1 recut
        // made that face LIVE via isMissing(), and the hand-built hop in
        // declineReasonNamesSymbolCallAndFeatureCallGates pins it on the exact shape that
        // disclosed the sentinel). These PARSED hops all carry real fixed-point types, so —
        // since the #489 isNavCallArg revival CLAIMED the decoded population — the pins read
        // the ADMISSION (declineReason empty + the lowered FieldAccess arg at its decoded
        // root/depth), not the retired clean spellings. Each callee is named per case so the
        // call nodes are uniquely findable in the parsed model.
        String source = """
                namespace "test.step488"

                type Sub:
                    x number (1..1)

                type Leg:
                    rate number (1..1)
                    sub Sub (1..1)

                type Trade:
                    legs Leg (0..*)
                    leg Leg (1..1)
                    amount number (1..1)

                func CParam:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CChain:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CDeep:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CMulti:
                    inputs:
                        vs Leg (0..*)
                    output:
                        out number (1..1)
                    set out: 1

                func CItem:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CElided:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func FnDirect:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (0..*)
                    set result:
                        [
                            CParam(trade -> amount),
                            CChain(trade -> leg -> rate),
                            CDeep(trade -> leg -> sub -> x),
                            CMulti(trade -> legs)
                        ]

                func FnItem:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CItem(item -> rate)

                func FnElided:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CElided(rate)
                """;
        RModel model = AstBuilder.buildFromString(source, "step488-argnav-decode.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // #489 (the L-042 REVIVAL): every decoded facet family the #488 pins read as a DECLINE now
        // lowers WHOLE — isNavCallArg admits the meta-free fixed-point-typed chains at their decoded
        // roots, so declineReason reads EMPTY (the empty ⇔ lowers mirror) and the IRApply carries the
        // lowered FieldAccess argument. The depth ladder and the .multi face ride the SAME admission
        // (the render is the same chain machinery at any depth; the accessor is the per-param
        // asMulti decision — facet tailMulti — regardless of the arg's accumulated cardinality).
        assertNavArgClaim(ws, "CParam", IRVariable.VariableKind.PARAM, 1);
        assertNavArgClaim(ws, "CChain", IRVariable.VariableKind.PARAM, 2);
        assertNavArgClaim(ws, "CDeep", IRVariable.VariableKind.PARAM, 3);
        assertNavArgClaim(ws, "CMulti", IRVariable.VariableKind.PARAM, 1); // accumulated-MULTI — the .multi face admits

        // the filter/extract-bound implicit roots — the explicit item and the elided bare attr claim too.
        assertNavArgClaim(ws, "CItem", IRVariable.VariableKind.USER_ITEM, 1);
        assertNavArgClaim(ws, "CElided", IRVariable.VariableKind.SYNTHETIC_ITEM, 1);
        // The DECLINE faces stay the drift detectors — meta. and alias roots are unreachable
        // through a real lowering (the nav arms decline meta features and alias receivers before
        // any arg lowers), closureParam/aliasVar/letBinder have no nav-arg construction in this
        // fixture family, and the typeMissing. face keeps its live behavioral pin in
        // declineReasonNamesSymbolCallAndFeatureCallGates (the hand-built MISSING-sentinel
        // chain still DECLINES — isNavCallArg rejects an untyped hop); since #491 the ROOT-typed
        // meta./typeMissing. states are additionally pinned DIRECTLY by
        // argNavRootTypeBeltDeclinesMetaAndMissingRoots (the hand-constructed belt pin); the
        // corpus probe reads the post-claim distribution (every admitted face ZERO).
    }

    /**
     * #489: the L-042-revival admission lock — the named call lowers WHOLE (declineReason empty, the
     * mirror law), the {@link IRApply} carries the lowered {@link FieldAccess} argument, and the
     * chain's depth + root match the #488 decode facet the shape used to read.
     */
    private void assertNavArgClaim(RWorkspace ws, String calleeName, IRVariable.VariableKind expectedRoot,
                                   int expectedHops) {
        RSymbolReference callNode = call(ws, calleeName);
        assertEquals(Optional.empty(), adapter.declineReason(callNode, ws),
                calleeName + ": the nav-arg call lowers whole — declineReason empty (the mirror law)");
        IRApply lowered = assertInstanceOf(IRApply.class, adapt(callNode, ws),
                calleeName + ": the call lowers to IRApply");
        FieldAccess nav = assertInstanceOf(FieldAccess.class, lowered.args().get(0),
                calleeName + ": the argument is the lowered FieldAccess chain");
        int hops = 0;
        IRExpr walk = nav;
        while (walk instanceof FieldAccess hop) {
            hops++;
            walk = hop.receiver();
        }
        assertEquals(expectedHops, hops, calleeName + ": the chain depth");
        IRVariable root = assertInstanceOf(IRVariable.class, walk, calleeName + ": the chain root is a variable");
        assertEquals(expectedRoot, root.variableKind(), calleeName + ": the decoded root family");
    }

    /** The unique args-bearing call reference to the named callee in the parsed fixture. */
    private RSymbolReference call(RWorkspace ws, String calleeName) {
        List<RSymbolReference> calls = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream()
                .filter(r -> calleeName.equals(r.name()) && !r.args().isEmpty())
                .toList();
        assertEquals(1, calls.size(), "exactly one " + calleeName + " call in the fixture");
        return calls.get(0);
    }

    @Test
    void argNavRootTypeBeltDeclinesMetaAndMissingRoots() {
        // #491 — the #489 Seat-1 OBS-2 ROOT-TYPE BELT pin: argNavFacet's hop walk reads hop
        // RESULT types only, and a meta/missing-typed ROOT under an otherwise-ADMITTING root
        // kind was excluded only INDIRECTLY (adaptSymbolReference declines meta inputs, so a
        // meta-rooted chain can never LOWER). That indirection is also why this pin
        // hand-CONSTRUCTS the lowered IR triple and calls the package-private classifier
        // directly: the belt's distinct state — clean-typed hops over a dirty root — is
        // unreachable through adapt, and a hand-built RAW chain stamps the MISSING sentinel on
        // every hop, masking the root's contribution behind the existing sentinel pin. The
        // three-way contrast holds everything constant but the root's type: meta → the meta.
        // drift face (the belt fires), MISSING → the typeMissing. drift face (the belt fires),
        // clean → the belt does NOT fire and the face falls through to the type-agreement
        // gate's typeGap. (the typeCall-less hand-built param resolves MISSING), proving the
        // belt is the sole discriminator across the triple.
        RModel model = AstBuilder.buildFromString("""
                namespace "test.step491"

                type Marker:
                    flag boolean (1..1)
                """, "step491-rootbelt.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RAttribute param = scalarParam("q");
        RExpression rawArg = intLiteral(1); // param roots never consult the raw arg
        RMetaAnnotatedType clean = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        java.util.function.Function<RMetaAnnotatedType, FieldAccess> navOver = rootType ->
                new FieldAccess(
                        new IRVariable("leg", IRVariable.VariableKind.PARAM, NodeId.ROOT, rootType,
                                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE),
                        "rate", NodeId.ROOT, clean, ExpressionCardinality.SINGLE,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        assertEquals("meta.param.hop1",
                ExpressionToIRAdapter.argNavFacet(
                        navOver.apply(RMetaAnnotatedType.withMeta(
                                RNumberType.unconstrained(), List.of("reference"))),
                        rawArg, param, ws),
                "a meta-typed admitting root declines on the drift face DIRECTLY (the belt)");
        assertNull(
                ExpressionToIRAdapter.argNavFacet(navOver.apply(RMetaAnnotatedType.MISSING),
                        rawArg, param, ws),
                "RETENSED #527: a MISSING-typed PARAM root ADMITS (the missing-hop serve class"
                        + " — the belt still reads the sentinel, the admit routes it to the"
                        + " compiler's argNavMissingHop whole-legacy serve; the meta leg above"
                        + " keeps the belt's decline live)");
        assertEquals("typeGap.param.hop1.g:number_null",
                ExpressionToIRAdapter.argNavFacet(navOver.apply(clean), rawArg, param, ws),
                "the clean-rooted twin passes the belt — its face is the type-agreement gate's"
                        + " (the hand-built param resolves MISSING, spelled null by the #514"
                        + " gap-class suffix), so the belt is the sole"
                        + " discriminator across the triple");
    }

    @Test
    void aliasOperandRevivalAdmitsProvenBodiesAtEqualityAndExistence() {
        // #491 — the alias-operand teach (the L-035 arithmetic-operand law extended to the
        // EQUALITY and EXISTENCE gates): a lowered ALIAS operand admits when the shared
        // aliasOperandBodyFacet walk proves the shortcut BODY typed and meta-free (the engine
        // types a shortcut's DEFINING expression even though the shortcut REFERENCE reads
        // MISSING — legacy resolveOperandJavaType's own note) and, at equality, the sibling is
        // not a bare enum-value reference (legacy resolves the enum qualifier through the
        // sibling's type; the emitter defers on an unresolvable sibling class — the decoded
        // operandAlias.clean.enumSibling follow-up face). The render is legacy's bare
        // context-free aliasName(inputs) — the null-typed alias invocation defeats every
        // equality-seat coercion lever, and the existence seat carries none (both read at
        // source). RECUT at #510: the comparison and logical seats — #491's decline pins —
        // now ADMIT every alias face through the whole-oracle shape-leg routing (arms A4/A4c;
        // the acceptance positives below are the recut locks).
        String source = """
                namespace "test.step491b"

                enum SideEnum:
                    BUY
                    SELL

                type Leg:
                    rate number (1..1)
                    side SideEnum (1..1)

                type Trade:
                    leg Leg (1..1)
                    amount number (1..1)

                func FnEq:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias legRate: trade -> leg -> rate
                    set out:
                        legRate = trade -> amount

                func FnExists:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias legRate2: trade -> leg -> rate
                    set out:
                        legRate2 exists

                func FnEnumSib:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias legSide: trade -> leg -> side
                    set out:
                        legSide = SideEnum -> BUY

                func FnCmp:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias legRate3: trade -> leg -> rate
                    set out:
                        legRate3 > trade -> amount

                func FnLogical:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias sideOk: trade -> leg -> side = SideEnum -> BUY
                    set out:
                        sideOk and trade -> amount exists

                func FnBroken:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out boolean (1..1)
                    alias broken: mysteryField
                    set out:
                        broken exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step491b-alias-operand.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // The EQUALITY admit: proven body + non-enum sibling — the claim lowers WHOLE
        // (declineReason empty ⇔ lowers, the mirror law) with the alias operand lowered in place.
        REqualityExpr eq = firstUnder(ws, "FnEq", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eq, ws),
                "a proven-body alias equality operand admits (the L-035 law at the equality gate)");
        BinaryOp loweredEq = assertInstanceOf(BinaryOp.class, adapt(eq, ws),
                "the admitted equality lowers to BinaryOp");
        IRReference aliasOperand = assertInstanceOf(IRReference.class, loweredEq.left(),
                "the alias operand lowers in place");
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasOperand.referenceKind(),
                "the lowered operand is the ALIAS reference");

        // The EXISTENCE admit — the seat legacy compiles bare (no retype/strip levers).
        RExistenceExpr exists = firstUnder(ws, "FnExists", RExistenceExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(exists, ws),
                "a proven-body alias existence operand admits");

        // RECUT at #503 (the pin-recut law — 'the named follow-up' taught): the enumSibling
        // carve-out is RETIRED — the alias node now carries the BODY-channel type, so the
        // emitter's enumOperandRequalifies compares the real enum classes and the pair
        // ADMITS at the adapter (a divergent/unresolvable pair still declines at the
        // emitter's requalify gate, byte-safely).
        REqualityExpr enumEq = firstUnder(ws, "FnEnumSib", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(enumEq, ws),
                "a proven-body alias vs enum-value pair admits since the #503 arm — the"
                        + " requalifier owns the residual gate");

        // RECUT at #510 (arm-A4c — the refine-in-place precedent): the comparison-seat alias
        // decline flipped to an admission. The #372 comparisonIntWiden lever — a numeric-alias
        // inequality firing a widen hop on its sibling — was the NATIVE render's decline
        // reason; the containing root now renders WHOLE through the oracle-root comparison
        // serve (the containsOracleLeaf comparison-with-alias shape leg), so the widen is
        // legacy's own inside the literal super.visitComparison line and EVERY alias body
        // admits.
        RComparisonExpr cmp = firstUnder(ws, "FnCmp", RComparisonExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(cmp, ws),
                "a comparison alias operand admits since the #510 arm-A4c — the oracle serve"
                        + " owns the widen lever");
        assertInstanceOf(BinaryOp.class, adapt(cmp, ws),
                "the admitted comparison lowers to BinaryOp");

        // RECUT at #510 (arm-A4): the logical FLAT face flipped to an admission — legacy
        // LogicalHandler coerces the alias ComparisonResult.ofNullSafe(<render>) inside its
        // own composition for ANY body (the isCoercedBooleanOperand shape leg routes the
        // root whole-oracle), so the #491 body facets are irrelevant at this seat.
        RLogicalExpr logical = firstUnder(ws, "FnLogical", RLogicalExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(logical, ws),
                "a logical alias operand admits since the #510 arm-A4 — the ofNullSafe"
                        + " coercion is legacy's own inside the oracle serve");
        assertInstanceOf(BinaryOp.class, adapt(logical, ws),
                "the admitted logical lowers to BinaryOp");

        // bodyMissing: an unresolvable body declines prove-or-decline (#514: the body's own
        // class rides as the in-place re-decode suffix; #518: the RSymbolReference class one
        // key deeper — the symbol-channel walk's own stop reason, here a bare name off the
        // enclosing inputs).
        RExistenceExpr broken = firstUnder(ws, "FnBroken", RExistenceExpr.class);
        assertEquals(Optional.of("operandAlias.bodyMissing.body:RSymbolReference.bareNotInput"),
                adapter.declineReason(broken, ws),
                "an untypeable shortcut body declines on the bodyMissing face");
    }

    /** The first {@code klass} expression under the named function in the parsed fixture. */
    private static <T extends RNode> T firstUnder(RWorkspace ws, String funcName, Class<T> klass) {
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> funcName.equals(f.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("fixture function absent: " + funcName));
        List<T> nodes = AstWalker.findAll(fn, klass);
        assertFalse(nodes.isEmpty(), funcName + " carries a " + klass.getSimpleName());
        return nodes.get(0);
    }

    @Test
    void declineReasonNoAdaptArmAndEquivalenceSweep() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        RAttribute a = scalarParam("a");
        RAttribute b = scalarParam("b");
        functionWith(ws, a, b);
        // The fixture family recut at #513 (contains → join) and again at #515: the untargeted
        // close armed `join`, so the lock pins the still-unarmed cardinality-check (with
        // `reduce` the last two noAdaptArm families).
        RCardinalityCheckExpr check = new RCardinalityCheckExpr();
        check.setArgument(paramRef("a", a, ws));
        check.setOp(CardCheckOp.CHOICE);
        check.setNecessity(Necessity.REQUIRED);
        check.attributes().add("x");
        check.attributes().add("y");
        check.attachToWorkspace(ws);
        assertEquals(Optional.of("noAdaptArm"), adapter.declineReason(check, ws),
                "an un-armed family names noAdaptArm (the #515-recut fixture family)");

        REnumeration en = new REnumeration();
        en.setName("FooEnum");
        REnumValue ev = new REnumValue();
        ev.setName("Cash");
        ev.setParent(en);
        en.values().add(ev);
        en.attachToWorkspace(ws);
        ev.attachToWorkspace(ws);
        REnumValueRef disguised = new REnumValueRef();
        disguised.setEnumName("foo");
        disguised.setValueName("bar");
        disguised.attachToWorkspace(ws);

        // The equivalence sweep — reason present ⟺ adapt empty, never "unattributed", across
        // claiming AND declining shapes (the mirror-fidelity lock the probe re-proves at population).
        List<RExpression> fixtures = List.of(
                intLiteral(7),                                   // lowers (literal leaf)
                paramRef("a", a, ws),                            // lowers (wired scalar param)
                enumValueRef("FooEnum", "Cash", en, ev, ws),     // lowers (genuine enum value)
                check,                                           // declines — noAdaptArm
                disguised);                                      // declines — noResolutionChannel
        for (RExpression fixture : fixtures) {
            Optional<String> reason = adapter.declineReason(fixture, ws);
            assertEquals(adapter.adapt(fixture, ws).isEmpty(), reason.isPresent(),
                    "empty ⟺ lowers must hold for " + fixture.getClass().getSimpleName());
            assertTrue(reason.map(r -> !r.equals("unattributed")).orElse(true),
                    "no sweep fixture may classify as unattributed — the mirror residue is a defect");
        }
    }

    @Test
    void syntheticFilterExtractItemReceiverRetypesAndLowers() {
        // #479 — the filter/extract synthetic-item receiver teach: a SYNTHETIC implicit item (the
        // parser's elided-operand materialisation AND legacy's own synthesized in-lambda receivers
        // — the re-entrant visitFeatureCall claims) ALWAYS reads MISSING through the node-keyed
        // type cache (the #479 witness read typeMissing on every synthetic bucket at population),
        // so the arm rebuilds the lowered receiver with the binder SOURCE's element type — the
        // engine's own Cat-8 law applied at the adapter boundary (the #478 retype precedent). The
        // fixture parses a REAL extract (the fixed-point walk types the source) and hand-builds
        // the synthesized-equivalent nav into the parsed lambda — exactly the shape legacy's
        // synthesizeImplicitItemNavigation builds on the re-entrant path.
        String source = """
                namespace "test.step479"

                type Leg:
                    rate number (1..1)

                func MyFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step479-synitem.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction lambda = assertInstanceOf(RInlineFunction.class, extract.body());
        RSymbolReference bareRate = assertInstanceOf(RSymbolReference.class, lambda.body());
        RAttribute rate = assertInstanceOf(RAttribute.class, bareRate.symbol().orElseThrow());

        RImplicitVariable synItem = new RImplicitVariable();
        synItem.setSynthetic(true);
        synItem.setParent(lambda);
        RFeatureCall nav = new RFeatureCall();
        nav.setReceiver(synItem);
        nav.setFeatureName("rate");
        nav.setResolvedFeature(rate);
        nav.setParent(lambda);

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(nav, ws));
        IRVariable receiver = assertInstanceOf(IRVariable.class, fa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, receiver.variableKind());
        assertEquals("Leg", receiver.type().type().name(),
                "the receiver rebuilds with the binder source's element type (the Cat-8 law)");
        assertEquals(ExpressionCardinality.SINGLE, receiver.cardinality(),
                "an item is one element regardless of the source's MULTI cardinality");
        assertTrue(fa.type() != null && !fa.type().isMissing(),
                "the hop's type sources from the feature's ATTRIBUTE channel — a synthesized"
                        + " re-entrant node is invisible to the node-keyed cache (the #478 retype law)");
        assertTrue(adapter.declineReason(nav, ws).isEmpty(),
                "empty ⟺ lowers: the mirror restates the retype admission with the arm's own helper");

        // RECUT at #504 (the pin-recut law): the SAME nav shape OUTSIDE a filter/extract
        // binder — no binder source to retype from — now MINTS the DISTINCT shallow
        // IRSynItemNav (the arm-B teach: the un-retypeable residue lowers unconditionally;
        // the render is the containing root's oracle-root serve — legacy's own binder-scope
        // machinery, byte-identical BY IDENTITY). The retype-PROVEN slice above keeps its
        // richer FieldAccess — the mint is the residue's own seat.
        RFunction fn = AstWalker.findFirst(ws.files().get(0), RFunction.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFunction"));
        RImplicitVariable unboundItem = new RImplicitVariable();
        unboundItem.setSynthetic(true);
        RFeatureCall unboundNav = new RFeatureCall();
        unboundNav.setReceiver(unboundItem);
        unboundNav.setFeatureName("rate");
        unboundNav.setResolvedFeature(rate);
        unboundItem.setParent(unboundNav);
        unboundNav.setParent(fn);
        IRSynItemNav minted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(unboundNav, ws).orElseThrow(() -> new AssertionError(
                        "an un-retypeable synthetic-item nav must mint since the #504 arm")),
                "the residue lowers to the shallow kind");
        assertEquals("rate", minted.featureName(),
                "the resolved feature's name is the neutral fact the kind carries");
        assertTrue(adapter.declineReason(unboundNav, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the mint's unconditional claim");
    }

    @Test
    void bareAttrAndChainArmsLowerTheItemNavEquivalents() {
        // #480 — the bare-attr + chain arms off the #479-taught base: a bare in-lambda ITEM-feature
        // read (Cat 9) and a bound-chain disguise (Cat 10) lower via the EXACT equivalents legacy
        // synthesizeImplicitItemNavigation/-Chain build, delegated through adaptFeatureCall (the
        // #479 synthetic-item retype at the base + the attribute-channel hop typing), rebuilt at
        // the cache boundary with the RAW node's cached type and the RAW node's source range on
        // the whole spine (the emitter's #480 correlation key).
        String bareSource = """
                namespace "test.step480"

                type Inner:
                    x number (1..1)

                type Leg:
                    rate number (1..1)
                    inner Inner (1..1)

                func BareFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract rate
                """;
        RModel bareModel = AstBuilder.buildFromString(bareSource, "step480-bare.rosetta");
        RWorkspace bws = RWorkspace.build(List.of(bareModel)).workspace();
        RExtractExpr bareExtract = AstWalker.findFirst(bws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction bareLambda = assertInstanceOf(RInlineFunction.class, bareExtract.body());
        RSymbolReference bareRate = assertInstanceOf(RSymbolReference.class, bareLambda.body());

        FieldAccess bareFa = assertInstanceOf(FieldAccess.class, adapt(bareRate, bws));
        IRVariable bareRecv = assertInstanceOf(IRVariable.class, bareFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, bareRecv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Leg", bareRecv.type().type().name(),
                "the base retypes with the binder source's element type (the Cat-8 law)");
        assertEquals(ExpressionCardinality.SINGLE, bareRecv.cardinality());
        assertEquals(bareRate.sourceRange(), bareRecv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertEquals(bareRate.sourceRange(), bareFa.sourceRange());
        assertTrue(bareFa.type() != null && !bareFa.type().isMissing(),
                "the hop rebuilds with the RAW node's cached type (the cache-boundary law)");
        assertTrue(adapter.declineReason(bareRate, bws).isEmpty(),
                "empty ⟺ lowers: the mirror twin restates the arm's admission");

        // The residue: the SAME bare read outside any lambda declines with the composed token
        // (the arm's first failing gate dot-suffixed onto the channel — the #478 pattern).
        RFunction bareFn = AstWalker.findFirst(bws.files().get(0), RFunction.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFunction"));
        RAttribute rateAttr = assertInstanceOf(RAttribute.class, bareRate.symbol().orElseThrow());
        RSymbolReference outsideRef = new RSymbolReference();
        outsideRef.setName("rate");
        outsideRef.setResolvedSymbol(rateAttr);
        outsideRef.setParent(bareFn);
        // #528 arm-5 RETENSE: the residue no longer declines — the L-113 conversion mints the
        // shallow kind for this exact census family (noFilterExtractBinder). The lock's real
        // invariant stands: the bare-attr ARM did not claim it (no item-nav equivalent could be
        // built), so the lowering is the SHALLOW serve kind, never the FieldAccess the arm
        // produces for a properly-bound read.
        IRExpr outsideLowered = adapter.adapt(outsideRef, bws).orElseThrow(
                () -> new AssertionError("the #528 mint claims the un-buildable residue"));
        assertInstanceOf(IRImplicitAttrNav.class, outsideLowered,
                "the shallow synthesized-receiver kind — NOT the arm's item-nav equivalent");
        assertEquals(Optional.empty(), adapter.declineReason(outsideRef, bws),
                "the census family's face retires with the mint");

        String chainSource = """
                namespace "test.step480"

                type Inner:
                    x number (1..1)

                type Leg:
                    rate number (1..1)
                    inner Inner (1..1)

                func ChainFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract inner -> x
                """;
        RModel chainModel = AstBuilder.buildFromString(chainSource, "step480-chain.rosetta");
        RWorkspace cws = RWorkspace.build(List.of(chainModel)).workspace();
        REnumValueRef chainNav = AstWalker.findFirst(cws.files().get(0), REnumValueRef.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no REnumValueRef"));

        FieldAccess chainTop = assertInstanceOf(FieldAccess.class, adapt(chainNav, cws));
        assertEquals("x", chainTop.feature(), "the top hop is the bound leaf");
        FieldAccess chainHead = assertInstanceOf(FieldAccess.class, chainTop.receiver());
        assertEquals("inner", chainHead.feature(), "the inner hop is the bound head");
        IRVariable chainBase = assertInstanceOf(IRVariable.class, chainHead.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, chainBase.variableKind());
        assertEquals("Leg", chainBase.type().type().name(),
                "the chain's base retypes with the binder source's element type");
        assertTrue(chainHead.type() != null && !chainHead.type().isMissing(),
                "the head hop types off the feature's ATTRIBUTE channel (the #478 retype law)");
        assertTrue(chainTop.type() != null && !chainTop.type().isMissing(),
                "the top hop rebuilds with the RAW node's cached type");
        assertEquals(chainNav.sourceRange(), chainBase.sourceRange(),
                "the whole spine re-stamps the RAW node's range — the emitter's correlation key");
        assertEquals(chainNav.sourceRange(), chainHead.sourceRange());
        assertEquals(chainNav.sourceRange(), chainTop.sourceRange());
        assertTrue(adapter.declineReason(chainNav, cws).isEmpty(),
                "empty ⟺ lowers: the chain mirror twin restates the arm's admission");

        // The #480 MF-1 precedence, #501-recut: a chain-bound node ALSO carrying resolvedSymbol
        // (the documented head-name collision shape — an item attribute and a workspace
        // func/rule sharing the name) takes legacy's rule/function-receiver channel — since the
        // #501 C2 teach the arm CLAIMS the class (IRSymbolNav, the oracle-root serve) whenever
        // the node types; THIS hand-built node carries no workspace typing, so it exercises the
        // arm's typing residue face (the composed exit — prove-or-decline).
        RFunction collidingFn = calleeFunction(cws, "inner",
                List.of(scalarParam("p")), scalarParam("out"));
        REnumValueRef collisionChain = new REnumValueRef();
        collisionChain.setEnumName("inner");
        collisionChain.setValueName("x");
        collisionChain.setResolvedAttributeChain(chainNav.resolvedAttributeChain().orElseThrow());
        collisionChain.setResolvedSymbol(collidingFn);
        collisionChain.setParent(chainNav.parent()); // the same lambda seat — only the symbol differs
        assertTrue(adapter.adapt(collisionChain, cws).isEmpty(),
                "an UNTYPED resolvedSymbol-carrying chain stays legacy's (the arm's typing gate)");
        assertEquals("attributeChain.headSymbolNav.untyped",
                adapter.declineReason(collisionChain, cws).orElseThrow(),
                "the twin names the arm's own typing gate (the #501 composed residue face)");
    }

    @Test
    void elidedPipeWideningLowersThenPipedSources() {
        // #481 — the elidedImplicit widening: a filter/extract whose grammar-elided argument
        // binds through the then body's implicit wrapper to the PIPE resolves its element form
        // AND its type on the enclosing then's ARGUMENT (resolveElidedPipedSource — the value
        // identity through legacy handle(RImplicitVariable)'s thenBody face), so the #479 retype
        // and the #480 arms see THROUGH the grammar-elided piped implicit. The parsed carrier:
        // the bare `rate` inside `legs then filter rate exists` adapts whole via the bare-attr
        // arm — the structural derivation resolves Leg through the pipe, the equivalent's
        // synthetic base retypes off the RESOLVED argument's cached type, and the mirror twin
        // agrees (empty ⟺ lowers).
        String source = """
                namespace "test.step481"

                type Leg:
                    rate number (1..1)

                func PipeFn:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        legs then filter rate exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step481-pipe.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFilterExpr filter = AstWalker.findFirst(ws.files().get(0), RFilterExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFilterExpr"));
        RImplicitVariable elided = assertInstanceOf(RImplicitVariable.class, filter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        assertTrue(elided.isSynthetic(), "the elided argument is the parser-synthesized implicit");
        RExistenceExpr exists = assertInstanceOf(RExistenceExpr.class, filter.body().body());
        RSymbolReference bareRate = assertInstanceOf(RSymbolReference.class, exists.argument());

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(bareRate, ws));
        IRVariable recv = assertInstanceOf(IRVariable.class, fa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, recv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Leg", recv.type().type().name(),
                "the base retypes with the PIPE ARGUMENT's element type — the widening's read"
                        + " (the elided node itself is cache-invisible)");
        assertEquals(ExpressionCardinality.SINGLE, recv.cardinality());
        assertEquals(bareRate.sourceRange(), recv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(fa.type() != null && !fa.type().isMissing(),
                "the hop rebuilds with the RAW node's cached type (the cache-boundary law)");
        assertTrue(adapter.declineReason(bareRate, ws).isEmpty(),
                "empty ⟺ lowers: the twin's derivation widened with the arm's");

        // The residue: the SAME synthetic-item nav whose elided source binds to a NON-then
        // implicit binder (the extract item piggy-back face) stays declined with the standing
        // token — the resolver admits ONLY the then face, prove-or-decline; the piggy-back /
        // rule-input / nested-pipe faces are the #481 witness's residue map, all still legacy's.
        RFunction fn = AstWalker.findFirst(ws.files().get(0), RFunction.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFunction"));
        RAttribute rateAttr = assertInstanceOf(RAttribute.class, bareRate.symbol().orElseThrow());
        RImplicitVariable nonThenItem = new RImplicitVariable();
        nonThenItem.setSynthetic(true);
        RFeatureCall nonThenNav = new RFeatureCall();
        nonThenNav.setReceiver(nonThenItem);
        nonThenNav.setFeatureName("rate");
        nonThenNav.setResolvedFeature(rateAttr);
        nonThenItem.setParent(nonThenNav);
        RInlineFunction innerFn = new RInlineFunction();
        innerFn.setBody(nonThenNav);
        nonThenNav.setParent(innerFn);
        RImplicitVariable nonThenElided = new RImplicitVariable();
        nonThenElided.setSynthetic(true);
        RFilterExpr innerFilter = new RFilterExpr();
        innerFilter.setArgument(nonThenElided);
        nonThenElided.setParent(innerFilter);
        innerFilter.setBody(innerFn);
        innerFn.setParent(innerFilter);
        RInlineFunction outerFn = new RInlineFunction();
        outerFn.setBody(innerFilter);
        innerFilter.setParent(outerFn);
        RExtractExpr outerExtract = new RExtractExpr();
        RSymbolReference outerSource = new RSymbolReference();
        outerSource.setName("legs");
        outerExtract.setArgument(outerSource);
        outerSource.setParent(outerExtract);
        outerExtract.setBody(outerFn);
        outerFn.setParent(outerExtract);
        outerExtract.setParent(fn);
        // RECUT at #504 (the pin-recut law): the extract-bound elided residue now MINTS the
        // shallow IRSynItemNav (the arm-B teach — the elided-pipe widening's decline face was
        // exactly the un-retypeable class the mint claims; the #481 then-face positive above
        // keeps its richer FieldAccess).
        IRSynItemNav nonThenMinted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(nonThenNav, ws).orElseThrow(() -> new AssertionError(
                        "an extract-bound elided-source nav must mint since the #504 arm")),
                "the un-retypeable residue lowers to the shallow kind");
        assertEquals("rate", nonThenMinted.featureName(),
                "the resolved feature's name is the neutral fact");
        assertTrue(adapter.declineReason(nonThenNav, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the mint");
    }

    @Test
    void nestedThenPipeWideningLowersPipedPipeSources() {
        // #482 — the nested-thenPipe widening: when the resolved pipe argument is ITSELF an
        // RThenExpr (the #481 residue map's dominant class), the element form of `A then B` is
        // B's BODY result — the pipe's value IS the body's value on the piped input (the #481
        // value identity one structural seat deeper) — so the allowlist and the structural
        // derivation recurse into the BODY expression while the type read stays on the then
        // node's own cached type (a parsed node; no new type channel). The parsed carrier
        // nests left-associatively: the bare `qty` inside
        // `legs then filter rate exists then filter qty exists` — the OUTER filter's elided
        // argument resolves to the INNER then, whose body (the element-preserving filter over
        // the provable bare input `legs`) proves and derives Leg through both pipes; the claim
        // adapts whole via the bare-attr arm off the #479-taught synthetic base, and the
        // mirror twin agrees (empty ⟺ lowers).
        String source = """
                namespace "test.step482"

                type Leg:
                    rate number (1..1)
                    qty number (1..1)

                func NestedPipeFn:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        legs then filter rate exists then filter qty exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step482-nested-pipe.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RThenExpr outerThen = AstWalker.findFirst(ws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RThenExpr.class, outerThen.argument(),
                "the left-associative chain must nest the inner pipe as the outer ARGUMENT");
        RFilterExpr outerFilter = assertInstanceOf(RFilterExpr.class,
                outerThen.body().orElseThrow().body(),
                "the outer then body must wrap the outer filter");
        RImplicitVariable elided = assertInstanceOf(RImplicitVariable.class, outerFilter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        assertTrue(elided.isSynthetic(), "the elided argument is the parser-synthesized implicit");
        RExistenceExpr exists = assertInstanceOf(RExistenceExpr.class, outerFilter.body().body());
        RSymbolReference bareQty = assertInstanceOf(RSymbolReference.class, exists.argument());

        FieldAccess fa = assertInstanceOf(FieldAccess.class, adapt(bareQty, ws));
        IRVariable recv = assertInstanceOf(IRVariable.class, fa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, recv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Leg", recv.type().type().name(),
                "the base retypes with the NESTED PIPE's element type — the then node's own"
                        + " cached type through the widened read");
        assertEquals(ExpressionCardinality.SINGLE, recv.cardinality());
        assertEquals(bareQty.sourceRange(), recv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(fa.type() != null && !fa.type().isMissing(),
                "the hop rebuilds with the RAW node's cached type (the cache-boundary law)");
        assertTrue(adapter.declineReason(bareQty, ws).isEmpty(),
                "empty ⟺ lowers: the twin's derivation widened with the arm's");

        // The residue, #501-recut: the IDENTITY-extract-bodied nested pipe's ELEMENT now
        // RESOLVES — the #501 nested-binder recursion (the srcElem widening) sees the written
        // `item` body through its own binder to the extract's piped source (the value identity
        // one binder out — exactly the "future widening" the pre-#501 comment named), so the
        // sourceElementUnresolved face is GONE and the decline moves DOWNSTREAM to the retype
        // slice's own gate: the equivalent's synthetic base sits outside the taught
        // filter/extract retype slice on this doubly-nested shape (the #479 residue class —
        // still prove-or-decline, the honest next gate).
        String extractSource = """
                namespace "test.step482b"

                type Leg:
                    rate number (1..1)
                    qty number (1..1)

                func ExtractPipeFn:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        legs then extract item then filter qty exists
                """;
        RModel extractModel = AstBuilder.buildFromString(extractSource, "step482-extract-pipe.rosetta");
        RWorkspace ews = RWorkspace.build(List.of(extractModel)).workspace();
        RThenExpr extractOuterThen = AstWalker.findFirst(ews.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RThenExpr.class, extractOuterThen.argument(),
                "the left-associative chain must nest the extract-bodied then as the outer ARGUMENT");
        RFilterExpr extractOuterFilter = assertInstanceOf(RFilterExpr.class,
                extractOuterThen.body().orElseThrow().body());
        RExistenceExpr extractExists = assertInstanceOf(RExistenceExpr.class,
                extractOuterFilter.body().body());
        RSymbolReference extractBareQty = assertInstanceOf(RSymbolReference.class,
                extractExists.argument());
        // RECUT at #502 (the #501 pin-recut law's next step — this residue was "the honest
        // next gate" and the #502 arm taught it): the equivalent's synthetic base now RETYPES
        // through the widened binder legs + the structural type read (the retype's
        // still-implicit branch reads sourceElementDataType, which resolves the doubly-nested
        // element exactly where the #501 recursion proved it), so the claim adapts whole.
        FieldAccess extractFa = assertInstanceOf(FieldAccess.class,
                adapter.adapt(extractBareQty, ews).orElseThrow(() -> new AssertionError(
                        "the doubly-nested pipe's bare attr must lower since the #502 retype"
                                + " widenings")),
                "the claim adapts whole through the widened retype");
        IRVariable extractRecv = assertInstanceOf(IRVariable.class, extractFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, extractRecv.variableKind());
        assertEquals("Leg", extractRecv.type().type().name(),
                "the base retypes with the doubly-nested pipe's element type");
        assertTrue(adapter.declineReason(extractBareQty, ews).isEmpty(),
                "empty ⟺ lowers: the twin's widened gates agree");
    }

    @Test
    void extractBodiedPipeWideningLowersExtractPipeSources() {
        // #483 — the extract-bodied widening: when the resolved pipe argument is (or nests to)
        // an RExtractExpr with an IMPLICIT-or-paramless body, the element form of
        // `A extract B` is B's OWN result per element — every legacy map route (the
        // single/item routes and the LoL routes alike) produces result elements that
        // ARE the body's values (the MULTI/LoL-body routes flattening to the body's own), and
        // the checker types the extract off its BODY — so the allowlist and the structural
        // derivation recurse into the extract's body expression while the type read stays on
        // the SEAT's resolved source (the extract node at the flat face, the enclosing then at
        // the nested face — both parsed cache-visible nodes). (1) The FLAT face: the outer
        // filter's elided argument resolves to `trades extract leg`, whose bare-attr body
        // proves and derives Leg; the bare `qty` claim adapts whole via the bare-attr arm off
        // the #479-taught synthetic base, and the mirror twin agrees (empty <=> lowers).
        String flatSource = """
                namespace "test.step483"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func FlatExtractPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract leg then filter qty exists
                """;
        RModel flatModel = AstBuilder.buildFromString(flatSource, "step483-flat-extract-pipe.rosetta");
        RWorkspace fws = RWorkspace.build(List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RExtractExpr.class, flatThen.argument(),
                "the extract must bind tighter than then — the flat face's resolved argument");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        RImplicitVariable flatElided = assertInstanceOf(RImplicitVariable.class,
                flatFilter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        assertTrue(flatElided.isSynthetic(), "the elided argument is the parser-synthesized implicit");
        RExistenceExpr flatExists = assertInstanceOf(RExistenceExpr.class, flatFilter.body().body());
        RSymbolReference flatBareQty = assertInstanceOf(RSymbolReference.class, flatExists.argument());

        FieldAccess flatFa = assertInstanceOf(FieldAccess.class, adapt(flatBareQty, fws));
        IRVariable flatRecv = assertInstanceOf(IRVariable.class, flatFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, flatRecv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Leg", flatRecv.type().type().name(),
                "the base retypes with the EXTRACT's element type — the extract node's own"
                        + " cached type through the widened read (the body's own result)");
        assertEquals(ExpressionCardinality.SINGLE, flatRecv.cardinality());
        assertEquals(flatBareQty.sourceRange(), flatRecv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(adapter.declineReason(flatBareQty, fws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (2) The NESTED face: the elided argument resolves to the INNER then
        //     (`trades then extract leg`) — the #482 then descent composes with the #483
        //     extract descent, and the type read stays on the inner THEN node.
        String nestedSource = """
                namespace "test.step483b"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func NestedExtractPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades then extract leg then filter qty exists
                """;
        RModel nestedModel = AstBuilder.buildFromString(nestedSource,
                "step483-nested-extract-pipe.rosetta");
        RWorkspace nws = RWorkspace.build(List.of(nestedModel)).workspace();
        RThenExpr nestedOuterThen = AstWalker.findFirst(nws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RThenExpr nestedInnerThen = assertInstanceOf(RThenExpr.class, nestedOuterThen.argument(),
                "the left-associative chain must nest the extract-bodied then as the outer ARGUMENT");
        assertInstanceOf(RExtractExpr.class, nestedInnerThen.body().orElseThrow().body(),
                "the inner then body must be the extract");
        RFilterExpr nestedFilter = assertInstanceOf(RFilterExpr.class,
                nestedOuterThen.body().orElseThrow().body());
        RExistenceExpr nestedExists = assertInstanceOf(RExistenceExpr.class,
                nestedFilter.body().body());
        RSymbolReference nestedBareQty = assertInstanceOf(RSymbolReference.class,
                nestedExists.argument());

        FieldAccess nestedFa = assertInstanceOf(FieldAccess.class, adapt(nestedBareQty, nws));
        IRVariable nestedRecv = assertInstanceOf(IRVariable.class, nestedFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, nestedRecv.variableKind());
        assertEquals("Leg", nestedRecv.type().type().name(),
                "the base retypes through BOTH descents — the inner then's own cached type");
        assertEquals(ExpressionCardinality.SINGLE, nestedRecv.cardinality());
        assertTrue(adapter.declineReason(nestedBareQty, nws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (3) The NAMED-extract descent (the #521 retense of the #483 named residue lock —
        //     decline → ADMIT): the per-element value identity holds regardless of the
        //     param's naming, and the body's `t -> leg` derives through the binder-source
        //     recursion — so the bare qty claim now adapts off the proven Leg element
        //     (pre-#521 this exact fixture WAS the deep:RExtractExpr.named face, the
        //     probe2-decoded dominant drr residue).
        String namedSource = """
                namespace "test.step483c"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func NamedExtractPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract t [ t -> leg ] then filter qty exists
                """;
        RModel namedModel = AstBuilder.buildFromString(namedSource,
                "step483-named-extract-pipe.rosetta");
        RWorkspace nmws = RWorkspace.build(List.of(namedModel)).workspace();
        RThenExpr namedThen = AstWalker.findFirst(nmws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr namedExtract = assertInstanceOf(RExtractExpr.class, namedThen.argument(),
                "the named extract must be the then's argument");
        assertFalse(namedExtract.body().isImplicit() || namedExtract.body().paramNames().isEmpty(),
                "the extract body must carry the explicit param — the named face's gate");
        RFilterExpr namedFilter = assertInstanceOf(RFilterExpr.class,
                namedThen.body().orElseThrow().body());
        RExistenceExpr namedExists = assertInstanceOf(RExistenceExpr.class,
                namedFilter.body().body());
        RSymbolReference namedBareQty = assertInstanceOf(RSymbolReference.class,
                namedExists.argument());
        assertTrue(adapter.adapt(namedBareQty, nmws).isPresent(),
                "a NAMED extract-bodied pipe ADAPTS since the #521 descent — the value"
                        + " identity is naming-blind and the body derives through the"
                        + " binder-source recursion (pre-#521 this claim WAS the"
                        + " deep:RExtractExpr[.named] face)");
        assertTrue(adapter.declineReason(namedBareQty, nmws).isEmpty(),
                "empty <=> lowers: the named-extract face is REMOVED (the #483"
                        + " conservatism retired at the source walk)");
    }

    @Test
    void bareFunctionPipeWideningLowersCallableOutputSources() {
        // #484 — the callable-output widening: when the resolved pipe argument is (or nests
        // to) an extract whose body is a BARE no-arg function reference, the applied value IS
        // the callee's OUTPUT — legacy ReferenceHandler.renderImplicitFunctionInvocation
        // renders `<callee>.evaluate(<binding>.get())` (the invocation carries the function's
        // output type per its contract) and the checker types the bare reference off
        // fn.output() (inferTypeOfNode's RFunction leg) — so the allowlist proves the OUTPUT
        // attribute non-meta and the structural derivation reads its declared data type: the
        // L-109 point-free application's element form, the value identity's function face.
        // (1) The FLAT face: the outer filter's elided argument resolves to
        //     `trades extract ToLeg`, whose bare-F body proves on ToLeg's output (Leg — a
        //     non-meta declared-DATA single output); the bare `qty` claim adapts whole via
        //     the bare-attr arm off the #479-taught synthetic base.
        String flatSource = """
                namespace "test.step484"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func ToLeg:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result Leg (1..1)
                    set result:
                        trade -> leg

                func FlatBareFnPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract ToLeg then filter qty exists
                """;
        RModel flatModel = AstBuilder.buildFromString(flatSource, "step484-flat-barefn-pipe.rosetta");
        RWorkspace fws = RWorkspace.build(List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr flatExtract = assertInstanceOf(RExtractExpr.class, flatThen.argument(),
                "the extract must bind tighter than then — the flat face's resolved argument");
        RSymbolReference flatBareFn = assertInstanceOf(RSymbolReference.class,
                flatExtract.body().body(),
                "the extract body must be the bare symbol reference — the point-free shape");
        assertInstanceOf(RFunction.class, flatBareFn.symbol().orElseThrow(),
                "the bare body must resolve to the FUNCTION — the callable-output face");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        RExistenceExpr flatExists = assertInstanceOf(RExistenceExpr.class, flatFilter.body().body());
        RSymbolReference flatBareQty = assertInstanceOf(RSymbolReference.class, flatExists.argument());

        FieldAccess flatFa = assertInstanceOf(FieldAccess.class, adapt(flatBareQty, fws));
        IRVariable flatRecv = assertInstanceOf(IRVariable.class, flatFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, flatRecv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Leg", flatRecv.type().type().name(),
                "the base retypes with the CALLEE's output type — the extract's own cached type"
                        + " (the checker types the extract off its bare-F body = fn.output())");
        assertEquals(ExpressionCardinality.SINGLE, flatRecv.cardinality());
        assertEquals(flatBareQty.sourceRange(), flatRecv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(adapter.declineReason(flatBareQty, fws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (2) The NESTED face: the elided argument resolves to the INNER then
        //     (`trades then extract ToLeg`) — the #482 then descent + the #483 extract descent
        //     compose with the #484 callable-output leg, three laws deep.
        String nestedSource = """
                namespace "test.step484b"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func ToLeg:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result Leg (1..1)
                    set result:
                        trade -> leg

                func NestedBareFnPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades then extract ToLeg then filter qty exists
                """;
        RModel nestedModel = AstBuilder.buildFromString(nestedSource,
                "step484-nested-barefn-pipe.rosetta");
        RWorkspace nws = RWorkspace.build(List.of(nestedModel)).workspace();
        RThenExpr nestedOuterThen = AstWalker.findFirst(nws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RThenExpr nestedInnerThen = assertInstanceOf(RThenExpr.class, nestedOuterThen.argument(),
                "the left-associative chain must nest the extract-bodied then as the outer ARGUMENT");
        RExtractExpr nestedExtract = assertInstanceOf(RExtractExpr.class,
                nestedInnerThen.body().orElseThrow().body(),
                "the inner then body must be the extract");
        assertInstanceOf(RFunction.class,
                assertInstanceOf(RSymbolReference.class, nestedExtract.body().body())
                        .symbol().orElseThrow(),
                "the nested extract body must resolve to the FUNCTION");
        RFilterExpr nestedFilter = assertInstanceOf(RFilterExpr.class,
                nestedOuterThen.body().orElseThrow().body());
        RExistenceExpr nestedExists = assertInstanceOf(RExistenceExpr.class,
                nestedFilter.body().body());
        RSymbolReference nestedBareQty = assertInstanceOf(RSymbolReference.class,
                nestedExists.argument());

        FieldAccess nestedFa = assertInstanceOf(FieldAccess.class, adapt(nestedBareQty, nws));
        IRVariable nestedRecv = assertInstanceOf(IRVariable.class, nestedFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, nestedRecv.variableKind());
        assertEquals("Leg", nestedRecv.type().type().name(),
                "the base retypes through the then + extract descents onto the callee's output");
        assertEquals(ExpressionCardinality.SINGLE, nestedRecv.cardinality());
        assertTrue(adapter.declineReason(nestedBareQty, nws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (3) The MULTI-output face: ToLegs returns Leg (0..*) — the arm is
        //     cardinality-agnostic like the bare-attribute leg (the applied values are the
        //     output's own elements — the #483 route-grid flatten faces); the element type
        //     stays the output's declared Leg.
        String multiSource = """
                namespace "test.step484d"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func ToLegs:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result Leg (0..*)
                    set result:
                        trade -> leg

                func MultiBareFnPipeFn:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result Leg (0..*)
                    set result:
                        trade extract ToLegs then filter qty exists
                """;
        RModel multiModel = AstBuilder.buildFromString(multiSource,
                "step484-multi-barefn-pipe.rosetta");
        RWorkspace uws = RWorkspace.build(List.of(multiModel)).workspace();
        RThenExpr multiThen = AstWalker.findFirst(uws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr multiExtract = assertInstanceOf(RExtractExpr.class, multiThen.argument());
        assertInstanceOf(RFunction.class,
                assertInstanceOf(RSymbolReference.class, multiExtract.body().body())
                        .symbol().orElseThrow(),
                "the multi-face body must resolve to the FUNCTION");
        RExistenceExpr multiExists = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, multiThen.body().orElseThrow().body())
                        .body().body());
        RSymbolReference multiBareQty = assertInstanceOf(RSymbolReference.class,
                multiExists.argument());

        FieldAccess multiFa = assertInstanceOf(FieldAccess.class, adapt(multiBareQty, uws));
        IRVariable multiRecv = assertInstanceOf(IRVariable.class, multiFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, multiRecv.variableKind());
        assertEquals("Leg", multiRecv.type().type().name(),
                "a MULTI output's element type is the output's own declared Leg — the"
                        + " cardinality-agnostic leg");
        assertEquals(ExpressionCardinality.SINGLE, multiRecv.cardinality());
        assertTrue(adapter.declineReason(multiBareQty, uws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (4) The META-output residue face: ToRef's output carries [metadata reference] — the
        //     allowlist's non-meta gate declines the source (the applied value carries meta:
        //     the L-029 gm coercion class), so the retype nulls and the equivalent stays
        //     unlowered — prove-or-decline, the safe direction; sized on the witness channel
        //     as symbol:RFunction.outputMeta, not claimed.
        String metaSource = """
                namespace "test.step484c"

                type Trade:
                    leg Leg (1..1)

                type Leg:
                    qty number (1..1)

                func ToRef:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result Leg (1..1)
                            [metadata reference]
                    set result:
                        trade -> leg

                func MetaBareFnPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract ToRef then filter qty exists
                """;
        RModel metaModel = AstBuilder.buildFromString(metaSource, "step484-meta-barefn-pipe.rosetta");
        RWorkspace mws = RWorkspace.build(List.of(metaModel)).workspace();
        RThenExpr metaThen = AstWalker.findFirst(mws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr metaExtract = assertInstanceOf(RExtractExpr.class, metaThen.argument());
        RFunction metaCallee = assertInstanceOf(RFunction.class,
                assertInstanceOf(RSymbolReference.class, metaExtract.body().body())
                        .symbol().orElseThrow(),
                "the meta-face body must resolve to the FUNCTION");
        assertTrue(metaCallee.output().isPresent()
                        && metaCallee.output().get().annotationRefs().stream()
                                .anyMatch(a -> "metadata".equals(a.annotationName())),
                "the callee output must carry the [metadata reference] annotation — the"
                        + " outputMeta face's EXACT gate predicate (the #483 assert-the-FACE"
                        + " law: isMetaAnnotated matches annotationName == \"metadata\", not"
                        + " mere annotation presence)");
        RExistenceExpr metaExists = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, metaThen.body().orElseThrow().body())
                        .body().body());
        RSymbolReference metaBareQty = assertInstanceOf(RSymbolReference.class,
                metaExists.argument());
        // RECUT at #504 (the pin-recut law): the META-output bare-function pipe — the aw0tw1
        // class exactly (the allowlist declines the meta output, the TYPING walk still proves
        // Leg through the callable-output leg) — now claims via the arm-B2 IRSynItemNav leg:
        // the equivalent's receiver mints and the bare attr re-mints with the RAW node's
        // range/type (the oracle-root bareSymbolRef serve renders legacy's own ladder whole).
        IRSynItemNav metaMinted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(metaBareQty, mws).orElseThrow(() -> new AssertionError(
                        "a meta-output-sourced bare item attr must mint since the #504 arm")),
                "the aw0tw1 residue lowers to the shallow kind");
        assertEquals("qty", metaMinted.featureName(),
                "the resolved feature's name is the neutral fact");
        assertTrue(adapter.declineReason(metaBareQty, mws).isEmpty(),
                "empty ⟺ lowers: the twin's navType gate matches the arm's");
    }

    @Test
    void ruleInputWideningLowersRuleTopElidedSources() throws IOException {
        // #485 — the rule-input widening: an UNRESOLVED elided implicit whose binder walk
        // stops noBinder at RULE top level takes the RULE INPUT's value — legacy
        // ReferenceHandler.handle(RImplicitVariable)'s isElidedOperandTopLevel route renders
        // `MapperS.of(input)` (the RFunction.fromRule-synthesized input parameter, typed by
        // the rule's `from` type) and the checker types the same face off the rule's
        // from-type (computeImplicitItemType's top-level branch → inferRuleFromType, always
        // withNoMeta) — so the allowlist admits and the structural derivation reads the
        // rule's declared from-type: a DECLARATION read, the value identity at the rule seat.
        // (1) The FLAT direct face: a rule-top elided filter — the filter's binding source is
        //     the rule input (a Trade), so the bare `leg` claim adapts whole via the
        //     bare-attr arm off the #479-taught synthetic base retyped to the FROM-type.
        String flatSource = """
                namespace "test.step485"

                type Trade:
                    leg Leg (0..*)

                type Leg:
                    qty number (1..1)

                reporting rule RuleTopFilter from Trade:
                    filter leg exists
                """;
        RModel flatModel = AstBuilder.buildFromString(flatSource, "step485-ruletop-filter.rosetta");
        RWorkspace fws = RWorkspace.build(List.of(flatModel)).workspace();
        RRule flatRule = AstWalker.findFirst(fws.files().get(0), RRule.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RRule"));
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatRule.expression().orElseThrow(),
                "the rule body must be the without-left filter");
        RImplicitVariable flatElided = assertInstanceOf(RImplicitVariable.class,
                flatFilter.argument(),
                "the rule-top filter must carry the grammar-elided argument");
        assertTrue(flatElided.isSynthetic(), "the elided operand is parser-synthesized");
        assertInstanceOf(RDataType.class,
                flatRule.fromType().orElseThrow().referencedType().orElseThrow(),
                "the rule's from-type must resolve to the DATA type — the fromData face's"
                        + " EXACT gate predicate (the assert-the-FACE law)");
        RExistenceExpr flatExists = assertInstanceOf(RExistenceExpr.class,
                flatFilter.body().body());
        RSymbolReference flatBareLeg = assertInstanceOf(RSymbolReference.class,
                flatExists.argument());

        FieldAccess flatFa = assertInstanceOf(FieldAccess.class, adapt(flatBareLeg, fws));
        IRVariable flatRecv = assertInstanceOf(IRVariable.class, flatFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, flatRecv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Trade", flatRecv.type().type().name(),
                "the base retypes with the RULE's declared from-type — the rule-input"
                        + " identity's declaration read (no cached-type channel)");
        assertEquals(ExpressionCardinality.SINGLE, flatRecv.cardinality());
        assertEquals(flatBareLeg.sourceRange(), flatRecv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(adapter.declineReason(flatBareLeg, fws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (2) The COMPOSED face (the flat filter.elidedImplicit class): the extract's elided
        //     source resolves to the FILTER whose own elided argument is the rule input — the
        //     filter arm's element-preserving recursion composes with the rule-input arm, and
        //     the type read stays on the FILTER node (parsed, cache-visible: the checker types
        //     a rule-top filter off the rule's from-type).
        String composedSource = """
                namespace "test.step485b"

                type Trade:
                    leg Leg (0..*)

                type Leg:
                    qty number (1..1)

                reporting rule RuleTopPipe from Trade:
                    filter leg exists then extract leg
                """;
        RModel composedModel = AstBuilder.buildFromString(composedSource,
                "step485-ruletop-pipe.rosetta");
        RWorkspace cws = RWorkspace.build(List.of(composedModel)).workspace();
        RRule composedRule = AstWalker.findFirst(cws.files().get(0), RRule.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RRule"));
        RThenExpr composedThen = assertInstanceOf(RThenExpr.class,
                composedRule.expression().orElseThrow(),
                "the rule body must be the then-chain");
        assertInstanceOf(RImplicitVariable.class,
                assertInstanceOf(RFilterExpr.class, composedThen.argument()).argument(),
                "the composed filter must carry the grammar-elided rule-input argument");
        RExtractExpr composedExtract = assertInstanceOf(RExtractExpr.class,
                composedThen.body().orElseThrow().body(),
                "the then body must be the extract");
        RSymbolReference composedBareLeg = assertInstanceOf(RSymbolReference.class,
                composedExtract.body().body(),
                "the extract body must be the bare leg reference — the claim seat");

        FieldAccess composedFa = assertInstanceOf(FieldAccess.class, adapt(composedBareLeg, cws));
        IRVariable composedRecv = assertInstanceOf(IRVariable.class, composedFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, composedRecv.variableKind());
        assertEquals("Trade", composedRecv.type().type().name(),
                "the base retypes through the filter composition — the FILTER's own cached"
                        + " type (the rule's from-type, element-preserving)");
        assertEquals(ExpressionCardinality.SINGLE, composedRecv.cardinality());
        assertTrue(adapter.declineReason(composedBareLeg, cws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (3) The functionTop DECLINE face: the same elided shape at FUNCTION top level —
        //     legacy's isElidedOperandTopLevel walk stops at RFunction (a function body's
        //     input is not named `input`), so the rule-input arm declines whole and the
        //     synthetic base never retypes. Hand-built with a RESOLVED feature (a parsed
        //     function-top elided form leaves its bare body nav Cat-9-unresolved — the
        //     fixture pins the STRUCTURE the arm walks, the witness fixture-3 seat).
        RWorkspace hws = workspace();
        RFunction fnTop = new RFunction();
        fnTop.setName("FnTopFixtureFn");
        fnTop.attachToWorkspace(hws);
        RAttribute qtyFeature = new RAttribute();
        qtyFeature.setName("qty");
        qtyFeature.setCardinality(cardinality(1, false));
        qtyFeature.attachToWorkspace(hws);
        RImplicitVariable fnTopItem = new RImplicitVariable();
        fnTopItem.setSynthetic(true);
        RFeatureCall fnTopNav = new RFeatureCall();
        fnTopNav.setReceiver(fnTopItem);
        fnTopNav.setFeatureName("qty");
        fnTopNav.setResolvedFeature(qtyFeature);
        fnTopItem.setParent(fnTopNav);
        RInlineFunction fnTopBodyFn = new RInlineFunction();
        fnTopBodyFn.setBody(fnTopNav);
        fnTopNav.setParent(fnTopBodyFn);
        RImplicitVariable fnTopElided = new RImplicitVariable();
        fnTopElided.setSynthetic(true);
        RFilterExpr fnTopFilter = new RFilterExpr();
        fnTopFilter.setArgument(fnTopElided);
        fnTopElided.setParent(fnTopFilter);
        fnTopFilter.setBody(fnTopBodyFn);
        fnTopBodyFn.setParent(fnTopFilter);
        fnTopFilter.setParent(fnTop);
        fnTopItem.attachToWorkspace(hws);
        fnTopNav.attachToWorkspace(hws);
        fnTopElided.attachToWorkspace(hws);

        // RECUT at #504 (the pin-recut law): the FUNCTION-top elided residue — the rule-input
        // arm's RFunction stop still declines the RETYPE, but the un-retypeable nav now MINTS
        // the shallow IRSynItemNav (the arm-B teach; the mint carries the resolved feature's
        // name and the containing root oracle-serves legacy's own ladder).
        IRSynItemNav fnTopMinted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(fnTopNav, hws).orElseThrow(() -> new AssertionError(
                        "a function-top elided-source nav must mint since the #504 arm")),
                "the un-retypeable residue lowers to the shallow kind");
        assertEquals("qty", fnTopMinted.featureName(),
                "the resolved feature's name is the neutral fact");
        assertTrue(adapter.declineReason(fnTopNav, hws).isEmpty(),
                "empty ⟺ lowers: the twin restates the mint");
    }

    @Test
    void conditionalJoinWideningLowersConditionalBodiedSources() throws IOException {
        // #487 — the conditional JOIN widening: the element form of `if c then A else B` is
        // the arms' COMMON form — the runtime elements are the EXECUTED arm's (legacy
        // ControlFlowHandler.handle(RConditionalExpr): every route, the hoisted locals and the
        // inline ternary alike, yields ONE arm's value) and the checker types the node
        // withNoMeta(typeJoin.join(thenT, elseT)) (ExpressionTypeComputer.computeConditional;
        // an absent/empty else types NOTHING — the join's bottom — so the empty-else face
        // types as the THEN arm alone), so the allowlist admits on the per-arm proofs (the
        // ONLY meta protection — the withNoMeta wrap strips arm meta from the cache) and the
        // structural derivation reads the arms' COMMON declared form (instance identity — the
        // list-literal law lifted to the two-armed choice).
        // (1) The DIRECT sameType face: the pipe argument IS a conditional over two
        //     same-typed inputs — the bare `leg` claim adapts whole via the bare-attr arm off
        //     the #479-taught synthetic base retyped through the conditional's CACHED join
        //     type (Trade — the checker's own withNoMeta(join(Trade, Trade))).
        String flatSource = """
                namespace "test.step487"

                type Trade:
                    leg Leg (0..*)

                type Leg:
                    qty number (1..1)

                func FlatCondPipeFn:
                    inputs:
                        trades Trade (0..*)
                        fallback Trade (0..*)
                        useMain boolean (1..1)
                    output:
                        result Trade (0..*)
                    set result:
                        (if useMain then trades else fallback) then filter leg exists
                """;
        RModel flatModel = AstBuilder.buildFromString(flatSource, "step487-flat-cond-pipe.rosetta");
        RWorkspace fws = RWorkspace.build(List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RConditionalExpr flatCond = assertInstanceOf(RConditionalExpr.class, flatThen.argument(),
                "the parenthesized conditional must be the then's ARGUMENT");
        assertTrue(flatCond.elseBranch().isPresent(),
                "the genuine-else carrier must parse its else branch");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body());
        RExistenceExpr flatExists = assertInstanceOf(RExistenceExpr.class,
                flatFilter.body().body());
        RSymbolReference flatBareLeg = assertInstanceOf(RSymbolReference.class,
                flatExists.argument());

        FieldAccess flatFa = assertInstanceOf(FieldAccess.class, adapt(flatBareLeg, fws));
        IRVariable flatRecv = assertInstanceOf(IRVariable.class, flatFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, flatRecv.variableKind(),
                "the equivalent's receiver lowers through the #479-taught synthetic base");
        assertEquals("Trade", flatRecv.type().type().name(),
                "the base retypes with the conditional's cached JOIN type — join(Trade,"
                        + " Trade) = Trade, withNoMeta by the checker's own wrap");
        assertEquals(ExpressionCardinality.SINGLE, flatRecv.cardinality());
        assertEquals(flatBareLeg.sourceRange(), flatRecv.sourceRange(),
                "the base re-stamps the RAW node's range — the emitter's correlation key");
        assertTrue(adapter.declineReason(flatBareLeg, fws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (2) The EXTRACT-BODIED sameType face — THE dominant decoded pipe class (435 at
        //     population): the extract's implicit body is a conditional over two same-typed
        //     item attributes; the #483 body-descent recursion composes with the #487 JOIN
        //     leg, and the type read stays on the EXTRACT node (typed off its body — the
        //     conditional's join, Leg).
        String extractSource = """
                namespace "test.step487b"

                type Trade:
                    leg Leg (1..1)
                    altLeg Leg (1..1)
                    useAlt boolean (1..1)

                type Leg:
                    qty number (1..1)

                func ExtractCondPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract (if useAlt then altLeg else leg) then filter qty exists
                """;
        RModel extractModel = AstBuilder.buildFromString(extractSource,
                "step487-extract-cond-pipe.rosetta");
        RWorkspace ews = RWorkspace.build(List.of(extractModel)).workspace();
        RThenExpr extractThen = AstWalker.findFirst(ews.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr extractExtract = assertInstanceOf(RExtractExpr.class, extractThen.argument());
        assertInstanceOf(RConditionalExpr.class, extractExtract.body().body(),
                "the extract body must be the parsed conditional — the dominant decoded face");
        RExistenceExpr extractExists = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, extractThen.body().orElseThrow().body())
                        .body().body());
        RSymbolReference extractBareQty = assertInstanceOf(RSymbolReference.class,
                extractExists.argument());

        FieldAccess extractFa = assertInstanceOf(FieldAccess.class, adapt(extractBareQty, ews));
        IRVariable extractRecv = assertInstanceOf(IRVariable.class, extractFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, extractRecv.variableKind());
        assertEquals("Leg", extractRecv.type().type().name(),
                "the base retypes through the extract's cached type — the checker types the"
                        + " extract off its BODY, the conditional's join(Leg, Leg) = Leg");
        assertEquals(ExpressionCardinality.SINGLE, extractRecv.cardinality());
        assertTrue(adapter.declineReason(extractBareQty, ews).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (3) The elseEmpty face: an else-less conditional — DefaultElseRule stamps the
        //     SYNTHETIC empty-list else, hasGenuineElse reads it structurally empty, and the
        //     arm admits on the then arm alone (an empty else contributes NO elements; the
        //     checker's join(Leg, NOTHING) = Leg bottom rule keeps the cached type the then
        //     form).
        String emptySource = """
                namespace "test.step487c"

                type Trade:
                    leg Leg (1..1)
                    altLeg Leg (1..1)
                    useAlt boolean (1..1)

                type Leg:
                    qty number (1..1)

                func EmptyElseCondPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract (if useAlt then altLeg) then filter qty exists
                """;
        RModel emptyModel = AstBuilder.buildFromString(emptySource,
                "step487-empty-else-cond-pipe.rosetta");
        RWorkspace mws = RWorkspace.build(List.of(emptyModel)).workspace();
        RThenExpr emptyThen = AstWalker.findFirst(mws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RConditionalExpr emptyCond = assertInstanceOf(RConditionalExpr.class,
                assertInstanceOf(RExtractExpr.class, emptyThen.argument()).body().body(),
                "the extract body must be the else-less conditional");
        assertTrue(emptyCond.elseBranch().isEmpty()
                        || emptyCond.elseBranch().get() instanceof REmptyLiteral
                        || (emptyCond.elseBranch().get() instanceof RListLiteral sl
                                && sl.elements().isEmpty()),
                "the empty-else face — absent, `empty`, or the DefaultElseRule synthetic"
                        + " empty list (the structural-emptiness discriminator's EXACT"
                        + " predicate)");
        RExistenceExpr emptyExists = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, emptyThen.body().orElseThrow().body())
                        .body().body());
        RSymbolReference emptyBareQty = assertInstanceOf(RSymbolReference.class,
                emptyExists.argument());

        FieldAccess emptyFa = assertInstanceOf(FieldAccess.class, adapt(emptyBareQty, mws));
        IRVariable emptyRecv = assertInstanceOf(IRVariable.class, emptyFa.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, emptyRecv.variableKind());
        assertEquals("Leg", emptyRecv.type().type().name(),
                "the empty-else face retypes to the THEN form alone — the join's bottom rule");
        assertTrue(adapter.declineReason(emptyBareQty, mws).isEmpty(),
                "empty <=> lowers: the twin's derivation widened with the arm's");

        // (4) The MIXED-form DECLINE face (typeDiffers.elseSuper at the witness): the arms
        //     derive DIFFERENT data types (ExtLeg extends Leg) — the derivation's
        //     same-instance law declines (the list-literal mixed law; the #487 decode read
        //     the whole typeDiffers family at ZERO population mass), so the bare-attr nav
        //     stays legacy's — prove-or-decline in the honest direction.
        String superSource = """
                namespace "test.step487d"

                type Trade:
                    ext ExtLeg (1..1)
                    base Leg (1..1)
                    useExt boolean (1..1)

                type Leg:
                    qty number (1..1)

                type ExtLeg extends Leg:
                    extra number (1..1)

                func SuperCondPipeFn:
                    inputs:
                        trades Trade (0..*)
                    output:
                        result Leg (0..*)
                    set result:
                        trades extract (if useExt then ext else base) then filter qty exists
                """;
        RModel superModel = AstBuilder.buildFromString(superSource,
                "step487-super-cond-pipe.rosetta");
        RWorkspace sws = RWorkspace.build(List.of(superModel)).workspace();
        RThenExpr superThen = AstWalker.findFirst(sws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RConditionalExpr.class,
                assertInstanceOf(RExtractExpr.class, superThen.argument()).body().body(),
                "the extract body must be the mixed-form conditional");
        RExistenceExpr superExists = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, superThen.body().orElseThrow().body())
                        .body().body());
        RSymbolReference superBareQty = assertInstanceOf(RSymbolReference.class,
                superExists.argument());

        // #528 arm-5 RETENSE: the mixed-form source still defeats the ARM (the same-instance
        // derivation law is untouched — no item-nav equivalent is built), but the L-113
        // conversion now mints the SHALLOW kind for the sourceElementUnresolved family instead
        // of declining. The lock's invariant is preserved in the stronger form: the lowering
        // must NOT be the arm's derived navigation.
        IRExpr mixedLowered = adapter.adapt(superBareQty, sws).orElseThrow(
                () -> new AssertionError("the #528 mint claims the unresolved-source residue"));
        assertInstanceOf(IRImplicitAttrNav.class, mixedLowered,
                "the shallow serve kind — the mixed face's render stays legacy's own");
    }

    private IRExpr adapt(RExpression expr, RWorkspace ws) {
        Optional<IRExpr> result = adapter.adapt(expr, ws);
        assertTrue(result.isPresent(), () -> expr.getClass().getSimpleName() + " should be Wave-0-expressible");
        return result.get();
    }

    private static RIntLiteral intLiteral(int value) {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RBooleanLiteral boolLiteral(boolean value) {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    /** A single-hop feature navigation {@code receiver -> feature} over an already-wired param receiver. */
    private static RFeatureCall featureCall(String recvName, RAttribute recv,
            String featureName, RAttribute feature, RWorkspace ws) {
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(paramRef(recvName, recv, ws));
        fc.setFeatureName(featureName);
        fc.setResolvedFeature(feature);
        fc.attachToWorkspace(ws);
        return fc;
    }

    /** A feature navigation hop {@code receiver -> feature} over an already-built receiver expression
     *  (the receiver may itself be a navigation, building a chain {@code a -> b -> c}). */
    private static RFeatureCall hop(RExpression receiver, String featureName, RAttribute feature, RWorkspace ws) {
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(receiver);
        fc.setFeatureName(featureName);
        fc.setResolvedFeature(feature);
        fc.attachToWorkspace(ws);
        return fc;
    }

    /** An existence/absence check over an already-wired function-input attribute. */
    private static RExistenceExpr existence(ExistenceOp op, String name, RAttribute attr, RWorkspace ws) {
        RExistenceExpr e = new RExistenceExpr();
        e.setOp(op);
        e.setArgument(paramRef(name, attr, ws));
        e.attachToWorkspace(ws);
        return e;
    }

    /** A flat postfix list op (`<param> distinct` etc.) over an already-wired function-input attribute. */
    private static RListOpExpr listOp(ListOp op, String name, RAttribute attr, RWorkspace ws) {
        RListOpExpr e = new RListOpExpr();
        e.setOp(op);
        e.setArgument(paramRef(name, attr, ws));
        e.attachToWorkspace(ws);
        return e;
    }

    /** A `<param> count` over an already-wired function-input attribute. */
    private static RCountExpr countOf(String name, RAttribute attr, RWorkspace ws) {
        RCountExpr e = new RCountExpr();
        e.setArgument(paramRef(name, attr, ws));
        e.attachToWorkspace(ws);
        return e;
    }

    /** A param/param ordered comparison over two already-wired function-input attributes. */
    private static RComparisonExpr comparison(CompOp op, String leftName, RAttribute left,
            String rightName, RAttribute right, RWorkspace ws) {
        RComparisonExpr cmp = new RComparisonExpr();
        cmp.setOp(op);
        cmp.setLeft(paramRef(leftName, left, ws));
        cmp.setRight(paramRef(rightName, right, ws));
        cmp.attachToWorkspace(ws);
        return cmp;
    }

    /** A param/param arithmetic operation over two already-wired function-input attributes. */
    private static RArithmeticExpr arithmetic(ArithOp op, String leftName, RAttribute left,
            String rightName, RAttribute right, RWorkspace ws) {
        RArithmeticExpr arith = new RArithmeticExpr();
        arith.setOp(op);
        arith.setLeft(paramRef(leftName, left, ws));
        arith.setRight(paramRef(rightName, right, ws));
        arith.attachToWorkspace(ws);
        return arith;
    }

    /** An arithmetic operation over two already-built operand sub-expressions (for the nested/compound shapes). */
    private static RArithmeticExpr arithmetic(ArithOp op, RExpression left, RExpression right, RWorkspace ws) {
        RArithmeticExpr arith = new RArithmeticExpr();
        arith.setOp(op);
        arith.setLeft(left);
        arith.setRight(right);
        arith.attachToWorkspace(ws);
        return arith;
    }

    /**
     * A conditional {@code if condition then thenBranch (else elseBranch)?} over already-built child
     * expressions. A {@code null} {@code elseBranch} builds the else-less form (never calls {@code
     * setElseBranch}, so the adapter sees a genuine no-else — D2). Mirrors the {@code comparison}/{@code
     * arithmetic}/{@code existence} AST-builders (and {@code IRPythonEmitterTest.conditionalExpr}) so the
     * conditional tests don't repeat the new/set/attach boilerplate.
     */
    private static RConditionalExpr conditional(RExpression condition, RExpression thenBranch,
            RExpression elseBranch, RWorkspace ws) {
        RConditionalExpr cond = new RConditionalExpr();
        cond.setCondition(condition);
        cond.setThenBranch(thenBranch);
        if (elseBranch != null) {
            cond.setElseBranch(elseBranch);
        }
        cond.attachToWorkspace(ws);
        return cond;
    }

    /** An {@code alias op intLiteral} sub-expression (e.g. {@code daysInNonLeapPeriod / 365}). */
    private static RArithmeticExpr aliasOverLiteral(ArithOp op, RShortcut alias, int literal, RWorkspace ws) {
        RSymbolReference aliasRef = new RSymbolReference();
        aliasRef.setName(alias.name());
        aliasRef.setResolvedSymbol(alias);
        aliasRef.attachToWorkspace(ws);
        RArithmeticExpr arith = new RArithmeticExpr();
        arith.setOp(op);
        arith.setLeft(aliasRef);
        arith.setRight(intLiteral(literal));
        arith.attachToWorkspace(ws);
        return arith;
    }

    @Test
    void aliasNavTeachLowersDisguisedAndBoundReceiverNavs() {
        // #492 — the alias-nav teach (the L-032 story): a disguised `<shortcut> -> <feature>`
        // REnumValueRef lowers through adaptAliasHeadNav when the shared aliasNavBodyFacet walk
        // admits the body (typed, meta-free, not output-referencing) and the feature resolves
        // NON-META with a typeable hop on the body's engine data type — a single-hop FieldAccess
        // over the BODY-retyped IRReference{ALIAS} (the #478/#479 cache-boundary law at the L-032
        // seat: the reference itself reads engine-MISSING, the BODY types). The bound-receiver
        // form (a parsed RFeatureCall whose receiver resolved to the RShortcut — the probed
        // receiverAlias.bodyTyped face) lowers through the retypedAliasNavReceiver slot of the
        // same law. The decline faces stay twin-exact with the probe spellings
        // (bodyTyped.featureOffBody pinned below; usesOutput RETENSED to the #518 mint at
        // leg 3 — no longer a decline face at this seat; the #326 META-wrapper subset the
        // ENGINE reads clean delegates at the Java-side guard's ALIAS_NAV_RECEIVER_META arm
        // — not this seat).
        String source = """
                namespace "test.step492"

                type Leg:
                    rate number (1..1)

                type Trade:
                    leg Leg (1..1)
                    legs Leg (0..*)

                func FnNav:
                    inputs:
                        trade Trade (1..1)
                    output:
                        out number (1..1)
                    alias legAlias: trade -> leg
                    alias legsAlias: trade -> legs
                    set out:
                        trade -> leg -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step492-alias-nav.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction fnNav = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "FnNav".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no FnNav"));
        RShortcut legAlias = fnNav.shortcuts().stream()
                .filter(s -> "legAlias".equals(s.name())).findFirst().orElseThrow();
        RAttribute parsedRate = AstWalker.findAll(ws.files().get(0), RAttribute.class).stream()
                .filter(a -> "rate".equals(a.name())).findFirst().orElseThrow();

        // Leg 1 — the ADMIT face (aliasHead.bodyTyped.featureOnBody): the disguised single-hop
        // nav lowers to FieldAccess{IRReference{ALIAS legAlias, BODY-typed, SINGLE}, rate}.
        REnumValueRef disguise = new REnumValueRef();
        disguise.setEnumName("legAlias");
        disguise.setValueName("rate");
        disguise.setResolvedInputFeature(scalarParam("rate"));
        disguise.setParent(fnNav);
        disguise.attachToWorkspace(ws);
        Optional<IRExpr> lowered = adapter.adapt(disguise, ws);
        assertTrue(lowered.isPresent(), "the admitted alias-head disguise must lower (#492)");
        FieldAccess nav = assertInstanceOf(FieldAccess.class, lowered.get());
        assertEquals("rate", nav.feature());
        IRReference aliasRecv = assertInstanceOf(IRReference.class, nav.receiver());
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasRecv.referenceKind());
        assertEquals("legAlias", aliasRecv.target());
        assertNotNull(aliasRecv.type(), "the receiver must carry the BODY's engine type");
        assertFalse(aliasRecv.type().isMissing(),
                "the retype must defeat the L-032 MISSING reference read");
        assertEquals(ExpressionCardinality.SINGLE, aliasRecv.cardinality());
        assertNotNull(nav.type(), "the hop types from the feature's attribute channel");
        assertFalse(nav.type().isMissing());
        assertEquals(ExpressionCardinality.SINGLE, nav.cardinality());
        assertTrue(adapter.declineReason(disguise, ws).isEmpty(),
                "a claimed disguise never re-spells its face (the drift-detector state)");

        // Leg 2 — the MULTI-body admit: the alias over `trade -> legs` (0..*) carries MULTI on
        // the receiver and the accumulated chain.
        REnumValueRef multiDisguise = new REnumValueRef();
        multiDisguise.setEnumName("legsAlias");
        multiDisguise.setValueName("rate");
        multiDisguise.setResolvedInputFeature(scalarParam("rate"));
        multiDisguise.setParent(fnNav);
        multiDisguise.attachToWorkspace(ws);
        Optional<IRExpr> multiLowered = adapter.adapt(multiDisguise, ws);
        assertTrue(multiLowered.isPresent(), "the MULTI-bodied alias disguise must lower");
        FieldAccess multiNav = assertInstanceOf(FieldAccess.class, multiLowered.get());
        assertEquals(ExpressionCardinality.MULTI,
                assertInstanceOf(IRReference.class, multiNav.receiver()).cardinality());
        assertEquals(ExpressionCardinality.MULTI, multiNav.cardinality(),
                "the accumulated cardinality rides the MULTI receiver");
        assertEquals(ExpressionCardinality.SINGLE, multiNav.featureCardinality(),
                "the hop's own feature cardinality stays the leaf's (map, not mapC)");

        // Leg 3 — RETENSED at #518 (the #516 lock-lineage law: decline → lower): a shortcut
        // whose body references the enclosing OUTPUT by name now lowers to the DISTINCT
        // shallow IROutputAliasNav oracle leaf (arm B2 — the face every Mapper-shaped arm
        // must never admit stays OUT of the FieldAccess-over-ALIAS shape by KIND: legacy
        // renders the BUILDER walk, so the mint's every route is the literal legacy line —
        // the L-111 identity serve at the relabel seat, the standing oracle-root serves at
        // the probed roots. The pre-#518 pin asserted adapt-empty + the
        // inputFeatureNav.featureUnresolved.aliasHead.usesOutput spelling at this site).
        RShortcut outAlias = new RShortcut();
        outAlias.setName("outAlias");
        RSymbolReference outRef = new RSymbolReference();
        outRef.setName("out");
        outAlias.setExpression(outRef);
        outRef.setParent(outAlias);
        fnNav.shortcuts().add(outAlias);
        outAlias.setParent(fnNav);
        REnumValueRef usesOutputDisguise = new REnumValueRef();
        usesOutputDisguise.setEnumName("outAlias");
        usesOutputDisguise.setValueName("rate");
        usesOutputDisguise.setResolvedInputFeature(scalarParam("rate"));
        usesOutputDisguise.setParent(fnNav);
        usesOutputDisguise.attachToWorkspace(ws);
        Optional<IRExpr> usesOutputLowered = adapter.adapt(usesOutputDisguise, ws);
        assertTrue(usesOutputLowered.isPresent(),
                "an output-referencing alias-head nav lowers to the #518 mint");
        IROutputAliasNav outputNav = assertInstanceOf(IROutputAliasNav.class,
                usesOutputLowered.get(), "the lowering is the DISTINCT oracle-leaf kind —"
                        + " never a FieldAccess (the Mapper-shaped native quiet-claim"
                        + " shape stays byte-frozen by construction)");
        assertEquals("outAlias", outputNav.headName());
        assertEquals("rate", outputNav.featureName());
        assertTrue(adapter.declineReason(usesOutputDisguise, ws).isEmpty(),
                "a claimed usesOutput nav never re-spells its face (the drift-detector state)");

        // Leg 4 — the featureOffBody decline face: the feature does not resolve on the body's
        // engine data type.
        REnumValueRef offBody = new REnumValueRef();
        offBody.setEnumName("legAlias");
        offBody.setValueName("nosuch");
        offBody.setResolvedInputFeature(scalarParam("nosuch"));
        offBody.setParent(fnNav);
        offBody.attachToWorkspace(ws);
        assertTrue(adapter.adapt(offBody, ws).isEmpty(), "an off-body feature must defer");
        assertEquals(
                Optional.of("inputFeatureNav.featureUnresolved.aliasHead.bodyTyped.featureOffBody"),
                adapter.declineReason(offBody, ws));

        // Leg 5 — the bound-receiver form (the probed receiverAlias.bodyTyped face): a parsed
        // RFeatureCall whose receiver resolved to the RShortcut lowers through the
        // retypedAliasNavReceiver slot — the SAME body-retype law at the feature-call arm.
        RFeatureCall boundNav = new RFeatureCall();
        boundNav.setReceiver(aliasRef(legAlias, ws));
        boundNav.setFeatureName("rate");
        boundNav.setResolvedFeature(parsedRate);
        boundNav.attachToWorkspace(ws);
        Optional<IRExpr> boundLowered = adapter.adapt(boundNav, ws);
        assertTrue(boundLowered.isPresent(), "the bound alias-receiver nav must lower (#492)");
        FieldAccess boundFa = assertInstanceOf(FieldAccess.class, boundLowered.get());
        IRReference boundRecv = assertInstanceOf(IRReference.class, boundFa.receiver());
        assertEquals(IRReference.ReferenceKind.ALIAS, boundRecv.referenceKind());
        assertFalse(boundRecv.type().isMissing(), "the receiver rebuilt with the BODY's type");
        assertNotNull(boundFa.type());
        assertFalse(boundFa.type().isMissing(),
                "the hop types from the feature's attribute channel (the alias-based-chain law)");
        assertTrue(adapter.declineReason(boundNav, ws).isEmpty(),
                "a claimed bound nav never re-spells receiverAlias.bodyTyped");
    }

    private static RAttribute scalarParam(String name) {
        return attribute(name, cardinality(1, false));
    }

    private static RAttribute multiParam(String name) {
        return attribute(name, cardinality(0, true));
    }

    /** An optional single attribute {@code (0..1)} — single cardinality, optional optionality. */
    private static RAttribute optionalParam(String name) {
        return attribute(name, cardinality(0, false));
    }

    private static RAttribute attribute(String name, RCardinality cardinality) {
        RAttribute attr = new RAttribute();
        attr.setName(name);
        attr.setCardinality(cardinality);
        return attr;
    }

    private static RCardinality cardinality(int inf, boolean unbounded) {
        RCardinality card = new RCardinality();
        card.setInf(inf);
        if (unbounded) {
            card.setUnbounded(true);     // (inf..*)
        } else {
            card.setSup(1);              // (inf..1)
        }
        return card;
    }

    @Test
    void constructorLowersAtRootAndAdmittedSlotsWithShallowNeutralFacts() {
        // #494 — the RConstructorExpr lowering: a ROOT constructor lowers to the deliberately-
        // shallow IRConstruct (typeName + attributeNames + spread, NO value children). The lock
        // body RETENSED at #526 (the lock-lineage law): the list fixture that pinned the
        // root-only law's child-position decline now LOWERS — a claim-ROOT list's elements are
        // ADMITTED slots (the #526 element admission, the neGate census's elem.ctor class —
        // 6 cdm5 + 7 cdm6 events) — so the root-only boundary's negative moves to a
        // CHAIN-EXTERNAL seat: the same list under a count receiver keeps declining (the count's
        // argument adapts at a child NodeId outside every admitted chain), and the recut mirror
        // names receiverNotExpressible BY CALL (the slot lowers at its probe root, declines at
        // the child seat — the leg-2 divergence read).
        String source = """
                namespace "test.step494"

                type Party:
                    partyName string (1..1)

                func BuildParty:
                    output:
                        result Party (0..*)
                    set result:
                        [Party { partyName: "ACME" }]

                func CountParty:
                    output:
                        result int (1..1)
                    set result:
                        [Party { partyName: "ACME" }] count
                """;
        RModel model = AstBuilder.buildFromString(source, "step494-ctor-adapter.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RConstructorExpr ctor = AstWalker.findFirst(ws.files().get(0), RConstructorExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConstructorExpr"));
        RListLiteral list = AstWalker.findFirst(ws.files().get(0), RListLiteral.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RListLiteral"));
        RCountExpr count = AstWalker.findFirst(ws.files().get(0), RCountExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RCountExpr"));

        IRConstruct lowered = assertInstanceOf(IRConstruct.class,
                adapter.adapt(ctor, ws).orElseThrow(() -> new AssertionError(
                        "a typed data-target ROOT constructor must lower")));
        assertEquals("Party", lowered.typeName(), "the constructed type's name is the neutral fact");
        assertEquals(List.of("partyName"), lowered.attributeNames(),
                "the attribute names ride in source order");
        assertFalse(lowered.spread(), "no ... operator on the fixture");
        assertFalse(lowered.type().isMissing(), "the cache type is stamped honest (typed fixture)");
        assertEquals(List.of(), lowered.children(),
                "the #494 node is SHALLOW — no value children this wave (the deep enrichment is a"
                        + " named later wave)");

        IRListConstruct listIr = assertInstanceOf(IRListConstruct.class,
                adapter.adapt(list, ws).orElseThrow(() -> new AssertionError(
                        "a claim-ROOT list with a ctor element must lower via the #526 element"
                                + " admission")),
                "the list lowers whole");
        assertEquals(1, listIr.elements().size(), "the single admitted element rides");
        assertInstanceOf(IRConstruct.class, listIr.elements().get(0),
                "the admitted element is the shallow construct");

        assertTrue(count.argument() instanceof RListLiteral,
                "premise: the count's receiver IS the ctor-bearing list (asserted, not assumed)");
        assertFalse(adapter.adapt(count, ws).isPresent(),
                "a ctor-bearing list under a count receiver must keep DECLINING: the count's"
                        + " argument adapts at a child NodeId outside every admitted chain — the"
                        + " root-only law's boundary, re-pinned one seat out");
        assertEquals(Optional.of("receiverNotExpressible"),
                adapter.declineReason(count, ws),
                "the recut mirror names the receiver BY CALL: the slot lowers at its probe root"
                        + " (the admitted re-adapt) yet declines at the child seat — leg-2 of the"
                        + " #526 slotNotExpressibleAtChild recut");
    }

    @Test
    void conditionalCtorSlotAdmitsAtClaimRootAndAdmittedChains() {
        // #495 — the ctor-slot admission: a claim-ROOT conditional's then-slot constructor is
        // admitted through adaptConstructorShallow, so the whole conditional lowers to
        // IRConditional{then=IRConstruct} (the condVisit probe's blocked.ctorSlot.then class —
        // 320 events, every one an else-less seam claim root). The lock body RETENSED at #526
        // (the lock-lineage law): the list fixture that pinned the child-position decline now
        // LOWERS — the ctor-slot conditional is itself an ADMITTED list element (the #526 glue
        // recursion, the neGate census's elem.condCtorSlot class — the drr
        // Create_CounterpartySpecificData shape) — so the fixture asserts the composed
        // IRListConstruct{IRConditional{then=IRConstruct}} and the chain-external boundary is
        // pinned by the #494 witness's count seat + the #526 wave witness's negatives.
        String source = """
                namespace "test.step495"

                type Party:
                    partyName string (1..1)

                func BuildParty:
                    inputs:
                        flag boolean (1..1)
                    output:
                        result Party (0..1)
                    set result:
                        if flag then Party { partyName: "ACME" }

                func BuildPartyList:
                    inputs:
                        flag boolean (1..1)
                    output:
                        result Party (0..*)
                    set result:
                        [if flag then Party { partyName: "ACME" }]
                """;
        RModel model = AstBuilder.buildFromString(source, "step495-cond-ctor-slot.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RConditionalExpr rootCond = AstWalker.findFirst(ws.files().get(0), RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConditionalExpr"));
        // The bracket list is the ELEMENT-BEARING one — DefaultElseRule stamps a synthetic EMPTY
        // list else onto every else-less conditional, and findFirst would grab that synthetic
        // (which lowers legitimately, the first run's own catch).
        RListLiteral list = AstWalker.findAll(ws.files().get(0), RListLiteral.class).stream()
                .filter(l -> !l.elements().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no bracket list"));

        IRConditional lowered = assertInstanceOf(IRConditional.class,
                adapter.adapt(rootCond, ws).orElseThrow(() -> new AssertionError(
                        "a claim-root conditional with a ctor then-slot must lower via the"
                                + " admission")),
                "the conditional lowers whole");
        IRConstruct thenCtor = assertInstanceOf(IRConstruct.class, lowered.thenBranch(),
                "the admitted then-slot is the shallow construct");
        assertEquals("Party", thenCtor.typeName(), "the slot carries the neutral ctor facts");
        assertEquals(List.of(), thenCtor.children(), "the admitted node stays SHALLOW (#494)");
        assertNull(lowered.elseBranch(), "the corpus class is else-less (all 320 noElse)");
        assertEquals(Optionality.OPTIONAL, lowered.optionality(),
                "no genuine else ⇒ OPTIONAL (the adapter's D5 absorbing rule)");

        IRListConstruct listIr = assertInstanceOf(IRListConstruct.class,
                adapter.adapt(list, ws).orElseThrow(() -> new AssertionError(
                        "a claim-ROOT list whose element is a ctor-slot conditional must lower"
                                + " via the #526 glue recursion")),
                "the list lowers whole");
        assertEquals(1, listIr.elements().size(), "the single admitted element rides");
        IRConditional elemCond = assertInstanceOf(IRConditional.class, listIr.elements().get(0),
                "the admitted element is the conditional, its own then-slot admitted in turn");
        assertInstanceOf(IRConstruct.class, elemCond.thenBranch(),
                "the recursion carries the admission one glue level down (le>ct — the census's"
                        + " own path spelling)");
    }

    @Test
    void lambdaOpLowersDeepAtBothFamilies() {
        // #496 — the monster wave's leg-1 arm: extract and filter lower to the DEEP IRLambdaOp
        // (children [receiver, body] load-bearing, D1 all-or-nothing) with the op flavour, the
        // null binder fact for an implicit lambda, and the documented neutral cardinality/
        // optionality rules (EXTRACT: the D4-style join; FILTER: the receiver's cardinality +
        // OPTIONAL unconditionally). The third fixture pins D1: a body the adapter defers
        // declines the WHOLE lambda, and the mirror names bodyNotExpressible. The lock body
        // RETENSED at #519 (the lock-lineage law): `sum` joined the taught set (every list
        // operator is taught now), so the lock rides a two-numeric-literal equality body
        // instead (`1 = 2` — the sibling-aware licensing gate's standing residue, pinned by
        // its own #518 negative witness; a then-pipe body would NOT do — RThenExpr lowers to
        // the shallow IRPipe since the #500 arm-B pipe teach).
        String source = """
                namespace "test.step496"

                type Party:
                    partyName string (1..1)

                func MapParties:
                    inputs:
                        partyList Party (0..*)
                    output:
                        result Party (0..*)
                    set result:
                        partyList extract item

                func FilterFlags:
                    inputs:
                        flags boolean (0..*)
                    output:
                        result boolean (0..*)
                    set result:
                        flags filter item

                func BlockedBody:
                    inputs:
                        partyList Party (0..*)
                    output:
                        result Party (0..1)
                    set result:
                        partyList extract [ 1 = 2 ] only-element
                """;
        RModel model = AstBuilder.buildFromString(source, "step496-lambda-adapter.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RExtractExpr> extracts = AstWalker.findAll(ws.files().get(0), RExtractExpr.class);
        assertEquals(2, extracts.size(), "the fixture parses two extracts");
        RFilterExpr filter = AstWalker.findFirst(ws.files().get(0), RFilterExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFilterExpr"));

        IRLambdaOp extractIr = assertInstanceOf(IRLambdaOp.class,
                adapter.adapt(extracts.get(0), ws).orElseThrow(() -> new AssertionError(
                        "an extract whose receiver and body both lower must lower DEEP")));
        assertEquals(IRLambdaOp.Op.EXTRACT, extractIr.op(), "the op flavour is the neutral fact");
        assertNull(extractIr.binderName(), "an implicit lambda carries the null binder fact");
        assertEquals(2, extractIr.children().size(),
                "the #496 node is DEEP — receiver and body are load-bearing children");
        assertEquals(ExpressionCardinality.MULTI, extractIr.cardinality(),
                "EXTRACT joins cardinality (a MULTI receiver maps per element)");

        IRLambdaOp filterIr = assertInstanceOf(IRLambdaOp.class,
                adapter.adapt(filter, ws).orElseThrow(() -> new AssertionError(
                        "a filter whose receiver and body both lower must lower DEEP")));
        assertEquals(IRLambdaOp.Op.FILTER, filterIr.op(), "the op flavour is the neutral fact");
        assertEquals(ExpressionCardinality.MULTI, filterIr.cardinality(),
                "FILTER keeps the receiver's cardinality (a predicate never changes multiplicity)");
        assertEquals(Optionality.OPTIONAL, filterIr.optionality(),
                "FILTER is OPTIONAL unconditionally — a predicate can reject every element");

        assertFalse(adapter.adapt(extracts.get(1), ws).isPresent(),
                "a body the adapter defers (the two-literal equality licensing residue) must"
                        + " decline the WHOLE lambda — D1 all-or-nothing, never a half-built node");
        assertEquals(Optional.of("bodyNotExpressible"),
                adapter.declineReason(extracts.get(1), ws),
                "the mirror names the body slot (the arm-mirror law)");
    }

    @Test
    void lambdaBodyCtorSlotAdmitsAtClaimRootAndAdmittedChains() {
        // #496 — the body-slot admission (the #495 conditional-slot pattern at the lambda's body
        // position): a claim-ROOT extract's direct constructor body is admitted through
        // adaptConstructorShallow, so the whole lambda lowers to IRLambdaOp{body=IRConstruct}
        // (the lambdaVisit probe's blocked.ctorSlot.body class — 194 extract events). The lock
        // body RETENSED at #526 (the lock-lineage law): the list fixture that pinned the
        // child-position decline now LOWERS — the ctor-body extract is itself an ADMITTED list
        // element (the #526 glue recursion, the lamCtorBody census class) — so the fixture
        // asserts the composed IRListConstruct{IRLambdaOp{body=IRConstruct}}; the
        // chain-external boundary is pinned by the #494 witness's count seat + the #526 wave
        // witness's negatives.
        String source = """
                namespace "test.step496"

                type Party:
                    partyName string (1..1)

                func BuildParties:
                    inputs:
                        partyList Party (0..*)
                    output:
                        result Party (0..*)
                    set result:
                        partyList extract Party { partyName: "ACME" }

                func BuildPartiesListed:
                    inputs:
                        partyList Party (0..*)
                    output:
                        result Party (0..*)
                    set result:
                        [partyList extract Party { partyName: "ACME" }]
                """;
        RModel model = AstBuilder.buildFromString(source, "step496-lambda-ctor-slot.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RExtractExpr rootExtract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RListLiteral list = AstWalker.findAll(ws.files().get(0), RListLiteral.class).stream()
                .filter(l -> !l.elements().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no bracket list"));

        IRLambdaOp lowered = assertInstanceOf(IRLambdaOp.class,
                adapter.adapt(rootExtract, ws).orElseThrow(() -> new AssertionError(
                        "a claim-root extract with a ctor body must lower via the admission")),
                "the lambda lowers whole");
        IRConstruct bodyCtor = assertInstanceOf(IRConstruct.class, lowered.body(),
                "the admitted body is the shallow construct");
        assertEquals("Party", bodyCtor.typeName(), "the body carries the neutral ctor facts");
        assertEquals(List.of(), bodyCtor.children(), "the admitted node stays SHALLOW (#494)");

        IRListConstruct listIr = assertInstanceOf(IRListConstruct.class,
                adapter.adapt(list, ws).orElseThrow(() -> new AssertionError(
                        "a claim-ROOT list whose element is a ctor-body extract must lower via"
                                + " the #526 glue recursion")),
                "the list lowers whole");
        assertEquals(1, listIr.elements().size(), "the single admitted element rides");
        IRLambdaOp elemLambda = assertInstanceOf(IRLambdaOp.class, listIr.elements().get(0),
                "the admitted element is the lambda, its own body admitted in turn");
        assertInstanceOf(IRConstruct.class, elemLambda.body(),
                "the recursion carries the admission one glue level down (le>eb — the census's"
                        + " own path spelling)");
    }

    @Test
    void notExpressibleGlueChainsAdmitAtClaimRootReachableSeatsOnly() {
        // #526 — the wave witness (the NotExpressible-cluster teach): the root-only law's named
        // admissions made RECURSIVE through glue (conditional slots / extract-filter bodies /
        // list elements), landing the neGate census's four decoded classes: (1) the NESTED
        // ctor-slot conditional branch (thenNotExpressible — the drr NotionalLeg shape), (2)
        // the MIXED direct-ctor-then + chain-else conditional (the census's then.ctor.atRoot
        // rows — the else was the real blocker, the reasonFor first-divergent-slot attribution's
        // one-level preview), (3) the deep extract-body chain (bodyNotExpressible — the drr-R
        // DTCC_UnderlyingAssetReport eb>ce spellings), (4) the multi-element cond-ctor list
        // (elementNotExpressible — the drr Create_CounterpartySpecificData fails:2of2 class).
        // The negatives pin BOTH boundaries: a glue chain under a CHAIN-EXTERNAL parent (the
        // #494 witness's count seat) stays declined, and the #512 ARG-position lambda-body seat
        // stays ctor-ONLY — an arg-extract whose body is a ctor-slot CONDITIONAL keeps the call
        // declining (widening that seat turns this witness RED — the decline-lock
        // witness-uniqueness law).
        String source = """
                namespace "test.step526"

                type Party:
                    partyName string (1..1)

                func NestedCondCtor:
                    inputs:
                        flagA boolean (1..1)
                        flagB boolean (1..1)
                    output:
                        result Party (0..1)
                    set result:
                        if flagA then (if flagB then Party { partyName: "ACME" })

                func MixedThenCtorElseChain:
                    inputs:
                        flagA boolean (1..1)
                        flagB boolean (1..1)
                    output:
                        result Party (0..1)
                    set result:
                        if flagA then Party { partyName: "THEN" }
                        else (if flagB then Party { partyName: "ELSE" })

                func DeepExtractChain:
                    inputs:
                        partyList Party (0..*)
                        flag boolean (1..1)
                    output:
                        result Party (0..*)
                    set result:
                        partyList extract (if flag then Party { partyName: "A" } else (if flag then Party { partyName: "B" }))

                func CondCtorListElements:
                    inputs:
                        flag boolean (1..1)
                    output:
                        result Party (0..*)
                    set result:
                        [(if flag then Party { partyName: "X" }), (if flag then Party { partyName: "Y" })]

                func TakeParties:
                    inputs:
                        ps Party (0..*)
                    output:
                        result Party (0..*)
                    set result:
                        ps

                func ArgSeatStaysCtorOnly:
                    inputs:
                        partyList Party (0..*)
                        flag boolean (1..1)
                    output:
                        result Party (0..*)
                    set result:
                        TakeParties(partyList extract (if flag then Party { partyName: "C" }))
                """;
        RModel model = AstBuilder.buildFromString(source, "step526-glue-chains.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RFunction> fns = AstWalker.findAll(ws.files().get(0), RFunction.class);
        RFunction nestedFn = fns.stream().filter(f -> "NestedCondCtor".equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("NestedCondCtor missing"));
        RFunction mixedFn = fns.stream().filter(f -> "MixedThenCtorElseChain".equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("MixedThenCtorElseChain missing"));
        RFunction deepFn = fns.stream().filter(f -> "DeepExtractChain".equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("DeepExtractChain missing"));
        RFunction listFn = fns.stream().filter(f -> "CondCtorListElements".equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("CondCtorListElements missing"));
        RFunction argFn = fns.stream().filter(f -> "ArgSeatStaysCtorOnly".equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("ArgSeatStaysCtorOnly missing"));

        // (1) the nested ctor-slot branch: cond(then=cond(then=ctor)) lowers whole — the
        // premises asserted per the #525 OBS-3 law (the outer then IS the inner conditional,
        // the inner then IS the constructor).
        RConditionalExpr outer = AstWalker.findFirst(nestedFn, RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("NestedCondCtor parses no conditional"));
        RConditionalExpr inner = assertInstanceOf(RConditionalExpr.class, outer.thenBranch(),
                "premise: the outer then-branch is the nested conditional");
        assertInstanceOf(RConstructorExpr.class, inner.thenBranch(),
                "premise: the inner then-branch is the constructor");
        IRConditional outerIr = assertInstanceOf(IRConditional.class,
                adapter.adapt(outer, ws).orElseThrow(() -> new AssertionError(
                        "a claim-root conditional whose then is a ctor-slot conditional must"
                                + " lower via the #526 glue recursion")));
        IRConditional innerIr = assertInstanceOf(IRConditional.class, outerIr.thenBranch(),
                "the admitted then rides as the nested conditional");
        assertInstanceOf(IRConstruct.class, innerIr.thenBranch(),
                "the recursion admits the ctor one glue level down (ct>ct)");

        // (2) the mixed class: then=DIRECT ctor (admitted at #495), else=chain (the census's
        // then.ctor.atRoot rows — the else was the real pre-teach blocker).
        RConditionalExpr mixed = AstWalker.findFirst(mixedFn, RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("MixedThenCtorElseChain parses no conditional"));
        assertInstanceOf(RConstructorExpr.class, mixed.thenBranch(),
                "premise: the mixed then-branch is the direct constructor");
        assertInstanceOf(RConditionalExpr.class, mixed.elseBranch().orElse(null),
                "premise: the mixed else-branch is the nested conditional");
        IRConditional mixedIr = assertInstanceOf(IRConditional.class,
                adapter.adapt(mixed, ws).orElseThrow(() -> new AssertionError(
                        "the mixed then-ctor + else-chain conditional must lower whole")));
        assertInstanceOf(IRConstruct.class, mixedIr.thenBranch(), "the direct then admits (#495)");
        IRConditional mixedElse = assertInstanceOf(IRConditional.class, mixedIr.elseBranch(),
                "the else chain admits through the recursion (ce>ct)");
        assertInstanceOf(IRConstruct.class, mixedElse.thenBranch(),
                "the else chain bottoms in the admitted ctor");

        // (3) the deep extract-body chain: extract(body=cond(then=ctor, else=cond(then=ctor)))
        // lowers whole — the drr-R eb>ct / eb>ce>ct spellings in one fixture.
        RExtractExpr deep = AstWalker.findFirst(deepFn, RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("DeepExtractChain parses no extract"));
        RConditionalExpr deepBody = assertInstanceOf(RConditionalExpr.class, deep.body().body(),
                "premise: the extract body is the conditional chain");
        assertInstanceOf(RConstructorExpr.class, deepBody.thenBranch(),
                "premise: the chain's then is a constructor");
        assertInstanceOf(RConditionalExpr.class, deepBody.elseBranch().orElse(null),
                "premise: the chain's else is the nested conditional");
        IRLambdaOp deepIr = assertInstanceOf(IRLambdaOp.class,
                adapter.adapt(deep, ws).orElseThrow(() -> new AssertionError(
                        "a claim-root extract whose body is a conditional glue chain must lower"
                                + " via the #526 recursion")));
        IRConditional deepBodyIr = assertInstanceOf(IRConditional.class, deepIr.body(),
                "the admitted body rides as the conditional");
        assertInstanceOf(IRConstruct.class, deepBodyIr.thenBranch(), "eb>ct admits");
        IRConditional deepElseIr = assertInstanceOf(IRConditional.class, deepBodyIr.elseBranch(),
                "eb>ce admits the nested conditional");
        assertInstanceOf(IRConstruct.class, deepElseIr.thenBranch(), "eb>ce>ct bottoms in the ctor");

        // (4) the multi-element cond-ctor list (the fails:2of2 census class).
        RListLiteral condList = AstWalker.findAll(listFn, RListLiteral.class).stream()
                .filter(l -> !l.elements().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("CondCtorListElements parses no bracket list"));
        assertEquals(2, condList.elements().size(), "premise: the bracket list carries BOTH elements");
        IRListConstruct condListIr = assertInstanceOf(IRListConstruct.class,
                adapter.adapt(condList, ws).orElseThrow(() -> new AssertionError(
                        "a claim-root list of ctor-slot conditionals must lower whole")));
        assertEquals(2, condListIr.elements().size(), "both admitted elements ride");
        for (IRExpr element : condListIr.elements()) {
            IRConditional elementIr = assertInstanceOf(IRConditional.class, element,
                    "each admitted element is a conditional (le>ct)");
            assertInstanceOf(IRConstruct.class, elementIr.thenBranch(),
                    "each element's chain bottoms in the admitted ctor");
        }

        // (5) the #512 arg-seat boundary: the ARG-position lambda-body admission stays
        // ctor-ONLY — an arg-extract whose body is a ctor-slot CONDITIONAL keeps the whole
        // call declining (the seat's reachable set is exact; widening it turns this RED).
        RSymbolReference call = AstWalker.findAll(argFn, RSymbolReference.class).stream()
                .filter(r -> !r.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("ArgSeatStaysCtorOnly parses no call"));
        RExtractExpr argExtract = assertInstanceOf(RExtractExpr.class, call.args().get(0),
                "premise: the call's argument is the extract");
        assertInstanceOf(RConditionalExpr.class, argExtract.body().body(),
                "premise: the arg-extract's body is a CONDITIONAL (not a direct ctor — the"
                        + " ARG_CTOR_ONLY boundary's own shape)");
        assertFalse(adapter.adapt(call, ws).isPresent(),
                "the call must keep DECLINING: the #512 arg-position body seat admits a DIRECT"
                        + " constructor only — the #526 glue recursion deliberately does NOT"
                        + " reach through the call-arg seat");
    }

    @Test
    void syntheticRuleInputRefLowersToParamVariable() {
        // #497 (the conversion cluster's seat-2 arm): a bare reference whose symbol is the
        // rule/report wrapper's ORPHAN synthetic `input` attribute (name "input", parent never
        // wired, typeCall present — the RFunction.fromRule/fromReport factory shape) lowers to
        // the PARAM variable leaf — legacy's ladder variable-paths the class
        // position-independently to MapperS.of(input), the proven Wave-0 emission. The three
        // gate fixtures pin the identity's decline-locks: a PARENTED attribute named "input"
        // takes the standard ladder (not this arm), and a typeCall-less orphan declines (the
        // from-less edge whose render could not compile).
        RModel model = AstBuilder.buildFromString("namespace \"test.step497\"",
                "step497-input.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        RTypeCall fromType = new RTypeCall();
        fromType.setTypeName("ReportableEvent");
        RAttribute orphanInput = new RAttribute();
        orphanInput.setName("input");
        orphanInput.setTypeCall(fromType);
        RCardinality card = new RCardinality();
        card.setInf(0);
        card.setSup(1);
        orphanInput.setCardinality(card);
        RSymbolReference ref = new RSymbolReference();
        ref.setName("input");
        ref.setResolvedSymbol(orphanInput);
        ref.attachToWorkspace(ws);

        IRVariable lowered = assertInstanceOf(IRVariable.class,
                adapter.adapt(ref, ws).orElseThrow(() -> new AssertionError(
                        "the orphan synthetic input must lower to the PARAM variable")));
        assertEquals("input", lowered.name(), "the render identifier is the rule's own parameter");
        assertEquals(IRVariable.VariableKind.PARAM, lowered.variableKind(),
                "the PARAM kind — the MapperS.of(name) Wave-0 render");
        assertEquals(ExpressionCardinality.SINGLE, lowered.cardinality(),
                "the factory's (0..1) declared cardinality");
        assertEquals(Optionality.OPTIONAL, lowered.optionality(),
                "the (0..1) lower bound — OPTIONAL by the declared-cardinality convention");

        // Decline-lock 1: a typeCall-less orphan (the from-less edge) must NOT take the arm.
        RAttribute typelessOrphan = new RAttribute();
        typelessOrphan.setName("input");
        RCardinality card2 = new RCardinality();
        card2.setInf(0);
        card2.setSup(1);
        typelessOrphan.setCardinality(card2);
        RSymbolReference typelessRef = new RSymbolReference();
        typelessRef.setName("input");
        typelessRef.setResolvedSymbol(typelessOrphan);
        typelessRef.attachToWorkspace(ws);
        // #528 arm-5 RETENSE, re-tensed at the fix commit: the RULE-INPUT arm still declines
        // it (prove-or-decline on the from-typed shape is untouched — that ladder is outside
        // the mint's census families), and the bare-attr fall-through mint now declines it
        // TOO — a typeCall-less orphan is exactly the untypeable carrier the mint's
        // #514-pattern type guard exists for (the Copilot #528 R1 catch: minting it would
        // hand the serialized channel a null type). The ORIGINAL #497 decline-lock returns
        // in its strongest form: neither the rule-input PARAM path nor a null-typed mint.
        assertTrue(adapter.adapt(typelessRef, ws).isEmpty(),
                "the typeless orphan declines — the rule-input arm's own gate AND the mint's"
                        + " type guard (never the PARAM variable, never a null-typed mint)");

        // Decline-lock 2: a PARENTED attribute named "input" is a genuine model attribute and
        // must take the standard ladder, never this arm (here: parented to a data type, outside
        // any admitting context — the ladder declines it, proving the arm did not fire on name
        // alone).
        RDataType owner = new RDataType();
        owner.setName("SomeType");
        RAttribute parentedInput = new RAttribute();
        parentedInput.setName("input");
        parentedInput.setTypeCall(fromType);
        RCardinality card3 = new RCardinality();
        card3.setInf(0);
        card3.setSup(1);
        parentedInput.setCardinality(card3);
        parentedInput.setParent(owner);
        RSymbolReference parentedRef = new RSymbolReference();
        parentedRef.setName("input");
        parentedRef.setResolvedSymbol(parentedInput);
        parentedRef.attachToWorkspace(ws);
        // #528 arm-5 RETENSE (the same class as decline-lock 1 above), re-tensed at the fix
        // commit: the standard ladder is still what runs — the rule-input arm does NOT fire
        // on the name — and the ladder's mint tail declines this fixture too: its typeCall
        // names a type this bare workspace cannot resolve, so the attribute channel cannot
        // type the mint (the same #514-pattern guard). The ORIGINAL #497 decline returns:
        // a rule-input claim would have been the PARAM variable, and no such claim fired.
        assertTrue(adapter.adapt(parentedRef, ws).isEmpty(),
                "a parented attribute named 'input' takes the standard ladder and declines —"
                        + " the orphan identity, never the name, is the arm's discriminator");

        // Decline-lock 3 (the Seat-1 #528 OBS-3 narrowing, added at the fix commit): the
        // L-113 mint is CENSUS-NARROW to the attr-outside-function LEG — the #497 census
        // faces are attrOutsideFunction.* spellings — so a FUNCTION-parented attribute
        // (here: a shadow bind — parented, neither an input nor the output) keeps its
        // standing decline even though its binder walk reads a mint family
        // (noFilterExtractBinder), and the mirror names the leg's own standing face rather
        // than dropping it. Pre-narrowing the mint claimed this shape (and, one seat over,
        // bypassed the #514 ARM-1 untyped-output decline); this lock is the over-wide
        // direction's tripwire.
        RFunction shadowHost = new RFunction();
        shadowHost.setName("ShadowHost");
        RAttribute shadowAttr = new RAttribute();
        shadowAttr.setName("shadow");
        shadowAttr.setTypeCall(fromType);
        RCardinality card4 = new RCardinality();
        card4.setInf(0);
        card4.setSup(1);
        shadowAttr.setCardinality(card4);
        shadowAttr.setParent(shadowHost);
        RSymbolReference shadowRef = new RSymbolReference();
        shadowRef.setName("shadow");
        shadowRef.setResolvedSymbol(shadowAttr);
        shadowRef.attachToWorkspace(ws);
        assertTrue(adapter.adapt(shadowRef, ws).isEmpty(),
                "a function-parented shadow bind declines — the mint is census-narrow to the"
                        + " attr-outside-function leg (the OBS-3 narrowing)");
        assertTrue(adapter.declineReason(shadowRef, ws).orElseThrow()
                        .startsWith("notAnInputParam.shadow"),
                "the mirror names the leg's standing face — the mint-family drop applies"
                        + " only at the attr-outside-function leg (twin lockstep)");
    }

    @Test
    void onlyElementAndSumLowerToListOpSinceThe519Teach() {
        // #498 leg A: `<recv> only-element` lowers to IRListOp{ONLY_ELEMENT} over the lowered
        // receiver child — the #498 census read the whole gate population typed with universally
        // lowering receivers, and the render stays the CollectionHandler oracle verbatim (the
        // inline `.get()` collapse + its selfUnwrapping consumer-marker ride the oracle's own
        // JavaExpression). The former sum decline-lock RETENSED at #519 (the lock-lineage law):
        // `sum` joined the taught set — the SAME receiver shape now lowers to IRListOp{SUM}
        // (the opSum face retired, the RListOpExpr family whole), and the render is
        // oracle-only on every route (a top-level sum keeps the standing CollectionOpRenderer
        // slot; an interior sum is an oracle leaf — the SUM-scoped containsOracleLeaf leg).
        String source = """
                namespace "test.step498"

                type Trade:
                    legs number (0..*)

                func Collapse:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs only-element

                func Total:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs sum

                func PipedTotal:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs then sum
                """;
        RModel model = AstBuilder.buildFromString(source, "step498-onlyelement.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RListOpExpr> ops = AstWalker.findAll(ws.files().get(0), RListOpExpr.class);
        RListOpExpr collapse = ops.stream().filter(o -> o.op() == ListOp.ONLY_ELEMENT).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no only-element"));
        List<RListOpExpr> sums = ops.stream().filter(o -> o.op() == ListOp.SUM).toList();
        assertEquals(2, sums.size(), "the fixture parses two sums (explicit + piped receiver)");
        RListOpExpr sum = sums.get(0);

        IRListOp lowered = assertInstanceOf(IRListOp.class,
                adapter.adapt(collapse, ws).orElseThrow(() -> new AssertionError(
                        "an only-element over a lowering receiver must lower since the #498 teach")),
                "the collapse lowers to the flat list-op node");
        assertEquals(IRListOp.Kind.ONLY_ELEMENT, lowered.op(), "the taught op");
        assertInstanceOf(FieldAccess.class, lowered.child(),
                "the receiver child is the lowered navigation — load-bearing, oracle-recursed");

        IRListOp sumLowered = assertInstanceOf(IRListOp.class,
                adapter.adapt(sum, ws).orElseThrow(() -> new AssertionError(
                        "a sum over a lowering receiver must lower since the #519 teach")),
                "the sum lowers to the flat list-op node");
        assertEquals(IRListOp.Kind.SUM, sumLowered.op(), "the #519-taught op");
        assertInstanceOf(FieldAccess.class, sumLowered.child(),
                "the receiver child is the lowered navigation — load-bearing; the render is"
                        + " the oracle on every route (no native sum arm exists)");

        // The dominant live class: `… then sum` — the list-op builder synthesizes the
        // implicit receiver (syntheticImplicitInput), which lowers unconditionally to a
        // SYNTHETIC_ITEM at the RImplicitVariable arm; the sum claims its own node whatever
        // the containing then does (the then has no adapt arm — its claim is untouched).
        IRListOp pipedLowered = assertInstanceOf(IRListOp.class,
                adapter.adapt(sums.get(1), ws).orElseThrow(() -> new AssertionError(
                        "a piped-receiver sum must lower since the #519 teach")),
                "the piped sum lowers to the flat list-op node");
        assertEquals(IRListOp.Kind.SUM, pipedLowered.op(), "the #519-taught op");
        IRVariable pipedReceiver = assertInstanceOf(IRVariable.class, pipedLowered.child(),
                "the receiver child is the parser-synthesized implicit");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, pipedReceiver.variableKind(),
                "a parser-synthesized implicit lowers SYNTHETIC_ITEM (the #479 convention)");
    }

    @Test
    void argsPresentRuleInvocationLowersToRuleApply() {
        // #498 leg B: `<Rule>(<arg>)` — an args-present reference resolving to an RRule (DRR's
        // rule-as-function invocation; the census read the calleeNotFunction gate 100% RRule at
        // arg1) — lowers on adaptApply's rule leg to IRApply{callee=IRReference{RULE}} over the
        // adapted argument, the callee facts read through the SAME RFunction.fromRule bridge
        // legacy's receiver derivation uses. The decline-lock: an inadmissible argument (`empty` —
        // the argEmpty class) keeps the reference declining, proving the shared arg gates run on
        // the rule leg too.
        String source = """
                namespace "test.step498b"

                type Trade:
                    tradeId string (1..1)

                reporting rule IdRule from Trade:
                    extract tradeId

                func Wrap:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (1..1)
                    set result:
                        IdRule(trade)

                func WrapEmpty:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (1..1)
                    set result:
                        IdRule(empty)
                """;
        RModel model = AstBuilder.buildFromString(source, "step498-ruleapply.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RSymbolReference> calls = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> !r.args().isEmpty()).toList();
        assertEquals(2, calls.size(), "the fixture parses both args-present rule invocations");
        RSymbolReference call = calls.stream()
                .filter(r -> r.args().get(0) instanceof RSymbolReference).findFirst()
                .orElseThrow(() -> new AssertionError("no param-arg invocation parsed"));
        RSymbolReference emptyCall = calls.stream()
                .filter(r -> r.args().get(0) instanceof REmptyLiteral).findFirst()
                .orElseThrow(() -> new AssertionError("no empty-arg invocation parsed"));
        assertInstanceOf(RRule.class, call.symbol().orElseThrow(),
                "the callee must resolve to the reporting rule — the census's symbolKind");

        IRApply lowered = assertInstanceOf(IRApply.class,
                adapter.adapt(call, ws).orElseThrow(() -> new AssertionError(
                        "an args-present rule invocation must lower since the #498 rule leg")),
                "the invocation lowers to the apply node");
        IRReference callee = assertInstanceOf(IRReference.class, lowered.callee(),
                "the callee reference");
        assertEquals(IRReference.ReferenceKind.RULE, callee.referenceKind(),
                "the bare-delegation arm's own callee kind, now with args");
        assertEquals("IdRule", callee.target(), "the rule's simple name — the bare arm's convention");
        assertEquals(1, lowered.args().size(), "the census's universal arg1 shape");
        assertInstanceOf(IRVariable.class, lowered.args().get(0),
                "the argument is the lowered input param — the #497-lowered class the freed"
                        + " claims re-landed from");

        // RECUT at #504 (the pin-recut law — the pin's own named class taught): the empty
        // argument now ADMITS through the shared call-arg gates (the arm-C2 argEmpty teach —
        // the lowered IREmptyLiteral IS the faithful arg; the containing claim root renders
        // WHOLE through the oracle-root callArgs serve via the apply-with-an-empty-arg SHAPE
        // leg, so legacy's own null-threading renders the slot). The rule leg shares the
        // gates, so the admission runs here too.
        IRApply emptyLowered = assertInstanceOf(IRApply.class,
                adapter.adapt(emptyCall, ws).orElseThrow(() -> new AssertionError(
                        "an empty-arg invocation must lower since the #504 arm-C2 admission")),
                "the empty-arg invocation lowers to the apply node");
        assertInstanceOf(IREmptyLiteral.class, emptyLowered.args().get(0),
                "the argument is the lowered empty literal — the faithful arg the oracle"
                        + " serve renders");
        assertTrue(adapter.declineReason(emptyCall, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the admission");
    }

    @Test
    void navOverCollapseLowersToFieldAccessWithLastDeclineLock() {
        // #499 arm A: `<chain> only-element -> <feature>` — a navigation whose receiver is an
        // ONLY_ELEMENT collapse over a param/nav child (the #498-exposed receiver:IRListOp
        // frontier; the census read the population 100% typed, kinds onlyElement+first only,
        // child shapes param/fieldAccess dominant) — lowers to FieldAccess{receiver=IRListOp}.
        // The emitter re-wraps the bare-item collapse `MapperS.of(<recv>.get())` exactly as
        // legacy nav_after_get_rewrap does. RECUT at #511 (the pin-recut law — the lock's own
        // named class taught): the SAME navigation over a `last` collapse now ALSO lowers —
        // the kind-wide receiver admission ships NO native render for the excluded class (the
        // #499 lock's "unverifiable render" reasoning dissolves): the containing claim root
        // routes through the compiler's oracle-root serve off the emitter's own
        // isCollapseNavBase boundary (byte-identical BY IDENTITY), and the receiver:IRListOp
        // token is now a drift face at its seat. The two fixtures now form the ROUTE contrast:
        // the ONLY_ELEMENT nav is the #499 native-render class, the LAST nav the #511
        // oracle-served class — both lower to the same FieldAccess{receiver=IRListOp} shape.
        String source = """
                namespace "test.step499a"

                type Leg:
                    rate number (1..1)

                type Trade:
                    legs Leg (0..*)

                func NavOverCollapse:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs only-element -> rate

                func NavOverLast:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs last -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step499-navovercollapse.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RFeatureCall> navs = AstWalker.findAll(ws.files().get(0), RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RListOpExpr).toList();
        assertEquals(2, navs.size(), "the fixture parses both collapse-receiver navigations");
        RFeatureCall overOnlyElement = navs.stream()
                .filter(fc -> ((RListOpExpr) fc.receiver()).op() == ListOp.ONLY_ELEMENT)
                .findFirst().orElseThrow();
        RFeatureCall overLast = navs.stream()
                .filter(fc -> ((RListOpExpr) fc.receiver()).op() == ListOp.LAST)
                .findFirst().orElseThrow();

        FieldAccess lowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(overOnlyElement, ws).orElseThrow(() -> new AssertionError(
                        "a nav over an only-element collapse must lower since the #499 admission")),
                "the navigation lowers with the collapse as its receiver child");
        IRListOp recv = assertInstanceOf(IRListOp.class, lowered.receiver(),
                "the receiver child is the lowered collapse — load-bearing");
        assertEquals(IRListOp.Kind.ONLY_ELEMENT, recv.op(), "the admitted kind");
        assertInstanceOf(FieldAccess.class, recv.child(),
                "the collapse's own child is the L-051 param/nav byte-equivalence class");

        FieldAccess lastLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(overLast, ws).orElseThrow(() -> new AssertionError(
                        "a nav over a LAST collapse must lower since the #511 kind-wide"
                                + " receiver admission")),
                "the navigation lowers with the excluded-kind collapse as its receiver child");
        IRListOp lastRecv = assertInstanceOf(IRListOp.class, lastLowered.receiver(),
                "the receiver child is the lowered collapse — load-bearing");
        assertEquals(IRListOp.Kind.LAST, lastRecv.op(),
                "the #511 admission is kind-wide — the render is the oracle serve BY IDENTITY,"
                        + " never a native teach for the excluded kind");
        assertTrue(adapter.declineReason(overLast, ws).isEmpty(),
                "lowers ⟺ empty reason — the twin restates the admission");
    }

    @Test
    void metaNavLowersToIRMetaAccessWithRecursiveCascade() {
        // #499 arm B: `<recv> -> <metaFeature>` — the L-109d relabel belt's convertible slice —
        // lowers to the DISTINCT IRMetaAccess kind whenever the receiver lowers (the render stays
        // the literal super.visitFeatureCall oracle at the claim root, byte-identical BY
        // IDENTITY). The cascade witness: a meta hop OVER a meta hop lowers bottom-up through
        // the same arm (the census's recvCascade class, 111 events), the inner node the outer's
        // receiver. Hand-built nodes (the file's fixture style): a parsed 2-name meta chain
        // takes the grammar's DISGUISED REnumValueRef shape, whose meta head stays honestly on
        // the belt (the census's recvBlocked.REnumValueRef faces) — the lowering-receiver class
        // this witness pins is the genuine RFeatureCall chain. The distinct-kind law: no native
        // consumer admits IRMetaAccess (the receiver-blocked residue face is pinned in
        // declineReasonNamesSymbolCallAndFeatureCallGates).
        RWorkspace ws = RWorkspace.build(List.of()).workspace();
        RAttribute party = scalarParam("party");
        functionWith(ws, party);
        RAttribute partyIdAttr = scalarParam("partyId");
        RAnnotationRef refQualifier = new RAnnotationRef();
        refQualifier.setAnnotationName("metadata");
        refQualifier.setQualifierName("reference");
        partyIdAttr.annotationRefs().add(refQualifier);
        RFeatureCall inner = featureCall("party", party, "partyId", partyIdAttr, ws);
        RAttribute idValueAttr = scalarParam("idValue");
        RAnnotationRef schemeQualifier = new RAnnotationRef();
        schemeQualifier.setAnnotationName("metadata");
        schemeQualifier.setQualifierName("scheme");
        idValueAttr.annotationRefs().add(schemeQualifier);
        RFeatureCall outer = new RFeatureCall();
        outer.setReceiver(inner);
        outer.setFeatureName("idValue");
        outer.setResolvedFeature(idValueAttr);
        outer.attachToWorkspace(ws);

        IRMetaAccess lowered = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(outer, ws).orElseThrow(() -> new AssertionError(
                        "a meta nav whose receiver lowers must lower since the #499 meta arm")),
                "the meta hop lowers to the distinct meta-access kind");
        assertEquals("idValue", lowered.feature(), "the hop's feature name");
        assertEquals(List.of("scheme"), lowered.metaQualifiers(),
                "the sorted metadata qualifier payload — the render-class discriminant");
        IRMetaAccess innerHop = assertInstanceOf(IRMetaAccess.class, lowered.receiver(),
                "the RECURSIVE cascade: the inner meta hop lowers bottom-up through the same arm"
                        + " and serves as the outer's receiver — the census's recvCascade class");
        assertEquals("partyId", innerHop.feature(), "the inner hop's feature");
        assertEquals(List.of("reference"), innerHop.metaQualifiers(),
                "the inner hop's own qualifier payload");
    }

    @Test
    void navOverMetaHopLowersWithMultiOperandDeclineLock() {
        // #500 arm A (recv seat): a PLAIN hop over a lowered meta hop — the #499-exposed
        // receiver:IRMetaAccess frontier (762 node-unit, 100% RFeatureCall consumers) — lowers
        // to FieldAccess{receiver=IRMetaAccess} through the widened isNavigableReceiver (the
        // render is the compiler's oracle-root serve — the literal super.visitFeatureCall — so
        // legacy's meta-typed receiver "Type coercion" deref stays legacy's own). Hand-built
        // nodes (the #499 fixture style — a parsed 2-name meta chain takes the grammar's
        // disguised REnumValueRef shape). RECUT at #510 (arm-A1 — the refine-in-place
        // precedent): the #500 MULTI decline was the admission's CENSUS-NARROW edge (202
        // single events live then, zero MULTI), not a byte-safety lock — the whole-oracle
        // routing is cardinality-agnostic (IRMetaAccess is THE original leaf, so the
        // containing root always serves the literal super.visitEquality line — legacy's own
        // All/Any default + MapperC join inside it). The #509 SOT's operand:IRMetaAccess 78
        // IS this residue; the decline flips to an acceptance.
        RWorkspace ws = RWorkspace.build(List.of()).workspace();
        RAttribute party = scalarParam("party");
        functionWith(ws, party);
        RAttribute partyIdAttr = scalarParam("partyId");
        RAnnotationRef refQualifier = new RAnnotationRef();
        refQualifier.setAnnotationName("metadata");
        refQualifier.setQualifierName("reference");
        partyIdAttr.annotationRefs().add(refQualifier);
        RFeatureCall metaHop = featureCall("party", party, "partyId", partyIdAttr, ws);
        RAttribute nameAttr = scalarParam("name");
        RFeatureCall plainHop = hop(metaHop, "name", nameAttr, ws);

        FieldAccess lowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(plainHop, ws).orElseThrow(() -> new AssertionError(
                        "a plain hop over a lowered meta hop must lower since the #500 admission")),
                "the consuming navigation lowers with the meta hop as its receiver child");
        IRMetaAccess recv = assertInstanceOf(IRMetaAccess.class, lowered.receiver(),
                "the receiver child is the lowered meta hop — load-bearing");
        assertEquals("partyId", recv.feature(), "the meta hop's feature");
        assertEquals("name", lowered.feature(), "the consuming hop's own feature");

        RAttribute multiMetaAttr = multiParam("partyIds");
        RAnnotationRef multiQualifier = new RAnnotationRef();
        multiQualifier.setAnnotationName("metadata");
        multiQualifier.setQualifierName("reference");
        multiMetaAttr.annotationRefs().add(multiQualifier);
        RFeatureCall multiMetaHop = featureCall("party", party, "partyIds", multiMetaAttr, ws);
        RAttribute other = scalarParam("other");
        functionWith(ws, other);
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(multiMetaHop);
        eq.setRight(paramRef("other", other, ws));
        eq.attachToWorkspace(ws);
        BinaryOp multiEq = assertInstanceOf(BinaryOp.class,
                adapter.adapt(eq, ws).orElseThrow(() -> new AssertionError(
                        "a MULTI meta-hop equality operand must admit since the #510 arm-A1")),
                "the recut: the MULTI meta operand admits — the oracle serve owns the"
                        + " cardinality semantics");
        assertInstanceOf(IRMetaAccess.class, multiEq.left(),
                "the MULTI meta hop lowers in place as the operand");
        assertEquals(Optional.empty(), adapter.declineReason(eq, ws),
                "the twin restates the admission (empty ⟺ lowers)");
    }

    @Test
    void metaOperandLowersInEqualityAndExistence() {
        // #500 arm A (operand seat): a SINGLE meta hop as an equality operand (202 node-unit at
        // the census) and as an existence operand (233 — cardinality-agnostic) both lower — the
        // claim roots render through the compiler's oracle-root serve (the literal
        // super.visitEquality / super.visitExistence lines), so legacy's comparison meta-strip
        // and wrapper-typed operand semantics stay legacy's own.
        RWorkspace ws = RWorkspace.build(List.of()).workspace();
        RAttribute party = scalarParam("party");
        RAttribute other = scalarParam("other");
        functionWith(ws, party, other);
        RAttribute schemeAttr = scalarParam("id");
        RAnnotationRef schemeQualifier = new RAnnotationRef();
        schemeQualifier.setAnnotationName("metadata");
        schemeQualifier.setQualifierName("scheme");
        schemeAttr.annotationRefs().add(schemeQualifier);

        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(featureCall("party", party, "id", schemeAttr, ws));
        eq.setRight(paramRef("other", other, ws));
        eq.attachToWorkspace(ws);
        BinaryOp loweredEq = assertInstanceOf(BinaryOp.class,
                adapter.adapt(eq, ws).orElseThrow(() -> new AssertionError(
                        "an equality over a SINGLE meta operand must lower since the #500 admission")),
                "the equality lowers with the meta hop as an operand child");
        assertInstanceOf(IRMetaAccess.class, loweredEq.left(),
                "the left operand is the lowered meta hop — load-bearing");

        RExistenceExpr exists = new RExistenceExpr();
        exists.setOp(ExistenceOp.EXISTS);
        exists.setArgument(featureCall("party", party, "id", schemeAttr, ws));
        exists.attachToWorkspace(ws);
        Existence loweredExists = assertInstanceOf(Existence.class,
                adapter.adapt(exists, ws).orElseThrow(() -> new AssertionError(
                        "an existence over a meta operand must lower since the #500 admission")),
                "the existence lowers with the meta hop as its operand child");
        assertInstanceOf(IRMetaAccess.class, loweredExists.arg(),
                "the existence operand is the lowered meta hop — load-bearing");
    }

    @Test
    void conversionLowersDeepWithArgDeclineLock() {
        // #500 arm B (the conversion teach): `<arg> to-enum <Target>` — the census read the
        // family 954-of-956 to-enum with the argument lowering in 946 — lowers to the DEEP
        // IRConversion carrying the lowered argument child + the kind/target facts (the render
        // is the compiler's oracle-root serve — the literal super.visitConversion). The
        // decline-lock: a conversion whose argument does NOT lower (a `reduce` receiver — the
        // still-unarmed family; recut from `sort` at the #513 noAdaptArm sweep and from
        // `join` at the #515 untargeted close — reduce + cardinality-check are the last two
        // noAdaptArm families) keeps declining with the arm's own argNotExpressible face.
        String source = """
                namespace "test.step500b"

                enum Colour:
                    RED
                    BLUE

                func ConvClaim:
                    inputs:
                        colourCode string (1..1)
                    output:
                        result Colour (1..1)
                    set result:
                        colourCode to-enum Colour

                func ConvDecline:
                    inputs:
                        codes string (0..*)
                    output:
                        result Colour (0..*)
                    set result:
                        (codes reduce acc, v [ acc + v ]) to-enum Colour
                """;
        RModel model = AstBuilder.buildFromString(source, "step500-conversion.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RConversionExpr> convs = AstWalker.findAll(ws.files().get(0), RConversionExpr.class);
        assertEquals(2, convs.size(), "the fixture parses both to-enum conversions");
        RConversionExpr claim = convs.stream()
                .filter(c -> c.argument() instanceof RSymbolReference).findFirst().orElseThrow();
        RConversionExpr decline = convs.stream()
                .filter(c -> !(c.argument() instanceof RSymbolReference)).findFirst().orElseThrow();

        IRConversion lowered = assertInstanceOf(IRConversion.class,
                adapter.adapt(claim, ws).orElseThrow(() -> new AssertionError(
                        "a to-enum over a lowering argument must lower since the #500 arm")),
                "the conversion lowers to the deep node");
        assertEquals("ENUM", lowered.conversionKind(), "the neutral operator token");
        assertEquals("Colour", lowered.targetTypeName(), "the resolved target enum's name");
        assertInstanceOf(IRVariable.class, lowered.child(),
                "the argument child is load-bearing — the lowered input param");

        assertFalse(adapter.adapt(decline, ws).isPresent(),
                "a non-lowerable argument (the reduce family — noAdaptArm) keeps the conversion"
                        + " declining — the census's ~10-event residue");
        assertEquals(Optional.of("argNotExpressible"), adapter.declineReason(decline, ws),
                "the arm's own decline face");
    }

    @Test
    void thenPipeAndOnlyExistsLowerShallowWithBinderDeclineLock() {
        // #500 arm B (the two shallow teaches): a `then` chain root lowers to the shallow
        // IRPipe (census-narrow: implicit-bare binders — 100% of the 582-event population) and
        // an `only exists` root lowers to the shallow IROnlyExists (197-of-200
        // receiver-expression paths) — both render through the compiler's oracle-root serve
        // (the literal super.visitThen / super.visitOnlyExists lines). The decline-lock: a
        // hand-built EXPLICIT-binder then keeps declining with the arm's pipeBinderForm face
        // (zero live events at the census — the drift face).
        String source = """
                namespace "test.step500c"

                type Details:
                    a string (0..1)
                    b string (0..1)

                func PipeClaim:
                    inputs:
                        xs number (0..*)
                    output:
                        result number (1..1)
                    set result:
                        xs then only-element

                func OnlyExistsClaim:
                    inputs:
                        details Details (1..1)
                    output:
                        result boolean (1..1)
                    set result:
                        details -> a only exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step500-shallow.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RThenExpr then = AstWalker.findAll(ws.files().get(0), RThenExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no then parsed"));
        ROnlyExistsExpr onlyExists = AstWalker.findAll(ws.files().get(0), ROnlyExistsExpr.class)
                .stream().findFirst().orElseThrow(() -> new AssertionError("no only-exists parsed"));

        IRPipe pipe = assertInstanceOf(IRPipe.class,
                adapter.adapt(then, ws).orElseThrow(() -> new AssertionError(
                        "an implicit-bare then chain must lower since the #500 arm")),
                "the chain lowers to the shallow pipe node");
        assertEquals(1, pipe.spineLength(), "the single-link chain's spine fact");

        IROnlyExists oe = assertInstanceOf(IROnlyExists.class,
                adapter.adapt(onlyExists, ws).orElseThrow(() -> new AssertionError(
                        "an only-exists root must lower since the #500 arm")),
                "the check lowers to the shallow node");
        assertEquals(1, oe.pathCount(), "the single-path fact");

        RThenExpr explicitBinder = new RThenExpr();
        explicitBinder.setArgument(then.argument());
        RInlineFunction named = new RInlineFunction();
        named.setImplicit(false);
        explicitBinder.setBody(named);
        explicitBinder.attachToWorkspace(ws);
        assertFalse(adapter.adapt(explicitBinder, ws).isPresent(),
                "an explicit-binder then keeps declining — census-narrow (zero live events)");
        assertEquals(Optional.of("pipeBinderForm"), adapter.declineReason(explicitBinder, ws),
                "the arm's own drift face");
    }

    @Test
    void shallowConsumerAdmissionsLowerLogicalAndCallArgRoots() {
        // #501 arm A: the #500-exposed shallow-kind consumer faces — a lowered only-exists check
        // joins the LOGICAL operand gate (producesComparisonResult; operand:IROnlyExists was the
        // top face, 246 sole / 100% RLogicalExpr) and a lowered conversion joins the CALL-ARG
        // gate (isSimpleCallArg; arg:IRConversion 28 / 100% FUNCTION callees). The roots lower;
        // the render is the containing root's oracle-root serve (byte-identity locked by the
        // compiler-side twin). The decline-lock: an operand kind OUTSIDE the admitted set (a
        // list literal) keeps its generic token — the gate stays census-narrow.
        String source = """
                namespace "test.step501a"

                enum PeriodEnum:
                    D
                    M

                type Details:
                    a string (0..1)

                type Trade:
                    details Details (1..1)
                    active boolean (1..1)

                func TakeEnum:
                    inputs:
                        p PeriodEnum (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        True

                func LogicalOverOnlyExists:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result boolean (1..1)
                    set result:
                        trade -> details -> a only exists and trade -> active exists

                func ConvArgCall:
                    inputs:
                        s string (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        TakeEnum(s to-enum PeriodEnum)

                func ToStringOperand:
                    inputs:
                        n number (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        n to-string = "x"
                """;
        RModel model = AstBuilder.buildFromString(source, "step501-shallow-consumers.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RLogicalExpr logical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no logical parsed"));
        BinaryOp loweredLogical = assertInstanceOf(BinaryOp.class,
                adapter.adapt(logical, ws).orElseThrow(() -> new AssertionError(
                        "a logical over a lowered only-exists must lower since the #501 admission")),
                "the root lowers to the boolean BinaryOp");
        assertInstanceOf(IROnlyExists.class, loweredLogical.left(),
                "the left operand is the admitted shallow only-exists node");
        assertTrue(adapter.declineReason(logical, ws).isEmpty(),
                "empty ⟺ lowers: the shared predicate auto-syncs the twin");

        RSymbolReference convCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "TakeEnum".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no TakeEnum call parsed"));
        IRApply loweredCall = assertInstanceOf(IRApply.class,
                adapter.adapt(convCall, ws).orElseThrow(() -> new AssertionError(
                        "a call with a lowered conversion arg must lower since the #501 admission")),
                "the call lowers to IRApply");
        assertInstanceOf(IRConversion.class, loweredCall.args().get(0),
                "the arg is the admitted deep conversion node");

        // RECUT at #503 (the pin-recut law — this WAS the #501 census-narrow lock, and the
        // face it locked is now the taught gate): an equality over a lowered to-string
        // ADMITS through the widened isEqualityOperand (the arm-A2 teach), lowers to
        // BinaryOp, and the containing root oracle-serves through the equality dispatch
        // (IRToString joined containsOracleLeaf — the emitter's nested-IRToString decline
        // stays the native-compose choke point).
        REqualityExpr toStringEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream().findFirst().orElseThrow(() -> new AssertionError("no equality parsed"));
        BinaryOp toStrLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(toStringEq, ws).orElseThrow(() -> new AssertionError(
                        "an equality over a lowered to-string must lower since the #503"
                                + " admission")),
                "the equality lowers with the to-string operand");
        assertInstanceOf(IRToString.class, toStrLowered.left(),
                "the left operand is the admitted to-string node");
        assertTrue(adapter.declineReason(toStringEq, ws).isEmpty(),
                "empty ⟺ lowers: the shared predicate auto-syncs the twin");
    }

    @Test
    void symbolReceiverChainLowersToSymbolNavAtTheCollisionShape() {
        // #501 C2 (the headSymbolNav teach): the #480 MF-1 head-name COLLISION shape — an item
        // attribute AND a workspace function sharing the head name, so the typing engine binds
        // the chain AND leaves resolvedSymbol in place — lowers to the DISTINCT shallow
        // IRSymbolNav kind (legacy renders the FUNCTION-receiver navigation there, BEFORE the
        // item-chain arm; the render is the containing root's oracle-root serve, byte-identical
        // BY IDENTITY). The decline-lock is the hand-built UNTYPED sibling in
        // bareAttrAndChainArmsLowerTheItemNavEquivalents (headSymbolNav.untyped — the arm's own
        // typing gate).
        String source = """
                namespace "test.step501b"

                type Inner:
                    x number (1..1)

                type Leg2:
                    inner Inner (1..1)

                func inner:
                    inputs:
                        dummy number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        dummy

                func ChainCollision:
                    inputs:
                        legs Leg2 (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract inner -> x
                """;
        RModel model = AstBuilder.buildFromString(source, "step501-symbol-nav.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef chain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class).stream()
                .filter(e -> "inner".equals(e.enumName())).findFirst()
                .orElseThrow(() -> new AssertionError("no disguised chain parsed"));
        assertTrue(chain.resolvedSymbol().isPresent(),
                "the fixture must bind the workspace function on the collision head");
        assertTrue(chain.resolvedAttributeChain().isPresent(),
                "the fixture must bind the item attribute chain (the collision shape)");
        IRSymbolNav nav = assertInstanceOf(IRSymbolNav.class,
                adapter.adapt(chain, ws).orElseThrow(() -> new AssertionError(
                        "the collision chain must lower to IRSymbolNav since the #501 C2 arm")),
                "the class lowers to the distinct shallow kind");
        assertEquals("function", nav.symbolKind(), "the neutral fact — legacy's FUNCTION branch");
        assertEquals(chain.sourceRange(), nav.sourceRange(),
                "the node carries the raw range — the oracle serve's correlation key");
        assertTrue(adapter.declineReason(chain, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the arm's admission");
    }

    @Test
    void ruleInputChainsLowerAtBothReceiverContexts() {
        // #501 C1 (the ruleInputChain teach): a rule-body disguised chain whose bound head is on
        // the rule's from-type — the wouldSynthesizeRuleInputChain precedence CLAIMED by
        // adapting legacy's own input-rooted equivalent. TOP LEVEL roots at the #497 ORPHAN
        // synthetic input (IRVariable{PARAM, "input"} — the MapperS.of(input) render); the
        // IN-LAMBDA slice roots at the synthetic item (the #479/#480 item render). Both spines
        // re-stamp the RAW node's range on every link.
        String source = """
                namespace "test.step501c"

                type Leg:
                    rate number (1..1)

                type Trade:
                    leg Leg (1..1)

                reporting rule TopChainRule from Trade:
                    leg -> rate

                reporting rule LambdaChainRule from Trade:
                    extract leg -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step501-rule-chains.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<REnumValueRef> chains = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "leg".equals(e.enumName())).toList();
        assertEquals(2, chains.size(), "both rule bodies parse the disguised chain");

        FieldAccess topLevel = assertInstanceOf(FieldAccess.class,
                adapter.adapt(chains.get(0), ws).orElseThrow(() -> new AssertionError(
                        "the top-level rule chain must lower since the #501 C1 arm")),
                "the top-level chain lowers to the nav spine");
        FieldAccess topHead = assertInstanceOf(FieldAccess.class, topLevel.receiver());
        IRVariable inputBase = assertInstanceOf(IRVariable.class, topHead.receiver());
        assertEquals(IRVariable.VariableKind.PARAM, inputBase.variableKind(),
                "the top-level receiver is the #497 orphan-input PARAM");
        assertEquals("input", inputBase.name(), "the MapperS.of(input) base");
        assertEquals(chains.get(0).sourceRange(), inputBase.sourceRange(),
                "the whole spine re-stamps the RAW node's range");

        FieldAccess inLambda = assertInstanceOf(FieldAccess.class,
                adapter.adapt(chains.get(1), ws).orElseThrow(() -> new AssertionError(
                        "the in-lambda rule chain must lower since the #501 C1 arm")),
                "the in-lambda chain lowers to the nav spine");
        FieldAccess lambdaHead = assertInstanceOf(FieldAccess.class, inLambda.receiver());
        IRVariable itemBase = assertInstanceOf(IRVariable.class, lambdaHead.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, itemBase.variableKind(),
                "the in-lambda receiver is the synthetic item (legacy's item ≡ input identity)");
        assertTrue(adapter.declineReason(chains.get(0), ws).isEmpty()
                        && adapter.declineReason(chains.get(1), ws).isEmpty(),
                "empty ⟺ lowers at both contexts: the twin restates the arm");
    }

    @Test
    void metaChainsAndPipedCallSourcesLowerThroughTheWidenedArms() {
        // #501 C4 + C3: (1) the chainMeta face — a disguised item chain whose LEAF is
        // meta-annotated lowers through the meta re-range leg (IRMetaAccess over the item-rooted
        // head hop — the #500 equivalentMetaAccess class CLAIMED); (2) the bareMeta face — a
        // bare meta attr lowers to the re-ranged IRMetaAccess over the synthetic item; (3) the
        // srcElem widening's decisive leg — an ARGS-PRESENT piped call source resolves to the
        // callee's output element, so the chain over it claims through the standing item-chain
        // arm (the census's dominant 793-node face).
        String source = """
                namespace "test.step501d"

                type Inner:
                    x number (1..1)
                    codeA string (1..1)
                        [metadata scheme]

                type Leg:
                    inner Inner (1..1)
                    codeB string (1..1)
                        [metadata scheme]

                type Trade:
                    legs Leg (0..*)

                func MkLegs:
                    inputs:
                        n number (1..1)
                    output:
                        out Leg (0..*)

                func MetaChainFn:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (0..*)
                    set result:
                        trade -> legs extract inner -> codeA

                func BareMetaFn:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (0..*)
                    set result:
                        trade -> legs extract codeB

                func PipedCallChainFn:
                    inputs:
                        n number (1..1)
                    output:
                        result number (0..*)
                    set result:
                        MkLegs(n) then extract inner -> x
                """;
        RModel model = AstBuilder.buildFromString(source, "step501-meta-and-src.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        REnumValueRef metaChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "inner".equals(e.enumName()) && "codeA".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no meta chain parsed"));
        IRMetaAccess chainMeta = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(metaChain, ws).orElseThrow(() -> new AssertionError(
                        "a meta-leaf item chain must lower since the #501 chainMeta leg")),
                "the chain lowers to the re-ranged meta spine (metaLeaf)");
        FieldAccess chainHead = assertInstanceOf(FieldAccess.class, chainMeta.receiver());
        IRVariable chainBase = assertInstanceOf(IRVariable.class, chainHead.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, chainBase.variableKind());
        assertEquals(metaChain.sourceRange(), chainMeta.sourceRange(),
                "the re-range stamps the RAW node's range on the meta link");
        assertEquals(metaChain.sourceRange(), chainBase.sourceRange(),
                "…and on the item base — the emitter's correlation key at every node");

        RSymbolReference bareTag = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "codeB".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no bare meta attr parsed"));
        IRMetaAccess bareMeta = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(bareTag, ws).orElseThrow(() -> new AssertionError(
                        "a bare meta attr must lower since the #501 bareMeta leg")),
                "the bare attr lowers to the re-ranged meta hop");
        IRVariable bareBase = assertInstanceOf(IRVariable.class, bareMeta.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, bareBase.variableKind());
        assertEquals(bareTag.sourceRange(), bareMeta.sourceRange(),
                "the re-range stamps the RAW node's range — the oracle serve's correlation key");

        REnumValueRef pipedChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "inner".equals(e.enumName()) && "x".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no piped chain parsed"));
        assertTrue(adapter.adapt(pipedChain, ws).isPresent(),
                "the chain over the ARGS-PRESENT piped call source must lower — the #501"
                        + " srcElem widening resolves the callee-output element");
    }

    @Test
    void aliasNameMatchAndClosureParamsLowerAtBothFlavors() {
        // #502 arms 1+2: the ALIAS NAME-MATCH fallback (legacy isAliasReference's name-keyed
        // render — the alias arm's own documented deferred edge) and the CLOSURE-PARAM family
        // in BOTH census flavors (the linker-BOUND named-param reference + the synthetic
        // resolve-by-name head). The decline-locks (the Seat-1 #501 MF-1 witness-uniqueness
        // law): the SELF-name inside the shortcut's own body keeps declining through the #453
        // exclusion (the flip this lock detects would make the adapt claim it), and a
        // no-match name keeps its symbolUnresolved facet.
        String source = """
                namespace "test.step502a"

                type Thing:
                    a string (0..1)
                    flag boolean (1..1)

                func Check:
                    inputs:
                        one Thing (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        one -> flag

                func BoundParamNav:
                    inputs:
                        xs Thing (1..*)
                    output:
                        out boolean (0..*)
                    set out:
                        xs extract x [ Check(x) ]

                func AliasHost:
                    inputs:
                        t Thing (1..1)
                    output:
                        out string (0..1)
                    alias detailsAlias: t -> a
                    set out:
                        detailsAlias
                """;
        RModel model = AstBuilder.buildFromString(source, "step502-alias-closure.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // The BOUND flavor: the named extract param referenced BARE in-lambda (the
        // CompareTradeLot witness shape — a call argument) lowers to the DISTINCT shallow
        // IRClosureParam through whichever bind state the linker leaves (the RInlineFunction
        // bind or the symbol-empty name-resolve — both #502 flavors mint the same kind).
        RSymbolReference boundX = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "x".equals(r.name()) && r.args().isEmpty()).findFirst()
                .orElseThrow(() -> new AssertionError("no bound param ref parsed"));
        IRClosureParam bound = assertInstanceOf(IRClosureParam.class,
                adapter.adapt(boundX, ws).orElseThrow(() -> new AssertionError(
                        "a named-param reference must lower since the #502 arm")),
                "the reference lowers to the shallow closure-param kind");
        assertEquals("x", bound.paramName(), "the neutral fact — the param name");
        assertEquals(Optionality.PRESENT, bound.optionality(),
                "Optionality NORMALIZED PRESENT — the #500/#501 shallow-kind convention");
        // The nav-over-param consumer (the census's dominant interior.RFeatureCall class) —
        // hand-built at the exact consumer shape (the parsed `x -> a` chain takes the
        // disguised REnumValueRef route; the synthesis-shape convention).
        RInlineFunction boundBinder = AstWalker.findAll(ws.files().get(0), RInlineFunction.class)
                .stream().filter(fn -> fn.paramNames().contains("x")).findFirst()
                .orElseThrow(() -> new AssertionError("no named binder parsed"));
        RAttribute thingA = AstWalker.findAll(ws.files().get(0), RAttribute.class).stream()
                .filter(attr -> "a".equals(attr.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no Thing.a parsed"));
        RSymbolReference navHead = new RSymbolReference();
        navHead.setName("x");
        navHead.setParent(boundBinder.body());
        RFeatureCall navOverParam = new RFeatureCall();
        navOverParam.setReceiver(navHead);
        navHead.setParent(navOverParam);
        navOverParam.setFeatureName("a");
        navOverParam.setResolvedFeature(thingA);
        navOverParam.setParent(boundBinder.body());
        navOverParam.attachToWorkspace(ws);
        FieldAccess navLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(navOverParam, ws).orElseThrow(() -> new AssertionError(
                        "the nav off a closure param must lower — the #502 receiver admission")),
                "the nav lowers with the closure-param base");
        assertInstanceOf(IRClosureParam.class, navLowered.receiver(),
                "the admitted base is the shallow kind — the render is the containing root's"
                        + " oracle serve (no leaf-emitter arm exists)");

        // The SYNTHETIC alias flavor: a symbol-EMPTY reference whose name matches an enclosing
        // shortcut (the legacy re-entrant synthesis head class — up-only parented, exactly the
        // #486 mechanism shape) lowers to the SAME IRReference{ALIAS} the bound arm mints.
        RFunction aliasHost = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "AliasHost".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no AliasHost parsed"));
        RSymbolReference syntheticAliasHead = new RSymbolReference();
        syntheticAliasHead.setName("detailsAlias");
        syntheticAliasHead.setParent(aliasHost.operations().get(0));
        syntheticAliasHead.attachToWorkspace(ws);
        IRReference aliasLowered = assertInstanceOf(IRReference.class,
                adapter.adapt(syntheticAliasHead, ws).orElseThrow(() -> new AssertionError(
                        "an unresolved name matching an enclosing shortcut must lower since"
                                + " the #502 arm — legacy isAliasReference's name-match render")),
                "the head lowers to the standing alias kind");
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasLowered.referenceKind(),
                "the SAME kind the linker-bound arm mints — the name-keyed render seam");
        assertTrue(adapter.declineReason(syntheticAliasHead, ws).isEmpty(),
                "empty ⟺ lowers: the twin's restated name-match claims it");

        // The SYNTHETIC closure-param flavor: a symbol-EMPTY reference whose name matches an
        // enclosing binder's declared param resolves by name (the head resolver binds
        // inputs/output only — pass 5 can never see the synthesis nodes).
        RInlineFunction binder = AstWalker.findAll(ws.files().get(0), RInlineFunction.class)
                .stream().filter(fn -> fn.paramNames().contains("x")).findFirst()
                .orElseThrow(() -> new AssertionError("no named binder parsed"));
        RSymbolReference syntheticParamHead = new RSymbolReference();
        syntheticParamHead.setName("x");
        syntheticParamHead.setParent(binder.body());
        syntheticParamHead.attachToWorkspace(ws);
        IRClosureParam syntheticLowered = assertInstanceOf(IRClosureParam.class,
                adapter.adapt(syntheticParamHead, ws).orElseThrow(() -> new AssertionError(
                        "a synthetic head naming an enclosing closure param must lower since"
                                + " the #502 arm")),
                "the synthetic flavor lowers to the same shallow kind");
        assertEquals("x", syntheticLowered.paramName(), "the neutral fact");

        // Decline-lock 1 (the #453 self-scope law): a symbol-EMPTY self-name INSIDE the
        // shortcut's own body keeps declining — the arm's name-match excludes the enclosing
        // shortcut, mirroring legacy isAliasReference's `shortcut != enclosingShortcut` guard
        // (an over-wide claim — the coarse collidesWithShortcut form — turns this red).
        RShortcut detailsAlias = aliasHost.shortcuts().get(0);
        RSymbolReference selfName = new RSymbolReference();
        selfName.setName("detailsAlias");
        selfName.setParent(detailsAlias.expression());
        selfName.attachToWorkspace(ws);
        assertTrue(adapter.adapt(selfName, ws).isEmpty(),
                "a self-name inside the shortcut's own body stays declined — the #453"
                        + " exclusion (legacy renders the shadowed item feature, never a"
                        + " self-call)");
        assertTrue(adapter.declineReason(selfName, ws).orElseThrow()
                        .startsWith("symbolUnresolved."),
                "the residue keeps the facet channel — the flip this lock detects would"
                        + " remove it");

        // Decline-lock 2: a name matching NOTHING keeps its facet (the arm claims only the
        // shortcut/param matches).
        RSymbolReference noMatch = new RSymbolReference();
        noMatch.setName("zzzNothing");
        noMatch.setParent(aliasHost.operations().get(0));
        noMatch.attachToWorkspace(ws);
        assertTrue(adapter.adapt(noMatch, ws).isEmpty(),
                "an unmatched name stays declined");
    }

    @Test
    void itemArgsAndMetaOutputCallsLowerAtTheApplySeats() {
        // #502 arms 3+4: the binder-UNBOUNDED item ARG (the argItem face — a typed explicit
        // `item` in a then body passed as a call argument; the #491 renderer's binding-general
        // law extended to the arg-seat admission) and the META-OUTPUT call (the
        // calleeMetaOutput face — the DISTINCT shallow IRMetaOutputApply, FUNCTION callees
        // census-narrow). RETENSED at #528: the calleeMetaParam SIBLING face the original
        // decline-lock guarded has been TAUGHT (the arms-dead recut), so the third leg now
        // proves the meta-INPUT call lowers as an ORDINARY apply — the seat split (distinct
        // kind for the meta OUTPUT, plain apply for the meta INPUT) is what it locks.
        String source = """
                namespace "test.step502b"

                type Thing:
                    a string (0..1)
                    flag boolean (1..1)

                func TakeThing:
                    inputs:
                        one Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        one -> flag

                func ItemArgCall:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (0..1)
                    set out:
                        t then TakeThing(item)

                func MetaOut:
                    inputs:
                        s string (1..1)
                    output:
                        out string (1..1)
                            [metadata scheme]
                    set out:
                        s

                func CallsMetaOut:
                    inputs:
                        s string (1..1)
                    output:
                        out string (0..1)
                    set out:
                        MetaOut(s)

                func MetaParamFn:
                    inputs:
                        p string (1..1)
                            [metadata scheme]
                    output:
                        out boolean (1..1)
                    set out:
                        True

                func CallsMetaParam:
                    inputs:
                        s string (1..1)
                    output:
                        out boolean (0..1)
                    set out:
                        MetaParamFn(s)
                """;
        RModel model = AstBuilder.buildFromString(source, "step502-apply-seats.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        RSymbolReference itemArgCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "TakeThing".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no TakeThing call parsed"));
        IRApply loweredItemCall = assertInstanceOf(IRApply.class,
                adapter.adapt(itemArgCall, ws).orElseThrow(() -> new AssertionError(
                        "a call with a typed then-body item arg must lower since the #502"
                                + " arg admission")),
                "the call lowers to IRApply");
        IRVariable itemArg = assertInstanceOf(IRVariable.class, loweredItemCall.args().get(0),
                "the arg is the item variable — the #491 binding-general renderer serves it");
        assertEquals(IRVariable.VariableKind.USER_ITEM, itemArg.variableKind(),
                "the explicit `item` lowers as USER_ITEM");

        RSymbolReference metaOutCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "MetaOut".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no MetaOut call parsed"));
        IRMetaOutputApply metaApply = assertInstanceOf(IRMetaOutputApply.class,
                adapter.adapt(metaOutCall, ws).orElseThrow(() -> new AssertionError(
                        "a FUNCTION call with a meta-annotated output must lower since the"
                                + " #502 arm")),
                "the call lowers to the DISTINCT shallow meta-output kind");
        assertEquals("MetaOut", metaApply.calleeName(), "the neutral fact — the callee name");
        assertTrue(metaApply.children().isEmpty(),
                "childless BY DESIGN — the args are not carried (the oracle-root serve renders"
                        + " legacy's whole call line)");

        // #528 arm-4 RETENSE (the lock's flip landed): the meta-PARAM sibling face is TAUGHT —
        // legacy's whole arg machinery is inert at a meta-annotated param (every branch keyed
        // on it declines: the #342/#346/#347/#349 gates and tryMetaDerefArg's core), so the
        // arg passes RAW and the call is an ORDINARY apply whose per-argument seats are the
        // only guard. The lock now proves the admission and keeps its negative on the OUTPUT
        // side (the distinct-kind split above), so the pair still detects a seat collapse.
        RSymbolReference metaParamCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "MetaParamFn".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no MetaParamFn call parsed"));
        IRApply metaParamApply = assertInstanceOf(IRApply.class,
                adapter.adapt(metaParamCall, ws).orElseThrow(() -> new AssertionError(
                        "a call with a meta-annotated INPUT must lower since the #528 arms-dead"
                                + " recut — legacy raw-passes the arg at a meta param")),
                "the meta-PARAM call lowers to the ORDINARY deep IRApply — NOT a distinct kind:"
                        + " only the meta-OUTPUT face needs one (the value legacy alone threads)");
        assertEquals(1, metaParamApply.args().size(),
                "the args ARE carried (the ordinary apply shape — the arg seats stay the guard)");
        assertInstanceOf(IRVariable.class, metaParamApply.args().get(0),
                "the raw-passed arg is the plain param reference");
        assertEquals(Optional.empty(), adapter.declineReason(metaParamCall, ws),
                "the calleeMetaParam token retires WITH the face — a re-appearance is drift");
    }

    @Test
    void sourceProofLegsWidenTheRetypeAtTheNewShapes() {
        // #502 arm 5: the source-proof widenings — the element-form-preserving listOp collapse
        // (5b), the alias body-descent (5c) and the THEN-body binder leg (5d, card1-only) all
        // retype the synthetic filter/extract/then item and the navs off it lower. The
        // decline-locks: a META-bottomed collapse source keeps declining (the L-029
        // FieldWithMeta-value protection) and a MULTI then-argument keeps declining (the
        // census-narrow card1 gate) — both flips would remove the receiverSyntheticItem token.
        String source = """
                namespace "test.step502c"

                type Inner:
                    x string (0..1)

                type Thing:
                    a string (0..1)
                    inner Inner (1..1)

                type Holder:
                    things Thing (1..*)
                    thing Thing (1..1)
                    metaThings Thing (0..*)
                        [metadata reference]

                func CollapseSourceNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        h -> things only-element extract a

                func AliasSourceNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..*)
                    alias someThings: h -> things
                    set out:
                        someThings extract a

                func ThenBodyNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out int (1..1)
                    set out:
                        h -> thing then inner -> x count

                func MetaCollapseNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        h -> metaThings only-element extract a

                func MultiThenNav:
                    inputs:
                        xs Thing (1..*)
                    output:
                        out int (1..1)
                    set out:
                        xs then inner -> x count
                """;
        RModel model = AstBuilder.buildFromString(source, "step502-source-proofs.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction collapseFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "CollapseSourceNav".equals(f.name())).findFirst().orElseThrow();
        RFunction aliasFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "AliasSourceNav".equals(f.name())).findFirst().orElseThrow();
        RFunction thenFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "ThenBodyNav".equals(f.name())).findFirst().orElseThrow();
        RFunction metaFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "MetaCollapseNav".equals(f.name())).findFirst().orElseThrow();
        RFunction multiFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "MultiThenNav".equals(f.name())).findFirst().orElseThrow();

        // 5b: the extract over an only-element collapse — the bare `a` claims through the
        // widened walks (the source's element form derives through the collapse's argument).
        RSymbolReference collapseA = AstWalker.findAll(collapseFn, RSymbolReference.class)
                .stream().filter(r -> "a".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no bare a parsed (collapse)"));
        assertTrue(adapter.adapt(collapseA, ws).isPresent(),
                "the bare attr over the ONLY_ELEMENT-collapsed source must lower — the #502"
                        + " listOp element-form leg proves the source");

        // 5c: the extract over an ALIAS source — the shortcut body-descent proves the element.
        RSymbolReference aliasA = AstWalker.findAll(aliasFn, RSymbolReference.class)
                .stream().filter(r -> "a".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no bare a parsed (alias)"));
        assertTrue(adapter.adapt(aliasA, ws).isPresent(),
                "the bare attr over the alias source must lower — the #502 alias body-descent"
                        + " leg proves the element through the shortcut body");

        // 5d: the THEN-body binder leg — hand-built at the exact retype shape (the corpus
        // carriers' parse forms vary; the movement receipt carries the population proof): a
        // synthetic implicit whose NEAREST binder is the then's implicit body, navigated by a
        // feature call. The retype stamps the ARGUMENT's own cached type (card1) and the nav
        // lowers natively.
        RThenExpr thenNode = AstWalker.findAll(thenFn, RThenExpr.class).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no then parsed"));
        RInlineFunction thenBinder = thenNode.body()
                .orElseThrow(() -> new AssertionError("no then body"));
        RAttribute thingInner = AstWalker.findAll(ws.files().get(0), RAttribute.class).stream()
                .filter(attr -> "inner".equals(attr.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no Thing.inner parsed"));
        RImplicitVariable thenItem = new RImplicitVariable();
        thenItem.setSynthetic(true);
        RFeatureCall thenChainRoot = new RFeatureCall();
        thenChainRoot.setReceiver(thenItem);
        thenItem.setParent(thenChainRoot);
        thenChainRoot.setFeatureName("inner");
        thenChainRoot.setResolvedFeature(thingInner);
        thenChainRoot.setParent(thenBinder.body());
        thenChainRoot.attachToWorkspace(ws);
        FieldAccess thenLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(thenChainRoot, ws).orElseThrow(() -> new AssertionError(
                        "the nav off a then-bound item must lower — the #502 then-body binder"
                                + " leg retypes it from the argument")),
                "the chain lowers natively");
        IRVariable thenBase = assertInstanceOf(IRVariable.class, thenLowered.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, thenBase.variableKind());
        assertFalse(thenBase.type().isMissing(),
                "the retype stamped the then-argument's own cached type");

        // RECUT at #504 (the pin-recut law) — both former decline-locks now MINT the shallow
        // IRSynItemNav (the arm-B teach): the RETYPE gates still decline these shapes (the
        // L-029 meta wall and the card1-only then gate keep the richer FieldAccess retype
        // out), but the un-retypeable navs lower to the DISTINCT kind and every containing
        // claim root oracle-serves legacy's own render (coercion and MULTI framing included —
        // byte-identical BY IDENTITY).
        // (1) the META-bottomed collapse — the aw0tw1 class: the bare attr claims via the
        //     arm-B2 leg.
        RSymbolReference metaA = AstWalker.findAll(metaFn, RSymbolReference.class)
                .stream().filter(r -> "a".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no bare a parsed (meta)"));
        IRSynItemNav metaCollapseMinted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(metaA, ws).orElseThrow(() -> new AssertionError(
                        "a meta-collapse-sourced bare item attr must mint since the #504 arm")),
                "the L-029-walled residue lowers to the shallow kind");
        assertEquals("a", metaCollapseMinted.featureName(),
                "the resolved feature's name is the neutral fact");

        // (2) the MULTI then-argument — the cardN class: the nav claims via the arm-B mint
        //     (the retype's card1-only gate keeps the FieldAccess arm out exactly as before).
        RThenExpr multiThen = AstWalker.findAll(multiFn, RThenExpr.class).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no multi then parsed"));
        RInlineFunction multiBinder = multiThen.body()
                .orElseThrow(() -> new AssertionError("no multi then body"));
        RImplicitVariable multiItem = new RImplicitVariable();
        multiItem.setSynthetic(true);
        RFeatureCall multiChainRoot = new RFeatureCall();
        multiChainRoot.setReceiver(multiItem);
        multiItem.setParent(multiChainRoot);
        multiChainRoot.setFeatureName("inner");
        multiChainRoot.setResolvedFeature(thingInner);
        multiChainRoot.setParent(multiBinder.body());
        multiChainRoot.attachToWorkspace(ws);
        IRSynItemNav multiMinted = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(multiChainRoot, ws).orElseThrow(() -> new AssertionError(
                        "a MULTI then-item nav must mint since the #504 arm")),
                "the cardN residue lowers to the shallow kind");
        assertEquals("inner", multiMinted.featureName(),
                "the resolved feature's name is the neutral fact");
        assertTrue(adapter.declineReason(multiChainRoot, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the mint");
    }

    @Test
    void modifiedEqualitiesComposeInLogicalsAndStayOutOfCallArgs() {
        // #503 arm-A1: the PARSED any-equality composes in a logical through the
        // producesComparisonResult admission (the census's dominant interior.RLogicalExpr
        // consumers — 7,832 node-units). The decline-lock (the witness-uniqueness law): the
        // kind admits at NO OTHER gate — a call with a modified-equality ARG keeps declining
        // with the generic arg token (the census read ZERO call-arg consumers; an over-wide
        // isSimpleCallArg admission turns this red).
        String source = """
                namespace "test.step503a"

                type Thing:
                    tags string (0..*)
                    flag boolean (1..1)

                func Check:
                    inputs:
                        ok boolean (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        ok

                func AnyEqInLogical:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        t -> tags any = "x" and t -> flag exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step503-allany.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REqualityExpr anyEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class).stream()
                .filter(eq -> eq.mod().isPresent()).findFirst()
                .orElseThrow(() -> new AssertionError("no modified equality parsed"));
        IRAllAnyCompare lowered = assertInstanceOf(IRAllAnyCompare.class,
                adapter.adapt(anyEq, ws).orElseThrow(() -> new AssertionError(
                        "a parsed any-equality must lower since the #503 arm")),
                "the modified equality lowers to the shallow kind");
        assertEquals("ANY", lowered.modifier());
        assertEquals(Optionality.PRESENT, lowered.optionality(),
                "Optionality NORMALIZED PRESENT — the shallow-kind convention");
        // The logical ROOT composes through the producesComparisonResult admission — the
        // #503 arm's SOLE consumer gate, made flip-sensitive HERE (the Seat-1 MF-4 recut:
        // removing the IRAllAnyCompare admission turns this red): `<any-eq> and <exists>`
        // lowers WHOLE, the modified equality riding the LEFT operand seat beside an
        // admitted Existence sibling (the census's interior.RLogicalExpr consumer shape).
        RLogicalExpr logical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no logical parsed"));
        BinaryOp composedRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(logical, ws).orElseThrow(() -> new AssertionError(
                        "the logical over an any-equality and an exists must compose —"
                                + " producesComparisonResult admits the #503 kind")),
                "the composed consumer root lowers whole");
        assertEquals(BinaryOp.BinOp.AND, composedRoot.op());
        assertInstanceOf(IRAllAnyCompare.class, composedRoot.left(),
                "the modified equality composes as the admitted LEFT operand");
        assertInstanceOf(Existence.class, composedRoot.right(),
                "the Existence sibling composes on the RIGHT");

        // The call-arg decline-lock: hand-built F(<any-eq>) keeps the generic token.
        RFunction checkFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "Check".equals(f.name())).findFirst().orElseThrow();
        RSymbolReference argCall = new RSymbolReference();
        argCall.setName("Check");
        argCall.setResolvedSymbol(checkFn);
        REqualityExpr argEq = new REqualityExpr();
        argEq.setOp(anyEq.op());
        argEq.setMod(CardMod.ANY);
        argEq.setLeft(anyEq.rawLeft());
        argEq.setRight(anyEq.rawRight());
        argCall.args().add(argEq);
        argEq.setParent(argCall);
        argCall.setParent(AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "AnyEqInLogical".equals(f.name())).findFirst().orElseThrow()
                .operations().get(0));
        argCall.attachToWorkspace(ws);
        assertTrue(adapter.adapt(argCall, ws).isEmpty(),
                "a call with a modified-equality arg stays declined — the kind admits at"
                        + " producesComparisonResult ONLY (census-narrow)");
        assertEquals(Optional.of("arg:IRAllAnyCompare"), adapter.declineReason(argCall, ws),
                "the generic arg token — the flip this lock detects would remove it");
    }

    @Test
    void choiceOptionAndClosureParamHeadNavsResolveByName() {
        // #503 arms B1+B2: the CHOICE-OPTION selection (the option resolves by its type-name
        // spelling against the choice's RDataType projection — legacy NavigationHandler's own
        // recovery mirrored) and the closure-param-head by-name feature resolution (the
        // binder-source walk + the member lookup). The decline-locks: a feature NOT among the
        // options keeps the nonSymbolReceiver face, and a closure head over an UNPROVABLE
        // source keeps the headUnresolved face — both flips would remove the tokens.
        String source = """
                namespace "test.step503b"

                type Inner:
                    x string (0..1)

                type OptionA:
                    a string (0..1)

                type OptionB:
                    b string (0..1)

                choice Pick:
                    OptionA
                    OptionB

                type Holder:
                    pick Pick (1..1)
                    inners Inner (1..*)

                func ChoiceNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        h -> pick -> OptionA -> a

                func ClosureHeadNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..*)
                    set out:
                        h -> inners extract y [ y -> x ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step503-choice-closure.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // B1: the option hop `-> OptionA` resolves against the projection and the whole
        // chain lowers natively.
        RFunction choiceFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "ChoiceNav".equals(f.name())).findFirst().orElseThrow();
        RFeatureCall optionHop = AstWalker.findAll(choiceFn, RFeatureCall.class).stream()
                .filter(fc -> "OptionA".equals(fc.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("no option hop parsed"));
        FieldAccess optionLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(optionHop, ws).orElseThrow(() -> new AssertionError(
                        "a choice-option hop must lower since the #503 arm")),
                "the option selection lowers as a navigation hop");
        assertEquals("OptionA", optionLowered.feature(), "the option's type-name spelling");
        assertTrue(adapter.declineReason(optionHop, ws).isEmpty(),
                "empty ⟺ lowers: the twin's choice restatement claims");

        // The B1 decline-lock: a NON-option feature on the choice keeps the face.
        RFeatureCall missHop = new RFeatureCall();
        missHop.setReceiver(optionHop.receiver());
        missHop.setFeatureName("NotAnOption");
        missHop.setParent(optionHop.parent());
        missHop.attachToWorkspace(ws);
        assertTrue(adapter.adapt(missHop, ws).isEmpty(),
                "a non-option feature stays declined — the projection lookup misses");

        // B2: the named-param head's feature resolves through the binder-source walk (the
        // parsed `y -> x` chain takes the disguised route; hand-build the RFeatureCall form
        // at the exact arm shape — the synthesis-shape convention).
        RFunction closureFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "ClosureHeadNav".equals(f.name())).findFirst().orElseThrow();
        RInlineFunction binder = AstWalker.findAll(closureFn, RInlineFunction.class).stream()
                .filter(fn -> fn.paramNames().contains("y")).findFirst()
                .orElseThrow(() -> new AssertionError("no named binder parsed"));
        RSymbolReference head = new RSymbolReference();
        head.setName("y");
        RFeatureCall headNav = new RFeatureCall();
        headNav.setReceiver(head);
        head.setParent(headNav);
        headNav.setFeatureName("x");
        headNav.setParent(binder.body());
        headNav.attachToWorkspace(ws);
        FieldAccess headLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(headNav, ws).orElseThrow(() -> new AssertionError(
                        "a closure-param-head nav must lower since the #503 arm — the feature"
                                + " resolves by name on the walked element type")),
                "the nav lowers over the closure-param head");
        assertInstanceOf(IRClosureParam.class, headLowered.receiver(),
                "the head lowers as the #502 shallow kind");
        assertFalse(headLowered.type() == null || headLowered.type().isMissing(),
                "the hop types from the resolved feature's attribute channel");

        // The B2 decline-lock: a head whose feature is not a member keeps declining.
        RFeatureCall memberMiss = new RFeatureCall();
        RSymbolReference missHead = new RSymbolReference();
        missHead.setName("y");
        memberMiss.setReceiver(missHead);
        missHead.setParent(memberMiss);
        memberMiss.setFeatureName("zzzNotAMember");
        memberMiss.setParent(binder.body());
        memberMiss.attachToWorkspace(ws);
        assertTrue(adapter.adapt(memberMiss, ws).isEmpty(),
                "a non-member feature over a closure head stays declined — the lookup misses");
    }

    @Test
    void bareBooleanCallAndPointFreeOperandsComposeAtTheCoercionSeats() {
        // #504 arm-A: the ComparisonResult.ofNullSafe coercion family — a lowered boolean
        // CALL (operandBareBooleanCall, 829 sole at the #503 SOT: 100% FUNCTION callees,
        // 100% boolean SINGLE outputs) and a lowered point-free reference
        // (operand:IRPointFreeApply — 332 logical + 347 equality + 142 existence sole) join
        // their operand seats. The renders stay legacy's own: every containing claim root
        // routes through the compiler's oracle-root serve via the SHAPE legs (the coerced
        // logical / the pf equality / the pf existence — containsOracleLeaf carries the
        // composed shapes, never the bare kinds, so args and nav receivers keep their
        // standing native composes). Each admission is made flip-sensitive HERE with a
        // composed-root assert (the #503 Seat-1 MF-4 witness-fixture law).
        String source = """
                namespace "test.step504a"

                type Thing:
                    flag boolean (1..1)

                func Pred:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        t -> flag

                func UseBareCall:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        Pred(t) and t -> flag exists

                func UsePfEquality:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        Pred = True

                func UsePfExists:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        Pred exists

                func UsePfLogical:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        Pred and t -> flag exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step504-boolop.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // (A1) the bareCall logical: `Pred(t) and t -> flag exists` composes WHOLE —
        // producesComparisonResult admits the IRApply operand (removing it turns this red).
        RLogicalExpr bareCallLogical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no logical parsed"));
        BinaryOp coercedRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(bareCallLogical, ws).orElseThrow(() -> new AssertionError(
                        "a logical over a boolean call must compose since the #504 arm")),
                "the coerced-logical root lowers whole");
        assertEquals(BinaryOp.BinOp.AND, coercedRoot.op());
        assertInstanceOf(IRApply.class, coercedRoot.left(),
                "the bare boolean call composes as the admitted LEFT operand — legacy's"
                        + " ComparisonResult.ofNullSafe coercion renders at the oracle serve");
        assertInstanceOf(Existence.class, coercedRoot.right(),
                "the Existence sibling composes on the RIGHT");
        assertTrue(adapter.declineReason(bareCallLogical, ws).isEmpty(),
                "empty ⟺ lowers: the shared predicate auto-mirrors");

        // (A2) the pf equality: `Pred = True` composes — isEqualityOperand admits the
        // point-free operand (removing it turns this red).
        REqualityExpr pfEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no equality parsed"));
        BinaryOp pfEqRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(pfEq, ws).orElseThrow(() -> new AssertionError(
                        "an equality over a point-free reference must compose since the #504 arm")),
                "the pf-equality root lowers whole");
        assertEquals(BinaryOp.BinOp.EQ, pfEqRoot.op());
        assertInstanceOf(IRPointFreeApply.class, pfEqRoot.left(),
                "the point-free reference composes as the admitted LEFT operand");
        assertInstanceOf(IRLiteral.class, pfEqRoot.right(),
                "the boolean literal composes on the RIGHT");

        // (A3) the pf existence: `Pred exists` composes — the existence admission
        // (removing it turns this red).
        RExistenceExpr pfExists = AstWalker.findAll(ws.files().get(0), RExistenceExpr.class)
                .stream()
                .filter(e -> e.argument() instanceof RSymbolReference)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no pf existence parsed"));
        Existence pfExistRoot = assertInstanceOf(Existence.class,
                adapter.adapt(pfExists, ws).orElseThrow(() -> new AssertionError(
                        "an existence over a point-free reference must compose since the #504 arm")),
                "the pf-existence root lowers whole");
        assertInstanceOf(IRPointFreeApply.class, pfExistRoot.arg(),
                "the point-free reference composes as the admitted operand");

        // (A1-pf) the pf logical: `Pred and t -> flag exists` composes — the
        // producesComparisonResult IRPointFreeApply half's OWN red-turning assert (the #504
        // Seat-1 OBS-6 lock: removing that clause alone must fail HERE, not only at the
        // corpus probe).
        RLogicalExpr pfLogical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class)
                .stream()
                .filter(l -> l.rawLeft() instanceof RSymbolReference bare && bare.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("no pf logical parsed"));
        BinaryOp pfLogRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(pfLogical, ws).orElseThrow(() -> new AssertionError(
                        "a logical over a point-free reference must compose since the #504 arm")),
                "the pf-logical root lowers whole");
        assertInstanceOf(IRPointFreeApply.class, pfLogRoot.left(),
                "the point-free reference composes as the admitted LEFT operand");

        // #528 arm-2 RETENSE (the lock's flip landed): the COMPARISON seat's pf non-admission
        // this leg guarded is TAUGHT — the kind's last un-admitted BOOLEAN-PRODUCING operand
        // seat joined the set (the arithmetic seat keeps its standing decline — the Seat-1
        // #528 MF-2 recut; legacy's ComparisonHandler composes the implicit-invocation render
        // inside its own line, and the compiler's comparison shape leg routes the containing
        // root to the oracle serve). The lock keeps its uniqueness by moving to the boundary
        // that still stands: with the pf side now admitted, this synthetic
        // `Pred < <boolean literal>` declines on the LITERAL side instead — a non-numeric
        // literal is not an ordered comparison operand at any sibling, so an over-wide
        // literal admission turns it red.
        RFunction predFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "Pred".equals(f.name())).findFirst().orElseThrow();
        RSymbolReference pfCmpRef = new RSymbolReference();
        pfCmpRef.setName("Pred");
        pfCmpRef.setResolvedSymbol(predFn);
        RComparisonExpr pfCmp = new RComparisonExpr();
        pfCmp.setOp(CompOp.LT);
        pfCmp.setLeft(pfCmpRef);
        pfCmpRef.setParent(pfCmp);
        pfCmp.setRight(pfEq.rawRight());
        pfCmp.setParent(AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "UsePfEquality".equals(f.name())).findFirst().orElseThrow()
                .operations().get(0));
        pfCmpRef.attachToWorkspace(ws);
        assertTrue(ExpressionToIRAdapter.isComparisonOperand(
                        adapter.adapt(pfCmpRef, ws).orElseThrow(), null),
                "the pf operand ITSELF is admitted at the comparison seat since the #528 arm-2"
                        + " — removing that clause turns this red");
        assertEquals(Optional.of("operand:IRLiteral"), adapter.declineReason(pfCmp, ws),
                "the surviving boundary: the BOOLEAN-literal side, never an ordered-comparison"
                        + " operand — the pf token retired with the face");
    }

    @Test
    void mintedSynItemNavsComposeAtTheConsumerSeats() {
        // #504 arm-B consumer admissions (the mint's own exposed frontier — the post-arm
        // probe read operand:IRSynItemNav 190 equality + 33 existence + 6 arg sole): a
        // MINTED un-retypeable synthetic-item nav composes as an equality/existence operand
        // and a call argument. The kind is an oracle LEAF (containsOracleLeaf), so every
        // composed root here renders whole-legacy at the oracle serve. Each admission is
        // flip-sensitive via its composed-root assert (the MF-4 law). The fixture's
        // un-retypeability: the extract SOURCE is meta-annotated (the aw0tw1 census class —
        // the allowlist declines the meta hop, the typing walk still proves the element).
        String source = """
                namespace "test.step504b"

                type Item:
                    inner string (1..1)
                    flag boolean (1..1)

                type Wrap:
                    itemRefs Item (0..*)
                        [metadata reference]

                func Pred:
                    inputs:
                        s string (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        s = "x"

                func UseSynEq:
                    inputs:
                        w Wrap (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        w -> itemRefs extract [ inner = "x" ]

                func UseSynExists:
                    inputs:
                        w Wrap (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        w -> itemRefs extract [ flag exists ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step504-synitem.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // The equality consumer: `inner = "x"` inside the meta-sourced extract — the bare
        // attr claims via the arm-B2 leg (IRSynItemNav) and the equality composes through
        // the isEqualityOperand admission (removing either turns this red).
        REqualityExpr synEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream()
                .filter(eq -> eq.rawLeft() instanceof RSymbolReference sr
                        && "inner".equals(sr.name()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no in-lambda equality parsed"));
        BinaryOp synEqRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(synEq, ws).orElseThrow(() -> new AssertionError(
                        "an equality over a minted syn-item nav must compose since the #504"
                                + " consumer admission")),
                "the composed equality root lowers whole");
        IRSynItemNav eqOperand = assertInstanceOf(IRSynItemNav.class, synEqRoot.left(),
                "the minted nav composes as the admitted LEFT operand");
        assertEquals("inner", eqOperand.featureName(), "the neutral fact");
        assertInstanceOf(IRLiteral.class, synEqRoot.right(), "the string literal sibling");

        // The existence consumer: `flag exists` inside the same shape.
        RExistenceExpr synExists = AstWalker.findAll(ws.files().get(0), RExistenceExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no in-lambda existence parsed"));
        Existence synExistRoot = assertInstanceOf(Existence.class,
                adapter.adapt(synExists, ws).orElseThrow(() -> new AssertionError(
                        "an existence over a minted syn-item nav must compose since the #504"
                                + " consumer admission")),
                "the composed existence root lowers whole");
        IRSynItemNav existOperand = assertInstanceOf(IRSynItemNav.class, synExistRoot.arg(),
                "the minted nav composes as the admitted operand");
        assertEquals("flag", existOperand.featureName(), "the neutral fact");

        // The call-arg consumer: hand-built Pred(<minted nav>) at the same lambda seat —
        // isSimpleCallArg admits the kind (removing the clause turns this red). The PARSED
        // (cache-typed) inner ref re-parents into the call (the #503 hand-build precedent —
        // the arm-B2 navType gate reads the node-keyed cache, so a fresh node would decline
        // untypeable rather than exercise the arg admission).
        RFunction predFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "Pred".equals(f.name())).findFirst().orElseThrow();
        RSymbolReference innerParsed = assertInstanceOf(RSymbolReference.class, synEq.rawLeft());
        RSymbolReference argCall = new RSymbolReference();
        argCall.setName("Pred");
        argCall.setResolvedSymbol(predFn);
        // The parsed node is FROZEN (no setParent) — the child link alone carries the arg
        // seat, and the arm-B2 binder walk reads the node's ORIGINAL parent chain (the same
        // lambda), so the mint fires identically.
        argCall.args().add(innerParsed);
        argCall.setParent(synEq.parent());
        argCall.attachToWorkspace(ws);
        IRApply argRoot = assertInstanceOf(IRApply.class,
                adapter.adapt(argCall, ws).orElseThrow(() -> new AssertionError(
                        "a call over a minted syn-item nav arg must compose since the #504"
                                + " consumer admission")),
                "the composed call root lowers whole");
        assertInstanceOf(IRSynItemNav.class, argRoot.args().get(0),
                "the minted nav composes as the admitted argument");
    }

    @Test
    void collidedBareNamesRequalifyAgainstTheirEnumSeats() {
        // #504 arm-C1: the legacy re-qualification mirrors — tryBareEnumArg at the call-arg
        // seat (unresolved-or-RBody/RDataType/RCorpus-bound names against the callee's
        // POSITIONAL declared enum parameter; the census read argOf 264 node-unit 100%
        // enumHit) and tryBareEnumComparand at the EQUALITY seat (RDataType/RChoice/RBody —
        // typeShadowEnumValueName's EXACT class set, RCorpus deliberately NOT admitted;
        // eqSib 37 enumHit). Both mint IRReference{ENUM_VALUE} carrying the seat's enum type
        // (the arg render is the oracle-served dotted constant; the equality render is the
        // standing native MapperS.of(EnumName.CONSTANT) wrap). Arm-C2: the explicit `empty`
        // argument admits (argEmpty 239 sole — the callArgs oracle serve renders legacy's
        // null-threading). The class-set locks keep the mirrors exact.
        String source = """
                namespace "test.step504c"

                body Authority TESTBODY
                corpus Specifications "Test Corp" TESTCORP

                enum RegimeEnum:
                    TESTBODY
                    TESTCORP
                    OTHERVAL

                type Thing:
                    regime RegimeEnum (1..1)

                func Take:
                    inputs:
                        regime RegimeEnum (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        regime exists

                func TakeStr:
                    inputs:
                        s string (0..1)
                    output:
                        out boolean (1..1)
                    set out:
                        s exists

                func UseEmpty:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        TakeStr(empty)
                """;
        RModel model = AstBuilder.buildFromString(source, "step504-qualname.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RBody body = AstWalker.findAll(ws.files().get(0), RBody.class).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no body parsed"));
        RCorpus corpus = AstWalker.findAll(ws.files().get(0), RCorpus.class).stream().findFirst()
                .orElseThrow(() -> new AssertionError("no corpus parsed"));
        RFunction takeFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "Take".equals(f.name())).findFirst().orElseThrow();
        RNode takeOp = takeFn.operations().get(0);

        // (C1a) the arg seat: Take(TESTBODY) with TESTBODY false-bound to the BODY — the
        // positional param's enum carries the name, so the arg mints the ENUM_VALUE
        // reference carrying the PARAM's enum type (removing the fallback turns this red).
        RSymbolReference bodyArg = new RSymbolReference();
        bodyArg.setName("TESTBODY");
        bodyArg.setResolvedSymbol(body);
        RSymbolReference bodyCall = new RSymbolReference();
        bodyCall.setName("Take");
        bodyCall.setResolvedSymbol(takeFn);
        bodyCall.args().add(bodyArg);
        bodyArg.setParent(bodyCall);
        bodyCall.setParent(takeOp);
        bodyArg.attachToWorkspace(ws);
        bodyCall.attachToWorkspace(ws);
        IRApply requalified = assertInstanceOf(IRApply.class,
                adapter.adapt(bodyCall, ws).orElseThrow(() -> new AssertionError(
                        "a body-collided enum arg must requalify since the #504 arm-C1a")),
                "the call lowers with the requalified arg");
        IRReference enumArg = assertInstanceOf(IRReference.class, requalified.args().get(0),
                "the argument is the minted enum-value reference");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, enumArg.referenceKind());
        assertEquals("TESTBODY", enumArg.target(), "the raw value name the emitter formats");
        assertEquals("RegimeEnum", enumArg.type().type().name(),
                "the PARAM's enum type — the emitter qualifies the constant by it");
        assertTrue(adapter.declineReason(bodyCall, ws).isEmpty(),
                "empty ⟺ lowers: the twin restates the fallback");

        // (C1a lock) the value-name match is the strong gate: a body-bound name matching NO
        // enum value keeps declining whole.
        RSymbolReference missArg = new RSymbolReference();
        missArg.setName("NOTAVALUE");
        missArg.setResolvedSymbol(body);
        RSymbolReference missCall = new RSymbolReference();
        missCall.setName("Take");
        missCall.setResolvedSymbol(takeFn);
        missCall.args().add(missArg);
        missArg.setParent(missCall);
        missCall.setParent(takeOp);
        missArg.attachToWorkspace(ws);
        missCall.attachToWorkspace(ws);
        assertTrue(adapter.adapt(missCall, ws).isEmpty(),
                "an enum-missed collided name keeps declining — legacy's own value-name gate");
        assertEquals(Optional.of("argNotExpressible"), adapter.declineReason(missCall, ws),
                "the arg stays unexpressible — the flip would remove the token");

        // (C1b) the equality seat: `regime = TESTBODY` with the PARSED enum-typed sibling
        // and the body-bound bare name — the sibling's cached enumeration carries the name,
        // so the operand mints and the equality composes NATIVELY (the standing enum-operand
        // wrap; removing the fallback turns this red).
        RExistenceExpr regimeExists = AstWalker.findAll(takeFn, RExistenceExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no regime exists parsed"));
        RExpression parsedRegimeRef = regimeExists.argument();
        RSymbolReference eqBodyRef = new RSymbolReference();
        eqBodyRef.setName("TESTBODY");
        eqBodyRef.setResolvedSymbol(body);
        REqualityExpr eqSeat = new REqualityExpr();
        eqSeat.setOp(EqOp.EQ);
        eqSeat.setLeft(parsedRegimeRef);
        eqSeat.setRight(eqBodyRef);
        eqBodyRef.setParent(eqSeat);
        eqSeat.setParent(takeOp);
        eqBodyRef.attachToWorkspace(ws);
        BinaryOp eqRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(eqSeat, ws).orElseThrow(() -> new AssertionError(
                        "a body-collided enum comparand must requalify since the #504 arm-C1b")),
                "the equality composes natively with the minted operand");
        IRReference eqEnum = assertInstanceOf(IRReference.class, eqRoot.right(),
                "the RIGHT operand is the minted enum-value reference");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, eqEnum.referenceKind());
        assertEquals("TESTBODY", eqEnum.target());

        // (C1b class-set lock) an RCorpus-bound name at the EQUALITY seat keeps declining —
        // legacy's typeShadowEnumValueName never admitted the corpus class there (the arg
        // seat's #376 extension is the ARG seat's own); the SAME corpus-bound name as a call
        // ARG claims — the asymmetry pins both mirrors.
        RSymbolReference eqCorpusRef = new RSymbolReference();
        eqCorpusRef.setName("TESTCORP");
        eqCorpusRef.setResolvedSymbol(corpus);
        REqualityExpr eqCorpusSeat = new REqualityExpr();
        eqCorpusSeat.setOp(EqOp.EQ);
        eqCorpusSeat.setLeft(parsedRegimeRef);
        eqCorpusSeat.setRight(eqCorpusRef);
        eqCorpusRef.setParent(eqCorpusSeat);
        eqCorpusSeat.setParent(takeOp);
        eqCorpusRef.attachToWorkspace(ws);
        assertTrue(adapter.adapt(eqCorpusSeat, ws).isEmpty(),
                "a corpus-bound name at the equality seat stays declined — the eq class set"
                        + " is RDataType/RChoice/RBody exactly (legacy's own)");
        RSymbolReference corpusArg = new RSymbolReference();
        corpusArg.setName("TESTCORP");
        corpusArg.setResolvedSymbol(corpus);
        RSymbolReference corpusCall = new RSymbolReference();
        corpusCall.setName("Take");
        corpusCall.setResolvedSymbol(takeFn);
        corpusCall.args().add(corpusArg);
        corpusArg.setParent(corpusCall);
        corpusCall.setParent(takeOp);
        corpusArg.attachToWorkspace(ws);
        corpusCall.attachToWorkspace(ws);
        IRApply corpusLowered = assertInstanceOf(IRApply.class,
                adapter.adapt(corpusCall, ws).orElseThrow(() -> new AssertionError(
                        "a corpus-collided enum ARG must requalify — the #376 arg-seat class")),
                "the arg seat admits the corpus class (the asymmetry)");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE,
                assertInstanceOf(IRReference.class, corpusLowered.args().get(0)).referenceKind());

        // (C2) the explicit `empty` argument: the parsed TakeStr(empty) lowers with the
        // faithful IREmptyLiteral arg (removing the admission turns this red).
        RSymbolReference emptyCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> !r.args().isEmpty()).findFirst()
                .orElseThrow(() -> new AssertionError("no TakeStr(empty) parsed"));
        IRApply emptyLowered = assertInstanceOf(IRApply.class,
                adapter.adapt(emptyCall, ws).orElseThrow(() -> new AssertionError(
                        "an empty-arg call must lower since the #504 arm-C2")),
                "the empty-arg call lowers");
        assertInstanceOf(IREmptyLiteral.class, emptyLowered.args().get(0),
                "the argument is the lowered empty literal");
    }

    /** Wires {@code inputs} as parameters of a fresh plain function and attaches everything to {@code ws}. */
    private static RFunction functionWith(RWorkspace ws, RAttribute... inputs) {
        RFunction func = new RFunction();
        func.setName("MyFunc");
        for (RAttribute in : inputs) {
            func.inputs().add(in);
            in.setParent(func);
            in.attachToWorkspace(ws);
        }
        func.attachToWorkspace(ws);
        return func;
    }

    private static RSymbolReference paramRef(String name, RAttribute resolved, RWorkspace ws) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(resolved);
        ref.attachToWorkspace(ws);
        return ref;
    }

    /** A reference to an enclosing-function shortcut (alias), attached to ws — lowers to IRReference{ALIAS}. */
    private static RSymbolReference aliasRef(RShortcut shortcut, RWorkspace ws) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(shortcut.name());
        ref.setResolvedSymbol(shortcut);
        ref.attachToWorkspace(ws);
        return ref;
    }

    /** A plain RFunction callee (default {@code Origin.FUNCTION}) with the given inputs + output, attached to ws. */
    private static RFunction calleeFunction(RWorkspace ws, String name, List<RAttribute> inputs, RAttribute output) {
        RFunction func = new RFunction();
        func.setName(name);
        for (RAttribute in : inputs) {
            func.inputs().add(in);
            in.setParent(func);
            in.attachToWorkspace(ws);
        }
        func.setOutput(output);
        output.setParent(func);
        output.attachToWorkspace(ws);
        func.attachToWorkspace(ws);
        return func;
    }

    /** A call reference {@code name(args…)} whose symbol resolves to the given callee RFunction. */
    private static RSymbolReference callRef(String name, RFunction callee, RWorkspace ws, RExpression... args) {
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(callee);
        for (RExpression arg : args) {
            ref.args().add(arg);
        }
        ref.attachToWorkspace(ws);
        return ref;
    }

    @Test
    void closureParamHeadChainsMintOverTheirBinderElements() {
        // #505 arm-A1 (attributeChain.closureParamHead — 441 sole at the #504 SOT; the
        // enumSeatGate census read twData.leafHit dominant): a single-arrow `x -> inner`
        // inside an extract with declared param `x` parses as a disguised REnumValueRef;
        // the mint is FieldAccess{IRClosureParam} off the binder-source element (removing
        // adaptClosureParamHeadChain turns the positive red). The meta-leaf decline lock
        // pins the arm's own residue gate.
        String source = """
                namespace "test.step505a1"

                type Leg:
                    inner string (1..1)
                    metaAttr string (1..1)
                        [metadata scheme]

                type Box:
                    legs Leg (0..*)

                func UseCp:
                    inputs:
                        b Box (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs extract x [ x -> inner = "x" ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step505-a1.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef cpChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "x".equals(e.enumName()) && "inner".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `x -> inner` ENR parsed"));
        FieldAccess cpNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(cpChain, ws).orElseThrow(() -> new AssertionError(
                        "a closure-param-head chain must mint since the #505 arm-A1")),
                "the chain lowers to the composed nav");
        assertEquals("inner", cpNav.feature(), "the VALUE segment resolves by name");
        IRClosureParam cpBase = assertInstanceOf(IRClosureParam.class, cpNav.receiver(),
                "the head is the oracle-leaf closure-param base");
        assertEquals("x", cpBase.paramName(), "the head segment names the param");
        // The composed equality root rides the oracle-leaf routing (containsOracleLeaf).
        REqualityExpr cpEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream().findFirst().orElseThrow();
        assertInstanceOf(BinaryOp.class, adapter.adapt(cpEq, ws).orElseThrow(
                () -> new AssertionError("the equality over the minted chain composes")),
                "the composed root lowers whole");
        // The meta-leaf residue lock: `x -> metaAttr` declines at the arm's own gate.
        REnumValueRef metaLeafChain = new REnumValueRef();
        metaLeafChain.setEnumName("x");
        metaLeafChain.setValueName("metaAttr");
        metaLeafChain.setParent(cpChain.parent());
        metaLeafChain.attachToWorkspace(ws);
        assertTrue(adapter.adapt(metaLeafChain, ws).isEmpty(),
                "a meta-annotated leaf declines — the deref is legacy's own (the arm's gate)");
    }

    @Test
    void choiceOptionNavsMintTheirShallowKind() {
        // #505 arm-A2 (the choiceOption face — 239 sole at the #504 SOT, cdm6-dominant): a
        // single-arrow `c -> OptA` whose leaf the pass-6 #451 arm bound to the global
        // choice-option root element mints the DISTINCT shallow IRChoiceOptionNav (removing
        // the mint turns the positive red); the existence consumer admission composes the
        // root (removing the admission turns the composed assert red).
        String source = """
                namespace "test.step505a2"

                type OptA:
                    xa string (1..1)

                type OptB:
                    xb string (1..1)

                choice Cho:
                    OptA
                    OptB

                func UseOpt:
                    inputs:
                        c Cho (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        c -> OptA exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step505-a2.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef optNav = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "OptA".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `c -> OptA` ENR parsed"));
        assertTrue(optNav.resolvedChoiceOption().isPresent(),
                "the fixture's premise: the pass-6 arm bound the option");
        IRChoiceOptionNav minted = assertInstanceOf(IRChoiceOptionNav.class,
                adapter.adapt(optNav, ws).orElseThrow(() -> new AssertionError(
                        "a bound choice-option nav must mint since the #505 arm-A2")),
                "the disguise lowers to the shallow kind");
        assertEquals("c", minted.headName(), "the head segment is the neutral fact");
        assertEquals("OptA", minted.optionName(), "the option segment is the neutral fact");
        RExistenceExpr optExists = AstWalker.findAll(ws.files().get(0), RExistenceExpr.class)
                .stream().findFirst().orElseThrow();
        Existence optRoot = assertInstanceOf(Existence.class,
                adapter.adapt(optExists, ws).orElseThrow(() -> new AssertionError(
                        "the existence over the minted kind composes — the #505 consumer"
                                + " admission")),
                "the composed root lowers whole");
        assertInstanceOf(IRChoiceOptionNav.class, optRoot.arg(), "the admitted operand");
    }

    @Test
    void dispatchBaseInputRefsMintTheirShallowKind() {
        // #505 arm-C (symbolUnresolved.synthetic.absent — 232 sole at the #504 SOT; the
        // dispatchGate census proved 100% variant + base-input-hit): a variant-body bare
        // read of a BASE declaration input mints the DISTINCT shallow IRDispatchInputRef
        // (removing adaptDispatchBaseInputRef turns the positive red); the synthesized-nav
        // recovery resolves the feature on the input's declared type (the corpus's
        // interior.RFeatureCall class — removing the recovery leg turns the nav red); a
        // record-typed head keeps declining (the honest baseInput:date residue).
        String source = """
                namespace "test.step505c"

                enum DispatchE:
                    V1
                    V2

                type Pay:
                    amt number (1..1)

                func CalcThing:
                    inputs:
                        sel DispatchE (1..1)
                        startAmt number (1..1)
                        pay Pay (1..1)
                        startDate date (1..1)
                    output:
                        out number (1..1)

                func CalcThing(sel: DispatchE -> V1):
                    set out:
                        startAmt
                """;
        RModel model = AstBuilder.buildFromString(source, "step505-c.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        // The corpus class is the RE-ENTRANT SYNTHESIS pieces exactly (the #486 mechanism
        // decode: the face was 100% synthetic. — parse-time variant reads resolve through
        // the fork's own scope passes), so the positive is the hand-built up-only-parented
        // piece legacy's render machinery mints: a fresh receiver named for the base input.
        RSymbolReference parsedAnchor = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "startAmt".equals(r.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no variant-body anchor"));
        RSymbolReference bareRead = new RSymbolReference();
        bareRead.setName("startAmt");
        bareRead.setParent(parsedAnchor.parent());
        bareRead.attachToWorkspace(ws);
        assertTrue(bareRead.symbol().isEmpty(),
                "the fixture's premise: the synthesized piece is unresolved by construction");
        IRDispatchInputRef mintedInput = assertInstanceOf(IRDispatchInputRef.class,
                adapter.adapt(bareRead, ws).orElseThrow(() -> new AssertionError(
                        "a variant-body base-input read must mint since the #505 arm-C")),
                "the scope-join lowers to the shallow kind");
        assertEquals("startAmt", mintedInput.inputName(), "the neutral fact");
        assertFalse(mintedInput.type() == null || mintedInput.type().isMissing(),
                "the attribute channel types the input (the census's typeMissing nodes"
                        + " notwithstanding — the DECLARED typeCall is the source)");
        // The synthesized-nav recovery (the interior.RFeatureCall class): a fresh
        // up-only-parented receiver named `pay` under a nav to `amt` — the #486 re-entrant
        // synthesis shape mirrored exactly.
        RSymbolReference synHead = new RSymbolReference();
        synHead.setName("pay");
        RFeatureCall synNav = new RFeatureCall();
        synNav.setReceiver(synHead);
        synNav.setFeatureName("amt");
        synHead.setParent(synNav);
        synNav.setParent(bareRead.parent());
        synHead.attachToWorkspace(ws);
        synNav.attachToWorkspace(ws);
        FieldAccess recovered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(synNav, ws).orElseThrow(() -> new AssertionError(
                        "the dispatch-head nav must compose since the #505 recovery leg")),
                "the nav composes over the minted base");
        assertInstanceOf(IRDispatchInputRef.class, recovered.receiver(),
                "the head is the oracle-leaf dispatch-input base");
        // The #505 record-typed residue lock RECUT at #509 (the refine-in-place precedent):
        // the parsed-seat RECORD arm now MINTS the class — but this fixture carries NO
        // builtins, so `date` never resolves to the RRecordType and the mint's SEMANTIC gate
        // (declaredRecordTypeOf — no name-matching shortcut exists by design) keeps the nav
        // declining HERE: the belt this block now locks. The arm's acceptance positives live
        // in the BUILTINS-backed record witness (recordFeatureNavsMintTheirShallowKind —
        // the parsed-seat mint block), per the witness-fixture law.
        RSymbolReference recordHead = new RSymbolReference();
        recordHead.setName("startDate");
        RFeatureCall recordNav = new RFeatureCall();
        recordNav.setReceiver(recordHead);
        recordNav.setFeatureName("day");
        recordHead.setParent(recordNav);
        recordNav.setParent(bareRead.parent());
        recordHead.attachToWorkspace(ws);
        recordNav.attachToWorkspace(ws);
        assertTrue(adapter.adapt(recordNav, ws).isEmpty(),
                "an UNRESOLVABLE declared type keeps the parsed record nav declining — the"
                        + " #509 arm-B1 mint gates on the resolved RRecordType (this fixture"
                        + " carries no builtins; the acceptance positives are the"
                        + " builtins-backed record witness's)");
    }

    @Test
    void parsedAliasChoiceParamAndChoiceSuperArmsClaim() {
        // #509 arms A2/A1/B2/B4 (the composed A+B wave; the aliasIdGate/headUnGate/nsrGate
        // censuses at the #508 SOT):
        //   A2 — a CHOICE-typed alias body's option nav lowers at BOTH legs: a plain option
        //       to the shallow IRChoiceOptionNav at the alias-head flavor (offBody
        //       choice:*.mChoiceHit:plain, 47) and a META option to IRMetaAccess over the
        //       retyped alias receiver (mChoiceHit:meta, 9) — identity-served, never
        //       natively composed (removing the choice legs turns the positives red);
        //   A1 — the guard-held query: a plain DATA-body alias nav whose body the strong
        //       allowlist cannot prove (an args-call body — the #506 unprovable class)
        //       reads TRUE (the belt leg's serve slice, 143 at the SOT), while the arm-claimed
        //       choice rows read FALSE (not FieldAccess — the claim path owns them);
        //   B2 — a named closure param over an UNPROVEN meta-wrapped source resolves its
        //       element feature by the structural walk alone (cpBind.symNull/symInline
        //       .srcUnproven.*.twData.mHit:plain, 40) → FieldAccess over the IRClosureParam
        //       base (oracle-leaf routing — byte-safe regardless of the proof);
        //   B4 — a nav whose DATA-typed receiver's ANCESTOR is a choice resolves the
        //       type-named option through the findChoiceSuperOption mirror (the PR #207
        //       legacy lever; nsrGate dt:TransferableProduct.mMiss, 33) → FieldAccess with
        //       the projected option.
        String source = """
                namespace "test.step509"

                type Inner:
                    val number (1..1)

                choice Wrap:
                    Inner

                type Holder extends Wrap:
                    marker string (0..1)

                type Box:
                    hold Holder (1..1)

                type Item9:
                    price number (1..1)

                type Bag9:
                    items Item9 (0..*)

                func GetItems:
                    inputs:
                        bIn Bag9 (1..1)
                    output:
                        items Item9 (0..*)

                func UseArms:
                    inputs:
                        w Wrap (1..1)
                        b Bag9 (1..1)
                        box Box (1..1)
                    output:
                        out boolean (1..1)
                    alias cho: w
                    alias unprovable: b -> items then extract y [ y ]
                    alias extracted: GetItems(b) extract x [ x -> price ]
                    set out:
                        box -> hold -> Inner exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step509.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        var f = ws.files().get(0);
        // The corpus class is the PARSED RFeatureCall seat (the live carriers: RFeatureCall
        // sole rows with NO resolution channel — a single-arrow source spelling would parse
        // as the disguised REnumValueRef and ride the #505 bound-option arm instead), so the
        // positives are hand-built parsed navs at the fixture's own scopes — the standing
        // hand-built-piece convention.
        RFeatureCall setAnchor = AstWalker.findAll(f, RFeatureCall.class).stream()
                .filter(c -> "Inner".equals(c.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no `box -> hold -> Inner` outer hop parsed"));
        // A2 plain leg: `cho -> Inner` — the alias body types to the CHOICE, the option
        // resolves by type name, the nav lowers to the shallow kind at the alias-head flavor.
        RSymbolReference choHead = new RSymbolReference();
        choHead.setName("cho");
        RFeatureCall choNav = new RFeatureCall();
        choNav.setReceiver(choHead);
        choNav.setFeatureName("Inner");
        choHead.setParent(choNav);
        choNav.setParent(setAnchor.parent());
        choHead.attachToWorkspace(ws);
        choNav.attachToWorkspace(ws);
        IRChoiceOptionNav choMint = assertInstanceOf(IRChoiceOptionNav.class,
                adapter.adapt(choNav, ws).orElseThrow(() -> new AssertionError(
                        "a choice-body alias option nav must lower since the #509 arm-A2")),
                "the plain option lowers to the shallow kind at the alias-head flavor");
        assertEquals("cho", choMint.headName(), "the alias head is the neutral fact");
        assertEquals("Inner", choMint.optionName(), "the option segment is the neutral fact");
        // A2 meta leg: hand-annotate the option (the #499 fixture convention — no builtins
        // needed) and re-adapt: the SAME nav now lowers to IRMetaAccess over the retyped
        // alias receiver with the option's qualifier carried.
        RChoice wrapChoice = AstWalker.findAll(f, RChoice.class).stream()
                .filter(c -> "Wrap".equals(c.name())).findFirst().orElseThrow();
        RAnnotationRef optMeta = new RAnnotationRef();
        optMeta.setAnnotationName("metadata");
        optMeta.setQualifierName("reference");
        wrapChoice.options().get(0).annotationRefs().add(optMeta);
        IRMetaAccess choMetaMint = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(choNav, ws).orElseThrow(() -> new AssertionError(
                        "the meta option must lower through the A2 meta leg")),
                "a META-annotated option lowers to the distinct meta kind");
        assertEquals(List.of("reference"), choMetaMint.metaQualifiers(),
                "the option's qualifier is carried");
        assertInstanceOf(IRReference.class, choMetaMint.receiver(),
                "the retyped alias receiver");
        wrapChoice.options().get(0).annotationRefs().remove(optMeta);
        // A1 query: `unprovable -> price` — hand-built at the PARSED seat (the guard-held
        // class: the then-pipe body types CLEAN to the element form — the cache channel's
        // meta-stripping wrap — but its NAMED-extract tail sits OUTSIDE the strong
        // allowlist, the live b:RThenExpr face's own shape), so the query reads TRUE — the
        // belt leg's serve slice — and the adapter's claim site DECLINES the same nav (the
        // #508 null-out: the serve slice ≡ the declined slice, twin-exact). The choice-body
        // nav reads FALSE (the claim path owns it — not FieldAccess).
        RSymbolReference unprovableHead = new RSymbolReference();
        unprovableHead.setName("unprovable");
        RFeatureCall guardHeldNav = new RFeatureCall();
        guardHeldNav.setReceiver(unprovableHead);
        guardHeldNav.setFeatureName("price");
        unprovableHead.setParent(guardHeldNav);
        guardHeldNav.setParent(setAnchor.parent());
        unprovableHead.attachToWorkspace(ws);
        guardHeldNav.attachToWorkspace(ws);
        assertTrue(adapter.isGuardHeldParsedAliasNav(guardHeldNav, ws),
                "an unprovable-body plain alias nav is the identity-serve leg's slice"
                        + " (the #509 arm-A1 query)");
        assertTrue(adapter.adapt(guardHeldNav, ws).isEmpty(),
                "the claim site declines the SAME slice — the serve leg and the null-out"
                        + " are twin-exact");
        assertFalse(adapter.isGuardHeldParsedAliasNav(choNav, ws),
                "a choice-body nav is the claim path's — never the serve leg's");
        // B2: `x -> price` — the named param's binder source (`GetItems(b)`, an ARGS-PRESENT
        // call) is made GENUINELY unproven by hand-annotating the callee's OUTPUT attribute
        // (the #501 callee-output proof leg reads exactly that attribute's meta — the Seat-1
        // MF-1 catch: a clean output PROVES the call, and the #503 strong path would then
        // resolve the element itself, leaving this pin inert). The element still derives
        // through the structural walk (the callable-output law reads the declared TYPE,
        // annotation-independent), so the #509 arm-B2 widening alone resolves the feature →
        // FieldAccess over the IRClosureParam base (removing the widening turns this red).
        // The parsed in-lambda spelling is the disguise (single-arrow), so the positive is
        // the hand-built parsed nav anchored INSIDE the lambda (the binder walk's own scope).
        RFunction getItemsFn = AstWalker.findAll(f, RFunction.class).stream()
                .filter(fn -> "GetItems".equals(fn.name())).findFirst().orElseThrow();
        RAnnotationRef outMeta = new RAnnotationRef();
        outMeta.setAnnotationName("metadata");
        outMeta.setQualifierName("reference");
        getItemsFn.output().orElseThrow().annotationRefs().add(outMeta);
        REnumValueRef paramDisguise = AstWalker.findAll(f, REnumValueRef.class).stream()
                .filter(e -> "x".equals(e.enumName()) && "price".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `x -> price` parsed"));
        RSymbolReference paramHead = new RSymbolReference();
        paramHead.setName("x");
        RFeatureCall paramNav = new RFeatureCall();
        paramNav.setReceiver(paramHead);
        paramNav.setFeatureName("price");
        paramHead.setParent(paramNav);
        paramNav.setParent(paramDisguise.parent());
        paramHead.attachToWorkspace(ws);
        paramNav.attachToWorkspace(ws);
        FieldAccess paramLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(paramNav, ws).orElseThrow(() -> new AssertionError(
                        "an unproven-source named-param nav must resolve since the #509"
                                + " arm-B2 (the structural element walk alone)")),
                "the nav lowers over the param base");
        assertInstanceOf(IRClosureParam.class, paramLowered.receiver(),
                "the oracle-leaf base — the routing safety");
        // B4: the PARSED `box -> hold -> Inner` outer hop — the receiver (the typed
        // disguised input nav) lowers and types to the DATA type Holder whose choice
        // SUPERTYPE carries the option; the mirror projects it and the nav composes over
        // the lowered receiver (the live carriers' own nonSymbolReceiver shape —
        // recvLowers:FieldAccess + dt:<Type> at the census).
        FieldAccess superLowered = assertInstanceOf(FieldAccess.class,
                adapter.adapt(setAnchor, ws).orElseThrow(() -> new AssertionError(
                        "a choice-SUPERTYPE option nav must resolve since the #509"
                                + " arm-B4 (the findChoiceSuperOption mirror)")),
                "the projected option composes over the lowered receiver");
        assertEquals("Inner", superLowered.feature(), "the projected option's name");
    }

    @Test
    void aliasAndListOpArgumentsComposeIntoTheirCalls() {
        // #505 arm-B (argNav.alias.hop1 211 + arg:IRListOp 229 sole at the #504 SOT): an
        // alias-rooted nav argument and a list-op argument compose into their calls — the
        // argNavFacet alias-root admission and the arg-loop IRListOp admission (removing
        // either turns its positive red). The RENDER routes through the callArgs oracle
        // serve via the containsOracleLeaf SHAPE legs (compiler-side — the #504
        // conservation argument: both were 100%-decline faces pre-teach).
        String source = """
                namespace "test.step505b"

                type Thing:
                    val string (1..1)

                func Callee:
                    inputs:
                        s string (1..1)
                    output:
                        o string (1..1)
                    set o:
                        s

                func Caller:
                    inputs:
                        t Thing (1..1)
                    output:
                        out string (1..1)
                    alias th: t
                    set out:
                        Callee(th -> val)

                func Caller2:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (1..1)
                    set out:
                        Callee(xs only-element)
                """;
        RModel model = AstBuilder.buildFromString(source, "step505-b.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        List<RSymbolReference> calls = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "Callee".equals(r.name()) && !r.args().isEmpty()).toList();
        assertEquals(2, calls.size(), "both calls parsed");
        IRApply aliasArgCall = assertInstanceOf(IRApply.class,
                adapter.adapt(calls.get(0), ws).orElseThrow(() -> new AssertionError(
                        "a call with an alias-rooted nav arg must lower since the #505"
                                + " arm-B alias admission")),
                "the call composes whole");
        FieldAccess aliasArg = assertInstanceOf(FieldAccess.class, aliasArgCall.args().get(0),
                "the argument is the alias-rooted chain");
        IRReference aliasRoot = assertInstanceOf(IRReference.class, aliasArg.receiver());
        assertEquals(IRReference.ReferenceKind.ALIAS, aliasRoot.referenceKind(),
                "the chain roots at the alias reference");
        IRApply listOpArgCall = assertInstanceOf(IRApply.class,
                adapter.adapt(calls.get(1), ws).orElseThrow(() -> new AssertionError(
                        "a call with a list-op arg must lower since the #505 arm-B listOp"
                                + " admission")),
                "the call composes whole");
        assertInstanceOf(IRListOp.class, listOpArgCall.args().get(0),
                "the argument is the lowered collapse");
    }

    @Test
    void synItemSpineChainsLowerThroughTheAcceptedMint() {
        // #505 (the equivalentSynItemSpine face RESOLVED — 67 sole at the #504 SOT): a
        // bound 2-segment chain inside a META-SOURCED extract lowers to the
        // FieldAccess{IRSynItemNav} spine — the #504 mint broke the plain-spine
        // implication and the #505 seat conversion installed the enum family's oracle leg,
        // so the chain arm now ACCEPTS the minted spine (removing the acceptance turns
        // this red; the fixture is the #504 aw0tw1 class with a two-segment leaf).
        String source = """
                namespace "test.step505s"

                type Sub:
                    leaf string (1..1)

                type Item2:
                    sub Sub (1..1)

                type Wrap2:
                    itemRefs Item2 (0..*)
                        [metadata reference]

                func UseSpine:
                    inputs:
                        w Wrap2 (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        w -> itemRefs extract [ sub -> leaf = "x" ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step505-spine.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef spineChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "sub".equals(e.enumName()) && "leaf".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `sub -> leaf` ENR parsed"));
        FieldAccess spineTop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(spineChain, ws).orElseThrow(() -> new AssertionError(
                        "the IRSynItemNav-based spine must be accepted since #505 (the"
                                + " #504 exclusion's premise dissolved)")),
                "the chain lowers to the accepted spine");
        IRSynItemNav spineHead = assertInstanceOf(IRSynItemNav.class, spineTop.receiver(),
                "the un-retypeable head hop is the #504 mint");
        assertEquals("sub", spineHead.featureName(), "the head segment");
        assertEquals("leaf", spineTop.feature(), "the leaf segment");
    }

    @Test
    void thenBodyBindersJoinTheChainAndBareAttrArms() {
        // #506 arm-A (attributeChain.noFilterExtractBinder 323 sole + attrOutsideFunction
        // .noFilterExtractBinder 121 sole at the #505 SOT; the walkBindGate census read the
        // #505 chainNoBinder bodyNotFilterExtract class 320-of-368 THEN-bodied with the
        // element derivable and the head/leaf resolving in the dominant rows): the shared
        // binder walk (filterExtractSourceOfArmBinder) accepts an implicit THEN body — its
        // source is the pipe ARGUMENT (the #481 value identity) — so a disguised chain and
        // a bare attribute inside a pipe body claim off the argument's element (removing
        // the then leg turns both positives red). The disguised-ENR pipe argument reads the
        // cardinality computer's conservative SINGLE, so the #502-5d then-leg retype fires
        // and the spine is the PLAIN re-ranged form over the retyped synthetic item.
        String source = """
                namespace "test.step506a"

                type Sub6:
                    name string (1..1)

                type Leg6:
                    sub Sub6 (1..1)
                    inner string (1..1)

                type Box6:
                    legs Leg6 (0..*)

                func UseThenChain:
                    inputs:
                        b Box6 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        b -> legs then [ sub -> name ]

                func UseThenBare:
                    inputs:
                        b Box6 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        b -> legs then [ inner ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step506-a.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef thenChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "sub".equals(e.enumName()) && "name".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `sub -> name` ENR parsed"));
        FieldAccess thenTop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(thenChain, ws).orElseThrow(() -> new AssertionError(
                        "a then-body disguised chain must claim since the #506 arm-A")),
                "the chain lowers to the spine off the pipe argument's element");
        assertEquals("name", thenTop.feature(), "the VALUE segment resolves by name");
        FieldAccess thenHead = assertInstanceOf(FieldAccess.class, thenTop.receiver(),
                "the head hop is the plain re-ranged form (the retype fired)");
        assertEquals("sub", thenHead.feature(), "the head segment");
        IRVariable thenItemBase = assertInstanceOf(IRVariable.class, thenHead.receiver(),
                "the spine bottoms at the retyped synthetic item");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, thenItemBase.variableKind(),
                "the #502-5d then-leg retype stamped the argument's element");
        RSymbolReference thenBare = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(s -> "inner".equals(s.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no bare `inner` ref parsed"));
        FieldAccess bareNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(thenBare, ws).orElseThrow(() -> new AssertionError(
                        "a then-body bare attribute must claim since the #506 arm-A")),
                "the bare attr lowers to the single-hop nav over the retyped item");
        assertEquals("inner", bareNav.feature(), "the bare attr's own name");
        assertInstanceOf(IRVariable.class, bareNav.receiver(), "the retyped item base");
    }

    @Test
    void itemReceiversAdmitAcrossImplicitBodies() {
        // #506 arm-C (itemNotFilterExtractBound — 194 sole at the #505 SOT; the itemBind
        // census read the residue thenBody.msrc0 86 + implicit min/max/sort 106, ALL
        // typed + plain-leafed): the receiver-seat binding gate generalizes from the
        // filter/extract pair to the implicit then/min/max/sort bodies (the #502 ARG-seat
        // unbounding at the receiver seat — the #491 emitter renders the USER item via
        // legacy handle(RImplicitVariable) verbatim, so the binding decision is legacy's
        // own at emit time). An explicit `item -> <attr>` inside a THEN body and inside a
        // MAX comparator body lowers to the nav over the USER item (removing the widened
        // walk turns both red); a META-SOURCED then item keeps declining (the L-029 lock —
        // the generalized meta-source guard's own witness).
        String source = """
                namespace "test.step506c"

                type Leg6c:
                    inner string (1..1)
                    rank string (1..1)
                    mark string (1..1)

                type Box6c:
                    legs Leg6c (0..*)
                    mlegs Leg6c (0..*)
                        [metadata reference]

                func UseThenItem:
                    inputs:
                        b Box6c (1..1)
                    output:
                        out string (0..*)
                    set out:
                        b -> legs then [ item -> inner ]

                func UseMaxItem:
                    inputs:
                        b Box6c (1..1)
                    output:
                        out Leg6c (0..1)
                    set out:
                        b -> legs max [ item -> rank ]

                func UseMetaThenItem:
                    inputs:
                        b Box6c (1..1)
                    output:
                        out string (0..*)
                    set out:
                        b -> mlegs then [ item -> mark ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step506-c.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFeatureCall thenItemNav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "inner".equals(fc.featureName())
                        && fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError("no `item -> inner` parsed"));
        FieldAccess thenItemTop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(thenItemNav, ws).orElseThrow(() -> new AssertionError(
                        "a then-bound item receiver must admit since the #506 arm-C")),
                "the nav composes over the admitted item");
        IRVariable thenItem = assertInstanceOf(IRVariable.class, thenItemTop.receiver(),
                "the base is the item variable");
        assertEquals(IRVariable.VariableKind.USER_ITEM, thenItem.variableKind(),
                "the user item render stays legacy-verbatim (#491)");
        RFeatureCall maxItemNav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "rank".equals(fc.featureName())
                        && fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError("no `item -> rank` parsed"));
        FieldAccess maxItemTop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(maxItemNav, ws).orElseThrow(() -> new AssertionError(
                        "a max-comparator-bound item receiver must admit since the #506"
                                + " arm-C")),
                "the comparator nav composes over the admitted item");
        assertInstanceOf(IRVariable.class, maxItemTop.receiver(), "the item base");
        RFeatureCall metaItemNav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "mark".equals(fc.featureName())
                        && fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError("no `item -> mark` parsed"));
        // #508 arm-A5 (the #506 decline pin recut to the mint witness — the #486
        // refine-in-place precedent): the META-SOURCED then item now lowers to the DISTINCT
        // shallow IRMetaItemNav (the 30th kind). The L-029 split holds at the ROUTING level
        // instead of the decline: the kind has no native emitter arm and every containing
        // claim root oracle-serves whole-legacy, so the FieldWithMetaX coercion is STILL
        // legacy's own — byte-identical BY IDENTITY.
        IRExpr metaItemLowered = adapter.adapt(metaItemNav, ws).orElseThrow(
                () -> new AssertionError("the #508 arm-A5 mint must lower the meta-sourced"
                        + " item nav"));
        IRMetaItemNav metaItemMint = assertInstanceOf(IRMetaItemNav.class, metaItemLowered,
                "the distinct shallow kind — no native compose can ever read the hop");
        assertEquals("mark", metaItemMint.featureName(),
                "the resolved feature's name is the neutral fact carried");
    }

    @Test
    void sourceWalkWideningsProveTheCensusBottoms() {
        // #506 arm-B (the walk-widening cluster's element-derivation legs, both walks in
        // lockstep; every fixture claims through the #505 closure-param chain arm, whose
        // closureParamElementType gates the aw walk AND derives the tw walk — one fixture
        // proves both): DISTINCT and LAST join the element-form-preserving list-op set (39 +
        // 29 flat-stop census rows); the ALIAS-HEAD leg derives `<alias> -> <leaf>` sources
        // (the 171-row dominant cpTw bottom, mislabeled metaLeaf by the aw-centric namer);
        // the cpSym leg derives a NAMED param in source position (16 rows); the EMPTY-THEN
        // mirror derives the genuine else alone (the #487 rule's symmetric bottom); the
        // depth bound raised 8 -> 16 proves the real drr pipe chains (~170 deepNest rows).
        // Removing any single widening turns its fixture red.
        String source = """
                namespace "test.step506b"

                type Part6b:
                    pname string (1..1)

                type Sub6b:
                    name string (1..1)
                    part Part6b (1..1)

                type Leg6b:
                    sub Sub6b (1..1)
                    inner string (1..1)

                type Box6b:
                    flag boolean (1..1)
                    legs Leg6b (0..*)

                func UseDistinct:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs distinct extract xd [ xd -> inner = "d" ]

                func UseLast:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..1)
                    set out:
                        b -> legs last extract xl [ xl -> inner = "l" ]

                func UseAliasHead:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    alias legsA: b -> legs
                    set out:
                        legsA -> sub extract p [ p -> name = "a" ]

                func UseCpSym:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs extract x [ x extract y [ y -> inner = "y" ] ]

                func UseEmptyThen:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    alias condA: if b -> flag = True then empty else b -> legs
                    set out:
                        condA extract xc [ xc -> inner = "c" ]

                func UseDeep:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs
                            filter [ inner exists ] filter [ inner exists ]
                            filter [ inner exists ] filter [ inner exists ]
                            filter [ inner exists ] filter [ inner exists ]
                            filter [ inner exists ] filter [ inner exists ]
                            extract xf [ xf -> inner = "f" ]

                func UseInLambdaSrc:
                    inputs:
                        b Box6b (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs extract [ sub -> part extract q [ q -> pname = "q" ] ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step506-b.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        for (String[] fixture : new String[][] {
                {"xd", "inner", "the DISTINCT pass-through"},
                {"xl", "inner", "the LAST pass-through"},
                {"p", "name", "the ALIAS-HEAD source leg"},
                {"y", "inner", "the cpSym source leg"},
                {"xc", "inner", "the EMPTY-THEN mirror rule"},
                {"xf", "inner", "the raised depth bound"},
                {"q", "pname", "the FUNCTION-scope in-lambda source leg (probe5's residue"
                        + " class — the bound-chain channel the aw walk always read)"}}) {
            REnumValueRef cpChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                    .stream().filter(e -> fixture[0].equals(e.enumName())
                            && fixture[1].equals(e.valueName()))
                    .findFirst().orElseThrow(() -> new AssertionError(
                            "no `" + fixture[0] + " -> " + fixture[1] + "` ENR parsed"));
            FieldAccess cpNav = assertInstanceOf(FieldAccess.class,
                    adapter.adapt(cpChain, ws).orElseThrow(() -> new AssertionError(
                            fixture[2] + " must derive the binder element since the #506"
                                    + " arm-B")),
                    fixture[2] + " claims through the #505 cp chain arm");
            assertInstanceOf(IRClosureParam.class, cpNav.receiver(),
                    fixture[2] + " roots at the closure-param base");
            assertEquals(fixture[1], cpNav.feature(), fixture[2] + " resolves the leaf");
        }
    }

    @Test
    void deepFeatureCallsMintTheirKindAndCompose() {
        // #507 arm-A (RDeepFeatureCall:noAdaptArm — 191 sole + 71 untargeted at the #506 SOT,
        // all cdm6 FUNCTION; the deepGate census read 100% featHit + 100% recvLowers): a
        // `c ->> common` deep call whose linker channel bound the common attribute mints
        // IRDeepFeatureNav over the lowered receiver (removing adaptDeepFeatureCall turns the
        // positive red); the chained-hop admission composes FieldAccess over it and the
        // existence operand admission composes the root (removing either admission turns its
        // composed assert red); the META deep feature keeps declining with the arm's own
        // token (the L-029 conservative belt — the census's explicit-zero face), and an
        // unresolved deep feature declines at the linker-channel gate.
        String source = """
                namespace "test.step507a"

                type Leafy:
                    leaf string (1..1)

                type OptA:
                    common Leafy (1..1)
                    metaCommon string (1..1)
                        [metadata scheme]

                type OptB:
                    common Leafy (1..1)
                    metaCommon string (1..1)
                        [metadata scheme]

                choice Cho:
                    OptA
                    OptB

                func UseDeep:
                    inputs:
                        c Cho (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        c ->> common -> leaf exists

                func UseDeepMeta:
                    inputs:
                        cm Cho (1..1)
                    output:
                        outM string (1..1)
                    set outM:
                        cm ->> metaCommon
                """;
        RModel model = AstBuilder.buildFromString(source, "step507-a.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RDeepFeatureCall deep = AstWalker.findAll(ws.files().get(0), RDeepFeatureCall.class)
                .stream().filter(d -> "common".equals(d.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `c ->> common` parsed"));
        assertTrue(deep.resolvedFeature().isPresent(),
                "the fixture's premise: the linker's deep channel bound the common attribute");
        IRDeepFeatureNav minted = assertInstanceOf(IRDeepFeatureNav.class,
                adapter.adapt(deep, ws).orElseThrow(() -> new AssertionError(
                        "a resolved deep call must mint since the #507 arm-A")),
                "the deep call lowers to its kind");
        assertEquals("common", minted.featureName(), "the neutral fact");
        assertInstanceOf(IRVariable.class, minted.receiver(),
                "the receiver subtree is a real IR child (the census's recvLowers fact)");
        // The chained-hop admission: `... ->> common -> leaf` composes FieldAccess over the
        // oracle leaf (the census's interior.RFeatureCall continuation shape).
        RFeatureCall chained = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "leaf".equals(fc.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError("no chained hop parsed"));
        FieldAccess hop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(chained, ws).orElseThrow(() -> new AssertionError(
                        "the chained hop must compose since the #507 receiver admission")),
                "the continuation composes");
        assertInstanceOf(IRDeepFeatureNav.class, hop.receiver(), "the hop's oracle-leaf base");
        // The existence operand admission: the whole `... exists` root composes.
        RExistenceExpr exists = AstWalker.findAll(ws.files().get(0), RExistenceExpr.class)
                .stream().findFirst().orElseThrow();
        assertInstanceOf(Existence.class,
                adapter.adapt(exists, ws).orElseThrow(() -> new AssertionError(
                        "the existence over the deep chain composes — the #507 admission")),
                "the composed root lowers whole");
        // The META decline lock: the arm's own token, count ZERO in the corpus census (the
        // conservative L-029 belt — a future flip removes exactly this token).
        RDeepFeatureCall metaDeep = AstWalker.findAll(ws.files().get(0), RDeepFeatureCall.class)
                .stream().filter(d -> "metaCommon".equals(d.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError("no meta deep call parsed"));
        assertTrue(metaDeep.resolvedFeature().isPresent(), "the meta fixture's premise");
        assertTrue(adapter.adapt(metaDeep, ws).isEmpty(),
                "a meta-annotated deep feature keeps declining (the L-029 belt)");
        assertEquals("deepFeatureMeta", adapter.declineReason(metaDeep, ws).orElseThrow(),
                "the mirror names the arm's meta gate");
        // The unresolved-feature decline: a hand-built deep call with no linker bind (the
        // #505 hand-built-piece convention) names the channel gate.
        RSymbolReference ghostHead = new RSymbolReference();
        ghostHead.setName("c");
        RDeepFeatureCall ghost = new RDeepFeatureCall();
        ghost.setReceiver(ghostHead);
        ghost.setFeatureName("ghost");
        ghostHead.setParent(ghost);
        ghost.setParent(deep.parent());
        ghostHead.attachToWorkspace(ws);
        ghost.attachToWorkspace(ws);
        assertTrue(adapter.adapt(ghost, ws).isEmpty(),
                "an unresolved deep feature declines at the linker-channel gate");
        assertEquals("deepFeatureUnresolved", adapter.declineReason(ghost, ws).orElseThrow(),
                "the mirror names the channel gate");
    }

    @Test
    void recordFeatureNavsMintTheirShallowKind() throws IOException {
        // #507 arm-B (REnumValueRef:noResolutionChannel — 175 sole at the #506 SOT; the
        // noChanGate census read the dominant faces dispatchInput.rec:date.leafRecHit +
        // input.rec:date/zonedDateTime.leafRecHit): a `startDate -> day` record-feature read
        // over a record-typed input mints the shallow IRRecordFeatureNav (removing
        // adaptRecordFeatureNav turns the positive red); the numeric-literal sibling
        // admission composes the `> 29` comparison root (removing the sibling widening turns
        // the composed assert red); the dispatch-variant read over the BASE input mints
        // through the #505 scope-join; the decline mirror names the arm's own gates
        // (headMiss / headNotRecord / leafMiss — the refined noResolutionChannel.<facet>
        // tokens, the #486 refine-in-place precedent).
        String source = """
                namespace "test.step507b"

                type Pay:
                    amt number (1..1)

                enum SelE:
                    V1

                func UseRecord:
                    inputs:
                        startDate date (1..1)
                        pay Pay (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        startDate -> day > 29

                func CalcDisp:
                    inputs:
                        sel SelE (1..1)
                        baseDate date (1..1)
                    output:
                        o int (1..1)

                func CalcDisp(sel: SelE -> V1):
                    set o:
                        baseDate -> year
                """;
        // The record types (`date`/`zonedDateTime`) are BUILTINS — the fixture workspace must
        // carry basictypes.rosetta for the head's declared typeCall to resolve to the
        // RRecordType (the arm's semantic gate; no name-matching shortcut exists by design).
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        models.add(AstBuilder.buildFromString(source, "step507-b.rosetta"));
        RWorkspace ws = RWorkspace.build(models).workspace();
        var fixtureFile = ws.files().get(ws.files().size() - 1);
        REnumValueRef dayRead = AstWalker.findAll(fixtureFile, REnumValueRef.class)
                .stream().filter(e -> "day".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `startDate -> day` parsed"));
        assertTrue(dayRead.enumeration().isEmpty() && dayRead.resolvedSymbol().isEmpty()
                        && dayRead.resolvedInputFeature().isEmpty()
                        && dayRead.resolvedAttributeChain().isEmpty()
                        && dayRead.resolvedChoiceOption().isEmpty()
                        && dayRead.resolvedTypeRestriction().isEmpty(),
                "the fixture's premise: the resolver binds NO channel for a record read"
                        + " (the #444 clear-without-binding law)");
        IRRecordFeatureNav minted = assertInstanceOf(IRRecordFeatureNav.class,
                adapter.adapt(dayRead, ws).orElseThrow(() -> new AssertionError(
                        "a record-feature read must mint since the #507 arm-B")),
                "the no-channel read lowers to the shallow kind");
        assertEquals("startDate", minted.headName(), "the head segment is the neutral fact");
        assertEquals("day", minted.featureName(), "the feature segment is the neutral fact");
        assertEquals("date", minted.recordTypeName(), "the record type is the neutral fact");
        // The numeric-literal sibling admission: the `> 29` comparison composes whole.
        RComparisonExpr cmp = AstWalker.findAll(fixtureFile, RComparisonExpr.class)
                .stream().findFirst().orElseThrow();
        BinaryOp cmpRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(cmp, ws).orElseThrow(() -> new AssertionError(
                        "the comparison over the record read composes — the #507 sibling"
                                + " admission (the record teach's own exposed frontier)")),
                "the composed root lowers whole");
        assertInstanceOf(IRRecordFeatureNav.class, cmpRoot.left(), "the admitted operand");
        // The dispatch-variant read over the BASE input (the YearFraction-family class —
        // the census's dominant dispatchInput.rec:date face) mints through the #505
        // scope-join shared verbatim.
        REnumValueRef variantRead = AstWalker.findAll(fixtureFile, REnumValueRef.class)
                .stream().filter(e -> "year".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no variant-body read parsed"));
        IRRecordFeatureNav dispatchMint = assertInstanceOf(IRRecordFeatureNav.class,
                adapter.adapt(variantRead, ws).orElseThrow(() -> new AssertionError(
                        "a dispatch-base record read must mint since the #507 arm-B")),
                "the dispatch-base head resolves through the shared scope-join");
        assertEquals("baseDate", dispatchMint.headName(), "the base input's name");
        // #509 arm-B1 — the PARSED-seat twin of the SAME class (the #505 record-typed lock
        // RECUT: the headUnGate census read the RFeatureCall face 72/72
        // dispatchInput.rec:date.mRecHit): a hand-built `baseDate -> year` RFeatureCall at
        // the variant body mints the SAME shallow kind through the SAME core (removing the
        // parsed record arm turns this positive red); a bound plain-input head
        // (`startDate -> day` spelled as the parsed nav with the RESOLVED head symbol —
        // the headAttr-face sibling) mints through the bound-attribute flavor.
        RSymbolReference parsedDispatchHead = new RSymbolReference();
        parsedDispatchHead.setName("baseDate");
        RFeatureCall parsedDispatchNav = new RFeatureCall();
        parsedDispatchNav.setReceiver(parsedDispatchHead);
        parsedDispatchNav.setFeatureName("year");
        parsedDispatchHead.setParent(parsedDispatchNav);
        parsedDispatchNav.setParent(variantRead.parent());
        parsedDispatchHead.attachToWorkspace(ws);
        parsedDispatchNav.attachToWorkspace(ws);
        IRRecordFeatureNav parsedDispatchMint = assertInstanceOf(IRRecordFeatureNav.class,
                adapter.adapt(parsedDispatchNav, ws).orElseThrow(() -> new AssertionError(
                        "a record-typed base input's PARSED nav must mint since the #509"
                                + " arm-B1 (the #507 core at the RFeatureCall seat)")),
                "the parsed record read lowers to the shallow kind");
        assertEquals("baseDate", parsedDispatchMint.headName(), "the head segment");
        assertEquals("year", parsedDispatchMint.featureName(), "the record feature");
        assertEquals("date", parsedDispatchMint.recordTypeName(), "the record type");
        // The membership rule stays prove-or-decline at the parsed seat too.
        RSymbolReference parsedGhostHead = new RSymbolReference();
        parsedGhostHead.setName("baseDate");
        RFeatureCall parsedGhostNav = new RFeatureCall();
        parsedGhostNav.setReceiver(parsedGhostHead);
        parsedGhostNav.setFeatureName("notAFeature");
        parsedGhostHead.setParent(parsedGhostNav);
        parsedGhostNav.setParent(variantRead.parent());
        parsedGhostHead.attachToWorkspace(ws);
        parsedGhostNav.attachToWorkspace(ws);
        assertTrue(adapter.adapt(parsedGhostNav, ws).isEmpty(),
                "a leaf off the record's own feature list keeps declining at the parsed"
                        + " seat (the #444 membership rule)");
        // The decline mirror's three gates, each with a hand-built piece (the #505
        // convention): a miss on the record's own feature list, a non-record head, and an
        // unresolvable head.
        REnumValueRef leafGhost = new REnumValueRef();
        leafGhost.setEnumName("startDate");
        leafGhost.setValueName("ghost");
        leafGhost.setParent(dayRead.parent());
        leafGhost.attachToWorkspace(ws);
        assertTrue(adapter.adapt(leafGhost, ws).isEmpty(), "a non-feature leaf declines");
        assertEquals("noResolutionChannel.leafMiss",
                adapter.declineReason(leafGhost, ws).orElseThrow(),
                "the mirror names the record-feature membership gate");
        REnumValueRef dataHead = new REnumValueRef();
        dataHead.setEnumName("pay");
        dataHead.setValueName("day");
        dataHead.setParent(dayRead.parent());
        dataHead.attachToWorkspace(ws);
        assertTrue(adapter.adapt(dataHead, ws).isEmpty(), "a data-typed head declines here");
        assertEquals("noResolutionChannel.headNotRecord",
                adapter.declineReason(dataHead, ws).orElseThrow(),
                "the mirror names the record-type gate");
        REnumValueRef ghostHead = new REnumValueRef();
        ghostHead.setEnumName("ghost");
        ghostHead.setValueName("day");
        ghostHead.setParent(dayRead.parent());
        ghostHead.attachToWorkspace(ws);
        assertTrue(adapter.adapt(ghostHead, ws).isEmpty(), "an unresolvable head declines");
        assertEquals("noResolutionChannel.headMiss.other",
                adapter.declineReason(ghostHead, ws).orElseThrow(),
                "the mirror names the honest residue (the census's lambda-scope class;"
                        + " #514: the non-cp residue spelled .other by the in-place split)");
    }

    @Test
    void metaSingleHopNavsNameTheirShapeExit() {
        // #507 (the l111Residue census's unattributed-119 decode) → #508 arm-A1 (the
        // acceptance): a single-hop read of a META-annotated input feature — `h -> par` with
        // [metadata scheme] — lowers its legacy-equivalent through the #499 IRMetaAccess arm.
        // Pre-#507 the L-111 rebuild's FieldAccess-shape exit declined it with NO mirror twin
        // (the meter's standing 93); #507 minted the metaFeature token; #508 ACCEPTS the
        // shape (the metaHopGate census read the class 100% recv:IRVariable, all typed): the
        // arm returns the re-ranged IRMetaAccess over the typed input-param head, the
        // compiler seat's #507 identity-serve leg renders the root BY IDENTITY, and the
        // pre-#508 decline pin recuts to this acceptance witness (the #486 refine-in-place
        // precedent; the metaFeature token stays as the off-receiver defensive belt).
        String source = """
                namespace "test.step507c"

                type Holder:
                    par string (1..1)
                        [metadata scheme]

                func UseMeta:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (1..1)
                    set out:
                        h -> par
                """;
        RModel model = AstBuilder.buildFromString(source, "step507-c.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        REnumValueRef metaRead = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "par".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `h -> par` parsed"));
        assertTrue(metaRead.resolvedInputFeature().isPresent(),
                "the fixture's premise: the typing engine bound the L-111 channel");
        IRExpr metaLowered = adapter.adapt(metaRead, ws).orElseThrow(
                () -> new AssertionError("the #508 arm-A1 acceptance must lower the class"));
        IRMetaAccess metaHop = assertInstanceOf(IRMetaAccess.class, metaLowered);
        assertInstanceOf(IRVariable.class, metaHop.receiver(),
                "the accepted shape — the typed input-param head (the census's 100% fact)");
        assertEquals("par", metaHop.feature());
        assertEquals(List.of("scheme"), metaHop.metaQualifiers());
        assertEquals(Optional.empty(), adapter.declineReason(metaRead, ws),
                "the mirror restates the acceptance (the twin reads UNATTRIBUTED-empty)");
    }

    @Test
    void aliasHeadMetaLegsLowerBothSeatsAndThePlainSeatTakesTheStrongGuard() {
        // #508 arm-A2 + arm-B1 (the metaHopGate census: aliasMeta.enr 99 · aliasMeta.fc 67 ·
        // aliasPlain 143 sole at the #507 SOT, heads 100% name-collision, bodies/hops typed):
        // the shared #492 core gains the META leg — `<shortcut> -> <metaFeature>` lowers to
        // IRMetaAccess over the BODY-retyped IRReference{ALIAS} at BOTH spelling seats (the
        // disguised REnumValueRef and the parsed RFeatureCall), served through the standing
        // meta-root oracle routing BY IDENTITY. The parsed seat's PLAIN leg carries the
        // STRONG body allowlist (isProvablyNonMetaElementSource, prove-or-decline — the
        // probe2 catch: an engine-clean body whose Java value is a FieldWithMetaX Mapper
        // rendered the native alias-nav WITHOUT legacy's "Type coercion" deref legs): a
        // provable body claims, a meta-terminal body KEEPS the probed decline spelling (the
        // lock — the #326 weaker-oracle class stays on legacy's own render).
        String source = """
                namespace "test.step508ab"

                type Leg8:
                    rate number (1..1)
                    mark string (1..1)
                        [metadata scheme]

                type Trade8:
                    leg Leg8 (1..1)
                    mleg Leg8 (1..1)
                        [metadata reference]

                func FnNav8:
                    inputs:
                        trade Trade8 (1..1)
                    output:
                        out number (1..1)
                    alias legAlias: trade -> leg
                    alias metaBodyAlias: trade -> mleg
                    set out:
                        trade -> leg -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step508-ab.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction fnNav = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "FnNav8".equals(f.name())).findFirst().orElseThrow();

        // Leg 1 — the enr-seat META claim: `legAlias -> mark` lowers IRMetaAccess over the
        // typed ALIAS receiver (the #499 build; qualifiers from the feature's annotations).
        REnumValueRef metaDisguise = new REnumValueRef();
        metaDisguise.setEnumName("legAlias");
        metaDisguise.setValueName("mark");
        metaDisguise.setResolvedInputFeature(scalarParam("mark"));
        metaDisguise.setParent(fnNav);
        metaDisguise.attachToWorkspace(ws);
        IRMetaAccess enrMeta = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(metaDisguise, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-A2 enr-seat meta leg must lower")),
                "the meta hop takes the DISTINCT kind, not a FieldAccess");
        IRReference enrMetaRecv = assertInstanceOf(IRReference.class, enrMeta.receiver());
        assertEquals(IRReference.ReferenceKind.ALIAS, enrMetaRecv.referenceKind());
        assertFalse(enrMetaRecv.type().isMissing(), "the receiver carries the BODY's type");
        assertEquals(List.of("scheme"), enrMeta.metaQualifiers());
        assertEquals(Optional.empty(), adapter.declineReason(metaDisguise, ws),
                "the twin restates the meta-leg claim");

        // Leg 2 — the PARSED-seat META claim (the fc twin of leg 1; no strong guard — the
        // meta-root oracle routing is the safety).
        RSymbolReference fcMetaHead = new RSymbolReference();
        fcMetaHead.setName("legAlias");
        RFeatureCall fcMeta = new RFeatureCall();
        fcMeta.setReceiver(fcMetaHead);
        fcMeta.setFeatureName("mark");
        fcMetaHead.setParent(fcMeta);
        fcMeta.setParent(fnNav);
        fcMeta.attachToWorkspace(ws);
        IRMetaAccess fcMetaLowered = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(fcMeta, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-A2 parsed-seat meta leg must lower")),
                "the parsed seat lowers the same meta form");
        assertEquals(IRReference.ReferenceKind.ALIAS,
                assertInstanceOf(IRReference.class, fcMetaLowered.receiver()).referenceKind());
        assertEquals(Optional.empty(), adapter.declineReason(fcMeta, ws));

        // Leg 3 — the PARSED-seat PLAIN claim on a PROVABLE body (`trade -> leg`, a non-meta
        // single-disguise the allowlist proves): the #492 render's own FieldAccess form.
        RSymbolReference fcPlainHead = new RSymbolReference();
        fcPlainHead.setName("legAlias");
        RFeatureCall fcPlain = new RFeatureCall();
        fcPlain.setReceiver(fcPlainHead);
        fcPlain.setFeatureName("rate");
        fcPlainHead.setParent(fcPlain);
        fcPlain.setParent(fnNav);
        fcPlain.attachToWorkspace(ws);
        FieldAccess fcPlainNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(fcPlain, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-B1 parsed-seat plain leg must lower a proven body")),
                "the plain leg keeps the #492 FieldAccess form");
        assertEquals(IRReference.ReferenceKind.ALIAS,
                assertInstanceOf(IRReference.class, fcPlainNav.receiver()).referenceKind());
        assertEquals(Optional.empty(), adapter.declineReason(fcPlain, ws));

        // Leg 4 — THE STRONG-GUARD LOCK: the same plain nav over a META-TERMINAL body
        // (`trade -> mleg`, [metadata reference] — the StockSplit/Qualify_Reset corpus class)
        // DECLINES and keeps the probed spelling: legacy's null-safe "Type coercion" deref
        // legs cannot be composed natively, and the parsed seat has no identity-serve leg
        // (the #509 banked candidate). Removing the guard turns this red via probe2's exact
        // byte-divergence class.
        RSymbolReference fcGuardHead = new RSymbolReference();
        fcGuardHead.setName("metaBodyAlias");
        RFeatureCall fcGuarded = new RFeatureCall();
        fcGuarded.setReceiver(fcGuardHead);
        fcGuarded.setFeatureName("rate");
        fcGuardHead.setParent(fcGuarded);
        fcGuarded.setParent(fnNav);
        fcGuarded.attachToWorkspace(ws);
        assertTrue(adapter.adapt(fcGuarded, ws).isEmpty(),
                "the unproven body declines — the FieldWithMetaX coercion is legacy's own");
        assertEquals(Optional.of("featureUnresolved.aliasHead.bodyTyped.featureOnBody"),
                adapter.declineReason(fcGuarded, ws),
                "the honest residue keeps the probed spelling (the guard's lock)");
    }

    @Test
    void metaChainSpinesAndTwNullChainsMintTheirCensusShapes() {
        // #508 arm-A3 + arm-A4 (the metaHopGate chainMeta census: 137 sole, 100% ONE shape
        // top:IRMetaAccess.inner:IRSynItemNav.metaLeaf.scheme, all typed · the metaSrcGate
        // cpSrc census: 118 sole, all typed, leafPlain everywhere): a meta-LEAF implicit-item
        // chain over an UN-RETYPEABLE binder source lowers as IRMetaAccess{IRSynItemNav} —
        // the meta twin of the #505 synTop acceptance — and a closure-param-head chain whose
        // element walk declined (the L-029 meta-sourced keep) mints FieldAccess over the
        // IRClosureParam base off the node's OWN engine type (no element proof, no leaf
        // attribute — the oracle render re-derives everything BY IDENTITY; the UNTYPED
        // residue keeps the twNull decline).
        String source = """
                namespace "test.step508cd"

                type Inner8:
                    mark string (1..1)
                        [metadata scheme]
                    plainLeaf string (1..1)

                type Leg8c:
                    inner Inner8 (1..1)

                type Box8:
                    mlegs Leg8c (0..*)
                        [metadata reference]

                func UseChains:
                    inputs:
                        b Box8 (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> mlegs filter [ inner -> mark exists ]

                func UseCpChain:
                    inputs:
                        b Box8 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        b -> mlegs extract x [ x -> inner -> plainLeaf ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step508-cd.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // Leg 1 — the A3 spine acceptance: `inner -> mark` (head un-retypeable over the
        // meta-sourced filter item, leaf [metadata scheme]) lowers IRMetaAccess over the
        // IRSynItemNav head, both re-ranged to the raw node.
        REnumValueRef metaChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "inner".equals(e.enumName())
                        && "mark".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `inner -> mark` parsed"));
        IRMetaAccess spineTop = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(metaChain, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-A3 acceptance must lower the meta spine")),
                "the accepted top is the meta hop");
        IRSynItemNav spineHead = assertInstanceOf(IRSynItemNav.class, spineTop.receiver(),
                "the head hop is the #504 un-retypeable mint (the census's 100% shape)");
        assertEquals("inner", spineHead.featureName());
        assertEquals(List.of("scheme"), spineTop.metaQualifiers());
        assertEquals(Optional.empty(), adapter.declineReason(metaChain, ws),
                "the twin restates the widened spine acceptance");

        // Leg 2 — the A4 typed-node mint: `x -> inner` (hop typed by the engine) over the
        // meta-sourced extract binder (closureParamElementType nulls BY DESIGN) mints the
        // FieldAccess over the IRClosureParam base with the node's OWN engine type.
        REnumValueRef cpChain = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream().filter(e -> "x".equals(e.enumName()) && "inner".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no `x -> inner` parsed"));
        FieldAccess cpNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(cpChain, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-A4 typed-node leg must mint over the meta-sourced"
                                + " binder")),
                "the chain lowers over the oracle-leaf base");
        IRClosureParam cpBase = assertInstanceOf(IRClosureParam.class, cpNav.receiver());
        assertEquals("x", cpBase.paramName());
        assertNotNull(cpNav.type(), "the hop carries the node's own engine type");
        assertFalse(cpNav.type().isMissing(),
                "the typed-node gate is the leg's whole admission");
        assertEquals(Optional.empty(), adapter.declineReason(cpChain, ws),
                "the twin restates the typed-node claim");
    }

    @Test
    void bareEnumRequalifiesNodeLocallyAtTheLegacyRecoverySeats() {
        // #508 arm-C (the symNotGate census: 166 sole → the argFires/eqFires context-loss
        // slice + the excluded residue): a bare reference the linker false-bound to a
        // colliding TYPE root requalifies NODE-LOCALLY against the parent seat's expected
        // enum — the call-ARG seat (legacy tryBareEnumArg's ladder — the #504 mirror reused
        // verbatim) and the EQUALITY seat (tryBareEnumComparand's). Every OTHER parent seat
        // keeps declining (legacy has no recovery there — the seat-scope lock).
        String source = """
                namespace "test.step508e"

                type Shade:
                    dummy string (1..1)

                enum ColourEnum8:
                    Shade
                    Blue

                func Callee8:
                    inputs:
                        colour ColourEnum8 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        True

                func UseBare:
                    inputs:
                        c ColourEnum8 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        True
                """;
        RModel model = AstBuilder.buildFromString(source, "step508-e.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction useBare = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "UseBare".equals(f.name())).findFirst().orElseThrow();
        RFunction callee = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "Callee8".equals(f.name())).findFirst().orElseThrow();
        RDataType shadeType = AstWalker.findAll(ws.files().get(0), RDataType.class).stream()
                .filter(t -> "Shade".equals(t.name())).findFirst().orElseThrow();

        // Leg 1 — the ARG seat: `Callee8(Shade)` with Shade false-bound to the TYPE (the
        // #368 type-shadow class) requalifies against the callee's declared enum param.
        RSymbolReference bareArg = new RSymbolReference();
        bareArg.setName("Shade");
        bareArg.setResolvedSymbol(shadeType);
        RSymbolReference call = new RSymbolReference();
        call.setName("Callee8");
        call.setResolvedSymbol(callee);
        call.args().add(bareArg);
        bareArg.setParent(call);
        call.setParent(useBare);
        call.attachToWorkspace(ws);
        bareArg.attachToWorkspace(ws);
        IRReference argMint = assertInstanceOf(IRReference.class,
                adapter.adapt(bareArg, ws).orElseThrow(() -> new AssertionError(
                        "the #508 arm-C arg-seat requalify must mint node-locally")),
                "the mint is the #504 enum constant");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, argMint.referenceKind());
        assertEquals("Shade", argMint.target());
        assertEquals(Optional.empty(), adapter.declineReason(bareArg, ws),
                "the twin restates the arg-seat claim");

        // Leg 2 — THE SEAT-SCOPE LOCK: the same false-bound bare at a NON-recovery parent
        // (a bare set/return position) keeps the probed decline spelling — legacy has no
        // recovery outside the arg/equality seats, so the node must not lower there.
        RSymbolReference bareElsewhere = new RSymbolReference();
        bareElsewhere.setName("Shade");
        bareElsewhere.setResolvedSymbol(shadeType);
        bareElsewhere.setParent(useBare);
        bareElsewhere.attachToWorkspace(ws);
        assertTrue(adapter.adapt(bareElsewhere, ws).isEmpty(),
                "a non-seat parent keeps declining (the seat-scope lock)");
        assertEquals(Optional.of("symbolNotAttribute.sym:RDataType.p:RFunction"),
                adapter.declineReason(bareElsewhere, ws),
                "the honest residue keeps the probed spelling (the #514 sym/parent facets"
                        + " refined in place)");
    }

    @Test
    void operandKindAdmissionsClaimBinaryRootsAcrossSeats() {
        // #510 arms A1/A2/A3/A5 (the composed A+D wave — the operand cluster): the equality
        // gate admits a lowered meta access (operand:IRMetaAccess, 78 equality sole at the
        // #509 SOT — the metaAccessGate census's 100% low2 meta-vs-meta pairs) and a MULTI
        // nav (operandMultiNav, 76 — the shape leg's class); the arithmetic gate admits a
        // lowered nav (operandNav, 96); the logical gate admits a lowered conditional
        // (operand:IRConditional, 86). Every admitted root renders WHOLE through the
        // oracle-root serves (the standing IRMetaAccess kind walk + the #510 shape legs in
        // containsOracleLeaf — byte-identical BY IDENTITY), so the adapter-side witnesses
        // assert the CLAIMS (declineReason empty ⇔ lowers, the mirror law) and the operand
        // kinds in place.
        String source = """
                namespace "test.step510a"

                type Sub10:
                    rate number (1..1)
                    codeA string (1..1)
                        [metadata scheme]
                    codeB string (1..1)
                        [metadata scheme]

                type Box10:
                    leg Sub10 (1..1)
                    legs Sub10 (0..*)
                    amount number (1..1)

                func EqMeta10:
                    inputs:
                        b Box10 (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        b -> legs extract [ codeA = codeB ]

                func EqMultiNav10:
                    inputs:
                        b Box10 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> legs -> rate = b -> amount

                func ArithNav10:
                    inputs:
                        b Box10 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        b -> leg -> rate + 1

                func LogCond10:
                    inputs:
                        b Box10 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        (if b -> amount > 1 then True else False) and b -> amount exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step510-operands.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // A1 — the meta-vs-meta equality: both operands lower to IRMetaAccess and the pair
        // claims (the corpus class: reference=reference / scheme=scheme / location=address).
        REqualityExpr eqMeta = firstUnder(ws, "EqMeta10", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqMeta, ws),
                "a meta-vs-meta equality admits since the #510 arm-A1");
        BinaryOp eqMetaIr = assertInstanceOf(BinaryOp.class, adapt(eqMeta, ws));
        assertInstanceOf(IRMetaAccess.class, eqMetaIr.left(),
                "the left operand lowers to the meta access in place");
        assertInstanceOf(IRMetaAccess.class, eqMetaIr.right(),
                "the right operand lowers to the meta access in place");

        // A2 — the MULTI-nav equality: the accumulated-MULTI chain admits against a scalar
        // sibling (legacy's cardinality-modifier default + MapperC join stay inside the
        // oracle-served literal line).
        REqualityExpr eqMulti = firstUnder(ws, "EqMultiNav10", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqMulti, ws),
                "a MULTI-nav equality operand admits since the #510 arm-A2");
        BinaryOp eqMultiIr = assertInstanceOf(BinaryOp.class, adapt(eqMulti, ws));
        FieldAccess multiNav = assertInstanceOf(FieldAccess.class, eqMultiIr.left(),
                "the MULTI nav lowers in place");
        assertEquals(ExpressionCardinality.MULTI, multiNav.cardinality(),
                "the admitted operand is the accumulated-MULTI chain");

        // A5 — the arithmetic nav operand: the chain admits against a numeric literal
        // (legacy's witness/join/coercion stay inside the oracle-served literal line).
        RArithmeticExpr arith = firstUnder(ws, "ArithNav10", RArithmeticExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(arith, ws),
                "an arithmetic nav operand admits since the #510 arm-A5");
        BinaryOp arithIr = assertInstanceOf(BinaryOp.class, adapt(arith, ws));
        assertInstanceOf(FieldAccess.class, arithIr.left(),
                "the nav operand lowers in place");

        // A3 — the logical conditional operand: the lowered conditional admits (legacy
        // coerces it ComparisonResult.ofNullSafe inside its own composition).
        RLogicalExpr logical = firstUnder(ws, "LogCond10", RLogicalExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(logical, ws),
                "a logical conditional operand admits since the #510 arm-A3");
        BinaryOp logicalIr = assertInstanceOf(BinaryOp.class, adapt(logical, ws));
        assertInstanceOf(IRConditional.class, logicalIr.left(),
                "the conditional operand lowers in place");
    }

    @Test
    void argNavOracleLeafRootsAdmitAtHopOne() {
        // #510 arm-D1 (argNav.root:IRSymbolNav.hop1 58 · argNav.root:IRMetaAccess.hop1
        // [.multi] 52+14 sole at the #509 SOT): the one-hop navs over the two standing
        // ORACLE-LEAF root kinds ADMIT at the argNavFacet classifier (the #505 alias-root
        // precedent one kind further — an apply carrying such an arg always renders WHOLE
        // through the callArgs oracle serve, the kind walk routing). The pin follows the
        // #491 belt-pin idiom: hand-built lowered triples call the package-private
        // classifier directly, and the DISCRIMINATING CONTRAST proves the admission wiring
        // — every depth falls THROUGH the root gate to the deeper #489 type-agreement gate
        // (the hand-built param resolves MISSING, so the face reads typeGap.<root>.<depth>
        // — the root ADMITTED). The hop2 leg RETENSED at #522 (the lock-lineage law): the
        // original pin held the #510 census-narrow root-decline spelling
        // (root:IRSymbolNav.hop2); the #522 sweep widened the standing roots to EVERY
        // depth (the live deeper faces at the #521 SOT + the teach-sensitive-depth law),
        // so hop2 now proves the SAME fall-through as hop1 — the depth boundary pin moved
        // to the UNWIDENED neighbors (root:IROutputRef.hop2, the #522 sweep witness). The
        // full null-face admission is the corpus probe's own witness (the movement + the
        // 55/55 identical= ring).
        RModel model = AstBuilder.buildFromString("""
                namespace "test.step510d1"

                type Marker10:
                    flag boolean (1..1)
                """, "step510-argroots.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RAttribute param = scalarParam("q");
        RExpression rawArg = intLiteral(1);
        RMetaAnnotatedType clean = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRSymbolNav symHead = new IRSymbolNav("rule", NodeId.ROOT, clean,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRMetaAccess metaHead = new IRMetaAccess(
                new IRVariable("leg", IRVariable.VariableKind.PARAM, NodeId.ROOT, clean,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE),
                "code", java.util.List.of("scheme"), NodeId.ROOT, clean,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        java.util.function.BiFunction<IRExpr, ExpressionCardinality, FieldAccess> hopOver =
                (root, card) -> new FieldAccess(root, "rate", NodeId.ROOT, clean, card,
                        ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        assertNull(
                ExpressionToIRAdapter.argNavFacet(
                        hopOver.apply(symHead, ExpressionCardinality.SINGLE), rawArg, param, ws),
                "RETENSED #527: a hop1 symbol-nav root ADMITS pair-blind — the oracle-leaf"
                        + " root screen skips the agreement gate entirely (the containing claim"
                        + " serves whole-legacy, any coercion legacy's own inside the serve;"
                        + " pre-#527 this exact shape held the typeGap.root:IRSymbolNav.hop1"
                        + ".g:number_null honest-residue face)");
        assertNull(
                ExpressionToIRAdapter.argNavFacet(
                        hopOver.apply(metaHead, ExpressionCardinality.MULTI), rawArg, param, ws),
                "RETENSED #527: a hop1 meta-access root ADMITS pair-blind the same way"
                        + " (pre-#527: typeGap.root:IRMetaAccess.hop1.multi.g:number_null)");
        assertNull(
                ExpressionToIRAdapter.argNavFacet(
                        hopOver.apply(hopOver.apply(symHead, ExpressionCardinality.SINGLE),
                                ExpressionCardinality.SINGLE), rawArg, param, ws),
                "RETENSED #527: the hop2 chain admits pair-blind too — the oracle-leaf skip is"
                        + " depth-blind exactly like the serve it routes to (pre-#522 the"
                        + " census-narrow root decline, pre-#527 the typeGap fall-through)");
    }

    @Test
    void minMaxNamedAndSwitchBindersJoinTheItemArms() {
        // #510 arm-D2 (the binder cluster — attrOutsideFunction.noFilterExtractBinder 67 +
        // attributeChain.noFilterExtractBinder 57 sole at the #509 SOT; the walkBindGate
        // census's three classes): (D2c) an implicit MIN/MAX key binder joins the shared
        // binder walk (the key lambda ranges the argument's elements — the Cat-8 law
        // verbatim); (D2b) a SINGLE-NAMED extract binder joins (the named param IS the item
        // by legacy's scope semantics); (D2a) outside every binder, a TYPE-guard switch
        // case narrows the subject (legacy's #221 machinery — caseNarrowedElementType, the
        // linker-resolved-guard mirror). All three classes fall OUTSIDE the #479 retype
        // slice by design, so the equivalents mint the IRSynItemNav ORACLE LEAF (never the
        // native hop compose) and every claim renders whole-legacy BY IDENTITY.
        String source = """
                namespace "test.step510d2"

                type Trig10:
                    level number (1..1)

                type Leg10:
                    rate number (1..1)
                    trig Trig10 (1..1)

                type Swap10:
                    premium number (1..1)
                    legs Leg10 (0..*)

                type Fra10:
                    fee number (1..1)

                choice Prod10:
                    Swap10
                    Fra10

                func MinBare10:
                    inputs:
                        legs Leg10 (0..*)
                    output:
                        out Leg10 (0..1)
                    set out:
                        legs min [ rate ]

                func MinChain10:
                    inputs:
                        legs Leg10 (0..*)
                    output:
                        out Leg10 (0..1)
                    set out:
                        legs min [ trig -> level ]

                func NamedExtract10:
                    inputs:
                        legs Leg10 (0..*)
                    output:
                        out number (0..*)
                    set out:
                        legs then extract L [ rate ]

                func SwitchBare10:
                    inputs:
                        p Prod10 (1..1)
                    output:
                        out number (0..1)
                    set out:
                        p switch
                            Swap10 then premium,
                            default empty

                func SwitchChain10:
                    inputs:
                        p Prod10 (1..1)
                    output:
                        out number (0..*)
                    set out:
                        p switch
                            Swap10 then legs -> rate,
                            default empty
                """;
        RModel model = AstBuilder.buildFromString(source, "step510-binders.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // D2c — the implicit MIN key binder: the bare attr claims off the argument's
        // element and mints the oracle leaf.
        RSymbolReference minBare = bareRefUnder(ws, "MinBare10", "rate");
        IRSynItemNav minMint = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(minBare, ws).orElseThrow(() -> new AssertionError(
                        "a min-key bare attr must claim since the #510 arm-D2c")),
                "the mint is the IRSynItemNav oracle leaf (outside the #479 retype slice)");
        assertEquals("rate", minMint.featureName());

        // D2c at the CHAIN seat: the disguised `trig -> level` claims the same way — the
        // IRSynItemNav-based spine (the #505 accepted form).
        REnumValueRef minChain = chainUnder(ws, "MinChain10", "trig", "level");
        FieldAccess minSpine = assertInstanceOf(FieldAccess.class,
                adapter.adapt(minChain, ws).orElseThrow(() -> new AssertionError(
                        "a min-key disguised chain must claim since the #510 arm-D2c")),
                "the chain lowers to the IRSynItemNav-based spine");
        assertInstanceOf(IRSynItemNav.class, minSpine.receiver(),
                "the spine bottoms at the oracle leaf");

        // D2b — the SINGLE-NAMED extract binder: the bare attr claims off the source's
        // element (the named param IS the item; the render stays legacy's scope-live
        // machinery inside the oracle serve).
        RSymbolReference namedBare = bareRefUnder(ws, "NamedExtract10", "rate");
        IRSynItemNav namedMint = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(namedBare, ws).orElseThrow(() -> new AssertionError(
                        "a named-extract bare attr must claim since the #510 arm-D2b")),
                "the mint is the IRSynItemNav oracle leaf (the retype slice keeps its"
                        + " named-binder decline)");
        assertEquals("rate", namedMint.featureName());

        // D2a — the TYPE-guard switch case: the bare attr resolves against the linker's
        // narrowed guard type and claims; the default branch stays outside the slice.
        RSymbolReference switchBare = bareRefUnder(ws, "SwitchBare10", "premium");
        IRSynItemNav switchMint = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(switchBare, ws).orElseThrow(() -> new AssertionError(
                        "a switch-case bare attr must claim since the #510 arm-D2a")),
                "the mint is the IRSynItemNav oracle leaf (the case-narrowed class)");
        assertEquals("premium", switchMint.featureName());

        // D2a at the CHAIN seat: the disguised `legs -> rate` claims off the narrowed type
        // (head by name on the case type, leaf on the head's declared type — legacy
        // caseNarrowedDisguisedLeaf's own ladder).
        REnumValueRef switchChain = chainUnder(ws, "SwitchChain10", "legs", "rate");
        FieldAccess switchSpine = assertInstanceOf(FieldAccess.class,
                adapter.adapt(switchChain, ws).orElseThrow(() -> new AssertionError(
                        "a switch-case disguised chain must claim since the #510 arm-D2a")),
                "the chain lowers to the IRSynItemNav-based spine off the narrowed type");
        assertInstanceOf(IRSynItemNav.class, switchSpine.receiver(),
                "the spine bottoms at the oracle leaf");
    }

    @Test
    void noAdaptArmSweepFamiliesLowerToTheFourKinds() {
        // #513 (the noAdaptArm sweep — witness 1): the seven un-armed families join the
        // dispatch through FOUR shallow kind mints (the #500 IRPipe family-arm pattern —
        // shape-gated unconditional claims, facts only, no IR children; the render is the
        // compiler's per-family oracle serve, byte-identical BY IDENTITY). The negative
        // witness is the flip-removed token: pre-#513 every one of these nodes declined
        // with `noAdaptArm` (the sweep faces Σ 99 sole at the #512 SOT); post-arm the
        // declineReason mirror reads EMPTY (adapt claims), and the still-unarmed `join`
        // family carries the token in the recut #476 fixture lock above.
        String source = """
                namespace "test.step513"

                type Leg13:
                    rate number (1..1)

                enum Colour13:
                    RED
                    BLUE

                func SortW:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (0..*)
                    set out:
                        xs sort

                func MaxW:
                    inputs:
                        legs Leg13 (0..*)
                    output:
                        out Leg13 (0..1)
                    set out:
                        legs max [ rate ]

                func MinW:
                    inputs:
                        legs Leg13 (0..*)
                    output:
                        out Leg13 (0..1)
                    set out:
                        legs min [ rate ]

                func ContainsW:
                    inputs:
                        xs string (0..*)
                        ys string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        xs contains ys

                func DisjointW:
                    inputs:
                        xs string (0..*)
                        ys string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        xs disjoint ys

                func DefaultW:
                    inputs:
                        a string (0..1)
                        b string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        a default b

                func SwitchW:
                    inputs:
                        c Colour13 (1..1)
                    output:
                        out int (1..1)
                    set out:
                        c switch
                            RED then 1,
                            default 2
                """;
        RModel model = AstBuilder.buildFromString(source, "step513-sweep.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        IRCollectOp sortMint = assertInstanceOf(IRCollectOp.class,
                adapter.adapt(firstUnder(ws, "SortW", RSortExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a sort must lower since the #513 sweep")),
                "the sort family lowers to the collect kind");
        assertEquals(IRCollectOp.Op.SORT, sortMint.op());
        assertFalse(sortMint.hasBody(), "a bodyless sort carries the hasBody=false fact");

        IRCollectOp maxMint = assertInstanceOf(IRCollectOp.class,
                adapter.adapt(firstUnder(ws, "MaxW", RMaxExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a keyed max must lower since the #513 sweep")),
                "the max family lowers to the collect kind");
        assertEquals(IRCollectOp.Op.MAX, maxMint.op());
        assertTrue(maxMint.hasBody(), "the comparison-key body rides as the neutral fact");

        IRCollectOp minMint = assertInstanceOf(IRCollectOp.class,
                adapter.adapt(firstUnder(ws, "MinW", RMinExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a keyed min must lower since the #513 sweep")),
                "the min family lowers to the collect kind");
        assertEquals(IRCollectOp.Op.MIN, minMint.op());

        IRMembershipOp containsMint = assertInstanceOf(IRMembershipOp.class,
                adapter.adapt(firstUnder(ws, "ContainsW", RContainsExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a contains must lower since the #513 sweep")),
                "the contains family lowers to the membership kind");
        assertEquals(IRMembershipOp.Op.CONTAINS, containsMint.op());

        IRMembershipOp disjointMint = assertInstanceOf(IRMembershipOp.class,
                adapter.adapt(firstUnder(ws, "DisjointW", RDisjointExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a disjoint must lower since the #513 sweep")),
                "the disjoint family lowers to the membership kind");
        assertEquals(IRMembershipOp.Op.DISJOINT, disjointMint.op());

        assertInstanceOf(IRDefaultOp.class,
                adapter.adapt(firstUnder(ws, "DefaultW", RDefaultExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a default must lower since the #513 sweep")),
                "the default family lowers to its kind");

        IRSwitchOp switchMint = assertInstanceOf(IRSwitchOp.class,
                adapter.adapt(firstUnder(ws, "SwitchW", RSwitchExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a switch must lower since the #513 sweep")),
                "the switch family lowers to its kind");
        assertEquals(2, switchMint.caseCount(), "both cases counted");
        assertTrue(switchMint.hasDefault(), "the default case rides as the neutral fact");

        // The mirror lock: a CLAIMING family reads NO decline reason (the #476 equivalence
        // law — reason present ⟺ adapt empty; the noAdaptArm token is REMOVED for all
        // seven families, the flip's own negative witness).
        for (String fn : List.of("SortW", "MaxW", "MinW", "ContainsW", "DisjointW",
                "DefaultW", "SwitchW")) {
            RFunction f = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                    .filter(x -> fn.equals(x.name())).findFirst().orElseThrow();
            RExpression node = AstWalker.findAll(f, RExpression.class).stream()
                    .filter(e -> e instanceof RSortExpr || e instanceof RMaxExpr
                            || e instanceof RMinExpr || e instanceof RContainsExpr
                            || e instanceof RDisjointExpr || e instanceof RDefaultExpr
                            || e instanceof RSwitchExpr)
                    .findFirst().orElseThrow();
            assertEquals(Optional.empty(), adapter.declineReason(node, ws),
                    fn + ": a claiming sweep family carries no decline face");
        }
    }

    @Test
    void untargetedCloseFamiliesLowerToTheTwoKinds() {
        // #515 (the untargeted close — the mint witness): the last two big no-IR-attempt
        // families join the dispatch through TWO shallow kind mints (the #513
        // noAdaptArmSweep pattern — UNCONDITIONAL claims per the without-left law, facts
        // only, no IR children; the render is the compiler's withMetaOp/joinOp oracle
        // serve, byte-identical BY IDENTITY). The negative witness is the flip-removed
        // token: pre-#515 every one of these nodes declined with `noAdaptArm` (RWithMetaExpr
        // 92 + RJoinExpr 2 untargeted roots at the #514 SOT); post-arm the declineReason
        // mirror reads EMPTY, and the still-unarmed `reduce` carries the token here (with
        // cardinality-check in the recut #476 fixture lock — the last two noAdaptArm
        // families, the token-uniqueness law).
        String source = """
                namespace "test.step515"

                func JoinW:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (1..1)
                    set out:
                        xs join ", "

                func JoinBare:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (1..1)
                    set out:
                        (xs join)

                func WithMetaW:
                    inputs:
                        val string (1..1)
                        src string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        val with-meta {
                            scheme: src
                        }

                func WithMetaBare:
                    inputs:
                        val string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        (val with-meta)

                func JoinPiped:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (1..1)
                    set out:
                        xs then join ", "

                func WithMetaPiped:
                    inputs:
                        xs string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        xs then with-meta {
                            scheme: xs
                        }

                func ReduceContrast:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (1..1)
                    set out:
                        xs reduce acc, v [ acc + v ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step515-untargeted-close.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        IRJoinOp joinMint = assertInstanceOf(IRJoinOp.class,
                adapter.adapt(firstUnder(ws, "JoinW", RJoinExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a separator-carrying join must lower since the #515 close")),
                "the join family lowers to its kind");
        assertTrue(joinMint.hasSeparator(),
                "the separator's presence rides as the neutral fact (the separator lives in"
                        + " separator(), never rawRight() — the Copilot #513 R1 shape note)");

        IRJoinOp bareJoinMint = assertInstanceOf(IRJoinOp.class,
                adapter.adapt(firstUnder(ws, "JoinBare", RJoinExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a separator-less join must lower — the separator is optional"
                                        + " by grammar")),
                "the bare join lowers too");
        assertTrue(bareJoinMint.hasSeparator(),
                "a workspace-built bare join reads hasSeparator=true — the engine's own"
                        + " DefaultJoinSeparatorRule derived-state pass materializes the"
                        + " empty-string separator (upstream's default-separator semantics),"
                        + " so the false reading is reachable only on un-derived hand-built"
                        + " nodes; the fact stays the node's own state at adapt time");

        IRWithMetaOp withMetaMint = assertInstanceOf(IRWithMetaOp.class,
                adapter.adapt(firstUnder(ws, "WithMetaW", RWithMetaExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a with-meta must lower since the #515 close")),
                "the with-meta family lowers to its kind");
        assertEquals(1, withMetaMint.entryCount(),
                "the entry count rides as the neutral fact (the scheme entry)");

        // The Copilot #515 R1 catch: the UNCONDITIONAL-arm edge shapes get their own
        // regression locks (a future refactor reintroducing null-sensitive logic or
        // entry assumptions must trip a unit test, not a corpus probe). The entry-less
        // brace-less form (count 0), the piped WITHOUT-LEFT join (the grammar's
        // JoinWithoutLeftExpr — the raw left stays NULL by parse: join is not among the
        // ten elided-operand synthesis slots, the #513 without-left law) and the piped
        // without-left with-meta (null argument + a live entry) all CLAIM.
        IRWithMetaOp bareMetaMint = assertInstanceOf(IRWithMetaOp.class,
                adapter.adapt(firstUnder(ws, "WithMetaBare", RWithMetaExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "an entry-less with-meta must lower — the braces are"
                                        + " optional by grammar")),
                "the entry-less form lowers too");
        assertEquals(0, bareMetaMint.entryCount(), "no entries — the fact reads zero");

        RJoinExpr pipedJoin = firstUnder(ws, "JoinPiped", RJoinExpr.class);
        assertNull(pipedJoin.rawLeft(),
                "the piped join keeps a NULL left by parse — the without-left premise");
        IRJoinOp pipedJoinMint = assertInstanceOf(IRJoinOp.class,
                adapter.adapt(pipedJoin, ws).orElseThrow(() -> new AssertionError(
                        "a without-left join must lower — the arm is unconditional")),
                "the without-left join lowers too");
        assertTrue(pipedJoinMint.hasSeparator(), "the explicit separator rides");

        RWithMetaExpr pipedMeta = firstUnder(ws, "WithMetaPiped", RWithMetaExpr.class);
        assertNull(pipedMeta.argument(),
                "the piped with-meta keeps a NULL argument by parse — the without-left"
                        + " premise");
        IRWithMetaOp pipedMetaMint = assertInstanceOf(IRWithMetaOp.class,
                adapter.adapt(pipedMeta, ws).orElseThrow(() -> new AssertionError(
                        "a without-left with-meta must lower — the arm is unconditional")),
                "the without-left with-meta lowers too");
        assertEquals(1, pipedMetaMint.entryCount(), "the scheme entry rides");

        // The mirror lock + the token-uniqueness contrast: the two armed families read NO
        // decline reason; the parsed reduce KEEPS the noAdaptArm token (a still-unarmed
        // family — the negative witness stays a token the flip removes only where it arms).
        for (String fn : List.of("JoinW", "JoinBare", "WithMetaW", "WithMetaBare",
                "JoinPiped", "WithMetaPiped")) {
            RFunction f = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                    .filter(x -> fn.equals(x.name())).findFirst().orElseThrow();
            RExpression node = AstWalker.findAll(f, RExpression.class).stream()
                    .filter(e -> e instanceof RJoinExpr || e instanceof RWithMetaExpr)
                    .findFirst().orElseThrow();
            assertEquals(Optional.empty(), adapter.declineReason(node, ws),
                    fn + ": a claiming untargeted-close family carries no decline face");
        }
        assertEquals(Optional.of("noAdaptArm"),
                adapter.declineReason(firstUnder(ws, "ReduceContrast", RReduceExpr.class), ws),
                "the still-unarmed reduce keeps the noAdaptArm token — the contrast lock");
    }

    @Test
    void sweepKindsJoinTheOperandArgAndLogicalGates() {
        // #513 (the noAdaptArm sweep — witness 2): the four minted kinds join the
        // equality/comparison operand sets (+ the numeric-literal licensing sets), the
        // call-arg gate and (membership) the coerced-boolean logical family — the sweep's
        // own exposed frontier taken up front (the pre-arm faces hid every consumer seat
        // behind the minimal-blocker attribution: the child family carried the face). All
        // four are oracle leaves with NO emitter arm, so every containing root renders
        // WHOLE through its oracle serve (the routing law — byte-identical BY IDENTITY).
        String source = """
                namespace "test.step513b"

                func Echo13:
                    inputs:
                        vals string (0..*)
                    output:
                        out string (0..*)
                    set out:
                        vals

                func EqW:
                    inputs:
                        xs string (0..*)
                        ys string (0..*)
                        flag boolean (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        (xs contains ys) = flag

                func CmpW:
                    inputs:
                        nums number (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        (nums max) > 0

                func ArgW:
                    inputs:
                        xs string (0..*)
                    output:
                        out string (0..*)
                    set out:
                        Echo13(xs sort)

                func LogW:
                    inputs:
                        xs string (0..*)
                        ys string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        (xs contains ys) and (ys exists)
                """;
        RModel model = AstBuilder.buildFromString(source, "step513-gates.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        BinaryOp eqMint = assertInstanceOf(BinaryOp.class,
                adapter.adapt(firstUnder(ws, "EqW", REqualityExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "an equality over a membership operand must lower")),
                "the equality gate admits the membership kind");
        assertInstanceOf(IRMembershipOp.class, eqMint.left(),
                "the membership operand is carried — the oracle-leaf routing law");

        BinaryOp cmpMint = assertInstanceOf(BinaryOp.class,
                adapter.adapt(firstUnder(ws, "CmpW", RComparisonExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a comparison over a collect operand + literal must lower")),
                "the comparison gate admits the collect kind AND licenses the literal"
                        + " against it (the #507/#511 paired-licensing precedent)");
        assertInstanceOf(IRCollectOp.class, cmpMint.left(), "the collect operand");

        IRApply argMint = assertInstanceOf(IRApply.class,
                adapter.adapt(AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                        .stream()
                        .filter(r -> "Echo13".equals(r.name()) && !r.args().isEmpty())
                        .findFirst().orElseThrow(), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a call with a collect argument must lower")),
                "the call-arg gate admits the collect kind");
        assertInstanceOf(IRCollectOp.class, argMint.args().get(0), "the collect argument");

        BinaryOp logMint = assertInstanceOf(BinaryOp.class,
                adapter.adapt(firstUnder(ws, "LogW", RLogicalExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a logical over a membership operand must lower")),
                "the logical gate's coerced family admits the membership kind");
        assertInstanceOf(IRMembershipOp.class, logMint.left(),
                "the membership operand under the logical");
    }

    @Test
    void aliasLiteralLicensingAndDefaultLogicalOperandsJoinAtProbe2() {
        // #513 probe2/probe3 (the operand-face teaches): (1) the ALIAS sibling joins the
        // comparison numeric-literal licensing set — the #513 facet read the standing
        // operandNumericLiteral face 100% sib:alias (15 + 17 sole), the #510 arm-A4c alias
        // OPERAND admission's missing licensing pair (the #507/#511 paired-licensing law);
        // (2) the DEFAULT fallback joins the coerced-boolean logical family — probe1's own
        // exposed frontier (operand:IRDefaultOp 6 logical sole, the boolean
        // `(a default b) and …` class). Both routes stay whole-legacy: the alias operand
        // triggers the standing comparison shape leg, the default is an oracle leaf.
        String source = """
                namespace "test.step513c"

                func AliasCmpW:
                    inputs:
                        a number (1..1)
                    output:
                        out boolean (1..1)
                    alias doubled: a
                    set out:
                        doubled > 0

                func DefLogW:
                    inputs:
                        maybe boolean (0..1)
                        must boolean (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        (maybe default False) and (must exists)
                """;
        RModel model = AstBuilder.buildFromString(source, "step513-probe2.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        BinaryOp aliasCmp = assertInstanceOf(BinaryOp.class,
                adapter.adapt(firstUnder(ws, "AliasCmpW", RComparisonExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "an alias-vs-literal comparison must lower since probe2")),
                "the literal licenses against the alias sibling");
        assertTrue(aliasCmp.left() instanceof IRReference ref
                        && ref.referenceKind() == IRReference.ReferenceKind.ALIAS,
                "the alias operand — the #510 admission this licensing pairs with");

        BinaryOp defLog = assertInstanceOf(BinaryOp.class,
                adapter.adapt(firstUnder(ws, "DefLogW", RLogicalExpr.class), ws)
                        .orElseThrow(() -> new AssertionError(
                                "a logical over a boolean default must lower since probe2")),
                "the coerced family admits the default fallback");
        assertInstanceOf(IRDefaultOp.class, defLog.left(),
                "the default operand under the logical");
    }

    @Test
    void listOpAndBinaryOperandsJoinTheBinarySeats() {
        // #511 (the listOp/BinaryOp cluster — the operand seats): the equality gate admits a
        // lowered nested BinaryOp (operand:BinaryOp, 58 equality sole at the #510 SOT) and a
        // non-COUNT list op (operand:IRListOp, 34 equality sole), the numeric-literal sibling
        // licensing widening with both; the existence gate admits a list op kind-wide (18
        // sole); the logical gate admits a list op into the coerced family (6 sole); the
        // comparison gate admits a non-COUNT list op + licensing (2 sole). Every admitted
        // root renders WHOLE through the oracle-root serves (the #511 shape legs in
        // containsOracleLeaf — byte-identical BY IDENTITY; COUNT keeps the standing NATIVE
        // resultCount compose via the twin carve-outs), so the adapter-side witnesses assert
        // the CLAIMS and the operand kinds in place. The discriminating contrast: an
        // UN-lowerable list op (`sum` has no adapt arm) still declines the pair — the
        // admissions ride the standing all-or-nothing lowerability gates.
        String source = """
                namespace "test.step511a"

                type Box11:
                    nums number (0..*)
                    tags string (0..*)
                    flags boolean (0..*)
                    amount number (1..1)

                func EqCompound11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> amount + b -> amount = b -> amount

                func EqCompoundLit11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> amount + b -> amount = 0

                func EqDistinct11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> tags distinct = b -> tags

                func ExistsDistinct11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> tags distinct exists

                func LogCollapse11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> flags only-element and b -> amount = b -> amount

                func CmpCollapse11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> nums only-element > 1

                func EqSum11:
                    inputs:
                        b Box11 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        b -> nums sum = b -> amount
                """;
        RModel model = AstBuilder.buildFromString(source, "step511-operands.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // The equality BinaryOp operand: `a + b = c` — the compound-vs-nav pair claims.
        REqualityExpr eqCompound = firstUnder(ws, "EqCompound11", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqCompound, ws),
                "an equality nested-BinaryOp operand admits since #511");
        BinaryOp eqCompoundIr = assertInstanceOf(BinaryOp.class, adapt(eqCompound, ws));
        assertInstanceOf(BinaryOp.class, eqCompoundIr.left(),
                "the compound operand lowers in place");

        // The widened licensing: `a + b = 0` — the literal admits off its BinaryOp sibling.
        REqualityExpr eqLit = firstUnder(ws, "EqCompoundLit11", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqLit, ws),
                "a numeric literal licenses off a BinaryOp sibling since #511");
        assertInstanceOf(BinaryOp.class, adapt(eqLit, ws));

        // The equality non-COUNT list-op operand: `xs distinct = xs` (the MULTI-nav sibling
        // rides the #510 arm-A2 admission).
        REqualityExpr eqDistinct = firstUnder(ws, "EqDistinct11", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqDistinct, ws),
                "an equality non-COUNT list-op operand admits since #511");
        BinaryOp eqDistinctIr = assertInstanceOf(BinaryOp.class, adapt(eqDistinct, ws));
        IRListOp distinctOperand = assertInstanceOf(IRListOp.class, eqDistinctIr.left(),
                "the list op lowers in place");
        assertEquals(IRListOp.Kind.DISTINCT, distinctOperand.op(),
                "the admitted operand is the non-COUNT kind");

        // The existence list-op operand: `xs distinct exists`.
        RExistenceExpr existsDistinct = firstUnder(ws, "ExistsDistinct11", RExistenceExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(existsDistinct, ws),
                "an existence list-op operand admits since #511 (kind-wide)");
        Existence existsIr = assertInstanceOf(Existence.class, adapt(existsDistinct, ws));
        assertInstanceOf(IRListOp.class, existsIr.arg(),
                "the list op lowers in place");

        // The logical list-op operand: `flags only-element and <equality>` — the collapse
        // joins the ComparisonResult.ofNullSafe coerced family.
        RLogicalExpr logCollapse = firstUnder(ws, "LogCollapse11", RLogicalExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(logCollapse, ws),
                "a logical list-op operand admits since #511");
        BinaryOp logIr = assertInstanceOf(BinaryOp.class, adapt(logCollapse, ws));
        assertInstanceOf(IRListOp.class, logIr.left(),
                "the coerced operand lowers in place");

        // The comparison non-COUNT list-op operand + licensing: `nums only-element > 1`.
        RComparisonExpr cmpCollapse = firstUnder(ws, "CmpCollapse11", RComparisonExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(cmpCollapse, ws),
                "a comparison non-COUNT list-op operand admits + licenses its literal sibling"
                        + " since #511");
        BinaryOp cmpIr = assertInstanceOf(BinaryOp.class, adapt(cmpCollapse, ws));
        assertInstanceOf(IRListOp.class, cmpIr.left(),
                "the collapse operand lowers in place");

        // The former discriminating contrast RETENSED at #519 (the lock-lineage law): `nums
        // sum = <nav>` — sum joined the taught set, so the operand lowers and the pair rides
        // the STANDING #511 non-COUNT equality admission (isNonCountListOp — SUM is non-COUNT
        // by construction) straight into the oracle exemption: the twin-exact #511 equality
        // list-op leg PLUS the #519 SUM-scoped kind leg both route the root whole-legacy, so
        // no native equality compose ever sees a sum operand.
        REqualityExpr eqSum = firstUnder(ws, "EqSum11", REqualityExpr.class);
        assertEquals(Optional.empty(), adapter.declineReason(eqSum, ws),
                "a sum equality operand admits since #519 (the #511 non-COUNT leg carries it)");
        BinaryOp eqSumIr = assertInstanceOf(BinaryOp.class, adapt(eqSum, ws));
        IRListOp sumOperand = assertInstanceOf(IRListOp.class, eqSumIr.left(),
                "the sum lowers in place as the operand");
        assertEquals(IRListOp.Kind.SUM, sumOperand.op(), "the #519-taught op");
    }

    @Test
    void binaryArgsAndListOpRootedNavArgsJoinTheCallArgGates() {
        // #511 (the listOp/BinaryOp cluster — the callArgs seats): the call-arg gate admits a
        // lowered BinaryOp argument (arg:BinaryOp, 47 sole at the #510 SOT — `F(a + b)`), and
        // the argNavFacet root gate admits a list-op chain root at EVERY depth
        // (argNav.root:IRListOp — hop1 31 · hop2 38 · deep 13 · hop1.multi 2 sole; unlike the
        // #510 hop1-only symbol/meta roots, all four spellings are live). The renders are the
        // callArgs oracle serve BY IDENTITY (the compiler's apply-arg BinaryOp leg + the
        // arg-root walk + the nav-over-list-op receiver leg). The deeper #489 gates stay
        // live: the parsed contrast pins typeGap.root:IRListOp.hop1 on a param-type mismatch,
        // and the hand-built hop2 triple falls through the root gate to the SAME typeGap
        // spelling (the root ADMITTED at depth; since the #522 every-depth sweep the
        // standing symbol/meta roots fall through identically — the depth boundary pin
        // lives on the UNWIDENED hop1-only neighbors, root:IROutputRef.hop2).
        String source = """
                namespace "test.step511b"

                type Sub11:
                    code string (1..1)

                type Bag11:
                    subs Sub11 (0..*)
                    amount number (1..1)

                func TakeString11:
                    inputs:
                        s string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        s

                func TakeNumber11:
                    inputs:
                        n number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        n

                func CallCompound11:
                    inputs:
                        b Bag11 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        TakeNumber11(b -> amount + b -> amount)

                func CallNavOverCollapse11:
                    inputs:
                        b Bag11 (1..1)
                    output:
                        out string (1..1)
                    set out:
                        TakeString11(b -> subs only-element -> code)

                func CallNavTypeGap11:
                    inputs:
                        b Bag11 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        TakeNumber11(b -> subs only-element -> code)
                """;
        RModel model = AstBuilder.buildFromString(source, "step511-callargs.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // arg:BinaryOp — the compound argument claims; the lowered BinaryOp IS the faithful
        // arg (the callArgs oracle serve renders legacy's own operand + unwrap line).
        RSymbolReference callCompound = callUnder(ws, "CallCompound11", "TakeNumber11");
        assertEquals(Optional.empty(), adapter.declineReason(callCompound, ws),
                "a BinaryOp call argument admits since #511");
        IRApply compoundIr = assertInstanceOf(IRApply.class, adapt(callCompound, ws));
        assertInstanceOf(BinaryOp.class, compoundIr.args().get(0),
                "the argument is the lowered compound — load-bearing");

        // argNav.root:IRListOp — the list-op-rooted nav argument claims (type-agreeing):
        // the #511 receiver admission lowers the nav, the root gate admits the chain.
        RSymbolReference callNav = callUnder(ws, "CallNavOverCollapse11", "TakeString11");
        assertEquals(Optional.empty(), adapter.declineReason(callNav, ws),
                "a list-op-rooted nav argument admits since #511");
        IRApply navIr = assertInstanceOf(IRApply.class, adapt(callNav, ws));
        FieldAccess navArg = assertInstanceOf(FieldAccess.class, navIr.args().get(0),
                "the argument is the lowered chain");
        assertInstanceOf(IRListOp.class, navArg.receiver(),
                "the chain roots at the lowered list op — load-bearing");

        // RETENSED #527: the SAME chain into a number param now ADMITS pair-blind — the
        // list-op root is an ORACLE LEAF, so the oracle-leaf root screen skips the
        // type-agreement gate entirely (the containing claim serves whole-legacy; any
        // coercion is legacy's own inside the serve). Pre-#527 this exact call held the
        // argNav.typeGap.root:IRListOp.hop1.g:string_number designed-residue face; the
        // agreement gate's live boundary moved to the NATIVE roots (the param-rooted
        // g:int_number pin in the #514 lock below).
        RSymbolReference callGap = callUnder(ws, "CallNavTypeGap11", "TakeNumber11");
        assertEquals(Optional.empty(), adapter.declineReason(callGap, ws),
                "the oracle-leaf root screen admits the pair-gapped chain — the serve route"
                        + " carries any legacy coercion BY IDENTITY");

        // The DEPTH-WIDE admission (the hand-built #510-pin idiom): a hop2 chain over a
        // list-op root falls THROUGH the root gate to the deeper type-agreement gate (the
        // hand-built param resolves MISSING → typeGap.root:IRListOp.hop2). Since the #522
        // every-depth sweep the standing symbol/meta roots fall through identically — the
        // cross-kind depth contrast lives on the UNWIDENED hop1-only neighbors
        // (root:IROutputRef.hop2, the #522 sweep witness).
        RAttribute param = scalarParam("q");
        RExpression rawArg = intLiteral(1);
        RMetaAnnotatedType clean = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRListOp listRoot = new IRListOp(IRListOp.Kind.DISTINCT,
                new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT, clean,
                        ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE),
                NodeId.ROOT, clean, ExpressionCardinality.MULTI, Optionality.PRESENT,
                SourceRange.NONE);
        FieldAccess hop1 = new FieldAccess(listRoot, "rate", NodeId.ROOT, clean,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        FieldAccess hop2 = new FieldAccess(hop1, "value", NodeId.ROOT, clean,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(hop2, rawArg, param, ws),
                "RETENSED #527: the hop2 list-op root admits PAIR-BLIND (the oracle-leaf root"
                        + " screen — pre-#527 the shape fell through to the agreement gate and"
                        + " held typeGap.root:IRListOp.hop2.g:number_null); every depth is the"
                        + " same oracle-served line, so the #511 depth-wide admission and the"
                        + " #527 pair-blind skip compose");
    }

    /** The unique args-present call of the named callee under the named function. */
    private RSymbolReference callUnder(RWorkspace ws, String funcName, String calleeName) {
        RFunction fn = ws.files().stream()
                .flatMap(file -> AstWalker.findAll(file, RFunction.class).stream())
                .filter(f -> funcName.equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no function " + funcName));
        return AstWalker.findAll(fn, RSymbolReference.class).stream()
                .filter(r -> calleeName.equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no args-present call of " + calleeName + " under " + funcName));
    }

    @Test
    void recordReceiverAndQualifierItemNavsMintAtTheNonSymbolSeat() throws IOException {
        // #512 arms B1+B2 (RFeatureCall:featureUnresolved.nonSymbolReceiver — 79 sole at the
        // #511 SOT; the #509-banked nsrGate census decoded BOTH classes): (B1) a
        // record-feature read over a LOWERED COMPOUND receiver (recvLowers + rec:… + mRecHit
        // — the `… -> value -> date`-family chains) mints the receiver-carrying
        // IRRecordReceiverNav (the 31st kind; the #507/#509 childless twin keeps the
        // SYMBOL-head seats); (B2) a metadata-QUALIFIER read off a filter/extract-bound item
        // (mMiss + qMiss over `string (0..*) [metadata scheme]` sources) mints the shallow
        // IRQualifierItemNav (the 32nd kind) through the #512 DEEP PIPE WALK — the elided
        // then-chain resolves to the terminal feature carrying the qualifier. The
        // discriminating contrasts: a leaf off the record's feature list and a qualifier
        // read over a NON-meta source both keep the nonSymbolReceiver decline (removing
        // either arm turns its positive red; the contrasts pin the prove-or-decline gates).
        String source = """
                namespace "test.step512b"

                type Stamp512:
                    at zonedDateTime (1..1)

                type Wrap512:
                    stamp Stamp512 (1..1)

                type Holder512:
                    ids string (0..*)
                        [metadata scheme]
                    plains string (0..*)

                func TakeDate512:
                    inputs:
                        d date (1..1)
                    output:
                        o date (1..1)
                    set o:
                        d

                func CheckS512:
                    inputs:
                        s string (0..1)
                    output:
                        o boolean (1..1)
                    set o:
                        s exists

                func RecordCompound512:
                    inputs:
                        w Wrap512 (1..1)
                    output:
                        out date (0..1)
                    set out:
                        w -> stamp -> at -> date

                func RecordArg512:
                    inputs:
                        w Wrap512 (1..1)
                    output:
                        out date (0..1)
                    set out:
                        TakeDate512(w -> stamp -> at -> date)

                func RecordMiss512:
                    inputs:
                        w Wrap512 (1..1)
                    output:
                        out date (0..1)
                    set out:
                        w -> stamp -> at -> notAFeature512

                func QualFilter512:
                    inputs:
                        h Holder512 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        h -> ids
                            then filter CheckS512(item -> scheme)

                func QualDeep512:
                    inputs:
                        h Holder512 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        h -> ids
                            then filter item -> scheme = "A"
                            then first
                            then extract item -> scheme

                func QualMiss512:
                    inputs:
                        h Holder512 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        h -> plains
                            then extract item -> scheme
                """;
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        models.add(AstBuilder.buildFromString(source, "step512-b.rosetta"));
        RWorkspace ws = RWorkspace.build(models).workspace();
        var fixtureFile = ws.files().get(ws.files().size() - 1);

        // B1 — the compound-receiver record read mints the receiver-carrying kind.
        RFeatureCall recordNav = navUnder(fixtureFile, "RecordCompound512", "date");
        IRRecordReceiverNav minted = assertInstanceOf(IRRecordReceiverNav.class,
                adapter.adapt(recordNav, ws).orElseThrow(() -> new AssertionError(
                        "a compound-receiver record read must mint since the #512 arm-B1")),
                "the nonSymbolReceiver record class lowers to the 31st kind");
        assertEquals("date", minted.featureName(), "the record feature is the neutral fact");
        assertEquals("zonedDateTime", minted.recordTypeName(), "the record type's name");
        assertNotNull(minted.receiver(), "the receiver subtree is a real IR child");

        // B1 at the ARG seat — the call-arg gate admits the kind (the census's live
        // interior.RSymbolReference claim roots).
        RSymbolReference recordCall = callUnder(ws, "RecordArg512", "TakeDate512");
        assertEquals(Optional.empty(), adapter.declineReason(recordCall, ws),
                "a record-receiver-nav call argument admits since #512");
        IRApply recordCallIr = assertInstanceOf(IRApply.class, adapt(recordCall, ws));
        assertInstanceOf(IRRecordReceiverNav.class, recordCallIr.args().get(0),
                "the argument is the lowered record read — load-bearing");

        // The membership contrast: a leaf off the record's OWN feature list keeps the
        // decline (the #444 membership rule at the compound seat).
        RFeatureCall recordMiss = navUnder(fixtureFile, "RecordMiss512", "notAFeature512");
        assertEquals(Optional.of("featureUnresolved.nonSymbolReceiver"),
                adapter.declineReason(recordMiss, ws),
                "the member-miss residue keeps the honest face");

        // B2 — the qualifier read off the filter-bound item mints the shallow kind, and the
        // containing call lowers (the isSimpleCallArg admission).
        RFeatureCall qualNav = navUnder(fixtureFile, "QualFilter512", "scheme");
        IRQualifierItemNav qualMinted = assertInstanceOf(IRQualifierItemNav.class,
                adapter.adapt(qualNav, ws).orElseThrow(() -> new AssertionError(
                        "a qualifier read over a [metadata scheme] source must mint since"
                                + " the #512 arm-B2")),
                "the qMiss class lowers to the 32nd kind");
        assertEquals("scheme", qualMinted.qualifierName(), "the qualifier is the neutral fact");
        RSymbolReference qualCall = callUnder(ws, "QualFilter512", "CheckS512");
        assertEquals(Optional.empty(), adapter.declineReason(qualCall, ws),
                "a qualifier-item-nav call argument admits since #512");

        // B2 at DEPTH — the deep pipe walk crosses filter → first → extract to the same
        // terminal (the corpus's `then filter … then first then extract …` spelling), and
        // the equality operand admission composes the filter predicate whole.
        RFunction qualDeep = AstWalker.findAll(fixtureFile, RFunction.class).stream()
                .filter(f -> "QualDeep512".equals(f.name())).findFirst().orElseThrow();
        List<RFeatureCall> deepNavs = AstWalker.findAll(qualDeep, RFeatureCall.class).stream()
                .filter(f -> "scheme".equals(f.featureName())).toList();
        assertEquals(2, deepNavs.size(), "the fixture carries the predicate + extract reads");
        for (RFeatureCall nav : deepNavs) {
            assertInstanceOf(IRQualifierItemNav.class,
                    adapter.adapt(nav, ws).orElseThrow(() -> new AssertionError(
                            "every pipe-depth qualifier read must mint — the deep walk")),
                    "the multi-hop pipe resolves to the same terminal feature");
        }
        REqualityExpr qualEq = AstWalker.findAll(qualDeep, REqualityExpr.class).stream()
                .findFirst().orElseThrow();
        BinaryOp qualEqIr = assertInstanceOf(BinaryOp.class,
                adapter.adapt(qualEq, ws).orElseThrow(() -> new AssertionError(
                        "the `item -> scheme = \"…\"` equality composes — the #512 operand"
                                + " admission")),
                "the equality lowers whole");
        assertInstanceOf(IRQualifierItemNav.class, qualEqIr.left(), "the admitted operand");

        // The qualifier contrast: the SAME spelling over a NON-meta source keeps the
        // decline (prove-or-decline at the walk's terminal).
        RFeatureCall qualMiss = navUnder(fixtureFile, "QualMiss512", "scheme");
        assertEquals(Optional.of("featureUnresolved.nonSymbolReceiver"),
                adapter.declineReason(qualMiss, ws),
                "a qualifier read over a plain source keeps the honest face");
    }

    @Test
    void functionHeadRecordReadMintsSinceThe520Probe2() throws IOException {
        // #520 probe2 (the decline-decode loop's teach): the probe1 headOtherGate census
        // read the featureUnresolved.headOther face 100% RFunction.mRecHit, 100%
        // drr-FUNCTION — the `GetExecutionTimestamp(reportableEvent) -> date` record reads
        // over a record-returning CALL head (4 atRoot + 16 interior: comparison 4 /
        // extract 4 / logical 8). The FUNCTION-head slice joins the #512-B1
        // receiver-carrying mint (the childless #507/#509 IRRecordFeatureNav renders BY
        // NAME at attribute heads and cannot carry a call receiver), and the
        // RECEIVER-carrying kind joins isOracleNavOperand so the containing
        // comparison/equality roots compose (the routing is the STANDING kind-wide
        // containsOracleLeaf walk — zero compiler changes). The member-miss contrast keeps
        // the honest headOther belt (removing the member gate turns it).
        String source = """
                namespace "test.step520e"

                type Wrap520:
                    at zonedDateTime (1..1)

                func GetStamp520:
                    inputs:
                        w Wrap520 (1..1)
                    output:
                        stamp zonedDateTime (0..1)
                    set stamp:
                        w -> at

                func UseStamp520:
                    inputs:
                        w Wrap520 (1..1)
                        cutoff date (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        cutoff <= GetStamp520(w) -> date

                func MissStamp520:
                    inputs:
                        w Wrap520 (1..1)
                    output:
                        out date (0..1)
                    set out:
                        GetStamp520(w) -> notAFeature520
                """;
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                models.add(AstBuilder.buildFromFile(p));
            }
        }
        models.add(AstBuilder.buildFromString(source, "step520-e.rosetta"));
        RWorkspace ws = RWorkspace.build(models).workspace();
        var fixtureFile = ws.files().get(ws.files().size() - 1);

        // The record read over the CALL head mints the receiver-carrying kind, the
        // receiver the lowered call.
        RFeatureCall stampNav = navUnder(fixtureFile, "UseStamp520", "date");
        IRRecordReceiverNav minted = assertInstanceOf(IRRecordReceiverNav.class,
                adapter.adapt(stampNav, ws).orElseThrow(() -> new AssertionError(
                        "a record read over a record-returning call head must mint since"
                                + " the #520 probe2 teach")),
                "the headOther RFunction.mRecHit class lowers to the #512 kind");
        assertEquals("date", minted.featureName(), "the record feature is the neutral fact");
        assertEquals("zonedDateTime", minted.recordTypeName(), "the record type's name");
        assertInstanceOf(IRApply.class, minted.receiver(),
                "the receiver is the LOWERED call — the receiver-carrying mint's premise");

        // The containing comparison composes — the isOracleNavOperand growth (the
        // interior.RComparisonExpr census rows; removing the growth turns this red).
        RComparisonExpr cmp = AstWalker.findAll(fixtureFile, RComparisonExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no comparison parsed"));
        BinaryOp cmpIr = assertInstanceOf(BinaryOp.class,
                adapter.adapt(cmp, ws).orElseThrow(() -> new AssertionError(
                        "the `cutoff <= <call> -> date` comparison must compose since the"
                                + " #520 probe2 operand admission")),
                "the comparison lowers whole");
        assertInstanceOf(IRRecordReceiverNav.class, cmpIr.right(),
                "the record read composes as the RIGHT operand");

        // The member-miss contrast: a leaf off the record's OWN feature list keeps the
        // honest headOther belt (the #444 membership rule at the FUNCTION-head seat).
        RFeatureCall missNav = navUnder(fixtureFile, "MissStamp520", "notAFeature520");
        assertEquals(Optional.of("featureUnresolved.headOther"),
                adapter.declineReason(missNav, ws),
                "the member-miss residue keeps the honest face");
    }

    @Test
    void argKindSweepAdmitsFiveLoweredKindsAtTheCallGate() {
        // #512 — the ARG-KIND ADMISSION SWEEP (the #511 arg:BinaryOp / #505 arg:IRListOp
        // pattern VERBATIM, five kinds): arg:IRToString 51 + arg:IRListConstruct 28 +
        // arg:IRLambdaOp 26 + arg:IRConstruct 22 + arg:IRConditional 16 sole at the #511
        // SOT. Each lowered node IS the faithful argument; the render routes through the
        // callArgs oracle serve (IRToString by its standing #503 leaf-kind walk, the other
        // four by the #512 composed-shape apply-arg legs). The standing #503 lock
        // `a call with a modified-equality arg stays declined` keeps the gate's decline
        // surface witnessed (the census-narrow IRAllAnyCompare contrast).
        String source = """
                namespace "test.step512c"

                type Box512:
                    num number (1..1)
                    tags string (0..*)
                    flag boolean (1..1)

                type Thing512:
                    val string (1..1)

                func TakeS512:
                    inputs:
                        s string (1..1)
                    output:
                        o string (1..1)
                    set o:
                        s

                func TakeMulti512:
                    inputs:
                        xs string (0..*)
                    output:
                        o string (0..1)
                    set o:
                        xs first

                func TakeThing512:
                    inputs:
                        t Thing512 (1..1)
                    output:
                        o string (1..1)
                    set o:
                        t -> val

                func CallToString512:
                    inputs:
                        b Box512 (1..1)
                    output:
                        out string (1..1)
                    set out:
                        TakeS512(b -> num to-string)

                func CallListLit512:
                    output:
                        out string (0..1)
                    set out:
                        TakeMulti512(["a", "b"])

                func CallLambda512:
                    inputs:
                        b Box512 (1..1)
                    output:
                        out string (0..1)
                    set out:
                        TakeMulti512(b -> tags extract item)

                func CallConstruct512:
                    output:
                        out string (1..1)
                    set out:
                        TakeThing512(Thing512 { val: "x" })

                func CallCond512:
                    inputs:
                        b Box512 (1..1)
                    output:
                        out string (1..1)
                    set out:
                        TakeS512(if b -> flag then "x" else "y")
                """;
        RModel model = AstBuilder.buildFromString(source, "step512-callargs.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        record SweepCase(String caller, String callee, Class<? extends IRExpr> argKind) { }
        List<SweepCase> cases = List.of(
                new SweepCase("CallToString512", "TakeS512", IRToString.class),
                new SweepCase("CallListLit512", "TakeMulti512", IRListConstruct.class),
                new SweepCase("CallLambda512", "TakeMulti512", IRLambdaOp.class),
                new SweepCase("CallConstruct512", "TakeThing512", IRConstruct.class),
                new SweepCase("CallCond512", "TakeS512", IRConditional.class));
        for (SweepCase c : cases) {
            RSymbolReference call = callUnder(ws, c.caller(), c.callee());
            assertEquals(Optional.empty(), adapter.declineReason(call, ws),
                    "a lowered " + c.argKind().getSimpleName()
                            + " call argument admits since #512");
            IRApply ir = assertInstanceOf(IRApply.class, adapt(call, ws),
                    "the call lowers whole for " + c.caller());
            assertInstanceOf(c.argKind(), ir.args().get(0),
                    "the argument is the lowered kind — load-bearing");
        }
    }

    @Test
    void outputAndMetaParamRefsMintAtTheBareSeats() {
        // #514 (witness 1 — ARM-1 IROutputRef + ARM-3 IRMetaParamRef + the shared 1b/3b
        // disguised-nav acceptance): a bare function-OUTPUT reference mints the shallow
        // IROutputRef oracle leaf (the notAnInputParam.out decode — 100% output-by-identity);
        // a bare META-annotated input mints IRMetaParamRef; a `metaParam -> feature` disguise
        // lowers FieldAccess over the leaf (the recv:metaParam.* heal — the SAME acceptance
        // leg serves the output-headed disguises, probe-proven at population). The negative
        // witnesses: a META output keeps the named decline face (the wrapper coercion is
        // legacy's own), and a call passing the meta param as an ARG keeps declining — the
        // kind's containment (the pPlain/argSeatMiss decode slices need legacy's unwrap).
        String source = """
                namespace "test.step514"

                type Priced14:
                    value number (1..1)

                func SelfOut14:
                    inputs:
                        xs string (0..*)
                    output:
                        outs string (0..*)
                    add outs:
                        xs
                    set outs:
                        outs distinct

                func MetaNav14:
                    inputs:
                        price Priced14 (1..1)
                            [metadata scheme]
                    output:
                        out number (1..1)
                    set out:
                        price -> value

                func MetaBare14:
                    inputs:
                        price Priced14 (1..1)
                            [metadata scheme]
                    output:
                        out Priced14 (1..1)
                    set out:
                        price

                func TakeP14:
                    inputs:
                        p Priced14 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        p -> value exists

                func MetaArg14:
                    inputs:
                        price Priced14 (1..1)
                            [metadata scheme]
                    output:
                        out boolean (1..1)
                    set out:
                        TakeP14(price)

                func TakeP14Multi:
                    inputs:
                        ps Priced14 (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        ps exists

                func MetaArgMulti14:
                    inputs:
                        price Priced14 (1..1)
                            [metadata scheme]
                    output:
                        out boolean (1..1)
                    set out:
                        TakeP14Multi(price)

                func MetaOut14:
                    inputs:
                        x string (1..1)
                    output:
                        outRef string (1..1)
                            [metadata scheme]
                    set outRef:
                        x
                    add outRef:
                        outRef
                """;
        RModel model = AstBuilder.buildFromString(source, "step514-outmeta.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // ARM-1: the self-referencing output claim (`outs distinct` — the GetAllBusinessCenters
        // corpus shape, the p:RListOpExpr decode row).
        RSymbolReference outsRef = bareRefUnder(ws, "SelfOut14", "outs");
        IROutputRef outMint = assertInstanceOf(IROutputRef.class,
                adapter.adapt(outsRef, ws).orElseThrow(() -> new AssertionError(
                        "a bare output reference must mint since the #514 ARM-1")),
                "the mint is the IROutputRef oracle leaf");
        assertEquals("outs", outMint.outputName());
        assertEquals(ExpressionCardinality.MULTI, outMint.cardinality(),
                "the (0..*) output carries the MULTI fact from the attribute channel");
        assertEquals(Optional.empty(), adapter.declineReason(outsRef, ws),
                "empty <=> lowers: the notAnInputParam.out face is REMOVED (the flip's"
                        + " negative witness)");

        // ARM-3: the bare meta-param claim (the statement-seat decode class) + the 1b/3b
        // nav acceptance over it.
        RSymbolReference priceRef = bareRefUnder(ws, "MetaBare14", "price");
        IRMetaParamRef mpMint = assertInstanceOf(IRMetaParamRef.class,
                adapter.adapt(priceRef, ws).orElseThrow(() -> new AssertionError(
                        "a bare meta-annotated input must mint since the #514 ARM-3")),
                "the mint is the IRMetaParamRef oracle leaf");
        assertEquals("price", mpMint.paramName());
        assertFalse(mpMint.type().isMissing(),
                "the attribute channel types the mint (a scheme qualifier is not a type"
                        + " wrapper — the channel reads the PLAIN form; the [metadata]"
                        + " annotation itself is the arm's gate)");
        REnumValueRef priceNav = chainUnder(ws, "MetaNav14", "price", "value");
        FieldAccess mpHop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(priceNav, ws).orElseThrow(() -> new AssertionError(
                        "a metaParam-headed disguise must claim via the #514 arm-3b leg")),
                "the disguise lowers to FieldAccess over the leaf");
        assertInstanceOf(IRMetaParamRef.class, mpHop.receiver(),
                "the base is the oracle leaf — every containing root oracle-serves");
        assertEquals(priceNav.sourceRange(), mpHop.receiver().sourceRange(),
                "the base re-stamps the RAW node's range — the correlation key");
        assertEquals(Optional.empty(), adapter.declineReason(priceNav, ws),
                "empty <=> lowers: the recv:metaParam face is REMOVED at this seat");

        // #530 arm-D — the #514 CONTAINMENT RETIRED for the DEREF-PROVEN slice: a meta
        // param passed where the callee declares the SAME value type, meta-free and
        // single-card (legacy tryMetaDerefArg's own preconditions — the null-guarded
        // getValue() deref) now ADMITS at the callArgs gate; the deref render is legacy's
        // own inside the oracle-root serve (IRMetaParamRef is a containsOracleLeaf
        // member — byte-identical BY IDENTITY).
        RSymbolReference metaArgCall = callUnder(ws, "MetaArg14", "TakeP14");
        assertTrue(adapter.adapt(metaArgCall, ws).isPresent(),
                "the deref-proven meta-param argument admits — the #530 arm-D claim");
        assertEquals(Optional.empty(), adapter.declineReason(metaArgCall, ws),
                "empty <=> lowers: the arg:IRMetaParamRef face is REMOVED at the"
                        + " deref-proven seat (the flip's negative witness)");

        // The arm's PAIRED GUARD (census-narrow — dir:other read ZERO): a MULTI-card
        // same-type param fails the deref preconditions (legacy tryMetaDerefArg declines
        // multi), so the call keeps the standing containment and the generic token.
        RSymbolReference metaArgMultiCall = callUnder(ws, "MetaArgMulti14", "TakeP14Multi");
        assertTrue(adapter.adapt(metaArgMultiCall, ws).isEmpty(),
                "a multi-card param keeps the whole call declining — the containment"
                        + " survives outside the deref-proven slice");
        assertEquals(Optional.of("arg:IRMetaParamRef"),
                adapter.declineReason(metaArgMultiCall, ws),
                "the residue re-spells at the callArgs gate's generic arg:<Class> token");

        // The META-output contrast: the wrapper coercion is legacy's own — the named face.
        RSymbolReference metaOutRef = bareRefUnder(ws, "MetaOut14", "outRef");
        assertTrue(adapter.adapt(metaOutRef, ws).isEmpty(),
                "a META-annotated output keeps declining — the wrapper render is legacy's");
        assertEquals(Optional.of("notAnInputParam.out.meta"),
                adapter.declineReason(metaOutRef, ws),
                "the meta-output residue keeps its named face");
    }

    @Test
    void ruleInputTopNavLowersOverTheParamLeaf() {
        // #514 (witness 2 — ARM-2): the TOP-LEVEL bare rule-input navigation claims through
        // the throwaway `input`-rooted equivalent — the #497 orphan-input arm lowers the
        // receiver to IRVariable{PARAM,"input"} (the proven MapperS.of(input) render) and the
        // hop is the standing FieldAccess compose; the IN-LAMBDA slice keeps its #485
        // synthetic-base route (the contrast — both slices in one fixture).
        String source = """
                namespace "test.step514b"

                type Trade14:
                    leg Leg14 (0..*)

                type Leg14:
                    rate number (1..1)

                reporting rule RiTop14 from Trade14:
                    leg exists

                reporting rule RiLam14 from Trade14:
                    filter leg exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step514-ruletop.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        RRule topRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiTop14".equals(r.name())).findFirst().orElseThrow();
        RSymbolReference topLeg = AstWalker.findAll(topRule, RSymbolReference.class).stream()
                .filter(r -> "leg".equals(r.name())).findFirst().orElseThrow();
        IRRuleInputNav topNav = assertInstanceOf(IRRuleInputNav.class,
                adapter.adapt(topLeg, ws).orElseThrow(() -> new AssertionError(
                        "a TOP-LEVEL rule-input bare attr must claim since the #514 ARM-2")),
                "the claim is the shallow oracle leaf (the probe2 pivot — the native"
                        + " FieldAccess form leaked to the emitter decline site; the serve"
                        + " re-synthesizes the input receiver inside legacy's own line)");
        assertEquals("leg", topNav.featureName());
        assertEquals(topLeg.sourceRange(), topNav.sourceRange(),
                "the mint carries the RAW node's range — the correlation key");
        assertFalse(topNav.type().isMissing(),
                "the raw node's own cached type rides the mint — the consumer gates read it");
        assertEquals(Optional.empty(), adapter.declineReason(topLeg, ws),
                "empty <=> lowers: the ruleInputNav face is REMOVED at the top-level slice"
                        + " (the flip's negative witness)");

        // The IN-LAMBDA contrast: the #485 synthetic-base route is byte-unchanged.
        RRule lamRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiLam14".equals(r.name())).findFirst().orElseThrow();
        RSymbolReference lamLeg = AstWalker.findAll(lamRule, RSymbolReference.class).stream()
                .filter(r -> "leg".equals(r.name())).findFirst().orElseThrow();
        FieldAccess lamNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(lamLeg, ws).orElseThrow(),
                "the in-lambda slice keeps claiming through the #485 source gates");
        IRVariable lamBase = assertInstanceOf(IRVariable.class, lamNav.receiver());
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, lamBase.variableKind(),
                "the in-lambda receiver stays the #479-taught synthetic base — the two"
                        + " slices split exactly at hasEnclosingRuleLambda");
    }

    @Test
    void ruleInputBoundItemNavLowersAsTheSameOracleLeaf() {
        // v3.3 seat 2 (PR #638) - THE HEAL: a literal `item` ROOTING a navigation at rule-body top
        // level is the rule input (no lambda binds `item` there), and legacy renders it on the same
        // `input` root as the bare-attr form at the same seat - handle(RImplicitVariable)'s
        // itemNavReceiverInputSlot arm, PR #579, whose own note names the bare-attr form its
        // two-forms-agree mirror. So the claim mints the SAME shallow IRRuleInputNav the #514 ARM-2
        // bare-attr claim above mints, and the render is the containing root's whole-legacy oracle
        // serve BY IDENTITY. The census measured this class at 152 arrivals per drr 7.x RULE cell,
        // 100% itemBind.noLambda.leafPlain...inRule.typed, ZERO on any FUNCTION seam.
        String source = """
                namespace "test.step638"

                type Trade38:
                    leg Leg38 (0..*)
                    crit Crit38 (1..1)

                type Leg38:
                    rate number (1..1)

                type OptA38:
                    fieldA number (1..1)

                choice Crit38:
                    OptA38

                reporting rule RiItemTop38 from Trade38:
                    item -> leg

                reporting rule RiSwitchTyped38 from Trade38:
                    crit switch
                        OptA38 then item -> fieldA

                reporting rule RiSwitchOpen38 from Trade38:
                    crit switch
                        default item -> fieldA

                reporting rule RiItemLam38 from Trade38:
                    leg filter item -> rate exists

                reporting rule RiNamedSort38 from Trade38:
                    leg sort l [ item -> rate ]

                reporting rule RiReduce38 from Trade38:
                    leg reduce a, b [ item -> rate ]

                func FnItem38:
                    inputs:
                        t Trade38 (1..1)
                    output:
                        out Leg38 (0..*)
                    set out:
                        t -> leg
                """;
        RModel model = AstBuilder.buildFromString(source, "step638-ruleitemnav.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        RRule topRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiItemTop38".equals(r.name())).findFirst().orElseThrow();
        RFeatureCall topNavCall = AstWalker.findAll(topRule, RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the fixture must carry an item-rooted navigation at rule top level"));
        IRRuleInputNav claimed = assertInstanceOf(IRRuleInputNav.class,
                adapter.adapt(topNavCall, ws).orElseThrow(() -> new AssertionError(
                        "a rule-input-bound item navigation must claim since v3.3 seat 2")),
                "the claim is the SAME shallow oracle leaf the #514 bare-attr form mints - the"
                        + " neutral fact is identical (navigate this feature off the rule input)");
        assertEquals("leg", claimed.featureName());
        assertEquals(topNavCall.sourceRange(), claimed.sourceRange(),
                "the mint carries the RAW node's range - the correlation key");
        assertEquals(Optional.empty(), adapter.declineReason(topNavCall, ws),
                "empty <=> lowers: the itemNotFilterExtractBound face is REMOVED at this slice"
                        + " (the flip's negative witness)");

        // NEGATIVE CONTROL 1 - the in-lambda item is UNMOVED: the widening must not reach the
        // filter/extract binder legs, and this pins that it does not. It does NOT witness the
        // RInlineFunction stop, and the first cut said it did (PR #638 round 1, cq MF-3): a
        // filter-bound item passes isFilterExtractBoundItemReceiver at the receiver gate, so the
        // residue block holding the mint is never entered, and the arm's own !itemBindingBound
        // conjunct excludes it a second time. Lane W2 deletes the RInlineFunction stop and this
        // stays GREEN - a fact about THIS carrier, not about the stop: a filter body is excluded
        // twice over, so W2 was green by construction here (PR #638 round 2, cq MF-1). The stop's
        // own residue and its witness are negative control 4 below.
        RRule lamRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiItemLam38".equals(r.name())).findFirst().orElseThrow();
        RFeatureCall lamNavCall = AstWalker.findAll(lamRule, RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow();
        assertFalse(adapter.adapt(lamNavCall, ws).orElseThrow() instanceof IRRuleInputNav,
                "an in-lambda item keeps its own standing route - the widening must not reach the"
                        + " filter/extract binder legs (control 1 pins the standing route, not the"
                        + " RInlineFunction stop - control 4 does)");

        // NEGATIVE CONTROL 2 - the SHAPE this class needs is absent from a FUNCTION body, which is
        // why the census read ZERO itemBind arrivals on the FUNCTION seam. This asserts the fixture,
        // not the walk (PR #638 round 1, cq MF-3 / spec SF-8): FnItem38's only feature call is
        // `t -> leg`, an RSymbolReference receiver, so the stream is empty on every tree. The
        // RFunction stop it was first labelled with cannot fire in any case - no RRule can enclose
        // an RFunction, so the walk runs off the top and returns null regardless (lane W4 deletes it
        // and this test stays green); it is the same defensive leg enclosingRule carries.
        assertNull(ruleInputBoundItemFor(ws, "FnItem38"),
                "a FUNCTION body carries no item-rooted navigation - the class's FUNCTION-seam zero");

        // NEGATIVE CONTROL 3 - THE CASE SPLIT that keeps the RSwitchExpr stop off the hot path, and
        // the pin a future widening must turn. Round 1's cq MF-3 predicted that a case body under a
        // non-type guard reaches this arm with itemBindingBound FALSE, so that only the RSwitchExpr
        // stop prevents a wrong mint - legacy binds `item` to the switch SUBJECT there (the #221
        // switchChoiceHoist arm, which runs BEFORE itemNavReceiverInputSlot). Cut as a fixture and
        // MEASURED, it does not, and the two branches below are why:
        //   (a) a case that is NOT type-guarded narrows `item` to nothing, so no feature off it
        //       resolves and the claim declines at featureUnresolved BEFORE any receiver gate. The
        //       arm mints for an RFeatureCall only, so it is unreachable from here;
        //   (b) a TYPE-guarded case is precisely what switchCaseNarrowedBinding binds, so
        //       itemBindingBound is TRUE and the arm is excluded by its own conjunct - the claim
        //       lowers through the #523 arm-B admission instead.
        // Lane W3 deletes the RSwitchExpr stop and this test stays GREEN, as W4 does: this stop and
        // the RFunction stop are belts over the caller's guard chain and the linker (the
        // RInlineFunction stop is a belt over the front end's D41 lock instead - control 4). THE
        // DAY (a) CHANGES - a teach
        // that narrows `item` on a non-type-guarded case - 3a goes RED and the stop becomes
        // load-bearing, which is the moment the implementer needs to be sent here.
        RRule openRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiSwitchOpen38".equals(r.name())).findFirst().orElseThrow();
        RFeatureCall openNav = AstWalker.findAll(openRule, RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the fixture must carry an item-rooted navigation in a non-type-guarded case"));
        assertTrue(adapter.declineReason(openNav, ws).orElse("").startsWith("featureUnresolved"),
                "(a) a non-type-guarded case narrows `item` to nothing, so the claim declines at the"
                        + " linker and never reaches the rule-input arm - if this turns, the"
                        + " RSwitchExpr stop has become load-bearing and needs its own witness");
        assertFalse(adapter.adapt(openNav, ws).orElse(null) instanceof IRRuleInputNav,
                "(a) and it certainly does not mint the rule-input nav");

        RRule typedRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                .filter(r -> "RiSwitchTyped38".equals(r.name())).findFirst().orElseThrow();
        RFeatureCall typedNav = AstWalker.findAll(typedRule, RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "the fixture must carry an item-rooted navigation in a TYPE-guarded case"));
        assertFalse(adapter.adapt(typedNav, ws).orElse(null) instanceof IRRuleInputNav,
                "(b) a TYPE-guarded case IS the switchCaseNarrowedBinding the arm's own"
                        + " !itemBindingBound conjunct excludes - the item is the case subject, and"
                        + " the claim keeps the #523 arm-B route");
        assertTrue(adapter.adapt(typedNav, ws).isPresent(),
                "(b) and it LOWERS, through the #523 arm-B admission - an outright decline would"
                        + " satisfy the line above alone (PR #638 round 2, cq SF-5)");

        // NEGATIVE CONTROL 4 - THE RInlineFunction STOP'S OWN RESIDUE, which is NOT the conjunct's
        // (PR #638 round 2, cq MF-1). Under a NAMED binder - a then / min / max / sort body with a
        // parameter, or any reduce body - itemBindingBound is FALSE (newLegItemBindingSource returns
        // null for a named lambda), so the arm's own conjunct does NOT exclude an item-rooted
        // navigation there; the first cut said it did, on lane W2's evidence, and W2's only carrier
        // was RiItemLam38, a FILTER body excluded twice over - green by construction. What excludes
        // the class on this tree is the FRONT END: the literal `item` keyword is shadowed by an
        // explicit closure parameter (TypeInferenceEngine.computeImplicitItemType's D41-LOCK keeps
        // `item` MISSING under a body WITH a parameter), so no feature resolves off it and the claim
        // declines at featureUnresolved BEFORE any receiver gate - the same case split as (a) above,
        // MEASURED here on two carriers (the named sort and the reduce body; the named-then carrier
        // was RETIRED at v3.3 seat 3 - it rode the fork-only `then l [ ... ]` shape upstream's grammar
        // refuses; the seat-6 grammar port is not on the tree yet, and a refused fixture reddens the parse, not the stop). The stop is a belt over that lock, not over the conjunct,
        // and the census's zero `named.` / `thenBody` faces in the class is why nothing reaches it.
        // Lanes T1 / T2 (on offload box, the lock DROPPED in the parser) measure what the stop guards: with
        // the lock dropped and the stop present, (b) holds and (a) goes RED - the tripwire fires;
        // with the lock dropped AND the stop deleted, (b) goes RED - the arm MINTS the rule input
        // where legacy's nearestEnclosingInlineFunction gate would not render it. (b) is asserted
        // FIRST so a red print names the stop, not the tripwire, when the stop is what moved. Since v3.3
        // seat 3 (PR #638 round 3, spec NIT-1 = cq NIT-1 / NIT-2) the two carriers run under ONE assertAll,
        // so a lane print carries BOTH verdicts (the #638 T lanes evidenced the first carrier only), and
        // (a)'s message carries the reason the adapter READ, so a red print names WHERE the claim declined.
        assertAll(List.of("RiNamedSort38", "RiReduce38").stream().map(named -> (org.junit.jupiter.api.function.Executable) () -> {
            RRule namedRule = AstWalker.findAll(ws.files().get(0), RRule.class).stream()
                    .filter(r -> named.equals(r.name())).findFirst().orElseThrow();
            RFeatureCall namedNav = AstWalker.findAll(namedRule, RFeatureCall.class).stream()
                    .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                    .findFirst().orElseThrow(() -> new AssertionError(
                            named + ": the fixture must carry an item-rooted navigation under a named binder"));
            assertFalse(adapter.adapt(namedNav, ws).orElse(null) instanceof IRRuleInputNav,
                    named + ": (b) an item under a NAMED binder is not the rule input - the RInlineFunction"
                            + " stop must not let the walk reach the RRule");
            String reason = adapter.declineReason(namedNav, ws).orElse("<lowers>");
            assertTrue(reason.startsWith("featureUnresolved"),
                    named + ": (a) the front end shadows `item` under a named parameter (the D41 lock), so"
                            + " the claim declines at the linker - if this turns, the RInlineFunction stop"
                            + " is load-bearing and (b) is its witness; the adapter read: " + reason);
        }));
    }

    /**
     * The negative control's reader: the adapter's own verdict on a FUNCTION body's item-rooted
     * navigation, expressed through the public surface (an item-rooted nav in a function body is
     * absent from the fixture by construction, so the control asserts the SHAPE is absent rather than
     * reaching a private walk - the reviewers' read-only law).
     */
    private static RFeatureCall ruleInputBoundItemFor(RWorkspace ws, String functionName) {
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> functionName.equals(f.name())).findFirst().orElseThrow();
        return AstWalker.findAll(fn, RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RImplicitVariable)
                .findFirst().orElse(null);
    }

    @Test
    void numberConstraintGapsAdmitAtTheArgNavAgreementGate() {
        // #514 (witness 3 — ARM-4): the #489 type-agreement mirror widens to JAVA equality
        // for the number family — a constraint-differing NON-INTEGER number pair (the
        // g:number_number decode class, 100% of the typeGap.synItem.hop2 face) maps to
        // BigDecimal on BOTH sides, so legacy's paramJavaType.equals(actualItemType)
        // precondition passes the arg RAW and the admission is byte-safe; an int-vs-number
        // pair (the genuine Integer→BigDecimal hoist class) keeps the face, g-suffixed.
        String source = """
                namespace "test.step514c"

                func TakeNum14:
                    inputs:
                        n number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        n
                """;
        RModel model = AstBuilder.buildFromString(source, "step514-numgap.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RAttribute parsedParam = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "TakeNum14".equals(f.name())).findFirst().orElseThrow()
                .inputs().get(0);

        RExpression rawArg = intLiteral(1);
        RMetaAnnotatedType constrained = RMetaAnnotatedType.withNoMeta(new RNumberType(
                java.util.OptionalInt.of(30), java.util.OptionalInt.empty(),
                Optional.empty(), Optional.empty()));
        RMetaAnnotatedType intTyped = RMetaAnnotatedType.withNoMeta(new RNumberType(
                java.util.OptionalInt.empty(), java.util.OptionalInt.of(0),
                Optional.empty(), Optional.empty()));
        IRVariable root = new IRVariable("q", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                constrained, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                SourceRange.NONE);
        FieldAccess constrainedHop = new FieldAccess(root, "rate", NodeId.ROOT, constrained,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(constrainedHop, rawArg, parsedParam, ws),
                "a constraint-differing non-integer number pair ADMITS — both sides are"
                        + " BigDecimal, legacy passes the arg raw (the #514 widening)");

        FieldAccess intHop = new FieldAccess(root, "count", NodeId.ROOT, intTyped,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("typeGap.param.hop1.g:int_number",
                ExpressionToIRAdapter.argNavFacet(intHop, rawArg, parsedParam, ws),
                "the int-vs-number pair keeps the face — the genuine Integer→BigDecimal"
                        + " hoist class stays legacy's, g-suffixed by the #514 decode; SINCE"
                        + " #527 this pin doubles as the arms-dead widening's boundary"
                        + " negative: a PARAM-rooted int→number chain has NO standing serve"
                        + " (scanNone at a native root — a native render would drop the"
                        + " hoist), so the face survives the #527 recut exactly here");
    }

    @Test
    void argNavArmsDeadWideningAdmitsTheCensusClasses() {
        // #527 (the wave witness) — the ARG_NAV-dict teach: the argGapGate census (probe0,
        // the #526-SOT pool of 52) read legacy's OWN evaluate-arg preconditions BY CALL —
        // a coercion fires ONLY on a numeric/meta-wrapper ACTUAL into a BigDecimal param
        // (ReferenceHandler.tryMetaDerefArg's three arms) — so the #489 agreement gate
        // recut to the ARMS-DEAD split. The four fixture legs mirror the census classes:
        // (1) the MODEL-pair raw pass (the census's rawNoArm/paramUnread verdicts — the
        // StrikeSchedule_RateSchedule class), parsed end-to-end; (2) the int_int raw pass
        // (constraint-differing int pair — an int-family param never maps to BigDecimal,
        // so the #514 MF-3 Java-equality wall dissolves: no arm needs the equality);
        // (3) the synItem.deep int_number SERVE admission (the ≥3-hop item chain trips the
        // standing post-pin scan and serves whole-legacy — the census's scan:multiHopItemNav
        // column) with the hop2 boundary negative (no standing serve — the face survives);
        // (4) the missing-hop admission rides the retensed #488 pin (the reason-mirror
        // fixture) and the #491 belt pin — not restated here.
        String source = """
                namespace "test.step527"

                type Sched27:
                    factor number (1..1)

                type StrikeSched27 extends Sched27:
                    cap number (1..1)

                type Deal27:
                    strike StrikeSched27 (1..1)

                func TakeSched27:
                    inputs:
                        s Sched27 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        s -> factor

                func CallModelPair27:
                    inputs:
                        deal Deal27 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        TakeSched27(deal -> strike)
                """;
        RModel model = AstBuilder.buildFromString(source, "step527-argnav.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // (1) the MODEL-pair leg, parsed end-to-end: the arg chain's stamped result is the
        // SUB-model (StrikeSched27) where the callee param declares the SUPER (Sched27) —
        // pre-#527 the exact typeGap.param.hop1.g:StrikeSched27_Sched27 face; a model-classed
        // actual can never fire a coercion arm, legacy passes the arg RAW and the flat native
        // render is param-type-blind, so the claim ADMITS whole.
        RSymbolReference modelPairCall = callUnder(ws, "CallModelPair27", "TakeSched27");
        assertEquals(Optional.empty(), adapter.declineReason(modelPairCall, ws),
                "the model-pair arg admits — no coercion arm can fire on a model actual");
        IRApply modelPairIr = assertInstanceOf(IRApply.class, adapt(modelPairCall, ws));
        FieldAccess modelPairArg = assertInstanceOf(FieldAccess.class,
                modelPairIr.args().get(0), "the argument is the lowered chain");
        assertEquals("StrikeSched27", modelPairArg.type().type().name(),
                "the premise holds: the stamped arg type IS the sub-model — the pair the"
                        + " #489 R-equality mirror declined");

        // (2) the int_int leg (the hand-built #510-pin idiom over a PARSED int param): a
        // constraint-differing int pair — digits-5 int into the plain int param. R-unequal
        // (RNumberType.equals compares the constraints) but ARMS-DEAD: an int-family param
        // maps to Integer/Long/BigInteger BY DIGITS, never BigDecimal, so neither numeric
        // hoist arm can fire and legacy passes the arg RAW whatever the Java classes are.
        String intSource = """
                namespace "test.step527b"

                func TakeInt27:
                    inputs:
                        k int (1..1)
                    output:
                        out int (1..1)
                    set out:
                        k
                """;
        RModel intModel = AstBuilder.buildFromString(intSource, "step527-intint.rosetta");
        RWorkspace intWs = RWorkspace.build(List.of(intModel)).workspace();
        RAttribute intParam = AstWalker.findAll(intWs.files().get(0), RFunction.class).stream()
                .filter(f -> "TakeInt27".equals(f.name())).findFirst().orElseThrow()
                .inputs().get(0);
        RMetaAnnotatedType intWide = RMetaAnnotatedType.withNoMeta(new RNumberType(
                java.util.OptionalInt.of(5), java.util.OptionalInt.of(0),
                Optional.empty(), Optional.empty()));
        assertTrue(((RNumberType) intWide.type()).isInteger(),
                "the premise holds: the constrained side IS int-family");
        IRVariable intRoot = new IRVariable("q", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                intWide, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess intIntHop = new FieldAccess(intRoot, "seq", NodeId.ROOT, intWide,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(intIntHop, intLiteral(1), intParam, intWs),
                "the int_int pair ADMITS — pre-#527 the typeGap.param.hop1.g:int_int face;"
                        + " the raw-pass verdict needs no Java-equality proof");

        // (3) the synItem.deep int_number SERVE leg (hand-built over the parsed number
        // param of the #514 fixture's shape): a 3-hop SYNTHETIC_ITEM-rooted chain whose
        // result is int-family into a number param — the genuine hoist pair, admitted
        // census-narrow because the ≥3-hop item chain trips the standing post-pin scan
        // (MULTI_HOP_ITEM_NAV) and the claim serves whole-legacy with the hoist inside.
        // The raw head premise: an implicit-variable head (the proven-route set).
        String numSource = """
                namespace "test.step527c"

                func TakeNum27:
                    inputs:
                        n number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        n
                """;
        RModel numModel = AstBuilder.buildFromString(numSource, "step527-intnum.rosetta");
        RWorkspace numWs = RWorkspace.build(List.of(numModel)).workspace();
        RAttribute numParam = AstWalker.findAll(numWs.files().get(0), RFunction.class).stream()
                .filter(f -> "TakeNum27".equals(f.name())).findFirst().orElseThrow()
                .inputs().get(0);
        RMetaAnnotatedType cleanT = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        RMetaAnnotatedType intT = RMetaAnnotatedType.withNoMeta(new RNumberType(
                java.util.OptionalInt.empty(), java.util.OptionalInt.of(0),
                Optional.empty(), Optional.empty()));
        RImplicitVariable itemHead = new RImplicitVariable();
        IRVariable synRoot = new IRVariable("item", IRVariable.VariableKind.SYNTHETIC_ITEM,
                NodeId.ROOT, cleanT, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                SourceRange.NONE);
        FieldAccess deep1 = new FieldAccess(synRoot, "a", NodeId.ROOT, cleanT,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        FieldAccess deep2 = new FieldAccess(deep1, "b", NodeId.ROOT, cleanT,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        FieldAccess deep3 = new FieldAccess(deep2, "count", NodeId.ROOT, intT,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(deep3, itemHead, numParam, numWs),
                "the synItem.deep int→number chain ADMITS for the serve — pre-#527 the"
                        + " typeGap.synItem.deep.g:int_number face (9 sole at the #526 SOT)");
        FieldAccess hop2Only = new FieldAccess(deep1, "count", NodeId.ROOT, intT,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertEquals("typeGap.synItem.hop2.g:int_number",
                ExpressionToIRAdapter.argNavFacet(hop2Only, itemHead, numParam, numWs),
                "the hop2 boundary NEGATIVE: a 2-hop item chain is the #469-taught NATIVE"
                        + " class (no scan trip, no serve), so the census-narrow admission"
                        + " stops at deep and the face survives exactly here");
    }

    /** The unique parsed {@code … -> feature} nav with the given leaf under the named function. */
    private RFeatureCall navUnder(RModel fixtureFile, String funcName, String featureName) {
        RFunction fn = AstWalker.findAll(fixtureFile, RFunction.class).stream()
                .filter(f -> funcName.equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no function " + funcName));
        return AstWalker.findAll(fn, RFeatureCall.class).stream()
                .filter(f -> featureName.equals(f.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no `-> " + featureName + "` nav under " + funcName));
    }

    @Test
    void arithmeticOperandClusterAdmitsSinceThe517Teach() {
        // #517 (witness 1 — the ARITHMETIC operand cluster): the seven #516-SOT decline faces
        // join isArithmeticOperand — count (operandCount), call (operandCall), item
        // (operandItem), NON-numeric literal (operand:IRLiteral — the drr-RULE string-concat
        // class) and to-string (operand:IRToString) are fixtured here; the synthetic-item
        // nav (operand:IRSynItemNav) and the record read (operand:IRRecordFeatureNav) share
        // the same one-line kind admissions but their engine states have no compact fixture
        // (the record read lowers only on the live no-channel head shapes; the synthetic
        // item needs the un-retypeable binder state) — the census carries their live proof.
        // The negative witness: a MULTI-cardinality param operand keeps the
        // operandMultiParam face (the honest residue). Routing safety is the compiler's:
        // the four native-kind operands take twin-exact containsOracleLeaf shape legs, the
        // three oracle-leaf kinds ride the standing kind walk.
        String source = """
                namespace "test.step517a"

                func OneNum17:
                    inputs:
                        x number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        x

                func CountArith17:
                    inputs:
                        xs string (0..*)
                    output:
                        out int (1..1)
                    set out:
                        xs count + 1

                func CallArith17:
                    inputs:
                        x number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        OneNum17(x) + 1

                func StringConcat17:
                    inputs:
                        s string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        "pre" + s

                func ToStringConcat17:
                    inputs:
                        n number (1..1)
                    output:
                        out string (1..1)
                    set out:
                        n to-string + "x"

                func ItemArith17:
                    inputs:
                        ns number (0..*)
                    output:
                        outs number (0..*)
                    set outs:
                        ns extract [ item * 2 ]

                func MultiParamArith17:
                    inputs:
                        ms number (0..*)
                    output:
                        out number (1..1)
                    set out:
                        ms + 1
                """;
        RModel model = AstBuilder.buildFromString(source, "step517-arith.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // operandCount: `xs count + 1` — the COUNT list-op operand (kind-wide list-op
        // admission; the comparison/equality COUNT carve-outs are different operators).
        RArithmeticExpr countArith = firstUnder(ws, "CountArith17", RArithmeticExpr.class);
        BinaryOp countLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(countArith, ws).orElseThrow(() -> new AssertionError(
                        "a count arithmetic operand must admit since the #517 cluster")),
                "the claim lowers to the arithmetic BinaryOp");
        IRListOp countOperand = assertInstanceOf(IRListOp.class, countLowered.left(),
                "the left operand is the lowered COUNT list-op");
        assertEquals(IRListOp.Kind.COUNT, countOperand.op());
        assertEquals(Optional.empty(), adapter.declineReason(countArith, ws),
                "empty <=> lowers: the operandCount face is REMOVED (the flip's negative"
                        + " witness)");

        // operandCall: `OneNum17(x) + 1` — the IRApply operand.
        RArithmeticExpr callArith = firstUnder(ws, "CallArith17", RArithmeticExpr.class);
        BinaryOp callLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(callArith, ws).orElseThrow(() -> new AssertionError(
                        "a call arithmetic operand must admit since the #517 cluster")));
        assertInstanceOf(IRApply.class, callLowered.left(),
                "the left operand is the lowered call");
        assertEquals(Optional.empty(), adapter.declineReason(callArith, ws));

        // operand:IRLiteral: `"pre" + s` — the NON-numeric literal widening (the drr-RULE
        // string-concat class; a numeric literal was the standing pre-#517 admission).
        RArithmeticExpr concatArith = firstUnder(ws, "StringConcat17", RArithmeticExpr.class);
        BinaryOp concatLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(concatArith, ws).orElseThrow(() -> new AssertionError(
                        "a string-literal arithmetic operand must admit since the #517"
                                + " widening")));
        IRLiteral concatOperand = assertInstanceOf(IRLiteral.class, concatLowered.left(),
                "the left operand is the lowered string literal");
        assertEquals(IRLiteral.LiteralKind.STRING, concatOperand.literalKind());
        assertEquals(Optional.empty(), adapter.declineReason(concatArith, ws));

        // operand:IRToString: `n to-string + "x"` — the to-string operand (a standing
        // oracle-leaf kind — the containing root routes through the kind walk).
        RArithmeticExpr tsArith = firstUnder(ws, "ToStringConcat17", RArithmeticExpr.class);
        BinaryOp tsLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(tsArith, ws).orElseThrow(() -> new AssertionError(
                        "a to-string arithmetic operand must admit since the #517 cluster")));
        assertInstanceOf(IRToString.class, tsLowered.left(),
                "the left operand is the lowered to-string");
        assertEquals(Optional.empty(), adapter.declineReason(tsArith, ws));

        // operandItem: `item * 2` inside the extract lambda — the implicit item variable.
        RArithmeticExpr itemArith = firstUnder(ws, "ItemArith17", RArithmeticExpr.class);
        BinaryOp itemLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(itemArith, ws).orElseThrow(() -> new AssertionError(
                        "an item arithmetic operand must admit since the #517 cluster")));
        IRVariable itemOperand = assertInstanceOf(IRVariable.class, itemLowered.left(),
                "the left operand is the implicit item variable");
        assertTrue(itemOperand.variableKind() == IRVariable.VariableKind.USER_ITEM
                        || itemOperand.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM,
                "the item admission covers both item kinds (twin-exact with the compiler's"
                        + " isItemVariableOperand leg)");
        assertEquals(Optional.empty(), adapter.declineReason(itemArith, ws));

        // The NEGATIVE witness: a MULTI-cardinality param operand keeps declining with the
        // operandMultiParam face — the honest residue the cluster does not claim.
        RArithmeticExpr multiArith = firstUnder(ws, "MultiParamArith17", RArithmeticExpr.class);
        assertTrue(adapter.adapt(multiArith, ws).isEmpty(),
                "a MULTI param operand stays declined — outside the cluster");
        assertEquals(Optional.of("operandMultiParam"),
                adapter.declineReason(multiArith, ws),
                "the residue keeps its named face");
    }

    @Test
    void equalityOperandClusterAdmitsSinceThe518Teach() {
        // #518 (witness 1 — the EQUALITY operand cluster, the #517 arithmetic pattern at the
        // equality seat): the existence-check operand (operand:Existence), the MULTI-param +
        // list-literal pair (operandMultiParam / operand:IRListConstruct) and the then-bound
        // implicit item (operandItem — the drr `then if item = DAIL` class) are fixtured
        // here; the call (operandMultiCall), conversion (operand:IRConversion) and lambda-op
        // (operand:IRLambdaOp) faces share the same one-line kind admissions but their
        // engine states have no compact fixture (the MULTI call needs a dispatch-free
        // multi-output callee in operand position, the conversion/lambda-op faces live
        // corpus-side only) — the census carries their live proof (the engine-state-fixture
        // law). Routing safety is the compiler's: twin-exact containsOracleLeaf equality
        // shape legs (item / Existence / list-construct / call / MULTI-param / lambda-op),
        // the standing kind walk for IRConversion; the item leg deliberately reroutes the
        // standing filter/extract-bound native composes BY IDENTITY (the #504 arm-C1a/C2
        // precedent — the oracleRootLowered receipt carries the moved mass).
        String source = """
                namespace "test.step518a"

                type Leg18:
                    rate number (1..1)

                func ExistsEq18:
                    inputs:
                        leg Leg18 (0..1)
                        flag boolean (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        leg exists = flag

                func MultiListEq18:
                    inputs:
                        codes string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes = ["A", "B"]

                func ThenItemEq18:
                    inputs:
                        code string (1..1)
                    output:
                        out string (1..1)
                    set out:
                        code then if item = "A" then "X" else "Y"

                func TwoLit18:
                    inputs:
                        x number (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        1 = 2
                """;
        RModel model = AstBuilder.buildFromString(source, "step518-equality.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // operand:Existence: `leg exists = flag` — the postfix existence binds tighter than
        // `=`, so the check IS the left equality operand.
        REqualityExpr existsEq = firstUnder(ws, "ExistsEq18", REqualityExpr.class);
        BinaryOp existsLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(existsEq, ws).orElseThrow(() -> new AssertionError(
                        "an existence-check equality operand must admit since the #518"
                                + " cluster")));
        assertInstanceOf(Existence.class, existsLowered.left(),
                "the left operand is the lowered existence check");
        assertEquals(Optional.empty(), adapter.declineReason(existsEq, ws),
                "empty <=> lowers: the operand:Existence face is REMOVED");

        // operandMultiParam + operand:IRListConstruct: `codes = ["A", "B"]` — both faces in
        // one claim (the MULTI param via isMultiParamOperand, the list literal by kind).
        REqualityExpr multiEq = firstUnder(ws, "MultiListEq18", REqualityExpr.class);
        BinaryOp multiLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(multiEq, ws).orElseThrow(() -> new AssertionError(
                        "a MULTI-param-vs-list-literal equality must admit since the #518"
                                + " cluster")));
        IRVariable multiOperand = assertInstanceOf(IRVariable.class, multiLowered.left(),
                "the left operand is the MULTI param variable");
        assertEquals(IRVariable.VariableKind.PARAM, multiOperand.variableKind());
        assertEquals(ExpressionCardinality.MULTI, multiOperand.cardinality(),
                "the admission is the MULTI slice only — SINGLE params keep the standing"
                        + " native scalar compose (twin-exact with the compiler's"
                        + " isMultiParamOperand leg)");
        assertInstanceOf(IRListConstruct.class, multiLowered.right(),
                "the right operand is the lowered list literal");
        assertEquals(Optional.empty(), adapter.declineReason(multiEq, ws));

        // operandItem: `item = "A"` inside the then body — the then-bound implicit item the
        // filter/extract bound gate never admitted (the drr projection-func class).
        REqualityExpr itemEq = firstUnder(ws, "ThenItemEq18", REqualityExpr.class);
        BinaryOp itemLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(itemEq, ws).orElseThrow(() -> new AssertionError(
                        "a then-bound item equality operand must admit since the #518"
                                + " cluster")));
        IRVariable itemOperand = assertInstanceOf(IRVariable.class, itemLowered.left(),
                "the left operand is the implicit item variable");
        assertTrue(itemOperand.variableKind() == IRVariable.VariableKind.USER_ITEM
                        || itemOperand.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM,
                "the item admission is kind-wide (twin-exact with the compiler's"
                        + " isItemVariableOperand equality leg)");
        assertEquals(Optional.empty(), adapter.declineReason(itemEq, ws));

        // The NEGATIVE witness: the two-numeric-literal pair keeps declining through the
        // sibling-aware licensing gate (no resolved sibling — the honest residue the
        // cluster does not touch).
        REqualityExpr twoLit = firstUnder(ws, "TwoLit18", REqualityExpr.class);
        assertTrue(adapter.adapt(twoLit, ws).isEmpty(),
                "a two-literal equality stays declined — the licensing sets are untouched");
        assertEquals(Optional.of("operandNumericLiteral.sib:literal"),
                adapter.declineReason(twoLit, ws),
                "the residue keeps its named face");
    }

    @Test
    void existenceOperandClusterAdmitsSinceThe519Teach() {
        // #519 (witness 1 — the EXISTENCE operand cluster, the #518 equality pattern at the
        // existence seat): the then-bound implicit item (operandItem), the ELIDED implicit
        // operand (`… then exists` — the without-left grammar arm builds a genuinely-null
        // argument, so the adapter SYNTHESIZES a SYNTHETIC_ITEM typed from the enclosing
        // lambda's own pipe source, the #479 Category-8 law at synthesis) and the
        // list-literal operand (operand:IRListConstruct) are fixtured here; the conversion
        // (operand:IRConversion) and output-alias-nav (operand:IROutputAliasNav) faces share
        // the same one-line kind admissions but their engine states have no compact fixture
        // (the conversion faces live in engine-typed to-enum chains, the output-alias faces
        // need the #518 usesOutput mint's dispatch-variant state) — the census carries their
        // live proof (the engine-state-fixture law). Routing safety is the compiler's: the
        // twin-exact containsOracleLeaf Existence shape legs (item KIND-WIDE — deliberately
        // rerouting the standing filter/extract-bound native existence composes BY IDENTITY,
        // the #518 equality-item precedent, with the oracleRootLowered existence receipt
        // carrying the moved mass — and list-construct), plus the standing kind walk for
        // IRConversion / IROutputAliasNav (no legs — the #501 shallow-kind law).
        String source = """
                namespace "test.step519a"

                func ThenItemExists19:
                    inputs:
                        codes string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes then [ item exists ]

                func ThenElidedExists19:
                    inputs:
                        amounts number (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        amounts then exists

                func ListConstructExists19:
                    inputs:
                        x string (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        [x, "B"] exists

                func BinaryOperandExists19:
                    inputs:
                        x number (1..1)
                        y number (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        (x = y) exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step519-existence.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // operandItem: `item exists` inside the then body — the then-bound implicit the
        // filter/extract bound gate never admitted; the admission is KIND-WIDE (isItemOperand,
        // twin-exact with the compiler's isItemVariableOperand Existence leg).
        RExistenceExpr itemExists = firstUnder(ws, "ThenItemExists19", RExistenceExpr.class);
        Existence itemLowered = assertInstanceOf(Existence.class,
                adapter.adapt(itemExists, ws).orElseThrow(() -> new AssertionError(
                        "a then-bound item existence operand must admit since the #519"
                                + " cluster")));
        IRVariable itemOperand = assertInstanceOf(IRVariable.class, itemLowered.arg(),
                "the operand is the implicit item variable");
        assertEquals(IRVariable.VariableKind.USER_ITEM, itemOperand.variableKind(),
                "a WRITTEN `item` lowers USER_ITEM at the RImplicitVariable arm");
        assertEquals(Optional.empty(), adapter.declineReason(itemExists, ws),
                "empty <=> lowers: the operandItem face is REMOVED");

        // elidedOperand: `amounts then exists` — the without-left arm's null argument; the
        // synthesized SYNTHETIC_ITEM carries the then source's OWN type and cardinality (the
        // whole-piped-value binder semantics — number MULTI here, never a guessed type).
        RExistenceExpr elided = firstUnder(ws, "ThenElidedExists19", RExistenceExpr.class);
        assertNull(elided.argument(), "the fixture premise: the without-left arm is null-argument");
        Existence elidedLowered = assertInstanceOf(Existence.class,
                adapter.adapt(elided, ws).orElseThrow(() -> new AssertionError(
                        "an elided existence operand must SYNTHESIZE since the #519 teach")));
        IRVariable synthesized = assertInstanceOf(IRVariable.class, elidedLowered.arg(),
                "the operand is the synthesized implicit");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, synthesized.variableKind(),
                "an adapter-synthesized implicit is SYNTHETIC_ITEM (the synthesized-implicit"
                        + " convention; no RAW RImplicitVariable exists behind it)");
        assertFalse(synthesized.type() == null || synthesized.type().isMissing(),
                "the synthesis types from the pipe source's own cached type — never missing"
                        + " (the missing-source slice declines instead)");
        assertEquals(ExpressionCardinality.MULTI, synthesized.cardinality(),
                "a then binder is the WHOLE piped value — the source's own cardinality");
        assertEquals(elided.sourceRange(), synthesized.sourceRange(),
                "the synthesized node carries the synthesizing construct's own range (the"
                        + " documented collision convention — the #479 renderer indexes can"
                        + " never resolve it, so a drift arrival at a native arm declines)");
        assertEquals(Optional.empty(), adapter.declineReason(elided, ws),
                "empty <=> lowers: the elidedOperand face is REMOVED for the walkable slice");

        // operand:IRListConstruct: `[x, "B"] exists` — the list-literal operand admits by
        // kind; the routing is the twin-exact Existence-over-list-construct shape leg.
        RExistenceExpr listExists = firstUnder(ws, "ListConstructExists19", RExistenceExpr.class);
        Existence listLowered = assertInstanceOf(Existence.class,
                adapter.adapt(listExists, ws).orElseThrow(() -> new AssertionError(
                        "a list-literal existence operand must admit since the #519 cluster")));
        assertInstanceOf(IRListConstruct.class, listLowered.arg(),
                "the operand is the lowered list literal");
        assertEquals(Optional.empty(), adapter.declineReason(listExists, ws));

        // The NEGATIVE witness: a lowered nested-binary operand keeps declining — the cluster
        // admitted only the taught kinds (the licensing residue keeps its generic face).
        RExistenceExpr binExists = firstUnder(ws, "BinaryOperandExists19", RExistenceExpr.class);
        assertTrue(adapter.adapt(binExists, ws).isEmpty(),
                "a nested-binary existence operand stays declined — the admission set is"
                        + " exactly the taught cluster");
        assertEquals(Optional.of("operand:BinaryOp"), adapter.declineReason(binExists, ws),
                "the residue keeps its generic kind face");
    }

    @Test
    void dispatchArgLambdaAndLibraryClusterAdmitSinceThe520Teach() {
        // #520 (witness 1 — the composed dispatch/head + IRLambdaOp cluster): the LIBRARY
        // apply (calleeNotFunction 20 — the #519 calleeGate census read the class 100%
        // RLibraryFunction, 100% atRoot: the Min/Max/IsLeapYear builtins), the LOGICAL
        // lambda-op operand (operand:IRLambdaOp 10 — producesComparisonResult grows the
        // kind, the #518 equality-seat admission's sibling) and the NAV lambda-op receiver
        // (receiver:IRLambdaOp 9 — isNavigableReceiver) turn here; the dispatch-root argNav
        // admission (argNav.root:IRDispatchInputRef hop1 12 + hop2 6) is pinned DIRECTLY at
        // the package-private classifier with the census-narrow DEEP negative (the #491
        // belt-pin precedent — the corpus class is the #505 re-entrant synthesis, so the
        // parsed-path admission has no compact engine fixture; the census carries its live
        // proof, the engine-state-fixture law). Routing safety is the compiler's:
        // isCoercedBooleanOperand grows IRLambdaOp (twin-exact), the nav-over-lambda-op
        // containsOracleLeaf shape leg (NO native carve-out — the #511 IRListOp leg's
        // sibling; the leaf emitter's frame-slot-gated lambda arm is the drift choke
        // point), the IRLibraryApply kind screens (the #502 IRMetaOutputApply pattern
        // verbatim), and the STANDING kind walk for the dispatch root (no new leg — the
        // #510-D1/#514 oracle-leaf-root routing).
        String source = """
                namespace "test.step520a"

                library function Min(x number, y number) number

                type Leg20:
                    rate number (1..1)

                func UseMin20:
                    inputs:
                        a number (1..1)
                        b number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        Min(a, b)

                func LambdaLogical20:
                    inputs:
                        codes string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes exists and (codes filter [ item = "A" ])

                func LambdaNav20:
                    inputs:
                        legs Leg20 (0..*)
                    output:
                        out number (0..*)
                    set out:
                        legs filter [ item -> rate exists ] -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step520-cluster.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // calleeNotFunction → the IRLibraryApply mint: childless, the callee name the
        // neutral fact (the args are legacy's own — the #502 IRMetaOutputApply pattern).
        RSymbolReference minCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "Min".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no Min call parsed"));
        IRLibraryApply lib = assertInstanceOf(IRLibraryApply.class,
                adapter.adapt(minCall, ws).orElseThrow(() -> new AssertionError(
                        "an args-present library call must mint since the #520 arm")),
                "the library apply lowers to the childless shallow kind");
        assertEquals("Min", lib.calleeName(), "the neutral fact");
        assertTrue(lib.children().isEmpty(),
                "childless BY DESIGN — the args render legacy-side inside the serve");
        assertEquals(Optional.empty(), adapter.declineReason(minCall, ws),
                "empty <=> lowers: the calleeNotFunction face is REMOVED for the library"
                        + " class");

        // operand:IRLambdaOp at the LOGICAL seat: the filter composes as the coerced
        // operand (legacy LogicalHandler's ComparisonResult.ofNullSafe family — removing
        // the producesComparisonResult admission turns this red).
        RLogicalExpr logical = firstUnder(ws, "LambdaLogical20", RLogicalExpr.class);
        BinaryOp logRoot = assertInstanceOf(BinaryOp.class,
                adapter.adapt(logical, ws).orElseThrow(() -> new AssertionError(
                        "a logical over a lambda-op operand must compose since the #520"
                                + " arm")),
                "the logical root lowers whole");
        assertEquals(BinaryOp.BinOp.AND, logRoot.op());
        assertInstanceOf(IRLambdaOp.class, logRoot.right(),
                "the lambda-op operand composes on the RIGHT");
        assertEquals(Optional.empty(), adapter.declineReason(logical, ws),
                "empty <=> lowers: the logical operand:IRLambdaOp face is REMOVED");

        // receiver:IRLambdaOp at the NAV seat: `(legs filter […]) -> rate` builds the
        // FieldAccess over the lowered lambda op (removing the isNavigableReceiver
        // admission turns this red).
        RFeatureCall navOverFilter = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "rate".equals(fc.featureName())
                        && fc.receiver() instanceof RFilterExpr)
                .findFirst().orElseThrow(() -> new AssertionError("no nav-over-filter parsed"));
        FieldAccess navOverLambda = assertInstanceOf(FieldAccess.class,
                adapter.adapt(navOverFilter, ws).orElseThrow(() -> new AssertionError(
                        "a nav over a lambda-op receiver must compose since the #520 arm")),
                "the nav lowers over the admitted receiver");
        assertInstanceOf(IRLambdaOp.class, navOverLambda.receiver(),
                "the receiver is the lowered lambda op");
        assertEquals(Optional.empty(), adapter.declineReason(navOverFilter, ws),
                "empty <=> lowers: the receiver:IRLambdaOp face is REMOVED");

        // argNav.root:IRDispatchInputRef — the depth ladder pinned DIRECTLY (the #491
        // belt-pin precedent): hop1/hop2 ADMIT (null = admitted), the DEEP spelling keeps
        // its decline face (the census-narrow depth gate — hop1 12 + hop2 6 live at the
        // #519 SOT, deep ZERO; a flip widening the depth gate must turn the negative).
        RAttribute numberParam = AstWalker.findAll(ws.files().get(0), RFunction.class)
                .stream().filter(f -> "UseMin20".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no UseMin20 parsed"))
                .inputs().get(0);
        RMetaAnnotatedType cleanNumber = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRExpr dispatchRoot = new IRDispatchInputRef("pay", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        java.util.function.Function<IRExpr, FieldAccess> hopOver = recv ->
                new FieldAccess(recv, "amt", NodeId.ROOT, cleanNumber,
                        ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, SourceRange.NONE);
        FieldAccess hop1 = hopOver.apply(dispatchRoot);
        FieldAccess hop2 = hopOver.apply(hop1);
        FieldAccess hop3 = hopOver.apply(hop2);
        RExpression navRawArg = intLiteral(1); // non-item roots never consult the raw arg
        assertNull(ExpressionToIRAdapter.argNavFacet(hop1, navRawArg, numberParam, ws),
                "a hop1 dispatch-root nav arg ADMITS since the #520 teach (null = admitted)");
        assertNull(ExpressionToIRAdapter.argNavFacet(hop2, navRawArg, numberParam, ws),
                "a hop2 dispatch-root nav arg ADMITS (the second census-live depth)");
        assertEquals("root:IRDispatchInputRef.deep",
                ExpressionToIRAdapter.argNavFacet(hop3, navRawArg, numberParam, ws),
                "the DEEP spelling keeps its decline face — the census-narrow depth gate");
    }

    @Test
    void closureParamArgNavDepthLadderAdmitsSinceThe521Teach() {
        // #521 (witness 1 — the closure-param argNav admission): the #510-D1/#514/#520
        // oracle-leaf-root pattern's FOURTH application, at the SIXTH root kind (#510
        // admitted symbol+meta, #514 output+metaParam, #520 dispatch — the
        // application-tally unit of the #520 MF-1 recut) — IRClosureParam is a standing
        // kind-wide containsOracleLeaf entry since #502, so an apply carrying a
        // closure-param-rooted nav arg ALWAYS renders WHOLE through the oracle-root
        // callArgs serve (zero compiler changes — the standing kind walk routes). The
        // depth ladder is pinned DIRECTLY at the package-private classifier (the #491
        // belt-pin precedent, the #520 dispatch-ladder style): EVERY depth ADMITS — the
        // #511 IRListOp every-depth pattern, because the depth landscape proved
        // TEACH-SENSITIVE in the #521 probe loop (the base typing deepened the lowered
        // args: the #520-SOT hop1 20 + deep.multi 10 faces re-read as hop2 once the
        // outer hops could lower — a census-narrow depth gate would chase its own teach).
        String source = """
                namespace "test.step521b"

                func Ladder21:
                    inputs:
                        a number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        a
                """;
        RModel model = AstBuilder.buildFromString(source, "step521-ladder.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RAttribute numberParam = AstWalker.findAll(ws.files().get(0), RFunction.class)
                .stream().filter(f -> "Ladder21".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no Ladder21 parsed"))
                .inputs().get(0);
        RMetaAnnotatedType cleanNumber = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRExpr closureRoot = new IRClosureParam("p", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        java.util.function.BiFunction<IRExpr, ExpressionCardinality, FieldAccess> hopWith =
                (recv, card) -> new FieldAccess(recv, "amt", NodeId.ROOT, cleanNumber,
                        card, ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, SourceRange.NONE);
        FieldAccess cpHop1 = hopWith.apply(closureRoot, ExpressionCardinality.SINGLE);
        FieldAccess cpHop2 = hopWith.apply(cpHop1, ExpressionCardinality.SINGLE);
        FieldAccess cpHop3Multi = hopWith.apply(cpHop2, ExpressionCardinality.MULTI);
        RExpression cpRawArg = intLiteral(1); // non-item roots never consult the raw arg
        assertNull(ExpressionToIRAdapter.argNavFacet(cpHop1, cpRawArg, numberParam, ws),
                "a hop1 closure-param-root nav arg ADMITS since the #521 teach (null ="
                        + " admitted; pre-teach this exact shape WAS the"
                        + " argNav.root:IRClosureParam.hop1 face — 20 sole at the #520 SOT)");
        assertNull(ExpressionToIRAdapter.argNavFacet(cpHop2, cpRawArg, numberParam, ws),
                "a hop2 closure-param-root nav arg ADMITS (the depth the probe3 base"
                        + " typing EXPOSED — the #520-SOT hop1 drr-f faces re-read here"
                        + " once the outer hops could lower; the every-depth gate)");
        assertNull(ExpressionToIRAdapter.argNavFacet(cpHop3Multi, cpRawArg, numberParam, ws),
                "a DEEP MULTI closure-param-root nav arg ADMITS (deep.multi 10 sole drr-r"
                        + " at the #520 SOT — the every-depth gate's third live depth)");
    }

    @Test
    void elidedPipeSourceClusterProvesSinceThe521Teach() {
        // #521 (witness 2 — the elided-pipe source-walk widening, the #481/#485
        // continuation): FOUR sourceElementDataType legs turn — (A1) the RULE-reference
        // source (DRR's rule-chaining idiom `… then extract <rule>`: the element form
        // derives on the referenced rule's OWN body through the same walk — the #484
        // callable-output law at the RULE kind), (A1b) the CONSTRUCTOR source (the
        // element form IS the constructed data type — the dominant rule-chain bottom),
        // (A2) the unresolved-feature by-name source (the cdm6 navigate-by-type shape
        // `… -> pick -> <ChoiceOption>`: the name selects a choice option, resolvedFeature
        // structurally empty — the DIRECT-choice option-by-type-name read), and (A3) the
        // min/max element-preserving collapse (min picks ONE element of its argument).
        // Pre-teach these exact claims WERE the attrOutsideFunction.sourceElementUnresolved
        // src:RImplicitVariable.deep:*/src:RFeatureCall.unres/src:RMinExpr probed + L-113
        // belt faces (55 probed sole + 34 of the 40 belt at the #520 SOT). The render
        // route is UNCHANGED (the standing #480 equivalent through adaptFeatureCall's own
        // gates; the identity guard re-derives the outer attribute on the proven element
        // form, so a wrong derivation declines, never claims).

        // (A1+A1b) the rule-chain source: the bare ctor-seat attr proves through the
        // referenced rule's body — Inner21's extract bottoms at the Pay21 constructor.
        String ruleSource = """
                namespace "test.step521c"

                type Wrap21:
                    pay Pay21 (1..1)

                type Pay21:
                    kind string (1..1)
                    amt number (0..1)

                reporting rule Inner21 from Wrap21:
                    extract Pay21 { kind: "x", amt: empty }

                reporting rule Outer21 from Wrap21:
                    extract Inner21
                    then extract Pay21 { kind: kind, amt: empty }
                """;
        RModel ruleModel = AstBuilder.buildFromString(ruleSource, "step521-rulechain.rosetta");
        RWorkspace rws = RWorkspace.build(List.of(ruleModel)).workspace();
        RSymbolReference ctorSeatKind = AstWalker.findAll(rws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "kind".equals(r.name()) && r.args().isEmpty()
                        && r.symbol().orElse(null) instanceof RAttribute)
                .findFirst().orElseThrow(() -> new AssertionError("no bare kind ref parsed"));
        IRSynItemNav kindNav = assertInstanceOf(IRSynItemNav.class,
                adapter.adapt(ctorSeatKind, rws).orElseThrow(() -> new AssertionError(
                        "a rule-chain-sourced bare attr must adapt since the #521 legs")),
                "the bare attr lowers via the legacy-equivalent item navigation — the"
                        + " RULE-seam synthetic-item nav kind");
        assertEquals("kind", kindNav.featureName(),
                "the nav carries the attribute by name (the A1 recursion through Inner21's"
                        + " expression landing on the A1b constructor leg proved Pay21)");
        assertTrue(adapter.declineReason(ctorSeatKind, rws).isEmpty(),
                "empty <=> lowers: the src:RExtractExpr/deep:* face is REMOVED for the"
                        + " rule-chain class");

        // (A2) the DIRECT-choice by-name source: the filter-body bare attr proves through
        // the option-by-type-name read (resolvedFeature empty on the OptA21 step).
        String choiceSource = """
                namespace "test.step521d"

                choice Pick21:
                    OptA21
                    OptB21

                type OptA21:
                    flag boolean (1..1)

                type OptB21:
                    other string (1..1)

                type Holder21:
                    pick Pick21 (0..*)

                func UsePick21:
                    inputs:
                        holder Holder21 (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        holder -> pick -> OptA21
                            filter flag
                            then extract flag
                """;
        RModel choiceModel = AstBuilder.buildFromString(choiceSource, "step521-choice.rosetta");
        RWorkspace cws = RWorkspace.build(List.of(choiceModel)).workspace();
        RFeatureCall optionStep = AstWalker.findAll(cws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "OptA21".equals(fc.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError("no option step parsed"));
        assertTrue(optionStep.resolvedFeature().isEmpty(),
                "the fixture's premise: a choice-option step carries NO resolved feature —"
                        + " the src:RFeatureCall.unres class");
        RSymbolReference filterFlag = AstWalker.findAll(cws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "flag".equals(r.name()) && r.args().isEmpty()
                        && r.symbol().orElse(null) instanceof RAttribute)
                .findFirst().orElseThrow(() -> new AssertionError("no bare flag ref parsed"));
        assertTrue(adapter.adapt(filterFlag, cws).isPresent(),
                "a choice-option-sourced bare attr must adapt since the #521 by-name leg"
                        + " (pre-teach: attrOutsideFunction.sourceElementUnresolved"
                        + ".src:RFeatureCall.unres — 6 sole cdm6-f at the #520 SOT; the"
                        + " fixture's receiver is the disguised `a -> b` ENR head, the"
                        + " corpus's dominant shape)");
        assertTrue(adapter.declineReason(filterFlag, cws).isEmpty(),
                "empty <=> lowers: the unres face is REMOVED for the choice class");

        // (A3) the min element-preserving source: the then-extract bare attr proves on
        // the min's ARGUMENT element form (min picks ONE element — the FIRST/ONLY_ELEMENT
        // law at the ordering op).
        String minSource = """
                namespace "test.step521e"

                type Leg21:
                    qty number (1..1)

                func UseMin21:
                    inputs:
                        legs Leg21 (0..*)
                    output:
                        out number (0..*)
                    set out:
                        legs min [ qty ] then extract qty
                """;
        RModel minModel = AstBuilder.buildFromString(minSource, "step521-min.rosetta");
        RWorkspace mws = RWorkspace.build(List.of(minModel)).workspace();
        RThenExpr minThen = AstWalker.findFirst(mws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("no then parsed"));
        RSymbolReference pipedQty = AstWalker.findAll(minThen.body().orElseThrow(),
                        RSymbolReference.class)
                .stream().filter(r -> "qty".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no piped qty ref parsed"));
        assertTrue(adapter.adapt(pipedQty, mws).isPresent(),
                "a min-piped bare attr must adapt since the #521 element-preserving leg"
                        + " (pre-teach: the src:RMinExpr belt face — 2 at the #520 SOT)");
        assertTrue(adapter.declineReason(pipedQty, mws).isEmpty(),
                "empty <=> lowers: the min face is REMOVED");
    }

    @Test
    void argNavOracleLeafRootSweepAdmitsEveryDepthSinceThe522Teach() {
        // #522 (witness 1 — arm C, the standing-root depth-gate + new-root argNav sweep):
        // the #510-D1/#514/#520/#521 oracle-leaf-root pattern's FIFTH application. The
        // standing #510 IRSymbolNav/IRMetaAccess hop1-only census gates WIDEN to every
        // depth (the live deeper faces at the #521 SOT: root:IRMetaAccess.hop2 9 +
        // .hop2.multi 4 + .deep 1 · root:IRSymbolNav.deep 7), and the four remaining
        // kind-wide containsOracleLeaf chain roots ADMIT at every depth (the SEVENTH
        // through TENTH root kinds — IRSynItemNav #504 · IRChoiceOptionNav #505 ·
        // IRDeepFeatureNav #507 · IRMetaItemNav #508; the live faces:
        // root:IRDeepFeatureNav.hop1.multi 12 · root:IRChoiceOptionNav.hop1 3 ·
        // root:IRSynItemNav.hop1.multi 3 + .hop2 1 + .hop2.multi 1 ·
        // root:IRMetaItemNav.hop2 1). The serve is depth-blind (the oracle-root callArgs
        // line at any hop count) and the co-landing #522 arm-B lowerings can root exactly
        // these kinds — the #521 teach-sensitive-depth law. The ladder is pinned DIRECTLY
        // at the package-private classifier (the #491 belt-pin / #520-#521 ladder style);
        // the census-narrow NEIGHBOR gates stay UNWIDENED (the #510/#514 conservatism —
        // the negative pins the sweep's exact boundary).
        String source = """
                namespace "test.step522c"

                func Sweep22:
                    inputs:
                        a number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        a
                """;
        RModel model = AstBuilder.buildFromString(source, "step522-sweep.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RAttribute numberParam = AstWalker.findAll(ws.files().get(0), RFunction.class)
                .stream().filter(f -> "Sweep22".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no Sweep22 parsed"))
                .inputs().get(0);
        RMetaAnnotatedType cleanNumber = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        java.util.function.BiFunction<IRExpr, ExpressionCardinality, FieldAccess> hopWith =
                (recv, card) -> new FieldAccess(recv, "amt", NodeId.ROOT, cleanNumber,
                        card, ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, SourceRange.NONE);
        java.util.function.Function<IRExpr, FieldAccess> hop = recv ->
                hopWith.apply(recv, ExpressionCardinality.SINGLE);
        RExpression rawArg = intLiteral(1); // non-item roots never consult the raw arg
        IRVariable inner = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                cleanNumber, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                SourceRange.NONE);

        // The WIDENED standing roots — the live deeper spellings ADMIT (pre-teach these
        // exact shapes WERE the root:IRMetaAccess.hop2[.multi]/.deep and
        // root:IRSymbolNav.deep faces).
        IRExpr metaRoot = new IRMetaAccess(inner, "id", List.of(), NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(hop.apply(hop.apply(metaRoot)),
                        rawArg, numberParam, ws),
                "a hop2 meta-access-root nav arg ADMITS since the #522 sweep (null = admitted)");
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hopWith.apply(hop.apply(metaRoot), ExpressionCardinality.MULTI),
                        rawArg, numberParam, ws),
                "a hop2 MULTI meta-access-root nav arg ADMITS (the .hop2.multi face)");
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hop.apply(hop.apply(hop.apply(metaRoot))), rawArg, numberParam, ws),
                "a DEEP meta-access-root nav arg ADMITS (the .deep face)");
        IRExpr symRoot = new IRSymbolNav("func", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hop.apply(hop.apply(hop.apply(symRoot))), rawArg, numberParam, ws),
                "a DEEP symbol-nav-root nav arg ADMITS (the root:IRSymbolNav.deep face — 7"
                        + " sole drr-r at the #521 SOT)");

        // The FOUR new roots — every depth ADMITS (hop1/hop2 the live faces; deep the
        // teach-sensitive headroom).
        IRExpr choiceRoot = new IRChoiceOptionNav("payout", "OptA22", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(hop.apply(choiceRoot), rawArg,
                        numberParam, ws),
                "a hop1 choice-option-root nav arg ADMITS (root:IRChoiceOptionNav.hop1 — 3"
                        + " sole cdm6-f at the #521 SOT)");
        IRExpr synRoot = new IRSynItemNav("leg", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hopWith.apply(synRoot, ExpressionCardinality.MULTI), rawArg,
                        numberParam, ws),
                "a hop1 MULTI syn-item-root nav arg ADMITS (root:IRSynItemNav.hop1.multi)");
        assertNull(ExpressionToIRAdapter.argNavFacet(hop.apply(hop.apply(synRoot)), rawArg,
                        numberParam, ws),
                "a hop2 syn-item-root nav arg ADMITS (root:IRSynItemNav.hop2)");
        IRExpr metaItemRoot = new IRMetaItemNav("obs", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(hop.apply(hop.apply(metaItemRoot)),
                        rawArg, numberParam, ws),
                "a hop2 meta-item-root nav arg ADMITS (root:IRMetaItemNav.hop2)");
        IRExpr deepRoot = new IRDeepFeatureNav(inner, "product", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hopWith.apply(deepRoot, ExpressionCardinality.MULTI), rawArg,
                        numberParam, ws),
                "a hop1 MULTI deep-feature-root nav arg ADMITS"
                        + " (root:IRDeepFeatureNav.hop1.multi — 12 sole cdm6-f, the sweep's"
                        + " largest face)");
        assertNull(ExpressionToIRAdapter.argNavFacet(
                        hop.apply(hop.apply(hop.apply(deepRoot))), rawArg, numberParam, ws),
                "a DEEP deep-feature-root nav arg ADMITS (the every-depth gate — the"
                        + " #511/#521 pattern)");

        // The BOUNDARY negative: the census-narrow NEIGHBOR gates stay UNWIDENED — an
        // output-ref root beyond hop1 keeps its decline face (the #514 conservatism; a
        // future widening must turn this pin).
        IRExpr outputRoot = new IROutputRef("out", NodeId.ROOT, cleanNumber,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("root:IROutputRef.hop2",
                ExpressionToIRAdapter.argNavFacet(hop.apply(hop.apply(outputRoot)), rawArg,
                        numberParam, ws),
                "the sweep widened EXACTLY the six oracle-leaf roots — the hop1-only"
                        + " neighbors keep their census-narrow gates");
    }

    @Test
    void guardedItemArgClusterAdmitsSinceThe523Teach() {
        // #523 (witness 1 — the guard co-design's ADAPTER half): the argNav
        // itemChain./itemSrcUnprovable. decline mirror RETIRES for the proven raw-HEAD
        // classes — an item/synItem-rooted call arg admits when its raw head is an
        // RImplicitVariable (any depth — the compiler's standing raw-side recognizer trips
        // multi-hop and the #523 claim-root leg serves whole-legacy), an REnumValueRef
        // single-arrow disguise (any depth — the guard's #523 call-branch probe trips via
        // legacy's OWN synthesizeImplicitItemChain), or a hop1 bare symbol (the #479/#480
        // proven native). Pre-teach these exact shapes WERE the argNav.itemChain.* 50 +
        // argNav.itemSrcUnprovable.* 35 faces (85 sole at the #522 SOT).
        String source = """
                namespace "test.step523"

                type Sub23:
                    x number (1..1)

                type Leg23:
                    rate number (1..1)
                    sub Sub23 (1..1)

                type Trade23:
                    legs Leg23 (0..*)

                func CChainItem23:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CDisguised23:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func FnItemChain23:
                    inputs:
                        trade Trade23 (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CChainItem23(item -> sub -> x)

                func FnDisguised23:
                    inputs:
                        trade Trade23 (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CDisguised23(sub -> x)
                """;
        RModel model = AstBuilder.buildFromString(source, "step523-itemarg.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // Leg 1 — the EXPLICIT chained item arg (the former itemChain.item.hop2 spelling —
        // the `ExtractRegimeInformation(item, item -> reportingSide -> reportingParty)`
        // corpus idiom): the call lowers WHOLE, the arg the 2-hop FieldAccess chain over
        // the USER item.
        assertNavArgClaim(ws, "CChainItem23", IRVariable.VariableKind.USER_ITEM, 2);

        // Leg 2 — the DISGUISED single-arrow arg (the former itemSrcUnprovable.synItem.hop2
        // spelling — the drr-r `InitialMargin…(item, reportingSide -> reportingParty)`
        // idiom): `sub -> x` parses as the REnumValueRef disguise (the single-arrow law),
        // the adapter's ladder resolves it item-headed and lowers the 2-hop chain over the
        // SYNTHETIC item; the head class admits at the arg gate.
        assertNavArgClaim(ws, "CDisguised23", IRVariable.VariableKind.SYNTHETIC_ITEM, 2);

        // Leg 3 — the residue drift face (the itemRawHead. stable token): a multi-hop
        // item-rooted lowering whose raw head is NEITHER an implicit var, a disguise, nor
        // a hop1 bare symbol is structurally unreachable through adapt (the parser's
        // single-arrow law makes every multi-name head an REnumValueRef), so the pin
        // hand-constructs the lowered triple with a bare-symbol raw at hop2 and calls the
        // package-private classifier directly (the #491 belt-pin pattern).
        RMetaAnnotatedType cleanNumber523 = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRVariable synItem523 = new IRVariable("item", IRVariable.VariableKind.SYNTHETIC_ITEM,
                NodeId.ROOT, cleanNumber523, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                SourceRange.NONE);
        FieldAccess symHeadedHop2 = new FieldAccess(
                new FieldAccess(synItem523, "a", NodeId.ROOT, cleanNumber523,
                        ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE,
                        Optionality.PRESENT, SourceRange.NONE),
                "b", NodeId.ROOT, cleanNumber523, ExpressionCardinality.SINGLE,
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        RSymbolReference bareSymRaw = new RSymbolReference();
        bareSymRaw.setName("a");
        assertEquals("itemRawHead.synItem.hop2",
                ExpressionToIRAdapter.argNavFacet(symHeadedHop2, bareSymRaw,
                        scalarParam("q"), ws),
                "a hop2 lowering over a BARE-SYMBOL raw head keeps a decline face — the"
                        + " #523 admission widened EXACTLY the proven head classes");
    }

    @Test
    void switchBoundItemNavAdmitsSinceThe523Teach() {
        // #523 (witness 2 — arm B, the switch-case binding admission): a lambda-less
        // implicit item whose nearest binding boundary is a TYPE-guarded switch case
        // admits at the receiver gate — legacy's #221 switchChoiceHoist re-root lives
        // INSIDE handle(RImplicitVariable) (the cast case var off the live scope), which
        // the emitter's ImplicitItemRenderer reuses VERBATIM, so the exclusion was
        // admission lag exactly like the #506 then/min/max/sort legs. Pre-teach these
        // claims WERE the RFeatureCall:itemNotFilterExtractBound 10 (the cdm6
        // CheckCriteria carrier — the walkBindGate itemBind.switchCrossed.leafPlain rows,
        // 5 atRoot + 5 interior). The switchCaseOptionMetaSuspect belt (a meta-annotated
        // choice option would make the item's Java value a wrapper the plain guard type
        // cannot see) carries census-only proof — the corpus has ZERO meta-optioned
        // choices (the #521 T3/RMax precedent for zero-population belts).
        String source = """
                namespace "test.step523b"

                type OptA23:
                    fieldA string (1..1)

                type OptB23:
                    fieldB string (1..1)

                choice Crit23:
                    OptA23
                    OptB23

                func CheckA23:
                    inputs:
                        s string (0..1)
                    output:
                        out boolean (1..1)
                    set out: True

                func Dispatch23:
                    inputs:
                        crit Crit23 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        crit switch
                            OptA23 then CheckA23(item -> fieldA),
                            default False
                """;
        RModel model = AstBuilder.buildFromString(source, "step523-switch.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // The type-guarded case's item nav LOWERS (the receiver gate's switch leg) and the
        // containing call claims WHOLE — pre-teach the nav's receiver minted
        // itemNotFilterExtractBound and the claim declined.
        RSymbolReference checkCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "CheckA23".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no CheckA23 call parsed"));
        assertEquals(Optional.empty(), adapter.declineReason(checkCall, ws),
                "the switch-case call claim lowers whole since the #523 arm-B admission"
                        + " (the mirror law)");
        IRApply loweredCall = assertInstanceOf(IRApply.class, adapt(checkCall, ws),
                "the call lowers to IRApply");
        FieldAccess caseNav = assertInstanceOf(FieldAccess.class, loweredCall.args().get(0),
                "the argument is the lowered item nav");
        IRVariable caseItem = assertInstanceOf(IRVariable.class, caseNav.receiver(),
                "the nav roots on the switch-bound item");
        assertEquals(IRVariable.VariableKind.USER_ITEM, caseItem.variableKind(),
                "the explicit `item` keyword lowers as the USER item");

        // The boundary negative: the DEFAULT case has no guard, so the switch leg proves
        // no narrowing — an item nav there keeps declining (the prove-or-decline walk;
        // a future guard-less admission must turn this pin).
        String negSource = """
                namespace "test.step523c"

                type OptC23:
                    fieldC string (1..1)

                choice CritC23:
                    OptC23

                func DispatchNeg23:
                    inputs:
                        crit CritC23 (1..1)
                    output:
                        out string (1..1)
                    set out:
                        crit switch
                            default item -> fieldC
                """;
        RModel negModel = AstBuilder.buildFromString(negSource, "step523-switchneg.rosetta");
        RWorkspace negWs = RWorkspace.build(List.of(negModel)).workspace();
        RFeatureCall negNav = AstWalker.findAll(negWs.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "fieldC".equals(fc.featureName()))
                .findFirst().orElseThrow(() -> new AssertionError("no default-case nav parsed"));
        assertTrue(adapter.declineReason(negNav, negWs).isPresent(),
                "a DEFAULT-case item nav stays declined — the switch leg admits only a"
                        + " TYPE-guarded case's proven narrowing");
    }

    @Test
    void nonAttrBareItemNavClusterProvesSinceThe522Teach() {
        // #522 (witness 2 — arm B, the BY-NAME implicit-item bare-nav admission): a bare
        // name the linker bound to a GLOBAL non-attribute declaration lowers as the
        // enclosing binder's implicit-ITEM feature read — legacy
        // synthesizeImplicitItemBareNav's ladder mirrored prove-or-decline, the
        // adaptBareAttrItemNav equivalent-adapt at the BY-NAME seat. Pre-teach these exact
        // claims WERE the symbolNotAttribute.sym:RDataType.p:RExistenceExpr 29 /
        // sym:RAnnotation.p:REqualityExpr 19 / sym:RSegmentDef.p:RKeyValuePair 5 faces
        // (61 sole at the #521 SOT).

        // (a) the DIRECT-choice bare option read (the cdm6 navigate-by-type class —
        // `payouts extract [ PerformancePayout exists ]`): the bare name selects a choice
        // option by TYPE NAME on the binder's element form.
        String choiceSource = """
                namespace "test.step522a"

                choice Pick22:
                    OptA22
                    OptB22

                type OptA22:
                    flag boolean (1..1)

                type OptB22:
                    other string (1..1)

                func UsePick22:
                    inputs:
                        picks Pick22 (0..*)
                    output:
                        out boolean (0..*)
                    set out:
                        picks extract [ OptA22 exists ]

                func TopLevel22:
                    inputs:
                        picks Pick22 (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        OptA22 exists
                """;
        RModel choiceModel = AstBuilder.buildFromString(choiceSource, "step522-choice.rosetta");
        RWorkspace cws = RWorkspace.build(List.of(choiceModel)).workspace();
        RSymbolReference bareOption = AstWalker.findAll(cws.files().get(0), RFunction.class)
                .stream().filter(f -> "UsePick22".equals(f.name()))
                .flatMap(f -> AstWalker.findAll(f, RSymbolReference.class).stream())
                .filter(r -> "OptA22".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no bare OptA22 parsed"));
        assertInstanceOf(RDataType.class, bareOption.symbol().orElse(null),
                "the fixture's premise: the linker binds the bare option name to the GLOBAL"
                        + " TYPE (the corpus face's sym:RDataType spelling)");
        FieldAccess optionNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(bareOption, cws).orElseThrow(() -> new AssertionError(
                        "a direct-choice bare option must adapt since the #522 arm-B")),
                "the bare option lowers via the legacy-equivalent item navigation — the"
                        + " synthetic-item hop (the bound twin's FieldAccess shape; the"
                        + " render is the #479/#480 proven synthetic-item compose)");
        assertEquals("OptA22", optionNav.feature(),
                "the nav carries the option by TYPE NAME (the projection's attribute name)");
        IRVariable optionRecv = assertInstanceOf(IRVariable.class, optionNav.receiver(),
                "the receiver is the synthetic implicit item");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, optionRecv.variableKind(),
                "the receiver variable is the SYNTHETIC_ITEM kind — the #480 range-correlated"
                        + " renderer's contract");
        assertFalse(optionNav.type() == null || optionNav.type().isMissing(),
                "the #522 type channel: the PROVEN option element types the mint (the"
                        + " attached-original read — the #521 elem-proven-typing pattern)");
        assertTrue(adapter.declineReason(bareOption, cws).isEmpty(),
                "empty <=> lowers: the sym:RDataType.p:RExistenceExpr face is REMOVED for"
                        + " the binder class");

        // The OUTSIDE-binder negative: the same bare name at claim TOP LEVEL has no
        // implicit item — legacy falls to the variable path, the arm declines, the face
        // STAYS (prove-or-decline; the boundary pin).
        RSymbolReference topLevel = AstWalker.findAll(cws.files().get(0), RFunction.class)
                .stream().filter(f -> "TopLevel22".equals(f.name()))
                .flatMap(f -> AstWalker.findAll(f, RSymbolReference.class).stream())
                .filter(r -> "OptA22".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no top-level OptA22 parsed"));
        assertEquals(Optional.of("symbolNotAttribute.sym:RDataType.p:RExistenceExpr"),
                adapter.declineReason(topLevel, cws),
                "outside every binder the face STAYS — the arm is binder-scoped exactly"
                        + " like legacy's ladder");

        // (b) the annotation-shadowed rule-filter read (the drr class — `then filter
        // qualification = …`): the bare name is the piped item's OWN attribute, shadowed
        // in the linker by a same-named global annotation.
        String annotationSource = """
                namespace "test.step522b"

                annotation qual22: <"the fixture annotation shadowing the attribute name">

                type Stamp22:
                    qual22 string (0..1)

                type Wrap22:
                    stamps Stamp22 (0..*)

                reporting rule Filt22 from Wrap22:
                    extract stamps
                    then filter qual22 = "x"
                """;
        RModel annotationModel = AstBuilder.buildFromString(annotationSource,
                "step522-annotation.rosetta");
        RWorkspace aws = RWorkspace.build(List.of(annotationModel)).workspace();
        RSymbolReference bareQual = AstWalker.findAll(aws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "qual22".equals(r.name()) && r.args().isEmpty()
                        && r.parent() instanceof REqualityExpr)
                .findFirst().orElseThrow(() -> new AssertionError("no bare qual22 parsed"));
        // The premise MOVED at v3.1 C1 part 2 (PR #567, MF-2): pass 5 still
        // binds the same-named global annotation, but an RAnnotation is not an
        // upstream RosettaSymbol, so the Cat-9 implicit-item arm now RE-BINDS
        // the piped item's OWN attribute — upstream's answer (R6: the filter
        // removes every non-symbol; probes P3/P7 pin the class). The #204
        // sym:RAnnotation mis-bind this fixture used to document is HEALED at
        // the linker level; the adapter's arm-B read below is now downstream
        // of a correct binding rather than recovering from a wrong one.
        assertInstanceOf(com.regnosys.rosetta.ast.supporting.RAttribute.class,
                bareQual.symbol().orElse(null),
                "the premise since PR #567 (MF-2): the item's OWN attribute wins the bare"
                        + " name — the annotation mis-bind is healed at the linker");
        IRExpr qualNav = adapter.adapt(bareQual, aws).orElseThrow(() -> new AssertionError(
                "an annotation-shadowed item attribute must adapt since the #522 arm-B"));
        String qualFeature = qualNav instanceof IRSynItemNav syn ? syn.featureName()
                : qualNav instanceof FieldAccess fa ? fa.feature()
                : qualNav instanceof IRMetaAccess ma ? ma.feature() : null;
        assertEquals("qual22", qualFeature,
                "the lowered nav carries the item's OWN attribute by name (one of the"
                        + " bound twin's accepted shapes)");
        assertTrue(adapter.declineReason(bareQual, aws).isEmpty(),
                "empty <=> lowers: the sym:RAnnotation.p:REqualityExpr face is REMOVED");

        // (c) the segment-shadowed constructor value read (the drr GetOthrPmt class —
        // `pmtDt: date` with `segment date` in scope): the bare name at a constructor
        // VALUE seat is the extract item's OWN attribute.
        String segmentSource = """
                namespace "test.step522d"

                segment seg22

                type Src22:
                    seg22 string (0..1)

                type Out22:
                    v string (0..1)

                func Build22:
                    inputs:
                        srcs Src22 (0..*)
                    output:
                        out Out22 (0..*)
                    add out:
                        srcs extract Out22 { v: seg22 }
                """;
        RModel segmentModel = AstBuilder.buildFromString(segmentSource, "step522-segment.rosetta");
        RWorkspace sws = RWorkspace.build(List.of(segmentModel)).workspace();
        RSymbolReference bareSeg = AstWalker.findAll(sws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "seg22".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no bare seg22 parsed"));
        // The premise MOVED at v3.1 C1 part 2 (PR #567, MF-2): an RSegmentDef
        // is one of the R-1 eight non-symbols the upstream whitelist excludes,
        // so the Cat-9 arm now re-binds the extract item's OWN attribute over
        // the pass-5 segment bind — the same heal as the annotation case above.
        assertInstanceOf(com.regnosys.rosetta.ast.supporting.RAttribute.class,
                bareSeg.symbol().orElse(null),
                "the premise since PR #567 (MF-2): the item's OWN attribute wins the bare"
                        + " name — the segment mis-bind is healed at the linker");
        IRExpr segNav = adapter.adapt(bareSeg, sws).orElseThrow(() -> new AssertionError(
                "a segment-shadowed item attribute must adapt since the #522 arm-B"));
        String segFeature = segNav instanceof IRSynItemNav syn ? syn.featureName()
                : segNav instanceof FieldAccess fa ? fa.feature()
                : segNav instanceof IRMetaAccess ma ? ma.feature() : null;
        assertEquals("seg22", segFeature,
                "the constructor-value bare name lowers as the item attribute read");
        assertTrue(adapter.declineReason(bareSeg, sws).isEmpty(),
                "empty <=> lowers: the sym:RSegmentDef.p:RKeyValuePair face is REMOVED");
    }

    @Test
    void aliasBodySymbolChannelTypesSinceThe518Teach() {
        // #518 (witness 2 — arm B1, the alias-operand SYMBOL-channel typing): an
        // RSymbolReference-shaped shortcut body the expression channel leaves untyped types
        // from its referenced symbol's own channel (an attribute's inferred type; a resolved
        // callee's OUTPUT type), and the standing #491 alias-operand admission carries the
        // claim. The byte-story is UNCHANGED: emitAlias stamps a null expression type
        // whatever the node carries, and the #326 META-signature subset still trips the
        // compiler-side postPin guard on legacy's own lever.
        String source = """
                namespace "test.step518b"

                type Price18:
                    value number (1..1)

                func CheckOk18:
                    inputs:
                        p Price18 (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        True

                func FnSym18:
                    inputs:
                        observation Price18 (1..1)
                        flag boolean (1..1)
                    output:
                        out boolean (1..1)
                    alias isOk: CheckOk18(observation)
                    set out:
                        isOk = flag
                """;
        RModel model = AstBuilder.buildFromString(source, "step518-symchannel.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RFunction fnSym = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "FnSym18".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no FnSym18"));

        // The PARSED leg: the call-bodied alias (`alias isOk: CheckOk18(observation)` — the
        // cdm IsLeapYear/ExtractTradePurchasePrice class) admits as an equality operand.
        // Whichever channel types the body in THIS engine build (the expression channel if
        // the fixture inference visits the call, else the #518 symbol channel off the
        // callee's OUTPUT), the admission and the lowered node's carried type must hold.
        REqualityExpr parsedEq = firstUnder(ws, "FnSym18", REqualityExpr.class);
        BinaryOp parsedLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(parsedEq, ws).orElseThrow(() -> new AssertionError(
                        "a call-bodied alias equality operand must admit since the #518"
                                + " symbol-channel teach")));
        IRReference parsedAlias = assertInstanceOf(IRReference.class, parsedLowered.left(),
                "the left operand lowers as the ALIAS reference");
        assertEquals(IRReference.ReferenceKind.ALIAS, parsedAlias.referenceKind());
        assertNotNull(parsedAlias.type(), "the alias node carries a resolved type");
        assertFalse(parsedAlias.type().isMissing(),
                "the body types through the expression OR the #518 symbol channel");
        assertEquals(Optional.empty(), adapter.declineReason(parsedEq, ws));

        // The CACHE-COLD leg (the symbol channel pinned BY CONSTRUCTION): a hand-built
        // shortcut whose body is a fresh RSymbolReference linker-bound to the parsed
        // `observation` input — the hand-built body never entered the engine cache, so the
        // expression channel is certainly MISSING and only the #518 symbol channel
        // (getInferredAttributeType on the referenced attribute) can type it.
        RAttribute observationAttr = fnSym.inputs().stream()
                .filter(a -> "observation".equals(a.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no observation input parsed"));
        RAttribute flagAttr = fnSym.inputs().stream()
                .filter(a -> "flag".equals(a.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no flag input parsed"));
        RShortcut coldAlias = new RShortcut();
        coldAlias.setName("coldAlias");
        RSymbolReference coldBody = new RSymbolReference();
        coldBody.setName("observation");
        coldBody.setResolvedSymbol(observationAttr);
        coldAlias.setExpression(coldBody);
        coldBody.setParent(coldAlias);
        fnSym.shortcuts().add(coldAlias);
        coldAlias.setParent(fnSym);
        RSymbolReference coldRef = new RSymbolReference();
        coldRef.setName("coldAlias");
        coldRef.setResolvedSymbol(coldAlias);
        RSymbolReference flagRef = new RSymbolReference();
        flagRef.setName("flag");
        flagRef.setResolvedSymbol(flagAttr);
        REqualityExpr coldEq = new REqualityExpr();
        coldEq.setOp(EqOp.EQ);
        coldEq.setLeft(coldRef);
        coldEq.setRight(flagRef);
        coldRef.setParent(coldEq);
        flagRef.setParent(coldEq);
        coldEq.setParent(fnSym);
        coldEq.attachToWorkspace(ws);
        BinaryOp coldLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(coldEq, ws).orElseThrow(() -> new AssertionError(
                        "a cache-cold bare-input alias body must admit through the #518"
                                + " symbol channel — the expression channel cannot type a"
                                + " hand-built node")),
                "the cold equality lowers whole");
        IRReference coldAliasOperand = assertInstanceOf(IRReference.class, coldLowered.left());
        assertEquals(IRReference.ReferenceKind.ALIAS, coldAliasOperand.referenceKind());
        assertNotNull(coldAliasOperand.type(),
                "the SYMBOL channel types the node (the referenced attribute's own type)");
        assertFalse(coldAliasOperand.type().isMissing(),
                "the symbol-channel type is the admission oracle — missing would decline");
        assertFalse(coldAliasOperand.type().hasMeta(),
                "the fixture attribute is meta-free — the bodyMeta face stays a decline");

        // The NEGATIVE witness (the transitive-output guard — byte-critical): an
        // OUTPUT-referencing body renders the BUILDER-threaded aliasName(result, inputs…)
        // invocation (legacy FunctionAliasHelper threads the output builder as the FIRST
        // param), never the bare aliasName(inputs) line emitAlias produces — the
        // symbol-channel walk must decline the class up front, keeping the bodyMissing
        // face (the token this teach otherwise removes — the decline-lock uniqueness law).
        RShortcut outBodied = new RShortcut();
        outBodied.setName("outBodied");
        RSymbolReference outBodyRef = new RSymbolReference();
        outBodyRef.setName("out");
        outBodied.setExpression(outBodyRef);
        outBodyRef.setParent(outBodied);
        fnSym.shortcuts().add(outBodied);
        outBodied.setParent(fnSym);
        RSymbolReference outBodiedRef = new RSymbolReference();
        outBodiedRef.setName("outBodied");
        outBodiedRef.setResolvedSymbol(outBodied);
        RSymbolReference flagRef2 = new RSymbolReference();
        flagRef2.setName("flag");
        flagRef2.setResolvedSymbol(flagAttr);
        REqualityExpr outEq = new REqualityExpr();
        outEq.setOp(EqOp.EQ);
        outEq.setLeft(outBodiedRef);
        outEq.setRight(flagRef2);
        outBodiedRef.setParent(outEq);
        flagRef2.setParent(outEq);
        outEq.setParent(fnSym);
        outEq.attachToWorkspace(ws);
        assertTrue(adapter.adapt(outEq, ws).isEmpty(),
                "an output-referencing alias body stays declined — the builder-threaded"
                        + " render class the bare alias invocation cannot reproduce");
        assertEquals(
                Optional.of("operandAlias.bodyMissing.body:RSymbolReference.guardOutput"),
                adapter.declineReason(outEq, ws),
                "the guard keeps the honest bodyMissing face, named at its own key");
    }

    @Test
    void dispatchInputNavAndSchemeHopClaimSinceThe517BeltTeaches() {
        // #517 (witness 2 — arms B1/B2, the L-111 belt teaches): a variant-body
        // `<base-input> -> <feature>` disguise lowers over the #505 IRDispatchInputRef
        // oracle leaf — the plain hop through the #514-1b acceptance leg (arm-B1: the
        // offShape.fa:IRDispatchInputRef face, the belt's largest blocked class) and the
        // META-annotated hop through the #508 arm-A1 leg widened to the dispatch receiver
        // (arm-B2: the metaFeature.q:scheme residue). Both faces become DEFENSIVE BELTS
        // (census-zero); the render routing is the standing law — at the L-111 root seat
        // the #507 identity-serve leg renders the literal super.visitEnumValueRef line,
        // byte-identical BY IDENTITY (the compiler witness pins it at unit speed).
        String source = """
                namespace "test.step517b"

                type Rates17:
                    rate number (1..1)
                    code string (1..1)
                        [metadata scheme]

                enum Proc17Enum:
                    A
                    B

                func Proc17:
                    inputs:
                        rates Rates17 (1..1)
                        which Proc17Enum (1..1)
                    output:
                        out number (1..1)

                func Proc17(which: Proc17Enum -> A):
                    set out:
                        rates -> rate

                func Proc17Code:
                    inputs:
                        rates Rates17 (1..1)
                        which Proc17Enum (1..1)
                    output:
                        outS string (1..1)

                func Proc17Code(which: Proc17Enum -> B):
                    set outS:
                        rates -> code
                """;
        RModel model = AstBuilder.buildFromString(source, "step517-dispatch.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // Arm-B1: the PLAIN variant-body hop lowers to FieldAccess over the dispatch leaf,
        // re-ranged link-by-link (the raw node's range on both nodes — the correlation key).
        REnumValueRef plainDisguise = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream()
                .filter(e -> "rates".equals(e.enumName()) && "rate".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no disguised `rates -> rate` chain in the dispatch fixture"));
        FieldAccess plainHop = assertInstanceOf(FieldAccess.class,
                adapter.adapt(plainDisguise, ws).orElseThrow(() -> new AssertionError(
                        "a dispatch-head disguise must claim via the #517 arm-B1 leg")),
                "the disguise lowers to FieldAccess over the leaf");
        IRDispatchInputRef plainBase = assertInstanceOf(IRDispatchInputRef.class,
                plainHop.receiver(),
                "the base is the #505 oracle leaf — every containing root oracle-serves");
        assertEquals("rates", plainBase.inputName());
        assertEquals(plainDisguise.sourceRange(), plainBase.sourceRange(),
                "the base re-stamps the RAW node's range — the correlation key");
        assertEquals(Optional.empty(), adapter.declineReason(plainDisguise, ws),
                "empty <=> lowers: the offShape.fa:IRDispatchInputRef face is REMOVED (the"
                        + " flip's negative witness)");

        // Arm-B2: the META-annotated variant-body hop lowers to IRMetaAccess over the SAME
        // dispatch leaf (the scheme qualifier read from the feature's annotation).
        REnumValueRef metaDisguise = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream()
                .filter(e -> "rates".equals(e.enumName()) && "code".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no disguised `rates -> code` chain in the dispatch fixture"));
        IRMetaAccess metaHop = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(metaDisguise, ws).orElseThrow(() -> new AssertionError(
                        "a dispatch-head META hop must claim via the #517 arm-B2 widening")),
                "the meta hop lowers to IRMetaAccess over the dispatch leaf");
        IRDispatchInputRef metaBase = assertInstanceOf(IRDispatchInputRef.class,
                metaHop.receiver(),
                "the receiver is the #505 oracle leaf — the meta routing law serves the root");
        assertEquals("rates", metaBase.inputName());
        assertEquals(metaDisguise.sourceRange(), metaBase.sourceRange(),
                "the receiver re-stamps the RAW node's range");
        assertTrue(metaHop.metaQualifiers().contains("scheme"),
                "the scheme qualifier rides from the feature's own annotation");
        assertEquals(Optional.empty(), adapter.declineReason(metaDisguise, ws),
                "empty <=> lowers: the inputFeatureNav.metaFeature face is REMOVED at this"
                        + " seat (the #508 IRVariable acceptance + the #517 dispatch widening"
                        + " make it a two-kind defensive belt)");
    }

    @Test
    void emptySymbolBareItemNavClusterProvesSinceThe524Teach() {
        // #524 (witness 1 — the EMPTY-symbol bare-item admission): legacy's dispatch
        // admits a symbol-EMPTY bare name into the SAME synthesizeImplicitItemBareNav
        // ladder (the handle(RSymbolReference) gate lists the empty symbol FIRST — the
        // #506 admission-lag law at the last unmirrored member). Pre-teach these exact
        // claims WERE the symbolUnresolved.synthetic.itemAttr[Meta] 32 +
        // lambdaSrcUnprovable.global.type 13 + synthetic.lambda.global.type 4 faces
        // (the #523 SOT; the #524 iaGate census read every one lgFires BY CALL). The
        // corpus mechanism is the render-time SYNTHESIZED disguised-nav receiver — an
        // up-only-parented fresh node (the #486 mechanism split) — so the fixture
        // constructs exactly that orphan shape at a live lambda seat.
        String source = """
                namespace "test.step524a"

                type Inner24:
                    deep string (1..1)

                type Meta24:
                    sTag24 string (0..1)
                        [metadata scheme]
                    plain string (0..1)
                    inner Inner24 (0..1)

                type Wrap24:
                    metas Meta24 (0..*)

                func Use24:
                    inputs:
                        w Wrap24 (1..1)
                    output:
                        out string (0..*)
                    set out:
                        w -> metas extract [ item -> plain ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step524-orphan.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RInlineFunction lambda = AstWalker.findAll(ws.files().get(0), RInlineFunction.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no extract lambda parsed"));

        // (a) the PLAIN member read — the synthetic.itemAttr class: the orphan's name IS
        // an attribute of the lambda's element type, so the arm adapts the exact
        // equivalent legacy synthesizes and re-stamps the bound twin's FieldAccess.
        RSymbolReference plainOrphan = new RSymbolReference();
        plainOrphan.setName("plain");
        plainOrphan.setParent(lambda);
        FieldAccess plainNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(plainOrphan, ws).orElseThrow(() -> new AssertionError(
                        "a symbol-EMPTY member read must adapt since the #524 admission")),
                "the empty-symbol bare name lowers via the legacy-equivalent item"
                        + " navigation (the bound twin's FieldAccess shape)");
        assertEquals("plain", plainNav.feature(), "the nav carries the member by name");
        IRVariable plainRecv = assertInstanceOf(IRVariable.class, plainNav.receiver(),
                "the receiver is the synthetic implicit item");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, plainRecv.variableKind(),
                "the receiver variable is the SYNTHETIC_ITEM kind — the #480"
                        + " range-correlated renderer's contract");
        assertEquals(Optional.empty(), adapter.declineReason(plainOrphan, ws),
                "empty <=> lowers: the twin moved with the arm (the mirror law)");

        // (b) the META member read — the synthetic.itemAttrMeta class (the cdm6
        // partyReference [metadata reference] / drr identifier [metadata scheme]
        // corpus shape): the equivalent lowers through the meta arm to IRMetaAccess.
        RSymbolReference metaOrphan = new RSymbolReference();
        metaOrphan.setName("sTag24");
        metaOrphan.setParent(lambda);
        IRMetaAccess metaNav = assertInstanceOf(IRMetaAccess.class,
                adapter.adapt(metaOrphan, ws).orElseThrow(() -> new AssertionError(
                        "a symbol-EMPTY META member read must adapt since the #524"
                                + " admission")),
                "the meta-annotated member lowers through the meta arm (the #501 C4"
                        + " bareMeta shape at the empty-symbol seat)");
        IRVariable metaRecv = assertInstanceOf(IRVariable.class, metaNav.receiver(),
                "the meta nav roots on the synthetic implicit item");
        assertEquals(IRVariable.VariableKind.SYNTHETIC_ITEM, metaRecv.variableKind(),
                "the meta receiver keeps the SYNTHETIC_ITEM kind");

        // (c) the disguised-chain HEAD recovery at the fc seat (the #524 fc leg): a
        // synthesized feature call over the empty-symbol head — the exact equivalent
        // legacy's synthesizeFeatureCall re-root builds for `inner -> deep` — composes
        // once the head resolves through the SHARED ladder and the LEAF resolves by
        // name on the head attribute's DECLARED type.
        RSymbolReference chainHead = new RSymbolReference();
        chainHead.setName("inner");
        chainHead.setParent(lambda);
        RFeatureCall chainFc = new RFeatureCall();
        chainFc.setReceiver(chainHead);
        chainFc.setFeatureName("deep");
        chainFc.setParent(lambda);
        FieldAccess chainNav = assertInstanceOf(FieldAccess.class,
                adapter.adapt(chainFc, ws).orElseThrow(() -> new AssertionError(
                        "a synthesized chain over an empty-symbol member head must"
                                + " compose since the #524 fc head-recovery leg")),
                "the chain lowers as the standing nav build over the arm-lowered head");
        assertEquals("deep", chainNav.feature(), "the leaf resolves on the head's"
                + " DECLARED type (the engine's ct channel never types these heads)");
        assertInstanceOf(FieldAccess.class, chainNav.receiver(),
                "the head composes as the receiver (the arm's synthetic-item hop)");

        // The chain boundary negative: a leaf that is NOT a member of the head's
        // declared type keeps the chain declined (prove-or-decline at the leaf too).
        RFeatureCall chainMiss = new RFeatureCall();
        RSymbolReference chainMissHead = new RSymbolReference();
        chainMissHead.setName("inner");
        chainMissHead.setParent(lambda);
        chainMiss.setReceiver(chainMissHead);
        chainMiss.setFeatureName("nonDeep24");
        chainMiss.setParent(lambda);
        assertTrue(adapter.adapt(chainMiss, ws).isEmpty(),
                "a non-member LEAF keeps the chain declined — the fc leg widens the"
                        + " head channel only, never the leaf resolution");

        // The boundary negative: a name that is NOT a member of the element form (and
        // matches no global) keeps declining with the standing facet — the arm is
        // prove-or-decline exactly like legacy's ladder, and the in-lambda absent face
        // is pinned as the residue spelling.
        RSymbolReference missOrphan = new RSymbolReference();
        missOrphan.setName("nonMember24");
        missOrphan.setParent(lambda);
        assertEquals(Optional.of("symbolUnresolved.synthetic.lambda.absent"),
                adapter.declineReason(missOrphan, ws),
                "a non-member name stays declined — the admission widens the SYMBOL"
                        + " class gate only, never the by-name resolution");
    }

    @Test
    void metaPathShortFormBareSchemeLowersSinceThe524Teach() {
        // #524 (witness 2 — the metaPathShortForm leg, legacy #348; the #522 OBS-3 named
        // gap CLOSED): a bare `scheme` read filtering a META-WRAPPER item has no model
        // element form — legacy synthesizes the SAME unresolved feature-call shape the
        // parser produces for an explicit `item -> scheme` and the render seat resolves
        // the qualifier short form. The arm mirrors the synthesis and the adapter's OWN
        // qualifier gates prove the wrapper (the binder source's terminal resolved
        // feature meta-annotated + the name a qualifier it carries), minting the
        // IRQualifierItemNav ORACLE LEAF re-stamped with the raw node's range. Pre-teach
        // this exact claim WAS the symbolUnresolved.lambdaSrcUnprovable.global.typeAlias
        // 3 face (drr Extract_BondConnect — the #524 iaGate census's lgMsf row).
        String source = """
                namespace "test.step524b"

                type Meta24b:
                    sTag24 string (0..1)
                        [metadata scheme]
                    plain string (0..1)

                type Wrap24b:
                    metas Meta24b (0..*)

                func Msf24:
                    inputs:
                        w Wrap24b (1..1)
                    output:
                        out string (0..*)
                    set out:
                        w -> metas extract sTag24 then filter scheme = "x"

                func MsfNeg24:
                    inputs:
                        w Wrap24b (1..1)
                    output:
                        out string (0..*)
                    set out:
                        w -> metas extract plain then filter scheme = "x"
                """;
        RModel model = AstBuilder.buildFromString(source, "step524-msf.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RSymbolReference bareScheme = AstWalker.findAll(ws.files().get(0), RFunction.class)
                .stream().filter(f -> "Msf24".equals(f.name()))
                .flatMap(f -> AstWalker.findAll(f, RSymbolReference.class).stream())
                .filter(r -> "scheme".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no bare scheme parsed"));
        assertTrue(bareScheme.symbol().isEmpty(),
                "the fixture's premise: the bare qualifier name resolves to NOTHING (the"
                        + " corpus face's symbol-EMPTY shape)");
        IRQualifierItemNav qualNav = assertInstanceOf(IRQualifierItemNav.class,
                adapter.adapt(bareScheme, ws).orElseThrow(() -> new AssertionError(
                        "a bare scheme over a meta-wrapper item must adapt since the"
                                + " #524 short-form leg")),
                "the short form lowers to the IRQualifierItemNav oracle leaf — the"
                        + " containing roots render whole-legacy BY IDENTITY");
        assertEquals("scheme", qualNav.qualifierName(), "the leaf carries the qualifier name");
        assertEquals(bareScheme.sourceRange(), qualNav.sourceRange(),
                "the mint is re-stamped with the RAW node's range (the oracle-serve"
                        + " correlation key)");

        // The boundary negative: the same bare name over a NON-meta piped item proves
        // no wrapper — the qualifier gates decline and the face stays (prove-or-decline).
        RSymbolReference negScheme = AstWalker.findAll(ws.files().get(0), RFunction.class)
                .stream().filter(f -> "MsfNeg24".equals(f.name()))
                .flatMap(f -> AstWalker.findAll(f, RSymbolReference.class).stream())
                .filter(r -> "scheme".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no negative scheme parsed"));
        assertTrue(adapter.declineReason(negScheme, ws).isPresent(),
                "a bare scheme over a PLAIN item stays declined — the short-form leg"
                        + " fires only on the adapter's own meta-wrapper proof");
    }

    @Test
    void armTypedSiblingEnumComparandMintsSinceThe524Teach() {
        // #524 (witness 3 — the ARM-typed sibling channel at the #504 equality-recovery
        // seat): a bare enum-VALUE name whose equality sibling the engine cache never
        // typed but the adapter LOWERS (the #522-taught annotation-shadowed item read —
        // its navType is the attribute channel's enum) requalifies against the SIBLING'S
        // LOWERED type — legacy tryBareEnumComparand types the same sibling through its
        // own compiled render. Pre-teach this exact claim WAS the
        // symbolUnresolved.lambda.absent 3 face (drr rule ConfirmationTimestamp's
        // `filter qualification = confirmationDateTime` — the golden renders
        // MapperS.of(EventTimestampQualificationEnum.CONFIRMATION_DATE_TIME)).
        String source = """
                namespace "test.step524c"

                annotation qual24: <"the fixture annotation shadowing the attribute name">

                enum QE24:
                    confVal24
                    otherVal24

                type Stamp24:
                    qual24 QE24 (0..1)

                type Wrap24c:
                    stamps Stamp24 (0..*)

                reporting rule Filt24 from Wrap24c:
                    extract stamps
                    then filter qual24 = confVal24

                reporting rule FiltNeg24 from Wrap24c:
                    extract stamps
                    then filter qual24 = notAValue24
                """;
        RModel model = AstBuilder.buildFromString(source, "step524-comparand.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RSymbolReference bareValue = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "confVal24".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no bare confVal24 parsed"));
        // The premise MOVED at v3.1 C1 part 2 (PR #567): the equality-RHS
        // expected-type seat (upstream caseEqualsOperation, RIGHT-only) now
        // BINDS the bare value to the sibling attribute's enum at the ENGINE
        // level — Cat 14/16 read the MF-2-healed attribute binding and stamp
        // the REnumValue. The #524 adapter channel this test teaches remains
        // for shapes the engine cannot type (the negative + re-entrancy pins
        // below still exercise its guards on genuinely-unresolved names); on
        // THIS fixture the adapter is now downstream of a resolved value.
        assertInstanceOf(com.regnosys.rosetta.ast.supporting.REnumValue.class,
                bareValue.symbol().orElse(null),
                "the premise since PR #567: the bare value BINDS to the sibling enum's"
                        + " REnumValue at the engine level (the equality-RHS expected seat)");
        IRReference minted = assertInstanceOf(IRReference.class,
                adapter.adapt(bareValue, ws).orElseThrow(() -> new AssertionError(
                        "an arm-typed sibling's enum comparand must adapt since the"
                                + " #524 channel")),
                "the comparand mints the #504 IRReference — the same builder, the"
                        + " sibling's enum type");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, minted.referenceKind(),
                "the mint is the ENUM_VALUE kind (MapperS.of(EnumName.CONSTANT) — the"
                        + " golden's own render)");
        assertEquals("confVal24", minted.target(), "the mint carries the value name");

        // The boundary negative: a name that is NOT one of the sibling enum's own
        // values keeps declining — the membership gate is untouched, and the residue
        // keeps the in-lambda absent spelling (the corpus face's own).
        RSymbolReference negValue = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "notAValue24".equals(r.name()) && r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no bare notAValue24 parsed"));
        assertEquals(Optional.of("symbolUnresolved.lambda.absent"),
                adapter.declineReason(negValue, ws),
                "a non-value name stays declined at the standing face — the channel"
                        + " widens the sibling TYPING only, never the membership gate");

        // The RE-ENTRANCY fence (the Seat-1 #524 OBS-1 close): an equality whose BOTH
        // operands are admissible bare comparand shapes (two dangling names) must
        // DECLINE — without the fence the sibling-adapt would re-enter the comparand
        // seat with the roles swapped, unbounded. The fixture builds the exact cycle
        // shape: two fresh unresolved bare references under one equality, parented at
        // the live lambda seat.
        REqualityExpr danglingEq = new REqualityExpr();
        RSymbolReference danglingLeft = new RSymbolReference();
        danglingLeft.setName("danglingA24");
        RSymbolReference danglingRight = new RSymbolReference();
        danglingRight.setName("danglingB24");
        danglingEq.setLeft(danglingLeft);
        danglingEq.setRight(danglingRight);
        danglingLeft.setParent(danglingEq);
        danglingRight.setParent(danglingEq);
        danglingEq.setParent(AstWalker.findAll(ws.files().get(0), RInlineFunction.class)
                .stream().findFirst().orElseThrow());
        assertTrue(adapter.adapt(danglingLeft, ws).isEmpty(),
                "two dangling comparands TERMINATE and decline — the fence skips the"
                        + " sibling-adapt when the sibling is itself an admissible"
                        + " bare comparand shape");
    }

    @Test
    void qualifierLeafOverLoweredMemberHeadMintsSinceThe525Teach() {
        // #525 (the wave witness — the QUALIFIER leaf over the lowered member head): the
        // #524 exposed frontier CLAIMED. A disguised `partyReference -> reference` /
        // `identifier -> scheme` chain inside a filter/extract lambda — legacy's
        // re-entrant render synthesizes the feature call over an EMPTY-symbol head — has
        // NO resolvable leaf attribute (a metadata qualifier is never a member), but the
        // head attribute's OWN [metadata …] annotation carries the name. The arm proves
        // it via the #512 qualifier gates at the MEMBER head (isMetaAnnotated + the name
        // ∈ metaQualifierNames), lowers the head by its own #524 gate (the headUnGate
        // census's uniform headLowers:IRMetaAccess fact) and mints the DISTINCT
        // receiver-carrying IRQualifierReceiverNav (the 44th kind): an ORACLE LEAF —
        // at-root claims serve through the standing navChain dispatch leg, byte-identical
        // BY IDENTITY. Pre-teach these exact claims WERE the RFeatureCall:
        // featureUnresolved.headUnresolved 9 face (7 cdm6-f + 2 drr-f at the #524 SOT).
        String source = """
                namespace "test.step525"

                type Party25:
                    pname string (0..1)

                type Collide25:
                    reference string (0..1)

                type Cpty25:
                    partyRef25 Party25 (0..1)
                        [metadata reference]
                    sTag25 string (0..1)
                        [metadata scheme]
                    plainRef25 Party25 (0..1)
                    collideRef25 Collide25 (0..1)
                        [metadata reference]

                type Wrap25:
                    cptys Cpty25 (0..*)

                func Use25:
                    inputs:
                        w Wrap25 (1..1)
                    output:
                        out Party25 (0..*)
                    add out:
                        w -> cptys extract [ item -> plainRef25 ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step525-qualifier.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RInlineFunction lambda = AstWalker.findAll(ws.files().get(0), RInlineFunction.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no extract lambda parsed"));

        // (a) the REFERENCE qualifier over the meta member head (the cdm6
        // `partyReference -> reference` class — 7 of the 9): the synthesized-FC
        // equivalent mints the receiver-carrying kind, the head composed as the
        // lowered meta hop.
        RSymbolReference refHead = new RSymbolReference();
        refHead.setName("partyRef25");
        refHead.setParent(lambda);
        RFeatureCall refFc = new RFeatureCall();
        refFc.setReceiver(refHead);
        refFc.setFeatureName("reference");
        refFc.setParent(lambda);
        // The premises ASSERTED (the Seat-1 #525 OBS-3 close — the #524 OBS-5 class):
        // the orphan mirrors legacy synthesizeFeatureCall's shape — an EMPTY-symbol,
        // args-empty head — and the DISTINCT ranges make the re-stamp assert real
        // (a null-range orphan would pass the range check vacuously).
        assertTrue(refHead.symbol().isEmpty(),
                "the fixture's premise: the synthesized head carries NO symbol");
        assertTrue(refHead.args().isEmpty(),
                "the fixture's premise: the synthesized head carries NO args");
        SourceRange headRange = new SourceRange("step525-qualifier.rosetta",
                20, 25, 20, 35, -1, -1);
        SourceRange fcRange = new SourceRange("step525-qualifier.rosetta",
                20, 25, 20, 48, -1, -1);
        refHead.setSourceRange(headRange);
        refFc.setSourceRange(fcRange);
        IRQualifierReceiverNav refNav = assertInstanceOf(IRQualifierReceiverNav.class,
                adapter.adapt(refFc, ws).orElseThrow(() -> new AssertionError(
                        "a qualifier leaf over a lowered meta member head must adapt"
                                + " since the #525 teach")),
                "the qualifier chain lowers to the receiver-carrying oracle leaf — the"
                        + " at-root claim serves through the navChain dispatch leg");
        assertEquals("reference", refNav.qualifierName(),
                "the leaf carries the qualifier name");
        assertInstanceOf(IRMetaAccess.class, refNav.receiver(),
                "the head composes as the lowered meta hop (the census's uniform"
                        + " headLowers:IRMetaAccess fact)");
        assertEquals(fcRange, refNav.sourceRange(),
                "the mint stamps the CLAIM node's own range — not the head's (the"
                        + " oracle-serve correlation key; the ranges are distinct by"
                        + " construction)");
        assertEquals(Optional.empty(), adapter.declineReason(refFc, ws),
                "empty <=> lowers: the decline attribution moved with the arm");

        // (b) the SCHEME qualifier twin over a STRING-typed meta head (the drr
        // `identifier -> scheme` class — 2 of the 9; declaredDataTypeOf reads null on a
        // basic-typed head, so the qualifier proof is the ONLY channel that can carry
        // the leaf).
        RSymbolReference schemeHead = new RSymbolReference();
        schemeHead.setName("sTag25");
        schemeHead.setParent(lambda);
        RFeatureCall schemeFc = new RFeatureCall();
        schemeFc.setReceiver(schemeHead);
        schemeFc.setFeatureName("scheme");
        schemeFc.setParent(lambda);
        IRQualifierReceiverNav schemeNav = assertInstanceOf(IRQualifierReceiverNav.class,
                adapter.adapt(schemeFc, ws).orElseThrow(() -> new AssertionError(
                        "a scheme qualifier over a string-typed meta head must adapt"
                                + " since the #525 teach")),
                "the string-typed head class mints through the same leg");
        assertEquals("scheme", schemeNav.qualifierName(),
                "the scheme twin carries its qualifier name");

        // Boundary negative 1 (the meta-proof gate): the same qualifier name over a
        // PLAIN head attribute proves no wrapper — the leg never fires and the claim
        // keeps the standing corpus face (prove-or-decline; the flip this lock detects
        // would remove the token).
        RSymbolReference plainHead = new RSymbolReference();
        plainHead.setName("plainRef25");
        plainHead.setParent(lambda);
        RFeatureCall plainFc = new RFeatureCall();
        plainFc.setReceiver(plainHead);
        plainFc.setFeatureName("reference");
        plainFc.setParent(lambda);
        assertEquals(Optional.of("featureUnresolved.headUnresolved"),
                adapter.declineReason(plainFc, ws),
                "a qualifier name over a PLAIN member head stays declined at the"
                        + " standing face — the leg fires on the meta proof only");

        // Boundary negative 2 (the qualifier-universe gate): a leaf OUTSIDE the head
        // annotation's own qualifiers keeps declining — [metadata reference] never
        // carries `scheme` (and Party25 has no such member).
        RSymbolReference wrongQualHead = new RSymbolReference();
        wrongQualHead.setName("partyRef25");
        wrongQualHead.setParent(lambda);
        RFeatureCall wrongQualFc = new RFeatureCall();
        wrongQualFc.setReceiver(wrongQualHead);
        wrongQualFc.setFeatureName("scheme");
        wrongQualFc.setParent(lambda);
        assertEquals(Optional.of("featureUnresolved.headUnresolved"),
                adapter.declineReason(wrongQualFc, ws),
                "a leaf outside the annotation's own qualifier set stays declined —"
                        + " the name must be one the head attribute itself carries");

        // The member-first order: a leaf that IS a member of the head's declared type
        // never mints the qualifier kind (the member walk stays FIRST).
        RSymbolReference memberHead = new RSymbolReference();
        memberHead.setName("partyRef25");
        memberHead.setParent(lambda);
        RFeatureCall memberFc = new RFeatureCall();
        memberFc.setReceiver(memberHead);
        memberFc.setFeatureName("pname");
        memberFc.setParent(lambda);
        assertTrue(adapter.adapt(memberFc, ws)
                        .filter(IRQualifierReceiverNav.class::isInstance).isEmpty(),
                "a member-name leaf takes the member resolution, never the qualifier"
                        + " mint (the member-first order)");

        // THE ORDER LOCK PROPER (the Seat-1 #525 OBS-1 close): a leaf that is BOTH a
        // member of the head's declared type AND one of the head annotation's own
        // qualifiers — Collide25.reference under collideRef25 [metadata reference] —
        // must take the MEMBER path. A swapped leg order would mint the qualifier
        // kind here (the gate's every member passes: meta-annotated head, the name in
        // its qualifiers, the head lowers), so this assert is flip-sensitive to the
        // order itself — the pname negative above cannot detect a swap (pname is in
        // no qualifier universe).
        RSymbolReference collideHead = new RSymbolReference();
        collideHead.setName("collideRef25");
        collideHead.setParent(lambda);
        RFeatureCall collideFc = new RFeatureCall();
        collideFc.setReceiver(collideHead);
        collideFc.setFeatureName("reference");
        collideFc.setParent(lambda);
        assertTrue(adapter.adapt(collideFc, ws)
                        .filter(IRQualifierReceiverNav.class::isInstance).isEmpty(),
                "a member-AND-qualifier collision takes the member resolution — the"
                        + " qualifier leg fires ONLY when no member resolves (the"
                        + " member-first order lock)");
    }

    @Test
    void ingestWallAndATailClustersLowerSinceThe529Teach() throws IOException {
        // #529 (the wave witness — the composed 38+35+A teach): the four census-decoded
        // classes at unit speed, premises asserted per the #525 OBS-3 law. (1) the ENR-seat
        // IMPLICIT-ITEM head classes (the itemHeadGate census's whole pool: every
        // headMiss.other head resolves on the enclosing BINDER's element form — the
        // qualifier-leaf class 27 and the zonedDateTime record-leaf class 11); (2) the
        // FC-seat CHOICE-OPTION classes (the headAttrGate census's whole pool 100%
        // ht:choice — the symbol-head channel + the receiver-general chain hop); (3) the
        // #528 mint's equality-seat frontier (eqImplGate: s:L.sib:IRLiteral); (4) the
        // argItem DECLARATION channel (itemArgSrcGate: g:unres.op:input.metaFree — the
        // subject input's declaration proves the cast item meta-free where the type channel
        // never can). The lambda-boundary negative for (4) rides the shared walk's
        // RInlineFunction stop (single-source with the census token — see
        // enclosingSwitchSubjectInput).
        String waveSource = """
                namespace "test.step529"

                type Party29:
                    pname string (0..1)

                type Cpty29:
                    partyRef29 Party29 (0..1)
                        [metadata reference]
                    ts29 zonedDateTime (0..1)
                    plain29 string (0..1)

                type Wrap29:
                    cptys Cpty29 (0..*)

                choice Inner29:
                    Party29

                choice Obs29:
                    Inner29
                    Wrap29

                enum Curr29:
                    USD
                    EUR

                choice Crit29:
                    Party29
                    Curr29

                func Take29:
                    inputs:
                        x Curr29 (1..1)
                    output:
                        y boolean (1..1)
                    set y: True

                func Use29:
                    inputs:
                        w Wrap29 (1..1)
                    output:
                        out string (0..*)
                    add out:
                        w -> cptys extract [ item -> plain29 ]

                func Opt29:
                    inputs:
                        obs Obs29 (1..1)
                    output:
                        got boolean (1..1)
                    set got:
                        obs -> Inner29 -> Party29 exists

                func Check29:
                    inputs:
                        c Crit29 (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        c switch
                            Party29 then False,
                            Curr29 then Take29(item)

                func CheckMeta29:
                    inputs:
                        cm Crit29 (1..1)
                            [metadata reference]
                    output:
                        ok boolean (1..1)
                    set ok:
                        cm switch
                            Party29 then False,
                            Curr29 then Take29(item)

                choice CritM29:
                    Party29
                    Curr29
                        [metadata scheme]

                func CheckMetaOpt29:
                    inputs:
                        co CritM29 (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        co switch
                            Party29 then False,
                            Curr29 then Take29(item)
                """;
        // zonedDateTime is a BUILTIN record — the fixture workspace carries basictypes (the
        // #507 record-arm fixture convention).
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        List<RModel> waveModels = new ArrayList<>();
        try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                waveModels.add(AstBuilder.buildFromFile(p));
            }
        }
        waveModels.add(AstBuilder.buildFromString(waveSource, "step529-wave.rosetta"));
        RWorkspace ws = RWorkspace.build(waveModels).workspace();
        var waveFile = ws.files().get(ws.files().size() - 1);
        RInlineFunction lambda = AstWalker.findAll(waveFile, RInlineFunction.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no extract lambda parsed"));

        // (1a) the ENR qualifier class — a disguised `partyRef29 -> reference` inside the
        // extract lambda: the head resolves on the binder's element form (Cpty29), the leaf
        // is the head's OWN metadata qualifier, and the equivalent-adapt mints the
        // receiver-carrying oracle leaf re-stamped with the RAW ENR's id and range.
        REnumValueRef qualEnr = new REnumValueRef();
        qualEnr.setEnumName("partyRef29");
        qualEnr.setValueName("reference");
        qualEnr.setParent(lambda);
        qualEnr.attachToWorkspace(ws);
        SourceRange qualRange = new SourceRange("step529-wave.rosetta", 60, 9, 60, 34, -1, -1);
        qualEnr.setSourceRange(qualRange);
        assertTrue(qualEnr.resolvedAttributeChain().isEmpty()
                        && qualEnr.resolvedInputFeature().isEmpty()
                        && qualEnr.resolvedChoiceOption().isEmpty()
                        && qualEnr.resolvedTypeRestriction().isEmpty()
                        && qualEnr.enumeration().isEmpty() && qualEnr.resolvedSymbol().isEmpty(),
                "the fixture's premise: the resolver binds NO channel (the corpus class's"
                        + " own no-channel identity)");
        IRQualifierReceiverNav qualNav = assertInstanceOf(IRQualifierReceiverNav.class,
                adapter.adapt(qualEnr, ws).orElseThrow(() -> new AssertionError(
                        "an implicit-item qualifier disguise must adapt since the #529 teach")),
                "the qualifier class mints the #525 receiver-carrying kind at the ENR seat");
        assertEquals("reference", qualNav.qualifierName(), "the leaf carries the qualifier");
        assertInstanceOf(IRMetaAccess.class, qualNav.receiver(),
                "the head composes as the lowered meta member hop");
        assertEquals(qualRange, qualNav.sourceRange(),
                "the mint re-stamps the RAW ENR's own range (distinct from the synthesized"
                        + " equivalent's null ranges by construction)");
        assertEquals(Optional.empty(), adapter.declineReason(qualEnr, ws),
                "empty <=> lowers: the headMiss.other attribution moved with the arm");

        // (1b) the ENR record class — a disguised `ts29 -> date`: the head resolves on the
        // element form, declares the zonedDateTime RECORD, and the leaf is one of the
        // record's own features; the mint is the receiver-carrying record kind at the
        // DECLARED channel.
        REnumValueRef recEnr = new REnumValueRef();
        recEnr.setEnumName("ts29");
        recEnr.setValueName("date");
        recEnr.setParent(lambda);
        recEnr.attachToWorkspace(ws);
        IRRecordReceiverNav recNav = assertInstanceOf(IRRecordReceiverNav.class,
                adapter.adapt(recEnr, ws).orElseThrow(() -> new AssertionError(
                        "an implicit-item record disguise must adapt since the #529 teach")),
                "the record class mints the #512-B1 receiver-carrying kind at the DECLARED"
                        + " channel (the engine never types the synthesized head)");
        assertEquals("date", recNav.featureName(), "the leaf carries the record feature");
        assertEquals("zonedDateTime", recNav.recordTypeName(),
                "the record identity is the head's declared record");
        assertEquals(Optional.empty(), adapter.declineReason(recEnr, ws));

        // (1c) boundary negatives — a head resolving nowhere (incl. the binder element)
        // keeps the honest residue; a record leaf outside the record's own features keeps
        // declining (the flip either lock detects would remove the spelling).
        REnumValueRef ghostEnr = new REnumValueRef();
        ghostEnr.setEnumName("ghost29");
        ghostEnr.setValueName("date");
        ghostEnr.setParent(lambda);
        ghostEnr.attachToWorkspace(ws);
        assertTrue(adapter.adapt(ghostEnr, ws).isEmpty(), "a nowhere-head stays declined");
        assertEquals(Optional.of("noResolutionChannel.headMiss.other"),
                adapter.declineReason(ghostEnr, ws),
                "the honest residue spelling stands for the genuinely unresolvable");
        REnumValueRef wrongLeafEnr = new REnumValueRef();
        wrongLeafEnr.setEnumName("ts29");
        wrongLeafEnr.setValueName("ghostLeaf");
        wrongLeafEnr.setParent(lambda);
        wrongLeafEnr.attachToWorkspace(ws);
        assertTrue(adapter.adapt(wrongLeafEnr, ws).isEmpty(),
                "a leaf outside the record's own features stays declined"
                        + " (the #444 membership rule at the new seat)");

        // (2a) the FC choice-option class — the PARSED `obs -> Inner29 -> Party29` chain:
        // the inner hop mints over the bound symbol head (channel a), and the outer hop
        // composes over the LOWERED inner (channel b — the receiver-general chain leg the
        // symbol-head teach exposes at this same wave), each typing from the ATTACHED
        // option's referenced type.
        RFunction opt29 = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "Opt29".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Opt29 not parsed"));
        // The corpus class is ENGINE-BLIND (every headAttrGate row read typeMissing — the
        // engine's node-keyed cache types neither the head read nor the hops, which is
        // exactly why the standing #503 engine-typed option projection at the same seat
        // never claimed these faces). Synthesized nodes reproduce that cache-missing shape
        // EXACTLY (a parsed inner hop would be engine-typed through its own resolvedChoice-
        // Option bind and take the STANDING projection leg instead — the fixture's parsed
        // Opt29 chain documents that standing route stays live).
        RAttribute obsInput = opt29.inputs().get(0);
        RSymbolReference obsHead = new RSymbolReference();
        obsHead.setName("obs");
        obsHead.setResolvedSymbol(obsInput);
        obsHead.setParent(opt29);
        RFeatureCall innerOpt = new RFeatureCall();
        innerOpt.setReceiver(obsHead);
        innerOpt.setFeatureName("Inner29");
        innerOpt.setParent(opt29);
        innerOpt.attachToWorkspace(ws);
        RFeatureCall outerOpt = new RFeatureCall();
        outerOpt.setReceiver(innerOpt);
        outerOpt.setFeatureName("Party29");
        outerOpt.setParent(opt29);
        outerOpt.attachToWorkspace(ws);
        assertTrue(outerOpt.resolvedFeature().isEmpty() && innerOpt.resolvedFeature().isEmpty(),
                "the fixture's premise: the engine resolves NO feature for a choice-option"
                        + " selection (the declMiss class's own fact)");
        IRChoiceReceiverNav outerNav = assertInstanceOf(IRChoiceReceiverNav.class,
                adapter.adapt(outerOpt, ws).orElseThrow(() -> new AssertionError(
                        "a choice-option selection must adapt since the #529 teach")),
                "the option selection mints the receiver-carrying 46th kind");
        assertEquals("Party29", outerNav.optionName(), "the leaf carries the option name");
        assertEquals("Inner29", outerNav.choiceName(),
                "the choice identity is the RECEIVER's proven choice (the chain channel)");
        IRChoiceReceiverNav innerNav = assertInstanceOf(IRChoiceReceiverNav.class,
                outerNav.receiver(),
                "the chain hop composes over the inner mint — the receiver-general channel");
        assertEquals("Inner29", innerNav.optionName());
        assertEquals("Obs29", innerNav.choiceName(),
                "the inner hop reads the head attribute's DECLARED choice (channel a)");
        assertTrue(innerNav.type() != null && !innerNav.type().isMissing(),
                "the inner types from the ATTACHED option (choice -> RChoiceTypeRef — the"
                        + " #521 elem-proven-typing law), which is what lets the outer hop"
                        + " prove its own choice");
        assertEquals(Optional.empty(), adapter.declineReason(outerOpt, ws),
                "empty <=> lowers: the declMiss attribution moved with the arm");

        // (2b) boundary negative — an option name OUTSIDE the choice's own list keeps the
        // honest declMiss residue (the mirror's UNATTRIBUTED leg fires only on a real
        // option hit).
        RSymbolReference obsHead2 = new RSymbolReference();
        obsHead2.setName("obs");
        obsHead2.setResolvedSymbol(obsInput);
        obsHead2.setParent(opt29);
        RFeatureCall wrongOpt = new RFeatureCall();
        wrongOpt.setReceiver(obsHead2);
        wrongOpt.setFeatureName("Ghost29");
        wrongOpt.setParent(opt29);
        wrongOpt.attachToWorkspace(ws);
        assertTrue(adapter.adapt(wrongOpt, ws).isEmpty(),
                "an off-option leaf over a choice head stays declined");
        assertEquals(Optional.of("featureUnresolved.headAttr.declMiss"),
                adapter.declineReason(wrongOpt, ws),
                "the honest declMiss spelling stands for the off-option residue");

        // (3) the #528 mint's equality-seat frontier — a bare attr-outside-function read
        // (the noFilterExtractBinder census family, the #528 retensed-lock shape) as the
        // LEFT operand of an equality against a literal (the eqImplGate census's
        // s:L.sib:IRLiteral shape, witness MapIntent): the equality now lowers, the
        // containing root serving whole-legacy through the kind-wide oracle routing.
        RFunction use29 = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "Use29".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Use29 not parsed"));
        RAttribute plainAttr = AstWalker.findAll(waveFile, RAttribute.class).stream()
                .filter(a -> "plain29".equals(a.name())).findFirst()
                .orElseThrow(() -> new AssertionError("plain29 not parsed"));
        RSymbolReference bareOutside = new RSymbolReference();
        bareOutside.setName("plain29");
        bareOutside.setResolvedSymbol(plainAttr);
        bareOutside.setParent(use29);
        assertInstanceOf(IRImplicitAttrNav.class,
                adapter.adapt(bareOutside, ws).orElseThrow(() -> new AssertionError(
                        "the fixture's premise: the bare outside-function read mints the"
                                + " #528 kind")),
                "the fixture's premise: the operand IS the #528 shallow mint");
        RStringLiteral xLit = new RStringLiteral();
        xLit.setValue("x");
        xLit.setParent(use29);
        REqualityExpr eqFrontier = new REqualityExpr();
        eqFrontier.setOp(EqOp.EQ);
        eqFrontier.setLeft(bareOutside);
        eqFrontier.setRight(xLit);
        eqFrontier.setParent(use29);
        eqFrontier.attachToWorkspace(ws);
        BinaryOp eqLowered = assertInstanceOf(BinaryOp.class,
                adapter.adapt(eqFrontier, ws).orElseThrow(() -> new AssertionError(
                        "an equality over the #528 kind must adapt since the #529"
                                + " admission")),
                "the equality seat admits the kind (s:L.sib:IRLiteral — the census's own"
                        + " shape)");
        assertInstanceOf(IRImplicitAttrNav.class, eqLowered.left(),
                "the admitted operand rides inside the lowered equality");
        assertEquals(Optional.empty(), adapter.declineReason(eqFrontier, ws),
                "empty <=> lowers: the operand:IRImplicitAttrNav face retires");

        // (4) the argItem DECLARATION channel — the switch-case item arg whose guard's
        // element type is NOT data-bindable (the enum-option case: caseNarrowedElementType
        // declines non-data guards, so the item stays UNTYPED and the #502/#528 type
        // channel can never admit it), taught through the SUBJECT INPUT's declaration.
        RFunction check29 = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "Check29".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Check29 not parsed"));
        RSymbolReference takeCall = AstWalker.findAll(check29, RSymbolReference.class)
                .stream().filter(sr -> "Take29".equals(sr.name()) && !sr.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Take29(item) call not parsed"));
        RImplicitVariable caseItem = assertInstanceOf(RImplicitVariable.class,
                takeCall.args().get(0), "the fixture's premise: the arg is the bare item");
        RMetaAnnotatedType caseItemT = ws.getInferredType(caseItem);
        assertTrue(caseItemT == null || caseItemT.isMissing(),
                "the fixture's premise: the enum-guard case item is UNTYPED — the type"
                        + " channel can never admit it, only the declaration channel can");
        assertInstanceOf(IRApply.class,
                adapter.adapt(takeCall, ws).orElseThrow(() -> new AssertionError(
                        "an untyped switch-case item arg must admit through the subject"
                                + " declaration channel since the #529 teach")),
                "the call lowers whole — the item arg admitted by the meta-free subject"
                        + " declaration");
        assertEquals(Optional.empty(), adapter.declineReason(takeCall, ws),
                "empty <=> lowers: the argItem face's last live carrier taught");

        // (4b) boundary negative — the SAME shape under a [metadata reference] subject
        // input keeps the decline: the declaration channel proves nothing for a
        // meta-carrying subject (prove-or-decline; the flip this lock detects would
        // admit a wrapper-valued cast).
        RFunction checkMeta29 = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "CheckMeta29".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("CheckMeta29 not parsed"));
        RSymbolReference metaTakeCall = AstWalker.findAll(checkMeta29, RSymbolReference.class)
                .stream().filter(sr -> "Take29".equals(sr.name()) && !sr.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the meta-subject call not parsed"));
        assertTrue(adapter.adapt(metaTakeCall, ws).isEmpty(),
                "a meta-annotated subject input keeps the decline — the declaration"
                        + " channel is prove-or-decline");
        assertEquals(Optional.of("argItem"), adapter.declineReason(metaTakeCall, ws),
                "the argItem spelling stands for the meta-subject residue");

        // (4c) boundary negative — the OPTION-meta belt (the Seat-1 #529 MF-1 close):
        // a meta-free SUBJECT whose declared choice carries a [metadata]-annotated
        // MATCHED option keeps the decline. The guard is the ENUM option (the
        // non-data-guard path — the case narrowing types nothing, so the standing
        // #523 typed admission can never claim it and the DECLARATION channel is the
        // only live gate — the belt's own reachable class); legacy renders a
        // choice-subject case value through its own switchArgument OPTION-NAV ladder
        // (the witness golden CheckCriteria's own render), so the item's Java value
        // is the OPTION field's — a wrapper exactly when the option is meta-annotated
        // (the #523 switchCaseOptionMetaSuspect doctrine at the arg seat); the flip
        // this lock detects would admit a wrapper-valued item on a subject-only
        // proof. (A DATA-guard meta-option shape is claimed upstream by the standing
        // #523 typed admission — byte-safe BY IDENTITY, legacy's own render decides
        // the wrapper handling — so the belt's decline is reachable exactly at the
        // untyped-guard classes it was built for.)
        RFunction checkMetaOpt29 = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "CheckMetaOpt29".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("CheckMetaOpt29 not parsed"));
        RSymbolReference metaOptCall = AstWalker.findAll(checkMetaOpt29, RSymbolReference.class)
                .stream().filter(sr -> "Take29".equals(sr.name()) && !sr.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the meta-option call not parsed"));
        assertTrue(adapter.adapt(metaOptCall, ws).isEmpty(),
                "a meta-annotated MATCHED option keeps the decline — the option channel"
                        + " is part of the proof, not just the subject");
        assertEquals(Optional.of("argItem"), adapter.declineReason(metaOptCall, ws),
                "the argItem spelling stands for the meta-option residue");
    }

    @Test
    void compoundReceiverStructuralTypingLowersSinceThe530Teach() throws IOException {
        // #530 arm-B (the wave witness): the nonSymbolReceiver residue's three legs at the
        // compound-receiver seat — the nsrCtGate census's whole 19-face pool. (1) the
        // RECORD leg (`value -> date` over IdentifiedDate-family items — 9 cdm6-f + 2
        // drr-f; the golden renders legacy's own Date.of(zdt.toLocalDate()) record deref):
        // the lowered receiver's attribute-channel type proves the record where the engine
        // cache reads MISSING (the #479 cache-boundary class — the arm-B1 lowered-type
        // channel). (2) the CHOICE leg (`payout extract [ item -> OptionPayout ]` — 2
        // cdm6-f): the binder source's declared DIRECT choice proves the option where the
        // item's cache-boundary type never can (the arm-B2 structural channel). (3) the
        // QUALIFIER leg (`item -> scheme` over meta-string elements — 6 drr-r; the golden
        // renders getMeta().getScheme()): the standing #512-B2 deep-pipe qualifier walk at
        // the parsed seat. Each leg carries a PARSED twin (the engine-typed standing
        // routes — the #529 engine-blind fixture corollary's documentation half) and the
        // engine-blind legs carry SYNTHESIZED cache-missing twins (the corollary's pin
        // half; parents placed structurally per the #529 fence law).
        String waveSource = """
                namespace "test.step530"

                type Ident30:
                    value zonedDateTime (1..1)

                type Party30:
                    entityId string (0..*)
                        [metadata scheme]
                    entityId2 string (0..*)
                        [metadata scheme]
                    plain30 string (0..*)

                type OptA30:
                    ax string (0..1)

                type OptB30:
                    bx string (0..1)

                choice Pay30:
                    OptA30
                    OptB30

                func Str30:
                    inputs:
                        s string (0..1)
                    output:
                        strOut boolean (1..1)
                    set strOut:
                        s exists

                func RecRecv30:
                    inputs:
                        ids Ident30 (0..*)
                    output:
                        recOut date (0..*)
                    add recOut:
                        ids extract [ item -> value -> date ]

                func ChoiceRecv30:
                    inputs:
                        pays Pay30 (0..*)
                    output:
                        chOut boolean (1..1)
                    set chOut:
                        pays extract [ item -> OptA30 ] exists

                reporting rule QualRecv30 from Party30:
                    extract entityId
                    then filter Str30(item -> scheme)
                    then first

                reporting rule QualJoin30 from Party30:
                    extract
                        ((if plain30 exists
                        then entityId2
                        else entityId)
                            then filter Str30(item -> scheme)
                            then first)

                reporting rule QualJoinNeg30 from Party30:
                    extract
                        ((if plain30 exists
                        then plain30
                        else entityId)
                            then filter Str30(item -> scheme)
                            then first)
                """;
        // zonedDateTime is a BUILTIN record — the fixture workspace carries basictypes
        // (the #507 record-arm fixture convention).
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        List<RModel> waveModels = new ArrayList<>();
        try (Stream<Path> s = Files.walk(BUILTINS_ROOT)) {
            for (Path p : s.filter(x -> x.toString().endsWith(".rosetta")).sorted().toList()) {
                waveModels.add(AstBuilder.buildFromFile(p));
            }
        }
        waveModels.add(AstBuilder.buildFromString(waveSource, "step530-wave.rosetta"));
        RWorkspace ws = RWorkspace.build(waveModels).workspace();
        var waveFile = ws.files().get(ws.files().size() - 1);

        // (1a) the RECORD leg, parsed twin — the standing engine-type channel documents
        // the route (the checker types the in-fixture receiver).
        RFeatureCall recNav = navUnder(waveFile, "RecRecv30", "date");
        IRRecordReceiverNav recMint = assertInstanceOf(IRRecordReceiverNav.class,
                adapter.adapt(recNav, ws).orElseThrow(() -> new AssertionError(
                        "a record read over a lowered compound receiver must adapt")),
                "the record leg mints the #512 receiver-carrying kind");
        assertEquals("date", recMint.featureName());
        assertEquals("zonedDateTime", recMint.recordTypeName());
        assertEquals(Optional.empty(), adapter.declineReason(recNav, ws),
                "empty <=> lowers: the nonSymbolReceiver face is gone at this seat");

        // (1b) the RECORD leg, SYNTHESIZED cache-missing twin (the corpus's engine-blind
        // state: the receiver types only through the ATTRIBUTE channel): a detached
        // `<item> -> value -> date` chain parented at the extract lambda, the inner hop
        // linker-bound (the corpus premise — recvLowers:FieldAccess) but never
        // checker-typed (synthesized nodes miss the expression-keyed cache).
        RFunction recFn = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "RecRecv30".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("RecRecv30 not parsed"));
        RInlineFunction recLambda = AstWalker.findAll(recFn, RInlineFunction.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no extract lambda"));
        RAttribute valueAttr = AstWalker.findAll(waveFile, RDataType.class).stream()
                .filter(t -> "Ident30".equals(t.name()))
                .flatMap(t -> t.attributes().stream())
                .filter(a -> "value".equals(a.name())).findFirst()
                .orElseThrow(() -> new AssertionError("Ident30.value not parsed"));
        RImplicitVariable synthItem = new RImplicitVariable();
        synthItem.setSynthetic(true);
        RFeatureCall synthValue = new RFeatureCall();
        synthValue.setFeatureName("value");
        synthValue.setReceiver(synthItem);
        synthValue.setResolvedFeature(valueAttr);
        RFeatureCall synthDate = new RFeatureCall();
        synthDate.setFeatureName("date");
        synthDate.setReceiver(synthValue);
        synthDate.setParent(recLambda);
        // the up-only-parented mechanism: setReceiver never wires the child's parent,
        // and the binder/context walks ascend FROM the inner nodes — wire the chain
        // explicitly so the fixture is structurally faithful (the Copilot #530 R1-C2
        // point; the choice twin below wires its chain the same way).
        synthValue.setParent(synthDate);
        synthItem.setParent(synthValue);
        synthDate.attachToWorkspace(ws);
        RMetaAnnotatedType synthRecvT = ws.getInferredType(synthValue);
        assertTrue(synthRecvT == null || synthRecvT.isMissing(),
                "the fixture's premise: the synthesized receiver is ENGINE-BLIND — only"
                        + " the lowered attribute-channel type can prove the record");
        IRRecordReceiverNav synthRecMint = assertInstanceOf(IRRecordReceiverNav.class,
                adapter.adapt(synthDate, ws).orElseThrow(() -> new AssertionError(
                        "an engine-blind record read must adapt through the #530 arm-B1"
                                + " lowered-type channel")),
                "the cache-missing twin mints the same receiver-carrying kind");
        assertEquals("zonedDateTime", synthRecMint.recordTypeName(),
                "the record fact came from the LOWERED receiver's attribute-channel type");
        assertNotNull(synthRecMint.receiver().type(),
                "the receiver carries its lowered type — the arm's own proof channel");
        assertFalse(synthRecMint.receiver().type().isMissing(),
                "the lowered receiver's attribute-channel type is the record proof (the"
                        + " engine cache read MISSING — the receiver CLASS is whichever"
                        + " standing synthetic-item channel lowered it)");

        // (2a) the CHOICE leg, parsed twin — the ENGINE-TYPED item takes a standing route
        // (the checker types the extract item, the option projects through the by-name
        // channels — the corollary's documentation half: the parsed twin claims, whichever
        // standing channel carries it).
        RFeatureCall choiceNav = navUnder(waveFile, "ChoiceRecv30", "OptA30");
        assertTrue(adapter.adapt(choiceNav, ws).isPresent(),
                "an option selection over an engine-typed item claims through the"
                        + " standing channels");
        assertEquals(Optional.empty(), adapter.declineReason(choiceNav, ws),
                "empty <=> lowers at the parsed seat");

        // (2b) the CHOICE leg, SYNTHESIZED cache-missing twin (the corpus's bareRef
        // state: the item types only through the binder source's DECLARATION): a detached
        // `<item> -> OptA30` parented at the extract lambda — the #530 arm-B2 structural
        // channel (declaredDirectChoiceOf on the binder source).
        RFunction choiceFn = AstWalker.findAll(waveFile, RFunction.class).stream()
                .filter(f -> "ChoiceRecv30".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("ChoiceRecv30 not parsed"));
        RInlineFunction choiceLambda = AstWalker.findAll(choiceFn, RInlineFunction.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no choice extract lambda"));
        RImplicitVariable synthChoiceItem = new RImplicitVariable();
        synthChoiceItem.setSynthetic(false);
        RFeatureCall synthOpt = new RFeatureCall();
        synthOpt.setFeatureName("OptA30");
        synthOpt.setReceiver(synthChoiceItem);
        synthOpt.setParent(choiceLambda);
        // the up-only-parented mechanism: setReceiver never wires the child's parent, and
        // the binder walk (isFilterOrExtractBound) ascends FROM the item — wire the chain
        // explicitly (the #529 structural-parent-placement law).
        synthChoiceItem.setParent(synthOpt);
        synthOpt.attachToWorkspace(ws);
        RMetaAnnotatedType synthItemT = ws.getInferredType(synthChoiceItem);
        assertTrue(synthItemT == null || synthItemT.isMissing(),
                "the fixture's premise: the synthesized item is ENGINE-BLIND — only the"
                        + " binder declaration can prove the choice");
        IRChoiceReceiverNav synthChoiceMint = assertInstanceOf(IRChoiceReceiverNav.class,
                adapter.adapt(synthOpt, ws).orElseThrow(() -> new AssertionError(
                        "an engine-blind option selection must adapt through the #530"
                                + " arm-B2 structural channel")),
                "the cache-missing twin mints the same receiver-carrying kind");
        assertEquals("Pay30", synthChoiceMint.choiceName(),
                "the choice fact came from the binder source's DECLARATION");

        // (3) the QUALIFIER leg, parsed at the corpus's own geometry (the elided-pipe
        // filter over a meta-string element, the read inside a call argument — the
        // interior.RSymbolReference census position): the fc mints the #512 shallow
        // qualifier kind through the standing deep-pipe walk, and the CONTAINING call
        // admits the oracle-leaf argument (the whole chain lowers — a claiming fc under
        // a declining call would be a respell, not a teach).
        RRule qualRule = AstWalker.findAll(waveFile, RRule.class).stream()
                .filter(r -> "QualRecv30".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("QualRecv30 not parsed"));
        RFeatureCall qualNav = AstWalker.findAll(qualRule, RFeatureCall.class).stream()
                .filter(f -> "scheme".equals(f.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("the item -> scheme nav not parsed"));
        assertInstanceOf(IRQualifierItemNav.class,
                adapter.adapt(qualNav, ws).orElseThrow(() -> new AssertionError(
                        "a qualifier read over a meta-string item must adapt")),
                "the qualifier leg mints the #512 shallow kind at the rule seat");
        RSymbolReference strCall = AstWalker.findAll(qualRule, RSymbolReference.class)
                .stream().filter(sr -> "Str30".equals(sr.name()) && !sr.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the Str30(item -> scheme) call"
                        + " not parsed"));
        assertTrue(adapter.adapt(strCall, ws).isPresent(),
                "the containing call admits the qualifier-nav argument — the #530 arg-seat"
                        + " follow-through (the co-block would otherwise respell)");
        assertEquals(Optional.empty(), adapter.declineReason(strCall, ws),
                "empty <=> lowers: the whole filter-predicate chain claims");

        // (3b) the JOIN-LICENSED qualifier read (the #530 arm-B3 + B3b — the corpus's own
        // geometry: a DIVERGENT conditional at the resolved pipe top, its else-branch an
        // UNBOUND rule-seat bare name): each branch's defining feature carries the scheme
        // qualifier, so the license joins where the instance-identity terminal correctly
        // nulls — the drr UnderlyingAssetTradingPlatformIdentifier class.
        RRule joinRule = AstWalker.findAll(waveFile, RRule.class).stream()
                .filter(r -> "QualJoin30".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("QualJoin30 not parsed"));
        RFeatureCall joinNav = AstWalker.findAll(joinRule, RFeatureCall.class).stream()
                .filter(f -> "scheme".equals(f.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("the joined item -> scheme nav"
                        + " not parsed"));
        assertInstanceOf(IRQualifierItemNav.class,
                adapter.adapt(joinNav, ws).orElseThrow(() -> new AssertionError(
                        "a join-licensed qualifier read must adapt since the #530 arm-B3")),
                "the divergent-join license mints the same shallow qualifier kind");
        assertEquals(Optional.empty(), adapter.declineReason(joinNav, ws),
                "empty <=> lowers: the nonSymbolReceiver face is gone at the joined seat");

        // (3c) the join's BOUNDARY NEGATIVE (the sweep-boundary law): an UNLICENSED branch
        // (a plain string feature — no metadata qualifier) keeps the whole join declining;
        // the qualifier read over its elements would have no wrapper to deref.
        RRule joinNegRule = AstWalker.findAll(waveFile, RRule.class).stream()
                .filter(r -> "QualJoinNeg30".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("QualJoinNeg30 not parsed"));
        RFeatureCall joinNegNav = AstWalker.findAll(joinNegRule, RFeatureCall.class).stream()
                .filter(f -> "scheme".equals(f.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("the negative's item -> scheme nav"
                        + " not parsed"));
        assertTrue(adapter.adapt(joinNegNav, ws).isEmpty(),
                "an unlicensed branch keeps the decline — the license is per-branch AND,"
                        + " prove-or-decline");
        assertEquals(Optional.of("featureUnresolved.nonSymbolReceiver"),
                adapter.declineReason(joinNegNav, ws),
                "the honest face stands for the unlicensed-join residue");
    }

    @Test
    void contextSeatRequalifyAndAppliedRuleSourceLowerSinceThe530Teach() {
        // #530 arms A + C (the Seat-1 MF-1 close — the two arms the wave witness above
        // does not carry): the context-typed requalify pair (the KVP seat + the
        // conditional-branch seat, with the census-narrow negatives) and the
        // applied-RULE-callee source leg (the #521 rule-reference law at the applied
        // seat, composed through the standing #487 conditional join).
        String source = """
                namespace "test.step530ac"

                type Thing30:
                    weight number (1..1)

                type Wide30:
                    w string (0..1)

                enum Kind30:
                    Thing30x
                    Wide30

                type Holder30:
                    kind Kind30 (0..1)
                    things Thing30 (0..*)

                func MkHolder30:
                    inputs:
                        flag boolean (1..1)
                    output:
                        outH Holder30 (1..1)
                    set outH:
                        Holder30 {
                            kind: Wide30,
                            ...
                        }

                func PickKind30:
                    inputs:
                        flag boolean (1..1)
                    output:
                        outK Kind30 (0..1)
                    set outK:
                        if flag
                        then Thing30x
                        else Wide30

                func MkHolderSeg30:
                    inputs:
                        flag boolean (1..1)
                    output:
                        outH Holder30 (1..1)
                    set outH -> kind:
                        Wide30

                func CondSeat30:
                    inputs:
                        flag boolean (1..1)
                    output:
                        outK Kind30 (0..1)
                    set outK:
                        if Wide30
                        then Thing30x

                reporting rule ThingsOf30 from Holder30:
                    extract things

                reporting rule PickWeights30 from Holder30:
                    extract
                        ((if kind exists
                        then ThingsOf30(item)
                        else ThingsOf30(item))
                            then extract weight)
                """;
        RModel model = AstBuilder.buildFromString(source, "step530-ac.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        var acFile = ws.files().get(ws.files().size() - 1);
        // The colliding refs bind to the TYPE (the fixture premise), so the standard
        // attribute-bound finder cannot serve them - a local by-name lookup.
        java.util.function.BiFunction<String, String, RSymbolReference> refUnder =
                (fnName, refName) -> {
                    RFunction fn = AstWalker.findAll(acFile, RFunction.class).stream()
                            .filter(f -> fnName.equals(f.name())).findFirst()
                            .orElseThrow(() -> new AssertionError("no function " + fnName));
                    return AstWalker.findAll(fn, RSymbolReference.class).stream()
                            .filter(r -> refName.equals(r.name()) && r.args().isEmpty())
                            .findFirst().orElseThrow(() -> new AssertionError(
                                    "no bare `" + refName + "` under " + fnName));
                };

        // (C-1) the KVP seat: `kind: Wide30` — the bare value name the linker bound to
        // the TYPE Wide30 (the fixture premise — the corpus's context-free bind), the
        // key attribute's declared enum carrying a value of the same name; the requalify
        // mints the #504-builder enum constant.
        RSymbolReference kvpRef = refUnder.apply("MkHolder30", "Wide30");
        assertInstanceOf(RDataType.class, kvpRef.symbol().orElse(null),
                "the fixture's premise: the linker bound the TYPE (the collision class)");
        IRReference kvpMint = assertInstanceOf(IRReference.class,
                adapter.adapt(kvpRef, ws).orElseThrow(() -> new AssertionError(
                        "a KVP-seat colliding value name must requalify since the #530"
                                + " arm-C teach")),
                "the mint is the #504 enum-constant builder");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, kvpMint.referenceKind());
        assertEquals("Wide30", kvpMint.target());
        assertEquals(Optional.empty(), adapter.declineReason(kvpRef, ws),
                "empty <=> lowers: the symbolNotAttribute face is gone at the KVP seat");

        // (C-2) the conditional-BRANCH seat: the else-branch value ascends to the plain
        // un-segmented set target (the enclosing function's own enum output).
        RSymbolReference condRef = refUnder.apply("PickKind30", "Wide30");
        assertInstanceOf(RDataType.class, condRef.symbol().orElse(null),
                "the fixture's premise: the TYPE bind at the branch seat");
        IRReference condMint = assertInstanceOf(IRReference.class,
                adapter.adapt(condRef, ws).orElseThrow(() -> new AssertionError(
                        "a conditional-branch colliding value name must requalify via the"
                                + " set-target ascent since the #530 arm-C teach")),
                "the same builder at the ascended seat");
        assertEquals(IRReference.ReferenceKind.ENUM_VALUE, condMint.referenceKind());
        assertEquals(Optional.empty(), adapter.declineReason(condRef, ws));

        // (C-3) the SEGMENTED-target boundary negative (census-narrow: zero corpus
        // carriers — the walk declines a `set outH -> kind:` seat, prove-or-decline).
        RSymbolReference segRef = refUnder.apply("MkHolderSeg30", "Wide30");
        assertTrue(adapter.adapt(segRef, ws).isEmpty(),
                "a segmented set target keeps the decline — the census-narrow boundary");
        assertEquals(Optional.of("symbolNotAttribute.sym:RDataType.p:ROperation"),
                adapter.declineReason(segRef, ws),
                "the honest face stands at the un-taught seat");

        // (C-4) the CONDITION-seat boundary negative: the walk returns null for the
        // boolean seat (never an expected-enum seat), so the face stands.
        RSymbolReference condSeatRef = refUnder.apply("CondSeat30", "Wide30");
        assertTrue(adapter.adapt(condSeatRef, ws).isEmpty(),
                "the condition seat keeps the decline — the boolean seat is never an"
                        + " expected-enum seat");
        assertEquals(Optional.of("symbolNotAttribute.sym:RDataType.p:RConditionalExpr"),
                adapter.declineReason(condSeatRef, ws),
                "the honest face stands at the condition seat");

        // (A) the applied-RULE-callee source leg, composed through the #487 conditional
        // join (the corpus geometry — SpreadCurrencyOfLeg2's shape): the pipe source is
        // a conditional of APPLIED rule calls; both branches derive on the callee rule's
        // own body (extract things -> Thing30), the join holds by instance identity, and
        // the bare `weight` resolves on the joined element form — the
        // sourceElementUnresolved face is gone.
        RRule weightsRule = AstWalker.findAll(acFile, RRule.class).stream()
                .filter(r -> "PickWeights30".equals(r.name())).findFirst()
                .orElseThrow(() -> new AssertionError("PickWeights30 not parsed"));
        RSymbolReference weightRef = AstWalker.findAll(weightsRule, RSymbolReference.class)
                .stream().filter(sr -> "weight".equals(sr.name()) && sr.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("the bare weight read not parsed"));
        assertTrue(adapter.adapt(weightRef, ws).isPresent(),
                "a bare attr over an applied-rule-call pipe source must adapt since the"
                        + " #530 arm-A teach (the rule-body derivation + the #487 join)");
        assertEquals(Optional.empty(), adapter.declineReason(weightRef, ws),
                "empty <=> lowers: the elided-pipe sourceElementUnresolved face is gone");
    }

    @Test
    void operandResidueSeatsLowerSinceThe528Teach() {
        // #528 (the wave witness — the OPERAND/ARG residue cluster, arms 1-3): the three
        // operand seats the opResGate census decoded at one DECODE each (the position token
        // splits the itemOperand and pfOperand faces at-root vs interior — the Seat-1 #528
        // MF-3 recut). (1) a bare boolean NAVIGATION as a logical operand
        // (operandBareBooleanNav — legacy LogicalHandler coerces every non-ComparisonResult
        // operand ComparisonResult.ofNullSafe inside its own composition, the
        // #504/#510/#511/#520 family); (2) a POINT-FREE reference as a COMPARISON operand
        // (operand:IRPointFreeApply — the kind's last un-admitted BOOLEAN-PRODUCING operand
        // seat, admitted at logical/existence/equality since #504; the arithmetic seat keeps
        // its standing decline); (3) an implicit ITEM bound by a #506-widened
        // then/min/max/sort body as a comparison operand (operandItem — the #502 binder-blind
        // ARG gate applied at the operand seat, the admission lag the census priced
        // argGate:yes). Every one routes through its containing root's oracle serve (the
        // compiler's shape legs), so the renders are legacy's own BY IDENTITY.
        String source = """
                namespace "test.step528"

                type Leg28:
                    flag boolean (1..1)
                    amount number (0..1)

                type Wrap28:
                    legs Leg28 (0..*)
                    marker string (0..1)

                func PfCallee28:
                    inputs:
                        w Wrap28 (1..1)
                    output:
                        out number (1..1)
                    set out:
                        1.0

                func LogNav28:
                    inputs:
                        l Leg28 (1..1)
                        w Wrap28 (1..1)
                    output:
                        out boolean (0..1)
                    set out:
                        l -> flag and w -> marker exists

                func PfOperand28:
                    inputs:
                        w Wrap28 (1..1)
                        n number (1..1)
                    output:
                        out boolean (0..1)
                    set out:
                        n > PfCallee28

                func ItemOperand28:
                    inputs:
                        w Wrap28 (1..1)
                    output:
                        out number (0..1)
                    set out:
                        w -> legs
                            extract [ item -> amount ]
                            then min [ item > 5.0 ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step528-operand-residue.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // (1) the logical seat: the bare boolean nav operand admits, so the whole logical
        // lowers (pre-teach the gate rejected the FieldAccess side and the claim declined).
        RLogicalExpr logical = underFunc(ws, "LogNav28", RLogicalExpr.class);
        BinaryOp loweredLogical = assertInstanceOf(BinaryOp.class,
                adapter.adapt(logical, ws).orElseThrow(() -> new AssertionError(
                        "a logical with a bare boolean nav operand must lower since the #528"
                                + " arm-1 (legacy coerces it ComparisonResult.ofNullSafe)")),
                "the logical lowers to the composed BinaryOp");
        assertInstanceOf(FieldAccess.class, loweredLogical.left(),
                "the LEFT operand is the bare boolean navigation the arm admitted");
        assertEquals(Optional.empty(), adapter.declineReason(logical, ws),
                "the operandBareBooleanNav token retires with the face");

        // (2) the comparison seat: the point-free operand admits.
        RComparisonExpr pfCmp = underFunc(ws, "PfOperand28", RComparisonExpr.class);
        BinaryOp loweredPf = assertInstanceOf(BinaryOp.class,
                adapter.adapt(pfCmp, ws).orElseThrow(() -> new AssertionError(
                        "a comparison with a point-free operand must lower since the #528"
                                + " arm-2 (the kind's last un-admitted boolean-producing"
                                + " operand seat)")),
                "the comparison lowers to the composed BinaryOp");
        assertInstanceOf(IRPointFreeApply.class, loweredPf.right(),
                "the RIGHT operand is the lowered point-free reference");

        // (3) the comparison seat: the min-body-bound item operand admits binder-blind (the
        // pre-#528 gate took the filter/extract pair ONLY, so this exact shape declined).
        RComparisonExpr itemCmp = underFunc(ws, "ItemOperand28", RComparisonExpr.class);
        BinaryOp loweredItem = assertInstanceOf(BinaryOp.class,
                adapter.adapt(itemCmp, ws).orElseThrow(() -> new AssertionError(
                        "a comparison over a min-body-bound item must lower since the #528"
                                + " arm-3 (the #502 binder-blind gate at the operand seat)")),
                "the comparison lowers to the composed BinaryOp");
        IRVariable itemOperand = assertInstanceOf(IRVariable.class, loweredItem.left(),
                "the LEFT operand is the implicit item");
        assertEquals(IRVariable.VariableKind.USER_ITEM, itemOperand.variableKind(),
                "an explicit `item` lowers as USER_ITEM whatever binds it");
    }

    /**
     * v3.3 seat 4 (PR #640) - THE DATA-RULE CONDITION SEATS, the heal the blocker probe aimed (probe-s4.print at
     * f22655948): a bare condition attribute as a comparison operand in BOTH orders (D54 item 3: the operand arm AND the
     * numeric-literal sibling set - the #528 arm-3 precedent), as an existence operand, as a call argument, as the
     * receiver of a chain (`sub -> x`, arm B / arm C by the parse), legacy's synthesized CONDITION-INSTANCE reference
     * (`leg640` inside `type Leg640`: a parentless attribute whose typeCall names the type - the probe's first class,
     * 9,393 sole claims on DRR 7.3) and the `<instance> -> attr` navigation legacy builds over it. Each asserts the
     * lowering; the empty declineReason beside it is the standing "empty <=> lowers" idiom and CANNOT fail after a
     * successful adapt (declineReason opens on adapt's own verdict - PR #640 round 1, cq SF-4), so it is NOT the
     * mirror's control: the mirror legs are locked by population (the blocker probe's `unattributed=0` / `anomalies=0`
     * on every probed DATA_RULE cell, probe-s4h.print) and by the NEGATIVE control below. The NEGATIVE control is a seat the gates still
     * refuse: the arithmetic operand set (`amount + qty > 0` over bare attributes) - a meta-annotated attribute would
     * test the mint, not the admissions.
     */
    @Test
    void dataRuleConditionSeatsLowerSinceThe640Heal() {
        String source = """
                namespace "test.step640"

                type Sub640:
                    x string (0..1)
                    ref string (0..1)
                        [metadata reference]

                type Leg640:
                    amount number (0..1)
                    qty int (0..1)
                    sub Sub640 (0..1)
                    code string (0..1)
                        [metadata scheme]

                    condition Cmp: amount > 0
                    condition EqLit: qty = 0
                    condition EqLitRev: 0 = qty
                    condition MetaEx: code exists
                    condition MetaLeaf: sub -> ref exists
                    condition CmpRev: 0 < amount
                    condition Ex: amount exists
                    condition Call: IsPos640(amount)
                    condition Chain: sub -> x exists
                    condition NegCtrl: amount + qty > 0

                func IsPos640:
                    inputs:
                        n number (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        n > 0
                """;
        RModel model = AstBuilder.buildFromString(source, "step640-condition-seats.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();

        // D54 item 3: the comparison operand arm and its numeric-literal sibling set, both orders
        RExpression cmp = conditionExpr(model, "Leg640", "Cmp");
        BinaryOp loweredCmp = assertInstanceOf(BinaryOp.class, adapter.adapt(cmp, ws).orElseThrow(
                () -> new AssertionError("`amount > 0` must lower since the #640 comparison-operand admission")));
        assertInstanceOf(IRImplicitAttrNav.class, loweredCmp.left(), "the LEFT operand is the bare condition attribute");
        assertEquals(Optional.empty(), adapter.declineReason(cmp, ws), "the operand:IRImplicitAttrNav token retires");
        RExpression cmpRev = conditionExpr(model, "Leg640", "CmpRev");
        BinaryOp loweredRev = assertInstanceOf(BinaryOp.class, adapter.adapt(cmpRev, ws).orElseThrow(
                () -> new AssertionError("`0 < amount` must lower: the numeric-literal SIBLING set gains the kind")));
        assertInstanceOf(IRImplicitAttrNav.class, loweredRev.right(), "the RIGHT operand is the bare condition attribute");
        assertEquals(Optional.empty(), adapter.declineReason(cmpRev, ws),
                "the operandNumericLiteral.sib:IRImplicitAttrNav face retires with the pair");

        // the existence operand set
        RExpression ex = conditionExpr(model, "Leg640", "Ex");
        Existence loweredEx = assertInstanceOf(Existence.class, adapter.adapt(ex, ws).orElseThrow(
                () -> new AssertionError("`amount exists` must lower since the #640 existence-operand admission")));
        assertInstanceOf(IRImplicitAttrNav.class, loweredEx.arg(), "the operand is the bare condition attribute");
        assertEquals(Optional.empty(), adapter.declineReason(ex, ws), "the existence mirror reads the arm's set");

        // the call-argument or-list
        RExpression call = conditionExpr(model, "Leg640", "Call");
        IRApply loweredCall = assertInstanceOf(IRApply.class, adapter.adapt(call, ws).orElseThrow(
                () -> new AssertionError("`IsPos640(amount)` must lower since the #640 call-argument admission")));
        assertInstanceOf(IRImplicitAttrNav.class, loweredCall.args().get(0), "the argument is the bare condition attribute");
        assertEquals(Optional.empty(), adapter.declineReason(call, ws), "the apply mirror reads the arm's or-list");

        // the chain off a bare condition attribute (the receiver admission; arm C when the parse disguises it)
        RExpression chain = conditionExpr(model, "Leg640", "Chain");
        Existence loweredChain = assertInstanceOf(Existence.class, adapter.adapt(chain, ws).orElseThrow(
                () -> new AssertionError("`sub -> x exists` must lower: the bare attribute is a navigation receiver now")));
        FieldAccess hop = assertInstanceOf(FieldAccess.class, loweredChain.arg(), "the chain lowers to a FieldAccess");
        IRImplicitAttrNav chainHead = assertInstanceOf(IRImplicitAttrNav.class, hop.receiver(),
                "whose receiver is the bare condition attribute (an oracle leaf)");
        assertEquals(hop.sourceRange(), chainHead.sourceRange(),
                "arm C re-ranges the head to the chain's own node, link by link (v3.3 seat 5; it carried SourceRange.NONE)");
        assertEquals(Optional.empty(), adapter.declineReason(chain, ws), "the nav mirror reads the same receiver set");

        // arm C: the DISGUISED chain as the corpus carries it - an REnumValueRef `sub -> x` the linker marks
        // resolvedInputFeature (the 4,256 `inputFeatureNav.featureUnresolved.headUnresolved` class on DRR 7.3) with no
        // enclosing function to resolve the head in. The PARSED `Chain` above already has this form (the grammar reads
        // `sub -> x` as an EnumValueRefExpr - lane L3 went RED on the parsed assertion, lanes-s4.print; PR #640 round 1,
        // spec SF-3 = cq SF-5: the first comment said the parse was an RFeatureCall, which was false), so this hand-built
        // node pins the linker-marked variant of the same route; the RFeatureCall route has its own witness below
        // (dataRuleFeatureCallReceiverLowersSinceThe640Heal).
        REnumValueRef disguised = new REnumValueRef();
        disguised.setEnumName("sub");
        disguised.setValueName("x");
        disguised.setParent(chain);
        disguised.setResolvedInputFeature(attributeOf(model, "Leg640", "sub"));
        FieldAccess disguisedHop = assertInstanceOf(FieldAccess.class, adapter.adapt(disguised, ws).orElseThrow(
                () -> new AssertionError("the disguised `sub -> x` chain in a condition must lower since arm C")));
        assertInstanceOf(IRImplicitAttrNav.class, disguisedHop.receiver(),
                "the head resolved against the condition's own type and minted as the oracle leaf");
        assertEquals(Optional.empty(), adapter.declineReason(disguised, ws), "the headUnresolved token retires");

        // legacy's CONDITION-INSTANCE reference, built exactly as ReferenceHandler.syntheticConditionInstanceRef builds it
        RTypeCall instanceTypeCall = new RTypeCall();
        instanceTypeCall.setTypeName("Leg640");
        RAttribute instanceAttr = new RAttribute();
        instanceAttr.setName("leg640");
        instanceAttr.setTypeCall(instanceTypeCall);
        RSymbolReference instanceRef = new RSymbolReference();
        instanceRef.setName("leg640");
        instanceRef.setParent(ex);
        instanceRef.setResolvedSymbol(instanceAttr);
        IRConditionInstance instance = assertInstanceOf(IRConditionInstance.class, adapter.adapt(instanceRef, ws).orElseThrow(
                () -> new AssertionError("the synthesized condition-instance reference must lower to the shallow leaf")));
        assertEquals("Leg640", instance.typeName(), "typed as the owning data type");
        assertEquals(Optional.empty(), adapter.declineReason(instanceRef, ws),
                "the attrOutsideFunction.noFilterExtractBinder.untyped token retires (the mirror claims it)");
        RFeatureCall instanceNav = new RFeatureCall();
        instanceNav.setReceiver(instanceRef);
        instanceNav.setFeatureName("amount");
        instanceNav.setResolvedFeature(attributeOf(model, "Leg640", "amount"));
        instanceNav.setParent(ex);
        FieldAccess instanceHop = assertInstanceOf(FieldAccess.class, adapter.adapt(instanceNav, ws).orElseThrow(
                () -> new AssertionError("legacy's `<instance> -> amount` navigation must lower over the instance leaf")));
        assertInstanceOf(IRConditionInstance.class, instanceHop.receiver(), "the receiver is the condition instance");
        assertEquals(Optional.empty(), adapter.declineReason(instanceNav, ws), "the nav mirror admits the instance receiver");

        // arm G: the equality seat's numeric-literal sibling pair, both orders
        for (String name : new String[] {"EqLit", "EqLitRev"}) {
            RExpression eq = conditionExpr(model, "Leg640", name);
            assertInstanceOf(BinaryOp.class, adapter.adapt(eq, ws).orElseThrow(
                    () -> new AssertionError(name + " must lower: the equality seat's literal-sibling set gains the kind")));
            assertEquals(Optional.empty(), adapter.declineReason(eq, ws),
                    "the operandNumericLiteral.sib:IRImplicitAttrNav face retires at the equality seat too: " + name);
        }

        // arm E: the META-annotated bare attribute joins the mint; arm F: its meta feature over the head
        RExpression metaEx = conditionExpr(model, "Leg640", "MetaEx");
        Existence loweredMetaEx = assertInstanceOf(Existence.class, adapter.adapt(metaEx, ws).orElseThrow(
                () -> new AssertionError("`code exists` over a [metadata scheme] attribute must lower since arm E")));
        IRImplicitAttrNav metaLeaf = assertInstanceOf(IRImplicitAttrNav.class, loweredMetaEx.arg(),
                "the meta-annotated attribute mints the same shallow leaf");
        assertNotNull(metaLeaf.type(), "typed");
        assertEquals(Optional.empty(), adapter.declineReason(metaEx, ws), "empty <=> lowers (the mirror reads the arm's own mint-type reader BY CALL)");
        RExpression metaLeafChain640 = conditionExpr(model, "Leg640", "MetaLeaf");
        Existence loweredMetaLeaf = assertInstanceOf(Existence.class, adapter.adapt(metaLeafChain640, ws).orElseThrow(
                () -> new AssertionError("`sub -> ref exists` (a [metadata reference] leaf) must lower since arm F")));
        assertInstanceOf(IRMetaAccess.class, loweredMetaLeaf.arg(), "the hop over a meta-annotated leaf is a meta access");
        IRImplicitAttrNav metaHead = assertInstanceOf(IRImplicitAttrNav.class, ((IRMetaAccess) loweredMetaLeaf.arg()).receiver(),
                "over the condition-attribute head (the corpus's inputFeatureNav.metaFeature class)");
        assertEquals(loweredMetaLeaf.arg().sourceRange(), metaHead.sourceRange(),
                "arm F re-ranges the head to the chain's own node, link by link (v3.3 seat 5)");
        assertEquals(Optional.empty(), adapter.declineReason(metaLeafChain640, ws), "the inputFeatureNav.metaFeature token retires");

        // the NEGATIVE control: the arithmetic operand set still refuses the kind, so the comparison declines whole
        RExpression neg = conditionExpr(model, "Leg640", "NegCtrl");
        assertEquals(Optional.empty(), adapter.adapt(neg, ws),
                "`amount + qty > 0` stays declined: the arithmetic seat admits no bare condition attribute (the control)");
        assertEquals(Optional.of("operandNotExpressible"), adapter.declineReason(neg, ws),
                "the comparison's left operand (the arithmetic) is the honest stop");
    }

    /**
     * v3.3 seat 4 (PR #640 round 1, spec SF-3 = cq SF-5) - THE RFeatureCall-SHAPED RECEIVER WITNESS: the probe's third
     * class ({@code RFeatureCall:receiver:IRImplicitAttrNav}, 3,269 sole claims on DRR 7.3, probe-s4.print) is an
     * {@link RFeatureCall} whose receiver is the BARE condition attribute - the form the adapter's own
     * {@code disguisedInputNavEquivalent} and legacy's handlers build, never the grammar (which reads {@code sub -> x}
     * as an EnumValueRefExpr). Built by hand over the fixture's own attributes, in its OWN method so its lanes read
     * alone (lanes-s4c6.sh): arm B's receiver admission removed -> RED here; arm C's head resolution removed -> still
     * GREEN (the route does not ride the disguised arm).
     */
    @Test
    void dataRuleFeatureCallReceiverLowersSinceThe640Heal() {
        String source = """
                namespace "test.step640b"

                type Sub640b:
                    x string (0..1)

                type Leg640b:
                    sub Sub640b (0..1)

                    condition Ex: sub exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step640b-featurecall-receiver.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RExpression ex = conditionExpr(model, "Leg640b", "Ex");

        RSymbolReference bareSub = new RSymbolReference();
        bareSub.setName("sub");
        bareSub.setResolvedSymbol(attributeOf(model, "Leg640b", "sub"));
        RFeatureCall nav = new RFeatureCall();
        nav.setReceiver(bareSub);
        nav.setFeatureName("x");
        nav.setResolvedFeature(attributeOf(model, "Sub640b", "x"));
        nav.setParent(ex);
        bareSub.setParent(nav);

        FieldAccess hop = assertInstanceOf(FieldAccess.class, adapter.adapt(nav, ws).orElseThrow(
                () -> new AssertionError("an RFeatureCall over the bare condition attribute must lower since arm B"
                        + " (the receiver:IRImplicitAttrNav class)")));
        assertInstanceOf(IRImplicitAttrNav.class, hop.receiver(), "the receiver is the bare condition attribute (an oracle leaf)");
        assertEquals("x", hop.feature(), "the hop names the feature");
    }

    /**
     * v3.3 seat 4 (PR #640 round 1, cq SF-1) - THE KEYWORD-NAMED CONDITION INSTANCE: legacy names the condition method's
     * parameter through {@code HandlerHelper.conditionInstanceName} -> {@code JavaNamingUtil.escapeJavaKeyword}, which
     * PREFIXES a Java keyword ({@code type Volatile} -> {@code _volatile}); the first cut accepted a TRAILING underscore
     * legacy never writes, so a keyword-named type's instance reference could never match. No vendored or adversarial
     * cell declares a keyword-named type today (the seat's sweep of all 26 cells), so this is the only witness.
     */
    @Test
    void keywordNamedConditionInstanceMatchesLegacysPrefixEscape() {
        String source = """
                namespace "test.step640c"

                type Volatile:
                    amount number (0..1)

                    condition Ex: amount exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step640c-keyword-instance.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RExpression ex = conditionExpr(model, "Volatile", "Ex");

        IRConditionInstance instance = assertInstanceOf(IRConditionInstance.class,
                adapter.adapt(syntheticInstanceRef("Volatile", "_volatile", ex), ws).orElseThrow(
                        () -> new AssertionError("legacy's `_volatile` (the PREFIX escape) must lower to the instance leaf")));
        assertEquals("Volatile", instance.typeName(), "typed as the owning data type");
        assertEquals(Optional.empty(), adapter.adapt(syntheticInstanceRef("Volatile", "volatile_", ex), ws),
                "`volatile_` is a spelling legacy never writes: it must NOT be read as the condition instance");
        assertEquals(Optional.empty(), adapter.adapt(syntheticInstanceRef("Volatile", "volatile", ex), ws),
                "nor is the unescaped keyword");
    }

    /**
     * v3.3 seat 5 (the banked cq SF-4 of PR #640) - THE ONE #640 MIRROR LEG A TEST CAN REACH, AND WHY THE OTHERS CANNOT BE:
     * {@code declineReason} runs a node's mirror only when the node DECLINES, so a copied mirror leg is observable only in
     * a MULTI-operand mirror where ANOTHER operand is the honest stop - the call-argument or-list. Here the first argument
     * is the bare condition attribute (admitted since #640 arm D) and the second is the arithmetic the gates still refuse:
     * the mirror must WALK PAST the admitted argument and name the second. Without the mirror's
     * {@code arg instanceof IRImplicitAttrNav} leg it stops at the first and mints the retired {@code arg:} face
     * (lanes-s5c2.sh M1: RED here). The SINGLE-operand and LEAF legs of #640 (the bare-attribute mint's mirror, the
     * disguised-head and meta-leaf acceptances, the existence re-sync) are unreachable while arm and mirror agree - the arm
     * lowers the node and its mirror never runs - so they are DRIFT-NAMING belts: their lock is the blocker probe's
     * {@code unattributed=0} / {@code anomalies=0} over the corpus, not a unit assertion. The comparison, equality,
     * existence-operand and nav-receiver mirrors share the arm's predicate BY CALL and need no twin.
     */
    @Test
    void callArgumentMirrorWalksPastTheAdmittedBareAttribute640() {
        String source = """
                namespace "test.step640d"

                type Leg640d:
                    amount number (0..1)
                    qty int (0..1)

                    condition Call2: IsBoth640(amount, amount + qty)

                func IsBoth640:
                    inputs:
                        a number (0..1)
                        b number (0..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        a > b
                """;
        RModel model = AstBuilder.buildFromString(source, "step640d-call-arg-mirror.rosetta");
        RWorkspace ws = RWorkspace.build(List.of(model)).workspace();
        RExpression call = conditionExpr(model, "Leg640d", "Call2");
        assertEquals(Optional.empty(), adapter.adapt(call, ws),
                "the call declines: its SECOND argument is arithmetic over bare condition attributes (the standing refusal)");
        assertEquals(Optional.of("argNotExpressible"), adapter.declineReason(call, ws),
                "the mirror walks PAST the admitted first argument and names the second - without its #640 leg it stops at"
                        + " the first and mints the retired arg: face");
    }

    /** Legacy's synthesized condition-instance reference, as {@code ReferenceHandler.syntheticConditionInstanceRef} builds it. */
    private static RSymbolReference syntheticInstanceRef(String typeName, String refName, RExpression parent) {
        RTypeCall typeCall = new RTypeCall();
        typeCall.setTypeName(typeName);
        RAttribute attr = new RAttribute();
        attr.setName(refName);
        attr.setTypeCall(typeCall);
        RSymbolReference ref = new RSymbolReference();
        ref.setName(refName);
        ref.setParent(parent);
        ref.setResolvedSymbol(attr);
        return ref;
    }

    /** The named condition's expression under the named data type. */
    private static RExpression conditionExpr(RModel model, String typeName, String conditionName) {
        RDataType type = model.rootElements().stream()
                .filter(e -> e instanceof RDataType dt && typeName.equals(dt.name()))
                .map(e -> (RDataType) e).findFirst().orElseThrow(() -> new AssertionError("no type " + typeName));
        return type.conditions().stream()
                .filter(c -> c.name().isPresent() && conditionName.equals(c.name().get()))
                .findFirst().orElseThrow(() -> new AssertionError("no condition " + conditionName)).expression();
    }

    /** The named attribute of the named data type. */
    private static RAttribute attributeOf(RModel model, String typeName, String attrName) {
        RDataType type = model.rootElements().stream()
                .filter(e -> e instanceof RDataType dt && typeName.equals(dt.name()))
                .map(e -> (RDataType) e).findFirst().orElseThrow();
        return type.attributes().stream().filter(a -> attrName.equals(a.name())).findFirst().orElseThrow();
    }

    /** The unique node of the given class under the named function. */
    private <T extends RExpression> T underFunc(RWorkspace ws, String funcName, Class<T> type) {
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> funcName.equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no function " + funcName));
        List<T> nodes = AstWalker.findAll(fn, type);
        if (nodes.size() != 1) {
            // The Copilot #528 R2 suppressed catch: "unique" must be ENFORCED, not assumed —
            // a fixture growing a second node of the type would silently shift the witness.
            throw new AssertionError(nodes.size() + " " + type.getSimpleName() + " nodes under "
                    + funcName + " — the fixture must keep the witness unique");
        }
        return nodes.get(0);
    }

    /** The unique attribute-bound bare reference with the given name under the named function. */
    private RSymbolReference bareRefUnder(RWorkspace ws, String funcName, String attrName) {
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> funcName.equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no function " + funcName));
        return AstWalker.findAll(fn, RSymbolReference.class).stream()
                .filter(r -> attrName.equals(r.name()) && r.args().isEmpty()
                        && r.symbol().orElse(null) instanceof RAttribute)
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no attribute-bound bare `" + attrName + "` under " + funcName));
    }

    /** The unique disguised {@code head -> leaf} chain under the named function. */
    private REnumValueRef chainUnder(RWorkspace ws, String funcName, String head, String leaf) {
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> funcName.equals(f.name()))
                .findFirst().orElseThrow(() -> new AssertionError("no function " + funcName));
        return AstWalker.findAll(fn, REnumValueRef.class).stream()
                .filter(e -> head.equals(e.enumName()) && leaf.equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "no disguised `" + head + " -> " + leaf + "` under " + funcName));
    }
}
