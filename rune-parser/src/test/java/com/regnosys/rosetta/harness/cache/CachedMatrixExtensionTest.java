package com.regnosys.rosetta.harness.cache;

import com.regnosys.rosetta.harness.matrix.Corpus;
import com.regnosys.rosetta.harness.matrix.ElementKind;
import com.regnosys.rosetta.harness.matrix.MatrixCoordinate;
import com.regnosys.rosetta.harness.matrix.Version;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CachedMatrixExtensionTest {

    private static final Version V = Version.parse("6.16.0");

    // ---- resolveMode ----

    @Test
    void resolveMode_defaults_to_read_write_when_sys_prop_unset() {
        String prior = System.clearProperty(CachedMatrixExtension.MODE_PROPERTY);
        try {
            assertEquals(CachedMatrixExtension.Mode.READ_WRITE,
                    CachedMatrixExtension.resolveMode());
        } finally {
            if (prior != null) System.setProperty(CachedMatrixExtension.MODE_PROPERTY, prior);
        }
    }

    @Test
    void resolveMode_parses_case_insensitive() {
        String prior = System.getProperty(CachedMatrixExtension.MODE_PROPERTY);
        try {
            System.setProperty(CachedMatrixExtension.MODE_PROPERTY, "read_only");
            assertEquals(CachedMatrixExtension.Mode.READ_ONLY,
                    CachedMatrixExtension.resolveMode());
            System.setProperty(CachedMatrixExtension.MODE_PROPERTY, "write_only");
            assertEquals(CachedMatrixExtension.Mode.WRITE_ONLY,
                    CachedMatrixExtension.resolveMode());
            System.setProperty(CachedMatrixExtension.MODE_PROPERTY, "DISABLED");
            assertEquals(CachedMatrixExtension.Mode.DISABLED,
                    CachedMatrixExtension.resolveMode());
        } finally {
            if (prior == null) System.clearProperty(CachedMatrixExtension.MODE_PROPERTY);
            else System.setProperty(CachedMatrixExtension.MODE_PROPERTY, prior);
        }
    }

    @Test
    void resolveMode_falls_back_to_read_write_on_garbage() {
        String prior = System.getProperty(CachedMatrixExtension.MODE_PROPERTY);
        try {
            System.setProperty(CachedMatrixExtension.MODE_PROPERTY, "nonsense");
            assertEquals(CachedMatrixExtension.Mode.READ_WRITE,
                    CachedMatrixExtension.resolveMode());
        } finally {
            if (prior == null) System.clearProperty(CachedMatrixExtension.MODE_PROPERTY);
            else System.setProperty(CachedMatrixExtension.MODE_PROPERTY, prior);
        }
    }

    // ---- resolveRoot ----

    @Test
    void resolveRoot_defaults_to_target_matrix_cache() {
        String prior = System.clearProperty(CachedMatrixExtension.ROOT_PROPERTY);
        try {
            assertEquals(CachedMatrixExtension.DEFAULT_ROOT,
                    CachedMatrixExtension.resolveRoot());
        } finally {
            if (prior != null) System.setProperty(CachedMatrixExtension.ROOT_PROPERTY, prior);
        }
    }

    @Test
    void resolveRoot_honours_sys_prop_override() {
        String prior = System.getProperty(CachedMatrixExtension.ROOT_PROPERTY);
        try {
            System.setProperty(CachedMatrixExtension.ROOT_PROPERTY, "target/alt-cache");
            assertEquals(Path.of("target/alt-cache"),
                    CachedMatrixExtension.resolveRoot());
        } finally {
            if (prior == null) System.clearProperty(CachedMatrixExtension.ROOT_PROPERTY);
            else System.setProperty(CachedMatrixExtension.ROOT_PROPERTY, prior);
        }
    }

    @Test
    void resolveRoot_empty_sys_prop_falls_back_to_default() {
        String prior = System.getProperty(CachedMatrixExtension.ROOT_PROPERTY);
        try {
            System.setProperty(CachedMatrixExtension.ROOT_PROPERTY, "");
            assertEquals(CachedMatrixExtension.DEFAULT_ROOT,
                    CachedMatrixExtension.resolveRoot());
        } finally {
            if (prior == null) System.clearProperty(CachedMatrixExtension.ROOT_PROPERTY);
            else System.setProperty(CachedMatrixExtension.ROOT_PROPERTY, prior);
        }
    }

    // ---- sha256OfCanonicalisedFile ----

    @Test
    void sha256OfCanonicalisedFile_cross_platform_line_endings_hash_equal(@TempDir Path dir) throws IOException {
        Path lfFile = dir.resolve("lf.rosetta");
        Path crlfFile = dir.resolve("crlf.rosetta");
        Files.writeString(lfFile, "namespace x\ntype Foo:\n", StandardCharsets.UTF_8);
        Files.writeString(crlfFile, "namespace x\r\ntype Foo:\r\n", StandardCharsets.UTF_8);
        assertEquals(
                CachedMatrixExtension.sha256OfCanonicalisedFile(lfFile),
                CachedMatrixExtension.sha256OfCanonicalisedFile(crlfFile),
                "CRLF should canonicalise to LF before hashing");
    }

    @Test
    void sha256OfCanonicalisedFile_bom_stripped_before_hash(@TempDir Path dir) throws IOException {
        Path noBom = dir.resolve("nobom.rosetta");
        Path withBom = dir.resolve("bom.rosetta");
        Files.writeString(noBom, "type Foo:", StandardCharsets.UTF_8);
        // Spell the BOM explicitly — an embedded BOM char is
        // invisible in diffs and prone to editor/copy-paste corruption.
        Files.writeString(withBom, "\uFEFFtype Foo:", StandardCharsets.UTF_8);
        assertEquals(
                CachedMatrixExtension.sha256OfCanonicalisedFile(noBom),
                CachedMatrixExtension.sha256OfCanonicalisedFile(withBom),
                "leading BOM should strip before hashing");
    }

    @Test
    void sha256OfCanonicalisedFile_different_content_different_hash(@TempDir Path dir) throws IOException {
        Path a = dir.resolve("a.rosetta");
        Path b = dir.resolve("b.rosetta");
        Files.writeString(a, "type Foo:", StandardCharsets.UTF_8);
        Files.writeString(b, "type Bar:", StandardCharsets.UTF_8);
        assertNotEquals(
                CachedMatrixExtension.sha256OfCanonicalisedFile(a),
                CachedMatrixExtension.sha256OfCanonicalisedFile(b));
    }

    // ---- sha256OfClassBytes ----

    @Test
    void sha256OfClassBytes_returns_lowercase_hex_for_known_class() {
        String hash = CachedMatrixExtension.sha256OfClassBytes(CachedMatrixExtensionTest.class);
        assertEquals(64, hash.length());
        assertTrue(hash.chars().allMatch(c -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')));
    }

    @Test
    void sha256OfClassBytes_is_stable_across_invocations() {
        String first = CachedMatrixExtension.sha256OfClassBytes(CachedMatrixExtensionTest.class);
        String second = CachedMatrixExtension.sha256OfClassBytes(CachedMatrixExtensionTest.class);
        assertEquals(first, second);
    }

    @Test
    void sha256OfClassBytes_differs_between_different_classes() {
        String here = CachedMatrixExtension.sha256OfClassBytes(CachedMatrixExtensionTest.class);
        String other = CachedMatrixExtension.sha256OfClassBytes(CachedMatrixExtension.class);
        assertNotEquals(here, other);
    }

    // ---- extractCoordinate ----

    @Test
    void extractCoordinate_finds_coord_in_argument_list() {
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "", V, ElementKind.TYPE, Path.of("a.rosetta"));
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgs(List.of("ignored", mc, 42));
        Optional<MatrixCoordinate> got = CachedMatrixExtension.extractCoordinate(ctx);
        assertTrue(got.isPresent());
        assertSame(mc, got.get());
    }

    @Test
    void extractCoordinate_returns_empty_when_no_coord_present() {
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgs(List.of("x", 42));
        assertEquals(Optional.empty(), CachedMatrixExtension.extractCoordinate(ctx));
    }

    @Test
    void extractCoordinate_returns_empty_on_empty_arg_list() {
        FakeReflectiveContext ctx = FakeReflectiveContext.withArgs(List.of());
        assertEquals(Optional.empty(), CachedMatrixExtension.extractCoordinate(ctx));
    }

    // ---- describeThrowable ----

    @Test
    void describeThrowable_includes_class_and_message() {
        String s = CachedMatrixExtension.describeThrowable(
                new IllegalStateException("boom"));
        assertTrue(s.contains("IllegalStateException"));
        assertTrue(s.contains("boom"));
    }

    @Test
    void describeThrowable_handles_null_message() {
        String s = CachedMatrixExtension.describeThrowable(new RuntimeException());
        assertTrue(s.contains("RuntimeException"));
        // no NPE
    }

    // ---- FakeInvocation enforces single-dispatch contract ----

    @Test
    void fake_invocation_throws_on_double_proceed() throws Throwable {
        FakeInvocation inv = new FakeInvocation(() -> {});
        inv.proceed();
        assertThrows(IllegalStateException.class, inv::proceed);
    }

    @Test
    void fake_invocation_throws_on_double_skip() {
        FakeInvocation inv = new FakeInvocation(() -> {});
        inv.skip();
        assertThrows(IllegalStateException.class, inv::skip);
    }

    @Test
    void fake_invocation_throws_on_skip_after_proceed() throws Throwable {
        FakeInvocation inv = new FakeInvocation(() -> {});
        inv.proceed();
        assertThrows(IllegalStateException.class, inv::skip);
    }

    @Test
    void fake_invocation_throws_on_proceed_after_skip() {
        FakeInvocation inv = new FakeInvocation(() -> {});
        inv.skip();
        assertThrows(IllegalStateException.class, inv::proceed);
    }
}
