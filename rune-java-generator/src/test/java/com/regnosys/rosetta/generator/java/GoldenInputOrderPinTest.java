package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.java.D11CorpusRegressionTest.CellSpec;
import com.regnosys.rosetta.generator.java.template.TemplateRenderer;
import com.regnosys.rosetta.generator.java.template.model.PackageInfoModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * N1 (v3.1 phase C, C0 item 3) — proves the committed
 * {@code D11CorpusRegressionTest.GOLDEN_INPUT_ORDER_PINS} reproduce every cell's
 * package-info goldens, and prints the exact literal to add when they do not.
 *
 * <p><b>Why pins exist at all.</b> Upstream's package-info generator collects
 * {@code (namespace → definition)} into a Guava {@code LinkedHashMultimap} fed in
 * resource LOAD order, which for the maven mojo is {@code File.listFiles()} — the build
 * machine's readdir order, explicitly unordered. A namespace whose files carry
 * DIFFERENT definitions therefore freezes that machine's enumeration into the golden.
 * It is upstream's reproducible-builds defect (FINOS issue drafted in the waiver-file
 * header); we replay the recorded order rather than pretend it is derivable. PR #413
 * decoded four such namespaces by hand for the two originally-gated CDM cells.
 *
 * <p><b>What this adds (C0).</b> The band expansion brought eight further CDM cells
 * carrying the same artifact, and hand-decoding does not scale to a corpus that grows.
 * This decodes mechanically and <b>by byte oracle rather than by hypothesis</b>: it
 * feeds the cell's real walk — reordered by {@code applyGoldenInputOrderPins}, the
 * harness's own replay — through the real {@link PackageInfoModel} and
 * {@code templates/java-package-info.stg}, and asks whether the committed golden comes
 * back byte-for-byte. No parsing of golden prose, no assumption about which file suffix
 * comes first, and the pins are checked THROUGH the production replay path rather than
 * against a reimplementation of it.
 *
 * <p><b>On ambiguity.</b> Files in one namespace sharing a verbatim definition are
 * interchangeable — the multimap drops the later duplicate — so several literals can be
 * equally correct (PR #413 recorded exactly this for the {@code cdm.observable.asset}
 * func/type pair). The test therefore asserts the OUTPUT property (the golden
 * reproduces), never literal equality with one decoded permutation.
 *
 * <p>Skips when the corpus is absent (CI / fresh clone).
 */
class GoldenInputOrderPinTest {

    /** Namespaces larger than this are reported rather than brute-forced. */
    private static final int MAX_FILES_TO_PERMUTE = 6;

    private final TemplateRenderer renderer = newRenderer();

    private static TemplateRenderer newRenderer() {
        TemplateRenderer r = new TemplateRenderer();
        r.loadGroupFromClasspath("templates/java-package-info.stg", '$', '$');
        return r;
    }

    @Test
    void committedPinsReproduceEveryPackageInfoGolden() throws Exception {
        List<CellSpec> cells = D11CorpusRegressionTest.activeCells()
                .filter(D11CorpusRegressionTest::cellGoldensExist)
                .toList();
        assumeTrue(!cells.isEmpty(), "corpus absent");

        List<String> problems = new ArrayList<>();
        Map<String, List<List<String>>> decoded = new TreeMap<>();

        for (CellSpec cell : cells) {
            checkCell(cell, problems, decoded);
        }

        assertTrue(problems.isEmpty(), () -> String.join("\n\n", problems)
                + (decoded.isEmpty() ? "" : "\n\n--- decoded pin literals to add ---\n"
                        + renderPinLiterals(decoded)));
    }

