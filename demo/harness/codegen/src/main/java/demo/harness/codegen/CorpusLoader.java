package demo.harness.codegen;

import com.regnosys.rosetta.ast.builder.AstBuilder;
import com.regnosys.rosetta.ast.model.RModel;
import com.regnosys.rosetta.symbols.RLinkingResult;
import com.regnosys.rosetta.symbols.RWorkspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads a set of {@code .rosetta} source roots plus the builtins directory into one linked
 * fork workspace.
 *
 * <p>The sequence mirrors {@code ArgumentPositionRuleWrapSeatTest.link()} /
 * {@code loadBuiltinsOnly()} exactly: {@link AstBuilder#buildFromFile(Path)} per file, then a
 * single {@link RWorkspace#build(List)} over all of them together. The builtins
 * ({@code basictypes.rosetta}, {@code annotations.rosetta}) MUST be in the same workspace or
 * {@code string} / {@code boolean} and every {@code [metadata ...]} annotation resolve to
 * nothing.
 */
public final class CorpusLoader {

    private CorpusLoader() {
    }

    /** The two builtin file names, which are loaded for resolution but never generated from. */
    public static final List<String> BUILTIN_FILE_NAMES =
            List.of("basictypes.rosetta", "annotations.rosetta");

    /**
     * The outcome of a load: the linked workspace, the models in load order, and the split
     * timings the CONTRACTS section-4 metrics need.
     */
    public record Loaded(RLinkingResult linking, List<RModel> models, int builtinCount,
                         long parseMs, long linkMs) {

        public RWorkspace workspace() {
            return linking.workspace();
        }

        /** Corpus files only (builtins excluded) — the population every count is taken over. */
        public int corpusFileCount() {
            return models.size() - builtinCount;
        }
    }

    /** Every {@code .rosetta} file under {@code dir}, recursively, in sorted path order. */
    public static List<Path> rosettaFilesUnder(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IOException("not a directory: " + dir.toAbsolutePath());
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(p -> p.toString().endsWith(".rosetta"))
                    .filter(Files::isRegularFile)
                    .sorted()
                    .toList();
        }
    }

    /**
     * Parses every {@code .rosetta} under {@code builtinsDir} first, then under each of
     * {@code srcRoots} in the given order, and links them as one workspace.
     *
     * @param srcRoots   the corpus source roots; each is walked recursively
     * @param builtinsDir the builtins directory; may be {@code null} to load none, which is
     *                    only ever correct if a source root already contains them
     */
    public static Loaded load(List<Path> srcRoots, Path builtinsDir) throws IOException {
        List<Path> builtinFiles =
                builtinsDir == null ? List.of() : rosettaFilesUnder(builtinsDir);
        List<Path> corpusFiles = new ArrayList<>();
        for (Path root : srcRoots) {
            corpusFiles.addAll(rosettaFilesUnder(root));
        }

        List<RModel> models = new ArrayList<>(builtinFiles.size() + corpusFiles.size());
        List<String> failures = new ArrayList<>();

        long parseStart = System.nanoTime();
        for (Path p : builtinFiles) {
            parseInto(models, p, failures);
        }
        for (Path p : corpusFiles) {
            parseInto(models, p, failures);
        }
        long parseMs = millisSince(parseStart);

        if (!failures.isEmpty()) {
            // A partly-parsed corpus produces counts that look plausible and are wrong.
            // Fail loudly instead.
            throw new IOException("parse failures (" + failures.size() + "): "
                    + String.join("; ", failures.subList(0, Math.min(10, failures.size()))));
        }

        long linkStart = System.nanoTime();
        // Single-closure population (one version of each of builtins/cdm/iso/drr), so the
        // cell-provenance overload RWorkspace.build(files, cellRoots) would be a no-op here:
        // its javadoc states an empty cell list IS the plain build(List) behaviour, and cell
        // preference only ever changes resolution where duplicate FQNs exist across cells.
        RLinkingResult linking = RWorkspace.build(models);
        long linkMs = millisSince(linkStart);

        return new Loaded(linking, List.copyOf(models), builtinFiles.size(), parseMs, linkMs);
    }

    private static void parseInto(List<RModel> models, Path file, List<String> failures) {
        try {
            models.add(AstBuilder.buildFromFile(file));
        } catch (Exception e) {
            failures.add(file + " -- " + e);
        }
    }

    /** Whether this model came from one of the two builtin files. */
    public static boolean isBuiltin(RModel model) {
        String file = model.sourceRange() == null ? null : model.sourceRange().file();
        if (file == null) {
            return false;
        }
        String name = fileName(file);
        return BUILTIN_FILE_NAMES.contains(name);
    }

    /** The last path segment of a source-range file string (either separator). */
    public static String fileName(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
