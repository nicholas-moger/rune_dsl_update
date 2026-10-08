package demo.harness.exec.corpus;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.records.Date;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Deterministically populates instances of the generated model types via their generated
 * builders - adapted verbatim from {@code rune-benchmarks/.../corpus/ReflectivePopulator.java}
 * (one addition: {@link LocalDateTime}, which the demo closure's zoned/date-time attributes
 * reach). Every single-argument {@code set*}/{@code add*} method whose parameter type the
 * value table can supply gets a deterministic value - fixed scalars, first enum constant,
 * bounded-depth recursion for nested model objects; unsupplied parameter types are skipped
 * and the instance stays sparser.
 *
 * <p>These are SYNTHETIC inputs, not real trade samples, and every receipt says so. Nothing
 * is silently swallowed into a smaller denominator: instance-level failures are counted
 * ({@link #failedCount()}) and per-setter rejections - tolerated by design, the instance just
 * stays sparser - are counted too ({@link #setterRejectionCount()}); both appear in the
 * workload census lines.
 *
 * <p>Determinism matters twice here: the same inputs must be built on BOTH legs (otherwise
 * the legs are not doing the same work), and the same inputs must be built on every repeat
 * run (otherwise a receipt is not reproducible). The method sort below is what buys that -
 * {@code getMethods()} order is JVM-unspecified.
 */
public final class ReflectivePopulator {

    private int populated;
    private int failed;
    private int setterRejections;

    public int populatedCount() {
        return populated;
    }

    public int failedCount() {
        return failed;
    }

    public int setterRejectionCount() {
        return setterRejections;
    }

    /** Build + populate an instance of {@code type} (a generated model interface). */
    public Object populate(Class<?> type, int depth) {
        try {
            Method builderM = type.getMethod("builder");
            Object builder = builderM.invoke(null);
            fill(builder, depth);
            Object built = builder.getClass().getMethod("build").invoke(builder);
            populated++;
            return built;
        } catch (Throwable t) {
            failed++;
            return null;
        }
    }

    private void fill(Object builder, int depth) {
        Method[] methods = builder.getClass().getMethods();
        // name + first-param-type tie-break: getMethods() order is JVM-unspecified, and
        // same-named overloads must not swap between runs for the census to be stable
        Arrays.sort(methods, Comparator.comparing(Method::getName)
                .thenComparing(m -> m.getParameterCount() == 1
                        ? m.getParameterTypes()[0].getName() : ""));
        for (Method m : methods) {
            String n = m.getName();
            if (!(n.startsWith("set") || n.startsWith("add"))) {
                continue;
            }
            if (m.getParameterCount() != 1 || Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            Object v = valueFor(m.getParameterTypes()[0], depth);
            if (v == null) {
                continue;
            }
            try {
                m.invoke(builder, v);
            } catch (Throwable t) {
                // a setter rejecting the synthetic value is fine; the instance stays sparser
                setterRejections++;
            }
        }
    }

    /** The deterministic value table; null = "cannot supply this type". */
    public Object valueFor(Class<?> p, int depth) {
        if (p == String.class) {
            return "DEMO1";
        }
        if (p == Integer.class || p == int.class) {
            return 1;
        }
        if (p == Long.class || p == long.class) {
            return 1L;
        }
        if (p == Boolean.class || p == boolean.class) {
            return Boolean.TRUE;
        }
        if (p == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (p == BigInteger.class) {
            return BigInteger.ONE;
        }
        if (p == Date.class) {
            return Date.of(2026, 1, 15);
        }
        if (p == LocalDate.class) {
            return LocalDate.of(2026, 1, 15);
        }
        if (p == LocalTime.class) {
            return LocalTime.NOON;
        }
        if (p == LocalDateTime.class) {
            return LocalDateTime.of(2026, 1, 15, 12, 0, 0, 0);
        }
        if (p == ZonedDateTime.class) {
            return ZonedDateTime.of(2026, 1, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        }
        if (p.isEnum()) {
            Object[] constants = p.getEnumConstants();
            return constants.length > 0 ? constants[0] : null;
        }
        if (RosettaModelObject.class.isAssignableFrom(p) && depth > 0) {
            return populate(p, depth - 1);
        }
        if (p == List.class) {
            return null; // add* single-element overloads carry the element type
        }
        return null;
    }
}
