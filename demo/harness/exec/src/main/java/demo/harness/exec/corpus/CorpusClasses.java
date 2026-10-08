package demo.harness.exec.corpus;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * In-JVM javac of the FORK-GENERATED tree into one classes directory per runtime leg -
 * adapted from {@code rune-benchmarks/.../corpus/CorpusClasses.java} (the same javax.tools
 * approach as the D11 CompileGate) with two deliberate changes:
 *
 * <ol>
 *   <li><b>Source.</b> The reference compiles a corpus cell's FROZEN goldens; this compiles
 *       {@code demo/work/gen-m1/} - the tree the fork's own generator just emitted (byte
 *       identical to golden, which is exactly the claim the demo is making). One tree, one
 *       javac call: the demo closure is already merged (builtins + cdm + iso + drr), so
 *       there are no upstream cells to chain.</li>
 *   <li><b>Marker.</b> The reference keys its up-to-date marker on the SOURCE COUNT alone,
 *       sound there because golden content drift is gated by the frozen-corpus manifest
 *       test. Nothing gates {@code gen-m1}: the integrator regenerates it, and a regenerated
 *       tree can easily have the same file count. So the marker here carries a CONTENT
 *       STAMP - a SHA-256 over every source's relative path, size and mtime - plus the leg's
 *       own pins, because classes compiled against one leg's runtime must never be reused
 *       for the other.</li>
 * </ol>
 *
 * <p>The compile classpath is this JVM's own {@code java.class.path}, which for the shaded
 * leg jar IS the leg's runtime plus jackson, commons-lang3 and guice at their pinned
 * versions. That is the whole mechanism by which "compiled against the leg" is true.
 */
public final class CorpusClasses {

    public static final String MARKER_NAME = ".compiled-marker";
    public static final String ERRORS_NAME = "compile-errors.txt";
    /** Extra entries for the javac classpath, path-separator joined. */
    public static final String EXTRA_CP_PROPERTY = "demo.exec.extraCp";

    private CorpusClasses() {}

    /** Every {@code .java} under {@code root}, sorted (compile order must be stable). */
    public static List<Path> listJavaSources(Path root) throws IOException {
        try (Stream<Path> s = Files.walk(root)) {
            List<Path> list = new ArrayList<>(
                    s.filter(p -> p.toString().endsWith(".java")).toList());
            list.sort(Path::compareTo);
            return list;
        }
    }

