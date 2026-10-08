package org.finos.rune.benchmarks;

import com.rosetta.model.lib.mapper.MapperS;
import org.finos.rune.benchmarks.navmodel.NavModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Locks the semantic pairing behind {@code NavFourHopBenchmark}: the Mapper idiom,
 * the direct null-ladder AND the as-shipped v3 form (the emitter's
 * accumulated-prefix ladder + the {@code MapperS.of((..)).filterSingle} boundary,
 * PR-9's third rung) must produce the SAME value on the same chain, hit or miss —
 * otherwise the benchmark compares different computations and the datum is void.
 */
class NavModelEquivalenceTest {

    private static BigDecimal mapperNav(NavModel.Root root) {
        return MapperS.of(root)
                .<NavModel.Trade>map("getTrade", NavModel.Root::getTrade)
                .<NavModel.Product>map("getProduct", NavModel.Trade::getProduct)
                .<NavModel.Payout>map("getPayout", NavModel.Product::getPayout)
                .<BigDecimal>map("getNotional", NavModel.Payout::getNotional)
                .get();
    }

    private static BigDecimal directNav(NavModel.Root root) {
        if (root == null) return null;
        NavModel.Trade t = root.getTrade();
        if (t == null) return null;
        NavModel.Product p = t.getProduct();
        if (p == null) return null;
        NavModel.Payout po = p.getPayout();
        if (po == null) return null;
        return po.getNotional();
    }

    private static BigDecimal shippedNav(NavModel.Root root) {
        return MapperS.of((root == null ? null
                : root.getTrade() == null ? null
                : root.getTrade().getProduct() == null ? null
                : root.getTrade().getProduct().getPayout() == null ? null
                : root.getTrade().getProduct().getPayout().getNotional()))
                .filterSingle(sameNav -> true)
                .get();
    }

    @Test
    void fullChainAgreesAndResolves() {
        NavModel.Root full = NavModel.fullChain();
        assertEquals(new BigDecimal("1000000.00"), mapperNav(full));
        assertEquals(mapperNav(full), directNav(full));
        assertEquals(mapperNav(full), shippedNav(full));
    }

    @Test
    void brokenChainAgreesOnNull() {
        NavModel.Root broken = NavModel.brokenAtHop2();
        assertNull(mapperNav(broken));
        assertNull(directNav(broken));
        assertNull(shippedNav(broken));
    }

    @Test
    void nullRootAgreesOnNull() {
        assertNull(mapperNav(null));
        assertNull(directNav(null));
        assertNull(shippedNav(null));
    }
}
