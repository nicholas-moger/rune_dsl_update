package com.regnosys.rosetta.corpus;

import com.regnosys.rosetta.testutil.CorpusWalker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Frozen rune-dsl 9.83.0 baseline-corpus guard — <b>local-only by design</b>.
 *
 * <p><b>Why this exists.</b> The working {@code test-corpus/} is cloned-on-demand
 * and CI's {@code build-and-test} job never clones it, so corpus-derived goldens
 * (the symbol-table snapshots + the RRootElement audit doc) silently went stale at
 * the PR&nbsp;#85 9.83.0 rebaseline: they were last memorialised against the
 * pre-#85 1,486/3,112-file corpus and never refreshed when the corpus was trimmed
 * to the 5-cell 9.83.0 set (236/215). Locally the goldens were red; in CI the
 * tests skipped or vacuously passed. That "skip masks staleness" failure mode is
 * exactly what this guard closes — <em>locally</em>, without adding any GitHub
 * Actions load.
 *
 * <p><b>Architecture.</b> The complete 9.83.0 corpus — every {@code .rosetta}
 * source input <em>and</em> every {@code generated/java} golden the D11 byte-compare
 * (and the published parity %) measures against, plus (since PR #607) the goldens
 * the four resolution-only cells carry for the optimised route's pair gate, generated
 * by each cell's OWN toolchain and never byte-compared — is frozen ADD-ONLY at
 * {@code corpus-baseline-9.83/} (gitignored; on the CLAUDE.md (local) do-not-delete list):
 * nothing frozen is ever rewritten or removed, and a cell's goldens are added in place
 * exactly once (PR #607 grew the freeze by 32,619 files, removing none).
 * Tests + regen run against the separate working {@code test-corpus/} copy. This
 * committed manifest ({@code corpus-baseline-9.83.manifest}) is the durable
 * contract that survives a fresh clone where the frozen folder is absent.
 *
 * <p><b>What the guard asserts.</b> Every manifest entry is present in the working
 * corpus with a byte-identical SHA-256, and the working corpus carries no
 * in-scope file the manifest does not list. Any drift — a re-clone of a different
 * tag, a {@code mvn process-sources} re-materialisation with a different generator
 * — fails loudly with a precise diff. When the corpus is absent (CI, fresh clone)
 * the file-level checks {@code assumeTrue}-skip; the {@link #manifestIsSelfConsistent()}
 * header check still runs everywhere because it only reads the committed manifest.
 *
 * <p><b>Scope</b> mirrors the manifest generator: {@code *.rosetta} plus every
 * {@code .java} under a {@code generated/java} directory (the cdm/drr/rune-fpml
 * {@code src/generated/java} goldens and the iso20022 {@code target/.../generated/java}
 * goldens). Other frozen fixtures (xml/json/xsd) are available in
 * {@code corpus-baseline-9.83/} for ad-hoc comparison but are not golden
 * determinants, so they are not manifest-tracked.
 */
class Corpus983BaselineManifestTest {

    private static final Path REPO = CorpusWalker.repoRoot();
    private static final Path WORKING = REPO.resolve("test-corpus");
    private static final Path FROZEN = REPO.resolve("corpus-baseline-9.83");
    private static final Path MANIFEST =
            REPO.resolve("rune-parser/src/test/resources/corpus-baseline-9.83.manifest");

    /** Max per-category drift lines to print before truncating the failure message. */
    private static final int MAX_REPORTED = 25;

    // =========================================================================
    // Guard: working corpus must match the frozen 9.83 baseline manifest.
    // =========================================================================

    @Test
    void workingCorpusMatchesBaselineManifest() throws IOException {
        assumeTrue(workingCorpusPresent(),
                "test-corpus/ not present (cloned-on-demand; absent in CI) — "
                        + "frozen-baseline guard skipped. Gating is the local full-suite run.");
        assertTrue(Files.exists(MANIFEST),
                "Committed baseline manifest missing at " + MANIFEST + ". Regenerate via:\n"
                        + "  mvn -f rune-parser/pom.xml test "
                        + "-Dtest=Corpus983BaselineManifestTest -Dcorpus.manifest.regen=true");

        Map<String, String> expected = parseManifest(MANIFEST);
        Map<String, Path> actual = collectScopedFiles(WORKING);

        // Hash the working files in parallel — ~35k small files; per-file open
        // dominates, so parallelism is the win, not the digest throughput.
        Map<String, String> actualHashes = new ConcurrentHashMap<>();
        actual.entrySet().parallelStream()
                .forEach(e -> actualHashes.put(e.getKey(), sha256(e.getValue())));

        List<String> missing = new ArrayList<>();   // in manifest, absent on disk
        List<String> mismatched = new ArrayList<>();
        for (Map.Entry<String, String> e : expected.entrySet()) {
            String hash = actualHashes.get(e.getKey());
            if (hash == null) {
                missing.add(e.getKey());
            } else if (!hash.equals(e.getValue())) {
                mismatched.add(e.getKey());
            }
        }
        List<String> extra = new ArrayList<>();      // on disk, not in manifest
        for (String key : actualHashes.keySet()) {
            if (!expected.containsKey(key)) {
                extra.add(key);
            }
        }

        if (!missing.isEmpty() || !mismatched.isEmpty() || !extra.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Working test-corpus/ has DRIFTED from the frozen rune-dsl 9.83.0 baseline.\n")
              .append("This guard pins the corpus the symbol-table snapshots, RRootElement audit,\n")
              .append("and D11 byte-compare (parity %) all derive from. Re-sync the working copy\n")
              .append("from corpus-baseline-9.83/, or — if the corpus legitimately changed — re-freeze\n")
              .append("the baseline + regenerate this manifest (-Dcorpus.manifest.regen=true) and all\n")
              .append("dependent goldens, then commit.\n");
            appendDrift(sb, "MISSING (in manifest, absent from working corpus)", missing);
            appendDrift(sb, "HASH MISMATCH (content drifted)", mismatched);
            appendDrift(sb, "EXTRA (in working corpus, not in manifest)", extra);
            fail(sb.toString());
        }
    }

    // =========================================================================
    // Always-on (no corpus needed): the committed manifest is self-consistent.
    // Reads only the committed file, so it gates everywhere — including CI and a
    // fresh clone — catching a truncated / hand-edited / mis-counted manifest.
    // =========================================================================

    @Test
    void manifestIsSelfConsistent() throws IOException {
        assertTrue(Files.exists(MANIFEST), "Committed baseline manifest missing at " + MANIFEST);
        List<String> lines = Files.readAllLines(MANIFEST, StandardCharsets.UTF_8);

        int headerFiles = -1;
        int headerRosetta = -1;
        int headerGenerated = -1;
        int bodyRows = 0;
        int bodyRosetta = 0;
        int bodyGenerated = 0;
        for (String line : lines) {
            if (line.startsWith("# files:")) {
                headerFiles = Integer.parseInt(line.substring("# files:".length()).trim());
            } else if (line.startsWith("# rosetta:")) {
                headerRosetta = Integer.parseInt(line.substring("# rosetta:".length()).trim());
            } else if (line.startsWith("# generated-java:")) {
                headerGenerated = Integer.parseInt(line.substring("# generated-java:".length()).trim());
            } else if (!line.startsWith("#") && !line.isBlank()) {
                bodyRows++;
                String rel = line.substring(0, line.indexOf('\t'));
                if (rel.endsWith(".rosetta")) {
                    bodyRosetta++;
                } else {
                    bodyGenerated++;
                }
            }
        }

        assertTrue(headerFiles >= 0 && headerRosetta >= 0 && headerGenerated >= 0,
                "Manifest header must declare '# files:', '# rosetta:' and '# generated-java:' counts");
        assertEquals(headerFiles, bodyRows,
                "Manifest '# files:' header (" + headerFiles + ") disagrees with the actual row count ("
                        + bodyRows + ") — truncated or hand-edited? Regenerate via -Dcorpus.manifest.regen=true");
        assertEquals(headerRosetta, bodyRosetta, "Manifest '# rosetta:' header disagrees with body");
        assertEquals(headerGenerated, bodyGenerated, "Manifest '# generated-java:' header disagrees with body");
        assertEquals(headerFiles, headerRosetta + headerGenerated,
                "Manifest '# files:' must equal '# rosetta:' + '# generated-java:'");
    }

    // =========================================================================
    // Regen (dev-only): re-hash the FROZEN baseline and rewrite the manifest.
    // Fails intentionally so the human commits the regenerated manifest explicitly.
    // =========================================================================

    @Test
    @EnabledIfSystemProperty(named = "corpus.manifest.regen", matches = "true",
            disabledReason = "Dev-only manifest regen — run with -Dcorpus.manifest.regen=true")
    void regenerateManifestFromFrozenBaseline() throws IOException {
        assertTrue(Files.isDirectory(FROZEN),
                "Frozen baseline corpus-baseline-9.83/ not found at " + FROZEN
                        + " — freeze it from test-corpus/ before regenerating the manifest.");
        Map<String, Path> files = collectScopedFiles(FROZEN);
        Map<String, String> hashes = new ConcurrentHashMap<>();
        files.entrySet().parallelStream().forEach(e -> hashes.put(e.getKey(), sha256(e.getValue())));

        writeManifest(MANIFEST, new TreeMap<>(hashes));

        fail("Regenerated " + MANIFEST.toAbsolutePath().normalize() + " from the frozen baseline ("
                + hashes.size() + " entries). Test fails intentionally so the regen is committed "
                + "explicitly. Re-run without -Dcorpus.manifest.regen=true to verify.");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean workingCorpusPresent() {
        if (!Files.isDirectory(WORKING)) {
            return false;
        }
        try (Stream<Path> s = Files.walk(WORKING, 12)) {
            return s.anyMatch(p -> p.toString().endsWith(".rosetta"));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * True for the manifest scope: any {@code .rosetta}, or {@code .java} under a
     * {@code generated/java} dir — EXCLUDING the {@code chaos/} cell. This manifest is the
     * durable byte contract for the VENDORED, gitignored corpora (which a fresh clone lacks);
     * the chaos cell is AUTHORED and fully COMMITTED (the v3.2 charter § 3), so git itself is
     * its byte contract and a second manifest would be a drifting copy of facts git already
     * pins. Its drift gates are the expander-determinism diff and the census test's
     * sink-digest pin, not this manifest.
     */
    private static boolean inScope(String relForwardSlash) {
        if (relForwardSlash.startsWith("chaos/")) {
            return false;
        }
        if (relForwardSlash.endsWith(".rosetta")) {
            return true;
        }
        return relForwardSlash.endsWith(".java") && relForwardSlash.contains("/generated/java/");
    }

    /** Map of {@code <relpath-forward-slash> -> absolute Path} for every in-scope file under {@code root}. */
    private static Map<String, Path> collectScopedFiles(Path root) {
        Map<String, Path> out = new TreeMap<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                String rel = root.relativize(p).toString().replace('\\', '/');
                if (inScope(rel)) {
                    out.put(rel, p);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("failed to walk " + root, e);
        }
        return out;
    }

    private static String sha256(Path file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // Stream the file into the digest rather than Files.readAllBytes — with
            // ~35k files hashed in parallel, holding each full file in memory adds
            // needless GC pressure; an 8 KiB buffer bounds the per-thread footprint.
            byte[] buf = new byte[8192];
            try (InputStream in = Files.newInputStream(file)) {
                int n;
                while ((n = in.read(buf)) != -1) {
                    md.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to hash " + file, e);
        }
    }

    /** Parse the committed manifest into {@code relpath -> sha256}, skipping {@code #} comments. */
    private static Map<String, String> parseManifest(Path manifest) throws IOException {
        Map<String, String> out = new TreeMap<>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            int tab = line.indexOf('\t');
            if (tab <= 0) {
                // A non-comment, non-blank row with no tab (or an empty relpath) means
                // the manifest is truncated / hand-corrupted — fail loud rather than
                // silently dropping the entry, which would let real drift go unchecked.
                throw new IllegalStateException(
                        "Malformed manifest row (expected '<relpath>\\t<sha256>'): " + line);
            }
            out.put(line.substring(0, tab), line.substring(tab + 1).trim());
        }
        return out;
    }

    /** Write the canonical manifest: date-free header + sorted {@code relpath\tsha} rows, LF-terminated. */
    private static void writeManifest(Path out, TreeMap<String, String> rows) throws IOException {
        int rosetta = (int) rows.keySet().stream().filter(k -> k.endsWith(".rosetta")).count();
        int generated = rows.size() - rosetta;
        StringBuilder sb = new StringBuilder();
        sb.append("# Frozen rune-dsl 9.83.0 baseline corpus manifest\n");
        sb.append("# Pins the correctness-critical corpus: every .rosetta source input AND every\n");
        sb.append("# generated/java golden the D11 byte-compare (and the parity %) measures against\n");
        sb.append("# OR the optimised route's pair gate reads (the resolution-only cells' own-toolchain\n");
        sb.append("# goldens, PR #607), so the working test-corpus/ cannot silently drift from the\n");
        sb.append("# frozen 9.83 baseline.\n");
        sb.append("# The full frozen corpus lives at corpus-baseline-9.83/ (gitignored, do-not-delete);\n");
        sb.append("# this committed manifest is the durable contract the guard checks. Guard:\n");
        sb.append("# Corpus983BaselineManifestTest (rune-parser, local-only; skips when corpus absent).\n");
        sb.append("# Regenerate ONLY from corpus-baseline-9.83/ (never a drifted working copy):\n");
        sb.append("#   mvn -f rune-parser/pom.xml test -Dtest=Corpus983BaselineManifestTest -Dcorpus.manifest.regen=true\n");
        sb.append("# Format: <relpath>\\t<sha256>  (sorted ordinal; LF; date-free for byte-stable diffs)\n");
        sb.append("# rosetta: ").append(rosetta).append('\n');
        sb.append("# generated-java: ").append(generated).append('\n');
        sb.append("# files: ").append(rows.size()).append('\n');
        for (Map.Entry<String, String> e : rows.entrySet()) {
            sb.append(e.getKey()).append('\t').append(e.getValue()).append('\n');
        }
        Files.createDirectories(out.getParent());
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void appendDrift(StringBuilder sb, String label, List<String> items) {
        if (items.isEmpty()) {
            return;
        }
        sb.append('\n').append(label).append(" (").append(items.size()).append("):\n");
        items.stream().sorted().limit(MAX_REPORTED).forEach(i -> sb.append("  ").append(i).append('\n'));
        if (items.size() > MAX_REPORTED) {
            sb.append("  ... and ").append(items.size() - MAX_REPORTED).append(" more\n");
        }
    }
}
