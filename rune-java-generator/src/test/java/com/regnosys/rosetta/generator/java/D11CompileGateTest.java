package com.regnosys.rosetta.generator.java;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * W42 compile-gate driver (PR #233). For each cell: generate the fork's Java (all kinds),
 * compute the divergent set (fork vs golden, newline-normalized), build the golden
 * {@code .class} closure (self-check: 0 errors or HARNESS-BLOCKED), then compile each
 * divergent fork file individually against the closure for a clean, cascade-free
 * COMPILES / NON-COMPILING verdict. Writes per-cell JSON to {@code target/compile-gate/}.
 *
 * <p>The compile classpath is the cell's UPSTREAM 9.83.0 classpath — the runtime the
 * goldens target — resolved per-cell from each cell's {@code rosetta-source/pom.xml} by
 * {@code scripts/compile-gate/resolve-classpaths.sh}. NOT the test JVM classpath: the
 * fork's {@code rune-runtime} (0.0.0.main-SNAPSHOT) has drifted (verified at the cdm5
 * proving checkpoint — the golden closure fails 1110 @Override checks against the fork
 * runtime, 0 errors against upstream 9.83.0). Every transitive dependency is supplied as
 * the correct-version jar on that classpath (cdm6 cp → rune-fpml:1.5.3; drr cp →
 * cdm-java:5.37.0 + iso:1.37.0), so a cell's closure is just its own goldens.
 *
 * <p>{@code @Tag("compile-gate")} — excluded from the routine suite (heavy; compiles
 * thousands of files/cell). Invoke a single cell with:
 * <pre>
 *   scripts/compile-gate/resolve-classpaths.sh   # once
 *   mvn -f rune-java-generator/pom.xml test -Dtest=D11CompileGateTest#cdm5 -Drune.excludedGroups=none
 * </pre>
 * Reuses the REAL D11 loader (no D11 change), mirroring
 * {@link FunctionTransitiveIso20022ClosureTest}.
 */
@Tag("compile-gate")
class D11CompileGateTest {

    private static final Path OUT_ROOT = Path.of("target/compile-gate");
    private final JavaTypeUtil typeUtil = new JavaTypeUtil();
    private final JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
    private final CompileGate gate = new CompileGate();

    @Test
    void cdm5() throws IOException {
        runCell(new D11CorpusRegressionTest.CellSpec("cdm", "5.38.0",
                Path.of("../test-corpus/cdm/cdm-5.38.0")));
    }

    @Test
    void cdm6() throws IOException {
        runCell(new D11CorpusRegressionTest.CellSpec("cdm", "6.20.6",
                Path.of("../test-corpus/cdm/cdm-6.20.6")));
    }

    @Test
    void drr() throws IOException {
        runCell(new D11CorpusRegressionTest.CellSpec("drr", "6.34.1",
                Path.of("../test-corpus/drr/drr-6.34.1")));
    }

    @Test
    void iso20022() throws IOException {
        runCell(new D11CorpusRegressionTest.CellSpec("iso20022", "1.38.0",
                Path.of("../test-corpus/iso20022/iso20022-1.38.0")));
    }

    @Test
    void runeFpml() throws IOException {
        runCell(new D11CorpusRegressionTest.CellSpec("rune-fpml", "2.0.0",
                Path.of("../test-corpus/rune-fpml/rune-fpml-2.0.0")));
    }