    /**
     * Content stamp over the source set: SHA-256 of {@code relpath\0size\0mtimeMillis} for
     * every file, in sorted order. Cheap (no file bodies read) but it moves whenever the
     * integrator regenerates, which a bare count does not.
     */
    public static String stamp(Path root, List<Path> sources) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (Path p : sources) {
                String rel = root.relativize(p).toString().replace('\\', '/');
                md.update(rel.getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(Long.toString(Files.size(p)).getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                md.update(Long.toString(Files.getLastModifiedTime(p).toMillis())
                        .getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
            }
            byte[] d = md.digest();
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", d[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("stamping failed for " + root, e);
        }
    }

    /** The compile result plus everything a receipt needs to describe it. */
    public record CompileResult(Path classesDir, int sources, int excluded, String stamp,
                                boolean reused, long compileMs) {}

    /**
     * Ensure {@code srcRoot} is compiled into {@code outDir} for {@code legPinsLabel}.
     * Sources whose path contains {@code excludeContains} are left out and COUNTED (never
     * silently dropped - the honest-denominator law).
     */
    public static CompileResult ensureCompiled(Path srcRoot, Path outDir, String excludeContains,
                                               String legPinsLabel, boolean force) {
        if (!Files.isDirectory(srcRoot)) {
            throw new IllegalStateException("generated tree not present: " + srcRoot
                    + " - the integrator generates it first (harness/codegen 'generate --mode m1"
                    + " --out demo/work/gen-m1')");
        }
        try {
            List<Path> all = listJavaSources(srcRoot);
            if (all.isEmpty()) {
                throw new IllegalStateException("no .java sources under " + srcRoot);
            }
            List<Path> sources = excludeContains == null || excludeContains.isEmpty() ? all
                    : all.stream()
                    .filter(p -> !p.toString().replace('\\', '/').contains(excludeContains))
                    .toList();
            int excluded = all.size() - sources.size();
            String contentStamp = stamp(srcRoot, sources);

            Path marker = outDir.resolve(MARKER_NAME);
            String expected = "sources=" + sources.size()
                    + " excluded=" + excluded
                    + (excludeContains == null || excludeContains.isEmpty()
                        ? "" : " token=" + excludeContains)
                    + " stamp=" + contentStamp
                    + " pins={" + legPinsLabel + "}";
            if (!force && Files.isRegularFile(marker)
                    && expected.equals(Files.readString(marker, StandardCharsets.UTF_8).trim())) {
                System.out.printf("[CorpusClasses] reused %d classes for {%s} -> %s%n",
                        sources.size(), contentStamp, outDir);
                return new CompileResult(outDir, sources.size(), excluded, contentStamp, true, 0L);
            }
            deleteRecursively(outDir);
            Files.createDirectories(outDir);
            long t0 = System.nanoTime();
            compile(sources, outDir);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Files.writeString(marker, expected, StandardCharsets.UTF_8);
            System.out.printf("[CorpusClasses] compiled %d sources (%d excluded, {%s}) in %d ms"
                    + " -> %s%n", sources.size(), excluded, contentStamp, ms, outDir);
            return new CompileResult(outDir, sources.size(), excluded, contentStamp, false, ms);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> s = Files.walk(dir)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }

    private static void compile(List<Path> sources, Path out) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException(
                    "system Java compiler unavailable - run the exec harness on a JDK, not a JRE");
        }
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        List<String> classpath = new ArrayList<>();
        String extra = System.getProperty(EXTRA_CP_PROPERTY);
        if (extra != null && !extra.isBlank()) {
            classpath.addAll(List.of(extra.split(File.pathSeparator)));
        }
        classpath.addAll(List.of(System.getProperty("java.class.path").split(File.pathSeparator)));
        try (StandardJavaFileManager fm =
                     compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjectsFromPaths(sources);
            List<String> options = List.of(
                    "-classpath", String.join(File.pathSeparator, classpath),
                    "-d", out.toString(),
                    "-encoding", "UTF-8",
                    "-proc:none",
                    "-nowarn");
            boolean ok = Boolean.TRUE.equals(
                    compiler.getTask(null, fm, diags, options, null, units).call());
            List<Diagnostic<? extends JavaFileObject>> errors = diags.getDiagnostics().stream()
                    .filter(d -> d.getKind() == Diagnostic.Kind.ERROR).toList();
            if (!ok || !errors.isEmpty()) {
                // Full diagnostics to a file so the integrator can triage without re-running
                // a multi-minute compile; the first five inline for the console.
                StringBuilder all = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : errors) {
                    all.append(d).append(System.lineSeparator());
                }
                Path errFile = out.resolve(ERRORS_NAME);
                try {
                    Files.createDirectories(out);
                    Files.writeString(errFile, all.toString(), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // reporting the compile failure matters more than persisting it
                }
                StringBuilder sb = new StringBuilder("generated-tree compile FAILED ("
                        + errors.size() + " errors, full list in " + errFile
                        + "); first diagnostics:");
                errors.stream().limit(5).forEach(d -> sb.append("\n  ").append(d));
                throw new IllegalStateException(sb.toString());
            }
        }
    }

    /** Class loader over the compiled tree, parented by this JVM's (the leg's) loader. */
    public static URLClassLoader loaderOver(List<Path> outDirs) {
        URL[] urls = outDirs.stream().map(p -> {
            try {
                return p.toUri().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }).toArray(URL[]::new);
        return new URLClassLoader(urls, CorpusClasses.class.getClassLoader());
    }

    /** Dotted class names under {@code outDir} whose name passes {@code filter} (sorted). */
    public static List<String> classNamesUnder(Path outDir,
                                               java.util.function.Predicate<String> filter) {
        try (Stream<Path> s = Files.walk(outDir)) {
            return s.filter(p -> p.toString().endsWith(".class"))
                    .map(p -> {
                        String rel = outDir.relativize(p).toString()
                                .replace(File.separatorChar, '.').replace('/', '.');
                        return rel.substring(0, rel.length() - ".class".length());
                    })
                    .filter(filter)
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
