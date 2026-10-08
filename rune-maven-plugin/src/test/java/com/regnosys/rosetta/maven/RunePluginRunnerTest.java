package com.regnosys.rosetta.maven;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end runner locks on a mini fixture corpus: the REAL builtin models
 * ({@code basictypes.rosetta}/{@code annotations.rosetta} from the frozen
 * 9.83.0 test corpus) are zipped into a temp jar and supplied through the
 * {@code classPathLookupFilter} channel — the exact upstream mechanism (the
 * {@code rune-runtime} jar on the consumer's compile classpath). Skips via
 * assumption when the local corpus clone is absent (CI / fresh clone), the
 * corpus-gate convention.
 */
class RunePluginRunnerTest {

    private static final Path BUILTINS_DIR =
            Path.of("../test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model");

    @TempDir
    Path tmp;

    private Path builtinsJar;
    private final List<String> logged = new ArrayList<>();

    private final RunePluginRunner.RunnerLog log = new RunePluginRunner.RunnerLog() {
        @Override
        public void info(String message) {
            logged.add("[INFO] " + message);
        }

        @Override
        public void warn(String message) {
            logged.add("[WARNING] " + message);
        }

        @Override
        public void error(String message) {
            logged.add("[ERROR] " + message);
        }
    };

    @BeforeEach
    void zipBuiltins() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(BUILTINS_DIR),
                "builtins corpus absent — runner locks skipped (CI / fresh clone)");
        builtinsJar = tmp.resolve("rune-runtime-fixture.jar");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(builtinsJar))) {
            for (String name : List.of("annotations.rosetta", "basictypes.rosetta")) {
                zip.putNextEntry(new ZipEntry("model/" + name));
                zip.write(Files.readAllBytes(BUILTINS_DIR.resolve(name)));
                zip.closeEntry();
            }
        }
    }

    private Path writeModel(String dirName, String fileName, String content) throws IOException {
        Path root = tmp.resolve(dirName);
        Files.createDirectories(root);
        Files.writeString(root.resolve(fileName), content);
        return root;
    }

    private RunePluginRunner runner(Path sourceRoot, RosettaConfigFile config, Path outDir,
                                    boolean failOnValidationError) {
        return new RunePluginRunner(
                List.of(sourceRoot),
                List.of(builtinsJar.toAbsolutePath().toString()),
                // The measured consumer filter shape, retargeted at the fixture jar.
                ".*rune-runtime-fixture\\.jar",
                config,
                outDir,
                failOnValidationError,
                log);
    }

    @Test
    void cleanModelGeneratesAndPrintsNothing() throws IOException {
        Path root = writeModel("src-clean", "mini.rosetta", """
                namespace test.mini
                version "1"

                type Foo:
                    bar string (1..1)

                enum Colour:
                    RED
                    BLUE
                """);
        Path out = tmp.resolve("out-clean");
        RunePluginRunner.Result result =
                runner(root, RosettaConfigFile.defaults(), out, true).run();

        assertEquals(0, result.errors());
        assertEquals(0, result.warnings());
        assertTrue(Files.isRegularFile(out.resolve("test/mini/Foo.java")),
                "POJO for Foo expected under the namespace path");
        assertTrue(Files.isRegularFile(out.resolve("test/mini/Colour.java")),
                "enum Colour expected under the namespace path");
        assertTrue(result.filesWritten() > 0);
        assertEquals(3, result.modelsParsed(), "2 builtins from the jar channel + 1 source model");
    }

    @Test
    void warningPrintsInTheUpstreamIssueLineFormat() throws IOException {
        Path root = writeModel("src-warn", "warn.rosetta", """
                namespace test.warn
                version "1"

                type lowercased:
                    bar string (1..1)
                """);
        Path out = tmp.resolve("out-warn");
        RunePluginRunner.Result result =
                runner(root, RosettaConfigFile.defaults(), out, true).run();

        assertEquals(0, result.errors());
        assertEquals(1, result.warnings(), "the naming validator fires on the lowercase type name");
        String expectedPrefix = "[WARNING] WARNING:Type name should start with a capital (file:/";
        assertTrue(logged.stream().anyMatch(l -> l.startsWith(expectedPrefix)
                        && l.contains("warn.rosetta line : ") && l.contains(" column : ")),
                "expected an upstream-format warning line, got: " + logged);
    }

    @Test
    void validationErrorFailsTheBuildWhenConfigured() throws IOException {
        Path root = writeModel("src-err", "err.rosetta", """
                namespace test.err
                version "1"

                type Broken:
                    bar Missing (1..1)
                """);
        Path out = tmp.resolve("out-err");
        assertThrows(RunePluginRunner.ValidationFailedException.class,
                () -> runner(root, RosettaConfigFile.defaults(), out, true).run());
        assertTrue(logged.stream().anyMatch(l -> l.startsWith("[ERROR] ERROR:")),
                "the linking error must print before the failure, got: " + logged);
    }

    @Test
    void siblingPrefixDirectoryStaysOutOfSourceScope() throws IOException {
        // The leak vector: a classpath DIRECTORY element whose absolute path
        // string-prefix-extends a source root (src-scope vs src-scope-lib).
        // Raw startsWith scoping would claim its diagnostics for the
        // source-scoped stream; the boundary check must keep them out.
        Path root = writeModel("src-scope", "clean.rosetta", """
                namespace test.scope
                version "1"

                type Fine:
                    bar string (1..1)
                """);
        Path libDir = writeModel("src-scope-lib", "lib.rosetta", """
                namespace lib.scope
                version "1"

                type leaky:
                    bar string (1..1)
                """);
        Path out = tmp.resolve("out-scope");
        RunePluginRunner.Result result = new RunePluginRunner(
                List.of(root),
                List.of(builtinsJar.toAbsolutePath().toString(), libDir.toAbsolutePath().toString()),
                ".*",
                RosettaConfigFile.defaults(),
                out,
                true,
                log).run();

        assertEquals(0, result.errors());
        assertEquals(0, result.warnings(),
                "the sibling library dir's naming warning must not leak into the source-scoped stream");
        assertTrue(logged.stream().noneMatch(l -> l.contains("lib.rosetta")),
                "no stream line may cite the sibling dir's file, got: " + logged);
    }

    @Test
    void namespaceFilterScopesEmission() throws IOException {
        Path root = tmp.resolve("src-two");
        Files.createDirectories(root);
        Files.writeString(root.resolve("a.rosetta"), """
                namespace keep.me
                version "1"

                type Kept:
                    bar string (1..1)
                """);
        Files.writeString(root.resolve("b.rosetta"), """
                namespace drop.me
                version "1"

                type Dropped:
                    bar string (1..1)
                """);
        Path out = tmp.resolve("out-two");
        Path yml = tmp.resolve("cfg.yml");
        Files.writeString(yml, """
                generators:
                  namespaces:
                  - keep.*
                """);
        RunePluginRunner.Result result =
                runner(root, RosettaConfigFile.load(yml), out, true).run();

        assertEquals(0, result.errors());
        assertTrue(Files.isRegularFile(out.resolve("keep/me/Kept.java")));
        assertTrue(Files.notExists(out.resolve("drop/me/Dropped.java")),
                "the drop.me namespace is outside the accept-list and must not emit");
    }
}
