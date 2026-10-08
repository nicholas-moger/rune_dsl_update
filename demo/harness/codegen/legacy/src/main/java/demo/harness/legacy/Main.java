package demo.harness.legacy;

import com.google.inject.Injector;
import com.regnosys.rosetta.generator.RosettaGenerator;
import com.regnosys.rosetta.rosetta.RosettaModel;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.generator.GeneratorContext;
import org.eclipse.xtext.generator.IFileSystemAccess;
import org.eclipse.xtext.generator.IOutputConfigurationProvider;
import org.eclipse.xtext.generator.OutputConfiguration;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.util.CancelIndicator;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * The LEGACY (upstream Xtext 9.83.0) code-generation driver for the Rune DSL stats demo.
 *
 * <pre>
 * generate --src "dir;dir;..." --out &lt;dir&gt;
 *          [--builtins &lt;dir&gt; | --builtins-ignored]
 *          [--filter drr|all] [--tolerate-errors]
 *          [--receipt &lt;file&gt;] [--command "&lt;exact line&gt;"]
 * </pre>
 *
 * <p>Runs the same five-call generator lifecycle the upstream Maven plugin runs, minus the
 * {@code StandaloneBuilder} scaffolding it needs only to be a Maven plugin:
 *
 * <pre>
 * gen.beforeAllGenerate(resourceSet, fsa, ctx);   // Rune extension, NOT on IGenerator2
 * for (r : subjects) gen.beforeGenerate(r, fsa, ctx);
 * for (r : subjects) gen.doGenerate(r, fsa, ctx);
 * for (r : subjects) gen.afterGenerate(r, fsa, ctx);   // emits package-info.java
 * gen.afterAllGenerate(resourceSet, fsa, ctx);    // Rune extension
 * </pre>
 *
 * <p>{@code beforeAllGenerate} / {@code afterAllGenerate} are Rune additions that
 * {@code IGenerator2} does not declare -- which is the entire reason upstream ships
 * {@code RuneStandaloneBuilder}, a subclass whose only job is to reflect into Xtext's
 * {@code StandaloneBuilder} and call those two. Calling {@link RosettaGenerator} directly
 * makes them ordinary calls and drops the {@code xtext-maven-plugin} / {@code maven-core}
 * dependency tree entirely.
 *
 * <p><b>Skipping {@code afterGenerate} would silently lose every {@code package-info.java}</b> --
 * that is the pass it performs.
 */
public final class Main {

    private Main() {
    }

    private static final String USAGE = String.join("\n",
            "demo-harness-legacy (org.finos.rune.demo:demo-harness-legacy:1.0.0)",
            "  the UPSTREAM Xtext 9.83.0 generation + EMF analytics driver",
            "",
            "  generate  --src \"dir;dir;...\" --out <dir>",
            "            [--builtins <dir> | --builtins-ignored]",
            "            [--filter drr|all] [--tolerate-errors]",
            "            [--receipt <file>] [--command \"<exact line>\"]",
            "",
            "  The analytics verb lives in the same artifact, under its own main class:",
            "    demo.harness.analytics.legacy.Main legacy-emf --src \"dir;dir;...\" ...",
            "",
            "  --builtins <dir> loads basictypes.rosetta + annotations.rosetta from disk",
            "    (recommended). With neither flag they are extracted from rune-runtime on",
            "    the classpath. --builtins-ignored loads none, which makes every basic type",
            "    unresolved; it exists only so the flag in the lane spec is honoured.");

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
                case "generate" -> generate(args);
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

