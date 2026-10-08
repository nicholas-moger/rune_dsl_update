package org.finos.rune.benchmarks.navmodel;

import java.math.BigDecimal;

/**
 * Hand-written four-hop navigation fixture mirroring the generated POJO getter
 * shape (nullable references, plain getters): Root -> Trade -> Product -> Payout
 * -> BigDecimal notional. Used by {@code NavFourHopBenchmark} to re-anchor the
 * Mapper-vs-direct per-navigation datum (the development IR page § Phase 2) on this box.
 *
 * <p>Deliberately NOT generated code: the datum isolates the Mapper machinery's
 * per-hop cost from everything else, so the model must be the minimal stable
 * substrate both shapes navigate identically.
 */
public final class NavModel {

    private NavModel() {}

    public static final class Root {
        private final Trade trade;
        public Root(Trade trade) { this.trade = trade; }
        public Trade getTrade() { return trade; }
    }

    public static final class Trade {
        private final Product product;
        public Trade(Product product) { this.product = product; }
        public Product getProduct() { return product; }
    }

    public static final class Product {
        private final Payout payout;
        public Product(Payout payout) { this.payout = payout; }
        public Payout getPayout() { return payout; }
    }

    public static final class Payout {
        private final BigDecimal notional;
        public Payout(BigDecimal notional) { this.notional = notional; }
        public BigDecimal getNotional() { return notional; }
    }

    /** A fully-populated chain: every hop resolves, the leaf is non-null. */
    public static Root fullChain() {
        return new Root(new Trade(new Product(new Payout(new BigDecimal("1000000.00")))));
    }

    /** A chain broken at hop 2: Trade exists but its Product is null. */
    public static Root brokenAtHop2() {
        return new Root(new Trade(null));
    }
}