    private void checkCell(CellSpec cell, List<String> problems,
                           Map<String, List<List<String>>> decoded) throws Exception {
        Path rosettaDir = D11CorpusRegressionTest.resolveRosettaInputDir(cell);
        Path goldensDir = D11CorpusRegressionTest.resolveGoldensDir(cell);

        List<Path> walk;
        try (Stream<Path> stream = Files.walk(rosettaDir)) {
            walk = stream.filter(p -> p.toString().endsWith(".rosetta")).sorted().toList();
        }
        // The § 4b skip (v3.2 PR-2): the chaos cell's 22 expected-refusal files
        // parse-refuse by design — the SAME shared filter the D11 loader applies, so
        // this replay walks exactly the population the harness generates from.
        walk = D11CorpusRegressionTest.dropChaosExpectedRefusals(cell, walk);
        // The production replay — this is the order the harness actually generates from.
        List<Path> pinned = D11CorpusRegressionTest.applyGoldenInputOrderPins(cell, walk);

        Map<String, List<NamespaceFile>> byNamespace = new LinkedHashMap<>();
        for (Path p : pinned) {
            RModel model = AstBuilder.buildFromFile(p);
            String definition = model.definition().orElse(null);
            if (definition != null) {
                byNamespace.computeIfAbsent(model.namespace(), k -> new ArrayList<>())
                        .add(new NamespaceFile(p.getFileName().toString(), definition));
            }
        }

        for (Map.Entry<String, List<NamespaceFile>> entry : byNamespace.entrySet()) {
            String namespace = entry.getKey();
            List<NamespaceFile> files = entry.getValue();

            Path golden = goldensDir.resolve(namespace.replace('.', '/') + "/package-info.java");
            if (!Files.isRegularFile(golden)) {
                continue;   // dependency-closure namespace: this cell emits no golden for it
            }
            String goldenSource = Files.readString(golden).replace("\r\n", "\n");
            if (render(namespace, files).equals(goldenSource)) {
                continue;   // the pinned (or canonical) order reproduces it
            }

            List<NamespaceFile> winner = searchOrder(namespace, files, goldenSource);
            if (winner == null) {
                problems.add(cell + " " + namespace + ": NO input order of its "
                        + files.size() + " files reproduces the golden package-info. This is"
                        + " not the #413 ordering artifact — the descriptions themselves"
                        + " differ. Decode by hand before pinning.");
                continue;
            }
            problems.add(cell + " " + namespace + ": the committed pins do not reproduce the"
                    + " golden. Order that does: "
                    + winner.stream().map(NamespaceFile::fileName).toList());
            decoded.computeIfAbsent(cell.toString(), k -> new ArrayList<>())
                    .add(winner.stream().map(NamespaceFile::fileName).toList());
        }
    }

    /** Renders the namespace's package-info exactly as {@code JavaPackageInfoGenerator} does. */
    private String render(String namespace, List<NamespaceFile> order) {
        // Mirrors namespaceToDescriptionMap: insertion-ordered, exact-duplicate
        // definitions collapsed (LinkedHashSet == the Guava multimap's dedup).
        Set<String> descriptions = new LinkedHashSet<>();
        order.forEach(f -> descriptions.add(f.definition()));
        return renderer.render("templates/java-package-info.stg", "packageInfo", "model",
                        new PackageInfoModel(namespace, new ArrayList<>(descriptions)))
                .replace("\r\n", "\n");
    }

    /** The byte oracle: the first permutation whose rendering equals the golden. */
    private List<NamespaceFile> searchOrder(String namespace, List<NamespaceFile> files,
                                            String goldenSource) {
        if (files.size() > MAX_FILES_TO_PERMUTE) {
            return null;
        }
        List<List<NamespaceFile>> permutations = new ArrayList<>();
        permute(new ArrayList<>(files), new ArrayList<>(), permutations);
        for (List<NamespaceFile> candidate : permutations) {
            if (render(namespace, candidate).equals(goldenSource)) {
                return candidate;
            }
        }
        return null;
    }

    private static void permute(List<NamespaceFile> remaining, List<NamespaceFile> prefix,
                                List<List<NamespaceFile>> out) {
        if (remaining.isEmpty()) {
            out.add(List.copyOf(prefix));
            return;
        }
        for (int i = 0; i < remaining.size(); i++) {
            NamespaceFile picked = remaining.get(i);
            List<NamespaceFile> rest = new ArrayList<>(remaining);
            rest.remove(i);
            prefix.add(picked);
            permute(rest, prefix, out);
            prefix.remove(prefix.size() - 1);
        }
    }

    private static String renderPinLiterals(Map<String, List<List<String>>> decoded) {
        return decoded.entrySet().stream()
                .map(e -> "            \"" + e.getKey() + "\", List.of(\n"
                        + e.getValue().stream()
                                .map(group -> "                    List.of("
                                        + group.stream().map(n -> "\"" + n + "\"")
                                                .collect(Collectors.joining(",\n                            "))
                                        + ")")
                                .collect(Collectors.joining(",\n"))
                        + ")")
                .collect(Collectors.joining(",\n"));
    }

    private record NamespaceFile(String fileName, String definition) {}
}
