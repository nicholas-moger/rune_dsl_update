package com.regnosys.rosetta.testutil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * THE ONE READ of the chaos expected-divergence baseline's ROWS for the generator module's seat suites (v3.2 seat 10,
 * round 1 — PR #631): {@code chaos-expected-divergence.txt}, the D11's declared-red set (charter § 5), keyed
 * {@code chaos/<version>/<KIND>:<golden-relative path>}. {@code D11CorpusRegressionTest} keeps its own loader (the
 * chaos-only / shrink-only / kind-keyed laws are its contract); this helper serves the seat suites that need the SET
 * of declared paths — the gen-1 carrier locks (LAW 81: a carrier the baseline declares divergent must still diverge;
 * a byte-identical declared carrier is a HEALED row to delete, never a silent pass) and the one-read cell test.
 */
public final class ChaosDeclaredRows {

    private static final String RESOURCE = "/chaos-expected-divergence.txt";

    private ChaosDeclaredRows() {
        // utility class — no instances
    }

    /** Every non-comment row of the committed baseline, trimmed, in file order. */
    public static List<String> rows() {
        List<String> out = new ArrayList<>();
        try (InputStream in = ChaosDeclaredRows.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("the chaos baseline " + RESOURCE + " is not on the test classpath");
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String raw;
            while ((raw = r.readLine()) != null) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                out.add(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read " + RESOURCE, e);
        }
        return Collections.unmodifiableList(out);
    }

    /**
     * The golden-relative PATHS the baseline declares (the part after the first {@code :}); a malformed row refuses.
     * Deliberately KIND-BLIND (round 2, spec NIT-2): a carrier lock asks whether its golden PATH is declared divergent at all,
     * and a path is unique across kinds in the baseline (one golden file, one kind); the LAW 73 set pin at every caller
     * ({@code assertEquals(expectedDeclared, toleratedDeclared)}) refuses any tolerance the caller did not name, so a
     * second kind declaring the same path could never widen a lock silently.
     */
    public static Set<String> declaredPaths() {
        Set<String> out = new TreeSet<>();
        for (String row : rows()) {
            int colon = row.indexOf(':');
            if (colon <= 0 || colon >= row.length() - 1) {
                throw new IllegalStateException("malformed baseline row (expected '<key>:<path>'): " + row);
            }
            out.add(row.substring(colon + 1));
        }
        return Collections.unmodifiableSet(out);
    }
}
