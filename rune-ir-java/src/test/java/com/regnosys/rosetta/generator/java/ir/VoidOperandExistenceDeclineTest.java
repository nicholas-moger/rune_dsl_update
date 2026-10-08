package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ast.util.AstWalker;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RWorkspace;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * v3.2 seat 9 (PR #630, F8 / D47 — the {@code nothing} render law on the IR route, LAW 77): a scalar-parameter
 * existence check whose operand is a Void item (a model-declared basicType — {@code java.lang.Void} at the Java seat,
 * the D47 mapping) DECLINES {@link IRExpressionCompiler#visitExistence}'s IR claim to the shared
 * {@code ExistenceHandler}, whose Void arm renders the EMPTY mapper upstream's coercion produces —
 * {@code exists(MapperS.<Void>ofNull())} — where the claim would have rendered {@code exists(MapperS.of(t))}
 * (the oracle group {@code void-mapping-render-edge}'s InputExists, pinned from the released plugin BEFORE the code).
 * The control is the same shape over a {@code string} input, where the claim fires as before.
 *
 * <p>This is the lane witness that lives INSIDE the mutated module (the seat-8 lane law): lane L7 removes the decline
 * and this class must go RED on the render alone. Round 3 (the code-quality review's SF-2) — the counters at HEAD:
 * the Void case is {@code irDriven=0 irDeclined=0} (the Void verdict is read BEFORE any compile since round 2, so the
 * operand is never driven, and the decline sits before the claim so nothing is recorded), where round 1 had
 * {@code irDriven=1} (the operand's scalar-parameter leaf compiled then discarded) — the verify gate at the round-2
 * head caught that move ({@code verify-c14.status}). The class's NON-VACUITY witness is therefore the RENDER assertion
 * ({@code exists(MapperS.<Void>ofNull())}), which cannot pass on an inert compiler — the shared handler must actually
 * produce it — and the STRING control's {@code irDriven=1} is the receipt that the IR compiler is live; the two
 * counter asserts (0 / 0) pin that nothing is driven or recorded on the Void path, not that the compiler ran.
 *
 * <p><b>THE LANE, MEASURED</b> (lane L7 of {@code target/v32-seat9-instruments/lanes-s9.py} at the commit-17 head
 * {@code 2e7bdbd65} — {@code scratch/lanes-s9-c17.status}, local; LAW 82 — first at {@code aa27ef95e}, the same at {@code 5f3f716bf} and {@code 041febe2f}): the decline block deleted → this class's Void
 * case RED alone (2 run / 1 F), the string control green — EXACTLY the prediction; the class's drive pin is 0 since round 2 (a Void operand is never compiled).
 */
class VoidOperandExistenceDeclineTest {

    private static final Path BUILTINS_ROOT =
            Path.of("../rune-dsl/rune-runtime/src/main/resources/model");

    private static final String MODEL = """
            namespace census.seat9voidir
            version "0.0.0"

            basicType etoken <"A model-declared basic type.">

            func InputExists: <"A Void-typed input under exists - the scalar-parameter seat.">
                inputs:
                    t etoken (0..1)
                output:
                    r boolean (1..1)
                set r: t exists

            func StrExists: <"The control: a string input under exists - the scalar-parameter claim fires.">
                inputs:
                    s string (0..1)
                output:
                    r boolean (1..1)
                set r: s exists
            """;

    private static RWorkspace cachedWorkspace;

    private static RWorkspace workspace() throws IOException {
        if (cachedWorkspace == null) {
            List<RModel> models = new ArrayList<>();
            RModel inline = AstBuilder.buildFromString(MODEL, "seat9voidir.rosetta");
            inline.setVersion("0.0.0.test");
            models.add(inline);
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

    /** The value of the ONE operation of the named function of the inline model. */
    private static RExpression operationValue(RWorkspace ws, String functionName) {
        for (RModel m : ws.files()) {
            for (RFunction fn : AstWalker.findAll(m, RFunction.class)) {
                if (functionName.equals(fn.name())) {
                    assertEquals(1, fn.operations().size(), "one operation in " + functionName);
                    return fn.operations().get(0).expression();
                }
            }
        }
        throw new AssertionError("function not found in the workspace: " + functionName);
    }

    private static String render(IRExpressionCompiler c, RExpression expr) {
        JavaStatementScope scope = new JavaStatementScope("test", null);
        JavaStatementBuilder out = c.compile(expr, null, scope);
        return ImportCollisionResolver.stripToBare(
                assertInstanceOf(JavaExpression.class, out).renderToString());
    }

    @Test
    void voidItemOperand_declinesTheClaim_theSharedHandlerRendersTheEmptyMapper() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT),
                "rune-dsl builtins absent (" + BUILTINS_ROOT.toAbsolutePath() + ") — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);
        String rendered = render(c, operationValue(ws, "InputExists"));
        assertEquals("exists(MapperS.<Void>ofNull())", rendered,
                "the Void-item operand is the EMPTY mapper - the shared handler's Void arm, not the IR claim's MapperS.of(t)");
        // MEASURED at round 1 (t-irdecline-adj-r2.log): ONE drive - the operand `t`, the scalar-parameter leaf the shared
        // handler compiled through this route before the Void arm discarded its render. Round 2 (the oracle group
        // void-mapping-render-hoist-second: upstream's discarded builder consumes NO name) reads the Void verdict BEFORE
        // any compile, so the operand is never driven - the pin moved 1 -> 0 and the verify gate at the round-2 head
        // caught the move (verify-c14.status); the existence claim itself never fired either way (the decline sits
        // BEFORE tryEmitFromIR, so no decline is recorded)
        assertEquals(0, c.irDrivenCount(), "no drive at all: the Void verdict precedes any compile since round 2 (measured)");
        assertEquals(0, c.irDeclinedCount(), "the decline precedes the claim - nothing recorded (measured)");
    }

    @Test
    void stringOperand_control_theScalarParameterClaimFires() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_ROOT), "builtins absent — skipped");
        RWorkspace ws = workspace();
        IRExpressionCompiler c = compiler(ws);
        String rendered = render(c, operationValue(ws, "StrExists"));
        assertEquals("exists(MapperS.of(s))", rendered, "the control keeps the scalar-parameter claim's render");
        // MEASURED (t-irdecline-adj-r2.log): the ONE drive is the existence claim, its operand rendered within it
        assertEquals(1, c.irDrivenCount(), "the existence claim's drive alone (measured)");
        assertEquals(0, c.irDeclinedCount(), "no decline on the control (measured)");
    }
}
