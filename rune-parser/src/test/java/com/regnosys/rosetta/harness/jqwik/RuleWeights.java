package com.regnosys.rosetta.harness.jqwik;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Rule-level weights for biasing {@link AtnStringGenerator}'s choice
 * among outgoing transitions.
 *
 * <p>P1.2 audit hook H18.2. Loaded from a classpath properties file
 * ({@value #DEFAULT_RESOURCE} by default), one entry per parser rule.
 * Keys must match entries in {@link
 * com.regnosys.rosetta.parser.RosettaParser#getRuleNames()
 * RosettaParser.getRuleNames()} — unknown keys are silently ignored:
 *
 * <pre>
 * expression=27463
 * typeCall=6850
 * attribute=5911
 * enumeration=262
 * </pre>
 *
 * <p>A rule whose name does not appear in the file uses
 * {@value #DEFAULT_WEIGHT} (uniform). Weights are relative, not
 * probabilities — the generator normalises them at every decision
 * point.
 *
 * <p>Values are floored at {@value #MIN_WEIGHT}: a weight of zero would
 * make the generator silently skip a rule even when the grammar requires
 * it, which is usually a misconfiguration rather than a modelling choice.
 *
 * <p>Source of truth: {@link CorpusRuleCounter} produces a properties
 * file by walking parse trees of a target corpus and tallying each
 * rule invocation. The committed file is derived from one CATALOGUE
 * version (see the file header for which one); regenerate with the
 * disabled-by-default test in {@code CorpusRuleCounter}.
 */
public final class RuleWeights {

    public static final String DEFAULT_RESOURCE = "/property-weights.properties";
    public static final double DEFAULT_WEIGHT = 1.0;
    public static final double MIN_WEIGHT = 0.001;

    private final Map<Integer, Double> weightsByRuleIndex;

    private RuleWeights(Map<Integer, Double> weightsByRuleIndex) {
        this.weightsByRuleIndex = Map.copyOf(weightsByRuleIndex);
    }

    /** Uniform weights — every rule has weight {@value #DEFAULT_WEIGHT}. */
    public static RuleWeights uniform() {
        return new RuleWeights(Collections.emptyMap());
    }

    /**
     * Load weights from {@link #DEFAULT_RESOURCE} on the classpath. If
     * the resource is absent, falls back to {@link #uniform()} — callers
     * get predictable behaviour even in a dev setup that hasn't yet
     * generated the file.
     */
    public static RuleWeights fromDefaultResource(String[] ruleNames) {
        try (InputStream in = RuleWeights.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) return uniform();
            return fromProperties(loadProperties(in), ruleNames);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "failed to load " + DEFAULT_RESOURCE + ": " + e.getMessage(), e);
        }
    }

    /**
     * Load a UTF-8-encoded properties stream. {@link Properties#load(InputStream)}
     * decodes as ISO-8859-1 per the properties-file spec, but
     * {@code property-weights.properties} is written in UTF-8 by
     * {@link CorpusRuleCounter} (header comment contains em-dashes, and
     * nothing prevents a future non-ASCII key). Going through a
     * {@link Reader} with an explicit charset keeps the decoder aligned
     * with the writer. Mirrors the fix applied to
     * {@code StructuralBaselines.fromDefaultResource} in H26b rd-1 F2.
     */
    static Properties loadProperties(InputStream in) throws IOException {
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return props;
    }

    /**
     * Build from an explicit {@link Properties} (useful for tests).
     * {@code ruleNames} is the parser's {@code getRuleNames()} array —
     * entries whose key doesn't match a known rule are ignored (with no
     * error: the grammar may add / drop rules over time and weights
     * should degrade gracefully).
     */
    public static RuleWeights fromProperties(Properties props, String[] ruleNames) {
        List<String> names = Arrays.asList(ruleNames);
        Map<Integer, Double> weights = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            int ruleIndex = names.indexOf(key);
            if (ruleIndex < 0) continue;
            double value = parseWeight(props.getProperty(key));
            // Reject NaN and +/-Infinity — `Double.parseDouble` accepts
            // "NaN" and "Infinity" literally, but a non-finite weight
            // would collapse the cumulative-distribution pick in
            // AtnStringGenerator (total = +Inf makes every cumulative
            // entry +Inf, pick never satisfies pick < cumulative[i], and
            // the walker degenerates to always picking the last
            // transition). Silently dropping is consistent with the
            // class's other "degrade gracefully on bad input" calls.
            if (!Double.isFinite(value)) continue;
            weights.put(ruleIndex, Math.max(MIN_WEIGHT, value));
        }
        return new RuleWeights(weights);
    }

    /**
     * Weight for a parser rule index. Returns {@value #DEFAULT_WEIGHT}
     * when the rule isn't in the loaded map.
     */
    public double weightFor(int ruleIndex) {
        return weightsByRuleIndex.getOrDefault(ruleIndex, DEFAULT_WEIGHT);
    }

    /** Number of rule-specific weights loaded (excludes defaults). */
    public int size() {
        return weightsByRuleIndex.size();
    }

    private static double parseWeight(String raw) {
        if (raw == null) return Double.NaN;
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
