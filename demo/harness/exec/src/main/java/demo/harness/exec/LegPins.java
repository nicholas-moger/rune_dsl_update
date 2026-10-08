package demo.harness.exec;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * The leg's dependency pins, read from {@code /demo-exec-leg.properties} - a resource
 * written by maven resource filtering from whichever leg profile was active when the jar
 * was built (see pom.xml). Nothing here is hard-coded in Java: a jar therefore always
 * reports the dependency set it was ACTUALLY built with, and cannot drift from the pom.
 *
 * <p>Two consequences the harness leans on:
 * <ul>
 *   <li>{@link #requireLeg(String)} refuses to run a {@code --leg plus-rt} command from a
 *       jar built with the legacy-rt profile (and vice versa). Mislabelling a measurement
 *       is the one failure mode that would silently invalidate the whole comparison.</li>
 *   <li>{@link #notes()} produces the receipt {@code notes} sentence naming every pin, so
 *       "which jackson was that?" is answerable from the receipt alone.</li>
 * </ul>
 */
public final class LegPins {

    public static final String RESOURCE = "/demo-exec-leg.properties";

    public static final String LEG;
    public static final String RUNTIME_GAV;
    public static final String RUNTIME_RESOLVED;
    public static final String JACKSON_VERSION;
    public static final String COMMONS_LANG3_VERSION;
    public static final String GUICE_VERSION;
    public static final String XTEND_PRESENT;

    static {
        Properties p = new Properties();
        try (InputStream in = LegPins.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("missing classpath resource " + RESOURCE
                        + " - this jar was not built by demo/harness/exec/pom.xml");
            }
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                p.load(r);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        LEG = p.getProperty("leg", "UNSET").trim();
        RUNTIME_GAV = p.getProperty("runtime.gav", "UNSET").trim();
        RUNTIME_RESOLVED = p.getProperty("runtime.resolved", "UNSET").trim();
        JACKSON_VERSION = p.getProperty("jackson.version", "UNSET").trim();
        COMMONS_LANG3_VERSION = p.getProperty("commonsLang3.version", "UNSET").trim();
        GUICE_VERSION = p.getProperty("guice.version", "UNSET").trim();
        XTEND_PRESENT = p.getProperty("xtend.present", "UNSET").trim();
    }

    private LegPins() {}

    /** The two legs this harness knows about. */
    public static boolean isKnownLeg(String leg) {
        return "legacy-rt".equals(leg) || "plus-rt".equals(leg);
    }

    /**
     * Fail fast unless {@code requested} is the leg this jar was built for. A build with no
     * {@code -P} active leaves the pins UNSET and is rejected here rather than producing an
     * unattributable measurement.
     */
    public static void requireLeg(String requested) {
        if ("UNSET".equals(LEG)) {
            throw new IllegalStateException("this jar was built with NO leg profile active - "
                    + "rebuild with 'mvn -P legacy-rt package' or 'mvn -P plus-rt package'");
        }
        if (!isKnownLeg(requested)) {
            throw new IllegalArgumentException("unknown --leg '" + requested
                    + "' (expected legacy-rt or plus-rt)");
        }
        if (!LEG.equals(requested)) {
            throw new IllegalStateException("--leg " + requested + " but this jar was built for leg "
                    + LEG + " (runtime " + RUNTIME_GAV + ") - run the " + requested
                    + " jar instead; a mislabelled measurement is worse than no measurement");
        }
    }

    /** One-line pin sentence for receipt {@code notes} and the run banner. */
    public static String notes() {
        return "leg=" + LEG
                + "; runtime=" + RUNTIME_GAV
                + (RUNTIME_GAV.equals(RUNTIME_RESOLVED) ? "" : " (resolves " + RUNTIME_RESOLVED + ")")
                + "; jackson=" + JACKSON_VERSION
                + "; commons-lang3=" + COMMONS_LANG3_VERSION
                + "; guice=" + GUICE_VERSION + " (held equal on both legs)"
                + "; xtend.lib on classpath=" + XTEND_PRESENT
                + "; all pins are DIRECT dependencies of the leg profile and are also held in its"
                + " dependencyManagement, so no transitive path can move them.";
    }
}
