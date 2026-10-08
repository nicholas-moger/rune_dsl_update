package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.ir.expr.anf.Bind;
import com.regnosys.rosetta.ir.expr.anf.Block;
import com.regnosys.rosetta.ir.expr.anf.JoinPoint;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RType;
import com.rosetta.util.types.JavaClass;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Replays the maintainer generator's hoist <strong>skeleton</strong> from a normalized ANF binding list —
 * the Java-target lowering of {@link Bind}s into the statement-hoist locals (design §5; the
 * {@code ir.expr.anf} ANF→Java step). It is the make-or-break byte surface of the hoisting tier: the
 * <em>per-scope render-walk temp-numbering replay</em> and the declare-then-assign decl shape.
 *
 * <h2>Phase-A scope: the SKELETON, not the body bytes</h2>
 * The branch interiors (the condition and the assigned value) depend on render-scope state (lambda-var
 * disambiguation, the {@code Mapper} chain) that the live "C" generator supplies, so this renderer emits
 * them as the placeholders {@link #MASK_COND}/{@link #MASK_THEN} and validates only what is reproducible
 * offline: the temp <em>base name</em>, its per-scope <em>number</em>, the <em>declared type</em>, the
 * {@code = null} init and the {@code if(<cond>){ tmp = <then>; }} shape (no {@code else} ⇒ stays
 * {@code null}). The full body bytes are the live-gate ("C") obligation. The same renderer is reused by
 * "C" once the bodies lower — there it reads {@code cond}/{@code then} off the {@link JoinPoint}'s
 * (then-non-null) {@code branching} instead of masking them.
 *
 * <h2>The temp-numbering replay (the parity linchpin)</h2>
 * Bindings are numbered by <strong>per-base-name group, in registration (list) order</strong> — exactly
 * the maintainer's {@code StatementHoistSession} (design §5 / §2.6 Q3; Q3 dump §1/§7). Each base group is
 * numbered {@code base, base0, base1, …} by registration index; a <strong>single</strong> occurrence stays
 * <strong>bare</strong> (design §5:318 — {@code ifThenElseResult}, NOT {@code ifThenElseResult0}; the dump's
 * {@code ifThenElseResult0} parenthetical is a transcription typo, ground-truthed against the {@code thenArg}
 * single-member carrier, dump §5.1). A base that is a Java keyword takes a leading {@code _} escape ONLY when
 * single ({@code _boolean}); a multi-member keyword group is unescaped ({@code boolean0}, {@code boolean1}).
 * The binding LIST order IS the render-walk order — the caller registers hoists in render-walk order, which
 * deliberately reorders versus source pre-order (switch default last; a {@code boolean} decl before the
 * {@code ifThenElseResult} it guards — later slices).
 *
 * <p><strong>This renderer is byte-inert</strong> — it is invoked by no {@code visit*} override and is on no
 * live emission path; only the Phase-A offline harness (and, later, "C") calls it (decision-log L-056).
 *
 * <p>Lab-authored Phase-2; not present upstream.
 */
public final class AnfSkeletonRenderer {

    /** Placeholder for a masked branch <em>condition</em> (its body bytes are the live-gate obligation). */
    public static final String MASK_COND = "__COND__";
    /** Placeholder for a masked branch <em>then-value</em> (its body bytes are the live-gate obligation). */
    public static final String MASK_THEN = "__THEN__";
    /**
     * Placeholder for a masked fluent-<em>initializer</em> value — the RHS of a {@code final <T> name = <value>;}
     * hoist. Family-agnostic: a slice-2 {@code boolean} condition ({@code final Boolean _boolean = <call>;}) and a
     * slice-3 {@code thenArg} ({@code final MapperC<…> thenArg = <pre-then chain>;}) are both masked initializers,
     * since the RHS bytes are render-scope-dependent (a "C" obligation); only the decl name/number/type/shape is
     * validated offline.
     */
    public static final String MASK_VALUE = "__VALUE__";
    /**
     * Placeholder for a hoist local's <em>declared type</em> when it is not resolvable offline. At the
     * #219 pin the dominant SET-conditional carriers have alias-receiver then-branches whose type is
     * L-032-{@code MISSING} via {@code getInferredType} (the live generator derives the decl type via the
     * {@code NavigationHandler} alias oracle, which the offline harness does not wire) — so the decl-type
     * byte cross-check is deferred to "C" alongside the branch bodies. The temp name/number/order and the
     * declare-then-assign shape ARE validated offline.
     */
    public static final String MASK_TYPE = "__TYPE__";

    private final JavaTypeTranslator translator;

    public AnfSkeletonRenderer(JavaTypeTranslator translator) {
        this.translator = translator;
    }

    /**
     * Render the hoist skeleton for a method scope's bindings. {@code hoists} and {@code declTypes} are
     * positionally aligned (binding {@code i}'s declared local type is {@code declTypes.get(i)}) — the
     * declared type travels as an out-of-band fact in Phase A because the branch interiors are not lowered
     * (the conditional carrier's alias-receiver branches are L-032-{@code MISSING}); at "C" it is read off
     * the lowered then-branch atom instead.
     *
     * @return the concatenated hoist skeletons, in registration order, each a {@code <Type> name = null;
     *         if (__COND__) { name = __THEN__; }} block
     * <p>A declared type that does not resolve offline ({@link RMetaAnnotatedType#MISSING}, the L-032
     * alias-receiver case) renders as {@link #MASK_TYPE} — the decl-type byte cross-check is the "C"
     * obligation; the temp name/number/order and the declare-then-assign shape are validated regardless.
     *
     * <p>A binding's value selects the render form: a {@link JoinPoint} → the declare-then-assign
     * {@code <T> name = null; if(__COND__){…}} (a conditional/switch result); a {@link Block} → the fluent
     * initializer {@code final <T> name = __VALUE__;} (a hoisted bare-fn-call {@code boolean} condition, slice 2,
     * or a {@code thenArg}, slice 3 — both masked initializers).
     *
     * @throws IllegalArgumentException if the lists differ in length, or a binding's value is an unhandled form
     */
    public String renderHoists(List<Bind> hoists, List<RMetaAnnotatedType> declTypes) {
        if (hoists.size() != declTypes.size()) {
            throw new IllegalArgumentException(
                    "hoists/declTypes length mismatch: " + hoists.size() + " vs " + declTypes.size());
        }
        Map<String, Integer> groupSize = new LinkedHashMap<>();
        for (Bind b : hoists) {
            groupSize.merge(b.name().base(), 1, Integer::sum);
        }
        Map<String, Integer> nextIndex = new LinkedHashMap<>();

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < hoists.size(); i++) {
            Bind bind = hoists.get(i);
            String base = bind.name().base();
            boolean multiMember = groupSize.get(base) > 1;
            int index = nextIndex.merge(base, 1, Integer::sum) - 1;
            String name = resolveName(base, multiMember, index);
            String javaType = javaTypeName(declTypes.get(i));

            if (bind.value() instanceof JoinPoint) {
                // statement-position conditional/switch result → declare-then-assign (no else → stays null)
                out.append(javaType).append(' ').append(name).append(" = null;\n");
                out.append("if (").append(MASK_COND).append(") {\n");
                out.append("    ").append(name).append(" = ").append(MASK_THEN).append(";\n");
                out.append("}\n");
            } else if (bind.value() instanceof Block) {
                // a fluent-initializer hoist → `final <T> name = <value>;` (a `boolean` condition, slice 2, or a
                // `thenArg`, slice 3 — both masked initializers; the family differs only in the base lexeme)
                out.append("final ").append(javaType).append(' ').append(name)
                        .append(" = ").append(MASK_VALUE).append(";\n");
            } else {
                throw new IllegalArgumentException(
                        "unhandled Bind value form: " + bind.value().getClass().getSimpleName()
                                + " for " + base);
            }
        }
        return out.toString();
    }

    /**
     * The per-scope render-walk name of a hoisted temporary. A single-member group stays bare (keyword →
     * {@code _}-escaped); a multi-member group is numbered from {@code 0} (a keyword base stays unescaped
     * once numbered). See the class docs for the ratified rule (design §5:318).
     */
    static String resolveName(String base, boolean multiMember, int index) {
        if (multiMember) {
            return base + index;
        }
        return isJavaKeyword(base) ? "_" + base : base;
    }

    private String javaTypeName(RMetaAnnotatedType declType) {
        if (declType == null || declType.isMissing()) {
            return MASK_TYPE; // L-032 alias-receiver: decl type is not resolvable offline (a "C" cross-check)
        }
        RType rType = declType.type();
        JavaClass<?> javaClass = rType == null ? null : translator.toJavaReferenceType(rType);
        return javaClass == null ? MASK_TYPE : javaClass.getSimpleName();
    }

    private static final Set<String> JAVA_KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "true", "false", "null");

    /** Whether {@code base} is a Java reserved word (needs a leading {@code _} escape as a single temp). */
    static boolean isJavaKeyword(String base) {
        return JAVA_KEYWORDS.contains(base);
    }
}
