package demo.harness.analytics;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The FORK analytics lane ({@code plus-jvm}) of the Rune DSL stats demo.
 *
 * <pre>
 * plus-jvm --src "dir;dir;..." --builtins &lt;dir&gt; [--receipt &lt;file&gt;] [--command "&lt;line&gt;"]
 * </pre>
 *
 * <p>Parses the merged corpus with the fork parser, links it into one {@link RWorkspace},
 * builds the demo-contract section 5 index and answers Q1-Q4. The queries themselves are
 * {@link Queries}, shared verbatim with the legacy lane.
 *
 * <p><b>The {@code legacy-emf} lane is NOT in this artifact.</b> The fork and upstream
 * {@code rune-lang:9.83.0} both own the java package {@code com.regnosys.rosetta.*} with
 * incompatible class shapes, so they cannot share a classpath. It ships instead inside
 * {@code demo-harness-legacy}, under {@code demo.harness.analytics.legacy.Main}, and emits the
 * identical metric shape. Invoking {@code legacy-emf} here says so and exits 2.
 */
public final class Main {

    private Main() {
    }

    private static final String USAGE = String.join("\n",
            "demo-harness-analytics (org.finos.rune.demo:demo-harness-analytics:1.0.0)",
            "",
            "  plus-jvm --src \"dir;dir;...\" --builtins <dir>",
            "           [--receipt <file>] [--command \"<exact line>\"]",
            "",
            "  The legacy-emf lane is a SEPARATE artifact -- upstream rune-lang:9.83.0 and the",
            "  fork cannot share a classpath (both own com.regnosys.rosetta.*). Run it as:",
            "    java -cp \"<demo-harness-legacy>/target/classes;<...>/target/lib/*\" \\",
            "         demo.harness.analytics.legacy.Main legacy-emf --src \"dir;dir;...\"");

    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.err.println(USAGE);
            System.exit(2);
        }
        String verb = argv[0];
        int code;
        try {
            Args args = Args.parse(argv, 1);
            code = switch (verb) {
                case "plus-jvm" -> plusJvm(args);
                case "legacy-emf" -> {
                    DemoOut.error("legacy-emf is not in this artifact: run"
                            + " demo.harness.analytics.legacy.Main from demo-harness-legacy,"
                            + " which carries the upstream 9.83.0 classpath");
                    System.err.println(USAGE);
                    yield 2;
                }
                case "-h", "--help", "help" -> {
                    System.out.println(USAGE);
                    yield 0;
                }
                default -> {
                    System.err.println("unknown verb '" + verb + "'\n\n" + USAGE);
                    yield 2;
                }
            };
        } catch (IllegalArgumentException e) {
            DemoOut.error(String.valueOf(e.getMessage()));
            System.err.println(USAGE);
            code = 2;
        } catch (Throwable t) {
            DemoOut.error(t.getClass().getSimpleName() + ": " + t.getMessage());
            t.printStackTrace();
            code = 4;
        }
        System.exit(code);
    }

    static int plusJvm(Args args) throws IOException {
        List<Path> srcRoots = args.requirePaths("src");
        Path builtinsDir = args.requirePath("builtins");
        Path receipt = args.optionalPath("receipt");

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        DemoOut.start("analytics.plus-jvm", "fork parser analytics over "
                + srcRoots.size() + " source root(s)");

        // "process start -> engine ready", the same thing the legacy lane measures as injector
        // boot. Forcing initialisation makes it a real measurement rather than a no-op: the
        // fork has no injector to build, and that IS the comparison.
        touchEngineClasses();
        long startupMs = System.currentTimeMillis() - jvmStart;
        DemoOut.metric("startupMs", startupMs);

        List<Path> builtinFiles = rosettaFilesUnder(builtinsDir);
        List<Path> corpusFiles = new ArrayList<>();
        for (Path root : srcRoots) {
            for (Path p : rosettaFilesUnder(root)) {
                if (!Queries.isBuiltinFile(p.getFileName().toString())) {
                    corpusFiles.add(p);
                }
            }
        }

        List<RModel> models = new ArrayList<>(builtinFiles.size() + corpusFiles.size());
        List<String> failures = new ArrayList<>();
        long parseStart = System.nanoTime();
        for (Path p : builtinFiles) {
            parseInto(models, p, failures);
        }
        int parsed = 0;
        for (Path p : corpusFiles) {
            parseInto(models, p, failures);
            if (++parsed % 250 == 0) {
                DemoOut.progress(parsed, corpusFiles.size());
            }
        }
        long parseMs = Queries.millisSince(parseStart);
        DemoOut.progress(corpusFiles.size(), corpusFiles.size());

        if (!failures.isEmpty()) {
            for (int i = 0; i < Math.min(10, failures.size()); i++) {
                DemoOut.error("parse failure: " + failures.get(i));
            }
            DemoOut.error(failures.size() + " parse failure(s); counts would be wrong");
            return 3;
        }
        DemoOut.metric("parseMs", parseMs);

        // The four queries as specified need only source-text names, so the fork could answer
        // them from the parse alone. The workspace is built anyway, and reported separately as
        // linkMs, so the comparison against the legacy lane's EcoreUtil.resolveAll is
        // like-for-like: both lanes do a full semantic load before being asked anything.
        long linkStart = System.nanoTime();
        RLinkingResult linking = RWorkspace.build(models);
        long linkMs = Queries.millisSince(linkStart);
        DemoOut.metric("linkMs", linkMs);
        DemoOut.log("[plus-jvm] parsed " + models.size() + " file(s) in " + parseMs
                + " ms; linked in " + linkMs + " ms");

        List<Decl> declarations = ForkExtractor.extract(linking.workspace().files());
        Queries.Outcome outcome = Queries.run(declarations);
        DemoOut.log("[plus-jvm] " + AnalyticsRun.summary(outcome));

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("linkMs", linkMs);
        extra.put("declarations", declarations.size());
        extra.put("wallMs", System.currentTimeMillis() - jvmStart);
        Map<String, Object> metrics = AnalyticsRun.metrics(
                "plus-jvm", corpusFiles.size(), parseMs, outcome, startupMs, extra);
        DemoOut.done(metrics);

        if (receipt != null) {
            Receipts.write(receipt, "analytics.plus-jvm", "analytics",
                    "Analytics - fork parser (plus-jvm)",
                    Receipts.commandLine("plus-jvm", args),
                    System.currentTimeMillis() - jvmStart, metrics,
                    "files counts the NON-builtin population; the builtins are loaded but not"
                    + " counted. parseMs is parse only, linkMs is the workspace build (the"
                    + " counterpart of the legacy lane's resolveAll). peakRssMb omitted -- see"
                    + " AnalyticsRun; peakHeapMb/peakNonHeapMb are the measurable peaks.");
        }
        return 0;
    }

    /**
     * Loads and initialises the parser and linker entry classes, so {@code startupMs} measures
     * "engine ready" rather than only JVM boot.
     */
    private static void touchEngineClasses() {
        try {
            Class.forName(AstBuilder.class.getName(), true,
                    Main.class.getClassLoader());
            Class.forName(RWorkspace.class.getName(), true,
                    Main.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            // Unreachable: both are compile-time dependencies. Nothing to recover, and a
            // startup measurement is not worth failing a run over.
            DemoOut.log("[plus-jvm] warning: engine pre-load skipped (" + e + ")");
        }
    }

    private static void parseInto(List<RModel> models, Path file, List<String> failures) {
        try {
            models.add(AstBuilder.buildFromFile(file));
        } catch (Exception e) {
            failures.add(file + " -- " + e);
        }
    }

    static List<Path> rosettaFilesUnder(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("not a directory: " + dir.toAbsolutePath());
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".rosetta"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }
}
