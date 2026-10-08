package com.regnosys.rosetta.generator.java;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * W42 compile-gate engine (PR #233). Runs the JDK compiler over generated Java to
 * classify each file COMPILES / NON-COMPILING. No JUnit coupling; driven by
 * {@code D11CompileGateTest}. The runtime half of the classpath is the test JVM's own
 * classpath ({@link #jvmClasspath()}), which already carries rune-runtime + transitive
 * deps because rune-java-generator depends on them.
 *
 * <p>This is verification tooling, not a generator change — it only reads generator
 * output and reports compile status. See the design spec
 * {@code docs/superpowers/specs/2026-06-20-w42-compile-gate-design.md} (local) and the
 * committed ground-truth assessment
 * the development audit "2026-06-20-pr233-w42-compile-gate-ground-truth".
 */
public final class CompileGate {

    public enum Verdict { COMPILES, NON_COMPILING }

    /** Per-file outcome: path, verdict, dominant error category, up to 3 diagnostic strings. */
    public record FileResult(String path, Verdict verdict, String category, List<String> diagnostics) {}

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * The test JVM's own classpath entries. Used only by the {@code CompileGateTest}
     * anchor (which compiles a dependency-free snippet). The production driver
     * {@code D11CompileGateTest} does NOT use this — it compiles against each cell's
     * resolved UPSTREAM 9.83.0 classpath, because the fork's own rune-runtime on the JVM
     * classpath has drifted (see the class/driver javadoc).
     */
    public static List<String> jvmClasspath() {
        return List.of(System.getProperty("java.class.path").split(File.pathSeparator));
    }

    /**
     * Compile a mix of on-disk source files + in-memory sources to {@code outDir};
     * return ERROR-kind diagnostics only. Used both for the golden closure build and
     * for single-file fork classification.
     */
    public List<Diagnostic<? extends JavaFileObject>> compile(
            List<File> sourceFiles, List<JavaFileObject> inMemory,
            List<String> classpath, Path outDir) throws IOException {
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler (ToolProvider.getSystemJavaCompiler() "
                    + "returned null) — run the compile-gate on a JDK, not a JRE.");
        }
        Files.createDirectories(outDir);
        var diags = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fm =
                     compiler.getStandardFileManager(diags, null, StandardCharsets.UTF_8)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outDir.toFile()));
            List<JavaFileObject> units = new ArrayList<>();
            if (!sourceFiles.isEmpty()) {
                fm.getJavaFileObjectsFromFiles(sourceFiles).forEach(units::add);
            }
            units.addAll(inMemory);
            List<String> options = List.of(
                    "-classpath", String.join(File.pathSeparator, classpath),
                    "-proc:none",       // no annotation processing
                    "-nowarn",
                    "-Xmaxerrs", "100000");
            compiler.getTask(null, fm, diags, options, null, units).call();
        }
        List<Diagnostic<? extends JavaFileObject>> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diags.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) errors.add(d);
        }
        return errors;
    }

    /**
     * Compile ONE fork source in-memory against {@code classpath}; return a per-file
     * verdict. The fork file's dependencies resolve to the golden {@code .class}
     * closure on the classpath, so a NON_COMPILING verdict means the file's OWN
     * generation is broken (no cascade).
     */
    public FileResult classifyForkFile(String canonicalPath, String forkSource,
                                       List<String> classpath, Path throwawayOut) throws IOException {
        JavaFileObject src = new StringSource(canonicalPath, forkSource);
        List<Diagnostic<? extends JavaFileObject>> errors =
                compile(List.of(), List.of(src), classpath, throwawayOut);
        if (errors.isEmpty()) {
            return new FileResult(canonicalPath, Verdict.COMPILES, "OK", List.of());
        }
        List<String> msgs = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : errors) {
            if (msgs.size() >= 3) break;
            msgs.add(fmt(d));
        }
        return new FileResult(canonicalPath, Verdict.NON_COMPILING, categorize(errors), msgs);
    }

    private static String fmt(Diagnostic<? extends JavaFileObject> d) {
        return "L" + d.getLineNumber() + ": " + d.getMessage(Locale.ROOT);
    }

    /**
     * Coarse dominant-category bucket from the javac diagnostic MESSAGE strings.
     * Literal matching on non-structured compiler log text (permitted by the
     * engineering standard; NOT regex over structured language content). First match
     * wins, scanning the concatenation of all error messages.
     */
    static String categorize(List<Diagnostic<? extends JavaFileObject>> errors) {
        StringBuilder all = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : errors) {
            all.append('\n').append(d.getMessage(Locale.ROOT));
        }
        String s = all.toString().toLowerCase(Locale.ROOT);
        if (s.contains("__synthesized_input__")) return "synthesized-input";
        if (s.contains("incompatible types")) return "incompatible-types";
        if (s.contains("is not abstract and does not override")) return "abstract-method";
        if (s.contains("no suitable method") || s.contains("cannot be applied")) return "method-args";
        if (s.contains("cannot find symbol")) return "cannot-find-symbol";
        return "other";
    }

    /** In-memory Java source whose URI carries the canonical path (so javac names it). */
    static final class StringSource extends SimpleJavaFileObject {
        private final String code;
        StringSource(String canonicalPath, String code) {
            super(URI.create("string:///" + canonicalPath), Kind.SOURCE);
            this.code = code;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) { return code; }
    }
}