    static int generate(Args args) throws IOException {
        List<Path> srcRoots = args.requirePaths("src");
        Path outDir = args.requirePath("out");
        Path builtinsDir = args.optionalPath("builtins");
        boolean skipBuiltins = args.has("builtins-ignored");
        String filterName = args.get("filter", "all");
        boolean tolerateErrors = args.has("tolerate-errors");
        Path receipt = args.optionalPath("receipt");

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        DemoOut.start("codegen.legacy", "upstream 9.83.0 generate filter=" + filterName
                + " roots=" + srcRoots.size());

        LegacyWorkspace.Loaded loaded =
                LegacyWorkspace.load(srcRoots, builtinsDir, skipBuiltins);
        DemoOut.metric("parseMs", loaded.parseMs());
        DemoOut.metric("resolveMs", loaded.resolveMs());
        DemoOut.metric("sourceFiles", loaded.corpus().size() + loaded.builtins().size());

        if (!loaded.resourceErrors().isEmpty()) {
            for (int i = 0; i < Math.min(10, loaded.resourceErrors().size()); i++) {
                DemoOut.error("resource error: " + loaded.resourceErrors().get(i));
            }
            if (!tolerateErrors) {
                DemoOut.error(loaded.resourceErrors().size() + " resource error(s) after"
                        + " resolveAll; no files were written (pass --tolerate-errors to"
                        + " generate anyway)");
                return 3;
            }
            DemoOut.log("[legacy] --tolerate-errors: continuing past "
                    + loaded.resourceErrors().size() + " resource error(s)");
        }

        Injector injector = loaded.injector();

        // Output configurations come from the bound provider rather than by constructing
        // RosettaOutputConfigurationProvider directly: IOutputConfigurationProvider is the
        // stable Xtext interface, and RosettaRuntimeModule binds the Rosetta implementation to
        // it, so this reaches the same object without depending on its constructor.
        IOutputConfigurationProvider configProvider =
                injector.getInstance(IOutputConfigurationProvider.class);
        Map<String, OutputConfiguration> configs = new LinkedHashMap<>();
        for (OutputConfiguration config : configProvider.getOutputConfigurations()) {
            String directory = IFileSystemAccess.DEFAULT_OUTPUT.equals(config.getName())
                    ? outDir.toAbsolutePath().toString()
                    : outDir.resolve(config.getName()).toAbsolutePath().toString();
            config.setOutputDirectory(directory);
            config.setCreateOutputDirectory(true);
            config.setOverrideExistingResources(true);
            // Upstream defaults this to true. A demo must never be able to wipe a directory
            // the operator pointed it at, so it is forced off here.
            config.setCanClearOutputDirectory(false);
            configs.put(config.getName(), config);
            DemoOut.log("[legacy] output config " + config.getName() + " -> " + directory);
        }

        // Both lookups are VERIFIED bound by org.eclipse.xtext.service.DefaultRuntimeModule in
        // xtext 2.38.0 (methods bindIResourceServiceProvider$Registry and
        // configureRuntimeEncodingProvider), which every Xtext language module extends. If a
        // future Xtext ever drops the Registry binding, IResourceServiceProvider.Registry.INSTANCE
        // is the equivalent static singleton -- RosettaStandaloneSetupGenerated.register(injector)
        // populates exactly that one with the "rosetta" extension.
        CountingFsa fsa = new CountingFsa(
                injector.getInstance(IResourceServiceProvider.Registry.class),
                injector.getInstance(IEncodingProvider.class),
                jvmStart);
        fsa.setOutputConfigurations(configs);

        Predicate<RosettaModel> emissionFilter = emissionFilter(filterName);
        List<Resource> subjects = new ArrayList<>();
        for (Resource resource : loaded.corpus()) {
            RosettaModel model = rosettaModelOf(resource);
            if (model == null) {
                DemoOut.log("[legacy] skipping (no RosettaModel root): " + resource.getURI());
                continue;
            }
            if (emissionFilter.test(model)) {
                subjects.add(resource);
            }
        }
        DemoOut.log("[legacy] generating from " + subjects.size() + " of "
                + loaded.corpus().size() + " corpus resource(s)");

        GeneratorContext context = new GeneratorContext();
        context.setCancelIndicator(CancelIndicator.NullImpl);
        RosettaGenerator generator = injector.getInstance(RosettaGenerator.class);

        Files.createDirectories(outDir);

        long genStart = System.nanoTime();
        generator.beforeAllGenerate(loaded.resourceSet(), fsa, context);
        for (Resource resource : subjects) {
            generator.beforeGenerate(resource, fsa, context);
        }
        for (Resource resource : subjects) {
            generator.doGenerate(resource, fsa, context);
        }
        for (Resource resource : subjects) {
            // Do NOT skip: this is the pass that emits package-info.java.
            generator.afterGenerate(resource, fsa, context);
        }
        generator.afterAllGenerate(loaded.resourceSet(), fsa, context);
        long generateMs = LegacyWorkspace.millisSince(genStart);

        int onDisk = countJavaFiles(outDir);
        DemoOut.progress(fsa.writes(), 0);
        if (onDisk != fsa.writes()) {
            // Reported, never reconciled by picking a winner. The two can legitimately differ
            // (a re-run over a non-empty output directory leaves earlier files in place), and
            // a silent choice would hide exactly that.
            DemoOut.log("[legacy] note: " + fsa.writes() + " generateFile call(s) but "
                    + onDisk + " .java file(s) on disk under " + outDir.toAbsolutePath()
                    + " -- output directory was not empty, or a file was written twice");
        }

        long wallMs = System.currentTimeMillis() - jvmStart;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("mode", "legacy");
        metrics.put("files", onDisk);
        metrics.put("wallMs", wallMs);
        metrics.put("jvmStartToFirstFileMs",
                fsa.firstFileMs() < 0 ? 0 : fsa.firstFileMs());
        metrics.put("exit", 0);
        metrics.put("filter", filterName);
        metrics.put("sourceFiles", loaded.corpus().size() + loaded.builtins().size());
        metrics.put("generatedModels", subjects.size());
        metrics.put("generateFileCalls", fsa.writes());
        metrics.put("startupMs", loaded.startupMs());
        metrics.put("parseMs", loaded.parseMs());
        metrics.put("resolveMs", loaded.resolveMs());
        metrics.put("generateMs", generateMs);
        metrics.put("resourceErrors", loaded.resourceErrors().size());
        DemoOut.done(metrics);

        if (receipt != null) {
            Receipts.write(receipt, "codegen.legacy", "codegen",
                    "Legacy generate (upstream Xtext 9.83.0)",
                    Receipts.commandLine("generate", args), wallMs, metrics,
                    "wallMs is JVM-start to end of generation and INCLUDES the Xtext injector"
                    + " boot (startupMs), which is the point of the comparison; peakRssMb"
                    + " omitted (measured by the outer wrapper). 'files' counts .java files on"
                    + " disk; 'generateFileCalls' counts what the generator asked for.");
        }
        return 0;
    }

