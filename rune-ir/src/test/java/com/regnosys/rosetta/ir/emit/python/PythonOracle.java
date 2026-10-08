package com.regnosys.rosetta.ir.emit.python;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * The python subprocess oracle shared by {@link IRPythonEmitterTest} and {@link IRPythonDeclarationEmitterTest} — ONE runner
 * (LAW 69; the two suites had carried a private copy each, both of the same shape).
 *
 * <p><b>Why it exists (#630, the seat's own chain).</b> Both private runners waited {@code 30 s} on the child BEFORE reading its
 * output and reported a timeout with neither the elapsed nor the output. On the seat's box that budget fired in two of three
 * chains of record with the seat's diff on rune-ir EMPTY — {@code s9c} @ {@code 82f354ca7}
 * ({@code navigationFunctionRunsAndPropagatesNoneThroughAnAbsentIntermediate}, 38.4 s, two review agents on the box) and
 * {@code s9d} @ {@code 6e64fb641} ({@code existenceCardinalityModifiersRun}, 31.0 s) — while a PASSING sibling of the same
 * {@code s9d} run took 17.7 s ({@code divisionRunsAtDecimal128Precision}) against the suite's ~1 s norm (the surefire report
 * {@code TEST-…IRPythonEmitterTest.xml} of that run; the chain logs {@code scratch/s9c-t-ir.log} / {@code s9d-t-ir.log}, local):
 * the box's process-start tail under a chain's IO load, not a hang of the emitted python. The same head re-taken alone was green
 * ({@code scratch/s9d-ir-retake.status}). Three changes, each measured by {@link PythonOracleTest}:
 * <ol>
 *   <li>the merged output is DRAINED CONCURRENTLY by a daemon thread, so a child that fills the pipe can never be the reason the
 *       parent's wait expires — the read-after-wait shape blocks on a full pipe ({@link Process}'s own javadoc: failure to promptly
 *       read the output stream may cause the subprocess to block); measured on this box with the old shape and a 1 MB print
 *       ({@code scratch/old-runner-pipe-stall.txt}, local);</li>
 *   <li>the budget is {@value #TIMEOUT_SECONDS} s — six times the old one, still finite: a genuine hang fails, with its elapsed;</li>
 *   <li>a timeout reports the ELAPSED, the budget, the label and the partial output; a passing run slower than
 *       {@value #SLOW_NOTE_SECONDS} s prints its elapsed to stderr — the next tail event is diagnosable from the surefire receipt
 *       instead of from a guess (the seat-8 housekeeping law: print what the next transient needs).</li>
 * </ol>
 */
final class PythonOracle {
    /** The per-invocation budget in seconds. */
    static final long TIMEOUT_SECONDS = 180;
    /** A passing run slower than this many seconds prints its elapsed to stderr. */
    static final long SLOW_NOTE_SECONDS = 5;
    /** The budget for the drain thread to see EOF after the child exited (round 2, cq SF-7). */
    static final long DRAIN_JOIN_SECONDS = 30;

    private PythonOracle() {}

    /** The child's exit code, its merged stdout+stderr and the wall-clock elapsed. */
    record Result(int exit, String output, long elapsedMillis) {}

    /** {@code python --version} exits 0 within 10 s — the assumption every python-backed emitter test is gated on. */
    static boolean pythonAvailable() {
        try {
            return run(new ProcessBuilder("python", "--version"), "python --version", 10).exit() == 0;
        } catch (Exception | AssertionError e) {
            return false;
        }
    }

    /** Runs {@code pb} under the default budget, {@link #TIMEOUT_SECONDS}. */
    static Result run(ProcessBuilder pb, String label) throws Exception {
        return run(pb, label, TIMEOUT_SECONDS);
    }

    /**
     * Runs {@code pb} with stderr merged into stdout, draining the merged stream concurrently while waiting, and returns the exit
     * code, the whole output and the elapsed. Throws {@link AssertionError} — the elapsed, the budget, the label and whatever output
     * had arrived in its message — in TWO cases (round 3, cq NIT-5): when the child has not exited within
     * {@code timeoutSeconds} (the child is destroyed then), and when the child HAS exited but its output stream stayed
     * open past {@link #DRAIN_JOIN_SECONDS} more (a grandchild holding the handle — nothing is destroyed, a passing run
     * is turned into a failure so a leaked handle cannot hang the parent past the {@code waitFor} budget).
     */
    static Result run(ProcessBuilder pb, String label, long timeoutSeconds) throws Exception {
        long t0 = System.nanoTime();
        Process p = pb.redirectErrorStream(true).start();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Thread drain = new Thread(() -> {
            try (InputStream in = p.getInputStream()) {
                in.transferTo(buf);
            } catch (IOException ignored) {
                // the child was destroyed under the reader; what arrived before that is the partial output
            }
        }, "python-oracle-drain");
        drain.setDaemon(true);
        drain.start();
        boolean exited = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        long elapsedMillis = (System.nanoTime() - t0) / 1_000_000L;
        if (!exited) {
            p.destroyForcibly();
            drain.join(TimeUnit.SECONDS.toMillis(5));
            throw new AssertionError("python timed out after " + elapsedMillis + " ms (budget " + timeoutSeconds + " s) for: " + label
                    + "\n--- partial output ---\n" + buf.toString(StandardCharsets.UTF_8));
        }
        // round 2 (cq SF-7): the drain's join is budgeted too - a child that exits leaving its stdout open (a grandchild
        // holding the handle) would otherwise hang the parent past the budget that guards waitFor alone
        drain.join(TimeUnit.SECONDS.toMillis(DRAIN_JOIN_SECONDS));
        if (drain.isAlive()) {
            throw new AssertionError("python exited after " + elapsedMillis + " ms but its output stream stayed open for "
                    + DRAIN_JOIN_SECONDS + " s more (a grandchild holding the handle?) for: " + label
                    + "\n--- partial output ---\n" + buf.toString(StandardCharsets.UTF_8));
        }
        if (elapsedMillis > TimeUnit.SECONDS.toMillis(SLOW_NOTE_SECONDS)) {
            System.err.println("[PythonOracle] SLOW " + elapsedMillis + " ms (budget " + timeoutSeconds + " s) for: " + label);
        }
        return new Result(p.exitValue(), buf.toString(StandardCharsets.UTF_8), elapsedMillis);
    }
}
