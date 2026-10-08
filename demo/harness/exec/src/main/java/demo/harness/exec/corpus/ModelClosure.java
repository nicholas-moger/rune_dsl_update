package demo.harness.exec.corpus;

import com.google.inject.Guice;
import com.google.inject.Injector;
import demo.harness.exec.DemoPaths;
import demo.harness.exec.LegPins;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The compiled generated closure for one leg, open for execution: a class loader over
 * {@code demo/work/exec-classes/<leg>/}, the DEFAULT Guice injector (no modules - so
 * {@code ModelObjectValidator} binds to {@code NoOpModelObjectValidator} and conditions run
 * through {@code DefaultConditionValidator}, the same bindings the reference benchmarks
 * measure), and the sorted class-name census, walked once and cached.
 *
 * <p>Opening REQUIRES a prior {@code setup --leg <leg>}: a workload must never silently
 * compile inside a measured run, and a compile that happens under JMH would land inside a
 * fork's trial. Opening also checks the marker's pins against this jar's, so classes built
 * against the other leg's runtime cannot be executed here by accident.
 */
public final class ModelClosure implements AutoCloseable {

    private final String leg;
    private final Path classesDir;
    private final URLClassLoader loader;
    private final Injector injector;
    private final String stamp;
    private List<String> allClassNames;

    private ModelClosure(String leg, Path classesDir, URLClassLoader loader, String stamp) {
        this.leg = leg;
        this.classesDir = classesDir;
        this.loader = loader;
        this.stamp = stamp;
        this.injector = Guice.createInjector();
    }

    /** Open the precompiled closure for {@code leg}; throws with a fix-it message if absent. */
    public static ModelClosure open(String leg) {
        LegPins.requireLeg(leg);
        Path dir = DemoPaths.classesRoot(leg);
        Path marker = dir.resolve(CorpusClasses.MARKER_NAME);
        if (!Files.isDirectory(dir) || !Files.isRegularFile(marker)) {
            throw new IllegalStateException("no compiled closure for leg " + leg + " at " + dir
                    + " - run 'setup --leg " + leg + "' first");
        }
        String recordedPins = GenTree.recordedPins(leg);
        String expectedPins = GenTree.pinsLabel();
        if (!expectedPins.equals(recordedPins)) {
            throw new IllegalStateException("the compiled closure at " + dir
                    + " was built against a DIFFERENT dependency set"
                    + "\n  marker: " + recordedPins
                    + "\n  this jar: " + expectedPins
                    + "\n  re-run 'setup --leg " + leg + "' with this jar");
        }
        URLClassLoader cl = CorpusClasses.loaderOver(List.of(dir));
        return new ModelClosure(leg, dir, cl, GenTree.recordedStamp(leg));
    }

    public String leg() {
        return leg;
    }

    public Path classesDir() {
        return classesDir;
    }

    public String stamp() {
        return stamp;
    }

    public Injector injector() {
        return injector;
    }

    public ClassLoader loader() {
        return loader;
    }

    /** Every top-level compiled class name, sorted; walked once. */
    public List<String> allClassNames() {
        if (allClassNames == null) {
            allClassNames = CorpusClasses.classNamesUnder(classesDir, n -> n.indexOf('$') < 0);
        }
        return allClassNames;
    }

    public List<String> classNames(Predicate<String> filter) {
        List<String> out = new ArrayList<>();
        for (String n : allClassNames()) {
            if (filter.test(n)) {
                out.add(n);
            }
        }
        return out;
    }

    public Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, true, loader);
    }

    /** Provenance every receipt carries: which tree, which pins, how many classes. */
    public Map<String, Object> describe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("leg", leg);
        m.put("classesDir", classesDir.toString().replace('\\', '/'));
        m.put("treeStamp", stamp);
        m.put("compiledClasses", allClassNames().size());
        m.put("runtime", LegPins.RUNTIME_RESOLVED);
        m.put("jacksonVersion", LegPins.JACKSON_VERSION);
        m.put("commonsLang3Version", LegPins.COMMONS_LANG3_VERSION);
        m.put("guiceVersion", LegPins.GUICE_VERSION);
        return m;
    }

    @Override
    public void close() {
        try {
            loader.close(); // Windows: release the exec-classes file locks
        } catch (IOException ignored) {
            // closing is best effort; the process is ending anyway
        }
    }
}
