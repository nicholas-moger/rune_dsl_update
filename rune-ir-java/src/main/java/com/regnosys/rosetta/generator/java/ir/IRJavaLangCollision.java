package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * THE {@code java.lang} SIMPLE-NAME COLLISION LAW, as ONE shared pure function (v3.3 seat 8, PR #644 - the property
 * gate; {@code PLAN.md} § B family 12, risk R3).
 *
 * <p>A generated type whose SIMPLE NAME is also the simple name of an implicitly-imported {@code java.lang} class is
 * never imported: every Java TYPE position writes it fully qualified, and the string literals keep the simple name
 * (the #306 facet; the corpus carriers are iso20022's {@code Error} and rune-fpml's {@code Exception} /
 * {@code Math}). The old generator states that law TWICE - {@code ValidatorScan.collidesWithJavaLang}
 * ({@code rune-java-generator}, {@code object/validators/ValidatorScan.java:215-230}) and
 * {@code DeepPathUtilGenerator.collidesWithJavaLang} ({@code object/deeppath/DeepPathUtilGenerator.java:417-432}) -
 * each with its own {@code ConcurrentHashMap} cache and its own class loader. Those are the two sites the data-type
 * emitter replaces, and this is the function it will call at both.
 *
 * <p><b>Why it is shared rather than mirrored (risk R3).</b> The law is
 * {@code Class.forName("java.lang." + n, false, <some loader>)}. An emitter that runs in {@code rune-ir-java} runs
 * under a different class loader from {@code rune-java-generator}'s, so two independent copies could disagree for a
 * reason that has nothing to do with the model - a divergence no model fact could explain and no lane could
 * attribute. ONE function removes the degree of freedom. It is the LAW 69 exception the seat states out loud: the
 * derived-facts reconcile asserts {@code collision.<simpleName>} with the old scan's own predicate on the source half
 * (reached through the {@code ValidatorScan} reconcile seam) and this function on the IR half, so the two halves stay
 * two producers over the NAME - what they must not be two producers over is the JDK.
 *
 * <p>The fail direction is the old sites': ANY lookup failure - the class is absent, the link fails, the loader
 * throws - answers {@code false} ("no collision"), which is the corpus default (a bare simple name).
 */
public final class IRJavaLangCollision {

    /** Memoised per simple name, exactly as both old sites memoise - the lookup is pure and the answer never moves. */
    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    private IRJavaLangCollision() {
    }

    /**
     * Whether {@code simpleName} names a class in {@code java.lang} - the ONE producer of the collision law for the
     * IR route.
     *
     * @param simpleName a Java simple name (never qualified); {@code null} or blank answers {@code false}
     * @return {@code true} iff {@code java.lang.<simpleName>} resolves
     */
    public static boolean collides(String simpleName) {
        if (simpleName == null || simpleName.isBlank()) {
            return false;
        }
        return CACHE.computeIfAbsent(simpleName, n -> {
            try {
                Class.forName("java.lang." + n, false, IRJavaLangCollision.class.getClassLoader());
                return Boolean.TRUE;
            } catch (ClassNotFoundException | LinkageError | RuntimeException absent) {
                if (absent instanceof SilentDegradation.Refusal refusal) {
                    throw refusal;   // the C0 contract ValidatorScan.collidesWithJavaLang keeps: a refusal is not a probe failure
                }
                return Boolean.FALSE;
            }
        });
    }
}
