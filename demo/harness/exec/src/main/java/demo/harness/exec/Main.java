package demo.harness.exec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rosetta.model.lib.RosettaModelObject;
import demo.harness.exec.corpus.CorpusClasses;
import demo.harness.exec.corpus.GenTree;
import demo.harness.exec.corpus.ModelClosure;
import demo.harness.exec.workload.SerialiseWorkload;
import demo.harness.exec.workload.Workload;
import demo.harness.exec.workload.Workloads;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The execution lane's CLI. Three verbs, all leg-scoped:
 *
 * <pre>
 *   setup --leg legacy-rt|plus-rt [--src DIR] [--exclude TOKEN] [--force]
 *   run   --leg LEG --workload populate|serialise|validate|report|projection
 *         --ops N --warmup N [--limit N] [--depth N]
 *   live  --leg LEG [--ops N] [--limit N] [--depth N]
 * </pre>
 *
 * All three accept {@code --receipt FILE} (write the CONTRACTS section 2 receipt directly)
 * and {@code --command "..."} (the exact reproducible command line the receipt should record;
 * demo/runs.json is the single source for that, so the wrapper passes it in rather than the
 * runner guessing).
 *
 * <p>There is deliberately no {@code --out}: the classes directory resolves through
 * {@link DemoPaths#classesRoot(String)} for WRITER and READER alike, so setup cannot put
 * classes somewhere run and live never look. Move it with {@code -Ddemo.classes=<root>},
 * which both halves consult.
 *
 * <p>The leg is not a label: {@link LegPins#requireLeg(String)} refuses to run when the
 * requested leg is not the one this jar was built for, and {@link ModelClosure#open(String)}
 * refuses to execute classes whose compile marker records a different dependency set. A
 * mislabelled measurement is the one failure this harness will not tolerate.
 */
public final class Main {

    private static final String SECTION = "execution";

    private Main() {}

    public static void main(String[] args) {
        int rc;
        try {
            rc = dispatch(args);
        } catch (Throwable t) {
            Demo.error(t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage()));
            t.printStackTrace();
            rc = 1;
        }
        System.exit(rc);
    }

    private static int dispatch(String[] args) throws Exception {
        if (args.length == 0 || "help".equals(args[0]) || "--help".equals(args[0])) {
            usage();
            return args.length == 0 ? 2 : 0;
        }
        String verb = args[0];
        if (!"setup".equals(verb) && !"run".equals(verb) && !"live".equals(verb)) {
            System.err.println("unknown verb '" + verb + "'");
            usage();
            return 2;
        }
        Args a = Args.parse(args);
        switch (verb) {
            case "setup":
                return setup(a);
            case "run":
                return run(a);
            default:
                return live(a);
        }
    }

    private static void usage() {
        System.out.println("demo-harness-exec (leg " + LegPins.LEG + ")");
        System.out.println("  " + LegPins.notes());
        System.out.println();
        System.out.println("  setup --leg legacy-rt|plus-rt [--src DIR] [--exclude TOKEN]");
        System.out.println("        [--force] [--receipt FILE] [--command STR]");
        System.out.println("  run   --leg LEG --workload " + String.join("|", Workloads.NAMES));
        System.out.println("        --ops N --warmup N [--limit N] [--depth N]");
        System.out.println("        [--receipt FILE] [--command STR]");
        System.out.println("  live  --leg LEG [--ops N] [--limit N] [--depth N]");
        System.out.println("        [--receipt FILE] [--command STR]");
        System.out.println();
        System.out.println("  paths: -Ddemo.root=DIR   the demo/ directory (else found by walking up)");
        System.out.println("         -Ddemo.gen=DIR    generated java tree (default demo/work/gen-m1)");
        System.out.println("         -Ddemo.classes=D  classes root (default demo/work/exec-classes),");
        System.out.println("                           leg appended; used by setup AND run/live alike");
        System.out.println("         -Ddemo.exec.serialiser=auto|jackson|rosetta-common");
        System.out.println();
        System.out.println("  JMH:  java -cp <this jar> org.openjdk.jmh.Main "
                + "demo.harness.exec.jmh.*");
    }

    // ------------------------------------------------------------------ setup

    private static int setup(Args a) throws Exception {
        String leg = a.required("leg");
        LegPins.requireLeg(leg);
        Path src = a.path("src", DemoPaths.genRoot());
        Path out = DemoPaths.classesRoot(leg);
        String exclude = a.get("exclude");
        boolean force = a.flag("force");

        String id = "execution.setup." + leg;
        Instant started = Instant.now();
        long t0 = System.nanoTime();
        Demo.start(id, "compiling " + src + " for leg " + leg);
        System.out.println("[setup] " + LegPins.notes());

        CorpusClasses.CompileResult r = GenTree.compileFor(leg, src, out, exclude, force);
        long durationMs = (System.nanoTime() - t0) / 1_000_000;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("leg", leg);
        metrics.put("workload", "setup");
        metrics.put("sources", r.sources());
        metrics.put("excluded", r.excluded());
        metrics.put("treeStamp", r.stamp());
        metrics.put("reused", r.reused());
        metrics.put("compileMs", r.compileMs());
        metrics.put("classesDir", r.classesDir().toString().replace('\\', '/'));
        metrics.put("sourceTree", src.toString().replace('\\', '/'));
        metrics.put("errors", 0);
        Demo.metric("sources", r.sources());
        Demo.metric("treeStamp", r.stamp());
        Demo.metric("compileMs", r.compileMs());
        Demo.done(metrics);

        a.maybeReceipt(id, "Compile generated closure - " + leg, started, durationMs, metrics,
                "setup is a PREPARATION step, not one of the five section-4 execution "
                        + "workloads: it javac-compiles demo/work/gen-m1 against this leg's "
                        + "runtime so the measured runs never pay compile cost. "
                        + LegPins.notes());
        return 0;
    }

    // -------------------------------------------------------------------- run

    private static int run(Args a) throws Exception {
        String leg = a.required("leg");
        String workload = a.required("workload");
        if (!Workloads.NAMES.contains(workload)) {
            throw new IllegalArgumentException("unknown --workload '" + workload
                    + "' (expected one of " + Workloads.NAMES + ")");
        }
        int ops = a.positive("ops", 20);
        int warmup = a.nonNegative("warmup", 5);
        int limit = a.nonNegative("limit", 0);
        int depth = a.nonNegative("depth", 2);

        String id = SECTION + "." + workload + "." + leg;
        Instant started = Instant.now();
        long t0 = System.nanoTime();
        Demo.start(id, "leg=" + leg + " workload=" + workload + " ops=" + ops
                + " warmup=" + warmup);
        System.out.println("[run] " + LegPins.notes());

        try (ModelClosure closure = ModelClosure.open(leg);
             Workload w = Workloads.create(workload, closure, limit, depth)) {

            System.out.println("[run] closure " + closure.describe());
            Map<String, Object> census = w.setUp();
            for (Map.Entry<String, Object> e : census.entrySet()) {
                Demo.metric("census." + e.getKey(), e.getValue());
            }
            Demo.metric("unitsPerOp", w.unitsPerOp());

            long total = warmup + (long) ops;
            long checksum = 0L;
            int errors = 0;
            int warmupErrors = 0;
            String firstError = null;

            for (int i = 0; i < warmup; i++) {
                try {
                    checksum += w.runOnce();
                } catch (Throwable t) {
                    // counted APART from measured-loop errors: section 4's "errors" is about
                    // the measurement, and folding warmup into it would misreport the run
                    warmupErrors++;
                    if (firstError == null) {
                        firstError = t.getClass().getSimpleName() + ": " + t.getMessage();
                    }
                }
                Demo.progress(i + 1L, total);
            }

            Timing timing = new Timing(ops);
            for (int i = 0; i < ops; i++) {
                timing.beginOp();
                try {
                    checksum += w.runOnce();
                    timing.endOp();
                } catch (Throwable t) {
                    // a failed op is not a timing sample: recording it would report the cost
                    // of the failure path as the cost of the workload
                    errors++;
                    if (firstError == null) {
                        firstError = t.getClass().getSimpleName() + ": " + t.getMessage();
                    }
                }
                Demo.progress(warmup + i + 1L, total);
            }
            long durationMs = (System.nanoTime() - t0) / 1_000_000;

            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("leg", leg);
            metrics.put("workload", workload);
            // ops = the number of TIMING SAMPLES mean/p95 are over; an op that threw is not a
            // sample, so requestedOps and errors together account for every attempt
            metrics.put("ops", timing.ops());
            metrics.put("requestedOps", ops);
            metrics.put("warmupOps", warmup);
            metrics.put("meanMs", timing.meanMs());
            metrics.put("p95Ms", timing.p95Ms());
            if (timing.allocMbPerOp() >= 0d) {
                metrics.put("allocMbPerOp", timing.allocMbPerOp());
            }
            metrics.put("errors", errors);
            metrics.put("warmupErrors", warmupErrors);
            if (firstError != null) {
                metrics.put("firstError", firstError);
            }
            metrics.put("minMs", timing.minMs());
            metrics.put("maxMs", timing.maxMs());
            metrics.put("unitsPerOp", w.unitsPerOp());
            metrics.put("unitLabel", w.unitLabel());
            metrics.put("treeStamp", closure.stamp());
            metrics.put("checksum", checksum);
            metrics.put("census", census);
            Demo.done(metrics);

            System.out.printf("[run] %s/%s ops=%d mean=%.4f ms p95=%.4f ms errors=%d "
                            + "warmupErrors=%d units/op=%d (%s)%n", leg, workload, timing.ops(),
                    timing.meanMs(), timing.p95Ms(), errors, warmupErrors, w.unitsPerOp(),
                    w.unitLabel());

            a.maybeReceipt(id, workloadTitle(workload) + " - " + leg, started, durationMs,
                    metrics, runNotes(w, closure));
            return errors > 0 && timing.ops() == 0 ? 1 : 0;
        }
    }

    private static String workloadTitle(String workload) {
        switch (workload) {
            case "populate":
                return "Builder population sweep";
            case "serialise":
                return "JSON round trip";
            case "validate":
                return "Validator sweep";
            case "report":
                return "DRR report evaluation";
            case "projection":
                return "ISO 20022 projection evaluation";
            default:
                return workload;
        }
    }

    private static String runNotes(Workload w, ModelClosure closure) {
        StringBuilder sb = new StringBuilder();
        sb.append(LegPins.notes());
        sb.append(" Inputs are SYNTHETIC (deterministic builder population), not real trade")
                .append(" samples, and are built outside the measured loop. One op = ")
                .append(w.unitsPerOp()).append(' ').append(w.unitLabel())
                .append(". Wall-clock loop timings from a single JVM, warm but not")
                .append(" JMH-controlled - the demo.harness.exec.jmh benchmarks are the")
                .append(" statistically defensible half. Generated tree stamp ")
                .append(closure.stamp()).append('.');
        if (!Timing.allocAvailable()) {
            sb.append(" allocMbPerOp omitted: this JVM does not expose per-thread allocation")
                    .append(" counters.");
        }
        if (w instanceof SerialiseWorkload s) {
            sb.append(" Serialiser: ").append(s.serialiserMode()).append(" (")
                    .append(s.json().detail()).append("); round trip is ")
                    .append(s.json().roundTripLabel())
                    .append(" - identical on both legs so the comparison is like for like.");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------- live

    private static int live(Args a) throws Exception {
        String leg = a.required("leg");
        int ops = a.positive("ops", 3);
        int limit = a.nonNegative("limit", 25);
        int depth = a.nonNegative("depth", 2);

        String id = SECTION + ".live." + leg;
        Instant started = Instant.now();
        long t0 = System.nanoTime();
        Demo.start(id, "leg=" + leg + " one pass of each workload, ops=" + ops
                + " limit=" + limit);
        System.out.println("[live] " + LegPins.notes());

        List<String> steps = Workloads.NAMES;
        Map<String, Object> perStep = new LinkedHashMap<>();
        Map<String, Object> sample = null;
        int errors = 0;

        try (ModelClosure closure = ModelClosure.open(leg)) {
            System.out.println("[live] closure " + closure.describe());
            for (int s = 0; s < steps.size(); s++) {
                String step = steps.get(s);
                Map<String, Object> stepMetrics = new LinkedHashMap<>();
                try (Workload w = Workloads.create(step, closure, limit, depth)) {
                    Map<String, Object> census = w.setUp();
                    if (w instanceof SerialiseWorkload sw && sample == null) {
                        sample = writeLiveSample(leg, closure, sw);
                    }
                    Timing timing = new Timing(ops);
                    long checksum = 0L;
                    for (int i = 0; i < ops; i++) {
                        timing.beginOp();
                        try {
                            checksum += w.runOnce();
                            timing.endOp();
                        } catch (Throwable t) {
                            errors++;
                        }
                    }
                    stepMetrics.put("ops", timing.ops());
                    stepMetrics.put("meanMs", timing.meanMs());
                    stepMetrics.put("p95Ms", timing.p95Ms());
                    stepMetrics.put("unitsPerOp", w.unitsPerOp());
                    stepMetrics.put("unitLabel", w.unitLabel());
                    stepMetrics.put("checksum", checksum);
                    stepMetrics.put("census", census);
                    Demo.metric("live." + step + ".meanMs", timing.meanMs());
                    Demo.metric("live." + step + ".unitsPerOp", w.unitsPerOp());
                    System.out.printf("[live] %-11s mean=%.4f ms units/op=%d (%s)%n", step,
                            timing.meanMs(), w.unitsPerOp(), w.unitLabel());
                } catch (Throwable t) {
                    // one broken step must not sink the whole live pass: the dashboard shows
                    // the steps that worked and the reason the others did not
                    errors++;
                    stepMetrics.put("error", t.getClass().getSimpleName() + ": " + t.getMessage());
                    Demo.metric("live." + step + ".error", String.valueOf(t.getMessage()));
                    System.out.println("[live] " + step + " FAILED: " + t);
                }
                perStep.put(step, stepMetrics);
                Demo.progress(s + 1L, steps.size());
            }

            long durationMs = (System.nanoTime() - t0) / 1_000_000;
            Map<String, Object> metrics = new LinkedHashMap<>();
            metrics.put("leg", leg);
            metrics.put("workload", "live");
            metrics.put("ops", ops);
            metrics.put("warmupOps", 0);
            metrics.put("errors", errors);
            metrics.put("treeStamp", closure.stamp());
            metrics.put("steps", perStep);
            if (sample != null) {
                metrics.put("sample", sample);
            }
            Demo.done(metrics);

            a.maybeReceipt(id, "Live sample flow - " + leg, started, durationMs, metrics,
                    "One pass of each workload at small N for the dashboard's live flow "
                            + "(create -> serialise -> validate -> report -> projection). NOT a "
                            + "benchmark: no warmup, tiny op counts. One real serialised object "
                            + "is written to demo/work/live-sample.json. " + LegPins.notes());
            return 0;
        }
    }

    /**
     * Write one REAL serialised object for the site to display, with its provenance beside
     * it. Chooses the first instance whose pretty JSON is big enough to be interesting and
     * small enough to render; if none qualifies, the largest that still fits.
     */
    private static Map<String, Object> writeLiveSample(String leg, ModelClosure closure,
                                                       SerialiseWorkload sw) throws Exception {
        final int minChars = 200;
        final int maxChars = 20000;
        Object chosen = null;
        String chosenJson = null;
        for (Object o : sw.instances()) {
            String pretty;
            try {
                pretty = sw.json().writePretty(o);
            } catch (Throwable t) {
                continue;
            }
            if (pretty.length() >= minChars && pretty.length() <= maxChars) {
                chosen = o;
                chosenJson = pretty;
                break;
            }
            // fallback = the largest that STILL FITS. The size test must gate the
            // first candidate too, or an oversized instance #1 is taken unconditionally
            // and the site is handed a document it cannot render.
            if (pretty.length() <= maxChars
                    && (chosenJson == null || pretty.length() > chosenJson.length())) {
                chosen = o;
                chosenJson = pretty;
            }
        }
        if (chosen == null || chosenJson == null) {
            System.out.println("[live] no serialisable instance to sample");
            return null;
        }
        String typeName = chosen instanceof RosettaModelObject rmo
                ? rmo.getType().getName() : chosen.getClass().getName();

        ObjectMapper envelope = new ObjectMapper();
        JsonNode value = envelope.readTree(chosenJson);
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("leg", leg);
        doc.put("runtime", LegPins.RUNTIME_RESOLVED);
        doc.put("serialiser", sw.serialiserMode());
        doc.put("serialiserDetail", sw.json().detail());
        doc.put("type", typeName);
        // the workload mapper's own pretty-printed length; the envelope below re-renders the
        // node with ITS printer, so the file's byte count will differ slightly - the field
        // describes what the leg's serialiser produced, which is the fact worth recording
        doc.put("serialisedChars", chosenJson.length());
        doc.put("treeStamp", closure.stamp());
        doc.put("inputs", "synthetic (deterministic builder population)");
        doc.put("value", value);

        Path file = DemoPaths.liveSample();
        Receipt.writeText(file, envelope.writerWithDefaultPrettyPrinter()
                .writeValueAsString(doc) + "\n");
        System.out.println("[live] sample " + typeName + " (" + chosenJson.length()
                + " chars) -> " + file);
        Demo.metric("liveSampleType", typeName);
        Demo.metric("liveSampleSerialisedChars", chosenJson.length());

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("type", typeName);
        summary.put("serialisedChars", chosenJson.length());
        summary.put("file", file.toString().replace('\\', '/'));
        summary.put("serialiser", sw.serialiserMode());
        return summary;
    }

    // ------------------------------------------------------------------ args

    /** Minimal {@code --key value} parser; unknown keys are rejected, never ignored. */
    static final class Args {
        // No --out: the classes directory must resolve through DemoPaths.classesRoot for
        // WRITER and READER alike, or setup could put classes somewhere run/live never look.
        // Move it with -Ddemo.classes=<root> instead, which both halves consult.
        private static final List<String> KNOWN = List.of("leg", "workload", "ops", "warmup",
                "limit", "depth", "src", "exclude", "receipt", "command");
        private static final List<String> FLAGS = List.of("force");

        private final Map<String, String> values = new LinkedHashMap<>();
        private final List<String> flags = new ArrayList<>();
        private final String commandLine;

        private Args(String[] argv) {
            StringBuilder cmd = new StringBuilder("demo-harness-exec");
            for (String s : argv) {
                cmd.append(' ').append(s.indexOf(' ') >= 0 ? "\"" + s + "\"" : s);
            }
            this.commandLine = cmd.toString();
        }

        static Args parse(String[] argv) {
            Args a = new Args(argv);
            for (int i = 1; i < argv.length; i++) {
                String tok = argv[i];
                if (!tok.startsWith("--")) {
                    throw new IllegalArgumentException("unexpected argument '" + tok + "'");
                }
                String key = tok.substring(2);
                if (FLAGS.contains(key)) {
                    a.flags.add(key);
                    continue;
                }
                if (!KNOWN.contains(key)) {
                    throw new IllegalArgumentException("unknown option '--" + key + "' (known: "
                            + KNOWN + " and flags " + FLAGS + ")");
                }
                if (i + 1 >= argv.length) {
                    throw new IllegalArgumentException("--" + key + " needs a value");
                }
                a.values.put(key, argv[++i]);
            }
            return a;
        }

        String get(String key) {
            return values.get(key);
        }

        boolean flag(String key) {
            return flags.contains(key);
        }

        String required(String key) {
            String v = values.get(key);
            if (v == null || v.isBlank()) {
                throw new IllegalArgumentException("--" + key + " is required");
            }
            return v;
        }

        int intValue(String key, int fallback) {
            String v = values.get(key);
            if (v == null || v.isBlank()) {
                return fallback;
            }
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("--" + key + " must be an integer, got '"
                        + v + "'");
            }
        }

        /** Every count argument is range-checked: a negative --warmup would corrupt the
         *  progress stream, and a negative --ops would silently measure nothing. */
        int positive(String key, int fallback) {
            int v = intValue(key, fallback);
            if (v <= 0) {
                throw new IllegalArgumentException("--" + key + " must be positive, got " + v);
            }
            return v;
        }

        int nonNegative(String key, int fallback) {
            int v = intValue(key, fallback);
            if (v < 0) {
                throw new IllegalArgumentException("--" + key + " must not be negative, got " + v);
            }
            return v;
        }

        Path path(String key, Path fallback) {
            String v = values.get(key);
            return (v == null || v.isBlank()) ? fallback
                    : Paths.get(v).toAbsolutePath().normalize();
        }

        /**
         * The receipt's {@code command} must equal what demo/runs.json holds, so the wrapper
         * passes it with {@code --command}; the reconstructed argv is only a fallback and is
         * labelled as such in that case.
         */
        void maybeReceipt(String id, String title, Instant started, long durationMs,
                          Map<String, Object> metrics, String notes) throws java.io.IOException {
            String receipt = values.get("receipt");
            if (receipt == null || receipt.isBlank()) {
                return;
            }
            String command = values.get("command");
            String effectiveNotes = notes;
            if (command == null || command.isBlank()) {
                command = commandLine;
                effectiveNotes = notes + " (command reconstructed from argv - no --command was"
                        + " passed; demo/runs.json is the single source for the exact line.)";
            }
            Receipt.write(Paths.get(receipt), id, SECTION, title, command, started, durationMs,
                    metrics, effectiveNotes);
        }
    }
}
