package org.finos.rune.equivalence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * v3.2 seat 6 (PR #627): the corpus-free witness of the pair gate's row-file reader law
 * ({@link OptimisedNavigationPairGateTest#readRowFile}). The optimised-route register reached
 * ZERO rows at the F5 fix — every declared row was the optimised twin of a declared default-route
 * D11 row and fell with it — before the v3.2 close deletes the declared sets together, so the
 * reader admits an EMPTY BODY for the caller that declares it (the register alone) and keeps
 * refusing it for every other declared set; absence and a duplicate row fail loud on BOTH paths.
 * Each case is a pair: the admitted path and the strict path over the SAME file, so the exception
 * is proven to reach exactly the one clause it names.
 */
class DeclaredRowFileReaderUnitTest {

    @TempDir
    Path dir;

    private static final String COMMENTS_ONLY = "# a register whose rows have all healed\n#\n# SHRINK LOG:\n#  - 11 -> 0\n\n";

    /** The empty body: the admitted path reads the empty set, the strict path fails loud — the SAME file (round 1's NIT-2). */
    @Test
    void anEmptyBodyReadsAsTheEmptySetWhereAdmittedAndFailsLoudOnTheStrictPath() throws IOException {
        Path file = Files.writeString(dir.resolve("register.txt"), COMMENTS_ONLY);
        assertEquals(Set.of(), OptimisedNavigationPairGateTest.readRowFile(file, true));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> OptimisedNavigationPairGateTest.readRowFile(file, false));
        assertTrue(e.getMessage().contains("carries no rows"), e.getMessage());
    }

    @Test
    void rowsReadIdenticallyOnBothPaths() throws IOException {
        Path file = Files.writeString(dir.resolve("rows.txt"),
                "# header\nchaos/s03/base/functions/C3Commons.java\n\n  chaos/s03/a1o1/functions/C3Commons.java  \n");
        Set<String> expected = Set.of("chaos/s03/base/functions/C3Commons.java",
                "chaos/s03/a1o1/functions/C3Commons.java");
        assertEquals(expected, OptimisedNavigationPairGateTest.readRowFile(file, true));
        assertEquals(expected, OptimisedNavigationPairGateTest.readRowFile(file, false));
    }

    @Test
    void aDuplicateRowFailsLoudOnBothPaths() throws IOException {
        Path file = Files.writeString(dir.resolve("dup.txt"),
                "chaos/s03/base/functions/C3Commons.java\nchaos/s03/base/functions/C3Commons.java\n");
        for (boolean admitted : new boolean[] {true, false}) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> OptimisedNavigationPairGateTest.readRowFile(file, admitted),
                    "emptyBodyAdmitted=" + admitted);
            assertTrue(e.getMessage().contains("duplicate declared row"), e.getMessage());
        }
    }

    @Test
    void anAbsentFileFailsLoudOnBothPaths() {
        Path file = dir.resolve("absent.txt");
        for (boolean admitted : new boolean[] {true, false}) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> OptimisedNavigationPairGateTest.readRowFile(file, admitted),
                    "emptyBodyAdmitted=" + admitted);
            assertTrue(e.getMessage().contains("committed declared-set file missing"), e.getMessage());
        }
    }
}
