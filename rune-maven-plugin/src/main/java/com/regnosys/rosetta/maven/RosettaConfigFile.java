package com.regnosys.rosetta.maven;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reader for the consumer-side {@code rosetta-config.yml} the released 9.83.0
 * plugin accepts through its {@code rosettaConfig} parameter. Two surfaces are
 * honored, matching what the upstream file-based configuration provider feeds
 * the generator:
 *
 * <ul>
 *   <li>{@code generators.namespaces} — the emission accept-list, applied with
 *       {@link NamespaceFilter} (upstream-exact semantics). Absent section →
 *       allow-all, upstream's default {@code n -> true}.</li>
 *   <li>{@code generators.doNotPrune} — {@code {type, attribute}} entries,
 *       normalized to {@code Type#attribute} keys exactly as the generator's
 *       corpus gate reads them (see {@code D11CorpusRegressionTest#readDoNotPrune}).</li>
 * </ul>
 *
 * <p>Other sections ({@code model.name}, {@code dependencies}, ...) are
 * metadata the generation path does not consume; they parse and are ignored,
 * so any real consumer file loads unchanged.
 */
public final class RosettaConfigFile {

    private final NamespaceFilter namespaceFilter;
    private final Set<String> doNotPrune;

    private RosettaConfigFile(NamespaceFilter namespaceFilter, Set<String> doNotPrune) {
        this.namespaceFilter = namespaceFilter;
        this.doNotPrune = doNotPrune;
    }

    /** The no-config default: allow-all namespaces, empty doNotPrune. */
    public static RosettaConfigFile defaults() {
        return new RosettaConfigFile(new NamespaceFilter(List.of()), Set.of());
    }

    public static RosettaConfigFile load(Path yml) {
        try (InputStream in = Files.newInputStream(yml)) {
            // SafeConstructor: plain scalars/maps/lists only — the config is
            // pure data; YAML global tags must never instantiate types here.
            Object root = new Yaml(new SafeConstructor(new LoaderOptions())).load(in);
            if (!(root instanceof Map<?, ?> rootMap)) {
                return defaults();
            }
            Object generators = rootMap.get("generators");
            if (!(generators instanceof Map<?, ?> genMap)) {
                return defaults();
            }
            return new RosettaConfigFile(
                    new NamespaceFilter(readNamespaces(genMap)),
                    readDoNotPrune(genMap));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed reading rosetta config " + yml, e);
        }
    }

    private static List<String> readNamespaces(Map<?, ?> genMap) {
        Object namespaces = genMap.get("namespaces");
        if (!(namespaces instanceof List<?> entries)) {
            return List.of();
        }
        List<String> patterns = new ArrayList<>();
        for (Object entry : entries) {
            if (entry instanceof String s) {
                patterns.add(s);
            } else {
                throw new IllegalArgumentException(
                        "generators.namespaces entries must be strings; got: " + entry);
            }
        }
        return patterns;
    }

    private static Set<String> readDoNotPrune(Map<?, ?> genMap) {
        Object doNotPrune = genMap.get("doNotPrune");
        if (!(doNotPrune instanceof List<?> entries)) {
            return Set.of();
        }
        Set<String> keys = new LinkedHashSet<>();
        for (Object entry : entries) {
            if (entry instanceof Map<?, ?> ref
                    && ref.get("type") instanceof String type
                    && ref.get("attribute") instanceof String attribute) {
                keys.add(type + "#" + attribute);
            }
        }
        return Set.copyOf(keys);
    }

    public NamespaceFilter namespaceFilter() {
        return namespaceFilter;
    }

    public Set<String> doNotPrune() {
        return doNotPrune;
    }
}
