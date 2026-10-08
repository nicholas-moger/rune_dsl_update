package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.generator.java.scoping.JavaStatementScope;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Always-on (corpus-independent) pins for the statement-hoist machinery's
 * pure logic: the {@link StatementHoistSession} group-law naming (bare when
 * singleton — keyword-escaped {@code _boolean} for the {@code boolean} group —
 * {@code 0..n-1} in registration order otherwise, per name group
 * INDEPENDENTLY — upstream {@code computeActualNames} over one
 * {@code assignOutputBodyScope}; facets {@code ifthenelse_result_hoisting} +
 * {@code biginteger_literal_hoist} + {@code boolean_condition_hoist}) and the
 * {@link JavaStatementScope}
 * statement-hoist sink walk (nearest-sink resolution stopping at lambda
 * boundaries, so lambda-interior conditionals and discarded lambda-sibling
 * compile attempts can never hoist into the live statement).
 * The corpus-gated {@code FunctionIfThenElseHoistTest} /
 * {@code FunctionBigIntegerLiteralHoistTest} /
 * {@code FunctionBooleanConditionHoistTest} byte-anchors skip when the
 * frozen cells are absent (CI / fresh clone); these pins do not.
 */
class StatementHoistSessionTest {

    @Test
    void singletonRegistration_resolvesBareName() {
        StatementHoistSession session = new StatementHoistSession();
        String sentinel = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertFalse(session.isEmpty());
        assertEquals(
                "final ComparisonResult ifThenElseResult;\nx = ifThenElseResult.get();",
                session.resolve("final ComparisonResult " + sentinel + ";\nx = " + sentinel + ".get();"),
                "a singleton group must keep the BARE ifThenElseResult name (no suffix)");
    }

    @Test
    void multipleRegistrations_numberZeroBasedInRegistrationOrder() {
        StatementHoistSession session = new StatementHoistSession();
        String first = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String second = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String third = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertEquals("ifThenElseResult0 ifThenElseResult1 ifThenElseResult2",
                session.resolve(first + " " + second + " " + third),
                "a group of n >= 2 must number EVERY member 0..n-1 in registration "
                + "order — a bare name never coexists with numbered ones");
    }

    @Test
    void distinctNameGroups_numberIndependently() {
        // The jfsa GetNtnlQty golden interleave: bigInteger0/1 with
        // ifThenElseResult0/1 — each group numbers 0..n-1 on its own count.
        StatementHoistSession session = new StatementHoistSession();
        String big0 = session.register(StatementHoistSession.BIG_INTEGER);
        String ite0 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String big1 = session.register(StatementHoistSession.BIG_INTEGER);
        String ite1 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertEquals("bigInteger0 ifThenElseResult0 bigInteger1 ifThenElseResult1",
                session.resolve(big0 + " " + ite0 + " " + big1 + " " + ite1),
                "name groups must number independently of each other");
    }

    @Test
    void singletonGroupBesideMultiGroup_keepsBareName() {
        // One bigInteger hoist beside two ifThenElseResult hoists: the
        // bigInteger group is a singleton and keeps the bare name even though
        // the session is not.
        StatementHoistSession session = new StatementHoistSession();
        String ite0 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String big = session.register(StatementHoistSession.BIG_INTEGER);
        String ite1 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertEquals("ifThenElseResult0 bigInteger ifThenElseResult1",
                session.resolve(ite0 + " " + big + " " + ite1),
                "a singleton group keeps its bare base name regardless of "
                + "OTHER groups' sizes");
    }

    @Test
    void booleanSingleton_escapesKeywordToUnderscoreName() {
        // facet boolean_condition_hoist: the bare base name 'boolean' is a
        // Java keyword — the singleton arm escapes with the upstream
        // GeneratorScope '_'-prefix loop (golden `final Boolean _boolean = `).
        StatementHoistSession session = new StatementHoistSession();
        String sentinel = session.register(StatementHoistSession.BOOLEAN);
        assertEquals(
                "final Boolean _boolean;\nif ((_boolean == null ? false : _boolean)) {",
                session.resolve("final Boolean " + sentinel + ";\nif ((" + sentinel
                        + " == null ? false : " + sentinel + ")) {"),
                "a singleton 'boolean' group must escape the keyword to _boolean");
    }

    @Test
    void booleanMultiple_numberWithoutEscape() {
        // boolean0/boolean1 are VALID identifiers — numbered members never
        // escape (_boolean0 has zero golden carriers).
        StatementHoistSession session = new StatementHoistSession();
        String first = session.register(StatementHoistSession.BOOLEAN);
        String second = session.register(StatementHoistSession.BOOLEAN);
        assertEquals("boolean0 boolean1",
                session.resolve(first + " " + second),
                "a 'boolean' group of n >= 2 must number boolean0..n-1 with NO escape");
    }

