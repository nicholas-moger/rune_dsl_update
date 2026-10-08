package demo.harness.exec.corpus;

import demo.harness.exec.DemoPaths;
import demo.harness.exec.LegPins;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The tree selector, adapted from {@code rune-benchmarks/.../corpus/BenchTree.java}. The
 * benchmark version switched between the reference goldens and an optimised-emission
 * overlay; the demo has one tree ({@code demo/work/gen-m1/}, the fork's own m1 output), so
 * what survives is BenchTree's actual discipline: print which tree ran and its content
 * stamp, every time, so a number can never be reported without the tree that produced it.
 *
 * <p>{@code -Ddemo.gen=<dir>} (or {@code --src}) points at a different tree - e.g. the
 * legacy toolchain's {@code demo/work/build-legacy-out/} - if the integrator wants to
 * cross-check that both toolchains' output executes identically.
 */
public final class GenTree {

    private GenTree() {}

    /** Compile (or reuse) the generated tree for {@code leg}. */
    public static CorpusClasses.CompileResult compileFor(String leg, Path srcRoot, Path outDir,
                                                         String excludeContains, boolean force) {
        CorpusClasses.CompileResult r = CorpusClasses.ensureCompiled(
                srcRoot, outDir, excludeContains, pinsLabel(), force);
        System.out.println("[GenTree] leg=" + leg + " src=" + srcRoot + " stamp={" + r.stamp()
                + "} sources=" + r.sources() + " excluded=" + r.excluded()
                + " reused=" + r.reused() + " -> " + r.classesDir());
        return r;
    }

    /**
     * Classes compiled against one leg's runtime must never be reused for the other, so the
     * leg's pins are part of the marker identity.
     */
    public static String pinsLabel() {
        return LegPins.LEG + "|" + LegPins.RUNTIME_RESOLVED + "|jackson=" + LegPins.JACKSON_VERSION
                + "|commons-lang3=" + LegPins.COMMONS_LANG3_VERSION
                + "|guice=" + LegPins.GUICE_VERSION;
    }

    /** The stamp recorded by the last successful compile for {@code leg}, or "none". */
    public static String recordedStamp(String leg) {
        Path marker = DemoPaths.classesRoot(leg).resolve(CorpusClasses.MARKER_NAME);
        try {
            if (Files.isRegularFile(marker)) {
                String text = Files.readString(marker, StandardCharsets.UTF_8).trim();
                int i = text.indexOf("stamp=");
                if (i >= 0) {
                    int end = text.indexOf(' ', i);
                    return end < 0 ? text.substring(i + 6) : text.substring(i + 6, end);
                }
            }
        } catch (Exception ignored) {
            // an unreadable marker is reported, never thrown from a describe-only helper
        }
        return "none";
    }

    /**
     * The marker's pins field, so a run can prove the classes it is about to execute were
     * compiled against THIS jar's leg. Returns "none" when there is no marker.
     */
    public static String recordedPins(String leg) {
        Path marker = DemoPaths.classesRoot(leg).resolve(CorpusClasses.MARKER_NAME);
        try {
            if (Files.isRegularFile(marker)) {
                String text = Files.readString(marker, StandardCharsets.UTF_8).trim();
                int i = text.indexOf("pins={");
                if (i >= 0) {
                    int end = text.indexOf('}', i);
                    if (end > i) {
                        return text.substring(i + 6, end);
                    }
                }
            }
        } catch (Exception ignored) {
            // same: describe-only
        }
        return "none";
    }
}
