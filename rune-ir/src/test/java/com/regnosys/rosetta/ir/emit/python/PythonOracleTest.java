package com.regnosys.rosetta.ir.emit.python;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The python oracle's positive controls — the instrument proven able to fail, and proven NOT to fail on the class the two private
 * runners it replaces could not survive (#630). Every control is timing-independent by construction: the timeout control's child
 * sleeps far past its budget whatever the box's process-start tail, the drain control's budget is sixty times its job, and the
 * exit-code control needs no clock at all. All three are gated on python being on the PATH, as every python-backed emitter test is.
 */
class PythonOracleTest {

    /** A child alive past a 2 s budget fails WITH its elapsed (at least the budget), the budget, the label and the partial-output section. */
    @Test void timeoutReportsElapsedBudgetAndLabel() {
        Assumptions.assumeTrue(PythonOracle.pythonAvailable(), "python not on PATH — the oracle's controls skipped");
        AssertionError e = assertThrows(AssertionError.class, () -> PythonOracle.run(
                new ProcessBuilder("python", "-c", "import time; time.sleep(120)"), "the sleeping child", 2));
        String m = e.getMessage();
        assertTrue(m.startsWith("python timed out after "), m);
        long elapsed = Long.parseLong(m.substring("python timed out after ".length(), m.indexOf(" ms")));
        assertTrue(elapsed >= 2_000L, m);
        assertTrue(m.contains(" ms (budget 2 s) for: the sleeping child\n--- partial output ---\n"), m);
    }

    /**
     * A child printing 1 MB — far past any pipe buffer — exits within a 60 s budget and every byte arrives: the concurrent drain.
     * The read-after-wait shape of the replaced runners blocks such a child on the full pipe until the budget expires (measured on
     * this box: {@code scratch/old-runner-pipe-stall.txt}, local).
     */
    @Test void largeOutputIsDrainedWhileWaiting() throws Exception {
        Assumptions.assumeTrue(PythonOracle.pythonAvailable(), "python not on PATH — the oracle's controls skipped");
        PythonOracle.Result r = PythonOracle.run(
                new ProcessBuilder("python", "-c", "import sys; sys.stdout.write('x' * 1048576); sys.stdout.write('\\nEND\\n')"),
                "the 1 MB child", 60);
        String out = r.output().replace("\r\n", "\n");
        assertEquals(0, r.exit(), out.length() > 200 ? out.substring(0, 200) : out);
        assertEquals(1048576 + 5, out.length());
        assertTrue(out.endsWith("\nEND\n"));
    }

    /** A child that prints and exits non-zero returns its exit code AND its output — the caller decides what a non-zero exit means. */
    @Test void nonZeroExitReturnsTheOutput() throws Exception {
        Assumptions.assumeTrue(PythonOracle.pythonAvailable(), "python not on PATH — the oracle's controls skipped");
        PythonOracle.Result r = PythonOracle.run(
                new ProcessBuilder("python", "-c", "import sys; print('before-exit'); sys.exit(3)"), "the exit-3 child");
        assertEquals(3, r.exit());
        assertEquals("before-exit", r.output().strip());
        assertTrue(r.elapsedMillis() >= 0L);
    }
}