    /**
     * The emission filter, matched to the fork leg's so the two trees are comparable.
     *
     * <p>Upstream's own {@code RosettaGenerator.shouldGenerate} consults
     * {@code rosetta-config.yml} on the thread context classloader and, finding none, generates
     * everything; it independently ignores {@code basictypes.rosetta},
     * {@code annotations.rosetta} and {@code model-no-code-gen.rosetta} by file name. Selecting
     * subjects here rather than shipping a config file keeps that default untouched and keeps
     * the choice visible on the command line.
     *
     * <p>CAVEAT for {@code --filter drr}: {@code afterGenerate}'s package-info pass iterates the
     * models in the RESOURCE SET, not the subject list, so package-info output may be wider
     * than the filter. For {@code --filter all} -- the comparable, default case -- this does
     * not arise.
     */
    static Predicate<RosettaModel> emissionFilter(String filterName) {
        return switch (filterName) {
            case "drr" -> model -> {
                String ns = model.getName();
                return ns != null && (ns.equals("drr") || ns.startsWith("drr."));
            };
            case "all" -> model -> {
                // RosettaModel.getName() IS the namespace; there is no getNamespace().
                String ns = model.getName();
                if (ns == null) {
                    return false;
                }
                return !ns.equals("com.rosetta.model") && !ns.startsWith("com.rosetta.model.")
                        && !ns.equals("com.rosetta.test.model")
                        && !ns.startsWith("com.rosetta.test.model.");
            };
            default -> throw new IllegalArgumentException(
                    "--filter must be 'drr' or 'all' (got '" + filterName + "')");
        };
    }

    /** The {@link RosettaModel} root of a loaded resource, or {@code null} if it has none. */
    static RosettaModel rosettaModelOf(Resource resource) {
        if (resource.getContents().isEmpty()) {
            return null;
        }
        EObject root = resource.getContents().get(0);
        return root instanceof RosettaModel model ? model : null;
    }

    private static int countJavaFiles(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return (int) walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .count();
        }
    }
}
