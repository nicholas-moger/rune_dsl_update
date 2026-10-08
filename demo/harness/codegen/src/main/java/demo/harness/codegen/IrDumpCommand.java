package demo.harness.codegen;

import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.ir.adapter.AstToIRAdapter;
import com.regnosys.rosetta.ir.core.IRNode;
import com.regnosys.rosetta.ir.emit.EmitterException;
import com.regnosys.rosetta.ir.emit.python.IRPythonDeclarationEmitter;
import com.regnosys.rosetta.ir.json.IRJsonSerializer;
import com.regnosys.rosetta.ir.print.IRPrinter;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code ir-dump} — parses the spotlight model, adapts it to the fork IR, and writes the IR
 * twice: as the canonical versioned JSON document (the input Lane D's emitters read) and as
 * {@link IRPrinter} text (what the dashboard shows a human).
 *
 * <p>The spotlight file is parsed inside a workspace that also holds the builtins, so
 * {@code [metadata key]} and the basic types resolve. Only the spotlight model is adapted;
 * the builtins are resolution context and are never dumped.
 *
 * <p>{@link IRJsonSerializer#toJson(List)} produces the versioned wrapper
 * {@code {"irFormatVersion":N,"nodes":[...]}} -- that is the schema Lane D codes against, not
 * the bare single-node fragment produced by the {@code toJson(IRNode)} overload.
 */
public final class IrDumpCommand {

    private IrDumpCommand() {
    }

    public static int run(Args args) throws IOException {
        Path src = args.requirePath("src");
        Path builtinsDir = args.requirePath("builtins");
        Path outJson = args.requirePath("outJson");
        Path outText = args.requirePath("outText");
        Path outPython = args.optionalPath("outPython");
        Path receipt = args.optionalPath("receipt");

        if (!Files.isRegularFile(src)) {
            throw new IOException("--src is not a file: " + src.toAbsolutePath());
        }

        long jvmStart = ManagementFactory.getRuntimeMXBean().getStartTime();
        DemoOut.start("spotlight.ir", "ir-dump " + src.getFileName());

        // The spotlight is passed as a FILE; CorpusLoader takes roots, so hand it the file's
        // parent and select the one model back out by file name. That also means a spotlight
        // directory holding several models still parses as one coherent workspace.
        Path srcRoot = src.toAbsolutePath().getParent();
        CorpusLoader.Loaded loaded = CorpusLoader.load(List.of(srcRoot), builtinsDir);

        String wanted = src.getFileName().toString();
        RModel spotlight = null;
        for (RModel model : loaded.workspace().files()) {
            // AstBuilder.buildFromFile records the BARE FILE NAME in the source range
            // (AstBuilder:304), not the full path, so this is a name comparison by design.
            String file = model.sourceRange() == null ? null : model.sourceRange().file();
            if (file != null && CorpusLoader.fileName(file).equals(wanted)) {
                spotlight = model;
                break;
            }
        }
        if (spotlight == null) {
            DemoOut.error("spotlight model not found in the linked workspace: " + wanted);
            return 3;
        }

        long adaptStart = System.nanoTime();
        List<IRNode> nodes = new AstToIRAdapter().adaptModel(spotlight);
        long adaptMs = CorpusLoader.millisSince(adaptStart);

        String json = new IRJsonSerializer().toJson(nodes);
        String text = new IRPrinter().printAll(nodes);

        writeFile(outJson, json);
        writeFile(outText, text);

        // --outPython: the ENGINE's own Python declaration emitter (rune-ir), not a demo-side
        // script - the point is that Python falls out of the same toolchain. A node the
        // emitter declines becomes an explicit UNSUPPORTED comment, never a silent absence.
        int pythonDeclines = 0;
        if (outPython != null) {
            IRPythonDeclarationEmitter py = new IRPythonDeclarationEmitter();
            StringBuilder module = new StringBuilder();
            module.append("# Emitted by the rune-dsl-plus engine (rune-ir IRPythonDeclarationEmitter)\n");
            module.append("# from the spotlight IR - declarations only, the engine's golden-tested surface.\n");
            module.append(IRPythonDeclarationEmitter.DECL_PREAMBLE).append("\n");
            for (IRNode node : nodes) {
                module.append("\n\n");
                try {
                    module.append(py.emit(node));
                } catch (EmitterException e) {
                    pythonDeclines++;
                    module.append("# UNSUPPORTED: ").append(node.kind()).append(" ")
                          .append(node.name()).append(" - ").append(e.getMessage());
                }
            }
            module.append("\n");
            writeFile(outPython, module.toString());
        }

        DemoOut.log("[ir-dump] namespace=" + spotlight.namespace()
                + " rootElements=" + spotlight.rootElements().size()
                + " adaptedNodes=" + nodes.size());
        if (nodes.size() < spotlight.rootElements().size()) {
            // The Phase-1 adapter handles data types, choices and enums only; functions,
            // rules, reports and aliases return empty. Say so rather than let a short list
            // read as a defect.
            DemoOut.log("[ir-dump] note: the Phase-1 adapter covers data types, choices and"
                    + " enums; other root elements are not adapted and are absent by design");
        }

        long wallMs = System.currentTimeMillis() - jvmStart;
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("namespace", spotlight.namespace());
        metrics.put("rootElements", spotlight.rootElements().size());
        metrics.put("irNodes", nodes.size());
        metrics.put("jsonBytes", json.getBytes(StandardCharsets.UTF_8).length);
        metrics.put("textBytes", text.getBytes(StandardCharsets.UTF_8).length);
        if (outPython != null) {
            metrics.put("pythonDeclines", pythonDeclines);
        }
        metrics.put("parseMs", loaded.parseMs());
        metrics.put("linkMs", loaded.linkMs());
        metrics.put("adaptMs", adaptMs);
        metrics.put("wallMs", wallMs);
        metrics.put("exit", 0);
        DemoOut.done(metrics);

        if (receipt != null) {
            Receipts.write(receipt, "spotlight.ir", "spotlight", "Spotlight IR dump",
                    Receipts.commandLine("ir-dump", args), wallMs, metrics,
                    "IR JSON is the versioned document form (irFormatVersion + nodes);"
                    + " the Phase-1 adapter covers types, choices and enums only.");
        }
        return 0;
    }

    static void writeFile(Path target, String content) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        DemoOut.log("[write] " + target.toAbsolutePath() + " ("
                + content.getBytes(StandardCharsets.UTF_8).length + " bytes)");
    }
}
