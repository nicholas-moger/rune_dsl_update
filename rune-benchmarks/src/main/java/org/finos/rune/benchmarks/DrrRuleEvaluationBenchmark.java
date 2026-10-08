package org.finos.rune.benchmarks;

import com.google.inject.Guice;
import com.google.inject.Injector;
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

import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Headline workload 1 (the plan § 6): DRR rule-evaluation throughput on the CURRENT
 * Mapper route — every drr-6.34.1 {@code *ReportFunction} that (a) Guice-instantiates
 * with the runtime's default bindings ({@code DefaultConditionValidator} — conditions
 * ENFORCED and throwing — plus {@code NoOpModelObjectValidator}), (b) gets a
 * builder-populated input, and (c) survives a one-shot probe evaluation, is evaluated
 * once per op. Synthetic-input baseline (real samples land with the PR-3 leg-S
 * loader); the probe-survivor census is printed and is part of the receipt.
 *
 * <p>LOCAL-ONLY: needs test-corpus/ (cdm-5.38.0 + iso20022-1.38.0 + drr-6.34.1 —
 * the DRR composite-workspace closure per test-corpus/CATALOGUE.md).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(2)
@State(Scope.Benchmark)
public class DrrRuleEvaluationBenchmark {

    record Runner(String name, Object fn, Method evaluate, Object input) {}

    private List<Runner> runners;
    private URLClassLoader cl;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        if (!CorpusClasses.corpusPresent()) {
            throw new IllegalStateException(
                    "test-corpus/ absent — DRR rule evaluation is a local-only benchmark");
        }
        Path cdm5 = CorpusClasses.ensureCompiled("cdm/cdm-5.38.0", List.of());
        Path iso = CorpusClasses.ensureCompiled("iso20022/iso20022-1.38.0", List.of());
        // The PR-4 before/after seam: -Dbench.optimisedTree=true (fork JVM) swaps the
        // DRR cell to the optimised-emission overlay compile; upstream cells stay
        // reference (the tranche re-emits drr's own functions only).
        Path drr = BenchTree.cellClasses("drr/drr-6.34.1", List.of(cdm5, iso), null);
        cl = CorpusClasses.loaderOver(List.of(cdm5, iso, drr));

        List<String> fnNames = CorpusClasses.classNamesUnder(drr,
                n -> n.endsWith("ReportFunction") && !n.contains("$"));
        Injector injector = Guice.createInjector();
        ReflectivePopulator populator = new ReflectivePopulator();
        runners = new ArrayList<>();
        int injectFailed = 0, noEvaluate = 0, inputFailed = 0, probeFailed = 0;

        for (String name : fnNames) {
            Class<?> cls = Class.forName(name, true, cl);
            Object fn;
            try {
                fn = injector.getInstance(cls);
            } catch (Throwable t) {
                injectFailed++;
                continue;
            }
            // min over param-type name: getMethods() order is JVM-unspecified, and the
            // survivor census must pick the same overload every run
            Method evaluate = Arrays.stream(cls.getMethods())
                    .filter(m -> m.getName().equals("evaluate") && m.getParameterCount() == 1
                            && m.getParameterTypes()[0] != Object.class)
                    .min(java.util.Comparator.comparing(m -> m.getParameterTypes()[0].getName()))
                    .orElse(null);
            if (evaluate == null) { noEvaluate++; continue; }
            Object input = populator.populate(evaluate.getParameterTypes()[0], 2);
            if (input == null) { inputFailed++; continue; }
            try {
                evaluate.invoke(fn, input);
            } catch (Throwable t) {
                probeFailed++;
                continue;
            }
            runners.add(new Runner(name, fn, evaluate, input));
        }
        System.out.printf(
                "[DrrRuleEval census] reportFunctions=%d survivors=%d injectFailed=%d "
                        + "noEvaluate=%d inputFailed=%d probeFailed=%d%n",
                fnNames.size(), runners.size(), injectFailed, noEvaluate, inputFailed,
                probeFailed);
        if (runners.isEmpty()) {
            throw new IllegalStateException("zero probe-surviving report functions — census above");
        }
    }

    @org.openjdk.jmh.annotations.TearDown(Level.Trial)
    public void tearDown() throws Exception {
        if (cl != null) cl.close(); // Windows: release the corpus-classes file locks
    }

    @Benchmark
    public void reportEvaluation(Blackhole bh) throws Exception {
        for (Runner r : runners) {
            bh.consume(r.evaluate().invoke(r.fn(), r.input()));
        }
    }
}
