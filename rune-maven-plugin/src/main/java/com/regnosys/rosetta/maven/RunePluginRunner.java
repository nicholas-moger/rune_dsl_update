package com.regnosys.rosetta.maven;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.JavaClassGenerator;
import com.regnosys.rosetta.generator.java.enums.EnumGenerator;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.JavaPackageInfoGenerator;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.spi.IRGenerationProvider;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;
import com.regnosys.rosetta.symbols.diagnostics.Severity;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The fork pipeline behind the mojo surface — the released 9.83.0 plugin's
 * generate semantics implemented on the fork's own parser + Java generator:
 *
 * <ol>
 *   <li><b>Library models from the classpath</b>: every classpath element whose
 *       path matches {@code classPathLookupFilter} is scanned for
 *       {@code *.rosetta} entries (jar or directory) — upstream's channel for
 *       the builtin {@code basictypes.rosetta}/{@code annotations.rosetta}
 *       carried by the {@code rune-runtime} jar.</li>
 *   <li><b>Source models</b>: every {@code *.rosetta} under the configured
 *       {@code sourceRoots}, parsed in sorted order per root.</li>
 *   <li><b>Workspace build</b>: {@link RWorkspace#build(List, List)} — link +
 *       validate, the same call the fork's corpus diagnostic gate pins.</li>
 *   <li><b>Diagnostic stream</b>: every linking + validation diagnostic sited
 *       in a source-root file is logged in the released plugin's issue-line
 *       format ({@link IssueFormatter}); upstream scopes its validation to
 *       source resources the same way (classpath models feed the index
 *       only).</li>
 *   <li><b>Generation</b>: the 11 generator passes exactly as the D11 corpus
 *       gate wires them (the swap-recon dump driver's order), filtered by the
 *       {@code rosetta-config.yml} namespace accept-list, written under the
 *       output directory. Every pass is constructed and dispatched through the
 *       {@link com.regnosys.rosetta.generator.java.spi.IRGeneration} seams, so a
 *       consumer can turn the IR route on with {@code -Drosetta.generator.ir=true}
 *       plus the provider jar on the plugin's own classpath; with the flag off
 *       every seam executes the exact legacy call and the output is byte-identical
 *       (the {@code route-fixture} goldens are that proof). Each pass logs the
 *       generator class that actually ran and the dispatch it took.</li>
 * </ol>
 */
public final class RunePluginRunner {

    /** Log adapter so the core stays free of Maven types. */
    public interface RunnerLog {
        void info(String message);

        void warn(String message);

        void error(String message);
    }

    /** Outcome counts for the mojo's summary line + failure decision. */
    public record Result(int modelsParsed, int filesWritten, int warnings, int errors) {
    }

    /** Signals a severe validation error when {@code failOnValidationError} is set. */
    public static final class ValidationFailedException extends RuntimeException {
        public ValidationFailedException(String message) {
            super(message);
        }
    }

    private final List<Path> sourceRoots;
    private final List<String> classpathElements;
    private final String classPathLookupFilter;
    private final RosettaConfigFile config;
    private final Path outputDir;
    private final boolean failOnValidationError;
    private final RunnerLog log;

    public RunePluginRunner(List<Path> sourceRoots,
                            List<String> classpathElements,
                            String classPathLookupFilter,
                            RosettaConfigFile config,
                            Path outputDir,
                            boolean failOnValidationError,
                            RunnerLog log) {
        this.sourceRoots = sourceRoots.stream().map(p -> p.toAbsolutePath().normalize()).toList();
        this.classpathElements = List.copyOf(classpathElements);
        this.classPathLookupFilter = classPathLookupFilter;
        this.config = config;
        this.outputDir = outputDir;
        this.failOnValidationError = failOnValidationError;
        this.log = log;
    }

    public Result run() throws IOException {
        // 1 + 2 — parse: classpath library models first, then the source roots
        // (the corpus gate's load order: builtins -> the corpus's own tree).
        List<RModel> models = new ArrayList<>(parseClasspathModels());
        int libraryCount = models.size();
        List<RModel> sourceModels = parseSourceModels();
        models.addAll(sourceModels);
        log.info("Parsed " + models.size() + " models (" + libraryCount + " classpath library, "
                + sourceModels.size() + " source)");

        // 3 — link + validate.
        RLinkingResult result = RWorkspace.build(models, sourceRoots);

        // 4 — the diagnostic stream, source-scoped, upstream line format.
        int warnings = 0;
        int errors = 0;
        List<RDiagnostic> stream = new ArrayList<>();
        stream.addAll(result.linkingDiagnostics());
        stream.addAll(result.workspace().validationDiagnostics());
        for (RDiagnostic d : stream) {
            if (!isInSourceScope(d.range().file())) {
                continue;
            }
            String line = IssueFormatter.format(d);
            if (d.severity() == Severity.ERROR) {
                errors++;
                log.error(line);
            } else if (d.severity() == Severity.WARNING) {
                warnings++;
                log.warn(line);
            } else {
                log.info(line);
            }
        }
        if (errors > 0 && failOnValidationError) {
            throw new ValidationFailedException(
                    "Execution failed due to a severe validation error.");
        }

        // 5 — generate + write.
        Map<String, String> generated = generateAll(result);
        int written = 0;
        for (Map.Entry<String, String> e : generated.entrySet()) {
            Path target = outputDir.resolve(e.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, e.getValue(), StandardCharsets.UTF_8);
            written++;
        }
        log.info("Generated " + written + " files -> " + outputDir);
        return new Result(models.size(), written, warnings, errors);
    }

    // ------------------------------------------------------------------
    // Parsing
    // ------------------------------------------------------------------

    private List<RModel> parseClasspathModels() throws IOException {
        List<RModel> models = new ArrayList<>();
        if (classPathLookupFilter == null || classPathLookupFilter.isBlank()) {
            return models;
        }
        Pattern filter = Pattern.compile(classPathLookupFilter);
        for (String element : classpathElements) {
            if (element == null || element.isBlank() || !filter.matcher(element).matches()) {
                continue;
            }
            Path path = Path.of(element);
            if (Files.isDirectory(path)) {
                models.addAll(parseModelsUnder(path));
            } else if (Files.isRegularFile(path)) {
                models.addAll(parseJarModels(path));
            }
        }
        return models;
    }

    private List<RModel> parseJarModels(Path jar) throws IOException {
        List<RModel> models = new ArrayList<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            List<ZipEntry> entries = new ArrayList<>();
            for (Enumeration<? extends ZipEntry> e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (!entry.isDirectory() && entry.getName().endsWith(".rosetta")) {
                    entries.add(entry);
                }
            }
            entries.sort(java.util.Comparator.comparing(ZipEntry::getName));
            for (ZipEntry entry : entries) {
                String content;
                try (InputStream in = zip.getInputStream(entry)) {
                    content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                // Pseudo-path for diagnostics; library models are index-only in
                // practice (never source-scoped), so the exact form is cosmetic.
                String fileName = normalize(jar.toString()) + "!/" + entry.getName();
                models.add(AstBuilder.buildFromString(content, fileName));
            }
        }
        return models;
    }

    private List<RModel> parseSourceModels() throws IOException {
        List<RModel> models = new ArrayList<>();
        for (Path root : sourceRoots) {
            models.addAll(parseModelsUnder(root));
        }
        return models;
    }

    private List<RModel> parseModelsUnder(Path root) throws IOException {
        List<RModel> models = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> files = stream
                    .filter(p -> p.toString().endsWith(".rosetta"))
                    .sorted()
                    .toList();
            for (Path p : files) {
                String content = Files.readString(p, StandardCharsets.UTF_8);
                models.add(AstBuilder.buildFromString(content, normalize(p.toAbsolutePath().toString())));
            }
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
        return models;
    }

    /** Forward-slashed absolute path — the form {@link IssueFormatter} URI-prefixes. */
    private static String normalize(String path) {
        return path.replace('\\', '/');
    }

    private boolean isInSourceScope(String file) {
        String normalized = normalize(file);
        for (Path root : sourceRoots) {
            String rootPrefix = normalize(root.toString());
            if (!rootPrefix.endsWith("/")) {
                rootPrefix = rootPrefix + "/";
            }
            if (normalized.startsWith(rootPrefix)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------
    // Generation — the 11 passes, wired exactly as the D11 corpus gate
    // (and its swap-recon dump driver) wires them.
    // ------------------------------------------------------------------

    /** The dispatch token of a generator this run never reached with any model. */
    private static final String NOT_DISPATCHED = "not-dispatched";

    private Map<String, String> generateAll(RLinkingResult corpus) {
        // v3.3 seat 9 (PR #645 commit 14b) - THE PLUGIN ROUTING. Every generator below is
        // CONSTRUCTED through an IRGeneration seam and DISPATCHED exactly as the D11 corpus
        // gate dispatches it, site for site (D11CorpusRegressionTest :1261/:1283, :1325/:1329/
        // :1341/:1382/:1384, :3873/:3876, :3890/:3905, :4731-:4897, :4936/:4942). With both
        // route flags off - the default of every consumer build - each seam executes the exact
        // legacy constructor and each dispatch falls through to the plain call, so the plugin's
        // output is byte-identical by construction; route-fixture/golden/** is that byte proof.
        //
        // The route header is the host's own three-way shape (D11CorpusRegressionTest
        // #printIrRouteHeader :190-200), COPIED rather than shared: the host is a test and this
        // is production, and a shared helper would have to live in rune-java-generator's
        // src/main, which this commit must not touch.
        log.info("IR route: " + routeLine(IRGeneration.providerOrNull(), IRGeneration.enabled()));

        RWorkspace workspace = corpus.workspace();
        Predicate<RModel> emission = model -> config.namespaceFilter().test(model.namespace());
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);

        Map<String, String> all = new LinkedHashMap<>();

        // ENUM
        {
            GeneratorModel gm = new GeneratorModel(workspace, emission);
            EnumGenerator gen = IRGeneration.enumGenerator(gm);
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errs = new ArrayList<>();
            String dispatch = NOT_DISPATCHED;
            for (RModel model : workspace.files()) {
                if (gm.shouldGenerate(model)) {
                    dispatch = dispatchRouted(gen, model, gm.version(model), out, errs);
                }
            }
            failOnErrors("ENUM", errs);
            merge(all, out, "ENUM", gen.getClass().getSimpleName(), dispatch);
        }

        // POJO + choice + rule + report + labelProvider (one shared GeneratorModel,
        // doNotPrune-aware - the only pass that consumes it)
        {
            GeneratorModel gm = new GeneratorModel(workspace, emission, config.doNotPrune());
            ModelObjectGenerator pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
            ChoiceObjectGenerator choiceGen =
                    IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
            // The rule / report / labelProvider generators are constructed with `new` over the
            // SEAM-built funcGen and dispatched DIRECTLY - the provider declares no seam for the
            // three (IRGenerationProvider :58-144) and the host wires them the same way
            // (:1341-:1352, :1386-:1388).
            FunctionGenerator funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            RuleGenerator ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
            ReportGenerator reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
            LabelProviderGenerator labelProviderGen = new LabelProviderGenerator(
                    gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                    new LabelProviderGeneratorUtil());
            Map<String, String> out = new LinkedHashMap<>();
            List<GenerationException> errs = new ArrayList<>();
            String pojoDispatch = NOT_DISPATCHED;
            String choiceDispatch = NOT_DISPATCHED;
            String ruleDispatch = NOT_DISPATCHED;
            String reportDispatch = NOT_DISPATCHED;
            String labelDispatch = NOT_DISPATCHED;
            for (RModel model : workspace.files()) {
                if (gm.shouldGenerate(model)) {
                    String version = gm.version(model);
                    pojoDispatch = dispatchRouted(pojoGen, model, version, out, errs);
                    choiceDispatch = dispatchRouted(choiceGen, model, version, out, errs);
                    ruleDispatch = dispatchDirect(ruleGen, model, version, out, errs);
                    reportDispatch = dispatchDirect(reportGen, model, version, out, errs);
                    labelDispatch = dispatchDirect(labelProviderGen, model, version, out, errs);
                }
            }
            failOnErrors("POJO", errs);
            // The block's SIX generators in CONSTRUCTION order. funcGen is named although it
            // writes no file of its own: it is the rule / report generators' collaborator and a
            // construction site in its own right, so leaving it out would leave that site
            // unreadable - a route that could lie. Its dispatch token is `collaborator`.
            merge(all, out, "POJO",
                    pojoGen.getClass().getSimpleName() + "," + choiceGen.getClass().getSimpleName()
                            + "," + funcGen.getClass().getSimpleName()
                            + "," + ruleGen.getClass().getSimpleName()
                            + "," + reportGen.getClass().getSimpleName()
                            + "," + labelProviderGen.getClass().getSimpleName(),
                    pojoDispatch + "," + choiceDispatch + ",collaborator,"
                            + ruleDispatch + "," + reportDispatch + "," + labelDispatch);
        }

        // METAFIELD (workspace-level pass)
        {
            GeneratorModel gm = new GeneratorModel(workspace, emission);
            MetaFieldGenerator gen = IRGeneration.metaFieldGenerator(gm, typeTranslator);
            Map<String, String> out = new LinkedHashMap<>();
            String dispatch = dispatchRoutedMeta(gen, out);
            merge(all, out, "METAFIELD", gen.getClass().getSimpleName(), dispatch);
        }

        // FUNCTION (workspace-level pass)
        {
            GeneratorModel gm = new GeneratorModel(workspace, emission);
            FunctionGenerator gen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
            Map<String, String> out = new LinkedHashMap<>();
            // generateWithErrors is a DIRECT call and has no dispatch seam to route through -
            // the host calls it directly too (:3905), so the token is `direct` by construction.
            failOnErrors("FUNCTION", gen.generateWithErrors(out));
            merge(all, out, "FUNCTION", gen.getClass().getSimpleName(), "direct");
        }

        // Per-model validator/meta/util/rule kinds. The five derived kinds dispatch through the
        // IRGeneration seam (routedDispatch=true, the host :4737-:4897); DATA_RULE is CONSTRUCTED
        // through its seam but DISPATCHED directly (routedDispatch=false, the host :4942) -
        // IRDataRuleGenerator is not an IREmittableGenerator, so the seam would decline to the
        // same call, and the runner mirrors the host site for site rather than diverging.
        generatePerModelKind(all, workspace, emission, "ONLY_EXISTS", true,
                gm -> IRGeneration.onlyExistsValidatorGenerator(gm, typeTranslator, typeUtil));
        generatePerModelKind(all, workspace, emission, "CARDINALITY", true,
                gm -> IRGeneration.cardinalityValidatorGenerator(gm, typeTranslator, typeUtil));
        generatePerModelKind(all, workspace, emission, "TYPE_FORMAT", true,
                gm -> IRGeneration.typeFormatValidatorGenerator(gm, typeTranslator, typeUtil));
        generatePerModelKind(all, workspace, emission, "XMETA", true,
                gm -> IRGeneration.modelMetaGenerator(gm, typeTranslator));
        generatePerModelKind(all, workspace, emission, "DEEP_PATH", true,
                gm -> IRGeneration.deepPathUtilGenerator(gm, typeTranslator, typeUtil));
        generatePerModelKind(all, workspace, emission, "DATA_RULE", false,
                gm -> IRGeneration.dataRuleGenerator(gm, typeTranslator, typeUtil));

        // PACKAGE_INFO (workspace-level pass)
        {
            GeneratorModel gm = new GeneratorModel(workspace, emission);
            JavaPackageInfoGenerator gen = new JavaPackageInfoGenerator(gm);
            Map<String, String> out = new LinkedHashMap<>();
            gen.generatePackageInfoClasses(out);
            // The twelfth pass has NO seam on IRGenerationProvider - the package-info family is
            // not part of the IR route at any head; constructed and dispatched directly, said so.
            merge(all, out, "PACKAGE_INFO", gen.getClass().getSimpleName(), "direct");
        }

        return all;
    }

    /**
     * The ROUTED per-model dispatch, and the token the pass logs for it. The token is ANSWERED
     * by the call that was actually made rather than declared beside it, so a site that stops
     * routing stops reporting {@code seam} in the same edit - the plugin's log cannot disagree
     * with the plugin's behaviour.
     */
    private static String dispatchRouted(JavaClassGenerator<?, ?> gen, RModel model, String version,
                                         Map<String, String> out, List<GenerationException> errs) {
        errs.addAll(IRGeneration.generateClasses(gen, model, version, out));
        return "seam";
    }

    /** The DIRECT per-model dispatch (no seam exists, or the host calls it directly). */
    private static String dispatchDirect(JavaClassGenerator<?, ?> gen, RModel model, String version,
                                         Map<String, String> out, List<GenerationException> errs) {
        errs.addAll(gen.generateClasses(model, version, out));
        return "direct";
    }

    /** The ROUTED workspace-wide metafield dispatch — {@link #dispatchRouted}'s law, one pass over. */
    private static String dispatchRoutedMeta(MetaFieldGenerator gen, Map<String, String> out) {
        IRGeneration.generateMeta(gen, out);
        return "seam";
    }

    /**
     * The route header's text - the host's three-way shape (D11CorpusRegressionTest
     * #printIrRouteHeader :190-200), as a PURE function of the two facts it reads so that all
     * three arms are assertable by exact text. The THIRD arm (flag set, no provider resolved)
     * cannot be produced from inside this module's surefire JVM - both provider jars are
     * unconditional test-scope dependencies - so a function is the only way it is ever read.
     *
     * @param provider          {@link IRGeneration#providerOrNull()}'s answer for the active route
     * @param referenceFlagSet  {@link IRGeneration#enabled()}, the reference route's own flag
     */
    static String routeLine(IRGenerationProvider provider, boolean referenceFlagSet) {
        if (provider != null) {
            return "ON (provider " + provider.getClass().getName() + ")";
        }
        if (referenceFlagSet) {
            return "OFF (flag -D" + IRGeneration.PROPERTY
                    + "=true set but NO provider on the classpath \u2014 Path-1)";
        }
        return "OFF";
    }

    private interface PerModelGen {
        JavaClassGenerator<?, ?> make(GeneratorModel gm);
    }

    /**
     * One per-model pass. {@code routedDispatch} is the ONE difference between the five derived
     * kinds and DATA_RULE (see the call sites): true dispatches through
     * {@link IRGeneration#generateClasses}, false calls the generator directly - stated once
     * here rather than copied into two near-identical methods.
     */
    private void generatePerModelKind(Map<String, String> all, RWorkspace workspace,
                                      Predicate<RModel> emission, String label,
                                      boolean routedDispatch, PerModelGen maker) {
        GeneratorModel gm = new GeneratorModel(workspace, emission);
        JavaClassGenerator<?, ?> gen = maker.make(gm);
        Map<String, String> out = new LinkedHashMap<>();
        List<GenerationException> errs = new ArrayList<>();
        String dispatch = NOT_DISPATCHED;
        for (RModel model : workspace.files()) {
            if (gm.shouldGenerate(model)) {
                String version = gm.version(model);
                dispatch = routedDispatch
                        ? dispatchRouted(gen, model, version, out, errs)
                        : dispatchDirect(gen, model, version, out, errs);
            }
        }
        failOnErrors(label, errs);
        merge(all, out, label, gen.getClass().getSimpleName(), dispatch);
    }

    private static void failOnErrors(String label, List<GenerationException> errs) {
        if (!errs.isEmpty()) {
            TreeMap<String, Integer> byMessage = new TreeMap<>();
            for (GenerationException e : errs) {
                byMessage.merge(String.valueOf(e.getMessage()), 1, Integer::sum);
            }
            throw new IllegalStateException(label + " generation failed with " + errs.size()
                    + " error(s); first: " + byMessage.firstKey());
        }
    }

    /**
     * Merges one pass's output into the run's output and logs the pass's observables: the file
     * count, the SIMPLE CLASS NAME of the generator(s) that actually ran - flag-off the legacy
     * names, flag-on the IR-routed ones - and the dispatch each of them took, one token per
     * named generator, in the same order. A route that lies is then readable in the plugin's
     * own log.
     */
    private void merge(Map<String, String> all, Map<String, String> out, String label,
                       String generators, String dispatch) {
        int collisions = 0;
        for (Map.Entry<String, String> e : out.entrySet()) {
            String prev = all.putIfAbsent(e.getKey(), e.getValue());
            if (prev != null) {
                collisions++;
                if (!prev.equals(e.getValue())) {
                    throw new IllegalStateException(label
                            + ": conflicting content for generated file " + e.getKey());
                }
            }
        }
        log.info(label + " files=" + out.size()
                + (collisions > 0 ? " (identical-collisions=" + collisions + ")" : "")
                + " generator=" + generators + " dispatch=" + dispatch);
    }
}
