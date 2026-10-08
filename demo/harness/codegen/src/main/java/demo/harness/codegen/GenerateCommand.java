package demo.harness.codegen;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.generator.GenerationException;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.function.FunctionGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGenerator;
import com.regnosys.rosetta.generator.java.function.LabelProviderGeneratorUtil;
import com.regnosys.rosetta.generator.java.object.ChoiceObjectGenerator;
import com.regnosys.rosetta.generator.java.object.ModelObjectGenerator;
import com.regnosys.rosetta.generator.java.object.datarule.DataRuleGenerator;
import com.regnosys.rosetta.generator.java.reports.ReportGenerator;
import com.regnosys.rosetta.generator.java.reports.RuleGenerator;
import com.regnosys.rosetta.generator.java.spi.IRGeneration;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.utils.DeepFeatureCallUtil;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * {@code generate} — runs the fork's full Java generator set over the demo corpus and writes
 * the result to disk.
 *
 * <p>The generator wiring is copied verbatim from
 * {@code ArgumentPositionRuleWrapSeatTest.generateCell(...)}: one {@link GeneratorModel} over
 * the linked workspace and an emission filter, then per-model
 * {@code pojo -> choice -> rule -> report -> dataRule -> labelProvider}, then a single
 * whole-workspace {@link FunctionGenerator#generateWithErrors(Map)} pass at the end (functions
 * are generated once for the whole workspace, not per model).
 *
 * <h2>Modes</h2>
 * {@code m1}, {@code m2} and {@code m3} require NO code differences here. The route is chosen
 * by system properties and classpath supplied by the OUTER command line — see the module
 * README. {@code --mode} is carried through to the metrics purely as a label, and is echoed
 * back so a receipt can never be attributed to the wrong route.
 */
public final class GenerateCommand {

    private GenerateCommand() {
    }

    /** Progress events are emitted at most once per this many written files (CONTRACTS s.3). */
    private static final int PROGRESS_EVERY = 250;

    /** The namespaces that are loaded for resolution but never generated (CONTRACTS s.1). */
    private static final List<String> UNGENERATED_NAMESPACES =
            List.of("com.rosetta.model", "com.rosetta.test.model");

