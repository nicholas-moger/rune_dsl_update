package demo.harness.analytics.legacy;

import demo.harness.legacy.AnalyticsRun;
import demo.harness.legacy.Args;
import demo.harness.legacy.Decl;
import demo.harness.legacy.DemoOut;
import demo.harness.legacy.LegacyWorkspace;
import demo.harness.legacy.Queries;
import demo.harness.legacy.Receipts;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The LEGACY analytics lane ({@code legacy-emf}) of the Rune DSL stats demo.
 *
 * <pre>
 * legacy-emf --src "dir;dir;..." [--builtins &lt;dir&gt; | --builtins-ignored]
 *            [--tolerate-errors] [--receipt &lt;file&gt;] [--command "&lt;exact line&gt;"]
 * </pre>
 *
 * <p>Boots the upstream Xtext 9.83.0 injector, loads the whole corpus into one
 * {@code XtextResourceSet}, runs {@code EcoreUtil.resolveAll}, then walks the EMF model to
 * build the CONTRACTS section-5 index and answer Q1-Q4. The queries are {@link Queries},
 * shared verbatim with the fork lane; only {@link EmfExtractor} differs.
 *
 * <p>It lives in the {@code demo-harness-legacy} artifact rather than in
 * {@code demo-harness-analytics} because the fork and upstream both own the java package
 * {@code com.regnosys.rosetta.*} with incompatible class shapes and cannot share a classpath.
 * This module already has exactly the classpath this lane needs.
 *
 * <p>The JVM-plus-EMF startup and {@code resolveAll} cost being VISIBLE is the point, not a
 * defect: {@code startupMs} (process start to injector ready) and {@code resolveMs} are both
 * reported as first-class numbers.
 */
public final class Main {

    private Main() {
    }

    private static final String USAGE = String.join("\n",
            "demo-harness-legacy :: analytics (demo.harness.analytics.legacy.Main)",
            "",
            "  legacy-emf --src \"dir;dir;...\" [--builtins <dir> | --builtins-ignored]",
            "             [--tolerate-errors] [--receipt <file>] [--command \"<exact line>\"]",
            "",
            "  With neither builtins flag, basictypes.rosetta and annotations.rosetta are",
            "  extracted from rune-runtime on the classpath. They are always LOADED (nothing",
            "  resolves otherwise) and never COUNTED.");

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
                case "legacy-emf" -> legacyEmf(args);
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

    static int legacyEmf(Args args) throws IOException {
        List<Path> srcRoots = args.requirePaths("src");
        Path builtinsDir = args.optionalPath("builtins");
        boolean skipBuiltins = args.has("builtins-ignored");
        boolean tolerateErrors = args.has("tolerate-errors");
        Path receipt = args.optionalPath("receipt");

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        DemoOut.start("analytics.legacy-emf", "upstream 9.83.0 EMF analytics over "
                + srcRoots.size() + " source root(s)");

        LegacyWorkspace.Loaded loaded =
                LegacyWorkspace.load(srcRoots, builtinsDir, skipBuiltins);
        DemoOut.metric("parseMs", loaded.parseMs());
        DemoOut.metric("resolveMs", loaded.resolveMs());

        if (!loaded.resourceErrors().isEmpty()) {
            for (int i = 0; i < Math.min(10, loaded.resourceErrors().size()); i++) {
                DemoOut.error("resource error: " + loaded.resourceErrors().get(i));
            }
            if (!tolerateErrors) {
                DemoOut.error(loaded.resourceErrors().size() + " resource error(s) after"
                        + " resolveAll; the counts would be wrong (pass --tolerate-errors to"
                        + " report them anyway)");
                return 3;
            }
            DemoOut.log("[legacy-emf] --tolerate-errors: counting past "
                    + loaded.resourceErrors().size() + " resource error(s)");
        }

        EmfExtractor.Stats stats = new EmfExtractor.Stats();
        List<Decl> declarations = EmfExtractor.extract(loaded.corpus(), stats);
        Queries.Outcome outcome = Queries.run(declarations);
        DemoOut.log("[legacy-emf] " + AnalyticsRun.summary(outcome));

        if (stats.unresolvedSuperTypes > 0 || stats.unresolvedAttributeTypes > 0) {
            // Reported, never swallowed: an unresolved reference here is the one thing that
            // could make this lane's answers differ from the fork lane's.
            DemoOut.log("[legacy-emf] note: recovered " + stats.unresolvedSuperTypes
                    + " super-type and " + stats.unresolvedAttributeTypes
                    + " attribute-type reference(s) from the node model because EMF left them"
                    + " as proxies; if the lanes disagree, start here");
        }

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("resolveMs", loaded.resolveMs());
        extra.put("declarations", declarations.size());
        extra.put("unresolvedSuperTypes", stats.unresolvedSuperTypes);
        extra.put("unresolvedAttributeTypes", stats.unresolvedAttributeTypes);
        extra.put("skippedRootElements", stats.skippedRootElements);
        extra.put("resourceErrors", loaded.resourceErrors().size());
        extra.put("wallMs", System.currentTimeMillis() - jvmStart);
        Map<String, Object> metrics = AnalyticsRun.metrics(
                "legacy-emf", loaded.corpus().size(), loaded.parseMs(), outcome,
                loaded.startupMs(), extra);
        DemoOut.done(metrics);

        if (receipt != null) {
            Receipts.write(receipt, "analytics.legacy-emf", "analytics",
                    "Analytics - upstream Xtext 9.83.0 (legacy-emf)",
                    Receipts.commandLine("legacy-emf", args),
                    System.currentTimeMillis() - jvmStart, metrics,
                    "startupMs is process start to Guice injector ready and resolveMs is"
                    + " EcoreUtil.resolveAll; both are real costs of this lane and are reported"
                    + " rather than netted out. files counts the NON-builtin population."
                    + " peakRssMb omitted -- see AnalyticsRun.");
        }
        return 0;
    }
}
