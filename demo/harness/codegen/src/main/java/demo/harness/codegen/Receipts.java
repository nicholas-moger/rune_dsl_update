package demo.harness.codegen;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes a CONTRACTS.md section-2 receipt JSON file.
 *
 * <p>Receipts are normally assembled by the integrator's wrapper, which is the only thing that
 * can measure {@code peakRssMb}. A runner writes one directly only when handed
 * {@code --receipt <path>}, and then it records ONLY what it measured -- absent facts are
 * omitted, never invented.
 *
 * <p>{@code command} must equal what {@code demo/runs.json} holds (CONTRACTS section-8). The
 * wrapper should therefore pass the exact line with {@code --command "<line>"}; without it the
 * receipt records this process's own argv, which is honest but may differ in quoting from the
 * canonical form.
 */
public final class Receipts {

    private Receipts() {
    }

    /**
     * @param durationMs the runner's own measured wall time; the wrapper's receipt may carry a
     *                   larger number because it also sees process spawn and teardown
     * @param notes      honest caveats; must not be empty
     */
    public static void write(Path target, String id, String section, String title,
                             String command, long durationMs, Map<String, Object> metrics,
                             String notes) throws IOException {
        Map<String, Object> host = new LinkedHashMap<>();
        host.put("os", (System.getProperty("os.name", "") + " "
                + System.getProperty("os.version", "")).trim());
        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu != null && !cpu.isBlank()) {
            // Omitted rather than guessed on platforms that do not export it.
            host.put("cpu", cpu.trim());
        }
        host.put("java", System.getProperty("java.version", "unknown"));

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("id", id);
        receipt.put("section", section);
        receipt.put("title", title);
        receipt.put("command", command);
        receipt.put("startedAt", startedAtIso());
        receipt.put("durationMs", durationMs);
        receipt.put("host", host);
        receipt.put("metrics", metrics);
        receipt.put("notes", notes);

        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, DemoOut.jsonPretty(receipt).getBytes(StandardCharsets.UTF_8));
        DemoOut.log("[receipt] wrote " + target.toAbsolutePath());
    }

    /** This JVM's start time as an ISO-8601 UTC instant with second precision. */
    public static String startedAtIso() {
        long startMillis = ManagementFactory.getRuntimeMXBean().getStartTime();
        return Instant.ofEpochMilli(startMillis).truncatedTo(ChronoUnit.SECONDS).toString();
    }

    /**
     * The reproducible command line: {@code --command} verbatim when the wrapper supplied it,
     * otherwise this process's argv re-quoted (tokens containing whitespace get double quotes).
     */
    public static String commandLine(String verb, Args args) {
        String explicit = args.get("command", null);
        if (explicit != null && !explicit.isBlank()) {
            return explicit;
        }
        List<String> parts = new ArrayList<>();
        List<String> raw = args.raw();
        if (raw.isEmpty()) {
            parts.add(verb);
        } else {
            for (String token : raw) {
                parts.add(token.chars().anyMatch(Character::isWhitespace)
                        ? "\"" + token + "\"" : token);
            }
        }
        return String.join(" ", parts);
    }
}
