package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.enums.CardCheckOp;
import com.regnosys.rosetta.ast.enums.CompOp;
import com.regnosys.rosetta.ast.enums.EqOp;
import com.regnosys.rosetta.ast.enums.ExistenceOp;
import com.regnosys.rosetta.ast.enums.LogOp;
import com.regnosys.rosetta.ast.enums.Necessity;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RCardinality;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.expr.BinaryOp;
import com.regnosys.rosetta.ir.expr.Existence;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.IRListOp;
import com.regnosys.rosetta.ir.expr.IRLiteral;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.NodeId;
import com.regnosys.rosetta.ir.expr.Optionality;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RNumberType;
import com.regnosys.rosetta.types.RRecordType;
import com.regnosys.rosetta.types.RStringType;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the Wave-0 leaf flips: {@link IRExpressionCompiler} emits literal leaves
 * <em>from the IR</em> (not via legacy fallback) and the bytes are exactly the legacy
 * {@code LiteralHandler} forms.
 *
 * <p>Two things are asserted together, because byte-equality alone cannot distinguish
 * "IR drove it" from "fell back to legacy" (the outputs are identical by design): the
 * rendered Java is correct, AND {@link IRExpressionCompiler#irDrivenCount()} increments —
 * proving the IR path actually fired. The full-corpus byte-parity guarantee is the
 * {@code Path2ByteIdentityTest} FUNCTION rows; this is the focused non-vacuity proof.
 */
class IRExpressionCompilerTest {

    private static final Path BUILTINS_ROOT =
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model");

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

    private static IRExpressionCompiler compiler(RWorkspace ws) {
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        GeneratorModel gm = new GeneratorModel(ws, model -> true);
        return new IRExpressionCompiler(gm, new JavaTypeTranslator(typeUtil), typeUtil);
    }

    @Test
    void stringAndBooleanLiteralsAreIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        IRExpressionCompiler c = compiler(workspace());

        assertEquals("MapperS.of(\"hello\")", render(c, string("hello")));
        assertEquals(1, c.irDrivenCount(), "string literal must be emitted from the IR");

        assertEquals("MapperS.of(true)", render(c, bool(true)));
        assertEquals("MapperS.of(false)", render(c, bool(false)));
        assertEquals(3, c.irDrivenCount(), "both boolean literals must be emitted from the IR");
    }

    @Test
    void stringEscapingMatchesLegacy() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());

        // A quote, a backslash and a tab must be Java-escaped exactly as LiteralHandler does.
        assertEquals("MapperS.of(\"a\\\"b\")", render(c, string("a\"b")));
        assertEquals("MapperS.of(\"x\\\\y\")", render(c, string("x\\y")));
        assertEquals("MapperS.of(\"p\\tq\")", render(c, string("p\tq")));
        assertEquals(3, c.irDrivenCount());
    }

    @Test
    void numberAndEmptyLiteralsAreIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());

        assertEquals("MapperS.of(new BigDecimal(\"3.14\"))", render(c, number("3.14")));
        // Plain (non-scientific) rendering is preserved via toPlainString.
        assertEquals("MapperS.of(new BigDecimal(\"1000\"))", render(c, number("1E3")));
        assertEquals("null", render(c, new REmptyLiteral()));
        assertEquals(3, c.irDrivenCount(), "number and empty literals must be emitted from the IR");
    }

    @Test
    void integerLiteralsRespectContextAndMagnitude() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());
        JavaType bigDecimal = HandlerHelper.BIG_DECIMAL;

        // int / unknown context: bare int; the long band carries the LOWERCASE l on BOTH routes since v3.2 seat 13
        // (D53, the M5c heal: the oracle group conv-bigint-alias-long pins the released plugin's
        // `MapperS.of(123456789012l)`, and the leaf emitter renders through LiteralHandler.intLiteralCode, the ONE
        // render - LAW 77 by identity). The uppercase pin that stood here had no golden; the s13b chain's
        // rune-ir-java step caught it (seat 13 commit 5 - the verify gate had installed this module with tests skipped).
        assertEquals("MapperS.of(42)", render(c, intLiteral(42)));
        assertEquals("MapperS.of(5000000000l)", render(c, intLiteral(5_000_000_000L)));
        // BigDecimal (number) context: BigDecimal.valueOf, lowercase l on the long band.
        assertEquals("MapperS.of(BigDecimal.valueOf(42))", render(c, intLiteral(42), bigDecimal));
        assertEquals("MapperS.of(BigDecimal.valueOf(5000000000l))", render(c, intLiteral(5_000_000_000L), bigDecimal));
        assertEquals(4, c.irDrivenCount(), "in-long-range integer literals must be emitted from the IR");
    }

    @Test
    void beyondLongIntegerServesAtTheEmitterFrontier() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());
        c.setBlockerProbeForTest(true);

        // #515 THE EMITTER-FRONTIER SERVE witness (recut from the pre-#515 falls-back lock):
        // 2^64 exceeds long range, so the leaf emitter still declines the lowered literal
        // (the BigInteger hoist has no arm) — but the decline site now serves the decline's
        // OWN fallback (the literal super.visitIntLiteral line the caller's orElseGet would
        // run — byte-identical BY IDENTITY, legacy's BigInteger hoist render) and counts the
        // claim LOWERED (an IR lowering EXISTS — the #507 accounting law; only the counter
        // moves). The flip receipt and the census's .served row both read the serve.
        BigInteger beyondLong = BigInteger.ONE.shiftLeft(64);
        assertEquals("MapperS.of(new BigInteger(\"" + beyondLong + "\"))", render(c, intLiteral(beyondLong)));
        assertEquals(1, c.irDrivenCount(),
                "a beyond-long integer is SERVED at the emitter frontier since #515 — driven,"
                        + " with legacy's own bytes");
        assertEquals(1, c.emitterServeLoweredCount(), "the serve's flip receipt");
        assertEquals(0, c.irDeclinedCount(), "a served claim records no decline");
        assertEquals("RIntLiteral:root:LITERAL.served=1", c.leafEmitterGapBreakdown(),
                "the gap census names the served row (Σ served ≡ the receipt by construction)");
    }

    @Test
    void scalarFunctionParameterIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // The references-tier leaf flip: a bare scalar function-parameter reference is
        // emitted from the IR as MapperS.of(name) — byte-identical to ReferenceHandler arm 7.
        assertEquals("MapperS.of(foo)", render(c, scalarParamRef("foo", ws)));
        assertEquals(1, c.irDrivenCount(), "a scalar function-parameter reference must be emitted from the IR");
    }

    @Test
    void multiCardinalityParameterRendersMapperCWitnessFromTheIR() {
        // A MULTI parameter renders MapperC.<Item>of(name) — the witness <Item> is the IR node's item
        // type() mapped via the translator (mirrors ReferenceHandler.mapperCOfWrapWitness's
        // toJavaReferenceType(itemType)). Built directly with a string item type → the String witness.
        RMetaAnnotatedType itemType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        IRVariable multi = new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                itemType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(multi, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperC.<String>of(xs)", out.renderToString());
    }

    @Test
    void superCallIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());

        // super → super.doEvaluate(), emitted from IRReference{SUPER} (context-free). No CDM
        // function uses super, so the FUNCTION gate cannot exercise it — this is the proof.
        assertEquals("super.doEvaluate()", render(c, new RSuperCall()));
        assertEquals(1, c.irDrivenCount(), "a super call must be emitted from the IR");
    }


    /**
     * A synthetic fixture enum PARENTED into a minimal declaring model. Since
     * v3.1 C0 (PR #566) {@code JavaTypeTranslator.caseEnumType} REFUSES an
     * enum whose declaring model is unreachable rather than typing it as
     * Object — the refusal contract behaving as designed, which makes a
     * detached fixture enum malformed input. The eight-suite chain had not
     * run between that contract landing and the C1 close, so the breakage
     * surfaced at the #567 close-out chain run (7 tests) and every synthetic
     * enum here now carries its declaring model.
     */
    private static REnumeration fixtureEnum(String name) {
        RModel declaring = new RModel();
        declaring.setNamespace("test.fixture");
        REnumeration en = new REnumeration();
        en.setName(name);
        en.setParent(declaring);
        declaring.rootElements().add(en);
        return en;
    }

    @Test
    void enumValueReferenceRendersQualifiedConstantFromTheIR() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // The enum-value emitter reads only IR facts: the enum simple name from the node's enum
        // type() (an REnumTypeRef, whose name() is the enumeration name) and the raw value from
        // target() — convertValue (formatEnumName∘stripEscape) and the import are its Java concern.
        // Build the IR node directly (no Cat-13 inference) and assert the rendered bytes, mirroring
        // ReferenceHandler's en.name() + "." + convertValue(ev).
        REnumeration en = fixtureEnum("FooEnum");
        en.attachToWorkspace(ws);
        RMetaAnnotatedType enumType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
        IRReference ref = new IRReference("Cash", IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT,
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(ref, null, typeUtil, translator).orElseThrow());
        assertEquals("FooEnum.CASH", out.renderToString());
    }

    @Test
    void equalityWithBooleanAndEnumOperandsRendersFromTheIR() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        // A SINGLE parameter renders MapperS.of(name) independent of its type, so a shared placeholder type is fine.
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        // `flag = True` — the BOOLEAN literal already renders the wrapped MapperS.of(true); no enum wrap.
        IRVariable flag = new IRVariable("flag", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral trueLit = new IRLiteral(IRLiteral.LiteralKind.BOOLEAN, Boolean.TRUE, NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp boolEq = new BinaryOp(BinaryOp.BinOp.EQ, flag, trueLit, NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("areEqual(MapperS.of(flag), MapperS.of(true), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(boolEq, null, typeUtil, translator).orElseThrow()).renderToString());

        // `code = "FOO"` — a STRING literal operand renders MapperS.of("FOO"); like the boolean, no coercion and the
        // enum wrap is a no-op on it (it is an IRLiteral, not an IRReference{ENUM_VALUE}).
        IRVariable code = new IRVariable("code", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral fooLit = new IRLiteral(IRLiteral.LiteralKind.STRING, "FOO", NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp strEq = new BinaryOp(BinaryOp.BinOp.EQ, code, fooLit, NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("areEqual(MapperS.of(code), MapperS.of(\"FOO\"), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(strEq, null, typeUtil, translator).orElseThrow()).renderToString());

        // `status = FooEnum.Cash` — the bare enum operand renders FooEnum.CASH; the equality emitter wraps it
        // MapperS.of(FooEnum.CASH) (legacy HandlerHelper.wrapEnumOperand). The sibling `status` is the SAME enum
        // (FooEnum) so no requalification fires. `<>` → notEqual + the Any default.
        REnumeration en = fixtureEnum("FooEnum");
        en.attachToWorkspace(ws);
        RMetaAnnotatedType enumType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
        IRVariable status = new IRVariable("status", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRReference cash = new IRReference("Cash", IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT.child(1),
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp enumEq = new BinaryOp(BinaryOp.BinOp.EQ, status, cash, NodeId.ROOT,
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("areEqual(MapperS.of(status), MapperS.of(FooEnum.CASH), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(enumEq, null, typeUtil, translator).orElseThrow()).renderToString());
        BinaryOp enumNeq = new BinaryOp(BinaryOp.BinOp.NEQ, status, cash, NodeId.ROOT,
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("notEqual(MapperS.of(status), MapperS.of(FooEnum.CASH), CardinalityOperator.Any)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(enumNeq, null, typeUtil, translator).orElseThrow()).renderToString());

        // The INHERITED-enum REQUALIFICATION decline (enumOperandRequalifies — an enum operand whose Java enum
        // class differs from the sibling's, where legacy requalifies to the child enum) is verified by the CORPUS
        // byte gate, not here: synthetic test REnumerations with no namespace map to indistinguishable Java classes
        // via the translator, so a unit-level FooEnum-vs-BarEnum mismatch cannot be exercised. The 4 drr
        // Create_OrganisationIdentification15Choice__2/__3 functions (idFormat: PartyIdentifierFormat2Enum =
        // LeiIdentifierFormatEnum.LEI, an inherited value) are the real oracle — they diverged before the guard and
        // are byte-identical after it (adversarial-review catch; FUNCTION gate 2219/2219).
    }

    @Test
    void aliasReferenceIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // An alias/shortcut reference renders aliasName(input1, input2, …) — the helper-method call
        // threading the enclosing function's inputs in declaration order, NO MapperS.of wrap, NO refs —
        // byte-identical to ReferenceHandler's alias arm. The input threading + name disambiguation are
        // computed compiler-side from the enclosing function (kept off the neutral IR), so the IR drives
        // the whole invocation. No same-named function dependency here, so the name is undisambiguated.
        assertEquals("myAlias(a, b)", render(c, aliasRef("myAlias", ws, "a", "b")));
        assertEquals(1, c.irDrivenCount(), "an alias reference must be emitted from the IR");
    }

    /**
     * A reference to a function shortcut/alias, wired into a fresh plain function whose inputs are
     * {@code inputNames} (in declaration order). The ref's parent is the function so
     * {@code findEnclosingFunction} resolves the inputs the alias invocation threads.
     */
    private static RSymbolReference aliasRef(String aliasName, RWorkspace ws, String... inputNames) {
        RFunction func = new RFunction();
        func.setName("MyFunc");
        for (String in : inputNames) {
            RAttribute a = new RAttribute();
            a.setName(in);
            a.setCardinality(cardinality(1, false));
            func.inputs().add(a);
            a.setParent(func);
            a.attachToWorkspace(ws);
        }
        RShortcut shortcut = new RShortcut();
        shortcut.setName(aliasName);
        func.shortcuts().add(shortcut);
        shortcut.setParent(func);
        RSymbolReference ref = new RSymbolReference();
        ref.setName(aliasName);
        ref.setResolvedSymbol(shortcut);
        ref.setParent(func);
        func.attachToWorkspace(ws);
        shortcut.attachToWorkspace(ws);
        ref.attachToWorkspace(ws);
        return ref;
    }

    @Test
    void pointFreeCallArgDrivesThroughTheOracleRenderer() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // #492 — the point-free teach's callArg face: `Outer(BareF)` — the arg is a bare no-arg
        // reference to a function, which the adapter lowers to IRPointFreeApply and the emitter
        // renders through the range-correlated PointFreeRenderer reusing legacy
        // renderImplicitFunctionInvocation VERBATIM; the arg then passes RAW (the mirror of
        // legacy's argIsBareFnInvocation branch — the leg-2 ring's 151-carrier .get() class).
        // Byte-identity vs the plain legacy compiler is the whole assertion; the drive counter
        // proves the claim took the IR path (the leg-2 ring read the mass at leafEmitter — this
        // pin reproduces the seat unit-side).
        RFunction bareCallee = new RFunction();
        bareCallee.setName("BareF");
        RAttribute bareIn = new RAttribute();
        bareIn.setName("q");
        bareIn.setCardinality(cardinality(1, false));
        bareCallee.inputs().add(bareIn);
        bareIn.setParent(bareCallee);
        RAttribute bareOut = new RAttribute();
        bareOut.setName("bOut");
        bareOut.setCardinality(cardinality(1, false));
        bareCallee.setOutput(bareOut);
        bareOut.setParent(bareCallee);
        bareCallee.attachToWorkspace(ws);

        RFunction outerCallee = new RFunction();
        outerCallee.setName("Outer");
        RAttribute outerIn = new RAttribute();
        outerIn.setName("p");
        outerIn.setCardinality(cardinality(1, false));
        outerCallee.inputs().add(outerIn);
        outerIn.setParent(outerCallee);
        RAttribute outerOut = new RAttribute();
        outerOut.setName("oOut");
        outerOut.setCardinality(cardinality(1, false));
        outerCallee.setOutput(outerOut);
        outerOut.setParent(outerCallee);
        outerCallee.attachToWorkspace(ws);

        RSymbolReference bareArg = new RSymbolReference();
        bareArg.setName("BareF");
        bareArg.setResolvedSymbol(bareCallee);
        RSymbolReference call = new RSymbolReference();
        call.setName("Outer");
        call.setResolvedSymbol(outerCallee);
        call.args().add(bareArg);
        bareArg.setParent(call);
        // Distinct REAL ranges — the correlation keys the emitters index by. A
        // sentinel NONE range is deliberately NOT indexed (the Copilot #492
        // collision catch: synthetic nodes all default to NONE and would collide
        // on the one key), exactly as every parsed site carries its own range.
        call.setSourceRange(new SourceRange("fixture.rosetta", 1, 1, 1, 20, 0, 19));
        bareArg.setSourceRange(new SourceRange("fixture.rosetta", 1, 7, 1, 12, 6, 11));
        call.attachToWorkspace(ws);
        bareArg.attachToWorkspace(ws);

        String irRouted = renderScoped(c, call);
        String legacy = legacyRenderScoped(legacyCompiler(ws), call);
        assertEquals(legacy, irRouted,
                "a call with a point-free arg must render byte-identically (the RAW arg pass)");
        assertEquals(0, c.irDeclinedCount(),
                "the point-free callArg face must not decline (the leg-2 seat)");
        assertTrue(c.irDrivenCount() >= 1, "the call claim must drive through the IR path");
    }

    @Test
    void pointFreeAmbiguousRangeCollisionDeclines() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // The Copilot #492 catch, pinned: TWO DISTINCT point-free sites sharing one range key
        // (both left at the materialized-node SourceRange.NONE default) poison the correlation
        // index — the claim must DECLINE to legacy (never guess which site a NONE-ranged
        // IRPointFreeApply meant), byte-identically. The single-NONE-site face stays admitted
        // (the corpus's parser-materialized population) — this pin is the ambiguity boundary.
        RFunction firstCallee = new RFunction();
        firstCallee.setName("FirstF");
        RAttribute firstOut = new RAttribute();
        firstOut.setName("fOut");
        firstOut.setCardinality(cardinality(1, false));
        firstCallee.setOutput(firstOut);
        firstOut.setParent(firstCallee);
        firstCallee.attachToWorkspace(ws);

        RFunction secondCallee = new RFunction();
        secondCallee.setName("SecondF");
        RAttribute secondOut = new RAttribute();
        secondOut.setName("sOut");
        secondOut.setCardinality(cardinality(1, false));
        secondCallee.setOutput(secondOut);
        secondOut.setParent(secondCallee);
        secondCallee.attachToWorkspace(ws);

        RFunction pairCallee = new RFunction();
        pairCallee.setName("Pair");
        RAttribute pairInA = new RAttribute();
        pairInA.setName("a");
        pairInA.setCardinality(cardinality(1, false));
        pairCallee.inputs().add(pairInA);
        pairInA.setParent(pairCallee);
        RAttribute pairInB = new RAttribute();
        pairInB.setName("b");
        pairInB.setCardinality(cardinality(1, false));
        pairCallee.inputs().add(pairInB);
        pairInB.setParent(pairCallee);
        RAttribute pairOut = new RAttribute();
        pairOut.setName("pOut");
        pairOut.setCardinality(cardinality(1, false));
        pairCallee.setOutput(pairOut);
        pairOut.setParent(pairCallee);
        pairCallee.attachToWorkspace(ws);

        RSymbolReference firstArg = new RSymbolReference();
        firstArg.setName("FirstF");
        firstArg.setResolvedSymbol(firstCallee);
        RSymbolReference secondArg = new RSymbolReference();
        secondArg.setName("SecondF");
        secondArg.setResolvedSymbol(secondCallee);
        RSymbolReference pairCall = new RSymbolReference();
        pairCall.setName("Pair");
        pairCall.setResolvedSymbol(pairCallee);
        pairCall.args().add(firstArg);
        pairCall.args().add(secondArg);
        firstArg.setParent(pairCall);
        secondArg.setParent(pairCall);
        // Deliberately NO setSourceRange anywhere: all three references share the
        // SourceRange.NONE default — the two point-free args collide on the one key.
        pairCall.attachToWorkspace(ws);
        firstArg.attachToWorkspace(ws);
        secondArg.attachToWorkspace(ws);

        String irRouted = renderScoped(c, pairCall);
        String legacy = legacyRenderScoped(legacyCompiler(ws), pairCall);
        assertEquals(legacy, irRouted,
                "an ambiguous point-free collision must fall back to legacy byte-identically");
        // #515 recut: the emitter's decline on the poisoned key is SERVED at the emitter
        // frontier (the RSymbolReference raw family — the decline's own super.visitSymbolReference
        // fallback, byte-identical BY IDENTITY), so the never-guess-a-site mechanism now reads
        // as the serve receipt + legacy's own bytes rather than a surfaced decline: the
        // emitter still refused to guess (the render is legacy's, not a guessed IR site).
        assertEquals(0, c.irDeclinedCount(),
                "the poisoned-key refusal serves at the emitter frontier — no decline surfaces");
        assertTrue(c.emitterServeLoweredCount() >= 1,
                "the serve receipt names the refusal's route (the poisoned key never guessed)");
    }

    @Test
    void untargetedFamilyVisitIsCountedWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // The #475 census's UNTARGETED axis — the exemplar recut at #500 (RListLiteral moved
        // to the targeted set), at #513 (the noAdaptArm sweep moved `sort`) and again at
        // #515 (the untargeted close moved `join`), so the meter's mechanism is pinned on a
        // still-untargeted family (the cardinality-check — the count-then-super meter counts
        // the visit and the render is the EXACT super call, the twin-render inertness proof;
        // the hand-buildable one of the last two unarmed families — a hand-built reduce
        // cannot render in this null-scope harness, the #513 note).
        RCardinalityCheckExpr check = new RCardinalityCheckExpr();
        check.setArgument(scalarParamRef("c", ws));
        check.setOp(CardCheckOp.CHOICE);
        check.setNecessity(Necessity.REQUIRED);
        check.attributes().add("x");
        check.attributes().add("y");
        check.attachToWorkspace(ws);

        String irRouted = render(c, check);
        String legacy = legacyRender(legacyCompiler(ws), check);
        assertEquals(legacy, irRouted, "an untargeted family must render byte-identically to legacy");

        assertEquals(1, c.untargetedVisitCount(), "the untargeted census must count the check visit");
        assertEquals("RCardinalityCheckExpr=1", c.untargetedVisitBreakdown());
        assertEquals(0, c.irDeclinedCount(), "an untargeted visit is not an attempt — no decline moves");

        // The #500 conservation signature: the FORMER exemplar (a root list literal) now CLAIMS
        // — driven via the oracle-root serve (super.visitListLiteral, byte-identical), zero new
        // untargeted counts.
        IRExpressionCompiler c2 = compiler(ws);
        RListLiteral list = new RListLiteral();
        list.elements().add(intLiteral(1));
        list.elements().add(intLiteral(2));
        list.attachToWorkspace(ws);
        assertEquals(legacyRender(legacyCompiler(ws), list), render(c2, list),
                "the taught list literal must render legacy's own bytes (the oracle-root serve)");
        assertTrue(c2.oracleRootLoweredBreakdown().contains("listLiteral=1"),
                "the #500 receipt reads the serve");
        assertEquals(0, c2.untargetedVisitCount(),
                "the family left the untargeted meter — the teach's conservation signature");
    }

    @Test
    void declineCensusConservesAcrossFamilyAndSite() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // Two adapterGap declines (the #476 equality-over-check fixture — the #515 recut: the
        // beyond-long integer hoists that carried this lock's leafEmitter legs are SERVED at
        // the emitter frontier since #515, so the site pair moves to the two unit-expressible
        // decline sites) plus one aliasResolution decline (an alias reference with NO
        // enclosing function — legacy's empty-arg-list path): the per-family and per-site
        // breakdowns must both conserve to the scalar (the recordDecline single-seat law)
        // and rank count-DESCENDING (the worklist order).
        render(c, adapterGapEqualityFixture(ws));
        render(c, adapterGapEqualityFixture(ws));
        render(c, parentlessAliasRef("orphanAlias", ws));

        assertEquals(3, c.irDeclinedCount());
        assertEquals(4, c.irDrivenCount(),
                "the two param leaves drive under each fixture's legacy re-walk");
        assertEquals("REqualityExpr=2 RSymbolReference=1", c.irDeclineFamilyBreakdown());
        assertEquals("adapterGap=2 aliasResolution=1", c.irDeclineSiteBreakdown());
        assertEquals(2, c.untargetedVisitCount(),
                "declines are targeted-family events — only the nested un-armed check counts"
                        + " on the untargeted meter under each legacy re-walk");
    }

    @Test
    void censusBreakdownsReadNoneOnAFreshCompiler() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());

        // A zero-activity cell prints a stable literal (the RULE-seam cdm cells are exactly this
        // shape on the D11 census lines) rather than an empty tail a log diff could mistake for
        // truncation.
        assertEquals("none", c.irDeclineFamilyBreakdown());
        assertEquals("none", c.irDeclineSiteBreakdown());
        assertEquals("none", c.untargetedVisitBreakdown());
        assertEquals(0, c.untargetedVisitCount());
        assertEquals("none", c.wAliasGateBreakdown());
    }

    @Test
    void wFacetAliasOutputBuilderNavLowersSinceThe531Teach() throws IOException {
        // The #531 teach — the LAST decline taught, the 100% walk's close (the #381 W-facet
        // belt, corpus carrier cdm5 NewEquitySwapProduct ×6, the census face
        // wAlias.w.declined.out:product.collide:0.inputs:2=6 pre-teach — the fate segment
        // flips to lowered at this teach): an OUTPUT-ROOTED disguised-chain alias
        // call site renders the aliasOutputBuilderNav wrap natively from the IR —
        // MapperS.of(alias(output.toBuilder(), inputs).build()) — byte-identical to legacy's
        // ReferenceHandler W-arm, asserted BOTH ways (against legacy's own render on the SAME
        // node and against the literal golden compose). The alias DECLARATION parses from real
        // source, so the disguised body (an output-rooted nav the parser cannot root — the
        // #346 REnumValueRef disguise) is the parser's own; the call-site reference is
        // synthesized symbol-EMPTY (the corpus route: the #502 resolve-by-name arm lowers it),
        // parent-wired per the up-only-parented law.
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        String source = """
                namespace "test.step531"

                type WProduct531:
                    leg WLeg531 (0..1)

                type WLeg531:
                    fee number (0..1)

                func BuildW531:
                    inputs:
                        security string (1..1)
                        masterConfirmation number (0..1)
                    output:
                        product WProduct531 (1..1)
                    alias payout: product -> leg
                    alias other: security
                    set product:
                        WProduct531 { leg: empty }

                func ZeroInputW531:
                    output:
                        product WProduct531 (1..1)
                    alias payout: product -> leg
                    set product:
                        WProduct531 { leg: empty }
                """;
        RModel model = AstBuilder.buildFromString(source, "step531-wfacet.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "BuildW531".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no BuildW531 parsed"));

        RSymbolReference wRef = new RSymbolReference();
        wRef.setName("payout");
        wRef.setParent(fn);
        wRef.attachToWorkspace(ws);

        IRExpressionCompiler probed = compiler(ws);
        probed.setBlockerProbeForTest(true);
        assertEquals("MapperS.of(payout(product.toBuilder(), security, masterConfirmation).build())",
                renderScoped(probed, wRef),
                "the W-facet call site must render legacy's aliasOutputBuilderNav wrap from the IR");
        assertEquals(legacyRenderScoped(legacyCompiler(ws), wRef),
                renderScoped(compiler(ws), wRef),
                "the IR render and legacy's W-arm must agree byte-for-byte on the same node");
        assertEquals(1, probed.irDrivenCount(), "the W-face is IR-LOWERED since the teach");
        assertEquals(0, probed.irDeclinedCount(),
                "the W-face leaves the declined population — the flip the 100% walk closes on");
        assertEquals("wAlias.w.lowered.out:product.collide:0.inputs:2=1",
                probed.wAliasGateBreakdown(),
                "the census fate segment reads lowered at the SAME mint seat the pre-teach"
                        + " probe read declined (the BY-CALL flip proof)");

        // Boundary negative 1 — a NON-output-rooted alias in the same function keeps the bare
        // render (the W-gate admits only the output-rooted disguised body).
        RSymbolReference bareRef = new RSymbolReference();
        bareRef.setName("other");
        bareRef.setParent(fn);
        bareRef.attachToWorkspace(ws);
        IRExpressionCompiler bare = compiler(ws);
        assertEquals("other(security, masterConfirmation)", renderScoped(bare, bareRef),
                "a non-W alias keeps the bare aliasName(inputs) form");
        assertEquals(legacyRenderScoped(legacyCompiler(ws), bareRef),
                renderScoped(compiler(ws), bareRef),
                "the bare form agrees with legacy on the same node");

        // Boundary negative 2 — the ZERO-input W variant renders the empty-join branch:
        // MapperS.of(payout(product.toBuilder()).build()) — no trailing ", " seam (the
        // synthesized-input placeholder filtered on BOTH routes, the #426
        // zeroInputAliasCallArgs law).
        RFunction zeroFn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "ZeroInputW531".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no ZeroInputW531 parsed"));
        RSymbolReference zeroRef = new RSymbolReference();
        zeroRef.setName("payout");
        zeroRef.setParent(zeroFn);
        zeroRef.attachToWorkspace(ws);
        IRExpressionCompiler zero = compiler(ws);
        assertEquals("MapperS.of(payout(product.toBuilder()).build())",
                renderScoped(zero, zeroRef),
                "the zero-input W variant renders the empty-join branch of the wrap");
        assertEquals(legacyRenderScoped(legacyCompiler(ws), zeroRef),
                renderScoped(compiler(ws), zeroRef),
                "the zero-input wrap agrees with legacy on the same node");

        // Boundary negative 3 (the Seat-1 OBS-1 leg): the W×collide compose — the "1"
        // suffix INSIDE the wrap (corpus-absent; the mechanism lock at the emitter seat:
        // the same suffix law the bare form's own witness locks, asserted here on the
        // wrap compose directly so the collide branch of emitAliasOutputBuilderNav is
        // exercised, not inferred from the bare twin).
        assertEquals("MapperS.of(payout1(product.toBuilder(), security, masterConfirmation).build())",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emitAliasOutputBuilderNav("payout", true,
                                "product", java.util.List.of("security", "masterConfirmation")))
                        .renderToString(),
                "the dep-collision W wrap carries the \"1\" suffix inside the compose");
    }

    @Test
    void blockerProbeAttributesAdapterGapDeclineToNestedFamily() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler probed = compiler(ws);
        probed.setBlockerProbeForTest(true);

        // The #476 blocker probe's attribution proof (the fixture family recut at #513 and
        // again at #515 — join joined the armed set, so `p = (c required choice x, y)` pins
        // the mechanism): the EQUALITY root declines at adapterGap (the nested check has no
        // adapt arm, so the whole claim fails to lower), and the probe must attribute the
        // claim to the NESTED blocking family (RCardinalityCheckExpr), NOT the claim root's
        // own REqualityExpr — exactly the distinction the #475 family-of-root census could
        // never see. Sole blocker → both rankings.
        REqualityExpr eq = adapterGapEqualityFixture(ws);
        String irRouted = render(probed, eq);
        String legacy = legacyRender(legacyCompiler(ws), eq);
        assertEquals(legacy, irRouted, "an adapterGap-declined claim must render byte-identically under the probe");

        assertEquals(1, probed.blockerProbedClaimCount(), "the adapterGap decline must be probed");
        assertEquals("RCardinalityCheckExpr=1", probed.blockerClaimsBreakdown(),
                "the probe must attribute the claim to the nested check family, not the equality root");
        assertEquals("RCardinalityCheckExpr=1", probed.soleBlockerClaimsBreakdown(),
                "a single-family claim must join the sole-blocker (unlock) ranking");
        assertEquals(0, probed.blockerProbeAnomalyCount(), "a re-adapt contradiction would mean adapter state");
        // #477 — the per-ARM decode rides the same probe run: an un-armed family names noAdaptArm,
        // the singleton pair joins the sole-reason ranking, and the mirror residue reads zero.
        assertEquals("RCardinalityCheckExpr:noAdaptArm=1", probed.blockerReasonBreakdown(),
                "the minimal blocker carries the adapter's own first-failing-gate token");
        assertEquals("RCardinalityCheckExpr:noAdaptArm=1", probed.soleReasonClaimsBreakdown(),
                "a single-pair claim joins the sole-reason (per-gate unlock) ranking");
        assertEquals("none", probed.soleFamilyMultiReasonBreakdown(),
                "a single-pair claim leaves no multi-reason residue");
        assertEquals(0, probed.blockerReasonUnattributedCount(),
                "the mirror-fidelity meter must read zero");
        // The probe's own adapt calls move NO standing counters: the census still reads exactly the
        // root's ONE adapterGap decline; under the legacy re-walk the check child counts on the
        // UNTARGETED meter (not a decline — it makes no IR attempt) and the two param leaves
        // drive (the choice attribute list renders inside legacy's own composition, never a
        // visitor pass).
        assertEquals(1, probed.irDeclinedCount());
        assertEquals(2, probed.irDrivenCount());
        assertEquals("REqualityExpr=1", probed.irDeclineFamilyBreakdown());
        assertEquals("adapterGap=1", probed.irDeclineSiteBreakdown());
        assertEquals(1, probed.untargetedVisitCount());
    }

    @Test
    void blockerProbeSplitsMultiFamilyClaimFromSoleRanking() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // Two DISTINCT minimal-blocker families in one claim (the fixture families recut at
        // #513 and again at #515 — join joined the armed set, so the last two unarmed
        // families pin the mechanism: `(a reduce acc, v [ acc ]) = (c required choice x, y)`
        // — the reduce node AND the cardinality-check node (neither has an adapt arm; the
        // param and closure-param children lower) both block. A hand-built reduce cannot
        // render in a null-scope context (the #513 note), so the fixture PARSES — the
        // resolved workspace types the params and the scoped render twins supply the live
        // statement scope legacy's lambda render needs. Both families count on the
        // participation ranking; NEITHER on the sole ranking (teaching one family alone
        // does not unblock this claim — the ranking split the probe exists to expose).
        String source = """
                namespace "test.step515multi"

                type Choice515:
                    x string (0..1)
                    y string (0..1)

                func MultiBlock:
                    inputs:
                        a string (0..*)
                        c Choice515 (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        (a reduce acc, v [ acc ]) = (c required choice x, y)
                """;
        RModel model = AstBuilder.buildFromString(source, "step515-multiblock.rosetta");
        RWorkspace pws = RWorkspace.build(java.util.List.of(model)).workspace();
        REqualityExpr eq = AstWalker.findFirst(pws.files().get(0), REqualityExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no REqualityExpr"));

        IRExpressionCompiler probedP = compiler(pws);
        probedP.setBlockerProbeForTest(true);
        String irRouted = renderScoped(probedP, eq);
        String legacy = legacyRenderScoped(legacyCompiler(pws), eq);
        assertEquals(legacy, irRouted, "a multi-blocker claim must render byte-identically under the probe");

        assertEquals(1, probedP.blockerProbedClaimCount());
        assertEquals("RCardinalityCheckExpr=1 RReduceExpr=1", probedP.blockerClaimsBreakdown(),
                "each distinct blocking family counts once for the claim");
        assertEquals("none", probedP.soleBlockerClaimsBreakdown(),
                "a multi-family claim joins NO sole-blocker ranking — no single teach unblocks it");
        assertEquals(0, probedP.blockerProbeAnomalyCount());
        // #477 — the pair rankings mirror the family split: both pairs participate, neither is a
        // sole pair, and the multi-reason residue stays empty (it is a SOLE-FAMILY residue — a
        // multi-family claim is already outside every sole ranking).
        assertEquals("RCardinalityCheckExpr:noAdaptArm=1 RReduceExpr:noAdaptArm=1",
                probedP.blockerReasonBreakdown());
        assertEquals("none", probedP.soleReasonClaimsBreakdown());
        assertEquals("none", probedP.soleFamilyMultiReasonBreakdown());
        assertEquals(0, probedP.blockerReasonUnattributedCount());
        // Conservation under the legacy re-walk: both blocked children are UNTARGETED families
        // (their count-then-super meters fire once each); the two params and the lambda's bare
        // closure-param body drive (legacy's reduce handler compiles the receiver and body
        // through the visitor; the choice attribute list renders inside legacy's own
        // composition, never a visitor pass); only the root's ONE adapterGap decline moves
        // the census.
        assertEquals(2, probedP.untargetedVisitCount());
        assertEquals("RCardinalityCheckExpr=1 RReduceExpr=1", probedP.untargetedVisitBreakdown());
        assertEquals(1, probedP.irDeclinedCount());
        assertEquals(3, probedP.irDrivenCount());
    }

    @Test
    void blockerProbeSplitsSameFamilyMultiReasonFromSoleReason() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler probed = compiler(ws);
        probed.setBlockerProbeForTest(true);

        // #477 — ONE family, TWO arm gates in one claim: `(FooEnum -> Bar) = (BazEnum -> Cash)`
        // where the left ref resolved its enumeration but NOT the value (the raw-valueName
        // fallback legacy renders flat) and the right ref carries a resolved choice-option
        // channel (a disguise checked before the enumeration in the mirror's precedence; legacy's
        // enumeration-first arm still renders the flat constant, so the claim byte-compares).
        // Neither nested ref is genuine, the equality root declines at adapterGap, and the probe
        // reads two DIFFERENT reasons in the SAME family — the shape that lands on the sole-FAMILY
        // ranking but NOT the sole-REASON ranking, closing the #477 conservation identity
        // (soleBlocker == Σ soleReason + soleFamilyMultiReason, per family).
        REnumeration fooEnum = fixtureEnum("FooEnum");
        fooEnum.attachToWorkspace(ws);
        REnumValueRef valueFallback = new REnumValueRef();
        valueFallback.setEnumName("FooEnum");
        valueFallback.setValueName("Bar");
        valueFallback.setResolvedEnum(fooEnum);
        valueFallback.attachToWorkspace(ws);

        REnumeration bazEnum = fixtureEnum("BazEnum");
        REnumValue cash = new REnumValue();
        cash.setName("Cash");
        cash.setParent(bazEnum);
        bazEnum.values().add(cash);
        bazEnum.attachToWorkspace(ws);
        cash.attachToWorkspace(ws);
        RDataType choiceOption = new RDataType();
        choiceOption.setName("SomeOption");
        REnumValueRef choiceNarrowing = new REnumValueRef();
        choiceNarrowing.setEnumName("BazEnum");
        choiceNarrowing.setValueName("Cash");
        choiceNarrowing.setResolvedEnum(bazEnum);
        choiceNarrowing.setResolvedValue(cash);
        choiceNarrowing.setResolvedChoiceOption(choiceOption);
        choiceNarrowing.attachToWorkspace(ws);

        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(valueFallback);
        eq.setRight(choiceNarrowing);
        eq.attachToWorkspace(ws);

        String irRouted = render(probed, eq);
        String legacy = legacyRender(legacyCompiler(ws), eq);
        assertEquals(legacy, irRouted, "a same-family multi-reason claim must render byte-identically under the probe");

        // RECUT at #505: THREE probed claims now — the equality root plus the legacy
        // fallback's TWO re-entrant ENR visits at the CONVERTED visitEnumValueRef seat (the
        // L-109c exclusion retired; each nested ref declines at its own root as a counted
        // probe event). The choiceOption face spells its #505 refined residue
        // (choiceOption.untyped — the A2 twin's own gate on the untyped hand-built ref);
        // the two seat claims are sole-REASON each, the equality stays the multi-reason
        // residue, and the conservation closes: sole 3 == soleReason 2 + residue 1.
        assertEquals(3, probed.blockerProbedClaimCount());
        assertEquals("REnumValueRef=3", probed.blockerClaimsBreakdown(),
                "one family participates — the family ranking cannot see the two-gate split");
        assertEquals("REnumValueRef=3", probed.soleBlockerClaimsBreakdown(),
                "every claim is sole-FAMILY (the #476 unit still counts them)");
        assertEquals("REnumValueRef:choiceOption.untyped=2 REnumValueRef:valueNameFallback=2",
                probed.blockerReasonBreakdown(),
                "each distinct arm gate counts once per claim (root + seat re-entry)");
        assertEquals("REnumValueRef:choiceOption.untyped=1 REnumValueRef:valueNameFallback=1",
                probed.soleReasonClaimsBreakdown(),
                "the seat re-entries are sole-reason claims at their own roots");
        assertEquals("REnumValueRef=1", probed.soleFamilyMultiReasonBreakdown(),
                "the multi-reason residue closes the conservation: sole 3 == soleReason 2 + residue 1");
        assertEquals(0, probed.blockerReasonUnattributedCount());
        assertEquals(0, probed.blockerProbeAnomalyCount());
    }

    @Test
    void blockerProbeClassifiesNavGateShapes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler probed = compiler(ws);
        probed.setBlockerProbeForTest(true);

        // #478 — the nav-gate SHAPE witness: an REnumValueRef minimal blocker on the two
        // disguised-navigation gates additionally classifies by head/root class (legacy
        // ReferenceHandler's own resolution order) × the adapter's verdict on the EXACT
        // legacy-synthesized equivalent nav. Three shapes across two claims:
        //   (1) `s -> x` under a function whose SHORTCUT is `s` — resolveNameInFunction nulls a
        //       shortcut head by its own contract, so the equivalent carries an unresolved receiver
        //       AND no wired feature (the synthesizer resolves features only off a resolved head),
        //       and the adapter's mirror names its feature gate first — since the #492 head
        //       decode the verdict is faceted (the body-less fixture shortcut lands the
        //       bodyMissing face): inputFeatureNav:headShortcut:declines:featureUnresolved
        //       .aliasHead.bodyMissing — the HEAD-CLASS facet is what separates this from a
        //       resolvable head with a genuinely missing feature;
        //   (2) `trade -> leg` under the same function whose INPUT `trade` carries no declared
        //       type — the receiver resolves (the input match) but the feature lookup nulls, so
        //       the equivalent declines at the same gate under the headInput class:
        //       inputFeatureNav:headInput:declines:featureUnresolved.headAttr (#492: the
        //       resolved-attribute-head face);
        //   (3) a one-segment closure-param AttributeChain as the RECEIVER of a further hop — the
        //       chain synthesizer's own null case (attributeChain:oneSegClosure:noEquivalent), and
        //       the position facet reads the receiverOfChain seat.
        RFunction fn = new RFunction();
        fn.setName("NavGateFixtureFn");
        RShortcut shortcut = new RShortcut();
        shortcut.setName("s");
        fn.shortcuts().add(shortcut);
        shortcut.setParent(fn);
        RAttribute tradeInput = new RAttribute();
        tradeInput.setName("trade");
        tradeInput.setCardinality(cardinality(1, false));
        fn.inputs().add(tradeInput);
        tradeInput.setParent(fn);
        fn.attachToWorkspace(ws);
        tradeInput.attachToWorkspace(ws);

        // The binder-bound leaf the mirror keys the channel on (the classifier itself re-derives
        // nothing from it — the shape is the head class + the equivalent's own verdict).
        RAttribute boundFeature = new RAttribute();
        boundFeature.setName("x");
        boundFeature.setCardinality(cardinality(1, false));
        boundFeature.attachToWorkspace(ws);

        REnumValueRef shortcutHead = new REnumValueRef();
        shortcutHead.setEnumName("s");
        shortcutHead.setValueName("x");
        shortcutHead.setResolvedInputFeature(boundFeature);
        shortcutHead.attachToWorkspace(ws);
        REnumValueRef inputHead = new REnumValueRef();
        inputHead.setEnumName("trade");
        inputHead.setValueName("leg");
        inputHead.setResolvedInputFeature(boundFeature);
        inputHead.attachToWorkspace(ws);
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(shortcutHead);
        eq.setRight(inputHead);
        shortcutHead.setParent(eq);
        inputHead.setParent(eq);
        eq.setParent(fn);
        eq.attachToWorkspace(ws);

        String irRouted = render(probed, eq);
        String legacy = legacyRender(legacyCompiler(ws), eq);
        assertEquals(legacy, irRouted, "a nav-gate-blocked claim must render byte-identically under the probe");

        assertEquals("inputFeatureNav:headInput:declines:featureUnresolved.headAttr.declMiss=1"
                        + " inputFeatureNav:headShortcut:declines:featureUnresolved.aliasHead.bodyMissing=1",
                probed.navGateShapeBreakdown(),
                "each blocker NODE classifies by head class × the equivalent's own adapter verdict");
        assertEquals("inputFeatureNav:directPosition=2", probed.navGatePositionBreakdown(),
                "both operand seats are direct positions");
        assertEquals("inputFeatureNav:headInput:declines:featureUnresolved.headAttr.declMiss"
                        + " e.g. in=NavGateFixtureFn trade->leg"
                        + " | inputFeatureNav:headShortcut:declines:featureUnresolved.aliasHead.bodyMissing"
                        + " e.g. in=NavGateFixtureFn s->x",
                probed.navGateWitnessSamples(),
                "the witness pins one real site per bucket");

        // (3) the attribute-chain shape + the receiverOfChain position seat, a second claim on the
        // SAME compiler (the maps accumulate per cell exactly like every census counter).
        REnumValueRef closureChain = new REnumValueRef();
        closureChain.setEnumName("cp");
        closureChain.setValueName("x");
        closureChain.setResolvedAttributeChain(new REnumValueRef.AttributeChain(boundFeature));
        closureChain.attachToWorkspace(ws);
        RFeatureCall hop = new RFeatureCall();
        hop.setReceiver(closureChain);
        hop.setFeatureName("y");
        closureChain.setParent(hop);
        hop.attachToWorkspace(ws);
        RExistenceExpr exists = new RExistenceExpr();
        exists.setOp(ExistenceOp.EXISTS);
        exists.setArgument(hop);
        hop.setParent(exists);
        exists.attachToWorkspace(ws);

        String irRoutedExists = render(probed, exists);
        String legacyExists = legacyRender(legacyCompiler(ws), exists);
        assertEquals(legacyExists, irRoutedExists,
                "a chain-receiver nav-gate claim must render byte-identically under the probe");

        // The chain node classifies THRICE: once under the exists root's probe, once more when
        // the legacy re-walk re-enters visitFeatureCall on the nested hop (which declines at
        // adapterGap as its own probe event — the standing #476 event unit), and since #505 a
        // third time when the hop's legacy render re-enters the ENR at the CONVERTED
        // visitEnumValueRef seat (the L-109c exclusion retired — the seat's own decline is a
        // counted probe event now).
        assertEquals("attributeChain:oneSegClosure:noEquivalent=3"
                        + " inputFeatureNav:headInput:declines:featureUnresolved.headAttr.declMiss=1"
                        + " inputFeatureNav:headShortcut:declines:featureUnresolved.aliasHead.bodyMissing=1",
                probed.navGateShapeBreakdown(),
                "the closure-param chain is the synthesizer's own null case");
        assertEquals("attributeChain:receiverOfChain=3 inputFeatureNav:directPosition=2",
                probed.navGatePositionBreakdown(),
                "the chain-receiver seat splits from the direct operand seats (the third"
                        + " re-entrant event keeps the chain-receiver position — the raw"
                        + " node's own parent hop)");

        // Probe OFF leaves the witness maps untouched (the census channel's standing contract):
        // the same claims render byte-identically and every nav-gate surface reads `none`.
        IRExpressionCompiler off = compiler(ws);
        assertEquals(legacy, render(off, eq));
        assertEquals(legacyExists, render(off, exists));
        assertEquals("none", off.navGateShapeBreakdown());
        assertEquals("none", off.navGatePositionBreakdown());
        assertEquals("none", off.navGateWitnessSamples());
    }

    @Test
    void blockerProbeClassifiesImplicitRootShapes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler probed = compiler(ws);
        probed.setBlockerProbeForTest(true);

        // #479 — the implicit-ROOT shape witness: the three cluster gates the #478 witness proved
        // bottom out at the synthetic input/item base each classify by the LEGACY machinery that
        // renders the blocker's root. Three claims, one per channel:
        //   (1) `item -> leg` (a SYNTHETIC implicit receiver) inside an IMPLICIT extract lambda —
        //       the receiver gate declines (receiverSyntheticItem) and the witness classifies the
        //       binding context by handle(RImplicitVariable)'s own arm order (an implicit
        //       extract-body lambda → inLambdaImplicit.filterExtract) × the RETYPE-SOURCE channel
        //       (the binder argument here is an UNRESOLVED bare name — outside the arm's
        //       element-form allowlist → unprovableSource);
        //   (2) a bare reference to a DATA-TYPE attribute (attrOutsideFunction) with no enclosing
        //       rule/lambda/condition — every synthesizer in legacy's chain nulls →
        //       bareAttr:noSynthesizer:noEquivalent;
        //   (3) the one-segment closure-param AttributeChain (the #478 oneSegClosure shape) as its
        //       own claim — the rule-input chain synthesizer nulls, the fall-through decode walks
        //       legacy's own order (itemChain nulls without a lambda; no switch case) to the
        //       generic synthesizeFeatureCall seam, whose equivalent carries no wired feature →
        //       chainFall:genericSynth:declines:featureUnresolved.headUnresolved (#492: no
        //       enclosing function, no shortcut collision — the headUnresolved face) — while the
        //       #478 channel keeps its own oneSegClosure bucket (the additive-channel contract).
        RFunction fn = new RFunction();
        fn.setName("ImplicitRootFixtureFn");
        fn.attachToWorkspace(ws);
        RAttribute legFeature = new RAttribute();
        legFeature.setName("leg");
        legFeature.setCardinality(cardinality(1, false));
        legFeature.attachToWorkspace(ws);

        RImplicitVariable synItem = new RImplicitVariable();
        synItem.setSynthetic(true);
        RFeatureCall itemNav = new RFeatureCall();
        itemNav.setReceiver(synItem);
        itemNav.setFeatureName("leg");
        itemNav.setResolvedFeature(legFeature);
        synItem.setParent(itemNav);
        RInlineFunction inline = new RInlineFunction();
        inline.setBody(itemNav);
        itemNav.setParent(inline);
        RExtractExpr extract = new RExtractExpr();
        RSymbolReference extractSource = new RSymbolReference();
        extractSource.setName("legs");
        extract.setArgument(extractSource);
        extractSource.setParent(extract);
        extract.setBody(inline);
        inline.setParent(extract);
        extract.setParent(fn);
        synItem.attachToWorkspace(ws);
        itemNav.attachToWorkspace(ws);
        extractSource.attachToWorkspace(ws);

        String irRouted = render(probed, itemNav);
        String legacy = legacyRender(legacyCompiler(ws), itemNav);
        assertEquals(legacy, irRouted,
                "a synthetic-item-receiver claim must render byte-identically under the probe");
        // RECUT at #504 (the pin-recut law): the unprovable-source synthetic-item nav now
        // MINTS the shallow IRSynItemNav (the arm-B teach) — the shape LOWERS, so the #479
        // witness channel (blocker-fed) carries no row for it any more.
        assertEquals("none", probed.implicitRootShapeBreakdown(),
                "the lowered shape feeds no blocker witness — the mint claims it");

        RDataType priceType = new RDataType();
        priceType.setName("PriceHolder");
        RAttribute priceAttr = new RAttribute();
        priceAttr.setName("price");
        priceAttr.setCardinality(cardinality(1, false));
        priceAttr.setParent(priceType);
        priceType.attachToWorkspace(ws);
        priceAttr.attachToWorkspace(ws);
        RSymbolReference bareAttr = new RSymbolReference();
        bareAttr.setName("price");
        bareAttr.setResolvedSymbol(priceAttr);
        bareAttr.attachToWorkspace(ws);
        RExistenceExpr bareExists = new RExistenceExpr();
        bareExists.setOp(ExistenceOp.EXISTS);
        bareExists.setArgument(bareAttr);
        bareAttr.setParent(bareExists);
        bareExists.attachToWorkspace(ws);

        String irRoutedBare = render(probed, bareExists);
        String legacyBare = legacyRender(legacyCompiler(ws), bareExists);
        assertEquals(legacyBare, irRoutedBare,
                "a bare data-type-attribute claim must render byte-identically under the probe");

        RAttribute chainLeaf = new RAttribute();
        chainLeaf.setName("x");
        chainLeaf.setCardinality(cardinality(1, false));
        chainLeaf.attachToWorkspace(ws);
        REnumValueRef closureChain = new REnumValueRef();
        closureChain.setEnumName("cp");
        closureChain.setValueName("x");
        closureChain.setResolvedAttributeChain(new REnumValueRef.AttributeChain(chainLeaf));
        closureChain.attachToWorkspace(ws);
        RExistenceExpr chainExists = new RExistenceExpr();
        chainExists.setOp(ExistenceOp.EXISTS);
        chainExists.setArgument(closureChain);
        closureChain.setParent(chainExists);
        chainExists.attachToWorkspace(ws);

        String irRoutedChain = render(probed, chainExists);
        String legacyChain = legacyRender(legacyCompiler(ws), chainExists);
        assertEquals(legacyChain, irRoutedChain,
                "a fall-through chain claim must render byte-identically under the probe");

        // RECUT at #504: the synItemNav bucket vanished with the mint (the shape lowers);
        // the bareAttr + chainFall buckets keep their rows — the channel stays additive.
        // RECUT at #505: the chainFall row counts TWICE — the exists root's probe plus the
        // legacy fallback's re-entry at the CONVERTED visitEnumValueRef seat (the L-109c
        // exclusion retired — the seat's own decline is a counted probe event now).
        // RECUT at #528: the bareAttr row RETIRES for TYPED carriers — the L-113 conversion
        // mints the shallow IRImplicitAttrNav for the noSynthesizer/noEquivalent class, so
        // those claims LOWER and never reach the blocker probe; the chainFall rows (a
        // different family, outside the mint's census gate) stand unchanged, proving the
        // channel stayed additive.
        // RE-TENSED at the #528 fix commit: THIS fixture's attr carries no typeCall, so the
        // mint's #514-pattern type guard (the Copilot #528 R1 catch) declines it and the
        // bareAttr row RETURNS for exactly the untypeable class — the guard's own probe-side
        // witness. The typed population keeps minting (the corpus SOT carries no such row).
        assertEquals("chainFall:genericSynth:declines:featureUnresolved.headUnresolved=2"
                        + " bareAttr:noSynthesizer:noEquivalent=1",
                probed.implicitRootShapeBreakdown(),
                "each cluster gate classifies by the legacy machinery that renders its root —"
                        + " the untypeable bare-attr fixture re-enters the probe under the"
                        + " mint's type guard");
        assertEquals("attributeChain:oneSegClosure:noEquivalent=2",
                probed.navGateShapeBreakdown(),
                "the #478 channel keeps its own buckets — the fall-through decode is ADDITIVE");
        assertEquals("bareAttr:noSynthesizer:noEquivalent e.g. in=<unresolved> price"
                        + " | chainFall:genericSynth:declines:featureUnresolved.headUnresolved"
                        + " e.g. in=<unresolved> cp->x",
                probed.implicitRootWitnessSamples(),
                "the witness pins one real site per bucket — the untypeable bare-attr bucket"
                        + " returns with its own witness under the mint's type guard");

        // Probe OFF leaves the witness maps untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(ws);
        assertEquals(legacy, render(off, itemNav));
        assertEquals(legacyBare, render(off, bareExists));
        assertEquals(legacyChain, render(off, chainExists));
        assertEquals("none", off.implicitRootShapeBreakdown());
        assertEquals("none", off.implicitRootWitnessSamples());
    }

    @Test
    void blockerProbeDecodesImplicitRootSourcesAndArmAdmissions() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #480 — the pick-A decode-first opener: the SOURCE decode (the widening map over the
        // #479 allowlist residue + the still-declining equivalents' bases) and the
        // bare-attr/chain arms' admissions RESTATED per blocker (the #479 law — the witness's
        // verdict channel IS the arm's channel). PRE-teach the restatement read `claims` on the
        // claimable fixtures — the pre-sizing receipts (target-480-witness1/cpONprobe1) carry
        // that reading; POST-teach a claimable node LOWERS and never reaches the witness (the
        // conservation signature — locked by the e2e driven test below), so this lock pins the
        // STILL-DECLINING faces of the channels.

        // (1) The bare `rate` inside `legs extract rate` where `rate` carries [metadata scheme] —
        //     legacy's Cat-9 synthesizer fires (itemNav; the identity guard is meta-blind) and —
        //     since the #499 meta arm — the synthesized equivalent (`item -> rate`, a meta nav
        //     over a lowering item receiver) LOWERS to IRMetaAccess, so the witness bucket reads
        //     `lowers` while the arm restatement declines at its SHAPE gate
        //     (`declines:equivalentMetaAccess` — the bare-attr arm's FieldAccess-over-
        //     synthetic-item contract excludes the meta kind; the adapter twin composes the
        //     matching `attrOutsideFunction.equivalentMetaAccess` face — recut from the pre-#499
        //     `declines:metaFeature`/`declines:equivalent.metaFeature` pins). No source-decode
        //     entry (the decline is not a synthetic-receiver one).
        String source = """
                namespace "test.step480"

                type Leg:
                    rate number (1..1)
                        [metadata scheme]

                func MyFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step480-meta-bare-attr.rosetta");
        RWorkspace pws = RWorkspace.build(java.util.List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findFirst(pws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction lambda = assertInstanceOf(RInlineFunction.class, extract.body());
        RSymbolReference bareRate = assertInstanceOf(RSymbolReference.class, lambda.body());

        RExistenceExpr bareClaim = new RExistenceExpr();
        bareClaim.setOp(ExistenceOp.EXISTS);
        bareClaim.setArgument(bareRate);
        bareClaim.setParent(lambda); // the claim sits inside the lambda; the parsed node keeps its parent

        IRExpressionCompiler probed = compiler(pws);
        probed.setBlockerProbeForTest(true);
        String irBare = render(probed, bareClaim);
        String legacyBare = legacyRender(legacyCompiler(pws), bareClaim);
        assertEquals(legacyBare, irBare,
                "a meta bare-attr claim must render byte-identically under the probe");
        // #501: the bareMeta arm CLAIMS the class — the bare meta attr lowers to the re-ranged
        // IRMetaAccess over the synthetic item, the exists claim root admits it
        // (isExistenceOperand, the #500 admission) and renders WHOLE through the oracle-root
        // existence serve, and the oracle's re-entrant interior visit of the bare rate claims
        // at its own root through the #501 bareAttrMeta serve (the disguised-meta frame leg) —
        // so the class never reaches the minimal-blocker walk and every witness channel reads
        // EMPTY (the conservation signature; the pre-#501 readings
        // [bareAttr:itemNav:lowers=2 · bareAttrArm:declines:equivalentMetaAccess=2] stand in
        // the #500 receipts).
        assertEquals("none", probed.implicitRootShapeBreakdown(),
                "the claimed class never reaches the witness channel — the conservation signature");
        assertEquals("none", probed.implicitRootArmBreakdown(),
                "the arm restatement claims (UNATTRIBUTED) — no decline face mints");
        assertEquals("none", probed.implicitRootSourceBreakdown(),
                "a claimed node records no source-decode entry");
        assertTrue(probed.oracleRootLoweredBreakdown().contains("existence=1"),
                "the exists claim root serves through the oracle-root existence leg (the #500"
                        + " containment serve over the #501-admitted meta operand)");
        assertTrue(probed.oracleRootLoweredBreakdown().contains("bareSymbolRef=1"),
                "the re-entrant bare meta attr serves at its own root through the args-empty"
                        + " dispatch leg — the #501 bareAttrMeta token RECUT bareSymbolRef at"
                        + " #502 (closure-param roots joined the same leg)");

        // (2) The unprovable-source decode: the #479 witness's own fixture-1 shape (a synthetic
        //     item nav inside an implicit extract over an UNRESOLVED bare name) sub-decodes the
        //     standing unprovableSource bucket by source shape — the widening map's key.
        RWorkspace ws = workspace();
        IRExpressionCompiler synProbed = compiler(ws);
        synProbed.setBlockerProbeForTest(true);
        RFunction fn = new RFunction();
        fn.setName("SourceDecodeFixtureFn");
        fn.attachToWorkspace(ws);
        RAttribute legFeature = new RAttribute();
        legFeature.setName("leg");
        legFeature.setCardinality(cardinality(1, false));
        legFeature.attachToWorkspace(ws);
        RImplicitVariable synItem = new RImplicitVariable();
        synItem.setSynthetic(true);
        RFeatureCall itemNav = new RFeatureCall();
        itemNav.setReceiver(synItem);
        itemNav.setFeatureName("leg");
        itemNav.setResolvedFeature(legFeature);
        synItem.setParent(itemNav);
        RInlineFunction inline = new RInlineFunction();
        inline.setBody(itemNav);
        itemNav.setParent(inline);
        RExtractExpr synExtract = new RExtractExpr();
        RSymbolReference synExtractSource = new RSymbolReference();
        synExtractSource.setName("legs");
        synExtract.setArgument(synExtractSource);
        synExtractSource.setParent(synExtract);
        synExtract.setBody(inline);
        inline.setParent(synExtract);
        synExtract.setParent(fn);
        synItem.attachToWorkspace(ws);
        itemNav.attachToWorkspace(ws);
        synExtractSource.attachToWorkspace(ws);

        String irSyn = render(synProbed, itemNav);
        assertEquals(legacyRender(legacyCompiler(ws), itemNav), irSyn,
                "the synthetic-item fixture must render byte-identically under the probe");
        // RECUT at #504 (the pin-recut law): the unprovable-source nav now MINTS (the arm-B
        // teach) — the shape lowers, so the blocker-fed #479/#480 channels carry no rows.
        assertEquals("none", synProbed.implicitRootShapeBreakdown(),
                "the lowered shape feeds no blocker witness — the mint claims it");
        assertEquals("none", synProbed.implicitRootSourceBreakdown(),
                "the source decode is blocker-fed too — empty on the lowered shape");
        assertEquals("none", synProbed.implicitRootSourceWitnessSamples(),
                "no blocker, no sample");

        // Probe OFF leaves every #480 channel untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(pws);
        assertEquals(legacyBare, render(off, bareClaim));
        assertEquals("none", off.implicitRootSourceBreakdown());
        assertEquals("none", off.implicitRootSourceWitnessSamples());
        assertEquals("none", off.implicitRootArmBreakdown());
    }

    @Test
    void blockerProbeDecodesElidedPipeFaces() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #481 — the pick-A decode-first opener: every elidedImplicit binding source on the #480
        // source-decode channels restates the PLANNED widening's admission (the pipe facet),
        // written BEFORE the arm existed (the #480 law). The then-pipe carrier: `legs then filter
        // rate exists` — the filter's grammar-elided argument binds through the then body's
        // implicit wrapper to the PIPE, so the resolver recurses to the then's ARGUMENT (`legs` —
        // allowlist-provable, type-cached). PRE-teach this carrier read the claimable face
        // `thenArg.provable.typeOk=1` on BOTH the bareAttr channel (the nested bare `rate`
        // blocker) and the synItem channel (the legacy-synthesized re-entrant equivalent's
        // receiver) — the pre-sizing reading the commit-2 receipts carry
        // (target-481-irjavainstall1..4 + cpONprobe1/2: 298 nodes at population); POST-teach the
        // widened arm claims the slice whole, the claim is IR-DRIVEN, and NO blocker ever
        // reaches the witness — the conservation signature in miniature (this lock pins it,
        // plus the STILL-DECLINING nonThen face below).
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
        RModel model = AstBuilder.buildFromString(source, "step481-then-pipe.rosetta");
        RWorkspace pws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFilterExpr filter = AstWalker.findFirst(pws.files().get(0), RFilterExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFilterExpr"));
        assertInstanceOf(RImplicitVariable.class, filter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        RExistenceExpr existsClaim = assertInstanceOf(RExistenceExpr.class, filter.body().body(),
                "the filter body must be the parsed exists claim");

        IRExpressionCompiler probed = compiler(pws);
        probed.setBlockerProbeForTest(true);
        String ir = render(probed, existsClaim);
        assertEquals(legacyRender(legacyCompiler(pws), existsClaim), ir,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, probed.irDrivenCount(),
                "the then-piped claim must be IR-DRIVEN — the #481 widening's meter witness");
        assertEquals("none", probed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read typeOk=1 on both"
                        + " channels; the commit-2 receipts carry that reading)");
        assertEquals("none", probed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (2) The nonThen face: an elided implicit whose nearest implicit binder is an EXTRACT
        //     body (the item piggy-back class — a FUTURE widening, sized not claimed). Hand-built
        //     at the #480 fixture-2 seat: the synthetic item nav declines receiverSyntheticItem,
        //     the base decode reads elidedImplicit, and the pipe facet names the binder construct.
        RWorkspace ws = workspace();
        IRExpressionCompiler nonThenProbed = compiler(ws);
        nonThenProbed.setBlockerProbeForTest(true);
        RFunction fn = new RFunction();
        fn.setName("NonThenFixtureFn");
        fn.attachToWorkspace(ws);
        RAttribute legFeature = new RAttribute();
        legFeature.setName("leg");
        legFeature.setCardinality(cardinality(1, false));
        legFeature.attachToWorkspace(ws);
        RImplicitVariable innerItem = new RImplicitVariable();
        innerItem.setSynthetic(true);
        RFeatureCall itemNav = new RFeatureCall();
        itemNav.setReceiver(innerItem);
        itemNav.setFeatureName("leg");
        itemNav.setResolvedFeature(legFeature);
        innerItem.setParent(itemNav);
        RInlineFunction innerFn = new RInlineFunction();
        innerFn.setBody(itemNav);
        itemNav.setParent(innerFn);
        RImplicitVariable elidedArg = new RImplicitVariable();
        elidedArg.setSynthetic(true);
        RFilterExpr innerFilter = new RFilterExpr();
        innerFilter.setArgument(elidedArg);
        elidedArg.setParent(innerFilter);
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
        innerItem.attachToWorkspace(ws);
        itemNav.attachToWorkspace(ws);
        elidedArg.attachToWorkspace(ws);
        outerSource.attachToWorkspace(ws);

        String irNonThen = render(nonThenProbed, itemNav);
        assertEquals(legacyRender(legacyCompiler(ws), itemNav), irNonThen,
                "the nonThen fixture must render byte-identically under the probe");
        // RECUT at #504 (the pin-recut law): the extract-bound elided-source nav now MINTS
        // (the arm-B teach) — the shape lowers, so the blocker-fed pipe channel is empty.
        assertEquals("none", nonThenProbed.implicitRootPipeBreakdown(),
                "the lowered shape feeds no blocker witness — the mint claims it");

        // Probe OFF leaves the #481 channel untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(pws);
        assertEquals(ir, render(off, existsClaim));
        assertEquals("none", off.implicitRootPipeBreakdown());
        assertEquals("none", off.implicitRootPipeWitnessSamples());
    }

    @Test
    void blockerProbeDecodesNestedThenPipeBodies() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #482 — the pick-A decode-first opener: the #481 residue map's dominant class
        // (`thenArg.unprovable.thenPipe`, 2,109 at population) restates the PLANNED RThenExpr
        // widening's admission per occurrence BEFORE the arm existed (the #480/#481 law). The
        // nested-pipe carrier parses left-associatively — `(legs then filter rate exists) then
        // filter qty exists` — so the OUTER filter's grammar-elided argument resolves through
        // the outer then's implicit body binder to the outer ARGUMENT: ITSELF the inner
        // RThenExpr. The arm proves the inner BODY (the element-preserving filter over
        // the provable bare input `legs`, its own elided argument resolving one hop further)
        // and the inner then's own cached type reads OK — PRE-teach this carrier read the
        // claimable face `thenPipe.provable.typeOk=1` on BOTH decode channels (the bare `qty`
        // blocker's equivalent base + the legacy-synthesized re-entrant equivalent's
        // receiver) — the pre-sizing reading the commit-2 receipts carry
        // (target-482-irjavainstall1 + cpONprobe1: 97 nodes at population); POST-teach the
        // widened arm claims the slice whole, the claim is IR-DRIVEN, and NO blocker ever
        // reaches the witness — the conservation signature in miniature (this lock pins it,
        // plus the STILL-DECLINING extract-bodied residue face below).
        String nestedSource = """
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
        RModel nestedModel = AstBuilder.buildFromString(nestedSource, "step482-nested-then-pipe.rosetta");
        RWorkspace pws = RWorkspace.build(java.util.List.of(nestedModel)).workspace();
        RThenExpr outerThen = AstWalker.findFirst(pws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RThenExpr.class, outerThen.argument(),
                "the left-associative chain must nest the inner pipe as the outer ARGUMENT");
        RFilterExpr outerFilter = assertInstanceOf(RFilterExpr.class,
                outerThen.body().orElseThrow().body(),
                "the outer then body must wrap the outer filter");
        assertInstanceOf(RImplicitVariable.class, outerFilter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        RExistenceExpr qtyClaim = assertInstanceOf(RExistenceExpr.class, outerFilter.body().body(),
                "the outer filter body must be the parsed qty-exists claim");

        IRExpressionCompiler probed = compiler(pws);
        probed.setBlockerProbeForTest(true);
        String ir = render(probed, qtyClaim);
        assertEquals(legacyRender(legacyCompiler(pws), qtyClaim), ir,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, probed.irDrivenCount(),
                "the nested-then-piped claim must be IR-DRIVEN — the #482 widening's meter witness");
        assertEquals("none", probed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read"
                        + " thenPipe.provable.typeOk=1 on both channels; the commit-2 receipts"
                        + " carry that reading)");
        assertEquals("none", probed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (2) The residue face: the inner then's BODY is an EXTRACT pipe whose own body is the
        //     written `item` — the IDENTITY extract (the element form is the ARGUMENT's own, an
        //     argument-recursion face outside the #483 body-descent arm), so since #483 the
        //     sub-decode names the face `thenPipe.unprovable.extractPipe.literalItem` — sized
        //     for a FUTURE widening, not claimed (this face stays post-teach). The identity
        //     `extract item` keeps Leg elements flowing so the outer `qty exists` claim mirrors
        //     carrier (1) exactly.
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
        RModel extractModel = AstBuilder.buildFromString(extractSource, "step482-extract-then-pipe.rosetta");
        RWorkspace ews = RWorkspace.build(java.util.List.of(extractModel)).workspace();
        RThenExpr extractOuterThen = AstWalker.findFirst(ews.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RThenExpr.class, extractOuterThen.argument(),
                "the left-associative chain must nest the extract-bodied then as the outer ARGUMENT");
        RFilterExpr extractOuterFilter = assertInstanceOf(RFilterExpr.class,
                extractOuterThen.body().orElseThrow().body(),
                "the outer then body must wrap the outer filter");
        RExistenceExpr extractQtyClaim = assertInstanceOf(RExistenceExpr.class,
                extractOuterFilter.body().body(),
                "the outer filter body must be the parsed qty-exists claim");

        IRExpressionCompiler extractProbed = compiler(ews);
        extractProbed.setBlockerProbeForTest(true);
        String irExtract = render(extractProbed, extractQtyClaim);
        assertEquals(legacyRender(legacyCompiler(ews), extractQtyClaim), irExtract,
                "the residue-face claim must render byte-identically under the probe");
        // RECUT at #502 (the pin-recut law): the identity `extract item` pipe argument now
        // CLAIMS — the retype's still-implicit type read goes through the shared structural
        // walk (sourceElementDataType's nested-binder + pipe recursions resolve the doubly
        // nested element exactly where the #501 recursion proved it), so the class never
        // reaches the witness channel (the conservation signature; the pre-#502 literalItem
        // readings stand in the #501 receipts).
        assertEquals("none", extractProbed.implicitRootPipeBreakdown(),
                "the claimed class records no pipe-decode entry — the conservation signature");

        // Probe OFF leaves the refined channel untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(pws);
        assertEquals(ir, render(off, qtyClaim));
        assertEquals("none", off.implicitRootPipeBreakdown());
        assertEquals("none", off.implicitRootPipeWitnessSamples());
    }

    @Test
    void blockerProbeDecodesExtractBodiedPipeBodies() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #483 — the pick-A decode-first opener: the #482 residue map's dominant class (the
        // extract-bodied pipes — the nested `thenPipe.unprovable.extractPipe` 1,435 + the flat
        // `thenArg.unprovable.extractPipe` face 753 at population) restates the PLANNED
        // RExtractExpr widening's admission per occurrence BEFORE the arm exists (the
        // #480/#481/#482 law). The element form of `A extract B` is B's OWN result per element
        // (every legacy map route — the single/item routes and the LoL routes alike —
        // produces result elements that ARE the body's values, the MULTI/LoL-body routes
        // flattening to the body's own; the checker types the extract off its BODY), so the planned
        // arm proves the extract BODY under the planned allowlist and reads the type facts on
        // the SEAT's resolved source. (1) The FLAT claimable face: the outer filter's
        // grammar-elided argument resolves to the then's argument — `trades extract leg` —
        // whose bare-attr body `leg` proves (non-meta, resolved) and whose own cached type
        // reads OK (Leg, no meta) — PRE-teach this carrier read
        // `thenArg.unprovable.extractPipe.provable.typeOk=1` on BOTH decode channels (the
        // pre-sizing reading the commit-2 receipts carry — target-483-irjavainstall1 +
        // cpONprobe1: 351 nodes at population); POST-teach the widened arm claims the slice
        // whole, the claim is IR-DRIVEN, and NO blocker ever reaches the witness — the
        // conservation signature in miniature.
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
        RWorkspace fws = RWorkspace.build(java.util.List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        assertInstanceOf(RExtractExpr.class, flatThen.argument(),
                "the extract must bind tighter than then — the flat face's resolved argument");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        assertInstanceOf(RImplicitVariable.class, flatFilter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        RExistenceExpr flatQtyClaim = assertInstanceOf(RExistenceExpr.class, flatFilter.body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler flatProbed = compiler(fws);
        flatProbed.setBlockerProbeForTest(true);
        String irFlat = render(flatProbed, flatQtyClaim);
        assertEquals(legacyRender(legacyCompiler(fws), flatQtyClaim), irFlat,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, flatProbed.irDrivenCount(),
                "the flat extract-piped claim must be IR-DRIVEN — the #483 widening's meter witness");
        assertEquals("none", flatProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read"
                        + " thenArg.unprovable.extractPipe.provable.typeOk=1 on both channels;"
                        + " the commit-2 receipts carry that reading)");
        assertEquals("none", flatProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (2) The NESTED claimable face: the outer filter's elided argument resolves to the
        //     INNER then (`trades then extract leg`), whose body is the extract — the #482
        //     thenPipe sub-decode refines through the extract-body facet with the type carrier
        //     staying the INNER THEN node (the claim seat's resolved source).
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
        RWorkspace nws = RWorkspace.build(java.util.List.of(nestedModel)).workspace();
        RThenExpr nestedOuterThen = AstWalker.findFirst(nws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RThenExpr nestedInnerThen = assertInstanceOf(RThenExpr.class, nestedOuterThen.argument(),
                "the left-associative chain must nest the extract-bodied then as the outer ARGUMENT");
        assertInstanceOf(RExtractExpr.class,
                nestedInnerThen.body().orElseThrow().body(),
                "the inner then body must be the extract");
        RFilterExpr nestedFilter = assertInstanceOf(RFilterExpr.class,
                nestedOuterThen.body().orElseThrow().body(),
                "the outer then body must wrap the outer filter");
        RExistenceExpr nestedQtyClaim = assertInstanceOf(RExistenceExpr.class,
                nestedFilter.body().body(),
                "the outer filter body must be the parsed qty-exists claim");

        IRExpressionCompiler nestedProbed = compiler(nws);
        nestedProbed.setBlockerProbeForTest(true);
        String irNested = render(nestedProbed, nestedQtyClaim);
        assertEquals(legacyRender(legacyCompiler(nws), nestedQtyClaim), irNested,
                "the widened nested claim must render byte-identically — the oracle-rendered"
                        + " base + the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, nestedProbed.irDrivenCount(),
                "the nested extract-piped claim must be IR-DRIVEN — the #482 then descent"
                        + " composing with the #483 extract descent");
        assertEquals("none", nestedProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness (pre-teach"
                        + " this carrier read"
                        + " thenArg.unprovable.thenPipe.unprovable.extractPipe.provable.typeOk=1"
                        + " on both channels; the commit-2 receipts carry that reading)");
        assertEquals("none", nestedProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (3) The NAMED residue face: an explicit-param extract body carries the
        //     #357/#364/#367/#375 scope-live walk-out class (the #389 self-shadow face
        //     included) — the planned arm declines whole, the safe direction; the facet stops
        //     at the binder gate BEFORE any body reading. Sized, not claimed (stays post-teach).
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
        RWorkspace nmws = RWorkspace.build(java.util.List.of(namedModel)).workspace();
        RThenExpr namedThen = AstWalker.findFirst(nmws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr namedExtract = assertInstanceOf(RExtractExpr.class, namedThen.argument(),
                "the named extract must be the then's argument");
        assertFalse(namedExtract.body().isImplicit() || namedExtract.body().paramNames().isEmpty(),
                "the extract body must carry the explicit param — the namedBinder face's gate");
        RFilterExpr namedFilter = assertInstanceOf(RFilterExpr.class,
                namedThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        RExistenceExpr namedQtyClaim = assertInstanceOf(RExistenceExpr.class,
                namedFilter.body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler namedProbed = compiler(nmws);
        namedProbed.setBlockerProbeForTest(true);
        String irNamed = render(namedProbed, namedQtyClaim);
        assertEquals(legacyRender(legacyCompiler(nmws), namedQtyClaim), irNamed,
                "the named-face claim must render byte-identically under the probe");
        // RECUT at #504 (the pin-recut law): the namedBinder face's carriers now MINT the
        // shallow IRSynItemNav (the arm-B teach — the RETYPE keeps its named-extract decline,
        // but the un-retypeable shapes lower and oracle-serve legacy's own scope-live render,
        // byte-asserted above) — the blocker-fed pipe channel is empty.
        assertEquals("none", namedProbed.implicitRootPipeBreakdown(),
                "the lowered shapes feed no blocker witness — the mint claims them");

        // Probe OFF leaves the refined channel untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(fws);
        assertEquals(irFlat, render(off, flatQtyClaim));
        assertEquals("none", off.implicitRootPipeBreakdown());
        assertEquals("none", off.implicitRootPipeWitnessSamples());
    }

    @Test
    void blockerProbeDecodesBareFunctionPipeBodies() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #484 — the pick-A decode-first opener: the #483 residue map's dominant class (the
        // bare-FUNCTION-application extract bodies — `extractPipe.unprovable.symbol:RFunction`,
        // 787 nested + 293 flat = 1,080 at population, plus the 12-occurrence bare `thenArg`
        // face and the listLiteral/filter compositions) restates the PLANNED callable-output
        // arm's admission per occurrence BEFORE the arm exists (the #480–#483 law), refining
        // the flat `symbol:RFunction` token IN PLACE at its unprovableSourceShapeToken seat —
        // EVERY channel that composes the token carries the facets at once, Σ facets ≡ each
        // flat count per cell-channel BY CONSTRUCTION. The element form of a bare-function
        // body is the CALLEE's OUTPUT (legacy ReferenceHandler.renderImplicitFunctionInvocation
        // renders `<callee>.evaluate(<binding>.get())` — the applied value IS the output — and
        // the checker types the bare reference off fn.output()): the callable-output identity.
        // (1) The FLAT claimable face: `trades extract ToLeg then filter qty exists` — the
        //     extract body is a bare reference to ToLeg (Trade → Leg (1..1): a non-meta,
        //     declared-DATA, single output) — the facet reads outputData, THE full claim face
        //     (the planned allowlist leg proves the output non-meta, the planned derivation
        //     leg proves the element data type = Leg — the declared-typeCall channel).
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
        RWorkspace fws = RWorkspace.build(java.util.List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr flatExtract = assertInstanceOf(RExtractExpr.class, flatThen.argument(),
                "the extract must bind tighter than then — the flat face's resolved argument");
        RSymbolReference flatBody = assertInstanceOf(RSymbolReference.class,
                flatExtract.body().body(),
                "the extract body must be the bare symbol reference — the point-free shape");
        assertInstanceOf(RFunction.class, flatBody.symbol().orElseThrow(),
                "the bare body must resolve to the FUNCTION — the symbol:RFunction face");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        RExistenceExpr flatQtyClaim = assertInstanceOf(RExistenceExpr.class,
                flatFilter.body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler flatProbed = compiler(fws);
        flatProbed.setBlockerProbeForTest(true);
        String irFlat = render(flatProbed, flatQtyClaim);
        assertEquals(legacyRender(legacyCompiler(fws), flatQtyClaim), irFlat,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, flatProbed.irDrivenCount(),
                "the flat bare-function-piped claim must be IR-DRIVEN — the #484 widening's"
                        + " meter witness");
        assertEquals("none", flatProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read"
                        + " thenArg.unprovable.extractPipe.provable.typeOk=1 on both channels"
                        + " under the PLANNED tier, and symbol:RFunction.outputData=1 under the"
                        + " facet decode; the commit-2 receipts carry both readings)");
        assertEquals("none", flatProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (2) The NESTED claimable face: the elided argument resolves to the INNER then
        //     (`trades then extract ToLeg`) — the #482 thenPipe sub-decode refines through the
        //     extract-body facet into the callable-output facet, the composition three laws deep.
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
        RWorkspace nws = RWorkspace.build(java.util.List.of(nestedModel)).workspace();
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
                nestedOuterThen.body().orElseThrow().body(),
                "the outer then body must wrap the outer filter");
        RExistenceExpr nestedQtyClaim = assertInstanceOf(RExistenceExpr.class,
                nestedFilter.body().body(),
                "the outer filter body must be the parsed qty-exists claim");

        IRExpressionCompiler nestedProbed = compiler(nws);
        nestedProbed.setBlockerProbeForTest(true);
        String irNested = render(nestedProbed, nestedQtyClaim);
        assertEquals(legacyRender(legacyCompiler(nws), nestedQtyClaim), irNested,
                "the widened nested claim must render byte-identically — the oracle-rendered"
                        + " base + the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, nestedProbed.irDrivenCount(),
                "the nested bare-function-piped claim must be IR-DRIVEN — the #482 then descent"
                        + " + #483 extract descent composing with the #484 callable-output leg");
        assertEquals("none", nestedProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness (pre-teach"
                        + " this carrier read"
                        + " thenArg.unprovable.thenPipe.unprovable.extractPipe.provable.typeOk=1"
                        + " on both channels under the PLANNED tier; the commit-2 receipts carry"
                        + " that reading)");
        assertEquals("none", nestedProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (3) The META-output residue face: the callee's output carries [metadata reference] —
        //     the applied value carries meta, the planned allowlist's non-meta gate fails.
        //     DECLINED whole (the safe direction) — this face stays post-teach.
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
        RWorkspace mws = RWorkspace.build(java.util.List.of(metaModel)).workspace();
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
                        + " law: isMetaAnnotatedAttr matches annotationName == \"metadata\","
                        + " not mere annotation presence)");
        RExistenceExpr metaQtyClaim = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, metaThen.body().orElseThrow().body())
                        .body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler metaProbed = compiler(mws);
        metaProbed.setBlockerProbeForTest(true);
        String irMeta = render(metaProbed, metaQtyClaim);
        assertEquals(legacyRender(legacyCompiler(mws), metaQtyClaim), irMeta,
                "the meta-output face must keep the legacy fallback's own bytes");
        // RECUT at #504 (the pin-recut law): the outputMeta face's carriers now MINT the
        // shallow IRSynItemNav (the arm-B teach — the RETYPE keeps declining the meta output
        // exactly as before, but the un-retypeable nav/bare-attr lower and oracle-serve
        // legacy's own render, byte-asserted above) — the blocker-fed pipe channel is empty.
        assertEquals("none", metaProbed.implicitRootPipeBreakdown(),
                "the lowered shapes feed no blocker witness — the mint claims them");

        // (4) The MULTI-output claimable face (sized separately — the disclosure): the callee's
        //     output is Leg (0..*) — the applied values are the output's own elements (the #483
        //     route-grid flatten faces), so the planned arm admits cardinality-agnostically
        //     like the bare-attribute leg; the facet names the face for the honest sizing.
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
        RWorkspace uws = RWorkspace.build(java.util.List.of(multiModel)).workspace();
        RThenExpr multiThen = AstWalker.findFirst(uws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr multiExtract = assertInstanceOf(RExtractExpr.class, multiThen.argument());
        assertInstanceOf(RFunction.class,
                assertInstanceOf(RSymbolReference.class, multiExtract.body().body())
                        .symbol().orElseThrow(),
                "the multi-face body must resolve to the FUNCTION");
        RExistenceExpr multiQtyClaim = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, multiThen.body().orElseThrow().body())
                        .body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler multiProbed = compiler(uws);
        multiProbed.setBlockerProbeForTest(true);
        String irMulti = render(multiProbed, multiQtyClaim);
        assertEquals(legacyRender(legacyCompiler(uws), multiQtyClaim), irMulti,
                "the multi-output claim must render byte-identically — the oracle-rendered"
                        + " base + the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, multiProbed.irDrivenCount(),
                "the MULTI-output claim must be IR-DRIVEN — the arm is cardinality-agnostic"
                        + " like the bare-attribute leg (the applied values are the output's own"
                        + " elements, the #483 route-grid flatten faces); the outputDataMulti"
                        + " split lives in the facet decode receipts");
        assertEquals("none", multiProbed.implicitRootPipeBreakdown(),
                "post-teach the multi-output slice lowers and never reaches the witness");
        assertEquals("none", multiProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // Probe OFF leaves the refined channel untouched (the census channel's standing contract).
        IRExpressionCompiler off = compiler(fws);
        assertEquals(irFlat, render(off, flatQtyClaim));
        assertEquals("none", off.implicitRootPipeBreakdown());
        assertEquals("none", off.implicitRootPipeWitnessSamples());
    }

    @Test
    void blockerProbeDecodesRuleTopImplicitSources() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #485 — the pick-A decode-first opener: the #484 residue map's dominant remaining pipe
        // class (the rule-input faces — `noBinder` 1,038 + the flat `filter.elidedImplicit` 318,
        // ALL drr RULE, + the nested thenPipe filter residue 110) restates the PLANNED
        // rule-input arm's admission per occurrence BEFORE the arm exists (the #480–#484 law),
        // refining the flat `noBinder` token at its elidedPipeFacetToken seat AND the flat
        // `elidedImplicit` token at its unprovableSourceShapeToken seat IN PLACE — every channel
        // composing either token carries the facets at once, Σ facets ≡ each flat count per
        // cell-channel BY CONSTRUCTION. The elided implicit at RULE top level takes the RULE
        // INPUT's value (legacy ReferenceHandler.handle(RImplicitVariable)'s
        // isElidedOperandTopLevel route renders `MapperS.of(input)` — the fromRule-synthesized
        // input parameter — and the checker types the same face off the rule's from-type,
        // TypeInferenceEngine.computeImplicitItemType → inferRuleFromType, always withNoMeta):
        // THE RULE-INPUT identity — the element form is the rule's declared from-type, a
        // DECLARATION read.
        // (1) The FLAT direct claimable face: a rule-top elided filter — `filter leg exists`
        //     from Trade. The filter's grammar-elided argument IS the rule input; the walk finds
        //     no lambda and no switch (noBinder), the slot gate passes (the filter's argument
        //     slot), the walk hits the RRule with a data-typed from — the facet reads
        //     noBinder.fromData, THE full claim face.
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
        RWorkspace fws = RWorkspace.build(java.util.List.of(flatModel)).workspace();
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
                        + " EXACT gate predicate (the #483/#484 assert-the-FACE law)");
        RExistenceExpr flatClaim = assertInstanceOf(RExistenceExpr.class,
                flatFilter.body().body(),
                "the filter body must be the parsed exists claim");

        IRExpressionCompiler flatProbed = compiler(fws);
        flatProbed.setBlockerProbeForTest(true);
        String irFlat = render(flatProbed, flatClaim);
        assertEquals(legacyRender(legacyCompiler(fws), flatClaim), irFlat,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, flatProbed.irDrivenCount(),
                "the rule-top claim must be IR-DRIVEN — the #485 widening's meter witness"
                        + " (the refined ruleInputNav gate admits the in-lambda slice, the"
                        + " rule-input source arm proves the elided base)");
        assertEquals("none", flatProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read"
                        + " noBinder.fromData=1 on BOTH decode channels — ruleInputLambda +"
                        + " synItem; the commit-2 receipts carry that reading)");
        assertEquals("none", flatProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole claim left the witness population");

        // (2) The COMPOSED claimable face (the flat filter.elidedImplicit class): the pipe
        //     source resolves to a FILTER whose own elided argument is the rule input —
        //     `filter leg exists then extract leg` from Trade. The extract's elided source
        //     resolves to the then's ARGUMENT (the filter); the filter's inner is the rule-top
        //     implicit — the shape token composes filter.elidedImplicit.noBinder.fromData, and
        //     under the PLANNED tier the whole composition proves (the filter is
        //     element-preserving over the now-provable rule input).
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
        RWorkspace cws = RWorkspace.build(java.util.List.of(composedModel)).workspace();
        RRule composedRule = AstWalker.findFirst(cws.files().get(0), RRule.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RRule"));
        RThenExpr composedThen = assertInstanceOf(RThenExpr.class,
                composedRule.expression().orElseThrow(),
                "the rule body must be the then-chain");
        RFilterExpr composedFilter = assertInstanceOf(RFilterExpr.class, composedThen.argument(),
                "the then argument must be the rule-top filter");
        assertInstanceOf(RImplicitVariable.class, composedFilter.argument(),
                "the composed filter must carry the grammar-elided rule-input argument");
        RExtractExpr composedExtract = assertInstanceOf(RExtractExpr.class,
                composedThen.body().orElseThrow().body(),
                "the then body must be the extract");
        RSymbolReference composedLeg = assertInstanceOf(RSymbolReference.class,
                composedExtract.body().body(),
                "the extract body must be the bare leg reference — the claim seat");

        IRExpressionCompiler composedProbed = compiler(cws);
        composedProbed.setBlockerProbeForTest(true);
        String irComposed = render(composedProbed, composedLeg);
        assertEquals(legacyRender(legacyCompiler(cws), composedLeg), irComposed,
                "the widened composed claim must render byte-identically — the oracle-rendered"
                        + " base + the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, composedProbed.irDrivenCount(),
                "the filter-composed claim must be IR-DRIVEN — the filter arm's"
                        + " element-preserving recursion composing with the rule-input arm");
        assertEquals("none", composedProbed.implicitRootPipeBreakdown(),
                "post-teach the composed slice lowers and never reaches the witness (at the"
                        + " facet stage this carrier read"
                        + " thenArg.unprovable.filter.elidedImplicit.noBinder.fromData=1, and"
                        + " under the PLANNED tier thenArg.provable.typeOk=1 — the two-stage"
                        + " reading the commit-2 receipts carry)");
        assertEquals("none", composedProbed.implicitRootShapeBreakdown(),
                "no blocker at all — the whole composed claim left the witness population");

        // (3) The functionTop DECLINE face: an elided implicit at FUNCTION top level — legacy's
        //     isElidedOperandTopLevel walk stops at RFunction (a function body's input is not
        //     named `input`), so the planned arm declines whole. Hand-built at the #480
        //     fixture-2 seat (a parsed function-top elided form does not type-resolve its bare
        //     body nav — the fixture pins the STRUCTURE the facet walks).
        RWorkspace ws = workspace();
        IRExpressionCompiler fnTopProbed = compiler(ws);
        fnTopProbed.setBlockerProbeForTest(true);
        RFunction fnTop = new RFunction();
        fnTop.setName("FnTopFixtureFn");
        fnTop.attachToWorkspace(ws);
        RAttribute qtyFeature = new RAttribute();
        qtyFeature.setName("qty");
        qtyFeature.setCardinality(cardinality(1, false));
        qtyFeature.attachToWorkspace(ws);
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
        fnTopItem.attachToWorkspace(ws);
        fnTopNav.attachToWorkspace(ws);
        fnTopElided.attachToWorkspace(ws);

        String irFnTop = render(fnTopProbed, fnTopNav);
        assertEquals(legacyRender(legacyCompiler(ws), fnTopNav), irFnTop,
                "the function-top fixture must render byte-identically under the probe");
        // RECUT at #504 (the pin-recut law): the functionTop carrier now MINTS the shallow
        // IRSynItemNav (the arm-B teach — the rule-input RETYPE keeps its RFunction stop,
        // but the un-retypeable nav lowers and oracle-serves, byte-asserted above) — the
        // blocker-fed pipe channel is empty.
        assertEquals("none", fnTopProbed.implicitRootPipeBreakdown(),
                "the lowered shape feeds no blocker witness — the mint claims it");

        // Probe OFF leaves the refined channels untouched (the census channel's standing
        // contract).
        IRExpressionCompiler off485 = compiler(fws);
        assertEquals(irFlat, render(off485, flatClaim));
        assertEquals("none", off485.implicitRootPipeBreakdown());
        assertEquals("none", off485.implicitRootPipeWitnessSamples());
    }

    @Test
    void blockerProbeDecodesConditionalJoinFaces() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");

        // #487 — the pick-A decode-first opener: the residue map's dominant DECODED pipe face
        // (the extract-bodied conditional class 435 + the direct conditional pipe face 68 + the
        // thenPipe-direct 4 at population) restates the PLANNED RConditionalExpr widening's
        // admission per occurrence BEFORE the arm exists (the #480–#485 law), refining the flat
        // `conditional` token at its unprovableSourceShapeToken seat IN PLACE — every channel
        // composing it carries the facets at once, Σ facets ≡ each flat count per cell-channel
        // BY CONSTRUCTION — and intercepting the three pipe seats with the carrier-typed
        // variant (the #483 terminal-triple pattern). THE JOIN LAW: the element form of
        // `if c then A else B` is the arms' COMMON form — the runtime elements are the
        // EXECUTED arm's (legacy ControlFlowHandler: every route yields one arm's value), the
        // checker types the node withNoMeta(typeJoin.join(thenT, elseT))
        // (ExpressionTypeComputer.computeConditional; an absent/empty else types NOTHING — the
        // join's bottom — so the empty-else face types as the THEN arm alone), and each arm's
        // own allowlist proof is the ONLY meta protection (the withNoMeta wrap strips arm meta
        // from the cache channel — the L-029 law demands the per-arm proofs): the list-literal
        // all-IDENTICAL law lifted to the two-armed choice.
        // (1) The DIRECT sameType face: the pipe argument IS a conditional whose arms are
        //     same-typed input references — the walk resolves the outer filter's elided
        //     argument to the conditional (one then-hop), both arms prove (bare non-meta input
        //     attributes) and derive the SAME RDataType instance (Trade), and the carrier (the
        //     conditional itself — the claim seat's resolved source) reads typeOk
        //     (join(Trade, Trade) = Trade, withNoMeta by construction).
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
        RWorkspace fws = RWorkspace.build(java.util.List.of(flatModel)).workspace();
        RThenExpr flatThen = AstWalker.findFirst(fws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RConditionalExpr flatCond = assertInstanceOf(RConditionalExpr.class, flatThen.argument(),
                "the parenthesized conditional must be the then's ARGUMENT — the direct face's"
                        + " resolved source");
        assertTrue(flatCond.elseBranch().isPresent()
                        && !(flatCond.elseBranch().get() instanceof RListLiteral el
                                && el.elements().isEmpty()),
                "the genuine-else face — the else branch is present and value-bearing (the"
                        + " hasGenuineElseShape discriminator's EXACT predicate, the"
                        + " assert-the-FACE law)");
        RFilterExpr flatFilter = assertInstanceOf(RFilterExpr.class,
                flatThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        assertInstanceOf(RImplicitVariable.class, flatFilter.argument(),
                "the without-left then-filter must carry the grammar-elided argument");
        RExistenceExpr flatClaim = assertInstanceOf(RExistenceExpr.class, flatFilter.body().body(),
                "the filter body must be the parsed leg-exists claim");

        IRExpressionCompiler flatProbed = compiler(fws);
        flatProbed.setBlockerProbeForTest(true);
        String irFlat = render(flatProbed, flatClaim);
        assertEquals(legacyRender(legacyCompiler(fws), flatClaim), irFlat,
                "the widened claim must render byte-identically — the oracle-rendered base +"
                        + " the emitter's compose against the legacy fallback's own bytes");
        assertEquals(1, flatProbed.irDrivenCount(),
                "the direct conditional-piped claim must be IR-DRIVEN — the #487 JOIN-law"
                        + " widening's meter witness");
        assertEquals("none", flatProbed.implicitRootPipeBreakdown(),
                "post-teach the claimed slice lowers and never reaches the witness — the"
                        + " conservation signature (pre-teach this carrier read"
                        + " thenArg.unprovable.conditional.sameType.typeOk=1 on BOTH decode"
                        + " channels; the commit-2 receipts carry that reading)");
        assertEquals("none", flatProbed.implicitRootSourceBreakdown(),
                "the sources instrument empties with the claim (pre-teach this carrier read"
                        + " unprovableSource.elidedImplicit.resolves.conditional.sameType=1"
                        + " on both channels — the #485 dual-channel law)");

        // (2) The EXTRACT-BODIED sameType face — THE dominant decoded pipe class (435 at
        //     population): the extract's implicit body is a conditional over two same-typed
        //     item attributes; the type carrier stays the EXTRACT (the seat's resolved
        //     source), typed off its body = the conditional's join (Leg), withNoMeta.
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
        RWorkspace ews = RWorkspace.build(java.util.List.of(extractModel)).workspace();
        RThenExpr extractThen = AstWalker.findFirst(ews.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr extractExtract = assertInstanceOf(RExtractExpr.class, extractThen.argument(),
                "the extract must be the then's argument — the extract-bodied face's seat");
        assertInstanceOf(RConditionalExpr.class, extractExtract.body().body(),
                "the extract body must be the parsed conditional — the dominant decoded face");
        RFilterExpr extractFilter = assertInstanceOf(RFilterExpr.class,
                extractThen.body().orElseThrow().body(),
                "the then body must wrap the filter");
        RExistenceExpr extractClaim = assertInstanceOf(RExistenceExpr.class,
                extractFilter.body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler extractProbed = compiler(ews);
        extractProbed.setBlockerProbeForTest(true);
        String irExtract = render(extractProbed, extractClaim);
        assertEquals(legacyRender(legacyCompiler(ews), extractClaim), irExtract,
                "the widened extract-bodied claim must render byte-identically");
        assertEquals(1, extractProbed.irDrivenCount(),
                "the extract-bodied conditional claim must be IR-DRIVEN — the #483 body"
                        + " descent composing with the #487 JOIN leg");
        assertEquals("none", extractProbed.implicitRootPipeBreakdown(),
                "post-teach the dominant decoded face lowers whole (pre-teach this carrier"
                        + " read thenArg.unprovable.extractPipe.unprovable.conditional"
                        + ".sameType.typeOk=1 on both channels; the commit-2 receipts carry"
                        + " that reading)");

        // (3) The elseEmpty face: an else-less conditional — DefaultElseRule stamps the
        //     SYNTHETIC empty-list else, hasGenuineElseShape reads it structurally empty, and
        //     the face admits on the then arm alone (an empty else contributes NO elements;
        //     the checker's own join(thenT, NOTHING) = thenT bottom rule keeps the carrier
        //     type the then form — typeOk).
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
        RWorkspace mws = RWorkspace.build(java.util.List.of(emptyModel)).workspace();
        RThenExpr emptyThen = AstWalker.findFirst(mws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr emptyExtract = assertInstanceOf(RExtractExpr.class, emptyThen.argument());
        RConditionalExpr emptyCond = assertInstanceOf(RConditionalExpr.class,
                emptyExtract.body().body(),
                "the extract body must be the else-less conditional");
        assertTrue(emptyCond.elseBranch().isEmpty()
                        || emptyCond.elseBranch().get() instanceof REmptyLiteral
                        || (emptyCond.elseBranch().get() instanceof RListLiteral sl
                                && sl.elements().isEmpty()),
                "the empty-else face — absent, `empty`, or the DefaultElseRule synthetic empty"
                        + " list (the structural-emptiness discriminator's EXACT predicate)");
        RExistenceExpr emptyClaim = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, emptyThen.body().orElseThrow().body())
                        .body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler emptyProbed = compiler(mws);
        emptyProbed.setBlockerProbeForTest(true);
        String irEmpty = render(emptyProbed, emptyClaim);
        assertEquals(legacyRender(legacyCompiler(mws), emptyClaim), irEmpty,
                "the widened empty-else claim must render byte-identically");
        assertEquals(1, emptyProbed.irDrivenCount(),
                "the empty-else conditional claim must be IR-DRIVEN — admitted on the then"
                        + " arm alone, the join's bottom rule");
        assertEquals("none", emptyProbed.implicitRootPipeBreakdown(),
                "post-teach the elseEmpty face lowers whole (pre-teach this carrier read"
                        + " thenArg.unprovable.extractPipe.unprovable.conditional.elseEmpty"
                        + ".typeOk=1 on both channels; the commit-2 receipts carry that"
                        + " reading)");

        // (4) The typeDiffers.elseSuper face: the arms derive DIFFERENT data types with the
        //     else form the then form's ANCESTOR (ExtLeg extends Leg) — TypeJoin's subtype
        //     shortcut makes the join the supertype; the planned #487 derivation DECLINES the
        //     face (the list-literal mixed-decline law — sized for a future join-widening),
        //     while the shape law admits it, so the face carries the carrier verdict.
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
        RWorkspace sws = RWorkspace.build(java.util.List.of(superModel)).workspace();
        RThenExpr superThen = AstWalker.findFirst(sws.files().get(0), RThenExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RThenExpr"));
        RExtractExpr superExtract = assertInstanceOf(RExtractExpr.class, superThen.argument());
        assertInstanceOf(RConditionalExpr.class, superExtract.body().body(),
                "the extract body must be the mixed-form conditional");
        RExistenceExpr superClaim = assertInstanceOf(RExistenceExpr.class,
                assertInstanceOf(RFilterExpr.class, superThen.body().orElseThrow().body())
                        .body().body(),
                "the filter body must be the parsed qty-exists claim");

        IRExpressionCompiler superProbed = compiler(sws);
        superProbed.setBlockerProbeForTest(true);
        String irSuper = render(superProbed, superClaim);
        assertEquals(legacyRender(legacyCompiler(sws), superClaim), irSuper,
                "the mixed-form carrier must render byte-identically under the probe");
        // v3.3 seat 4 (PR #640): the `qty exists` filter body's bare attribute is an admitted existence operand now
        // (the #640 admission), so the third claim serves through the oracle-root existence serve: irDriven 2 -> 3
        assertEquals(3, superProbed.irDrivenCount(),
                "the mixed-form face splits BY CHANNEL — the retype/oracle route claims the"
                        + " synthetic-item renders off the source's CACHED join type (the"
                        + " checker's own withNoMeta(join(ExtLeg, Leg)) = Leg — the subtype"
                        + " shortcut; the shape law admits, mirroring the mixed-list"
                        + " precedent), and since the #640 existence-operand admission the"
                        + " bare-attr filter body lowers too (the same-instance derivation the"
                        + " NAV arm declined is not the existence operand's gate)");
        assertEquals("none", superProbed.implicitRootPipeBreakdown(),
                "the pipe decode never fires post-teach — the mixed conditional IS"
                        + " allowlist-provable (the shape law), so the source leaves the"
                        + " unprovable population; the decline moved to the arm's derivation"
                        + " gate (pre-teach this carrier read typeDiffers.elseSuper.typeOk=1"
                        + " on both channels — the commit-2 receipts carry that reading)");
        assertEquals("none", superProbed.implicitRootSourceBreakdown(),
                "the sources channel empties too — the retype/oracle route claimed the"
                        + " implicit renders, so no equivalent ever declined into the"
                        + " source decode");
        // #528: the bare-attr claim is no longer BLOCKED at all — the L-113 conversion mints
        // the shallow kind for the sourceElementUnresolved family, so the claim LOWERS and the
        // blocker probe never runs on it. BOTH decode channels empty together (they are fed by
        // the same probe, not by the arm's internal verdict): the ARM still declines this shape
        // — proven adapter-side by the #528 retense in ExpressionToIRAdapterTest, which asserts
        // the lowering is the SHALLOW kind and never the arm's derived navigation.
        assertEquals("none", superProbed.implicitRootShapeBreakdown(),
                "the shape channel empties with the claim's decline");
        assertEquals("none",
                superProbed.implicitRootArmBreakdown(),
                "the arm channel empties with it — same probe, one gate; the arm's own"
                        + " decline is proven adapter-side by the #528 kind retense");

        // Probe OFF leaves the refined channels untouched (the census channel's standing
        // contract).
        IRExpressionCompiler off487 = compiler(fws);
        assertEquals(irFlat, render(off487, flatClaim));
        assertEquals("none", off487.implicitRootPipeBreakdown());
        assertEquals("none", off487.implicitRootSourceBreakdown());
    }

    @Test
    void bareAttrAndChainClaimsAreIrDrivenViaTheOracleRenderer() {
        // #480 — the arms end-to-end: a bare in-lambda ITEM-feature read and a bound-chain
        // disguise (the #479 witness's pre-sized claimable shapes) ADAPT via their
        // legacy-equivalent item navigations (the #479 retype at the synthetic base + the
        // cache-boundary rebuild stamping the RAW node's range) and EMIT with the base served by
        // the WIDENED synthetic-item oracle renderer — the raw claim node correlates by range and
        // the renderer hands legacy handle(RImplicitVariable) the fresh implicit legacy itself
        // builds on this exact path, so the bytes are the legacy fallback's own by construction
        // (asserted, plus the DRIVEN meter as the teach's witness). Under the PROBE the claimed
        // shapes never reach the witness channels — the conservation signature in miniature (the
        // pre-teach `claims` readings live in the commit-2 receipts).
        String bareSource = """
                namespace "test.step480"

                type Leg:
                    rate number (1..1)

                func BareFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract rate
                """;
        RModel bareModel = AstBuilder.buildFromString(bareSource, "step480-bare-driven.rosetta");
        RWorkspace bws = RWorkspace.build(java.util.List.of(bareModel)).workspace();
        RExtractExpr bareExtract = AstWalker.findFirst(bws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction bareLambda = assertInstanceOf(RInlineFunction.class, bareExtract.body());
        RSymbolReference bareRate = assertInstanceOf(RSymbolReference.class, bareLambda.body());

        RExistenceExpr bareClaim = new RExistenceExpr();
        bareClaim.setOp(ExistenceOp.EXISTS);
        bareClaim.setArgument(bareRate);
        bareClaim.setParent(bareLambda);

        IRExpressionCompiler bareProbed = compiler(bws);
        bareProbed.setBlockerProbeForTest(true);
        String irBare = render(bareProbed, bareClaim);
        assertEquals(legacyRender(legacyCompiler(bws), bareClaim), irBare,
                "the oracle-rendered base + the emitter's hop compose must be the legacy bytes");
        assertEquals(1, bareProbed.irDrivenCount(),
                "the bare-attr claim must be IR-DRIVEN — the #480 teach's meter witness");
        assertEquals("none", bareProbed.implicitRootShapeBreakdown(),
                "a claimed shape never reaches the witness — the conservation signature");
        assertEquals("none", bareProbed.implicitRootArmBreakdown(),
                "post-teach the arm channel reads none for a claimed shape");

        String chainSource = """
                namespace "test.step480"

                type Inner:
                    x number (1..1)

                type Leg:
                    inner Inner (1..1)

                func ChainFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract inner -> x
                """;
        RModel chainModel = AstBuilder.buildFromString(chainSource, "step480-chain-driven.rosetta");
        RWorkspace cws = RWorkspace.build(java.util.List.of(chainModel)).workspace();
        RExtractExpr chainExtract = AstWalker.findFirst(cws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed chain model contains no RExtractExpr"));
        RInlineFunction chainLambda = assertInstanceOf(RInlineFunction.class, chainExtract.body());
        REnumValueRef chainNav = assertInstanceOf(REnumValueRef.class, chainLambda.body());

        RExistenceExpr chainClaim = new RExistenceExpr();
        chainClaim.setOp(ExistenceOp.EXISTS);
        chainClaim.setArgument(chainNav);
        chainClaim.setParent(chainLambda);

        // The chain renders under a REAL statement scope on BOTH routes: the second hop's
        // lambda-var naming goes through the deferred registry (the #478 channel — identical by
        // construction), where the scope-less unit fallback splits per legacy arm (`_`-prefixed
        // for the item arm, bare for the receiver-type arm) and would diverge cosmetically.
        IRExpressionCompiler chainCompiler = compiler(cws);
        String irChain = renderScoped(chainCompiler, chainClaim);
        assertEquals(legacyRenderScoped(legacyCompiler(cws), chainClaim), irChain,
                "the 2-hop chain over the oracle-rendered base must be the legacy bytes");
        assertEquals(1, chainCompiler.irDrivenCount(),
                "the chain claim must be IR-DRIVEN — the #480 teach's meter witness");

        // The residue: the SAME bare read OUTSIDE any lambda — the ARM declines it (the
        // adapter-level negative is locked in the rune-ir suite: the composed
        // attrOutsideFunction.noFilterExtractBinder token), so the enclosing exists claim
        // DECLINES here; the nested bare attr then drives via the STANDING L-113 root relabel
        // inside the legacy fallback (super.visitSymbolReference — bytes provably legacy's own),
        // NOT via the #480 arm. Byte-identity + the declined claim are the residue's witnesses.
        RFunction bareFn = AstWalker.findFirst(bws.files().get(0), RFunction.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFunction"));
        RAttribute rateAttr = assertInstanceOf(RAttribute.class, bareRate.symbol().orElseThrow());
        RSymbolReference outsideRef = new RSymbolReference();
        outsideRef.setName("rate");
        outsideRef.setResolvedSymbol(rateAttr);
        RExistenceExpr outsideClaim = new RExistenceExpr();
        outsideClaim.setOp(ExistenceOp.EXISTS);
        outsideClaim.setArgument(outsideRef);
        outsideRef.setParent(outsideClaim);
        outsideClaim.setParent(bareFn);

        IRExpressionCompiler residue = compiler(bws);
        assertEquals(legacyRender(legacyCompiler(bws), outsideClaim), render(residue, outsideClaim),
                "the residue outside the filter/extract slice stays on legacy byte-identically");
        // v3.3 seat 4 (PR #640): the #640 existence-operand admission (D54 item 3) lowers the bare attribute as an
        // existence operand everywhere the mint reaches, so the enclosing exists claim now serves through the
        // oracle-root existence serve (the bytes above stay legacy's own): irDeclined 1 -> 0, irDriven 1 -> 2 (the
        // standing L-113 relabel was the one driven claim before; PR #640 round 1, cq NIT-3)
        assertEquals(0, residue.irDeclinedCount(),
                "the exists claim over the outside residue LOWERS since the #640 existence-operand admission"
                        + " (it declined before: the #480 arm claimed exactly the witnessed slice)");
        assertEquals(2, residue.irDrivenCount(),
                "two driven claims: the exists root through the oracle-root existence serve, and the re-entered bare"
                        + " attribute through the standing L-113 relabel inside legacy's render");
    }

    @Test
    void drivenMetricSplitSeparatesLoweredFromDelegatedMass() {
        // #493 — the driven-metric split (the #492 relabel-banking discovery's mandate): of
        // irDrivenCount, the DELEGATED share is the mass counted at the eight wholesale-legacy-
        // render seats (the L-111/L-109d/L-109e/L-112/L-113 relabel channels, the L-109 pf residue
        // belt, the #471 claim-root + #472 composition delegations); the complement is IR-LOWERED
        // (the adapter lowered the claim and the leaf emitter emitted it). Two fixtures pin the
        // split's two faces + the two-channel conservation (total ≡ Σ of the eight seat
        // sub-counters) the D11 ring hard-asserts at corpus scale.
        String source = """
                namespace "test.step493"

                type Leg:
                    rate number (1..1)

                func BareFunc:
                    inputs:
                        legs Leg (0..*)
                    output:
                        result number (0..*)
                    set result:
                        legs extract rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step493-split.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();

        // (a) A pure IR-LOWERED emission — the emitter composes the bytes itself: driven moves,
        // the delegated total does NOT.
        IRExpressionCompiler lowered = compiler(ws);
        assertEquals("MapperS.of(\"hello\")", render(lowered, string("hello")));
        assertEquals(1, lowered.irDrivenCount(), "the literal must drive through the emitter");
        assertEquals(0, lowered.irDrivenDelegatedCount(),
                "an emitter-composed render is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(lowered), lowered.irDrivenDelegatedCount(),
                "the Σ conservation holds on the lowered fixture (all seats zero)");

        // (b) The L-113 implicit-attr seat — CONVERTED at #528 (the delegated-seat serve at the
        // last live relabel belt): a bare attribute reference outside any filter/extract binder
        // now ADAPTS (the shallow IRImplicitAttrNav mint — the walk-family residue the arm
        // cannot build an equivalent for), so the claim is IR-LOWERED, not delegated, and the
        // render is unchanged — super.visitSymbolReference either way (the belt's own line is
        // the oracle dispatch's bareSymbolRef serve). The lock asserts exactly that pair:
        // legacy's bytes BY IDENTITY, and the accounting moved off the delegated channel.
        RFunction fn = AstWalker.findFirst(ws.files().get(0), RFunction.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFunction"));
        RExtractExpr extract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RInlineFunction lambda = assertInstanceOf(RInlineFunction.class, extract.body());
        RSymbolReference lambdaRate = assertInstanceOf(RSymbolReference.class, lambda.body());
        RAttribute rateAttr = assertInstanceOf(RAttribute.class, lambdaRate.symbol().orElseThrow());
        RSymbolReference outsideRef = new RSymbolReference();
        outsideRef.setName("rate");
        outsideRef.setResolvedSymbol(rateAttr);
        outsideRef.setParent(fn);

        IRExpressionCompiler delegated = compiler(ws);
        assertEquals(legacyRender(legacyCompiler(ws), outsideRef), render(delegated, outsideRef),
                "the L-113 relabel renders legacy's own bytes by construction");
        assertEquals(1, delegated.irDrivenCount(), "the seat counts driven (the standing law)");
        assertEquals(0, delegated.irDrivenDelegatedCount(),
                "#528: the claim is IR-LOWERED — the mint gave it a lowering, so the #516"
                        + " accounting law counts it lowered whatever the render route");
        assertEquals(0, delegated.implicitAttrNavDrivenCount(),
                "the belt is UNREACHABLE for the converted class — the frozen-zero residue"
                        + " meter (a positive value would mean a mint regression)");
        assertEquals(delegatedSubCounterSum(delegated), delegated.irDrivenDelegatedCount(),
                "two-channel conservation: the delegated total ≡ Σ of the eight seat sub-counters");
    }

    /**
     * The shared #516 delegated-seat-serve fixture: {@code TakesList} takes a {@code (0..*)} number,
     * so a bare {@code (1..1)} input argument trips the SINGLETON_LIST_ARG evaluate-arg arm — at the
     * claim ROOT in {@code CallWithSingleton} (the #471/#473 claim-root seat) and NESTED under a
     * meta-free equality root in {@code CompareCall} (the #472 composition router; meta-free binary
     * roots are NOT oracle-root-shaped, so the claim stays scanned). Parsed + workspace-built (the
     * derived-state-fixture law — the cardinality read is the engine's own).
     */
    private static RWorkspace step516ServeWorkspace() {
        String source = """
                namespace "test.step516"

                func TakesList:
                    inputs:
                        xs number (0..*)
                    output:
                        result number (1..1)
                    set result:
                        1.0

                func CallWithSingleton:
                    inputs:
                        x number (1..1)
                    output:
                        result number (1..1)
                    set result:
                        TakesList(x)

                func CompareCall:
                    inputs:
                        x number (1..1)
                    output:
                        result boolean (1..1)
                    set result:
                        TakesList(x) = 1.0
                """;
        RModel model = AstBuilder.buildFromString(source, "step516-delegated-seat-serve.rosetta");
        return RWorkspace.build(java.util.List.of(model)).workspace();
    }

    @Test
    void claimRootCoercingCallServesLoweredWithLegacyBytes() {
        // #516 — THE DELEGATED-SEAT SERVE at the claim-root seat: a call that IS the whole claim
        // whose arg trips the accepted SINGLETON_LIST_ARG arm renders the literal decline-fallback
        // line (super.visitSymbolReference — legacy's own bytes BY IDENTITY, character-identical to
        // the pre-#516 delegation) and counts IR-LOWERED on the seat's flip receipt: the delegated
        // total and the seat's frozen-zero sub-counter must NOT move (the #507 identity-serve
        // accounting law — the adapter's lowering exists, only the counter moved at #516), and the
        // seat's FIRST per-arm census reads the arm.
        RWorkspace ws = step516ServeWorkspace();
        RSymbolReference rootCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> !r.args().isEmpty())
                .filter(r -> !(r.parent() instanceof REqualityExpr))
                .findFirst().orElseThrow(() -> new AssertionError("no root call parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), rootCall), renderScoped(c, rootCall),
                "the claim-root serve must render legacy's own bytes (the literal fallback line —"
                        + " the #469 claim-root law, unchanged by the #516 accounting move)");
        assertEquals(1, c.claimRootServeLoweredCount(),
                "the seat's flip receipt reads the serve");
        assertTrue(c.claimRootServeBreakdown().contains("singletonListArg=1"),
                "the seat's FIRST per-arm census attributes the accepted arm: "
                        + c.claimRootServeBreakdown());
        assertEquals(0, c.claimRootDelegatedCount(),
                "the delegated sub-counter is the frozen zero post-#516");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the serve is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the serve fixture (all seats zero)");
        assertTrue(c.irDrivenCount() >= 1,
                "the serve counts driven (interior re-entries may add — the #471 zero-absorption"
                        + " signature: every downstream sub-attempt still occurs)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the served fixture");
    }

    @Test
    void postPinTrippedProvenRootServesLoweredWithLegacyBytes() {
        // #516 — the serve at the #472 composition router: an equality claim (a proven root class,
        // meta-free so it stays scanned) over a NESTED SINGLETON_LIST_ARG-tripping call serves the
        // caller's own fallback (compositionRootDelegation — super.visitEquality's literal bytes,
        // character-identical to the pre-#516 delegation) and counts IR-LOWERED on the router's
        // flip receipt with the arm attribution on the SERVE census line; the delegation array and
        // scalar stay the frozen zeros.
        RWorkspace ws = step516ServeWorkspace();
        REqualityExpr eq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no equality parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), eq), renderScoped(c, eq),
                "the router's serve must render legacy's own bytes (the caller's own fallback —"
                        + " the #469/#472 proof pack, unchanged by the #516 accounting move)");
        assertEquals(1, c.postPinServeLoweredCount(),
                "the router's flip receipt reads the serve");
        assertTrue(c.postPinServeBreakdown().contains("singletonListArg=1"),
                "the serve census attributes the tripping arm: " + c.postPinServeBreakdown());
        assertTrue(c.postPinDelegationBreakdown().contains("singletonListArg=0"),
                "the delegation line is the frozen-zero honest-residue meter: "
                        + c.postPinDelegationBreakdown());
        assertEquals(0, c.postPinDelegatedCount(),
                "the router's delegated scalar is the frozen zero post-#516");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the serve is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(0, c.postPinCoercionDeclinedCount(),
                "the scan routes the trip to the serve — the decline meter keeps its taught zero");
        assertEquals(0, c.irDeclinedCount(),
                "nothing declines on the served fixture (the nested call's re-entry serves at the"
                        + " claim-root seat — the #471 zero-absorption signature)");
    }

    @Test
    void arithmeticCountOperandRootServesWithLegacyBytes() {
        // #517 (the ARITHMETIC operand cluster at the compiler): `xs count + 1` lowers through
        // the adapter's list-op operand admission and the claim root — now oracle-root-shaped
        // via the #517 arithmetic shape leg (an arithmetic with a list-op operand) — serves
        // through the standing oracle-root arithmetic leg: the literal super.visitArithmetic
        // line, legacy's own witness/join/coercion composition inside it, byte-identical BY
        // IDENTITY. The receipt reads under the family token; nothing delegates and nothing
        // declines (the interior count re-enters through the oracle's own recursion and claims
        // natively at its own root — the #504 re-entrant law).
        String source = """
                namespace "test.step517c"

                func CountServe17:
                    inputs:
                        xs string (0..*)
                    output:
                        out int (1..1)
                    set out:
                        xs count + 1
                """;
        RModel model = AstBuilder.buildFromString(source, "step517-countserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RArithmeticExpr arith = AstWalker.findAll(ws.files().get(0), RArithmeticExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no arithmetic parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), arith), renderScoped(c, arith),
                "the count-operand arithmetic must render legacy's own bytes (the oracle-root"
                        + " arithmetic serve — the literal super.visitArithmetic line)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("arithmetic=1"),
                "the serve counts under the standing family token: "
                        + c.oracleRootLoweredBreakdown());
        assertTrue(c.irDrivenCount() >= 1,
                "the serve counts driven (interior re-entries may add — the #471"
                        + " zero-absorption signature)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void dispatchInputNavServesAtTheL111SeatWithLegacyBytes() {
        // #517 arms B1/B2 (the L-111 belt teaches at the seat): a variant-body
        // `<base-input> -> <feature>` disguise now LOWERS (FieldAccess / IRMetaAccess over the
        // #505 IRDispatchInputRef oracle leaf), so the L-111 seat's #507 identity-serve leg
        // fires — the literal super.visitEnumValueRef line, character-identical to the belt
        // relabel it replaces (bytes CANNOT move) — and the event counts LOWERED on the
        // inputNavIdentityLowered receipt while the belt counter stays SILENT (the event left
        // the delegated series — the belt-conversion signature).
        String source = """
                namespace "test.step517d"

                type Rates17c:
                    rate number (1..1)
                    code string (1..1)
                        [metadata scheme]

                enum Proc17cEnum:
                    A
                    B

                func Proc17c:
                    inputs:
                        rates Rates17c (1..1)
                        which Proc17cEnum (1..1)
                    output:
                        out number (1..1)

                func Proc17c(which: Proc17cEnum -> A):
                    set out:
                        rates -> rate

                func Proc17cCode:
                    inputs:
                        rates Rates17c (1..1)
                        which Proc17cEnum (1..1)
                    output:
                        outS string (1..1)

                func Proc17cCode(which: Proc17cEnum -> B):
                    set outS:
                        rates -> code
                """;
        RModel model = AstBuilder.buildFromString(source, "step517-dispatchserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();

        // Arm-B1: the PLAIN hop serve.
        REnumValueRef plainDisguise = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream()
                .filter(e -> "rates".equals(e.enumName()) && "rate".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no plain disguise parsed"));
        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), plainDisguise),
                renderScoped(c, plainDisguise),
                "the dispatch-head disguise must render legacy's own bytes (the #507"
                        + " identity-serve leg — the literal super.visitEnumValueRef line the"
                        + " belt relabel rendered pre-#517)");
        assertEquals(1, c.inputNavIdentityLoweredCount(),
                "the seat's identity-serve receipt reads the belt conversion");
        assertEquals(0, c.inputFeatureNavDrivenCount(),
                "the L-111 belt counter stays SILENT — the event left the delegated series");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the serve is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the serve fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the served fixture");

        // Arm-B2: the SCHEME meta hop serve (a fresh compiler — the per-claim counters).
        REnumValueRef metaDisguise = AstWalker.findAll(ws.files().get(0), REnumValueRef.class)
                .stream()
                .filter(e -> "rates".equals(e.enumName()) && "code".equals(e.valueName()))
                .findFirst().orElseThrow(() -> new AssertionError("no meta disguise parsed"));
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), metaDisguise),
                renderScoped(c2, metaDisguise),
                "the dispatch-head scheme hop must render legacy's own bytes (the same"
                        + " identity-serve line — the metaFeature.q:scheme residue converted)");
        assertEquals(1, c2.inputNavIdentityLoweredCount(),
                "the scheme-hop conversion reads on the same seat receipt");
        assertEquals(0, c2.inputFeatureNavDrivenCount(),
                "the belt stays silent on the meta face too");
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "no delegated mass moves on the meta fixture");
        assertEquals(delegatedSubCounterSum(c2), c2.irDrivenDelegatedCount(),
                "the Σ conservation holds on the meta fixture (all seats zero)");
        assertEquals(0, c2.irDeclinedCount(), "nothing declines on the served meta fixture");
    }

    @Test
    void equalityOperandClusterRootServesWithLegacyBytes() {
        // #518 (the EQUALITY operand cluster at the compiler): an existence-check operand
        // (`flags exists = flag`) and a MULTI-param pair (`codes = others`) lower through the
        // adapter's #518 equality admissions and the claim roots — now oracle-root-shaped via
        // the #518 equality shape legs — serve through the standing oracle-root equality leg:
        // the literal super.visitEquality line, legacy's own operand render + modifier default
        // + coercion/witness threading inside it, byte-identical BY IDENTITY. The receipts
        // read under the family token; nothing delegates and nothing declines.
        String source = """
                namespace "test.step518d"

                func ExistsEqServe18:
                    inputs:
                        flags boolean (0..1)
                        flag boolean (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        flags exists = flag

                func MultiEqServe18:
                    inputs:
                        codes string (0..*)
                        others string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes = others
                """;
        RModel model = AstBuilder.buildFromString(source, "step518-equalityserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();

        // Leg 1 — the existence-operand serve.
        REqualityExpr existsEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream()
                .filter(e -> AstWalker.findAll(e, RExistenceExpr.class).size() == 1)
                .findFirst().orElseThrow(() -> new AssertionError("no existence equality parsed"));
        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), existsEq), renderScoped(c, existsEq),
                "the existence-operand equality must render legacy's own bytes (the"
                        + " oracle-root equality serve — the literal super.visitEquality line)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("equality=1"),
                "the serve counts under the standing family token: "
                        + c.oracleRootLoweredBreakdown());
        assertTrue(c.irDrivenCount() >= 1,
                "the serve counts driven (interior re-entries may add — the #471"
                        + " zero-absorption signature)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");

        // Leg 2 — the MULTI-param pair serve (a fresh compiler — the per-claim counters).
        REqualityExpr multiEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream()
                .filter(e -> AstWalker.findAll(e, RExistenceExpr.class).isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no multi-param equality parsed"));
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), multiEq), renderScoped(c2, multiEq),
                "the MULTI-param equality must render legacy's own bytes (the same serve —"
                        + " legacy derives the All default + the MapperC join inside its line)");
        assertTrue(c2.oracleRootLoweredBreakdown().contains("equality=1"),
                "the MULTI-param serve counts under the same family token: "
                        + c2.oracleRootLoweredBreakdown());
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "no delegated mass moves on the MULTI-param fixture");
        assertEquals(delegatedSubCounterSum(c2), c2.irDrivenDelegatedCount(),
                "the Σ conservation holds on the MULTI-param fixture (all seats zero)");
        assertEquals(0, c2.irDeclinedCount(), "nothing declines on the MULTI-param fixture");
    }

    @Test
    void outputAliasNavServesAtTheL111SeatWithLegacyBytes() {
        // #518 arm-B2 (the usesOutput teach at the compiler): an OUTPUT-consuming alias-head
        // nav now LOWERS (the shallow IROutputAliasNav oracle leaf — the DISTINCT kind that
        // can never enter the FieldAccess-over-ALIAS native quiet-claim shape), so the L-111
        // seat's #507 identity-serve leg fires — the literal super.visitEnumValueRef line,
        // character-identical to the belt relabel it replaces (bytes CANNOT move; legacy's
        // own BUILDER-walk machinery stays inside the serve) — and the event counts LOWERED
        // on the inputNavIdentityLowered receipt while the belt counter stays SILENT (the
        // belt-conversion signature, the #517 dispatch witness's law at the usesOutput face).
        // The disguise node is hand-built with the L-111 channel stamped (the adapter
        // witness's leg-3 pattern — the engine-state-fixture law: the corpus census carries
        // the live channel-binding proof; this witness pins the seat's serve mechanics).
        String source = """
                namespace "test.step518e"

                type Leg18c:
                    rate number (1..1)

                func BuildLeg18c:
                    inputs:
                        seed number (1..1)
                    output:
                        result Leg18c (1..1)
                    alias current: result
                    set result:
                        Leg18c { rate: seed }
                """;
        RModel model = AstBuilder.buildFromString(source, "step518-outputaliasserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFunction fn = AstWalker.findAll(ws.files().get(0), RFunction.class).stream()
                .filter(f -> "BuildLeg18c".equals(f.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no BuildLeg18c parsed"));
        RAttribute rateAttr = AstWalker.findAll(ws.files().get(0), RAttribute.class).stream()
                .filter(a -> "rate".equals(a.name())).findFirst()
                .orElseThrow(() -> new AssertionError("no rate attribute parsed"));

        REnumValueRef usesOutputNav = new REnumValueRef();
        usesOutputNav.setEnumName("current");
        usesOutputNav.setValueName("rate");
        usesOutputNav.setResolvedInputFeature(rateAttr);
        usesOutputNav.setParent(fn);
        usesOutputNav.attachToWorkspace(ws);

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), usesOutputNav),
                renderScoped(c, usesOutputNav),
                "the usesOutput alias-head nav must render legacy's own bytes (the #507"
                        + " identity-serve leg — the literal super.visitEnumValueRef line the"
                        + " belt relabel rendered pre-#518)");
        assertEquals(1, c.inputNavIdentityLoweredCount(),
                "the seat's identity-serve receipt reads the belt conversion");
        assertEquals(0, c.inputFeatureNavDrivenCount(),
                "the L-111 belt counter stays SILENT — the event left the delegated series");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the serve is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the serve fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the served fixture");
    }

    @Test
    void existenceOperandClusterRootServesWithLegacyBytes() {
        // #519 (the EXISTENCE operand cluster at the compiler): a then-bound item existence
        // (`item exists` — the KIND-WIDE item admission) and an ELIDED existence (`… then
        // exists` — the without-left arm's null argument, SYNTHESIZED adapter-side) lower
        // through the #519 admissions and the claim roots — oracle-root-shaped via the
        // KIND-WIDE Existence-item shape leg — serve through the standing oracle-root
        // existence leg: the literal super.visitExistence line, legacy's OWN implicit
        // resolution + operand wrap inside it (for the elided leg legacy synthesizes its own
        // implicit — the IR operand is accounting-only), byte-identical BY IDENTITY. The
        // item leg deliberately reroutes the standing filter/extract-bound native existence
        // composes to this same serve BY IDENTITY (the #518 equality-item precedent — the
        // oracleRootLowered existence receipt carries the moved mass); the receipts read
        // under the family token, nothing delegates and nothing declines.
        String source = """
                namespace "test.step519d"

                func ThenItemExistsServe19:
                    inputs:
                        codes string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes then [ item exists ]

                func ThenElidedExistsServe19:
                    inputs:
                        amounts number (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        amounts then exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step519-existenceserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        java.util.List<RExistenceExpr> exists =
                AstWalker.findAll(ws.files().get(0), RExistenceExpr.class);
        assertEquals(2, exists.size(), "the fixture parses two existence checks");

        // Leg 1 — the then-bound item existence serve (the operandItem face).
        RExistenceExpr itemExists = exists.stream().filter(e -> e.argument() != null)
                .findFirst().orElseThrow(() -> new AssertionError("no item existence parsed"));
        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), itemExists),
                renderScoped(c, itemExists),
                "the item-operand existence must render legacy's own bytes (the oracle-root"
                        + " existence serve — the literal super.visitExistence line)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("existence=1"),
                "the serve counts under the standing family token: "
                        + c.oracleRootLoweredBreakdown());
        assertTrue(c.irDrivenCount() >= 1, "the serve counts driven");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");

        // Leg 2 — the ELIDED existence serve (a fresh compiler — the per-claim counters).
        RExistenceExpr elided = exists.stream().filter(e -> e.argument() == null)
                .findFirst().orElseThrow(() -> new AssertionError("no elided existence parsed"));
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), elided), renderScoped(c2, elided),
                "the elided existence must render legacy's own bytes (the same serve —"
                        + " legacy's without-left handling synthesizes its own implicit"
                        + " inside the line)");
        assertTrue(c2.oracleRootLoweredBreakdown().contains("existence=1"),
                "the elided serve counts under the same family token: "
                        + c2.oracleRootLoweredBreakdown());
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "no delegated mass moves on the elided fixture");
        assertEquals(delegatedSubCounterSum(c2), c2.irDrivenDelegatedCount(),
                "the Σ conservation holds on the elided fixture (all seats zero)");
        assertEquals(0, c2.irDeclinedCount(), "nothing declines on the elided fixture");
    }

    @Test
    void sumRootServesWithLegacyBytes() {
        // #519 arm C (the opSum teach at the compiler): a SUM claim root lowers (the #519
        // IRListOp.Kind.SUM adapt case) and — IRListOp roots are deliberately NOT
        // oracle-root-shaped — serves through the STANDING CollectionOpRenderer slot: the
        // verbatim CollectionHandler.handle call on the RAW node (the RAW-class dispatch is
        // kind-agnostic — the #499 collapse teach's slot), legacy's own element-type-derived
        // sum composition inside it, byte-identical BY IDENTITY. An INTERIOR sum routes any
        // containing root whole-legacy via the SUM-scoped containsOracleLeaf leg (the #511
        // COUNT-carve-out pattern inverted — COUNT/collapse natives untouched); no native
        // arm exists on any route (IRJavaLeafEmitter's nested chain ends in the standing
        // defer).
        String source = """
                namespace "test.step519e"

                type Trade19:
                    legs number (0..*)

                func SumTotal19:
                    inputs:
                        trade Trade19 (1..1)
                    output:
                        result number (1..1)
                    set result:
                        trade -> legs sum
                """;
        RModel model = AstBuilder.buildFromString(source, "step519-sumserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RListOpExpr sum = AstWalker.findAll(ws.files().get(0), RListOpExpr.class).stream()
                .filter(o -> o.op() == ListOp.SUM).findFirst()
                .orElseThrow(() -> new AssertionError("no sum parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), sum), renderScoped(c, sum),
                "the sum root must render legacy's own bytes (the standing"
                        + " CollectionOpRenderer slot — the verbatim CollectionHandler call)");
        assertTrue(c.irDrivenCount() >= 1,
                "the taught sum counts driven (the opSum face is retired)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the sum fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(),
                "nothing declines on the taught fixture — pre-#519 this claim was the opSum"
                        + " decline");
    }

    @Test
    void lambdaOpConsumerRootsServeWithLegacyBytes() {
        // #520 (the IRLambdaOp consumer cluster at the compiler): a logical over a lambda-op
        // operand (operand:IRLambdaOp — the isCoercedBooleanOperand growth) and a nav over a
        // lambda-op receiver (receiver:IRLambdaOp — the nav-over-lambda-op containsOracleLeaf
        // shape leg, NO native carve-out) lower through the #520 admissions and the claim
        // roots serve through the standing oracle-root legs: the literal super.visitLogical /
        // super.visitFeatureCall lines, legacy's OWN ComparisonResult.ofNullSafe coercion and
        // NavigationHandler receiver handling inside them, byte-identical BY IDENTITY. Both
        // shapes were 100%-decline faces pre-teach (the #504 conservation argument — no
        // standing native compose reroutes).
        String source = """
                namespace "test.step520c"

                type Leg20:
                    rate number (1..1)

                func LambdaLogicalServe20:
                    inputs:
                        codes string (0..*)
                    output:
                        out boolean (1..1)
                    set out:
                        codes exists and (codes filter [ item = "A" ])

                func LambdaNavServe20:
                    inputs:
                        legs Leg20 (0..*)
                    output:
                        out number (0..*)
                    set out:
                        legs filter [ item -> rate exists ] -> rate
                """;
        RModel model = AstBuilder.buildFromString(source, "step520-lambdaserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();

        // Leg 1 — the logical serve (the operand:IRLambdaOp face).
        RLogicalExpr logical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("no logical parsed"));
        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), logical),
                renderScoped(c, logical),
                "the lambda-op-operand logical must render legacy's own bytes (the"
                        + " oracle-root logical serve — the literal super.visitLogical line)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("logical=1"),
                "the serve counts under the standing family token: "
                        + c.oracleRootLoweredBreakdown());
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(),
                "nothing declines on the taught fixture — pre-#520 this claim was the"
                        + " operand:IRLambdaOp decline");

        // Leg 2 — the nav serve (the receiver:IRLambdaOp face; a fresh compiler — the
        // per-claim counters).
        RFeatureCall navOverFilter = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "rate".equals(fc.featureName())
                        && fc.receiver() instanceof RFilterExpr)
                .findFirst().orElseThrow(() -> new AssertionError("no nav-over-filter parsed"));
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), navOverFilter),
                renderScoped(c2, navOverFilter),
                "the lambda-op-receiver nav must render legacy's own bytes (the oracle-root"
                        + " navChain serve — the literal super.visitFeatureCall line)");
        assertTrue(c2.oracleRootLoweredBreakdown().contains("navChain=1"),
                "the nav serve counts under the standing family token: "
                        + c2.oracleRootLoweredBreakdown());
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "no delegated mass moves on the nav fixture");
        assertEquals(0, c2.irDeclinedCount(),
                "nothing declines on the nav fixture — pre-#520 this claim was the"
                        + " receiver:IRLambdaOp decline");
    }

    @Test
    void libraryApplyRootServesWithLegacyBytes() {
        // #520 (the library-apply mint at the compiler): an args-present library-function
        // call (`Min(a, b)` — the calleeNotFunction face's 100% RLibraryFunction class)
        // lowers to the childless shallow IRLibraryApply (the 43rd kind — the #502
        // IRMetaOutputApply pattern verbatim) and the claim root — O(1)-screened via
        // isOracleRootShape — serves through the standing oracle-root callArgs leg: the
        // literal super.visitSymbolReference line, legacy's OWN external-function threading
        // (the argument compiles + the execute call) inside it, byte-identical BY IDENTITY.
        // No leaf-emitter arm exists — a drift arrival declines at the kind-dispatch end,
        // never a native compose.
        String source = """
                namespace "test.step520d"

                library function Min(x number, y number) number

                func UseMinServe20:
                    inputs:
                        a number (1..1)
                        b number (1..1)
                    output:
                        out number (1..1)
                    set out:
                        Min(a, b)
                """;
        RModel model = AstBuilder.buildFromString(source, "step520-libserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RSymbolReference minCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "Min".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no Min call parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), minCall), renderScoped(c, minCall),
                "the library call must render legacy's own bytes (the oracle-root callArgs"
                        + " serve — the literal super.visitSymbolReference line)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("callArgs=1"),
                "the serve counts under the standing family token: "
                        + c.oracleRootLoweredBreakdown());
        assertTrue(c.irDrivenCount() >= 1,
                "the taught library call counts driven (the calleeNotFunction face is"
                        + " retired for the class)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — no delegated mass moves on the fixture");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(),
                "nothing declines on the taught fixture — pre-#520 this claim was the"
                        + " calleeNotFunction decline");
    }

    @Test
    void guardedItemNavClaimRootServesWithLegacyBytes() {
        // #523 (the guard co-design's COMPILER half): an RSymbolReference-rooted resolved
        // call whose subtree trips an ITEM-NAV arm serves the WHOLE claim via
        // super.visitSymbolReference — the decline's own render, byte-identical BY
        // CONSTRUCTION (the #469 claim-root law, arm-independent — the
        // claimRootDelegationArm javadoc's own clause). Leg 1: the EXPLICIT chained item
        // arg trips the standing raw-side recognizer (isItemRootedFeatureCall +
        // isGuardedItemNav's chained leg — pure AST, oracle-free). Leg 2: the DISGUISED
        // single-arrow arg trips the #523 call-branch probe (legacy's OWN
        // synthesizeImplicitItemChain the predicate BY CALL). Pre-#523 both claims were
        // adapter-declined (the argNav itemChain./itemSrcUnprovable. mirror), and a
        // pre-#523 admission would have DECLINED here (RSymbolReference outside the
        // router's proven set) — the co-design lands both sides together.
        String source = """
                namespace "test.step523d"

                type Sub23d:
                    x number (1..1)

                type Leg23d:
                    rate number (1..1)
                    sub Sub23d (1..1)

                type Trade23d:
                    legs Leg23d (0..*)

                func CChainServe23:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func CDisguisedServe23:
                    inputs:
                        v number (1..1)
                    output:
                        out number (1..1)
                    set out: v

                func FnServe23:
                    inputs:
                        trade Trade23d (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CChainServe23(item -> sub -> x)

                func FnDisguisedServe23:
                    inputs:
                        trade Trade23d (1..1)
                    output:
                        result number (0..*)
                    set result:
                        trade -> legs extract CDisguisedServe23(sub -> x)
                """;
        RModel model = AstBuilder.buildFromString(source, "step523-itemserve.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();

        // Leg 1 — the explicit chained item arg: the claim-root serve with legacy bytes.
        RSymbolReference chainCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "CChainServe23".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("no CChainServe23 call parsed"));
        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), chainCall),
                renderScoped(c, chainCall),
                "the chained-item-arg call must render legacy's own bytes (the #523"
                        + " item-nav claim-root serve — the literal super.visitSymbolReference"
                        + " line)");
        assertTrue(c.postPinServeBreakdown().contains("multiHopItemNav=1"),
                "the serve counts on the tripped arm's own census bucket: "
                        + c.postPinServeBreakdown());
        assertEquals(0, c.irDeclinedCount(),
                "nothing declines on the taught fixture — pre-#523 this claim was the"
                        + " argNav.itemChain.item.hop2 adapter decline, and a bare admission"
                        + " would have broken the meter here");

        // Leg 2 — the disguised single-arrow arg: the #523 call-branch probe trips the
        // SAME arm (a fresh compiler — the per-claim counters).
        RSymbolReference disguisedCall = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "CDisguisedServe23".equals(r.name()) && !r.args().isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("no CDisguisedServe23 call parsed"));
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), disguisedCall),
                renderScoped(c2, disguisedCall),
                "the disguised-item-arg call must render legacy's own bytes (the #523"
                        + " disguised-chain probe + the item-nav claim-root serve)");
        assertTrue(c2.postPinServeBreakdown().contains("multiHopItemNav=1"),
                "the disguised trip counts on the same arm bucket: "
                        + c2.postPinServeBreakdown());
        assertEquals(0, c2.irDeclinedCount(),
                "nothing declines on the disguised fixture — pre-#523 this claim was the"
                        + " argNav.itemSrcUnprovable.synItem.hop2 adapter decline");
    }

    @Test
    void constructorClaimLowersAtRootWithLegacyBytes() {
        // #494 — the RConstructorExpr teach (the D menu pick, the biggest untargeted family): a
        // ROOT constructor lowers to the deliberately-shallow IRConstruct and the leaf emitter
        // renders it via the range-correlated ConstructRenderer — super.visitConstructor's LITERAL
        // bytes (oracle-closed at both exits), counted IR-LOWERED (driven moves, the delegated
        // total does NOT — an adapter-lowered claim under the #493 split's definition).
        String source = """
                namespace "test.step494"

                type Party:
                    partyName string (1..1)

                func BuildParty:
                    output:
                        result Party (1..1)
                    set result:
                        Party { partyName: "ACME" }
                """;
        RModel model = AstBuilder.buildFromString(source, "step494-ctor.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RConstructorExpr ctor = AstWalker.findFirst(ws.files().get(0), RConstructorExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConstructorExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRender(legacyCompiler(ws), ctor), render(c, ctor),
                "the construct render must be legacy's own bytes (the oracle-closed claim)");
        // driven = 2: the constructor's own claim + the pair-value string literal's claim (the
        // oracle's per-pair compile re-enters the seam — the walk's event accounting, exactly the
        // production shape where nested values claim at their own roots).
        assertEquals(2, c.irDrivenCount(),
                "the root constructor AND its literal value must claim (driven)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "an adapter-lowered construct is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the construct fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the typed data-target fixture");
    }

    @Test
    void conditionalClaimLowersAtRootWithLegacyBytes() {
        // #495 — the RConditionalExpr teach (the A menu pick, the census-marked adapter-ready
        // family): a ROOT conditional lowers to the fully childed IRConditional
        // (adaptConditional's D1 all-or-nothing recursion) and the leaf emitter renders it via
        // the range-correlated ConditionalRenderer — super.visitConditional's LITERAL bytes
        // (oracle-closed at both exits), counted IR-LOWERED (driven moves, the delegated total
        // does NOT — an adapter-lowered claim under the #493 split's definition).
        String source = """
                namespace "test.step495"

                func PickLabel:
                    inputs:
                        flag boolean (1..1)
                    output:
                        result string (1..1)
                    set result:
                        if flag then "A" else "B"
                """;
        RModel model = AstBuilder.buildFromString(source, "step495-cond.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RConditionalExpr cond = AstWalker.findFirst(ws.files().get(0), RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConditionalExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), cond), renderScoped(c, cond),
                "the conditional render must be legacy's own bytes (the oracle-closed claim)");
        // driven = 4: the conditional's own claim + the condition ref + both branch literals (the
        // oracle's condition/branch compiles re-enter the seam — the walk's event accounting,
        // exactly the production shape where interior nodes claim at their own roots).
        assertEquals(4, c.irDrivenCount(),
                "the root conditional AND its three interior claims must drive");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "an adapter-lowered conditional is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the conditional fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the fully lowerable fixture");
    }

    @Test
    void conditionalCtorSlotClaimRendersLegacyBytes() {
        // #495 — the composed heart: an else-less `if <cond> then Type{...}` (the condVisit
        // probe's blocked.ctorSlot.then class, 320 events — every one this shape) lowers via the
        // claim-root ctor-slot ADMISSION (the #494 root-only law's one named exception) and
        // renders the oracle's bytes wholesale; the admitted shallow construct is render-inert —
        // the oracle's branch compile re-enters the seam and the constructor claims at its OWN
        // visitConstructor visit, exactly like every nested constructor since #494.
        String source = """
                namespace "test.step495"

                type Party:
                    partyName string (1..1)

                func MaybeParty:
                    inputs:
                        flag boolean (1..1)
                    output:
                        result Party (0..1)
                    set result:
                        if flag then Party { partyName: "ACME" }
                """;
        RModel model = AstBuilder.buildFromString(source, "step495-cond-ctor.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RConditionalExpr cond = AstWalker.findFirst(ws.files().get(0), RConditionalExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RConditionalExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), cond), renderScoped(c, cond),
                "the ctor-slot conditional render must be legacy's own bytes (oracle-closed)");
        // driven = 5 since #500 (4 at the #495 teach): the conditional root + the condition ref
        // + the constructor's own visit-claim + the constructor's pair-value literal + the ctor
        // machinery's internally-visited list literal, which the #500 arm-B seat now CLAIMS
        // (previously the same visit counted untargeted=RListLiteral=1 and invisible to driven —
        // the oracleRootLowered receipt below is the flip's own witness; conservation exact on
        // this fixture: driven 4→5, untargeted 1→0).
        assertEquals(5, c.irDrivenCount(),
                "the root conditional AND its interior claims (incl. the ctor's own and the"
                        + " #500-claimed list-literal visit) must drive");
        assertTrue(c.oracleRootLoweredBreakdown().contains("listLiteral=1"),
                "the #500 receipt reads the ctor machinery's list-literal visit as a serve");
        assertEquals(0, c.untargetedVisitCount(),
                "the list-literal visit left the untargeted meter — the teach's conservation"
                        + " signature on this fixture");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the composed claim is IR-LOWERED throughout — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the ctor-slot fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the admitted fixture");
    }

    @Test
    void lambdaClaimLowersAtRootWithLegacyBytes() {
        // #496 leg 1 — the lambda teach (the monster wave's A pick): ROOT extract and filter
        // lambdas lower to the fully childed IRLambdaOp (adaptLambdaOp's D1 all-or-nothing
        // recursion — receiver + body) and the leaf emitter renders them via the range-correlated
        // LambdaOpRenderer — super.visitExtract/super.visitFilter's LITERAL bytes (oracle-closed
        // at both exits), counted IR-LOWERED (driven moves, the delegated total does NOT).
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
                """;
        RModel model = AstBuilder.buildFromString(source, "step496-lambda.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));
        RFilterExpr filter = AstWalker.findFirst(ws.files().get(0), RFilterExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RFilterExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), extract), renderScoped(c, extract),
                "the extract render must be legacy's own bytes (the oracle-closed claim)");
        // driven = 3: the extract's own claim + the receiver ref + the body item (the oracle's
        // receiver/body compiles re-enter the seam and claim at their own roots).
        assertEquals(3, c.irDrivenCount(),
                "the root extract AND its two interior claims must drive");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "an adapter-lowered lambda is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the extract fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the fully lowerable fixture");

        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), filter), renderScoped(c2, filter),
                "the filter render must be legacy's own bytes (the oracle-closed claim)");
        assertEquals(3, c2.irDrivenCount(),
                "the root filter AND its two interior claims must drive");
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "an adapter-lowered lambda is IR-LOWERED — the delegated total must not move");
        assertEquals(0, c2.irDeclinedCount(), "nothing declines on the fully lowerable fixture");
    }

    @Test
    void lambdaCtorBodyClaimRendersLegacyBytes() {
        // #496 leg 1's second face — the body-slot ADMISSION (the #494 root-only law's SECOND
        // named exception, the #495 conditional-slot pattern at the lambda's body position): a
        // claim-ROOT extract whose body IS a constructor (the lambdaVisit probe's
        // blocked.ctorSlot.body class, 194 events) lowers via adaptConstructorShallow at the
        // body slot and renders the oracle's bytes wholesale; the admitted shallow construct is
        // render-inert — the oracle's body compile re-enters the seam and the constructor claims
        // at its OWN visitConstructor visit, exactly like every nested constructor since #494.
        String source = """
                namespace "test.step496"

                type Party:
                    partyName string (1..1)

                func RelabelParties:
                    inputs:
                        partyList Party (0..*)
                    output:
                        result Party (0..*)
                    set result:
                        partyList extract Party { partyName: "ACME" }
                """;
        RModel model = AstBuilder.buildFromString(source, "step496-lambda-ctor.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findFirst(ws.files().get(0), RExtractExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RExtractExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), extract), renderScoped(c, extract),
                "the ctor-body extract render must be legacy's own bytes (oracle-closed)");
        // driven = 4: the extract root + the receiver ref + the constructor's own visit-claim +
        // the constructor's pair-value literal (the oracle's compiles re-enter the seam at every
        // interior root).
        assertEquals(4, c.irDrivenCount(),
                "the root extract AND its interior claims (incl. the ctor's own) must drive");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the composed claim is IR-LOWERED throughout — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the ctor-body fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the admitted fixture");
    }

    @Test
    void inputNavConversionLowersAtRootWithLegacyBytes() {
        // #496 leg 2 — the L-111 CONVERSION (the honest-series flip): a disguised
        // `<input> -> <feature>` REnumValueRef ROOT claims through the QUIET adapter-first path —
        // the #478 adaptDisguisedInputNav arm lowers it to the typed single-hop FieldAccess over
        // the IRVariable head and the emitter renders it NATIVELY (the same proven emission the
        // RFeatureCall claim roots ring-prove) — counted LOWERED on the L-111 lowered receipt,
        // with the relabel seat's delegated counters UNTOUCHED (the event left the delegated
        // series entirely).
        String source = """
                namespace "test.step496"

                type Party:
                    partyName string (1..1)

                func GetName:
                    inputs:
                        party Party (1..1)
                    output:
                        result string (1..1)
                    set result:
                        party -> partyName
                """;
        RModel model = AstBuilder.buildFromString(source, "step496-inputnav.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        REnumValueRef nav = AstWalker.findFirst(ws.files().get(0), REnumValueRef.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no REnumValueRef"));
        assertTrue(nav.resolvedInputFeature().isPresent() && nav.resolvedAttributeChain().isEmpty(),
                "the fixture must parse as the L-111 disguised input-feature shape");

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), nav), renderScoped(c, nav),
                "the conversion render must equal legacy's disguised-root bytes (oracle-closed"
                        + " per shape — the proven single-hop input-head FieldAccess emission)");
        assertEquals(1, c.irDrivenCount(),
                "the converted root drives ONCE — the emitter composes the nav natively, no"
                        + " interior re-entry");
        assertEquals(1, c.inputFeatureNavLoweredCount(),
                "the L-111 lowered receipt reads the flip");
        assertEquals(0, c.inputFeatureNavDrivenCount(),
                "the relabel seat must NOT fire — the event left the delegated series");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the conversion is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the conversion fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(),
                "the quiet path records NO declines — the residue contract");
    }

    @Test
    void onlyElementClaimLowersAtRootWithLegacyBytes() {
        // #498 leg A — the deferred ONLY_ELEMENT operator joins the taught flat-list-op set: the
        // claim root `trade -> legs only-element` lowers whole (the receiver child load-bearing)
        // and the render is the CollectionOpRenderer oracle — CollectionHandler.handle on the
        // compiler's own instance, the very call super.visitListOp makes — so the inline `.get()`
        // collapse and its selfUnwrapping consumer-marker are legacy's own bytes BY CONSTRUCTION.
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
                """;
        RModel model = AstBuilder.buildFromString(source, "step498-onlyelement-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RListOpExpr collapse = AstWalker.findFirst(ws.files().get(0), RListOpExpr.class)
                .orElseThrow(() -> new AssertionError("parsed model contains no RListOpExpr"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), collapse), renderScoped(c, collapse),
                "the only-element render must be legacy's own bytes (the verbatim oracle)");
        assertEquals(2, c.irDrivenCount(),
                "the collapse root AND the receiver nav drive — the oracle's compileInterior"
                        + " re-enters the seam at the receiver's own root");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the oracle-closed list-op claim is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the only-element fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void ruleApplyClaimLowersAtRootWithLegacyBytes() {
        // #498 leg B — the args-present rule invocation claims at its root: the adaptApply rule
        // leg lowers `IdRule(trade)` to IRApply{RULE, args} and the emitter serves it via the
        // root-site RuleApplyRenderer — getReferenceHandler().handle(ref, ctx, this), the ONE
        // line super.visitSymbolReference IS — so the bytes are the legacy fallback's own BY
        // IDENTITY, and the argument re-enters this compiler through the oracle's own compile
        // (driven at its own claim seat).
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
                """;
        RModel model = AstBuilder.buildFromString(source, "step498-ruleapply-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RSymbolReference call = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> !r.args().isEmpty()).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no args-present ref"));
        assertInstanceOf(RRule.class, call.symbol().orElseThrow(),
                "the callee must resolve to the reporting rule");

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), call), renderScoped(c, call),
                "the rule-apply render must be legacy's own bytes (the whole-reference oracle)");
        assertEquals(2, c.irDrivenCount(),
                "the apply root AND its argument drive — the oracle's compile re-enters the seam"
                        + " at the arg's own root");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the oracle-closed rule apply is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the rule-apply fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void navOverCollapseClaimLowersWithLegacyBytes() {
        // #499 arm A — a navigation over an ONLY_ELEMENT collapse claims at its root: the adapter
        // admits the collapse receiver (isCollapseNavBase — the census-live param/nav child) and
        // the emitter composes the nested collapse `<child>.get()` + the nav re-wrap
        // `MapperS.of(<recv>.get())` — legacy NavigationHandler's nav_after_get_rewrap facet
        // verbatim, so the render is legacy's own bytes.
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
                """;
        RModel model = AstBuilder.buildFromString(source, "step499-navovercollapse-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFeatureCall nav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class).stream()
                .filter(fc -> fc.receiver() instanceof RListOpExpr).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no collapse-receiver nav"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), nav), renderScoped(c, nav),
                "the nav-over-collapse render must be legacy's own bytes (the re-wrap + the"
                        + " inline collapse composed natively)");
        assertEquals(1, c.irDrivenCount(),
                "the nav root drives ONCE — the emitter composes the collapse and the inner nav"
                        + " internally, no interior re-entry (the #499 absorption class)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the native nav claim is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the nav-over-collapse fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void metaNavClaimLowersAtRootWithLegacyBytes() {
        // #499 arm B — a meta-feature navigation claims at its root: the adapter's meta arm
        // lowers `trade -> details -> tradeId` (a [metadata scheme] feature over a lowering
        // receiver chain — the 3-hop fixture keeps the META hop a genuine RFeatureCall, since a
        // 2-name chain takes the grammar's disguised REnumValueRef parse) to IRMetaAccess and
        // the emitter serves it via the root-site MetaNavRenderer — super.visitFeatureCall(site,
        // ctx), the LITERAL L-109d relabel-belt line — so the bytes are the belt's own BY
        // IDENTITY while the seat's accounting flips delegated → lowered (metaNavLoweredCount
        // up, the metaNavDrivenCount belt residue untouched).
        String source = """
                namespace "test.step499b"

                type Details:
                    tradeId string (1..1)
                        [metadata scheme]

                type Trade:
                    details Details (1..1)

                func MetaNav:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (1..1)
                    set result:
                        trade -> details -> tradeId
                """;
        RModel model = AstBuilder.buildFromString(source, "step499-metanav-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFeatureCall metaNav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class).stream()
                .filter(fc -> "tradeId".equals(fc.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no tradeId nav"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), metaNav), renderScoped(c, metaNav),
                "the meta-nav render must be legacy's own bytes (the literal super.visitFeatureCall"
                        + " oracle at the claim root)");
        assertEquals(2, c.irDrivenCount(),
                "the meta-access root AND the receiver drive — the oracle's own receiver compile"
                        + " re-enters the seam at the receiver's root");
        assertEquals(1, c.metaNavLoweredCount(),
                "the #499 conversion receipt reads the flip — the seat's lowered channel");
        assertEquals(0, c.metaNavDrivenCount(),
                "the relabel belt must NOT fire — the event left the delegated series");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the conversion is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the meta-nav fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void metaConsumerNavClaimLowersWithLegacyBytes() {
        // #500 arm A — a PLAIN hop over a meta hop claims at its root: the widened
        // isNavigableReceiver admits the IRMetaAccess receiver, the frame's shared
        // isOracleRootShape gate installs the generic OracleRootRenderer (a meta-bearing
        // FieldAccess root), and the emitter's entry consult serves the WHOLE claim via
        // super.visitFeatureCall(site, ctx) — the literal legacy line — so the
        // ReferenceWithMetaX deref chain is legacy's own bytes BY IDENTITY. The interior meta
        // hop re-enters through the oracle's own receiver compile and claims at ITS root via
        // the #499 MetaNavRenderer (the metaNavLowered receipt), whose own oracle re-enters the
        // param receiver — three driven events, all IR-LOWERED, zero delegated.
        String source = """
                namespace "test.step500a"

                type Party:
                    name string (1..1)

                type Details:
                    party Party (1..1)
                        [metadata reference]

                type Trade:
                    details Details (1..1)

                func MetaConsumerNav:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result string (1..1)
                    set result:
                        trade -> details -> party -> name
                """;
        // The 4-name chain keeps the META hop a genuine RFeatureCall (the #499 retro's 2-name
        // parse trap: `trade -> party` alone would parse as a disguised REnumValueRef, whose
        // meta-featured equivalent is the DEFERRED chainRecv class, not this recv class): the
        // receiver `trade -> details -> party` is a genuine fc whose own disguised
        // `trade -> details` prefix lowers through the #478 input-nav arm (non-meta), so the
        // party hop lowers to IRMetaAccess and the outer `-> name` is the census's
        // receiver:IRMetaAccess consumer.
        RModel model = AstBuilder.buildFromString(source, "step500-metaconsumer-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFeatureCall nav = AstWalker.findAll(ws.files().get(0), RFeatureCall.class).stream()
                .filter(fc -> "name".equals(fc.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no name nav"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), nav), renderScoped(c, nav),
                "the plain-hop-over-meta render must be legacy's own bytes (the oracle-root"
                        + " serve at the nav claim root)");
        assertEquals(3, c.irDrivenCount(),
                "the nav root, the re-entering meta hop and the re-entering disguised-input"
                        + " receiver all drive (3 = 1 root [the oracleRootLowered receipt] + 1"
                        + " meta hop [the metaNavLowered receipt] + 1 input-nav claim — each"
                        + " interior claims at its own root through the oracle's recursion)");
        assertEquals(1, c.metaNavLoweredCount(),
                "the interior meta hop claims through the #499 conversion at its re-entry root");
        assertTrue(c.oracleRootLoweredBreakdown().contains("navChain=1"),
                "the #500 receipt reads the consumer serve under its family token (recut"
                        + " metaNavChain -> navChain at the #501 token widening)");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "every claim is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the meta-consumer fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void logicalOverOnlyExistsClaimLowersWithLegacyBytes() {
        // #501 arm A: a logical root over a lowered only-exists operand (the #500-exposed
        // operand:IROnlyExists face — 246 sole, 100% RLogicalExpr) lowers through the #501
        // producesComparisonResult admission and renders WHOLE through the generic oracle-root
        // logical serve (super.visitLogical — the literal legacy line, its ComparisonResult
        // composition included) — byte-identical BY IDENTITY.
        String source = """
                namespace "test.step501bt1"

                type Details:
                    a string (0..1)

                type Trade:
                    details Details (1..1)
                    active boolean (1..1)

                func LogicalOverOnlyExists:
                    inputs:
                        trade Trade (1..1)
                    output:
                        result boolean (1..1)
                    set result:
                        trade -> details -> a only exists and trade -> active exists
                """;
        RModel model = AstBuilder.buildFromString(source, "step501-logical-onlyexists.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RLogicalExpr logical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("parsed model contains no logical"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), logical), renderScoped(c, logical),
                "the logical-over-only-exists render must be legacy's own bytes (the oracle-root"
                        + " logical serve)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("logical=1"),
                "the serve lands under the recut family-plain token");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — the delegated total must not move");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void symbolNavCollisionChainClaimLowersWithLegacyBytes() {
        // #501 arm C2 e2e: the #480 MF-1 head-name COLLISION chain (`legs extract inner -> x`
        // with a workspace function `inner` sharing the item attribute's name) — the chain
        // lowers to the DISTINCT shallow IRSymbolNav, the extract root claims through the D1
        // recursion and renders WHOLE through the LambdaOpRenderer oracle
        // (super.visitExtract — the literal legacy line, the FUNCTION-receiver ladder
        // included); the oracle's re-entrant interior visit of the chain routes through
        // visitEnumValueRef's attributeChain EXCLUSION straight to the same legacy ladder
        // (uncounted by the walk — the L-109c line; the seat conversion is a named future
        // lever) — byte-identical BY IDENTITY at every seat.
        String source = """
                namespace "test.step501bt2"

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
        RModel model = AstBuilder.buildFromString(source, "step501-symbolnav-twin.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findAll(ws.files().get(0), RExtractExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("parsed model contains no extract"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), extract), renderScoped(c, extract),
                "the collision-chain extract render must be legacy's own bytes (the LambdaOp"
                        + " oracle serve; the interior chain routes through the attributeChain"
                        + " exclusion to the same literal ladder)");
        assertTrue(c.irDrivenCount() >= 1,
                "the extract root claims — the D1 recursion admits the lowered IRSymbolNav body");
        // #515 recut: the ladder's own SYNTHESIZED function-receiver nav re-entry (an
        // RFeatureCall raw family — the pointfree meta-gate residue that used to surface as
        // this seat's one decline) is SERVED at the emitter frontier since #515 — the
        // decline's own super.visitFeatureCall fallback BY IDENTITY, counted LOWERED with
        // the flip receipt; the raw chain visit itself stays the excluded bucket's
        // straight-to-legacy route, uncounted by the walk.
        assertEquals(0, c.irDeclinedCount(),
                "the synthesized re-entry serves at the emitter frontier — no decline surfaces");
        assertEquals(1, c.emitterServeLoweredCount(),
                "the synthesized re-entry's serve carries the #515 flip receipt");
    }

    @Test
    void closureParamCallArgExtractClaimLowersWithLegacyBytes() {
        // #502 arm 2 e2e: the CompareTradeLot witness shape — a NAMED extract whose lambda
        // passes the param BARE as a call argument (`xs extract x [ Check(x) ]`). The param
        // reference lowers to the DISTINCT shallow IRClosureParam (either bind flavor), the
        // call admits it at isSimpleCallArg, the extract root claims through the D1 recursion
        // and renders WHOLE through the LambdaOpRenderer oracle (super.visitExtract — the
        // literal legacy line, whose own scope machinery renders the param name) —
        // byte-identical BY IDENTITY; the kind itself has NO leaf-emitter arm.
        String source = """
                namespace "test.step502bt1"

                type Thing:
                    flag boolean (1..1)

                func Check:
                    inputs:
                        one Thing (1..1)
                    output:
                        ok boolean (1..1)
                    set ok:
                        one -> flag

                func BoundParamCall:
                    inputs:
                        xs Thing (1..*)
                    output:
                        out boolean (0..*)
                    set out:
                        xs extract x [ Check(x) ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step502-closure-param-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RExtractExpr extract = AstWalker.findAll(ws.files().get(0), RExtractExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("parsed model contains no extract"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), extract), renderScoped(c, extract),
                "the named-param call-arg extract render must be legacy's own bytes (the"
                        + " LambdaOp oracle serve; the binder's scope machinery renders the"
                        + " param name inside the literal line)");
        assertTrue(c.irDrivenCount() >= 1,
                "the extract root claims — the D1 recursion admits the lowered closure-param"
                        + " call arg");
    }

    @Test
    void metaOutputCallClaimLowersAtRootWithLegacyBytes() {
        // #502 arm 4 e2e: a FUNCTION call whose callee output is [metadata]-annotated claims
        // at its root — the adapter lowers the DISTINCT shallow IRMetaOutputApply (childless;
        // the args are not carried), the frame installs the generic OracleRootRenderer (the
        // #502 kind screen), and the entry consult serves the claim via the callArgs dispatch
        // (super.visitSymbolReference — the literal legacy call line, the FieldWithMetaX
        // coercion threading included) — byte-identical BY IDENTITY.
        String source = """
                namespace "test.step502bt2"

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
                """;
        RModel model = AstBuilder.buildFromString(source, "step502-meta-output-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RSymbolReference call = AstWalker.findAll(ws.files().get(0), RSymbolReference.class)
                .stream().filter(r -> "MetaOut".equals(r.name()) && !r.args().isEmpty())
                .findFirst().orElseThrow(() -> new AssertionError("parsed model contains no call"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), call), renderScoped(c, call),
                "the meta-output call render must be legacy's own bytes (the oracle-root"
                        + " callArgs serve at the claim root)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("callArgs=1"),
                "the serve lands under the callArgs family token");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — the delegated total must not move");
    }

    @Test
    void modifiedEqualityClaimLowersAtRootWithLegacyBytes() {
        // #503 arm-A1 e2e: an ALL/ANY-modified equality claims at its root — the adapter
        // lowers the DISTINCT shallow IRAllAnyCompare, the frame installs the generic
        // OracleRootRenderer (the #503 kind screen), and the entry consult serves the claim
        // via the standing equality dispatch (super.visitEquality — the literal legacy line:
        // areEqual/notEqual with the EXPLICIT CardinalityOperator argument, legacy's own
        // operand composition included) — byte-identical BY IDENTITY.
        String source = """
                namespace "test.step503bt1"

                type Thing:
                    tags string (0..*)

                func AnyEq:
                    inputs:
                        t Thing (1..1)
                    output:
                        out boolean (1..1)
                    set out:
                        t -> tags any = "x"
                """;
        RModel model = AstBuilder.buildFromString(source, "step503-allany-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        REqualityExpr anyEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream().filter(eq -> eq.mod().isPresent()).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no modified equality"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), anyEq), renderScoped(c, anyEq),
                "the any-equality render must be legacy's own bytes (the oracle-root equality"
                        + " serve with the explicit CardinalityOperator)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("equality=1"),
                "the serve lands under the equality family token");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — the delegated total must not move");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void choiceOptionNavClaimLowersWithLegacyBytes() {
        // #503 arm-B1 e2e: a choice-OPTION navigation (`h -> pick -> OptionA -> a` — the
        // option selected by its type-name spelling) claims whole — the adapter resolves the
        // option against the choice's RDataType projection (legacy NavigationHandler's own
        // recovery mirrored) and the chain renders through the NATIVE nav composition —
        // byte-identical to legacy's recovery render.
        String source = """
                namespace "test.step503bt2"

                type OptionA:
                    a string (0..1)

                type OptionB:
                    b string (0..1)

                choice Pick:
                    OptionA
                    OptionB

                type Holder:
                    pick Pick (1..1)

                func ChoiceNav:
                    inputs:
                        h Holder (1..1)
                    output:
                        out string (0..1)
                    set out:
                        h -> pick -> OptionA -> a
                """;
        RModel model = AstBuilder.buildFromString(source, "step503-choice-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RFeatureCall leafHop = AstWalker.findAll(ws.files().get(0), RFeatureCall.class)
                .stream().filter(fc -> "a".equals(fc.featureName())).findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no leaf hop"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), leafHop), renderScoped(c, leafHop),
                "the choice-option chain render must be legacy's own bytes (the native nav"
                        + " composition over the projected option attribute)");
        assertTrue(c.irDrivenCount() >= 1, "the chain claims and drives");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void coercedBooleanLogicalClaimLowersAtRootWithLegacyBytes() {
        // #504 arm-A e2e: a logical with a bare boolean CALL operand claims at its root — the
        // adapter admits the IRApply operand at producesComparisonResult, the frame's
        // containsOracleLeaf SHAPE leg (a logical BinaryOp carrying a coercion-needing
        // operand) installs the generic OracleRootRenderer, and the entry consult serves the
        // claim via the standing logical dispatch (super.visitLogical — the literal legacy
        // line: LogicalHandler's andNullSafe chain with the ComparisonResult.ofNullSafe
        // coercion composed inside) — byte-identical BY IDENTITY.
        String source = """
                namespace "test.step504bt1"

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
                """;
        RModel model = AstBuilder.buildFromString(source, "step504-boolop-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RLogicalExpr logical = AstWalker.findAll(ws.files().get(0), RLogicalExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no logical"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), logical), renderScoped(c, logical),
                "the coerced-logical render must be legacy's own bytes (the oracle-root"
                        + " logical serve — the ofNullSafe coercion composed inside legacy's"
                        + " own andNullSafe chain)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("logical=1"),
                "the serve lands under the logical family token");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — the delegated total must not move");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void mintedSynItemNavEqualityClaimServesAtTheOracleRootWithLegacyBytes() {
        // #504 arm-B e2e: an in-lambda equality whose LEFT operand is a bare item attr over
        // a META-annotated extract source (the aw0tw1 un-retypeable class) claims at its
        // root — the bare attr mints the DISTINCT shallow IRSynItemNav (the arm-B2 leg), the
        // equality admits it (the consumer admission), the kind is an oracle LEAF, and the
        // frame serves the claim via the standing equality dispatch (super.visitEquality —
        // the literal legacy line: the binder-scope item resolution and every coercion run
        // inside legacy's own render) — byte-identical BY IDENTITY.
        String source = """
                namespace "test.step504bt2"

                type Item:
                    inner string (1..1)

                type Wrap:
                    itemRefs Item (0..*)
                        [metadata reference]

                func UseSynEq:
                    inputs:
                        w Wrap (1..1)
                    output:
                        out boolean (0..*)
                    set out:
                        w -> itemRefs extract [ inner = "x" ]
                """;
        RModel model = AstBuilder.buildFromString(source, "step504-synitem-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        REqualityExpr synEq = AstWalker.findAll(ws.files().get(0), REqualityExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no equality"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), synEq), renderScoped(c, synEq),
                "the syn-item equality render must be legacy's own bytes (the oracle-root"
                        + " equality serve — the item binding resolved by legacy's own scope"
                        + " machinery)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("equality=1"),
                "the serve lands under the equality family token");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the claim is IR-LOWERED — the delegated total must not move");
        // NO declined=0 assert here (unlike the native twins): legacy's oracle recursion
        // re-enters the bare attr through ReferenceHandler's THROWAWAY item-nav synthesis,
        // whose fresh unranged equivalent declines at its own re-entrant seat while the
        // claim root serves — the standing re-entrant accounting, bytes unaffected.
        assertTrue(c.irDrivenCount() >= 1, "the equality root claims and drives");
    }

    @Test
    void conversionClaimLowersAtRootWithLegacyBytes() {
        // #500 arm B (the deep conversion teach) — a `to-enum` claims at its root: the adapter
        // lowers IRConversion{child} (all-or-nothing argument), the frame installs the generic
        // OracleRootRenderer (an arm-B family kind), and the entry consult serves the claim via
        // super.visitConversion(site, ctx) — the literal legacy line (conversionHandler.handle)
        // — byte-identical BY IDENTITY. The argument re-enters through the oracle's own compile
        // and claims natively at its root.
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
                """;
        RModel model = AstBuilder.buildFromString(source, "step500-conversion-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RConversionExpr conv = AstWalker.findAll(ws.files().get(0), RConversionExpr.class)
                .stream().findFirst()
                .orElseThrow(() -> new AssertionError("parsed model contains no conversion"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), conv), renderScoped(c, conv),
                "the to-enum render must be legacy's own bytes (the oracle-root serve at the"
                        + " conversion claim root)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("conversion=1"),
                "the #500 receipt reads the family serve");
        assertEquals(0, c.irDrivenDelegatedCount(),
                "the conversion claim is IR-LOWERED — the delegated total must not move");
        assertEquals(delegatedSubCounterSum(c), c.irDrivenDelegatedCount(),
                "the Σ conservation holds on the conversion fixture (all seats zero)");
        assertEquals(0, c.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    @Test
    void pipeAndOnlyExistsClaimsLowerAtRootWithLegacyBytes() {
        // #500 arm B (the two shallow teaches) — a `then` chain root and an `only exists` root
        // claim at their visit seats: the adapter lowers the shallow IRPipe / IROnlyExists, the
        // frame installs the generic OracleRootRenderer, and the entry consult serves each via
        // super.visitThen / super.visitOnlyExists — the literal legacy lines
        // (collectionHandler.handle / existenceHandler.handle) — byte-identical BY IDENTITY.
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
        RModel model = AstBuilder.buildFromString(source, "step500-shallow-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
        RThenExpr then = AstWalker.findAll(ws.files().get(0), RThenExpr.class).stream()
                .findFirst().orElseThrow(() -> new AssertionError("no then parsed"));
        ROnlyExistsExpr onlyExists = AstWalker.findAll(ws.files().get(0), ROnlyExistsExpr.class)
                .stream().findFirst().orElseThrow(() -> new AssertionError("no only-exists parsed"));

        IRExpressionCompiler c = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), then), renderScoped(c, then),
                "the then-chain render must be legacy's own bytes (the oracle-root serve at the"
                        + " pipe claim root)");
        assertTrue(c.oracleRootLoweredBreakdown().contains("pipe=1"),
                "the #500 receipt reads the pipe serve");

        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRenderScoped(legacyCompiler(ws), onlyExists), renderScoped(c2, onlyExists),
                "the only-exists render must be legacy's own bytes (the oracle-root serve at"
                        + " the check's claim root)");
        assertTrue(c2.oracleRootLoweredBreakdown().contains("onlyExists=1"),
                "the #500 receipt reads the only-exists serve");
        assertEquals(0, c2.irDrivenDelegatedCount(),
                "the shallow claims are IR-LOWERED — the delegated total must not move");
        assertEquals(0, c2.irDeclinedCount(), "nothing declines on the taught fixture");
    }

    /**
     * The eight delegated-seat sub-counters summed (the #493 split's Σ channel — mirrors the D11
     * ring's hard assert; a ninth delegated seat added without joining BOTH channels breaks this).
     */
    private static int delegatedSubCounterSum(IRExpressionCompiler c) {
        return c.inputFeatureNavDrivenCount() + c.pointFreeFnDrivenCount()
                + c.implicitInputNavDrivenCount() + c.metaNavDrivenCount()
                + c.inputFeatureNavReceiverDrivenCount() + c.implicitAttrNavDrivenCount()
                + c.claimRootDelegatedCount() + c.postPinDelegatedCount();
    }

    @Test
    void syntheticFilterExtractItemNavIsIrDrivenViaTheOracleRenderer() {
        // #479 — the arm end-to-end: a synthetic-item nav inside a REAL parsed filter/extract
        // lambda (the legacy-synthesized re-entrant shape) ADAPTS (the boundary retype: the
        // receiver takes the binder source's element type, the hop takes the feature's
        // ATTRIBUTE-channel type — both invisible to the node-keyed cache) and EMITS with the
        // receiver served by the SYNTHETIC-item oracle renderer — legacy
        // handle(RImplicitVariable) verbatim on the range-correlated node, so the bytes are the
        // legacy fallback's own by construction (asserted, plus the DRIVEN meter as the teach's
        // witness). The same nav OUTSIDE a filter/extract binder stays declined byte-identically.
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
        RModel model = AstBuilder.buildFromString(source, "step479-synitem-compiler.rosetta");
        RWorkspace ws = RWorkspace.build(java.util.List.of(model)).workspace();
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

        IRExpressionCompiler c = compiler(ws);
        String ir = render(c, nav);
        String legacy = legacyRender(legacyCompiler(ws), nav);
        assertEquals(legacy, ir,
                "the oracle-rendered receiver + the emitter's hop compose must be the legacy bytes");
        assertEquals(1, c.irDrivenCount(),
                "the synthetic-item nav must be IR-DRIVEN — the #479 teach's meter witness");

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
        IRExpressionCompiler c2 = compiler(ws);
        assertEquals(legacyRender(legacyCompiler(ws), unboundNav), render(c2, unboundNav),
                "the residue outside the filter/extract slice serves legacy's own bytes");
        // RECUT at #504 (the pin-recut law): the outside-the-slice residue now MINTS the
        // shallow IRSynItemNav (the arm-B teach) and IS IR-driven — the render routes through
        // the oracle-root navChain serve (the byte assert above proves the identity; the
        // pre-#504 lock's "claims exactly the witnessed slice" boundary moved with the arm).
        // RECUT again at #515: the oracle re-walk's interior visit of the UNBOUND synthetic
        // item (the RImplicitVariable:root:VARIABLE class — the #514 gap census's 959-event
        // leader) used to decline quietly at the leaf emitter; the emitter frontier now
        // serves it (super.visitImplicitVariable BY IDENTITY), so the claim counts driven
        // — 2 with the root serve — and the flip receipt reads the interior serve.
        assertEquals(2, c2.irDrivenCount(),
                "the minted residue root serve + the interior synthetic-item serve since #515");
        assertEquals(1, c2.emitterServeLoweredCount(),
                "the interior unbound-item serve carries the #515 flip receipt");
    }

    @Test
    void blockerProbeOffByDefaultAndReadOnlyWhenOn() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // OFF by default (the standing rings' state): zero probe movement, stable `none` forms.
        IRExpressionCompiler off = compiler(ws);
        String offBytes = render(off, adapterGapEqualityFixture(ws));
        assertEquals(0, off.blockerProbedClaimCount(), "the probe must be OFF unless the property/test seam enables it");
        assertEquals("none", off.blockerClaimsBreakdown());
        assertEquals("none", off.soleBlockerClaimsBreakdown());
        assertEquals(0, off.blockerProbeAnomalyCount());
        assertEquals("none", off.blockerReasonBreakdown());
        assertEquals("none", off.soleReasonClaimsBreakdown());
        assertEquals("none", off.soleFamilyMultiReasonBreakdown());
        assertEquals(0, off.blockerReasonUnattributedCount());

        // READ-ONLY when on: the probed twin renders BYTE-identically with the standing counters
        // unmoved vs the un-probed twin — the probe is a reading, not a route (it runs after the
        // decline, calls only the stateless adapter, and no render decision reads its maps).
        IRExpressionCompiler on = compiler(ws);
        on.setBlockerProbeForTest(true);
        String onBytes = render(on, adapterGapEqualityFixture(ws));
        assertEquals(offBytes, onBytes, "probe-on and probe-off must render identical bytes");
        assertEquals(1, on.blockerProbedClaimCount());
        assertEquals(off.irDeclinedCount(), on.irDeclinedCount(), "the probe must not move the decline meter");
        assertEquals(off.irDrivenCount(), on.irDrivenCount(), "the probe must not move the driven meter");
        assertEquals(off.untargetedVisitCount(), on.untargetedVisitCount(), "the probe must not move the untargeted meter");
        assertEquals(off.irDeclineSiteBreakdown(), on.irDeclineSiteBreakdown(), "the probe must not move the site census");
    }

    /**
     * The #476 blocker-probe fixture: {@code p = (c required choice x, y)}. The equality ROOT
     * declines at the {@code adapterGap} seat (the nested cardinality-check has no adapt arm — the
     * family is absent from the adapter — and no relabel gate matches an equality), making the
     * CHECK node, not the root, the claim's minimal blocker. Fresh nodes per call so each compiler
     * renders its own tree.
     */
    private static REqualityExpr adapterGapEqualityFixture(RWorkspace ws) {
        // The nested blocker recut at #513 (contains → join) and again at #515 (the untargeted
        // close armed join, so the fixture pins the mechanism on the still-unarmed
        // cardinality-check — the hand-buildable one of the last two noAdaptArm families; a
        // hand-built reduce cannot render in this null-scope harness, the #513 note).
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(scalarParamRef("p", ws));
        RCardinalityCheckExpr check = new RCardinalityCheckExpr();
        check.setArgument(scalarParamRef("c", ws));
        check.setOp(CardCheckOp.CHOICE);
        check.setNecessity(Necessity.REQUIRED);
        check.attributes().add("x");
        check.attributes().add("y");
        check.attachToWorkspace(ws);
        eq.setRight(check);
        eq.attachToWorkspace(ws);
        return eq;
    }

    /** A plain legacy {@link ExpressionCompiler} over the same facts — the twin for inertness proofs. */
    private static ExpressionCompiler legacyCompiler(RWorkspace ws) {
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        GeneratorModel gm = new GeneratorModel(ws, model -> true);
        return new ExpressionCompiler(gm, new JavaTypeTranslator(typeUtil), typeUtil);
    }

    private static String legacyRender(ExpressionCompiler c, RExpression expr) {
        JavaStatementBuilder out = c.compile(expr, null, null);
        // stripToBare: a witness may carry the first-claim-wins import sentinel (legacy's fqnWitness
        // facet + the IR emitter's #478 mirror); production resolves it at buildStandardModel — the
        // unit harness neutralises it to the uncontested bare form on BOTH routes.
        return ImportCollisionResolver.stripToBare(
                assertInstanceOf(JavaExpression.class, out).renderToString());
    }

    /**
     * An alias/shortcut reference whose enclosing function does NOT resolve ({@code ref} has no
     * parent), so {@code tryEmitAlias} declines at the {@code aliasResolution} seat and legacy's
     * empty-arg-list path renders it — the census's decline-site fixture.
     */
    private static RSymbolReference parentlessAliasRef(String aliasName, RWorkspace ws) {
        RFunction func = new RFunction();
        func.setName("OrphanHost");
        RShortcut shortcut = new RShortcut();
        shortcut.setName(aliasName);
        func.shortcuts().add(shortcut);
        shortcut.setParent(func);
        RSymbolReference ref = new RSymbolReference();
        ref.setName(aliasName);
        ref.setResolvedSymbol(shortcut);
        func.attachToWorkspace(ws);
        shortcut.attachToWorkspace(ws);
        ref.attachToWorkspace(ws);
        return ref;
    }

    @Test
    void parameterComparisonIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // First structural flip: a param/param ordered comparison composes the fixed
        // ExpressionOperatorsNullSafe idiom from its two IR-driven operand leaves — the IR
        // tree (not a leaf value) drives emission, byte-identical to ComparisonHandler.
        RComparisonExpr cmp = new RComparisonExpr();
        cmp.setOp(CompOp.GT);
        cmp.setLeft(scalarParamRef("n1", ws));
        cmp.setRight(scalarParamRef("n2", ws));
        cmp.attachToWorkspace(ws);

        assertEquals("greaterThan(MapperS.of(n1), MapperS.of(n2), CardinalityOperator.All)", render(c, cmp));
        assertEquals(1, c.irDrivenCount(), "a param/param comparison must be emitted from the IR");
    }

    @Test
    void parameterArithmeticRendersMapperMathsFromTheIR() {
        // `a + b` over two `number` params renders the dominant golden combo
        // MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(a), MapperS.of(b)) — byte-identical
        // to ArithmeticHandler's typed-JOIN path. Built directly with `number` operand types (the translator
        // maps RNumberType.unconstrained() → BigDecimal). ctx=null (arithmetic needs no expected-type context
        // for same-kind param operands — each renders MapperS.of(name)).
        RMetaAnnotatedType numberType = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        IRVariable a = new IRVariable("a", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable b = new IRVariable("b", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp add = new BinaryOp(BinaryOp.BinOp.ADD, a, b, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(add, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add(MapperS.of(a), MapperS.of(b))",
                out.renderToString());

        // int/int DIVIDE → the golden mixed combo <BigDecimal, Integer, Integer>divide: divide's result is
        // ALWAYS BigDecimal, while the operands stay Integer (RNumberType.intType() → Integer).
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());
        IRVariable x = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable y = new IRVariable("y", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp div = new BinaryOp(BinaryOp.BinOp.DIV, x, y, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        JavaExpression divOut = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(div, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(x), MapperS.of(y))",
                divOut.renderToString());

        // A MIXED int/number pair declines (legacy joins to BigDecimal and may coerce the int operand) → the
        // whole arithmetic falls back to legacy; the same-kind gate is what makes the param render byte-safe.
        BinaryOp mixed = new BinaryOp(BinaryOp.BinOp.ADD, a, x, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertTrue(new IRJavaLeafEmitter().emit(mixed, null, typeUtil, translator).isEmpty(),
                "a mixed int/number arithmetic pair declines to legacy");
    }

    @Test
    void literalOperandArithmeticRendersMapperMathsFromTheIR() {
        // `daysInPeriod / 365`-style: a numeric LITERAL operand drives arithmetic emission, re-rendered against
        // the int/number JOIN's expected type — byte-identical to ArithmeticHandler compiling each operand
        // against operandExpectedType. ctx=null exercises the no-outer-context path (the emitter builds the
        // join-typed operand context itself).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType numberType = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        // number-param / int-literal: join BigDecimal → the int literal renders MapperS.of(BigDecimal.valueOf(365)),
        // the number param renders bare (own type == join, no coercion). divide → result BigDecimal.
        IRVariable days = new IRVariable("days", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit365 = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp numDivLit = new BinaryOp(BinaryOp.BinOp.DIV, days, lit365, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("MapperMaths.<BigDecimal, BigDecimal, BigDecimal>divide("
                        + "MapperS.of(days), MapperS.of(BigDecimal.valueOf(365)))",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(numDivLit, null, typeUtil, translator).orElseThrow()).renderToString());

        // int-param / int-literal: join Integer → the int literal renders BARE MapperS.of(365); divide → BigDecimal.
        IRVariable x = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit365b = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp intDivLit = new BinaryOp(BinaryOp.BinOp.DIV, x, lit365b, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(x), MapperS.of(365))",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(intDivLit, null, typeUtil, translator).orElseThrow()).renderToString());

        // Two int literals: join Integer → both bare (`1 + 2` → <Integer,Integer,Integer>).
        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(1), NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral two = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(2), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp litPlusLit = new BinaryOp(BinaryOp.BinOp.ADD, one, two, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("MapperMaths.<Integer, Integer, Integer>add(MapperS.of(1), MapperS.of(2))",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(litPlusLit, null, typeUtil, translator).orElseThrow()).renderToString());

        // A RESOLVED int param raised to a number join by a number LITERAL declines: legacy would coerce the
        // int param to BigDecimal (compile(intParam, BigDecimal, …)), which the IR does not reproduce.
        IRLiteral litNum = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new BigDecimal("1.5"), NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp resolvedRaised = new BinaryOp(BinaryOp.BinOp.ADD, x, litNum, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertTrue(new IRJavaLeafEmitter().emit(resolvedRaised, null, typeUtil, translator).isEmpty(),
                "an int param raised to a number join by a number literal declines (would be coerced)");
    }

    @Test
    void aliasOperandArithmeticRendersMapperMathsFromTheIR() {
        // `daysInPeriod / 365`: an ALIAS operand drives arithmetic via the compiler-supplied resolver — render =
        // aliasName(inputs), witness = the alias's numeric kind (here Integer, e.g. DateDifference -> int).
        // divide -> result BigDecimal. The alias IR node's type() is MISSING upstream and is never read for an
        // alias operand (the resolver supplies both the render facts and the witness).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());
        RMetaAnnotatedType numberType = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());

        IRReference alias = new IRReference("daysInPeriod", IRReference.ReferenceKind.ALIAS, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit365 = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp aliasDivLit = new BinaryOp(BinaryOp.BinOp.DIV, alias, lit365, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("startDate", "endDate"), "Integer"));
        assertEquals("MapperMaths.<BigDecimal, Integer, Integer>divide("
                        + "daysInPeriod(startDate, endDate), MapperS.of(365))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(aliasDivLit, null, typeUtil, translator).orElseThrow()).renderToString());

        // Without a resolver the alias operand is unclassifiable → the whole arithmetic declines to legacy.
        assertTrue(new IRJavaLeafEmitter().emit(aliasDivLit, null, typeUtil, translator).isEmpty(),
                "an alias operand with no resolver declines");

        // An int alias raised to a number join by a number param declines: a resolved operand whose kind
        // differs from the join would be coerced by legacy (the same gate that protects the param case).
        IRVariable rate = new IRVariable("rate", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp aliasPlusNumber = new BinaryOp(BinaryOp.BinOp.ADD, alias, rate, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRJavaLeafEmitter emitter2 = new IRJavaLeafEmitter();
        emitter2.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("startDate", "endDate"), "Integer"));
        assertTrue(emitter2.emit(aliasPlusNumber, null, typeUtil, translator).isEmpty(),
                "an int alias raised to a number join declines (would be coerced)");
    }

    @Test
    void functionCallRendersEvaluateFromTheIR() {
        // `myCallee(x)` → MapperS.of(myCallee.evaluate(x)) — byte-identical to ReferenceHandler's function-call
        // branch. The @Inject receiver name comes from the compiler-side CallReceiverResolver (a Java-emission
        // decision off the neutral IRApply); each scalar arg is UNWRAPPED from its MapperS.of(...) to the bare
        // evaluate slot; the single (scalar) result is wrapped once in MapperS.of(...). The IRApply/callee type()
        // is unread by emitApply, so a placeholder type suffices.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable x = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(x), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        assertEquals("MapperS.of(myCallee.evaluate(x))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // A LITERAL arg unwraps to its bare value: `myCallee(42)` → MapperS.of(myCallee.evaluate(42)).
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(42), NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply callLit = new IRApply(callee, List.of(lit), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRJavaLeafEmitter emitterLit = new IRJavaLeafEmitter();
        emitterLit.setCallReceiverResolver(c -> "myCallee");
        assertEquals("MapperS.of(myCallee.evaluate(42))",
                assertInstanceOf(JavaExpression.class,
                        emitterLit.emit(callLit, null, typeUtil, translator).orElseThrow()).renderToString());

        // Two scalar args compose comma-joined: `myCallee(x, y)` → MapperS.of(myCallee.evaluate(x, y)).
        IRVariable y = new IRVariable("y", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(2),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call2 = new IRApply(callee, List.of(x, y), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRJavaLeafEmitter emitter2args = new IRJavaLeafEmitter();
        emitter2args.setCallReceiverResolver(c -> "myCallee");
        assertEquals("MapperS.of(myCallee.evaluate(x, y))",
                assertInstanceOf(JavaExpression.class,
                        emitter2args.emit(call2, null, typeUtil, translator).orElseThrow()).renderToString());

        // The resolver's shortcut-collision "0" suffix flows through verbatim (legacy disambiguateDependencyReceiver):
        // receiver "dayOfWeek0" → MapperS.of(dayOfWeek0.evaluate(x)) (the IsWeekend corpus carrier).
        IRJavaLeafEmitter emitterColl = new IRJavaLeafEmitter();
        emitterColl.setCallReceiverResolver(c -> "dayOfWeek0");
        assertEquals("MapperS.of(dayOfWeek0.evaluate(x))",
                assertInstanceOf(JavaExpression.class,
                        emitterColl.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // No resolver — or a null receiver — declines to legacy (byte-safe fallback).
        assertTrue(new IRJavaLeafEmitter().emit(call, null, typeUtil, translator).isEmpty(),
                "a call with no receiver resolver declines to legacy");
        IRJavaLeafEmitter emitterNull = new IRJavaLeafEmitter();
        emitterNull.setCallReceiverResolver(c -> null);
        assertTrue(emitterNull.emit(call, null, typeUtil, translator).isEmpty(),
                "a call whose receiver resolves to null declines to legacy");
    }

    @Test
    void functionCallWithAliasArgRendersGetFromTheIR() {
        // Slice 2 (alias args — the biggest call-arg lever): an alias/shortcut argument is the bare Mapper
        // aliasName(inputs) (L-031 emitAlias) and unwraps with the oracle's .get() fall-through —
        //   MapperS.of(myCallee.evaluate(myAlias(in1).get()))
        // Byte-identical to legacy: the alias arm renders aliasName(inputs); ReferenceHandler.unwrapForEvaluateArg
        // appends .get() (a bare Mapper, not a MapperS.of wrap, not a dotted enum). The alias resolver (set by the
        // compiler) supplies the enclosing inputs + the dependency-collision flag; here a stub.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRReference aliasArg = new IRReference("myAlias", IRReference.ReferenceKind.ALIAS, NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(aliasArg), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        emitter.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("in1"), null));
        assertEquals("MapperS.of(myCallee.evaluate(myAlias(in1).get()))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // A dependency-name collision suffixes the alias invocation with "1" (legacy disambiguateAliasInvocation).
        IRJavaLeafEmitter emitterColl = new IRJavaLeafEmitter();
        emitterColl.setCallReceiverResolver(c -> "myCallee");
        emitterColl.setAliasResolver(a -> new AliasOperandResolver.Facts(true, List.of("in1"), null));
        assertEquals("MapperS.of(myCallee.evaluate(myAlias1(in1).get()))",
                assertInstanceOf(JavaExpression.class,
                        emitterColl.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // No alias resolver — the alias arg can't render → the whole call declines (byte-safe fallback to legacy).
        IRJavaLeafEmitter emitterNoAlias = new IRJavaLeafEmitter();
        emitterNoAlias.setCallReceiverResolver(c -> "myCallee");
        assertTrue(emitterNoAlias.emit(call, null, typeUtil, translator).isEmpty(),
                "an alias arg with no resolver declines the whole call");
    }

    @Test
    void functionCallWithNavArgRendersChainGetFromTheIR() {
        // #489 (the L-042 REVIVAL): a NAVIGATION argument is the bare Mapper chain the standing nav render
        // produces and unwraps with the oracle's chained-Mapper fall-through — the SAME path the alias arg
        // takes (golden witness: resolveAdjustableDates.evaluate(MapperS.of(valuationDates).<...>map(...)
        // .get()), cdm6 AdjustedValuationDates):
        //   MapperS.of(myCallee.evaluate(MapperS.of(foo).<String>map("getBar", _trade -> _trade.getBar()).get()))
        // Byte-identical to legacy by construction: the chain render is emitFieldAccess's standing render
        // (proven standalone), and ReferenceHandler.unwrapForEvaluateArg appends the accessor (a chained
        // Mapper is not a whole MapperS.of wrap, not a dotted enum). ctx=null gives the no-scope lambda
        // var ("_trade" from the receiver type name).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        REnumeration recvType = fixtureEnum("Trade");
        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(recvType));
        IRVariable foo = new IRVariable("foo", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1).child(0),
                receiverType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        RMetaAnnotatedType strType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        FieldAccess nav = new FieldAccess(foo, "bar", NodeId.ROOT.child(1),
                strType, ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.PRESENT,
                SourceRange.NONE);
        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(nav), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        assertEquals(
                "MapperS.of(myCallee.evaluate(MapperS.of(foo).<String>map(\"getBar\", _trade -> _trade.getBar()).get()))",
                ImportCollisionResolver.stripToBare(assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString()));

        // Into a MULTI callee param the SAME chain takes .getMulti() — the tailMulti accessor law (the
        // per-index asMulti follows the CALLEE parameter via CallParamMultiResolver, never the arg).
        IRJavaLeafEmitter emitterMulti = new IRJavaLeafEmitter();
        emitterMulti.setCallReceiverResolver(c -> "myCallee");
        emitterMulti.setCallParamMultiResolver((c, i) -> true);
        assertEquals(
                "MapperS.of(myCallee.evaluate(MapperS.of(foo).<String>map(\"getBar\", _trade -> _trade.getBar()).getMulti()))",
                ImportCollisionResolver.stripToBare(assertInstanceOf(JavaExpression.class,
                        emitterMulti.emit(call, null, typeUtil, translator).orElseThrow()).renderToString()));
    }

    @Test
    void functionCallArgEmissionIsExpectationNeutral() {
        // #489: the per-arg context is NEUTRALIZED — ExpressionContext.of(null, scope), legacy's own per-arg
        // construction (compile(arg, null, scope) — the #360 evaluateArgExpectedTypeReset facet: a call
        // boundary RESETS the argument expectation). The decisive witness is the expected-type-SENSITIVE
        // int-literal render: under a BigDecimal-expected OUTER context the pre-#489 ctx pass-through
        // coerced the arg to BigDecimal.valueOf(42) where legacy's neutral arg compile renders the bare 42
        // (the latent divergence seat this closed — population-inert, ring-proven, but latent).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(42), NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(lit), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        String render = assertInstanceOf(JavaExpression.class,
                emitter.emit(call, ExpressionContext.of(HandlerHelper.BIG_DECIMAL, null), typeUtil, translator)
                        .orElseThrow()).renderToString();
        assertEquals("MapperS.of(myCallee.evaluate(42))", render,
                "an int-literal arg renders BARE under a BigDecimal-expected outer context — the arg "
                        + "emission is expectation-neutral (legacy's #360 reset)");
    }

    @Test
    void functionCallWithEnumValueArgRendersConstantFromTheIR() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();

        // Slice (a) — a qualified enum-value argument `myCallee(FooEnum -> Cash)` renders the unwrapped constant:
        //   MapperS.of(myCallee.evaluate(FooEnum.CASH))
        // Byte-identical to legacy: the enum-value arm renders EnumName.CONSTANT (L-016 emitEnumValue — the enum
        // simple name from the node's enum type() REnumTypeRef, the constant formatEnumName∘stripEscape on the
        // carried value name), and ReferenceHandler.unwrapForEvaluateArg passes a dotted-enum constant through
        // unchanged (no MapperS.of wrap, no .get() — distinct from the alias arg's .get() fall-through).
        REnumeration en = fixtureEnum("FooEnum");
        en.attachToWorkspace(ws);
        RMetaAnnotatedType enumType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(en));
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRReference enumArg = new IRReference("Cash", IRReference.ReferenceKind.ENUM_VALUE, NodeId.ROOT.child(1),
                enumType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(enumArg), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        assertEquals("MapperS.of(myCallee.evaluate(FooEnum.CASH))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());
    }

    @Test
    void bareRuleDelegationRendersEvaluateInputFromTheIR() {
        // The bare rule-delegation shape `output = AssetClass` (a rule-body top-level reference to an RRule) →
        //   MapperS.of(assetClassRule.evaluate(input))
        // byte-identical to ReferenceHandler.renderImplicitRuleInvocation's top-level (non-lambda) form. The receiver
        // is the <Name>Rule @Inject field (here a stub resolver; the compiler supplies it via legacy
        // ruleInvocationReceiver); the implicit-input argument is the enclosing rule's `input` parameter, rendered as
        // the bare identifier. The IRApply carries NO explicit args (the input is the emitter's `input` binding).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference ruleCallee = new IRReference("AssetClass", IRReference.ReferenceKind.RULE, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply delegation = new IRApply(ruleCallee, List.of(), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setRuleReceiverResolver(c -> "assetClassRule");
        assertEquals("MapperS.of(assetClassRule.evaluate(input))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(delegation, null, typeUtil, translator).orElseThrow()).renderToString());

        // No rule-receiver resolver (a non-rule-family emission) → the delegation declines to legacy (byte-safe).
        assertTrue(new IRJavaLeafEmitter().emit(delegation, null, typeUtil, translator).isEmpty(),
                "a rule delegation with no rule-receiver resolver declines to legacy");
    }

    @Test
    void inLambdaRuleDelegationDelegatesToTheRenderer() {
        // The in-lambda shape `… then SomeRule` (a bare rule reference inside an extract/filter/then lambda) →
        //   MapperS.of(assetClassRule.evaluate(item.get()))
        // The implicit-input binding (item.get()/thenArg.get()) is polymorphic + AST-derived, so the compiler supplies
        // the COMPLETE render via a RuleDelegationRenderer that reuses legacy renderImplicitRuleInvocation VERBATIM
        // (L-049). The emitter just delegates to it — and in PREFERENCE to the top-level `input` path (so the same
        // IRApply{RULE} renders the lambda binding inside a lambda, the `input` binding at top level). The binding's
        // render CONTENT is byte-verified on the corpus by the RULE seam (Path2ByteIdentityTest); here we prove the
        // emitter prefers the renderer and returns its output, and declines when the renderer declines.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference ruleCallee = new IRReference("AssetClass", IRReference.ReferenceKind.RULE, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply delegation = new IRApply(ruleCallee, List.of(), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaExpression inLambdaRender = JavaExpression.wrappedInMapperSOf(
                JavaExpression.from("assetClassRule.evaluate(item.get())", null));

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        // Both an in-lambda renderer AND a (would-be top-level) receiver resolver are set — the renderer wins.
        emitter.setRuleReceiverResolver(c -> "assetClassRule");
        emitter.setRuleDelegationRenderer(() -> Optional.of(inLambdaRender));
        assertEquals("MapperS.of(assetClassRule.evaluate(item.get()))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(delegation, null, typeUtil, translator).orElseThrow()).renderToString());

        // A renderer that declines (empty) → the delegation declines to legacy even with a receiver resolver present
        // (once a renderer is installed, the in-lambda render is authoritative — there is no fall-through to `input`).
        IRJavaLeafEmitter declining = new IRJavaLeafEmitter();
        declining.setRuleReceiverResolver(c -> "assetClassRule");
        declining.setRuleDelegationRenderer(Optional::empty);
        assertTrue(declining.emit(delegation, null, typeUtil, translator).isEmpty(),
                "an in-lambda rule delegation whose renderer declines falls back to legacy");
    }

    @Test
    void flatListOpDelegatesToTheCollectionOpRenderer() {
        // A flat postfix list op (`<arg> distinct` → distinct(<arg>); also flatten/first/last/reverse and `count` →
        // <arg>.resultCount()) lowers to IRListOp ONLY when the receiver subtree itself lowers (L-050). The flat wrap
        // (the op→method mapping, the chain link, the distinct static-wildcard import) is supplied COMPLETE by the
        // compiler's CollectionOpRenderer reusing legacy CollectionHandler.handle VERBATIM; the emitter just delegates
        // to it. The render CONTENT is byte-verified on the corpus by Path2ByteIdentityTest; here we prove the emitter
        // delegates to the renderer's output, and declines when no renderer is installed (a non-list-op emission).
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRVariable child = new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        IRListOp distinct = new IRListOp(IRListOp.Kind.DISTINCT, child, NodeId.ROOT,
                anyType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);

        JavaExpression render = JavaExpression.from("distinct(MapperC.<String>of(xs))", null);
        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCollectionOpRenderer(() -> Optional.of(render));
        assertEquals("distinct(MapperC.<String>of(xs))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(distinct, null, typeUtil, translator).orElseThrow()).renderToString());

        // No collection-op renderer (not a list-op emission) → the op declines to legacy (byte-safe).
        assertTrue(new IRJavaLeafEmitter().emit(distinct, null, typeUtil, translator).isEmpty(),
                "a flat list op with no collection-op renderer declines to legacy");
    }

    @Test
    void multiOutputCallRendersMapperCFromTheIR() {
        // A MULTI-output call `myCallee(x)` → MapperC.<String>of(myCallee.evaluate(x)) — byte-identical to legacy
        // tryMultiValueWrap. The result wraps MapperC instead of MapperS; the <Item> witness is the call-result item
        // type (apply.type(), here a string — reusing the L-021 multi-param witness derivation). The arg unwrap is
        // UNCHANGED from the single-output slice (output cardinality only changes the RESULT wrap). With outputType
        // null (no enclosing-function collision) the witness is the bare simple name.
        RMetaAnnotatedType itemType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                itemType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable x = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                itemType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        // The IRApply carries MULTI cardinality (the multi output) + the call-result item type (the witness source).
        IRApply call = new IRApply(callee, List.of(x), NodeId.ROOT,
                itemType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        assertEquals("MapperC.<String>of(myCallee.evaluate(x))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // A multi-output call whose result type is MISSING → the witness can't be named → declines to legacy.
        IRApply missingType = new IRApply(callee, List.of(x), NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        IRJavaLeafEmitter emitterMissing = new IRJavaLeafEmitter();
        emitterMissing.setCallReceiverResolver(c -> "myCallee");
        assertTrue(emitterMissing.emit(missingType, null, typeUtil, translator).isEmpty(),
                "a multi-output call with a MISSING result type declines (cannot name the witness)");
    }

    @Test
    void functionCallWithAliasArgIntoMultiParamRendersGetMultiFromTheIR() {
        // The multi-PARAM lever: an alias argument into a MULTI callee parameter unwraps with .getMulti() (the
        // single→List coercion MapperS.getMulti() provides) instead of .get() (legacy facet tailMulti) —
        //   MapperS.of(myCallee.evaluate(myAlias(in1).getMulti()))
        // The accessor follows the CALLEE PARAMETER's cardinality (supplied per index by the CallParamMultiResolver),
        // NOT the arg's. With the resolver saying param 0 is MULTI the alias Mapper takes the .getMulti() fall-through.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType anyType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRReference aliasArg = new IRReference("myAlias", IRReference.ReferenceKind.ALIAS, NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(aliasArg), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        emitter.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("in1"), null));
        emitter.setCallParamMultiResolver((c, i) -> true); // param 0 is MULTI → .getMulti()
        assertEquals("MapperS.of(myCallee.evaluate(myAlias(in1).getMulti()))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // A SINGLE param (resolver false, or absent) keeps the scalar .get() — the slice-2 behaviour unregressed.
        IRJavaLeafEmitter emitterSingle = new IRJavaLeafEmitter();
        emitterSingle.setCallReceiverResolver(c -> "myCallee");
        emitterSingle.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("in1"), null));
        emitterSingle.setCallParamMultiResolver((c, i) -> false);
        assertEquals("MapperS.of(myCallee.evaluate(myAlias(in1).get()))",
                assertInstanceOf(JavaExpression.class,
                        emitterSingle.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());

        // A SCALAR-param arg into a MULTI param strips STRUCTURALLY to the bare slot regardless of asMulti — the
        // .getMulti() accessor fires ONLY for a Mapper-chain (alias) arg, never for a structurally-wrapped param.
        IRVariable scalarArg = new IRVariable("x", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRApply scalarCall = new IRApply(callee, List.of(scalarArg), NodeId.ROOT,
                anyType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRJavaLeafEmitter emitterStrip = new IRJavaLeafEmitter();
        emitterStrip.setCallReceiverResolver(c -> "myCallee");
        emitterStrip.setCallParamMultiResolver((c, i) -> true); // param 0 MULTI, but a scalar param strips — no accessor
        assertEquals("MapperS.of(myCallee.evaluate(x))",
                assertInstanceOf(JavaExpression.class,
                        emitterStrip.emit(scalarCall, null, typeUtil, translator).orElseThrow()).renderToString());
    }

    @Test
    void functionCallWithMultiParamArgStripsToBareSlot() {
        // The multi-param-ARG sub-slice (L-048): a bare MULTI parameter passed AS a call arg compiles to
        // MapperC.<Item>of(xs), which strips STRUCTURALLY (unwrapToBuilder) to the bare evaluate slot —
        //   MapperS.of(myCallee.evaluate(xs))
        // The <Item> witness is discarded by the strip and the .getMulti() accessor is NEVER reached (a structurally-
        // wrapped leaf returns before the chained-Mapper fall-through), so asMulti=true is inert here. Byte-identical to
        // legacy, which compiles a bare multi parameter to the same MapperC wrap and strips it the same way. A builtin
        // item type (String) names the <Item> witness so emitVariable does not decline.
        RMetaAnnotatedType itemType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        RMetaAnnotatedType outType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference callee = new IRReference("MyCallee", IRReference.ReferenceKind.FUNCTION, NodeId.ROOT.child(0),
                outType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable xs = new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                itemType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        IRApply call = new IRApply(callee, List.of(xs), NodeId.ROOT,
                outType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setCallReceiverResolver(c -> "myCallee");
        emitter.setCallParamMultiResolver((c, i) -> true); // the target param is multi, but the arg strips before the accessor
        assertEquals("MapperS.of(myCallee.evaluate(xs))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(call, null, typeUtil, translator).orElseThrow()).renderToString());
    }

    @Test
    void nestedCompoundArithmeticRendersFromTheIR() {
        // The COMPOUND day-count shape (ACT_ACT_ISDA `set result:`):
        //   (daysInNonLeapPeriod / 365) + (daysInLeapYearPeriod / 366)
        // Each inner divide is an alias/literal pair (L-035), witness Integer (e.g. DateDifference -> int), result
        // BigDecimal (divide is UNCONSTRAINED). CRITICAL: each nested divide's type() is MISSING in the real corpus
        // — ExpressionTypeComputer.computeArithmetic returns MISSING when an operand type is MISSING (an alias's is,
        // L-032) and does not special-case divide. So the outer ADD must classify each nested operand by STRUCTURE
        // (nestedArithmeticWitness: divide -> BigDecimal unconditionally), NOT by type() — exactly as legacy
        // numericOperandKind falls from its (MISSING) engine arm to its explicit RArithmeticExpr arm. Setting the
        // nested types MISSING here proves the witness is structural.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType numberType = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRReference nonLeap = new IRReference("daysInNonLeapPeriod", IRReference.ReferenceKind.ALIAS,
                NodeId.ROOT.child(0).child(0), intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit365 = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(365), NodeId.ROOT.child(0).child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp leftDiv = new BinaryOp(BinaryOp.BinOp.DIV, nonLeap, lit365, NodeId.ROOT.child(0),
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRReference leap = new IRReference("daysInLeapYearPeriod", IRReference.ReferenceKind.ALIAS,
                NodeId.ROOT.child(1).child(0), intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit366 = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(366), NodeId.ROOT.child(1).child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp rightDiv = new BinaryOp(BinaryOp.BinOp.DIV, leap, lit366, NodeId.ROOT.child(1),
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        BinaryOp outerAdd = new BinaryOp(BinaryOp.BinOp.ADD, leftDiv, rightDiv, NodeId.ROOT,
                RMetaAnnotatedType.MISSING, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        IRJavaLeafEmitter emitter = new IRJavaLeafEmitter();
        emitter.setAliasResolver(a -> new AliasOperandResolver.Facts(false, List.of("startDate", "endDate"), "Integer"));
        assertEquals(
                "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add("
                        + "MapperMaths.<BigDecimal, Integer, Integer>divide(daysInNonLeapPeriod(startDate, endDate), MapperS.of(365)), "
                        + "MapperMaths.<BigDecimal, Integer, Integer>divide(daysInLeapYearPeriod(startDate, endDate), MapperS.of(366)))",
                assertInstanceOf(JavaExpression.class,
                        emitter.emit(outerAdd, null, typeUtil, translator).orElseThrow()).renderToString());

        // A nested operand whose result kind differs from the outer join DECLINES (the same-kind gate generalizes):
        // `(intA + intB) * numberParam` — the nested ADD result is Integer, the outer join is BigDecimal (raised by
        // the number param), so legacy would coerce the nested Integer result; the IR does not reproduce that, so it
        // declines to legacy. (A nested operand is treated as RESOLVED — witness must equal the join.)
        IRVariable intA = new IRVariable("intA", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable intB = new IRVariable("intB", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp nestedIntAdd = new BinaryOp(BinaryOp.BinOp.ADD, intA, intB, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable numberParam = new IRVariable("rate", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp raisedNested = new BinaryOp(BinaryOp.BinOp.MUL, nestedIntAdd, numberParam, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertTrue(new IRJavaLeafEmitter().emit(raisedNested, null, typeUtil, translator).isEmpty(),
                "a nested int-result operand raised to a number join declines (would be coerced)");

        // NESTED-DIVIDE-FIRST regression guard (RETENSED at #470 — the lock's pin-era premise was corrected):
        // a nested divide classifies NUMBER unconditionally EVEN when its type() is engine-RESOLVED int. This
        // mirrors TODAY'S legacy numericOperandKind, whose post-pin `earlyDiv` arm (facet
        // dispatchVariantParamResolution #369 — landed AFTER the lab pin #279) checks divide BEFORE the engine
        // arm: the fork engine's int/int JOIN types a divide `int` and would mis-join the enclosing operation
        // (`<Integer, Integer, Integer>add(<BigDecimal, …>divide…)` — never a golden shape). `(intX / intY) + 5`:
        // outer join BigDecimal (raised by the divide), the int literal re-renders BigDecimal.valueOf(5). The
        // pin-era lock here asserted the ENGINE-first order (Integer join, bare literal) against pin-era legacy —
        // the exact `<Integer,…>`-where-legacy-renders-`<BigDecimal,…>` witness face the #466 A/B decoded in the
        // dispatch-variant carriers. (Not corpus-exercised on the green set: the compound carriers all have alias
        // operands -> MISSING types -> the structural arm; this locks the resolved-type divide arm.)
        IRVariable intX = new IRVariable("intX", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable intY = new IRVariable("intY", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp resolvedIntDiv = new BinaryOp(BinaryOp.BinOp.DIV, intX, intY, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral lit5 = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(5), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp resolvedDivPlusLit = new BinaryOp(BinaryOp.BinOp.ADD, resolvedIntDiv, lit5, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals(
                "MapperMaths.<BigDecimal, BigDecimal, BigDecimal>add("
                        + "MapperMaths.<BigDecimal, Integer, Integer>divide(MapperS.of(intX), MapperS.of(intY)), "
                        + "MapperS.of(BigDecimal.valueOf(5)))",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(resolvedDivPlusLit, null, typeUtil, translator).orElseThrow())
                        .renderToString());
    }

    @Test
    void literalBodiedAliasWitnessDeclines() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        IRExpressionCompiler c = compiler(workspace());

        // A LITERAL-bodied alias (e.g. `alias base: 365`) classifies as null (decline) — mirroring legacy
        // HandlerHelper.numericOperandKind's literal-alias-body guard. Without this guard, classifying the body
        // directly would over-classify (Integer/BigDecimal), and a literal-bodied-alias DIVIDE would diverge:
        // the IR's typed path emits <BigDecimal,Integer,Integer>divide while legacy's heuristic fallback (which
        // the literal alias body forces) emits <Integer,Integer,Integer>divide. Declining keeps it byte-identical.
        // [Adversarial-review regression guard — not corpus-exercised.]
        RIntLiteral intBody = new RIntLiteral();
        intBody.setValue(BigInteger.valueOf(365));
        assertNull(c.aliasNumericWitness(intBody), "an int-literal alias body declines");

        RNumberLiteral numberBody = new RNumberLiteral();
        numberBody.setValue(new BigDecimal("1.5"));
        assertNull(c.aliasNumericWitness(numberBody), "a number-literal alias body declines");
    }

    @Test
    void parameterEqualityIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // Equality extends the structural tier: a param/param `=` composes
        // areEqual(MapperS.of(l), MapperS.of(r), CardinalityOperator.All) and `<>` composes
        // notEqual(..., CardinalityOperator.Any) — the operator-dependent cardinality default
        // (the first cardMod surface), byte-identical to ComparisonHandler.handle(REqualityExpr).
        // As with comparison, the whole BinaryOp lowers via the IR (operands recurse through the
        // emitter, not the compiler), so irDrivenCount ticks once per equality, not per operand.
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(EqOp.EQ);
        eq.setLeft(scalarParamRef("s1", ws));
        eq.setRight(scalarParamRef("s2", ws));
        eq.attachToWorkspace(ws);
        assertEquals("areEqual(MapperS.of(s1), MapperS.of(s2), CardinalityOperator.All)", render(c, eq));

        REqualityExpr neq = new REqualityExpr();
        neq.setOp(EqOp.NEQ);
        neq.setLeft(scalarParamRef("n1", ws));
        neq.setRight(scalarParamRef("n2", ws));
        neq.attachToWorkspace(ws);
        assertEquals("notEqual(MapperS.of(n1), MapperS.of(n2), CardinalityOperator.Any)", render(c, neq));

        assertEquals(2, c.irDrivenCount(), "both param/param equalities must be emitted from the IR");
    }

    @Test
    void numericLiteralComparisonAndEqualityRenderFromTheIR() {
        // A NUMERIC-literal comparison/equality operand re-renders against the SIBLING's resolved numeric type —
        // byte-identical to ComparisonHandler.inferNumericType threading the literal's expected type (number
        // sibling → MapperS.of(BigDecimal.valueOf(N)); int sibling → bare MapperS.of(N)). Built directly with
        // numeric operand types; ctx=null exercises the no-outer-context path (the emitter builds the join-typed
        // literal context itself). The node's own type() is unread — the operands' witnesses drive the coercion.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType numberType = RMetaAnnotatedType.withNoMeta(RNumberType.unconstrained());
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        // number sibling + int literal: join BigDecimal → the literal renders MapperS.of(BigDecimal.valueOf(0))
        // (the Abs `arg < 0` sign-check carrier). `<` → lessThan + CardinalityOperator.All.
        IRVariable arg = new IRVariable("arg", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral zero = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(0), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp lt = new BinaryOp(BinaryOp.BinOp.LT, arg, zero, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("lessThan(MapperS.of(arg), MapperS.of(BigDecimal.valueOf(0)), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(lt, null, typeUtil, translator).orElseThrow()).renderToString());

        // int sibling + int literal: join Integer → the literal renders BARE MapperS.of(1) (the `count = 1`
        // cardinality-check carrier). `=` → areEqual + All.
        IRVariable count = new IRVariable("count", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(1), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp eq = new BinaryOp(BinaryOp.BinOp.EQ, count, one, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("areEqual(MapperS.of(count), MapperS.of(1), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(eq, null, typeUtil, translator).orElseThrow()).renderToString());

        // int sibling + int literal, `<>` → notEqual + the Any cardinality default (the SubString `startIndex <> 0`).
        IRVariable startIndex = new IRVariable("startIndex", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral zeroB = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(0), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp neq = new BinaryOp(BinaryOp.BinOp.NEQ, startIndex, zeroB, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("notEqual(MapperS.of(startIndex), MapperS.of(0), CardinalityOperator.Any)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(neq, null, typeUtil, translator).orElseThrow()).renderToString());

        // Literal on the LEFT coerces symmetrically (the emitter re-renders whichever operand is the literal):
        // `0 < arg` (number sibling) → the LHS literal renders MapperS.of(BigDecimal.valueOf(0)).
        IRLiteral zeroLeft = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(0), NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRVariable argRight = new IRVariable("arg", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp ltLeft = new BinaryOp(BinaryOp.BinOp.LT, zeroLeft, argRight, NodeId.ROOT,
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("lessThan(MapperS.of(BigDecimal.valueOf(0)), MapperS.of(arg), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(ltLeft, null, typeUtil, translator).orElseThrow()).renderToString());

        // A RESOLVED int sibling raised to a number join by a NUMBER literal DECLINES: legacy would coerce the
        // int operand to BigDecimal (compile(intOperand, BigDecimal, …)), which the IR does not reproduce for a
        // non-literal operand — mirrors the arithmetic resolved-raised decline. Falls back to legacy.
        IRVariable k = new IRVariable("k", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral numLit = new IRLiteral(IRLiteral.LiteralKind.NUMBER, new BigDecimal("1.5"), NodeId.ROOT.child(1),
                numberType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp raised = new BinaryOp(BinaryOp.BinOp.EQ, k, numLit, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertTrue(new IRJavaLeafEmitter().emit(raised, null, typeUtil, translator).isEmpty(),
                "an int sibling raised to a number join by a number literal declines (would be coerced)");
    }

    @Test
    void countAsAComparisonOperandRendersFromTheIR() {
        // `xs count = 1` — a count list-op reached as a comparison/equality OPERAND has NO collection-op renderer
        // (that is installed only for a TOP-LEVEL list-op expression), so the emitter GENUINELY composes the count
        // from the node: <emit(child)>.resultCount(), then wraps it MapperS.of(...) at the comparison — byte-identical
        // to legacy CollectionHandler.handle(RCountExpr) + ComparisonHandler.wrapCountOperand. The int literal sibling
        // renders BARE (count → Integer join), so areEqual(MapperS.of(MapperC.<String>of(xs).resultCount()),
        // MapperS.of(1), CardinalityOperator.All). The render CONTENT is byte-verified on the corpus by
        // Path2ByteIdentityTest; here we prove the nested compose + wrap fire with no renderer installed.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        RMetaAnnotatedType stringType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        RMetaAnnotatedType intType = RMetaAnnotatedType.withNoMeta(RNumberType.intType());

        IRVariable xs = new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                stringType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        IRListOp count = new IRListOp(IRListOp.Kind.COUNT, xs, NodeId.ROOT.child(0),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        IRLiteral one = new IRLiteral(IRLiteral.LiteralKind.INT, BigInteger.valueOf(1), NodeId.ROOT.child(1),
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        BinaryOp eq = new BinaryOp(BinaryOp.BinOp.EQ, count, one, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        // No CollectionOpRenderer installed (the operand path) — the emitter composes the count itself.
        assertEquals("areEqual(MapperS.of(MapperC.<String>of(xs).resultCount()), MapperS.of(1), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(eq, null, typeUtil, translator).orElseThrow()).renderToString());

        // `xs count >= 1` — the same nested compose under a comparison operator (GTE → greaterThanEquals).
        BinaryOp gte = new BinaryOp(BinaryOp.BinOp.GTE, count, one, NodeId.ROOT,
                intType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        assertEquals("greaterThanEquals(MapperS.of(MapperC.<String>of(xs).resultCount()), MapperS.of(1), CardinalityOperator.All)",
                assertInstanceOf(JavaExpression.class,
                        new IRJavaLeafEmitter().emit(gte, null, typeUtil, translator).orElseThrow()).renderToString());
    }

    @Test
    void parameterLogicalIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // Logical completes the binary-boolean tier: it composes the ComparisonResult-producing
        // comparison/equality leaves into a left.andNullSafe(right) / .orNullSafe(right) chain —
        // byte-identical to LogicalHandler. The logical itself contributes no ref/wildcard (those
        // arrive via the operands), and the whole tree lowers via the IR, so irDrivenCount ticks
        // once per logical (operands recurse through the emitter, not the compiler).
        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(comparison(CompOp.GT, "n1", "n2", ws));
        and.setRight(comparison(CompOp.GTE, "n3", "n4", ws));
        and.attachToWorkspace(ws);
        assertEquals(
                "greaterThan(MapperS.of(n1), MapperS.of(n2), CardinalityOperator.All)"
                        + ".andNullSafe(greaterThanEquals(MapperS.of(n3), MapperS.of(n4), CardinalityOperator.All))",
                render(c, and));

        RLogicalExpr or = new RLogicalExpr();
        or.setOp(LogOp.OR);
        or.setLeft(equality(EqOp.EQ, "s1", "s2", ws));
        or.setRight(equality(EqOp.NEQ, "s3", "s4", ws));
        or.attachToWorkspace(ws);
        assertEquals(
                "areEqual(MapperS.of(s1), MapperS.of(s2), CardinalityOperator.All)"
                        + ".orNullSafe(notEqual(MapperS.of(s3), MapperS.of(s4), CardinalityOperator.Any))",
                render(c, or));

        assertEquals(2, c.irDrivenCount(), "both param/param logicals must be emitted from the IR");
    }

    @Test
    void scalarParameterExistenceIsIrDrivenWithLegacyBytes() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);

        // The first unary structural family: `foo exists` / `bar is absent` over a scalar parameter
        // composes the fixed ExpressionOperatorsNullSafe call exists(arg) / notExists(arg) from its
        // IR-driven operand leaf — byte-identical to ExistenceHandler. Whole-node IR-driven, so
        // irDrivenCount ticks once per check (the operand recurses through the emitter).
        assertEquals("exists(MapperS.of(foo))", render(c, existence(ExistenceOp.EXISTS, "foo", ws)));
        assertEquals("notExists(MapperS.of(bar))", render(c, existence(ExistenceOp.ABSENT, "bar", ws)));

        // The payoff: an Existence is a ComparisonResult, so it composes through logical — the whole
        // `foo exists and bar exists` lowers to one IR tree (BinaryOp{AND} of two Existences).
        RLogicalExpr and = new RLogicalExpr();
        and.setOp(LogOp.AND);
        and.setLeft(existence(ExistenceOp.EXISTS, "foo", ws));
        and.setRight(existence(ExistenceOp.EXISTS, "bar", ws));
        and.attachToWorkspace(ws);
        assertEquals("exists(MapperS.of(foo)).andNullSafe(exists(MapperS.of(bar)))", render(c, and));

        assertEquals(3, c.irDrivenCount(), "both bare checks and the composed logical must be emitted from the IR");
    }

    private static RExistenceExpr existence(ExistenceOp op, String param, RWorkspace ws) {
        RExistenceExpr e = new RExistenceExpr();
        e.setOp(op);
        e.setArgument(scalarParamRef(param, ws));
        e.attachToWorkspace(ws);
        return e;
    }

    @Test
    void scalarReceiverNavigationRendersMapFromTheIR() {
        // A single-hop `receiver -> value` navigation off a scalar-param receiver renders
        // receiver.<Witness>map("getX", v -> v.getX()): the witness is the result type() mapped via
        // the translator (here a string feature → String); the lambda var is lowerCamelCase of the
        // RECEIVER's type name ("Trade" → trade). Built directly; ctx=null exercises the no-scope
        // lambda-var branch ("_" + name), so the byte form is deterministic without a render scope.
        REnumeration recvType = fixtureEnum("Trade");
        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(recvType));
        IRVariable receiver = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                receiverType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        RMetaAnnotatedType resultType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        FieldAccess fa = new FieldAccess(receiver, "value", NodeId.ROOT,
                resultType, ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(fa, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperS.of(trade).<String>map(\"getValue\", _trade -> _trade.getValue())",
                ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void existenceOverNavigationComposesFromTheIR() {
        // `foo -> bar exists` composes the navigation operand into the existence check —
        // exists(MapperS.of(foo).<String>map("getBar", v -> v.getBar())). Built directly; ctx=null
        // gives the no-scope lambda var ("_trade" from the receiver type "Trade").
        REnumeration recvType = fixtureEnum("Trade");
        RMetaAnnotatedType receiverType = RMetaAnnotatedType.withNoMeta(new REnumTypeRef(recvType));
        IRVariable receiver = new IRVariable("foo", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                receiverType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        RMetaAnnotatedType strType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        FieldAccess nav = new FieldAccess(receiver, "bar", NodeId.ROOT.child(0),
                strType, ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        Existence exist = new Existence(Existence.ExistOp.EXISTS, null, nav, NodeId.ROOT,
                strType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(exist, null, typeUtil, translator).orElseThrow());
        assertEquals("exists(MapperS.of(foo).<String>map(\"getBar\", _trade -> _trade.getBar()))",
                ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void existenceOverMultiParamRendersFromTheIR() {
        // `xs exists` where `xs` is a multi parameter: existence wraps the MapperC.<Item>of leaf identically
        // to a scalar operand — exists(MapperC.<String>of(xs)) — proving existence is
        // operand-cardinality-agnostic (the emitter is UNCHANGED; only the adapter operand gate broadened to
        // isExistenceOperand). Built directly with a string item type → the <String> witness; ctx=null.
        RMetaAnnotatedType itemType = RMetaAnnotatedType.withNoMeta(RStringType.unconstrained());
        IRVariable xs = new IRVariable("xs", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                itemType, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        Existence exist = new Existence(Existence.ExistOp.EXISTS, null, xs, NodeId.ROOT,
                itemType, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(exist, null, typeUtil, translator).orElseThrow());
        assertEquals("exists(MapperC.<String>of(xs))", out.renderToString());
    }

    @Test
    void chainedNavigationRendersNestedMapFromTheIR() {
        // `foo -> bar -> baz`: a 2-hop chain off a scalar-param base. The outer FieldAccess's receiver
        // is itself a FieldAccess, so emitFieldAccess recurses into the receiver subtree and appends a
        // second getter hop — confirming the chain is composed by the EXISTING emitter (the flip is
        // adapter-only: only the adapter's receiver gate had to broaden). Built directly; ctx=null gives
        // the no-scope lambda var ("_" + receiver-type lowerCamelCase), each hop derived from ITS
        // receiver's result type: the base receiver type "Trade" -> _trade; the inner hop's result type
        // "Party" -> _party. The fixture enums carry declaring models (the #568 heal), so the inner
        // hop's <Witness> types as <Party> — asserted below alongside the nested-map composition and
        // the per-hop lambda-var source (the pre-heal fixture collapsed it to Object, the L-021 pitfall).
        REnumeration tradeType = fixtureEnum("Trade");
        IRVariable base = new IRVariable("foo", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(tradeType)),
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        REnumeration partyType = fixtureEnum("Party");
        FieldAccess inner = new FieldAccess(base, "bar", NodeId.ROOT.child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(partyType)),
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess outer = new FieldAccess(inner, "baz", NodeId.ROOT,
                RMetaAnnotatedType.withNoMeta(RStringType.unconstrained()),
                ExpressionCardinality.SINGLE, ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(outer, null, typeUtil, translator).orElseThrow());
        // <Party> since PR #567's close: the fixture enum now carries its declaring
        // model, so the hop types instead of collapsing to Object (the pre-C0 render
        // this assertion had baked in; real corpus enums always typed this way).
        assertEquals("MapperS.of(foo).<Party>map(\"getBar\", _trade -> _trade.getBar())"
                + ".<String>map(\"getBaz\", _party -> _party.getBaz())", ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void multiFeatureNavigationRendersMapCFromTheIR() {
        // `trade -> legs` (legs (0..*)): a single MULTI feature off a scalar-param receiver renders
        // MapperS.of(trade).<Leg>mapC("getLegs", _trade -> _trade.getLegs()) — the mapC token comes from
        // the node's MULTI cardinality, the witness is the element type. Confirms the EXISTING emitter
        // already renders mapC for a MULTI FieldAccess (so the first multi hop is adapter-only for this
        // receiver-single cell). Built directly with a builtin result type (String stands in for the
        // element type → <String>); ctx=null gives the no-scope lambda var. A .mapC() instance call adds
        // no MapperC import (only MapperS.of at the leaf), matching legacy.
        REnumeration tradeType = fixtureEnum("Trade");
        IRVariable receiver = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(tradeType)),
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess fa = new FieldAccess(receiver, "legs", NodeId.ROOT,
                RMetaAnnotatedType.withNoMeta(RStringType.unconstrained()),
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.OPTIONAL, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(fa, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperS.of(trade).<String>mapC(\"getLegs\", _trade -> _trade.getLegs())",
                ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void chainThroughMultiRendersMapCThenMapFromTheIR() {
        // `trade -> legs -> rate` (legs multi, rate single): the emitter keys map/mapC on EACH hop's own
        // featureCardinality, so legs renders mapC and rate renders map (off the resulting MapperC):
        // MapperS.of(trade).<Leg>mapC("getLegs", ...).<Rate>map("getRate", ...). This is the per-hop-card
        // fix — keying on the node's accumulated cardinality (MULTI for the whole chain) would WRONGLY
        // render .mapC for the rate hop. Built directly: fixture-enum receiver types with declaring
        // models (the #568 heal — the inner witness types as <Leg>; the lambda vars read _trade, _leg)
        // + a builtin outer witness; ctx=null → "_"-prefixed lambda vars.
        REnumeration tradeType = fixtureEnum("Trade");
        IRVariable base = new IRVariable("trade", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0).child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(tradeType)),
                ExpressionCardinality.SINGLE, Optionality.PRESENT, SourceRange.NONE);
        REnumeration legType = fixtureEnum("Leg");
        FieldAccess inner = new FieldAccess(base, "legs", NodeId.ROOT.child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(legType)),
                ExpressionCardinality.MULTI, ExpressionCardinality.MULTI, Optionality.OPTIONAL, SourceRange.NONE);
        FieldAccess outer = new FieldAccess(inner, "rate", NodeId.ROOT,
                RMetaAnnotatedType.withNoMeta(RStringType.unconstrained()),
                ExpressionCardinality.MULTI, ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(outer, null, typeUtil, translator).orElseThrow());
        // <Leg> since PR #567's close: the declaring-model fixture types the hop
        // (see the Party note above).
        assertEquals("MapperS.of(trade).<Leg>mapC(\"getLegs\", _trade -> _trade.getLegs())"
                + ".<String>map(\"getRate\", _leg -> _leg.getRate())", ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void navigationOffMultiParamBaseRendersMapperCThenMapFromTheIR() {
        // `legs -> rate` (legs a multi PARAMETER, rate single): a navigation whose BASE receiver is a multi
        // parameter (an IRVariable{PARAM, MULTI}, not a FieldAccess) renders the multi-param MapperC leaf
        // then one getter hop: MapperC.<Leg>of(legs).<Rate>map("getRate", leg -> leg.getRate()). The flip is
        // adapter-only (isNavigableReceiver now admits a multi PARAM) — the EXISTING emitter already renders
        // the MapperC.of receiver leaf (L-021) and the per-hop map step (L-022/L-026). The lambda var derives
        // from the receiver's ELEMENT type name ("Leg" -> _leg), identical to legacy resolveSymbolTypeName
        // (which reads the declared element type regardless of cardinality) — so the per-hop lambda-var source
        // is cardinality-agnostic. The sole difference from chainThroughMultiRendersMapCThenMapFromTheIR is the
        // receiver leaf: a MapperC.of(legs) multi-param leaf vs a multi `trade -> legs` hop. Built directly: a
        // fixture-enum element type with a declaring model (the #568 heal — its <Witness> types as <Leg>;
        // the lambda var reads _leg) + a builtin outer witness; ctx=null -> "_"-prefixed lambda var.
        REnumeration legType = fixtureEnum("Leg");
        IRVariable legs = new IRVariable("legs", IRVariable.VariableKind.PARAM, NodeId.ROOT.child(0),
                RMetaAnnotatedType.withNoMeta(new REnumTypeRef(legType)),
                ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        FieldAccess fa = new FieldAccess(legs, "rate", NodeId.ROOT,
                RMetaAnnotatedType.withNoMeta(RStringType.unconstrained()),
                ExpressionCardinality.MULTI, ExpressionCardinality.SINGLE, Optionality.OPTIONAL, SourceRange.NONE);

        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);
        JavaExpression out = assertInstanceOf(JavaExpression.class,
                new IRJavaLeafEmitter().emit(fa, null, typeUtil, translator).orElseThrow());
        // <Leg> since PR #567's close: the declaring-model fixture types the base
        // (see the Party note above).
        assertEquals("MapperC.<Leg>of(legs).<String>map(\"getRate\", _leg -> _leg.getRate())",
                ImportCollisionResolver.stripToBare(out.renderToString()));
    }

    @Test
    void collisionFqnMirrorsLegacyJavaNameKeying() {
        // L-029: the witness/output collision decision now lives in the emitter and keys on JAVA names
        // (mirroring ReferenceHandler.mapperCWitnessOutputCollisionFqn / NavigationHandler.witnessOutputCollisionFqn),
        // NOT on Rune type-name strings (the old adapter proxy that mis-keyed the cross-namespace case).
        // Two real classes with the SAME simple name ("Date") but DIFFERENT packages model the collision.
        JavaClass<?> rosettaDate = JavaClass.from(com.rosetta.model.lib.records.Date.class);
        JavaClass<?> utilDate = JavaClass.from(java.util.Date.class);

        // Same Java simple name, different canonical → FQN-inline (returns the witness's dotted canonical).
        assertEquals("com.rosetta.model.lib.records.Date", IRJavaLeafEmitter.collisionFqn(rosettaDate, utilDate),
                "a witness colliding with the output on simple name (different package) renders FQN-inline");
        // Same type (same canonical) → no collision: the self-referential merge(xs T (0..*)) -> result T case.
        assertEquals(null, IRJavaLeafEmitter.collisionFqn(rosettaDate, rosettaDate),
                "the witness IS the output type → bare witness, no FQN-inline");
        // Different simple names → no collision.
        assertEquals(null, IRJavaLeafEmitter.collisionFqn(rosettaDate, JavaClass.from(String.class)),
                "different simple names → no collision");
        // No output (non-function seam / no output / meta output) → no collision.
        assertEquals(null, IRJavaLeafEmitter.collisionFqn(rosettaDate, null),
                "a null output type (no enclosing function output) → no collision, bare witness");
    }

    @Test
    void collisionFqnRenderTicksTheInstrumentationCounter() {
        // §4.2 instrumentation positive proof: when emit() actually PRODUCES a witness/output collision
        // FQN-inline render, collisionFqnRenderCount() ticks — so a corpus count of 0 is a real finding
        // ("no IR-driven emission hit a cross-namespace collision"), not a dead counter. Vehicle: a MULTI
        // `date` parameter — Rune `date` maps to com.rosetta.model.lib.records.Date, which collides on the
        // Java SIMPLE name "Date" with the enclosing function output java.util.Date but differs in package.
        RMetaAnnotatedType dateItem = RMetaAnnotatedType.withNoMeta(RRecordType.DATE);
        IRVariable multiDates = new IRVariable("ds", IRVariable.VariableKind.PARAM, NodeId.ROOT,
                dateItem, ExpressionCardinality.MULTI, Optionality.PRESENT, SourceRange.NONE);
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator translator = new JavaTypeTranslator(typeUtil);

        // Collision (output java.util.Date) → witness renders FQN-inline AND the counter ticks.
        IRJavaLeafEmitter colliding = new IRJavaLeafEmitter();
        JavaExpression collided = assertInstanceOf(JavaExpression.class,
                colliding.emit(multiDates, null, typeUtil, translator, JavaClass.from(java.util.Date.class)).orElseThrow());
        assertEquals("MapperC.<com.rosetta.model.lib.records.Date>of(ds)", collided.renderToString());
        assertEquals(1, colliding.collisionFqnRenderCount(), "an FQN-inline collision render must tick the counter");

        // Control: no output (null) → bare simple-name witness, counter flat — so the counter measures
        // collisions specifically, not every MapperC witness render.
        IRJavaLeafEmitter bare = new IRJavaLeafEmitter();
        JavaExpression bareOut = assertInstanceOf(JavaExpression.class,
                bare.emit(multiDates, null, typeUtil, translator).orElseThrow());
        assertEquals("MapperC.<Date>of(ds)", bareOut.renderToString());
        assertEquals(0, bare.collisionFqnRenderCount(), "no output collision → bare witness, counter flat");
    }

    private static RComparisonExpr comparison(CompOp op, String left, String right, RWorkspace ws) {
        RComparisonExpr cmp = new RComparisonExpr();
        cmp.setOp(op);
        cmp.setLeft(scalarParamRef(left, ws));
        cmp.setRight(scalarParamRef(right, ws));
        cmp.attachToWorkspace(ws);
        return cmp;
    }

    private static REqualityExpr equality(EqOp op, String left, String right, RWorkspace ws) {
        REqualityExpr eq = new REqualityExpr();
        eq.setOp(op);
        eq.setLeft(scalarParamRef(left, ws));
        eq.setRight(scalarParamRef(right, ws));
        eq.attachToWorkspace(ws);
        return eq;
    }

    private static String render(IRExpressionCompiler c, RExpression expr) {
        return render(c, expr, null);
    }

    /**
     * {@link #render} under a fresh REAL statement scope with the deferred-registry sentinels
     * FINALIZED ({@code resolveDeferredCoercionNames} — the production finalization hook), so the
     * hop lambda vars compare by their resolved names on both routes (#480).
     */
    private static String renderScoped(IRExpressionCompiler c, RExpression expr) {
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope =
                new com.regnosys.rosetta.generator.java.scoping.JavaStatementScope("test", null);
        JavaStatementBuilder out = c.compile(expr, null, scope);
        return scope.resolveDeferredCoercionNames(ImportCollisionResolver.stripToBare(
                assertInstanceOf(JavaExpression.class, out).renderToString()));
    }

    /** {@link #legacyRender}'s scoped + finalized twin — see {@link #renderScoped}. */
    private static String legacyRenderScoped(ExpressionCompiler c, RExpression expr) {
        com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope =
                new com.regnosys.rosetta.generator.java.scoping.JavaStatementScope("test", null);
        JavaStatementBuilder out = c.compile(expr, null, scope);
        return scope.resolveDeferredCoercionNames(ImportCollisionResolver.stripToBare(
                assertInstanceOf(JavaExpression.class, out).renderToString()));
    }

    private static String render(IRExpressionCompiler c, RExpression expr, JavaType expectedType) {
        JavaStatementBuilder out = c.compile(expr, expectedType, null);
        // stripToBare: see legacyRender — the sentinel neutralised identically on both routes.
        return ImportCollisionResolver.stripToBare(
                assertInstanceOf(JavaExpression.class, out).renderToString());
    }

    private static RStringLiteral string(String value) {
        RStringLiteral lit = new RStringLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RBooleanLiteral bool(boolean value) {
        RBooleanLiteral lit = new RBooleanLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RNumberLiteral number(String value) {
        RNumberLiteral lit = new RNumberLiteral();
        lit.setValue(new BigDecimal(value));
        return lit;
    }

    private static RIntLiteral intLiteral(long value) {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RIntLiteral intLiteral(BigInteger value) {
        RIntLiteral lit = new RIntLiteral();
        lit.setValue(value);
        return lit;
    }

    private static RSymbolReference scalarParamRef(String name, RWorkspace ws) {
        return paramRef(name, cardinality(1, false), ws);
    }

    /** A resolved bare reference to a plain function's input parameter, attached to {@code ws}. */
    private static RSymbolReference paramRef(String name, RCardinality card, RWorkspace ws) {
        RAttribute attr = new RAttribute();
        attr.setName(name);
        attr.setCardinality(card);
        RFunction func = new RFunction();
        func.setName("MyFunc");
        func.inputs().add(attr);
        attr.setParent(func);
        RSymbolReference ref = new RSymbolReference();
        ref.setName(name);
        ref.setResolvedSymbol(attr);
        func.attachToWorkspace(ws);
        attr.attachToWorkspace(ws);
        ref.attachToWorkspace(ws);
        return ref;
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
}
