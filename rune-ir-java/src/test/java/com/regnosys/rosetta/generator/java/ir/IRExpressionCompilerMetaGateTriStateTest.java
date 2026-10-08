package com.regnosys.rosetta.generator.java.ir;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * The metaGate's element walk ({@code IRExpressionCompiler.elementTerminalMeta}) is THREE-STATE by
 * contract — {@code TRUE} / {@code FALSE} / {@code null} = unknown (prove-or-decline; the caller treats
 * unknown as meta) — and {@code unknownFace} returns that {@code null} while recording the FIRST
 * bottoming shape. Four of its arms paired the primitive {@code isMetaAnnotatedAttr(…)} with the
 * {@code Boolean} {@code unknownFace(…)} in ONE conditional expression; a mixed conditional's type is
 * the primitive, so the unknown arm was UNBOXED at the return — an NPE ("Cannot invoke
 * Boolean.booleanValue() because the return value of unknownFace(String[], String) is null") that the
 * IR route turned into a WHOLE-BODY {@code /* TODO: expression compilation error … *&#47;} stub for
 * drr 7.0.0–7.3.0 {@code DTCC_UnderlyingAssetReportRule}. The stub pre-dated seat 20 (PR #592) and was
 * hidden while that file sat in the band for the legacy-route reason seat 20 healed; the heal exposed
 * it as the only file on which the two routes disagreed (ON 420 vs OFF 416). The arms are now boxed.
 *
 * <p>Reached by reflection (the walk is a private static) with hand-built nodes, the way the sibling
 * {@code IRExpressionCompilerTest} builds its fixtures. {@code a1}/{@code a2} are RED (an
 * {@code InvocationTargetException} wrapping the NPE) with the boxing reverted at the feature-call and
 * deep-feature-call arms; {@code b1}/{@code b2} are the positive controls — a RESOLVED plain attribute
 * reaches the boxed arm and reads {@code FALSE} with no face recorded. The other two repaired arms
 * ({@code evrChainLeafless} — {@code REnumValueRef.AttributeChain} rejects a null feature at
 * construction; {@code fnNoOutput} — the grammar requires a function output) cannot be reached by a
 * well-formed fixture and are pinned by the same edit class + the ON ring (the drr 7.x POJO rows).
 */
class IRExpressionCompilerMetaGateTriStateTest {

    private static Boolean gate(RExpression e, String[] face) throws Exception {
        Method m = IRExpressionCompiler.class.getDeclaredMethod(
                "elementTerminalMeta", RExpression.class, int.class, Set.class, String[].class);
        m.setAccessible(true);
        try {
            return (Boolean) m.invoke(null, e, 0, new HashSet<RRule>(), face);
        } catch (InvocationTargetException ite) {
            throw new AssertionError("elementTerminalMeta threw instead of returning the tri-state verdict: "
                    + ite.getCause(), ite.getCause());
        }
    }

    /** a1 — an UNRESOLVED feature call is the unknown verdict ({@code null}, face {@code fcUnresolved}), not an NPE. */
    @Test
    void a1_unresolvedFeatureCallIsUnknownNotAnNpe() {
        RFeatureCall fc = new RFeatureCall();
        fc.setFeatureName("identifier");
        String[] face = new String[1];
        AtomicReference<Boolean> verdict = new AtomicReference<>();
        assertDoesNotThrow(() -> verdict.set(gate(fc, face)));
        assertNull(verdict.get(), "an unresolved feature call must read UNKNOWN (null)");
        assertEquals("fcUnresolved", face[0], "the face recorder must still name the bottoming shape");
    }

    /** a2 — an UNRESOLVED deep feature call ({@code ->>}) likewise. */
    @Test
    void a2_unresolvedDeepFeatureCallIsUnknownNotAnNpe() {
        RDeepFeatureCall deep = new RDeepFeatureCall();
        deep.setFeatureName("identifier");
        String[] face = new String[1];
        AtomicReference<Boolean> verdict = new AtomicReference<>();
        assertDoesNotThrow(() -> verdict.set(gate(deep, face)));
        assertNull(verdict.get(), "an unresolved deep feature call must read UNKNOWN (null)");
        assertEquals("deepFcUnresolved", face[0]);
    }

    /** b1 — positive control: a RESOLVED plain attribute reaches the boxed arm and reads FALSE, no face. */
    @Test
    void b1_resolvedPlainFeatureCallReadsFalse() throws Exception {
        RAttribute plain = new RAttribute();
        plain.setName("identifier");
        RFeatureCall fc = new RFeatureCall();
        fc.setFeatureName("identifier");
        fc.setResolvedFeature(plain);
        String[] face = new String[1];
        assertEquals(Boolean.FALSE, gate(fc, face));
        assertNull(face[0], "a provable verdict records no face");
    }

    /** b2 — positive control for the deep arm. */
    @Test
    void b2_resolvedPlainDeepFeatureCallReadsFalse() throws Exception {
        RAttribute plain = new RAttribute();
        plain.setName("identifier");
        RDeepFeatureCall deep = new RDeepFeatureCall();
        deep.setFeatureName("identifier");
        deep.setResolvedFeature(plain);
        String[] face = new String[1];
        assertEquals(Boolean.FALSE, gate(deep, face));
        assertNull(face[0]);
    }
}
