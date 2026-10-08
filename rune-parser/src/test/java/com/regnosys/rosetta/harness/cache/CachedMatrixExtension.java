package com.regnosys.rosetta.harness.cache;

import com.regnosys.rosetta.harness.matrix.Canonicaliser;
import com.regnosys.rosetta.harness.matrix.MatrixCoordinate;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * JUnit {@link InvocationInterceptor} that consults a {@link LocalDiskCache}
 * before invoking a {@code @ParameterizedTest} or {@code @Test} method, and
 * records the outcome on miss.
 *
 * <p>P1.2 audit hook H24.2 — the behaviour layer on top of the H24 cache
 * foundation. A PASSED cache hit calls {@code Invocation.skip()}, which
 * JUnit treats as a successful no-op invocation (count-stays-PASSED; no
 * SKIPPED entry in reports). A miss, or a non-PASSED hit, runs the test
 * normally and writes the outcome back to the cache.
 *
 * <h2>Cache key</h2>
 * Per {@link CacheKey}:
 * <ul>
 *   <li>{@code inputSha} — SHA-256 of the canonicalised
 *       ({@link Canonicaliser}) contents of {@link MatrixCoordinate#source()}.
 *       BOM-strip + CRLF→LF means Windows dev and Linux CI produce
 *       identical keys for the same logical input.
 *   <li>{@code testBytecodeSha} — SHA-256 of the test class's .class
 *       file bytes, read from the class loader. Any edit to the test body
 *       invalidates every cached entry for the class — fail-closed.
 *   <li>{@code toolVersionSha} — SHA-256 of {@link #toolVersion()} (stable
 *       string, bumped by hand when a toolchain change should invalidate
 *       the cache; deriving from pom / git SHA is a P1.3 follow-up).
 * </ul>
 *
 * <h2>Mode (audit Q12)</h2>
 * Controlled by sys prop {@code matrix.cache.mode} (default
 * {@link Mode#READ_WRITE}):
 * <ul>
 *   <li>{@link Mode#READ_WRITE} — local dev / post-merge main
 *   <li>{@link Mode#READ_ONLY} — PR CI (reads ground-truth written nightly)
 *   <li>{@link Mode#WRITE_ONLY} — nightly / release runs that replace cache
 *   <li>{@link Mode#DISABLED} — escape hatch; extension is a no-op
 * </ul>
 *
 * <h2>Usage</h2>
 * {@snippet :
 * @ExtendWith(CachedMatrixExtension.class)
 * class MyTest {
 *     @ParameterizedTest
 *     @ArgumentsSource(MatrixCellsProvider.class)
 *     void run(MatrixCoordinate cell) { ... }
 * }
 * }
 *
 * <p>Tests that don't take a {@link MatrixCoordinate} argument are
 * passed through untouched — the extension is a no-op for non-matrix
 * invocations.
 */
public final class CachedMatrixExtension implements InvocationInterceptor {

    public enum Mode { READ_WRITE, READ_ONLY, WRITE_ONLY, DISABLED }

    /** Toolchain marker — bumped by hand to force a full cache invalidation. */
    static final String TOOL_VERSION = "rune-dsl-plus-p1.2-h24.2-v1";

    /** Precomputed once — avoids re-hashing the tool-version constant per invocation. */
    private static final String TOOL_VERSION_SHA = Digest.sha256String(TOOL_VERSION);

    /**
     * Per-class bytecode SHA cache. A large {@code @ParameterizedTest}
     * matrix invokes this hook once per cell (1,617× for the current
     * CorpusParseTest); reading + hashing the class bytes each time adds
     * avoidable I/O. The cache is populated lazily via
     * {@link ConcurrentMap#computeIfAbsent}, which is thread-safe for
     * idempotent loaders.
     */
    private static final ConcurrentMap<Class<?>, String> BYTECODE_SHA_CACHE = new ConcurrentHashMap<>();

    /** System-prop key controlling {@link Mode}. */
    public static final String MODE_PROPERTY = "matrix.cache.mode";

    /** System-prop key overriding the on-disk cache root. */
    public static final String ROOT_PROPERTY = "matrix.cache.root";

    /**
     * Default cache root resolved against Surefire's CWD for the
     * {@code rune-parser/} module.
     */
    public static final Path DEFAULT_ROOT = Path.of("target", "matrix-cache");

    private final LocalDiskCache cache;
    private final Mode mode;

    /** Required by JUnit's {@code @ExtendWith} contract. Reads sys props. */
    public CachedMatrixExtension() {
        this(LocalDiskCache.at(resolveRoot()), resolveMode());
    }

    /** Visible for tests — inject a hermetic cache + mode. */
    CachedMatrixExtension(LocalDiskCache cache, Mode mode) {
        this.cache = Objects.requireNonNull(cache, "cache");
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    @Override
    public void interceptTestTemplateMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        handle(invocation, invocationContext);
    }

    @Override
    public void interceptTestMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        handle(invocation, invocationContext);
    }

    // ---- core dispatch ----

    private void handle(Invocation<Void> invocation,
                        ReflectiveInvocationContext<Method> ctx) throws Throwable {
        if (mode == Mode.DISABLED) { invocation.proceed(); return; }

        Optional<MatrixCoordinate> coord = extractCoordinate(ctx);
        if (coord.isEmpty()) { invocation.proceed(); return; }

        CacheKey key = computeKey(coord.get(), ctx);

        if (mode == Mode.READ_WRITE || mode == Mode.READ_ONLY) {
            Optional<CacheEntry> hit = cache.get(key);
            if (hit.isPresent() && hit.get().outcome() == CacheEntry.Outcome.PASSED) {
                invocation.skip();
                return;
            }
        }

        // Miss / non-PASSED hit / WRITE_ONLY — run it. nanoTime is monotonic;
        // currentTimeMillis can jump backwards under NTP adjustment and produce
        // a negative duration that would throw from CacheEntry's validator.
        long startNanos = System.nanoTime();
        try {
            invocation.proceed();
        } catch (Throwable t) {
            if (mode == Mode.READ_WRITE || mode == Mode.WRITE_ONLY) {
                cache.put(CacheEntry.failed(key, elapsedMs(startNanos), describeThrowable(t)));
            }
            throw t;
        }
        if (mode == Mode.READ_WRITE || mode == Mode.WRITE_ONLY) {
            cache.put(CacheEntry.passed(key, elapsedMs(startNanos)));
        }
    }

    private static long elapsedMs(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

    // ---- seams (package-visible for unit tests) ----

    static Optional<MatrixCoordinate> extractCoordinate(ReflectiveInvocationContext<Method> ctx) {
        for (Object arg : ctx.getArguments()) {
            if (arg instanceof MatrixCoordinate mc) return Optional.of(mc);
        }
        return Optional.empty();
    }

    static CacheKey computeKey(MatrixCoordinate coord, ReflectiveInvocationContext<Method> ctx) {
        String inputSha = sha256OfCanonicalisedFile(coord.source());
        String testBytecodeSha = BYTECODE_SHA_CACHE.computeIfAbsent(
                ctx.getTargetClass(), CachedMatrixExtension::sha256OfClassBytes);
        return new CacheKey(inputSha, testBytecodeSha, TOOL_VERSION_SHA);
    }

    static String sha256OfCanonicalisedFile(Path p) {
        try {
            String raw = Files.readString(p, StandardCharsets.UTF_8);
            return Digest.sha256String(Canonicaliser.canonicalise(raw));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String sha256OfClassBytes(Class<?> clazz) {
        String resource = clazz.getName().replace('.', '/') + ".class";
        ClassLoader cl = clazz.getClassLoader();
        if (cl == null) {
            // Bootstrap class — extremely unlikely for a test class, but
            // degrade to a stable marker rather than throwing.
            return Digest.sha256String("bootstrap:" + clazz.getName());
        }
        try (InputStream in = cl.getResourceAsStream(resource)) {
            if (in == null) return Digest.sha256String("missing:" + clazz.getName());
            return Digest.sha256(in.readAllBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String toolVersion() {
        return TOOL_VERSION;
    }

    static Mode resolveMode() {
        String raw = System.getProperty(MODE_PROPERTY);
        if (raw == null) return Mode.READ_WRITE;
        try {
            return Mode.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Mode.READ_WRITE;
        }
    }

    static Path resolveRoot() {
        String raw = System.getProperty(ROOT_PROPERTY);
        return raw == null || raw.isEmpty() ? DEFAULT_ROOT : Path.of(raw);
    }

    /** Format a {@link Throwable} as a short stable string for cache messages. */
    static String describeThrowable(Throwable t) {
        return t.getClass().getName() + ": " + (t.getMessage() == null ? "" : t.getMessage());
    }
}
