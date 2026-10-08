package demo.harness.legacy;

import com.google.inject.Injector;
import com.regnosys.rosetta.RosettaStandaloneSetup;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;

import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Boots the upstream Xtext 9.83.0 pipeline standalone and loads a corpus into one
 * {@link XtextResourceSet}.
 *
 * <p>The sequence is the one proven in-repo by
 * {@code scripts/xtext-oracle/src/main/java/org/finos/rune/oracle/RosettaResolutionOracle.java},
 * which runs against the same released 9.83.0 artifacts:
 *
 * <ol>
 *   <li>{@code new RosettaStandaloneSetup().createInjectorAndDoEMFRegistration()} — this also
 *       clears {@code EValidator.Registry.INSTANCE} and registers the three EPackages;</li>
 *   <li>{@code injector.getInstance(XtextResourceSet.class)};</li>
 *   <li>load the BUILTINS FIRST, then the corpus, each as a plain {@code file:} URI;</li>
 *   <li>{@link EcoreUtil#resolveAll(org.eclipse.emf.ecore.resource.ResourceSet)}.</li>
 * </ol>
 *
 * <p><b>Why plain {@code file:} URIs work for the builtins.</b> Upstream addresses them as
 * {@code classpath:/model/basictypes.rosetta}, but
 * {@code com.regnosys.rosetta.builtin.RosettaBuiltinsService} documents and implements a
 * fallback: when {@code resourceSet.getResource(uri, false)} returns null it scans the resource
 * set for any resource whose URI path ends with the same file name. So a builtin loaded from
 * disk satisfies it. What is NOT optional is that {@code model/basictypes.rosetta} and
 * {@code model/annotations.rosetta} exist as CLASSPATH RESOURCES (they ship in
 * {@code org.finos.rune:rune-runtime:9.83.0}): {@code RosettaBuiltinsService}'s URL fields are
 * {@code Objects.requireNonNull}-guarded and the service is {@code @Inject}-ed into the scope
 * provider, so a missing resource kills the INJECTOR, long before any parsing.
 */
public final class LegacyWorkspace {

    private LegacyWorkspace() {
    }

    /** The two builtin resources as they are named on the classpath, in load order. */
    public static final List<String> BUILTIN_CLASSPATH_RESOURCES =
            List.of("/model/basictypes.rosetta", "/model/annotations.rosetta");

    /** The two builtin file names, matched when a builtins DIRECTORY is supplied. */
    public static final List<String> BUILTIN_FILE_NAMES =
            List.of("basictypes.rosetta", "annotations.rosetta");

    /**
     * @param startupMs   JVM process start to injector-ready (CONTRACTS section-5)
     * @param corpus      the resources that are subjects: generated from, and counted
     * @param builtins    the resources that are resolution context only
     * @param resourceErrors one formatted line per syntax/linking error found after resolve
     */
    public record Loaded(Injector injector, XtextResourceSet resourceSet,
                         List<Resource> corpus, List<Resource> builtins,
                         long startupMs, long parseMs, long resolveMs,
                         List<String> resourceErrors) {
    }

    /**
     * Boots the injector, loads and resolves.
     *
     * @param builtinsDir a directory holding the two builtins; when {@code null} they are
     *                    extracted from the classpath instead, unless {@code skipBuiltins}
     * @param skipBuiltins honour {@code --builtins-ignored}: load no builtins at all. Almost
     *                     always wrong -- every {@code string} / {@code boolean} /
     *                     {@code [metadata ...]} reference then resolves to nothing -- so it
     *                     warns loudly.
     */
    public static Loaded load(List<Path> srcRoots, Path builtinsDir, boolean skipBuiltins)
            throws IOException {
        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();

        Injector injector = new RosettaStandaloneSetup().createInjectorAndDoEMFRegistration();
        long startupMs = System.currentTimeMillis() - jvmStart;
        DemoOut.metric("startupMs", startupMs);
        DemoOut.log("[legacy] Xtext injector ready " + startupMs + " ms after JVM start");

        XtextResourceSet resourceSet = injector.getInstance(XtextResourceSet.class);

        List<Path> builtinFiles = new ArrayList<>();
        if (skipBuiltins) {
            DemoOut.log("[legacy] WARNING --builtins-ignored: no builtin models will be"
                    + " loaded; every basic type and [metadata ...] annotation will be"
                    + " unresolved and the results will be wrong");
        } else if (builtinsDir != null) {
            builtinFiles.addAll(rosettaFilesUnder(builtinsDir));
        } else {
            builtinFiles.addAll(extractBuiltinsFromClasspath());
        }

        List<Path> corpusFiles = new ArrayList<>();
        for (Path root : srcRoots) {
            for (Path p : rosettaFilesUnder(root)) {
                // A source root that happens to contain the builtins must not load them
                // twice: upstream's own generator ignores them by file name, and a duplicate
                // resource would make every builtin declaration ambiguous.
                if (!BUILTIN_FILE_NAMES.contains(p.getFileName().toString())) {
                    corpusFiles.add(p);
                }
            }
        }

        long parseStart = System.nanoTime();
        List<Resource> builtinResources = new ArrayList<>();
        for (Path p : builtinFiles) {
            builtinResources.add(loadResource(resourceSet, p));
        }
        // LinkedHashSet: two source roots that overlap must not yield the same resource twice.
        Set<Resource> corpusResources = new LinkedHashSet<>();
        for (Path p : corpusFiles) {
            corpusResources.add(loadResource(resourceSet, p));
        }
        long parseMs = millisSince(parseStart);

        long resolveStart = System.nanoTime();
        EcoreUtil.resolveAll(resourceSet);
        long resolveMs = millisSince(resolveStart);

        List<String> errors = new ArrayList<>();
        for (Resource r : corpusResources) {
            if (r instanceof XtextResource xtext) {
                for (var error : xtext.getErrors()) {
                    errors.add(r.getURI().lastSegment() + ":" + error.getLine() + " "
                            + oneLine(error.getMessage()));
                }
            }
        }

        DemoOut.log("[legacy] loaded " + corpusResources.size() + " corpus + "
                + builtinResources.size() + " builtin resource(s) in " + parseMs
                + " ms; resolveAll in " + resolveMs + " ms; " + errors.size() + " error(s)");

        return new Loaded(injector, resourceSet, List.copyOf(corpusResources),
                List.copyOf(builtinResources), startupMs, parseMs, resolveMs,
                List.copyOf(errors));
    }

    private static Resource loadResource(XtextResourceSet resourceSet, Path file)
            throws IOException {
        Resource resource = resourceSet.createResource(
                URI.createFileURI(file.toAbsolutePath().normalize().toString()));
        resource.load(resourceSet.getLoadOptions());
        return resource;
    }

    /**
     * Copies {@code model/basictypes.rosetta} and {@code model/annotations.rosetta} out of
     * {@code rune-runtime} on the classpath into a temp directory, so they can be loaded as
     * ordinary {@code file:} URIs like every other model.
     */
    private static List<Path> extractBuiltinsFromClasspath() throws IOException {
        Path tmp = Files.createTempDirectory("demo-legacy-builtins");
        tmp.toFile().deleteOnExit();
        List<Path> out = new ArrayList<>();
        for (String resource : BUILTIN_CLASSPATH_RESOURCES) {
            try (InputStream in = LegacyWorkspace.class.getResourceAsStream(resource)) {
                if (in == null) {
                    throw new IOException("builtin classpath resource missing: " + resource
                            + " -- org.finos.rune:rune-runtime:9.83.0 is not on the classpath."
                            + " Supply --builtins <dir> instead, or fix target/lib.");
                }
                Path target = tmp.resolve(resource.substring(resource.lastIndexOf('/') + 1));
                Files.copy(in, target);
                target.toFile().deleteOnExit();
                out.add(target);
            }
        }
        DemoOut.log("[legacy] builtins extracted from the classpath into " + tmp);
        return out;
    }

    /** Every {@code .rosetta} file under {@code dir}, recursively, in sorted path order. */
    public static List<Path> rosettaFilesUnder(Path dir) throws IOException {
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

    static String oneLine(String s) {
        return s == null ? "" : s.replace("\r", " ").replace("\n", " ").trim();
    }

    static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
