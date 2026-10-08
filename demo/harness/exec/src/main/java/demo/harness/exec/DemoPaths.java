package demo.harness.exec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Every path the exec lane touches, resolved from ONE anchor: the {@code demo/} directory,
 * found by walking up from the working directory until a {@code demo/CONTRACTS.md} appears
 * (override with {@code -Ddemo.root=<dir>}). Nothing outside {@code demo/} is read and
 * nothing outside {@code demo/work/} is written - CONTRACTS section 0 rules 2 and 3.
 */
public final class DemoPaths {

    public static final String ROOT_PROPERTY = "demo.root";
    public static final String GEN_PROPERTY = "demo.gen";
    public static final String CLASSES_PROPERTY = "demo.classes";

    private static volatile Path cachedDemoRoot;

    private DemoPaths() {}

    /** The {@code demo/} directory; resolved once and cached (every other path calls this). */
    public static Path demoRoot() {
        Path cached = cachedDemoRoot;
        if (cached != null) {
            return cached;
        }
        Path resolved = resolveDemoRoot();
        cachedDemoRoot = resolved;
        return resolved;
    }

    private static Path resolveDemoRoot() {
        String override = System.getProperty(ROOT_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        Path start = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (Path c = start; c != null; c = c.getParent()) {
            if (Files.isRegularFile(c.resolve("demo").resolve("CONTRACTS.md"))) {
                return c.resolve("demo");
            }
            if (Files.isRegularFile(c.resolve("CONTRACTS.md"))
                    && "demo".equals(String.valueOf(c.getFileName()))) {
                return c;
            }
        }
        throw new IllegalStateException("cannot locate demo/ from " + start
                + " - pass -D" + ROOT_PROPERTY + "=<path to demo>");
    }

    /** Read-only merged corpus roots (CONTRACTS section 1). */
    public static Path corpusRoot() {
        return demoRoot().resolve("corpus");
    }

    /** All derived output lives here; gitignored. */
    public static Path workRoot() {
        return demoRoot().resolve("work");
    }

    /**
     * The fork-generated Java tree the integrator produces before this lane runs
     * (mode m1, {@code demo/work/gen-m1/}); override with {@code -Ddemo.gen=<dir>} or
     * {@code --src}.
     */
    public static Path genRoot() {
        String override = System.getProperty(GEN_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        return workRoot().resolve("gen-m1");
    }

    /**
     * Compiled classes for one leg. EVERY caller - {@code setup} writing them, {@code run},
     * {@code live} and the JMH benchmarks reading them - resolves through this ONE function,
     * so an override cannot move the write without moving the read. Point it elsewhere with
     * {@code -Ddemo.classes=<root>} (the leg name is appended to that root).
     */
    public static Path classesRoot(String leg) {
        String override = System.getProperty(CLASSES_PROPERTY);
        if (override != null && !override.isEmpty()) {
            return Paths.get(override).toAbsolutePath().normalize().resolve(leg);
        }
        return workRoot().resolve("exec-classes").resolve(leg);
    }

    /** Where {@code live} drops one real serialised object for the dashboard. */
    public static Path liveSample() {
        return workRoot().resolve("live-sample.json");
    }
}