    /**
     * Compile-gate one cell. The closure is the cell's OWN goldens; every transitive
     * dependency (cdm/iso/fpml) is supplied as the correct-version jar on the cell's
     * resolved upstream classpath (see the class javadoc).
     */
    private void runCell(D11CorpusRegressionTest.CellSpec cell) throws IOException {
        // 0. Resolve the UPSTREAM 9.83.0 classpath (the runtime the goldens target).
        Path cellDir = OUT_ROOT.resolve(cell.corpus() + "-" + cell.version());
        Files.createDirectories(cellDir);
        List<String> upstream = readUpstreamCp(cellDir);

        // 1. Load + generate all kinds (fork output).
        var corpus = new D11CorpusRegressionTest().loadCellCorpusCached(cell);
        // facet isoPruneConfig (PR #331, Seat-1 should-fix): honour the doNotPrune config so a
        // future tag-gated compile-gate re-run generates the CP1 keep-form (not the pre-#331 nullify).
        var gm = new GeneratorModel(corpus.workspace(), D11CorpusRegressionTest.emissionFilter(cell),
                D11CorpusRegressionTest.readDoNotPrune(cell));
        GenResult gen = generateAllKinds(corpus, gm);

        // 2. Divergent set = fork output that differs from golden (newline-normalized).
        Path goldenDir = D11CorpusRegressionTest.resolveGoldensDir(cell);
        Map<String, String> divergent = new TreeMap<>();
        int matches = 0, noGolden = 0;
        for (var e : gen.output.entrySet()) {
            Path g = goldenDir.resolve(e.getKey());
            if (!Files.isRegularFile(g)) { noGolden++; continue; }
            if (normalize(Files.readString(g)).equals(normalize(e.getValue()))) matches++;
            else divergent.put(e.getKey(), e.getValue());
        }

        // 3. Build the golden closure (.class) from the cell's own goldens, with
        // self-check. Transitive deps come from the cp jars (see runCell javadoc).
        Path closure = cellDir.resolve("golden-classes");
        List<File> goldenFiles = javaFiles(goldenDir);
        List<Diagnostic<? extends JavaFileObject>> closureErrors =
                gate.compile(goldenFiles, List.of(), upstream, closure);
        boolean closureClean = closureErrors.isEmpty();

        // 4. Per-file compile of each divergent fork file against the closure.
        List<String> cp = new ArrayList<>(upstream);
        cp.add(closure.toString());
        Path throwaway = cellDir.resolve("fork-out");
        List<CompileGate.FileResult> results = new ArrayList<>();
        if (closureClean) {
            for (var e : divergent.entrySet()) {
                results.add(gate.classifyForkFile(e.getKey(), e.getValue(), cp, throwaway));
            }
        }

        // 5. Write JSON + a one-line summary to stdout.
        writeJson(cell, matches, noGolden, divergent.size(), gen.genFailurePaths(), closureClean, closureErrors, results);
        long nonComp = results.stream().filter(r -> r.verdict() == CompileGate.Verdict.NON_COMPILING).count();
        System.out.println("COMPILE-GATE " + cell + ": closureClean=" + closureClean
                + " closureErrors=" + closureErrors.size()
                + " matches=" + matches + " divergent=" + divergent.size()
                + " genFailures=" + gen.genFailurePaths().size()
                + " nonCompiling=" + nonComp + " compiles=" + (results.size() - nonComp));
    }

    private record GenResult(Map<String, String> output, List<String> genFailurePaths) {}

