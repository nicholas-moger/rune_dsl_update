package com.regnosys.rosetta.generator.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * THE IR FALLBACK REGISTER (v3.3 seat 5, PR #641 - decision D55, ruling R4): the committed, SHRINK-ONLY list of every
 * enum, choice and data-type file the OLD generator still writes on the reference IR route, per cell. The file meter
 * (the development IR page, the strict rule) counts a sub-kind as made from the IR only when a NEW IR emitter writes EVERY
 * file of it on every vendored cell - that is, only when this register holds NO row of that sub-kind. Until then the
 * old generator writes the file (its bytes unmoved) and the file is LISTED here.
 *
 * <p>The ON-route D11 asserts, per (cell, sub-kind), the set of files the old generator wrote EQUAL to the declared
 * set: an undeclared file is {@code NEW FALLBACK - a regression}, a declared file the old generator no longer writes
 * is {@code HEALED - delete the row} (in the healing commit). Equality is what makes the file the meter's ledger and
 * not a waiver. The loader and the arbiter are corpus-free, so {@link D11IrFallbackRegisterTest} proves both able to
 * fail without a corpus on disk.
 */
final class IrFallbackRegister {

    static final String RESOURCE = "/d11-ir-fallbacks.txt";
    /**
     * The three sub-kinds the declaration emitters retire - the enums LANDED at PR #642, then the data types, then the choices
     * (the maintainer's amended order of 2026-09-18); the set itself is unordered.
     */
    static final Set<String> SUB_KINDS = Set.of("ENUM", "CHOICE", "DATA_TYPE");

    private IrFallbackRegister() {
    }

    /** The register key of one sub-kind of one cell: {@code "<cell> <SUB_KIND>"}. */
    static String key(String cell, String subKind) {
        return cell + " " + subKind;
    }

    /** The committed resource, loaded and frozen; an {@code AssertionError} when it is absent. */
    static Map<String, Set<String>> load() {
        return load(RESOURCE);
    }

    static Map<String, Set<String>> load(String resource) {
        try (InputStream stream = IrFallbackRegister.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("D11 IR fallback register not found: " + resource
                        + " - expected at rune-java-generator/src/test/resources/ (an absent register is never an empty one)");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                return parse(reader, resource);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("failed to read " + resource, e);
        }
    }

    /**
     * Parses the register: {@code "<cell> <SUB_KIND>" -> the declared files}. Every refusal names {@code name:line}
     * and the offending text; rows of one key must be in strictly ascending path order (one spelling of one list, and a
     * duplicate is caught by the same comparison).
     *
     * <p>THE CROSS-PASS HALF OF "A FILE HAS ONE WRITER" (v3.3 seat 6, PR #642 - round 1 cq NIT-3): the ascending law
     * above is per key, so it cannot see one file of one cell listed under TWO sub-kinds - and that register would be
     * incoherent, because the arbiter judges each sub-kind against the ONE pass that writes it. A file listed twice
     * would be demanded of the data-type pass AND of the choice pass, so one of the two must read NEW FALLBACK or
     * HEALED for a reason that is the register's, not the route's. It is refused here, naming both keys, on the same
     * law {@code D11CorpusRegressionTest#collectAdded} enforces on the measurement side.
     */
    static Map<String, Set<String>> parse(BufferedReader reader, String name) throws IOException {
        Map<String, TreeSet<String>> raw = new LinkedHashMap<>();
        Map<String, String> last = new LinkedHashMap<>();
        // "<cell> <file>" -> the key that already claims it; neither half of a legal row holds whitespace,
        // so one space is an unambiguous join
        Map<String, String> writerOf = new LinkedHashMap<>();
        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String where = name + ":" + lineNum;
            String[] f = trimmed.split("\\s+");
            if (f.length != 3) {
                throw new AssertionError(where + " - malformed row (expected three fields '<corpus>/<version> <SUB_KIND> <file>', got "
                        + f.length + "): " + trimmed);
            }
            String[] cellParts = f[0].split("/", -1);
            if (cellParts.length != 2 || cellParts[0].isEmpty() || cellParts[1].isEmpty()) {
                throw new AssertionError(where + " - the cell must be '<corpus>/<version>' with both halves non-empty: " + f[0]);
            }
            if (!SUB_KINDS.contains(f[1])) {
                throw new AssertionError(where + " - the sub-kind must be one of " + new TreeSet<>(SUB_KINDS) + ": " + f[1]);
            }
            if (!f[2].endsWith(".java") || f[2].indexOf('\\') >= 0 || f[2].startsWith("/")) {
                throw new AssertionError(where + " - the file must be a relative, forward-slashed .java path: " + f[2]);
            }
            String key = key(f[0], f[1]);
            String previous = last.put(key, f[2]);
            if (previous != null && previous.compareTo(f[2]) >= 0) {
                throw new AssertionError(where + " - the rows of " + key + " must be in strictly ascending order (a duplicate or"
                        + " an unsorted row): '" + f[2] + "' after '" + previous + "'");
            }
            String claimed = writerOf.put(f[0] + " " + f[2], key);
            if (claimed != null && !claimed.equals(key)) {
                throw new AssertionError(where + " - the file is listed under TWO sub-kinds of one cell (a file has ONE"
                        + " writer): '" + f[2] + "' is declared by '" + claimed + "' and by '" + key + "'");
            }
            raw.computeIfAbsent(key, k -> new TreeSet<>()).add(f[2]);
        }
        Map<String, Set<String>> frozen = new LinkedHashMap<>();
        raw.forEach((k, v) -> frozen.put(k, java.util.Collections.unmodifiableSet(v)));
        return java.util.Collections.unmodifiableMap(frozen);
    }

    /**
     * The arbiter, corpus-free. {@code null} when the measurement agrees with the declaration; otherwise the verdict:
     * the files split between the two writers must conserve to the emitted set, no file may be written by both, a file
     * the NEW emitter writes may not be declared, and the old generator's files must EQUAL the declared set.
     */
    static String verdict(String cell, String subKind, Set<String> declared, Set<String> emitted, Set<String> byNewEmitter) {
        String head = "D11 " + cell + " " + subKind + " IR fallback gate: ";
        TreeSet<String> stray = new TreeSet<>(byNewEmitter);
        stray.removeAll(emitted);
        if (!stray.isEmpty()) {
            return head + "BROKEN PRINT - the new IR emitter claims " + stray.size() + " file(s) this pass did not emit: " + sample(stray);
        }
        TreeSet<String> byOldGenerator = new TreeSet<>(emitted);
        byOldGenerator.removeAll(byNewEmitter);
        TreeSet<String> listedButNative = new TreeSet<>(declared);
        listedButNative.retainAll(byNewEmitter);
        if (!listedButNative.isEmpty()) {
            return head + "HEALED - " + listedButNative.size() + " declared file(s) are written by the NEW IR emitter now; delete"
                    + " their rows in the healing commit (the register is SHRINK-ONLY): " + sample(listedButNative);
        }
        TreeSet<String> undeclared = new TreeSet<>(byOldGenerator);
        undeclared.removeAll(declared);
        if (!undeclared.isEmpty()) {
            return head + "NEW FALLBACK - a regression: the old generator writes " + undeclared.size()
                    + " file(s) the register does not declare: " + sample(undeclared);
        }
        TreeSet<String> gone = new TreeSet<>(declared);
        gone.removeAll(byOldGenerator);
        if (!gone.isEmpty()) {
            return head + "HEALED - " + gone.size() + " declared file(s) the old generator no longer writes; delete their rows"
                    + " in the healing commit (the register is SHRINK-ONLY): " + sample(gone);
        }
        return null;
    }

    private static String sample(TreeSet<String> files) {
        return files.stream().limit(5).toList() + (files.size() > 5 ? " (+" + (files.size() - 5) + " more)" : "");
    }

    /** The rows a sub-kind still holds over EVERY cell - zero is what lets the file meter count it. */
    static int rowsOf(Map<String, Set<String>> register, String subKind) {
        int rows = 0;
        for (Map.Entry<String, Set<String>> e : register.entrySet()) {
            if (e.getKey().endsWith(" " + subKind)) {
                rows += e.getValue().size();
            }
        }
        return rows;
    }
}
