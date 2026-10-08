package com.regnosys.rosetta.harness.matrix;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReporterTest {

    private static final Version V6160 = Version.parse("6.16.0");
    private static final Version V7031 = Version.parse("7.0.0-dev.111");

    @Test
    void write_emits_one_ndjson_line_per_result(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("report.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "base", V6160, ElementKind.TYPE, Path.of("a.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 42));
            r.record(Reporter.Result.failed(mc, 17, "assertion X"));
            r.record(Reporter.Result.skipped(mc, 0, "missing corpus"));
        }
        List<String> lines = Files.readAllLines(out);
        assertEquals(3, lines.size());
    }

    @Test
    void each_line_is_parseable_json_with_required_fields(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("report.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "base", V6160, ElementKind.TYPE, Path.of("a.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 42));
        }
        String line = Files.readAllLines(out).get(0);
        // Mandatory fields present as `"key":` tokens, literal match (no regex).
        for (String key : new String[]{
                "\"corpus\":", "\"project\":", "\"version\":",
                "\"kind\":", "\"source\":", "\"outcome\":",
                "\"duration_ms\":", "\"message\":"}) {
            assertTrue(line.contains(key), "expected key " + key + " in: " + line);
        }
        assertTrue(line.contains("\"outcome\":\"PASSED\""));
        assertTrue(line.contains("\"corpus\":\"CDM\""));
        assertTrue(line.contains("\"version\":\"6.16.0\""));
    }

    @Test
    void message_is_null_for_passed_and_string_for_failed_or_skipped(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("r.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "", V6160, ElementKind.ENUM, Path.of("x.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 1));
            r.record(Reporter.Result.failed(mc, 2, "boom"));
            r.record(Reporter.Result.skipped(mc, 3, "no corpus"));
        }
        List<String> lines = Files.readAllLines(out);
        assertTrue(lines.get(0).contains("\"message\":null"));
        assertTrue(lines.get(1).contains("\"message\":\"boom\""));
        assertTrue(lines.get(2).contains("\"message\":\"no corpus\""));
    }

    @Test
    void special_characters_in_message_are_json_escaped(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("r.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.DRR, "", V7031, ElementKind.RULE, Path.of("y.rosetta"));
        String msg = "line1\nline2 with \"quotes\" and \\backslash\\ and \t tab";
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.failed(mc, 0, msg));
        }
        String line = Files.readAllLines(out).get(0);
        // The raw characters must not survive — that would break NDJSON (one record per line).
        assertFalse(line.contains("\n\t"), "tab-after-newline sequence would indicate unescaped line break");
        long newlines = line.chars().filter(c -> c == '\n').count();
        assertEquals(0, newlines, "message newlines must be \\n-escaped; NDJSON has one record per physical line");
        assertTrue(line.contains("\\n"));
        assertTrue(line.contains("\\\""));
        assertTrue(line.contains("\\\\"));
        assertTrue(line.contains("\\t"));
    }

    @Test
    void each_record_ends_with_newline_even_the_last(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("r.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.ISO20022, "", Version.parse("1.36.0"), ElementKind.REPORT, Path.of("z.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 5));
        }
        String content = Files.readString(out);
        assertTrue(content.endsWith("\n"), "NDJSON convention: every record ends with \\n, including the last");
    }

    @Test
    void source_is_serialised_with_forward_slashes_even_on_windows(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("r.ndjson");
        // Use a path with a separator that differs from the platform default to
        // prove the reporter normalises — cross-platform comparability matters
        // for the NDJSON artifact to be stable on CI (Linux) and dev machines (Windows).
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.CDM, "base", V6160, ElementKind.TYPE,
                Path.of("test-corpus", "cdm", "cdm-6.16.0", "rosetta-source", "a.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 1));
        }
        String line = Files.readAllLines(out).get(0);
        assertTrue(line.contains("\"source\":\"test-corpus/cdm/cdm-6.16.0/rosetta-source/a.rosetta\""),
                "source path must use forward slashes in NDJSON: " + line);
    }

    @Test
    void open_tolerates_path_with_no_parent(@TempDir Path dir) throws IOException {
        // Path.of("report.ndjson").toAbsolutePath().getParent() is non-null, but
        // Path.of("report.ndjson").getParent() is null. Earlier implementation
        // called Files.createDirectories on the absolute parent which happens
        // to work but was guarded against NPE on the non-absolute form. This
        // test pins the contract: a filename-only path must be accepted.
        Path filenameOnly = Path.of("report-no-parent.ndjson");
        try {
            MatrixCoordinate mc = new MatrixCoordinate(
                    Corpus.CDM, "", V6160, ElementKind.TYPE, Path.of("a.rosetta"));
            try (Reporter r = Reporter.open(filenameOnly)) {
                r.record(Reporter.Result.passed(mc, 1));
            }
            assertTrue(Files.exists(filenameOnly));
        } finally {
            Files.deleteIfExists(filenameOnly);
        }
    }

    @Test
    void project_defaults_to_empty_string_and_preserved_in_output(@TempDir Path dir) throws IOException {
        Path out = dir.resolve("r.ndjson");
        MatrixCoordinate mc = new MatrixCoordinate(
                Corpus.RUNE_FPML, "", Version.parse("1.5.0"), ElementKind.TYPE, Path.of("a.rosetta"));
        try (Reporter r = Reporter.open(out)) {
            r.record(Reporter.Result.passed(mc, 1));
        }
        String line = Files.readAllLines(out).get(0);
        assertTrue(line.contains("\"project\":\"\""));
    }
}