    /** Generate every element kind D11 emits (enum/pojo/choice/rule/report/labelprovider/metafield/function). */
    private GenResult generateAllKinds(RLinkingResult corpus, GeneratorModel gm) {
        Map<String, String> out = new LinkedHashMap<>();
        var enumGen = new EnumGenerator(gm);
        var pojoGen = new ModelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = new ChoiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = new FunctionGenerator(gm, typeTranslator, typeUtil);
        var ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        var reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        var labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType), new LabelProviderGeneratorUtil());
        var metaGen = new MetaFieldGenerator(gm, typeTranslator);

        // Capture generation failures (no-emit / incomplete body) by waiver-key target path so the
        // ground-truth "no-emit" set is auditable, not just a count (Copilot R1).
        List<GenerationException> genErrs = new ArrayList<>();
        for (RModel m : corpus.workspace().files()) {
            if (!gm.shouldGenerate(m)) continue;
            String v = gm.version(m);
            genErrs.addAll(enumGen.generateClasses(m, v, out));
            genErrs.addAll(pojoGen.generateClasses(m, v, out));
            genErrs.addAll(choiceGen.generateClasses(m, v, out));
            genErrs.addAll(ruleGen.generateClasses(m, v, out));
            genErrs.addAll(reportGen.generateClasses(m, v, out));
            genErrs.addAll(labelProviderGen.generateClasses(m, v, out));
        }
        metaGen.generate(out);
        genErrs.addAll(funcGen.generateWithErrors(out));
        List<String> failPaths = genErrs.stream()
                .map(GenerationException::getTargetPath)
                .filter(p -> p != null)
                .distinct().sorted().collect(Collectors.toList());
        return new GenResult(out, failPaths);
    }

    /** Read the cell's upstream 9.83.0 classpath (resolve-classpaths.sh writes upstream-cp.txt). */
    private static List<String> readUpstreamCp(Path cellDir) throws IOException {
        Path cpFile = cellDir.resolve("upstream-cp.txt");
        if (!Files.isRegularFile(cpFile)) {
            throw new IllegalStateException("Upstream 9.83.0 classpath not resolved: "
                    + cpFile.toAbsolutePath() + " — run scripts/compile-gate/resolve-classpaths.sh first.");
        }
        List<String> cp = new ArrayList<>();
        for (String e : Files.readString(cpFile).trim().split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            if (!e.isBlank()) cp.add(e.trim());
        }
        return cp;
    }

    /** All .java files under the golden dir. */
    private static List<File> javaFiles(Path goldenDir) throws IOException {
        if (!Files.isDirectory(goldenDir)) return List.of();
        try (Stream<Path> walk = Files.walk(goldenDir)) {
            return walk.filter(p -> p.toString().endsWith(".java"))
                       .map(Path::toFile)
                       .collect(Collectors.toList());
        }
    }

    private static void writeJson(D11CorpusRegressionTest.CellSpec cell, int matches, int noGolden,
                                  int divergent, List<String> genFailurePaths, boolean closureClean,
                                  List<Diagnostic<? extends JavaFileObject>> closureErrors,
                                  List<CompileGate.FileResult> results) throws IOException {
        Path dir = OUT_ROOT.resolve(cell.corpus() + "-" + cell.version());
        Files.createDirectories(dir);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"cell\": \"").append(esc(cell.toString())).append("\",\n");
        sb.append("  \"matches\": ").append(matches).append(",\n");
        sb.append("  \"noGolden\": ").append(noGolden).append(",\n");
        sb.append("  \"divergent\": ").append(divergent).append(",\n");
        sb.append("  \"genFailures\": ").append(genFailurePaths.size()).append(",\n");
        // The no-emit (generation-failure) target paths — auditable, not just a count (Copilot R1).
        sb.append("  \"genFailurePaths\": [");
        for (int i = 0; i < genFailurePaths.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(esc(genFailurePaths.get(i))).append('"');
        }
        sb.append("],\n");
        sb.append("  \"closureClean\": ").append(closureClean).append(",\n");
        sb.append("  \"closureErrors\": ").append(closureErrors.size()).append(",\n");
        // First few closure errors help debug a HARNESS-BLOCKED cell.
        sb.append("  \"closureErrorSample\": [");
        for (int i = 0; i < Math.min(5, closureErrors.size()); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(esc(closureErrors.get(i).getMessage(Locale.ROOT))).append('"');
        }
        sb.append("],\n");
        sb.append("  \"files\": [\n");
        for (int i = 0; i < results.size(); i++) {
            var r = results.get(i);
            sb.append("    {\"path\": \"").append(esc(r.path())).append("\", \"verdict\": \"")
              .append(r.verdict()).append("\", \"category\": \"").append(esc(r.category()))
              .append("\", \"diagnostics\": [");
            for (int j = 0; j < r.diagnostics().size(); j++) {
                if (j > 0) sb.append(", ");
                sb.append('"').append(esc(r.diagnostics().get(j))).append('"');
            }
            sb.append("]}").append(i < results.size() - 1 ? "," : "").append("\n");
        }
        sb.append("  ]\n}\n");
        Files.writeString(dir.resolve("compile-gate.json"), sb.toString(), StandardCharsets.UTF_8);
    }

    /** Minimal JSON-string escaper: backslash/quote escaped; any control char (incl. \n \r \t) -> space (RFC 8259). */
    private static String esc(String s) {
        StringBuilder b = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' || c == '"') {
                b.append('\\').append(c);
            } else if (c < 0x20) {
                b.append(' ');
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").replace("\r", "\n");
    }
}
