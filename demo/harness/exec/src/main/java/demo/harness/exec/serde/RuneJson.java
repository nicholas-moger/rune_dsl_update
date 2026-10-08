package demo.harness.exec.serde;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * The serialiser used by the {@code serialise} workload, and the ONE decision in this lane
 * that had to be made rather than copied. Both possibilities are implemented; which one ran
 * is reported, never assumed.
 *
 * <h2>Mode 1 - {@code rosetta-common} (canonical, off by default)</h2>
 * {@code com.regnosys.rosetta.common.serialisation.RosettaObjectMapper#getNewRosettaObjectMapper()}
 * is the canonical Rune JSON mapper and IS resolvable (verified present in the user's local
 * repository). It is NOT a default dependency of this module, because {@code rosetta-common}
 * also depends on {@code org.finos.rune:rune-runtime} and {@code rune-maven-plugin} at its
 * own pinned version - which would put a THIRD rune-runtime on the classpath beside the
 * leg's, in the same packages, resolved first-wins. That destroys the attribution the two
 * leg profiles exist to provide. The {@code rosetta-common-serde} maven profile adds it with
 * those two excluded for an integrator who wants canonical JSON and accepts that
 * rosetta-common's own bytecode was linked against a different runtime version.
 *
 * <h2>Mode 2 - {@code jackson-rune-annotated} (default)</h2>
 * A plain Jackson mapper from the LEG'S OWN pinned jackson, taught the runtime's annotations
 * by {@link RuneAnnotationIntrospector} so property names come out as the Rune names
 * ({@code @data}, {@code @ref}, {@code @key}) rather than bean names. Zero extra
 * dependencies, so the leg's dependency set stays exactly what the profile pinned - and
 * jackson is itself one of the pinned differences under measurement, which makes this the
 * honest default.
 *
 * <p>Detection is REFLECTIVE, so nothing needs recompiling to switch: if the canonical
 * mapper class is on the classpath it wins, unless {@code -Ddemo.exec.serialiser=jackson}
 * forces the fallback (or {@code =rosetta-common} demands the canonical one and fails loudly
 * when it is absent).
 *
 * <h2>What "round trip" means here</h2>
 * IDENTICAL in both modes and on both legs, so the comparison stays apples to apples:
 * {@code value -> String -> JsonNode}. The write half exercises the whole generated getter
 * surface; the read half exercises the parser. It deliberately stops short of rebuilding the
 * model object, because model-object DESERIALISATION needs builder-aware deserialisers that
 * only the canonical mapper has - making a full round trip available on one mode only, which
 * would make the two legs incomparable the moment the modes differed.
 */
public final class RuneJson {

    public static final String SERIALISER_PROPERTY = "demo.exec.serialiser";
    public static final String CANONICAL_CLASS =
            "com.regnosys.rosetta.common.serialisation.RosettaObjectMapper";
    public static final String CANONICAL_FACTORY = "getNewRosettaObjectMapper";

    /** Which mapper is in use. */
    public enum Mode {
        ROSETTA_COMMON("rosetta-common"),
        JACKSON_RUNE_ANNOTATED("jackson-rune-annotated");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private final ObjectMapper mapper;
    private final Mode mode;
    private final String detail;

    private RuneJson(ObjectMapper mapper, Mode mode, String detail) {
        this.mapper = mapper;
        this.mode = mode;
        this.detail = detail;
    }

    public static RuneJson create() {
        String forced = System.getProperty(SERIALISER_PROPERTY, "auto").trim();
        boolean wantCanonical = "rosetta-common".equalsIgnoreCase(forced) || "auto".equalsIgnoreCase(forced);
        if (wantCanonical) {
            ObjectMapper canonical = tryCanonical();
            if (canonical != null) {
                return new RuneJson(canonical, Mode.ROSETTA_COMMON,
                        CANONICAL_CLASS + "#" + CANONICAL_FACTORY + "()");
            }
            if ("rosetta-common".equalsIgnoreCase(forced)) {
                throw new IllegalStateException("-D" + SERIALISER_PROPERTY
                        + "=rosetta-common but " + CANONICAL_CLASS + " is not on the classpath"
                        + " - rebuild with 'mvn -P <leg>,rosetta-common-serde package'");
            }
        }
        return new RuneJson(fallbackMapper(), Mode.JACKSON_RUNE_ANNOTATED,
                "plain jackson " + jacksonRuntimeLabel()
                        + " + RuneAnnotationIntrospector (@RuneAttribute / @RosettaAttribute names)");
    }

    private static ObjectMapper tryCanonical() {
        try {
            Class<?> c = Class.forName(CANONICAL_CLASS, true, RuneJson.class.getClassLoader());
            Method f = c.getMethod(CANONICAL_FACTORY);
            Object o = f.invoke(null);
            return o instanceof ObjectMapper ? (ObjectMapper) o : null;
        } catch (Throwable t) {
            return null; // absent or unusable: the fallback is the documented default anyway
        }
    }

    private static ObjectMapper fallbackMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setAnnotationIntrospector(AnnotationIntrospector.pair(
                new RuneAnnotationIntrospector(), new JacksonAnnotationIntrospector()));
        // Rune JSON omits absent attributes; NON_EMPTY also drops the empty lists that a
        // sparsely-populated synthetic instance is full of.
        m.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        // A generated interface with no readable property is data, not an error.
        m.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        m.registerModule(scalarModule());
        return m;
    }

    /**
     * Date/time scalars as their ISO {@code toString()} forms. Deliberately hand-rolled
     * rather than pulling {@code jackson-datatype-jsr310}: the leg profiles pin the
     * dependency set exactly, and adding a module to one leg's classpath that the runtime
     * itself does not depend on would blur what is being compared. {@code
     * com.rosetta.model.lib.records.Date.toString()} is already the ISO date.
     */
    private static SimpleModule scalarModule() {
        SimpleModule mod = new SimpleModule("demo-exec-rune-scalars");
        mod.addSerializer(com.rosetta.model.lib.records.Date.class, ToStringSerializer.instance);
        mod.addSerializer(LocalDate.class, ToStringSerializer.instance);
        mod.addSerializer(LocalTime.class, ToStringSerializer.instance);
        mod.addSerializer(LocalDateTime.class, ToStringSerializer.instance);
        mod.addSerializer(ZonedDateTime.class, ToStringSerializer.instance);
        mod.addSerializer(OffsetDateTime.class, ToStringSerializer.instance);
        return mod;
    }

    private static String jacksonRuntimeLabel() {
        try {
            // com.fasterxml.jackson.databind.cfg.PackageVersion is generated per release.
            Class<?> pv = Class.forName("com.fasterxml.jackson.databind.cfg.PackageVersion",
                    true, RuneJson.class.getClassLoader());
            Object version = pv.getField("VERSION").get(null);
            return String.valueOf(version);
        } catch (Throwable t) {
            return "(version unreported)";
        }
    }

    public Mode mode() {
        return mode;
    }

    public String modeLabel() {
        return mode.label();
    }

    public String detail() {
        return detail;
    }

    /** Identical on both modes and both legs - see the class javadoc. */
    public String roundTripLabel() {
        return "value -> String -> JsonNode";
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public String write(Object value) throws Exception {
        return mapper.writeValueAsString(value);
    }

    public String writePretty(Object value) throws Exception {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }

    public JsonNode read(String json) throws Exception {
        return mapper.readTree(json);
    }
}