    @Test
    void booleanGroup_numbersIndependentlyOfOtherGroups() {
        // A boolean hoist beside ifThenElseResult hoists: each group numbers
        // on its own count (the #177 per-name-group independence law).
        StatementHoistSession session = new StatementHoistSession();
        String ite0 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String bool = session.register(StatementHoistSession.BOOLEAN);
        String ite1 = session.register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertEquals("ifThenElseResult0 _boolean ifThenElseResult1",
                session.resolve(ite0 + " " + bool + " " + ite1),
                "a singleton 'boolean' group escapes to _boolean regardless of "
                + "OTHER groups' sizes");
    }

    @Test
    void register_rejectsNonIdentifierBase() {
        // The singleton keyword-escape loop terminates only for
        // identifier-shaped bases (a keyword escapes in one '_' prefix; a
        // base with illegal characters never would) — register() fails fast.
        StatementHoistSession session = new StatementHoistSession();
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> session.register("not-an-identifier"),
                "a non-identifier-shaped group base must be rejected at registration");
    }

    @Test
    void emptySession_isEmptyAndResolveIsIdentity() {
        StatementHoistSession session = new StatementHoistSession();
        assertTrue(session.isEmpty());
        assertEquals("unchanged", session.resolve("unchanged"));
    }

    @Test
    void sentinelsAreUniqueAcrossSessionsAndGroups() {
        // Two sessions' rendered strings must never carry the same token — the
        // first resolution's literal String.replace would consume both. The
        // one global counter also serves every name group within a session,
        // so SAME-group registrations across sessions must differ too (the
        // original cross-session pin — a per-session counter with the base
        // name embedded would pass the cross-group check yet collide here).
        String a = new StatementHoistSession().register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        String b = new StatementHoistSession().register(StatementHoistSession.BIG_INTEGER);
        String c = new StatementHoistSession().register(StatementHoistSession.IF_THEN_ELSE_RESULT);
        assertFalse(a.equals(b), "sentinel tokens must be globally unique across groups");
        assertFalse(a.equals(c), "sentinel tokens must be globally unique across "
                + "sessions registering the SAME name group");
    }

    @Test
    void sinkWalk_findsNearestMarkedAncestor() {
        StatementHoistSession session = new StatementHoistSession();
        JavaStatementScope root = new JavaStatementScope("stmt", null);
        root.markStatementHoistSink(session);
        JavaStatementScope body = root.bodyScope();
        assertSame(root, body.findStatementHoistSink(),
                "a non-lambda descendant must resolve the marked ancestor sink");
        assertSame(session, body.findStatementHoistSink().statementHoistSession());
    }

    @Test
    void sinkWalk_stopsAtLambdaBoundary() {
        JavaStatementScope root = new JavaStatementScope("stmt", null);
        root.markStatementHoistSink(new StatementHoistSession());
        JavaStatementScope lambda = root.lambdaScope();
        assertNull(lambda.findStatementHoistSink(),
                "a lambda boundary between the compile scope and the sink must "
                + "DECLINE the hoist (lambda-interior conditionals keep the "
                + "inline ternary; discarded lambda-sibling attempts cannot leak)");
        assertNull(lambda.bodyScope().findStatementHoistSink(),
                "the boundary also blocks deeper descendants of the lambda");
    }

    @Test
    void sinkWalk_unmarkedTreeFindsNothing() {
        JavaStatementScope root = new JavaStatementScope("stmt", null);
        assertNull(root.bodyScope().findStatementHoistSink(),
                "an unmarked scope tree (rule/alias/POJO-condition compilation, "
                + "then-extract sub-renders) must decline the hoist");
    }

    @Test
    void hoistRegistry_drainSinceMarkReturnsOnlyNewerBlocks() {
        JavaStatementScope sink = new JavaStatementScope("stmt", null);
        sink.markStatementHoistSink(new StatementHoistSession());
        sink.registerStatementHoist("block-a");
        int mark = sink.statementHoistMark();
        sink.registerStatementHoist("block-b");
        sink.registerStatementHoist("block-c");
        assertEquals(java.util.List.of("block-b", "block-c"),
                sink.drainStatementHoistsSince(mark),
                "per-arm drains must take exactly the blocks registered since "
                + "their mark, in registration order");
        assertEquals(java.util.List.of("block-a"),
                sink.drainStatementHoistsSince(0),
                "earlier blocks stay registered until their own drain point");
        assertEquals(java.util.List.of(), sink.drainStatementHoistsSince(0),
                "a drained registry is empty");
    }
}
