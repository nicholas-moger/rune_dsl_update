package com.regnosys.rosetta.generator.java;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Revert-safety anchor for {@link CompileGate}: proves the in-memory single-source
 * compile + per-file verdict bucketing works. Runs in the routine suite (tiny — no
 * corpus load). The W42 compile-gate (PR #233) is only as trustworthy as this engine,
 * so a deliberately-broken source MUST classify NON_COMPILING and a good one COMPILES.
 */
class CompileGateTest {

    @Test
    void compilesGoodSource_andRejectsBrokenSource() throws Exception {
        CompileGate gate = new CompileGate();
        List<String> cp = CompileGate.jvmClasspath();
        Path out = Files.createTempDirectory("compile-gate-anchor");

        CompileGate.FileResult good = gate.classifyForkFile(
                "anchor/Good.java",
                "package anchor; public class Good { public int n() { return 1; } }",
                cp, out.resolve("good"));
        assertEquals(CompileGate.Verdict.COMPILES, good.verdict());
        assertTrue(good.diagnostics().isEmpty());

        CompileGate.FileResult bad = gate.classifyForkFile(
                "anchor/Bad.java",
                "package anchor; public class Bad { public int n() { return notAThing(); } }",
                cp, out.resolve("bad"));
        assertEquals(CompileGate.Verdict.NON_COMPILING, bad.verdict());
        assertFalse(bad.diagnostics().isEmpty());
        assertEquals("cannot-find-symbol", bad.category());
    }
}
