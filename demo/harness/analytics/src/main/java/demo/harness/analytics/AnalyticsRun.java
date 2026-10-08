package demo.harness.analytics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assembles the demo contract section 4 {@code analytics} metrics for a lane.
 *
 * <p>Duplicated byte-for-byte (apart from its package line) into the legacy module, so both
 * lanes report the identical shape and the cross-lane {@code results} comparison is a
 * comparison of answers, not of formatting.
 *
 * <h2>peakRssMb is deliberately ABSENT</h2>
 * A JVM cannot observe its own resident set size portably. {@code MemoryPoolMXBean} peaks are
 * HEAP and NON-HEAP usage, which is a different and always-smaller quantity;
 * {@code com.sun.management.OperatingSystemMXBean} exposes committed and free SYSTEM memory,
 * not this process's peak. Publishing either under the name {@code peakRssMb} would be
 * fabricating a field, which demo-contract section 2 forbids. The two peaks that ARE measurable are
 * reported under their true names, {@code peakHeapMb} and {@code peakNonHeapMb}, and the outer
 * PowerShell wrapper supplies the real {@code peakRssMb} from {@code PeakWorkingSet64}.
 */
public final class AnalyticsRun {

    private AnalyticsRun() {
    }

    /**
     * @param lane      {@code plus-jvm} or {@code legacy-emf}
     * @param files     files in the POPULATION (builtins excluded)
     * @param startupMs process start to engine ready; {@code null} to omit
     * @param extra     lane-specific measured facts, appended after the contract keys
     */
    public static Map<String, Object> metrics(String lane, int files, long parseMs,
                                              Queries.Outcome outcome, Long startupMs,
                                              Map<String, Object> extra) {
        Queries.Timings timings = outcome.timings();
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("lane", lane);
        metrics.put("files", files);
        metrics.put("parseMs", parseMs);
        metrics.put("indexMs", timings.indexMs());
        metrics.put("q1Ms", timings.q1Ms());
        metrics.put("q2Ms", timings.q2Ms());
        metrics.put("q3Ms", timings.q3Ms());
        metrics.put("q4Ms", timings.q4Ms());
        if (startupMs != null) {
            metrics.put("startupMs", startupMs);
        }
        metrics.put("results", outcome.results().toResultsMap());

        // Measured extras, beyond the contract's keys.
        metrics.put("peakHeapMb", peakMb(MemoryType.HEAP));
        metrics.put("peakNonHeapMb", peakMb(MemoryType.NON_HEAP));
        metrics.put("indexSize", outcome.indexSize());
        metrics.put("indexCollisions", outcome.indexCollisions());
        metrics.put("extendsCycles", outcome.results().extendsCycles());
        if (extra != null) {
            metrics.putAll(extra);
        }
        metrics.put("exit", 0);
        return metrics;
    }

    /** A one-line human summary of the four answers, for the ordinary log. */
    public static String summary(Queries.Outcome outcome) {
        Queries.Results r = outcome.results();
        return "Q1 types=" + r.types() + " enums=" + r.enums()
                + " functions=" + r.functions() + " rules=" + r.rules()
                + " | Q2 metaAnnotatedTypes=" + r.metaAnnotatedTypes()
                + " | Q3 referencesToTarget=" + r.referencesToTarget()
                + " | Q4 deepestExtendsChain=" + r.deepestExtendsChain();
    }

    /** Peak usage of every pool of the given type, in whole megabytes. */
    private static long peakMb(MemoryType type) {
        long bytes = 0;
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() != type) {
                continue;
            }
            MemoryUsage peak = pool.getPeakUsage();
            if (peak != null) {
                bytes += peak.getUsed();
            }
        }
        return bytes / (1024L * 1024L);
    }
}
