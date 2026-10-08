package org.finos.rune.benchmarks;

import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import org.finos.rune.benchmarks.corpus.BenchTree;
import org.finos.rune.benchmarks.corpus.CorpusClasses;
import org.finos.rune.benchmarks.corpus.ReflectivePopulator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Headline workload 2 (the plan § 6): the CDM validation sweep on the CURRENT Mapper
 * route — every discoverable cdm-6.20.6 XMeta registry's cardinality validator,
 * type-format validator and data rules, run over deterministically builder-populated
 * instances (synthetic-input baseline; real-sample re-baseline lands with the PR-3
 * leg-S loader). One op = one full sweep across all usable (meta, instance) pairs.
 *
 * <p>Setup prints the census (metas discovered / usable pairs / skips) — those lines
 * are part of the receipt; the denominator is never silently smaller than it looks.
 * LOCAL-ONLY: requires test-corpus/ (fails fast with a recorded reason otherwise).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@State(Scope.Benchmark)
@SuppressWarnings({"rawtypes", "unchecked"})
public class CdmValidationSweepBenchmark {

    record Pair(Validator cardinality, Validator typeFormat, List<Validator> dataRules,
                Object instance) {}

    private List<Pair> pairs;
    private RosettaPath path;
    private URLClassLoader cl;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        if (!CorpusClasses.corpusPresent()) {
            throw new IllegalStateException(
                    "test-corpus/ absent — the CDM validation sweep is a local-only benchmark");
        }
        // The PR-4 before/after seam: -Dbench.optimisedTree=true (fork JVM) swaps the
        // cell to the optimised-emission overlay compile. The tranche does not touch
        // validator kinds, so the sweep's after-leg is expected FLAT — measured, not
        // asserted (the honest-receipt discipline).
        Path out = BenchTree.cellClasses("cdm/cdm-6.20.6", List.of(), "cdm/ingest/");
        cl = CorpusClasses.loaderOver(List.of(out));
        List<String> metaNames = CorpusClasses.classNamesUnder(out,
                n -> n.endsWith("Meta") && n.contains(".meta.") && !n.contains("$"));

        // Default carries an @Inject Injector (validators are guice-instantiated)
        ValidatorFactory factory =
                com.google.inject.Guice.createInjector().getInstance(ValidatorFactory.Default.class);
        ReflectivePopulator populator = new ReflectivePopulator();
        pairs = new ArrayList<>();
        path = RosettaPath.valueOf("Benchmark");
        int notMeta = 0, modelMissing = 0, populateFailed = 0;

        for (String metaName : metaNames) {
            Class<?> metaClass = Class.forName(metaName, true, cl);
            if (!RosettaMetaData.class.isAssignableFrom(metaClass)) { notMeta++; continue; }
            RosettaMetaData meta = (RosettaMetaData) metaClass.getConstructor().newInstance();
            String modelName = metaName.replace(".meta.", ".");
            modelName = modelName.substring(0, modelName.length() - "Meta".length());
            Class<?> modelClass;
            try {
                modelClass = Class.forName(modelName, true, cl);
            } catch (ClassNotFoundException e) {
                modelMissing++;
                continue;
            }
            Object instance = populator.populate(modelClass, 2);
            if (instance == null) { populateFailed++; continue; }
            pairs.add(new Pair(meta.validator(factory), meta.typeFormatValidator(factory),
                    meta.dataRules(factory), instance));
        }
        System.out.printf(
                "[ValidationSweep census] metaClasses=%d usablePairs=%d notMeta=%d "
                        + "modelMissing=%d populateFailed=%d (populator: ok=%d failed=%d "
                        + "setterRejections=%d)%n",
                metaNames.size(), pairs.size(), notMeta, modelMissing, populateFailed,
                populator.populatedCount(), populator.failedCount(),
                populator.setterRejectionCount());
        if (pairs.isEmpty()) {
            throw new IllegalStateException("zero usable (meta, instance) pairs — census above");
        }
    }

    @org.openjdk.jmh.annotations.TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (cl != null) cl.close(); // Windows: release the corpus-classes file locks
    }

    @Benchmark
    public void validationSweep(Blackhole bh) {
        for (Pair p : pairs) {
            bh.consume(p.cardinality().getValidationResults(path, p.instance()));
            bh.consume(p.typeFormat().getValidationResults(path, p.instance()));
            for (Validator dr : p.dataRules()) {
                bh.consume(dr.getValidationResults(path, p.instance()));
            }
        }
    }
}