    public static int run(Args args) throws IOException {
        String mode = args.get("mode", "m1");
        String filterName = args.get("filter", "all");
        List<Path> srcRoots = args.requirePaths("src");
        Path builtinsDir = args.requirePath("builtins");
        Path outDir = args.requirePath("out");
        Path receipt = args.optionalPath("receipt");

        if (!List.of("m1", "m2", "m3", "legacy").contains(mode)) {
            throw new IllegalArgumentException(
                    "--mode must be one of m1|m2|m3 (got '" + mode + "'); 'legacy' is the"
                    + " separate demo-harness-legacy module");
        }
        if ("legacy".equals(mode)) {
            throw new IllegalArgumentException(
                    "--mode legacy is not this module: run demo.harness.legacy.Main from the"
                    + " demo-harness-legacy artifact, which has its own disjoint classpath");
        }

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        String id = "codegen." + mode;
        DemoOut.start(id, "fork generate mode=" + mode + " filter=" + filterName
                + " roots=" + srcRoots.size());

        CorpusLoader.Loaded loaded = CorpusLoader.load(srcRoots, builtinsDir);
        DemoOut.metric("parseMs", loaded.parseMs());
        DemoOut.metric("linkMs", loaded.linkMs());
        DemoOut.metric("sourceFiles", loaded.models().size());
        DemoOut.log("[codegen] parsed " + loaded.models().size() + " .rosetta file(s) ("
                + loaded.builtinCount() + " builtin) in " + loaded.parseMs()
                + " ms; linked in " + loaded.linkMs() + " ms");

        Predicate<RModel> emissionFilter = emissionFilter(filterName);
        GeneratorModel gm = new GeneratorModel(loaded.workspace(), emissionFilter);

        // The FULL generator set, constructed and dispatched through the D43 IR seams —
        // the exact D11CorpusRegressionTest pattern, NOT the seat-test pattern. This is
        // load-bearing twice over: (1) the seat harness omits the enum, metafield and
        // (via ModelObjectGenerator alone) package-info surfaces, so a tree generated
        // that way is incomplete against the legacy toolchain's output; (2) the m2/m3
        // routes ONLY engage through IRGeneration — constructing generators directly
        // bypasses the ServiceLoader dispatch entirely, so the -Drosetta.generator.ir*
        // flags would be silently inert (no stderr notice fires, because the seam is
        // never consulted). Found at integration when gen-m3 came out byte-identical
        // to gen-m1 across all 9,085 files.
        JavaTypeUtil typeUtil = new JavaTypeUtil();
        JavaTypeTranslator typeTranslator = new JavaTypeTranslator(typeUtil);
        var enumGen = IRGeneration.enumGenerator(gm);
        var pojoGen = IRGeneration.modelObjectGenerator(gm, typeTranslator, typeUtil);
        var choiceGen = IRGeneration.choiceObjectGenerator(gm, typeTranslator, typeUtil, pojoGen);
        var funcGen = IRGeneration.functionGenerator(gm, typeTranslator, typeUtil);
        var metaGen = IRGeneration.metaFieldGenerator(gm, typeTranslator);
        RuleGenerator ruleGen = new RuleGenerator(gm, typeTranslator, funcGen);
        ReportGenerator reportGen = new ReportGenerator(gm, typeTranslator, funcGen);
        DataRuleGenerator dataRuleGen = new DataRuleGenerator(gm, typeTranslator, typeUtil);
        LabelProviderGenerator labelProviderGen = new LabelProviderGenerator(
                gm, typeTranslator, new DeepFeatureCallUtil(gm::getType),
                new LabelProviderGeneratorUtil());
        // The five remaining per-model kind generators + the per-namespace package-info pass —
        // exactly the set D11's onlyexists/cardinality/typeformat/xmeta/deeppath/packageinfo
        // comparison methods construct. Without these the tree is roughly HALF the legacy
        // toolchain's output (found at integration: fork 9,939 files vs legacy 19,219 — the
        // missing families were validation/ 4,456, exists/ 2,228, meta/ 2,228, util/ 262 and
        // 46 package-info.java). Constructed directly, as D11 does — no IR seam exists for
        // them; dispatched through IRGeneration.generateClasses anyway, which is a safe
        // pass-through for unclaimed generators and keeps every kind on one dispatch path.
        var onlyExistsGen = new com.regnosys.rosetta.generator.java.object.validators
                .OnlyExistsValidatorGenerator(gm, typeTranslator, typeUtil);
        var cardinalityGen = new com.regnosys.rosetta.generator.java.object.validators
                .CardinalityValidatorGenerator(gm, typeTranslator, typeUtil);
        var typeFormatGen = new com.regnosys.rosetta.generator.java.object.validators
                .TypeFormatValidatorGenerator(gm, typeTranslator, typeUtil);
        var xmetaGen = new com.regnosys.rosetta.generator.java.object
                .ModelMetaGenerator(gm, typeTranslator);
        var deepPathGen = new com.regnosys.rosetta.generator.java.object.deeppath
                .DeepPathUtilGenerator(gm, typeTranslator, typeUtil);
        var packageInfoGen = new com.regnosys.rosetta.generator.java.object
                .JavaPackageInfoGenerator(gm);

        // Route PROOF for the receipt: which route is this JVM actually on? A mode label
        // alone is not evidence — the provider presence + flag state is.
        String route;
        var provider = IRGeneration.providerOrNull();
        if (provider != null) {
            route = "IR:" + provider.getClass().getSimpleName();
        } else if (IRGeneration.enabled() || IRGeneration.optimisedEnabled()) {
            route = "LEGACY (flag set but NO provider on classpath - check the route jar!)";
        } else {
            route = "LEGACY";
        }
        DemoOut.metric("route", route);
        DemoOut.log("[codegen] dispatch route: " + route);

        Map<String, String> output = new LinkedHashMap<>();
        List<GenerationException> errors = new ArrayList<>();

        long genStart = System.nanoTime();
        int generatedModels = 0;
        for (RModel model : loaded.workspace().files()) {
            if (!gm.shouldGenerate(model)) {
                continue;
            }
            generatedModels++;
            String version = gm.version(model);
            errors.addAll(IRGeneration.generateClasses(enumGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(pojoGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(choiceGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(ruleGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(reportGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(dataRuleGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(labelProviderGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(onlyExistsGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(cardinalityGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(typeFormatGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(xmetaGen, model, version, output));
            errors.addAll(IRGeneration.generateClasses(deepPathGen, model, version, output));
        }
        // Functions are generated once over the whole workspace, not per model; metafields
        // likewise, through their own dispatch seam (JavaCodeGenerator:127's pattern);
        // package-info is one file per NAMESPACE, emitted by its own whole-workspace pass.
        errors.addAll(funcGen.generateWithErrors(output));
        IRGeneration.generateMeta(metaGen, output);
        packageInfoGen.generatePackageInfoClasses(output);
        long generateMs = CorpusLoader.millisSince(genStart);

        DemoOut.metric("generatedModels", generatedModels);
        DemoOut.metric("generateMs", generateMs);
        DemoOut.log("[codegen] generated " + output.size() + " java file(s) from "
                + generatedModels + " model(s) in " + generateMs + " ms");

        if (!errors.isEmpty()) {
            // A refusal is a real result, not a warning to bury. Report the first few by
            // target path so the dashboard shows something actionable, then exit nonzero.
            // For the demo corpus (the drr-6.34.1 closure) zero errors are expected.
            for (int i = 0; i < Math.min(10, errors.size()); i++) {
                GenerationException e = errors.get(i);
                DemoOut.error("generation refused: " + e.getTargetPath() + " -- " + e.getMessage());
            }
            DemoOut.error("generation produced " + errors.size()
                    + " error(s); no files were written");
            return 3;
        }

        long writeStart = System.nanoTime();
        Files.createDirectories(outDir);
        long firstFileMs = -1;
        int written = 0;
        int total = output.size();
        for (Map.Entry<String, String> entry : output.entrySet()) {
            Path target = outDir.resolve(entry.getKey()).normalize();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            // Bytes written verbatim: no newline translation, so the tree is byte-comparable
            // against the legacy tree by demo/build/diff-trees.ps1.
            Files.write(target, entry.getValue().getBytes(StandardCharsets.UTF_8));
            written++;
            if (firstFileMs < 0) {
                firstFileMs = System.currentTimeMillis() - jvmStart;
                DemoOut.metric("jvmStartToFirstFileMs", firstFileMs);
            }
            if (written % PROGRESS_EVERY == 0) {
                DemoOut.progress(written, total);
            }
        }
        long writeMs = CorpusLoader.millisSince(writeStart);
        DemoOut.progress(written, total);

        long wallMs = System.currentTimeMillis() - jvmStart;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("mode", mode);
        metrics.put("files", written);
        metrics.put("wallMs", wallMs);
        metrics.put("jvmStartToFirstFileMs", firstFileMs < 0 ? 0 : firstFileMs);
        metrics.put("exit", 0);
        // Measured detail beyond the contract's required keys. peakRssMb is deliberately
        // ABSENT: CONTRACTS s.4 assigns it to the outer PowerShell wrapper, and this process
        // cannot observe its own peak working set without guessing.
        metrics.put("filter", filterName);
        metrics.put("sourceFiles", loaded.models().size());
        metrics.put("generatedModels", generatedModels);
        metrics.put("parseMs", loaded.parseMs());
        metrics.put("linkMs", loaded.linkMs());
        metrics.put("generateMs", generateMs);
        metrics.put("writeMs", writeMs);

        DemoOut.done(metrics);
        if (receipt != null) {
            Receipts.write(receipt, id, "codegen", "Fork generate (" + mode + ")",
                    Receipts.commandLine("generate", args), wallMs, metrics,
                    "wallMs is JVM-start to last-file-written, so it includes JVM boot;"
                    + " peakRssMb omitted (measured by the outer wrapper).");
        }
        return 0;
    }

    /**
     * The emission filter. {@code drr} keeps only the {@code drr.} namespaces; {@code all}
     * keeps everything except the two ungenerated builtin namespaces. Either way
     * {@link GeneratorModel#shouldGenerate} additionally drops the builtin FILES by name, so
     * the two mechanisms agree even if a corpus re-namespaces its builtins.
     */
    static Predicate<RModel> emissionFilter(String filterName) {
        return switch (filterName) {
            case "drr" -> model -> {
                String ns = model.namespace();
                return ns != null && (ns.equals("drr") || ns.startsWith("drr."));
            };
            case "all" -> model -> {
                String ns = model.namespace();
                if (ns == null) {
                    return false;
                }
                for (String ungenerated : UNGENERATED_NAMESPACES) {
                    if (ns.equals(ungenerated) || ns.startsWith(ungenerated + ".")) {
                        return false;
                    }
                }
                return true;
            };
            default -> throw new IllegalArgumentException(
                    "--filter must be 'drr' or 'all' (got '" + filterName + "')");
        };
    }
}
