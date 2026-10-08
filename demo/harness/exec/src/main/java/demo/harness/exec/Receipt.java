package demo.harness.exec;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Writes the CONTRACTS section 2 receipt JSON. Receipts are normally assembled by the
 * integrator's wrapper (which alone can measure peak RSS); this runner writes one directly
 * when handed {@code --receipt <path>}.
 *
 * <p>Contract discipline enforced here: <em>never fabricate a field</em>. Only the keys the
 * caller actually measured are written - in particular {@code allocMbPerOp} is present only
 * when the JVM exposed thread allocation counters, and no peak-RSS field is written at all,
 * because this process cannot measure its own peak working set honestly.
 */
public final class Receipt {

    private static final ObjectMapper JSON = new ObjectMapper();

    private Receipt() {}

    public static void write(Path file,
                             String id,
                             String section,
                             String title,
                             String command,
                             Instant startedAt,
                             long durationMs,
                             Map<String, Object> metrics,
                             String notes) throws IOException {
        Map<String, Object> host = new LinkedHashMap<>();
        host.put("os", System.getProperty("os.name", "unknown"));
        host.put("cpu", cpuModel());
        host.put("java", System.getProperty("java.version", "unknown"));

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id);
        r.put("section", section);
        r.put("title", title);
        r.put("command", command);
        r.put("startedAt", startedAt.truncatedTo(ChronoUnit.SECONDS).toString());
        r.put("durationMs", durationMs);
        r.put("host", host);
        r.put("metrics", metrics);
        r.put("notes", notes);

        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(r));
        System.out.println("[receipt] wrote " + file.toAbsolutePath());
    }

    /** Best-effort CPU model. Windows exposes it as an environment variable; else omit-ish. */
    private static String cpuModel() {
        String id = System.getenv("PROCESSOR_IDENTIFIER");
        if (id != null && !id.isBlank()) {
            return id.trim();
        }
        String arch = System.getProperty("os.arch", "unknown");
        return "unknown (" + arch + ")";
    }

    /** UTF-8 helper for the sidecar files the live verb writes. */
    public static void writeText(Path file, String text) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }
}
