package demo.harness.legacy;

import org.eclipse.xtext.generator.JavaIoFileSystemAccess;
import org.eclipse.xtext.parser.IEncodingProvider;
import org.eclipse.xtext.resource.IResourceServiceProvider;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link JavaIoFileSystemAccess} that records when the FIRST generated file is written and
 * how many writes happened, so the legacy leg can report the same
 * {@code jvmStartToFirstFileMs} the fork leg reports.
 *
 * <p>Only the three-argument {@code generateFile(String, String, CharSequence)} is overridden.
 * That is deliberate and verified against the 9.83.0 Xtext jar: {@code IFileSystemAccess}
 * declares both {@code generateFile(String, CharSequence)} and the three-argument form,
 * {@code AbstractFileSystemAccess} implements the two-argument one by delegating to the
 * three-argument one with {@code DEFAULT_OUTPUT}, and {@code JavaIoFileSystemAccess} overrides
 * ONLY the three-argument form -- which is where it actually writes. Hooking just that one
 * therefore sees every write exactly once; hooking both would double-count.
 *
 * <p>The write count is reported ALONGSIDE, not instead of, a count of the files actually on
 * disk afterwards. If they disagree the run says so rather than picking a winner.
 */
public final class CountingFsa extends JavaIoFileSystemAccess {

    private final long jvmStartMillis;
    private final AtomicInteger writes = new AtomicInteger();
    private volatile long firstFileMs = -1;

    /**
     * INTEGRATOR: this two-argument super constructor is VERIFIED present in
     * {@code org.eclipse.xtext:org.eclipse.xtext:2.38.0} (descriptor
     * {@code (Lorg/eclipse/xtext/resource/IResourceServiceProvider$Registry;
     * Lorg/eclipse/xtext/parser/IEncodingProvider;)V}, annotated {@code @com.google.inject.Inject}).
     * A four-argument overload taking trace providers also exists; if a future Xtext removes
     * the short form, obtain the two extra arguments from the injector as well.
     */
    public CountingFsa(IResourceServiceProvider.Registry registry,
                       IEncodingProvider encodingProvider,
                       long jvmStartMillis) {
        super(registry, encodingProvider);
        this.jvmStartMillis = jvmStartMillis;
    }

    @Override
    public void generateFile(String fileName, String outputConfigName, CharSequence contents) {
        if (firstFileMs < 0) {
            firstFileMs = System.currentTimeMillis() - jvmStartMillis;
            DemoOut.metric("jvmStartToFirstFileMs", firstFileMs);
        }
        int n = writes.incrementAndGet();
        if (n % 250 == 0) {
            // "of" is not knowable up front on this leg -- upstream does not tell the caller
            // how many files it intends to emit -- so 0 is reported rather than a guess.
            DemoOut.progress(n, 0);
        }
        super.generateFile(fileName, outputConfigName, contents);
    }

    /** JVM start to first generated file, in ms, or {@code -1} if nothing was generated. */
    public long firstFileMs() {
        return firstFileMs;
    }

    /** Number of {@code generateFile} calls this access served. */
    public int writes() {
        return writes.get();
    }
}
