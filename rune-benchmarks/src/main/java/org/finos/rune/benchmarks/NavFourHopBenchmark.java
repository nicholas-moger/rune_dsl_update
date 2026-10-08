package org.finos.rune.benchmarks;

import com.rosetta.model.lib.mapper.MapperS;
import org.finos.rune.benchmarks.navmodel.NavModel;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

/**
 * The four-hop navigation datum (the plan § 6): the SAME navigation expressed as
 * (a) the generated Mapper idiom ({@code MapperS.of(..).map("name", ref)} per hop —
 * byte-shape of the REFERENCE bodies), (b) the ideal bare direct ladder (the nav
 * card's zero-wrapper end-state — the TARGET the § 6.3 consumer re-typing road
 * would reach), and (c) the AS-SHIPPED v3 form (PR-9): what the landed
 * §§ 9a/9b/10a/12 conversions actually emit at a converted SINGLE seat — the
 * emitter's accumulated-prefix null-guard ladder wrapped in ONE
 * {@code MapperS.of((..))} boundary plus the runtime-identity
 * {@code filterSingle(sameNav -> true)} suffix (census § 5a/§ 9a consumption-shape
 * contract), consumed through the boundary {@code .get()}. The ladder text mirrors
 * {@code NavigationChainClassifier.LadderPlan#ladder} verbatim: each guard
 * re-spells the full accumulated prefix (an expression seat cannot introduce
 * locals); the re-invoked getters are trivial field reads the JIT inlines and
 * CSEs. Run with {@code -prof gc} for the allocation half of the datum
 * (~25 allocations/op historically vs 1 boundary wrap vs 0).
 *
 * <p>Miss variants break the chain at hop 2 — Mapper wraps every hop regardless,
 * the shipped and direct ladders short-circuit; all three must return null
 * (semantic pairing locked by {@code NavModelEquivalenceTest}).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Benchmark)
public class NavFourHopBenchmark {

    private NavModel.Root full;
    private NavModel.Root broken;

    @Setup
    public void setup() {
        full = NavModel.fullChain();
        broken = NavModel.brokenAtHop2();
    }

    @Benchmark
    public BigDecimal mapperFourHop() {
        return MapperS.of(full)
                .<NavModel.Trade>map("getTrade", NavModel.Root::getTrade)
                .<NavModel.Product>map("getProduct", NavModel.Trade::getProduct)
                .<NavModel.Payout>map("getPayout", NavModel.Product::getPayout)
                .<BigDecimal>map("getNotional", NavModel.Payout::getNotional)
                .get();
    }

    @Benchmark
    public BigDecimal directFourHop() {
        NavModel.Root r = full;
        if (r == null) return null;
        NavModel.Trade t = r.getTrade();
        if (t == null) return null;
        NavModel.Product p = t.getProduct();
        if (p == null) return null;
        NavModel.Payout po = p.getPayout();
        if (po == null) return null;
        return po.getNotional();
    }

    @Benchmark
    public BigDecimal shippedFourHop() {
        NavModel.Root root = full;
        return MapperS.of((root == null ? null
                : root.getTrade() == null ? null
                : root.getTrade().getProduct() == null ? null
                : root.getTrade().getProduct().getPayout() == null ? null
                : root.getTrade().getProduct().getPayout().getNotional()))
                .filterSingle(sameNav -> true)
                .get();
    }

    @Benchmark
    public BigDecimal mapperFourHopMiss() {
        return MapperS.of(broken)
                .<NavModel.Trade>map("getTrade", NavModel.Root::getTrade)
                .<NavModel.Product>map("getProduct", NavModel.Trade::getProduct)
                .<NavModel.Payout>map("getPayout", NavModel.Product::getPayout)
                .<BigDecimal>map("getNotional", NavModel.Payout::getNotional)
                .get();
    }

    @Benchmark
    public BigDecimal shippedFourHopMiss() {
        NavModel.Root root = broken;
        return MapperS.of((root == null ? null
                : root.getTrade() == null ? null
                : root.getTrade().getProduct() == null ? null
                : root.getTrade().getProduct().getPayout() == null ? null
                : root.getTrade().getProduct().getPayout().getNotional()))
                .filterSingle(sameNav -> true)
                .get();
    }

    @Benchmark
    public BigDecimal directFourHopMiss() {
        NavModel.Root r = broken;
        if (r == null) return null;
        NavModel.Trade t = r.getTrade();
        if (t == null) return null;
        NavModel.Product p = t.getProduct();
        if (p == null) return null;
        NavModel.Payout po = p.getPayout();
        if (po == null) return null;
        return po.getNotional();
    }
}
