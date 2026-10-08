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
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * THE v3.3 IR-SHARE DECLINE REGISTER (v3.3 seat 1, PR #637): the loader, the breakdown parser and the
 * equality verdict behind {@code D11CorpusRegressionTest}'s IR-share gate — kept corpus-free so
 * {@link D11IrDeclineRegisterTest} can prove every refusal and both verdict directions without a cell.
 *
 * <p>The resource {@value #RESOURCE} ({@code rune-java-generator/src/test/resources/}) carries one row per
 * measured IR decline of the ON route — {@code <corpus>/<version> <SEAM> <axis> <token> <count>} — and the
 * D11 asserts, per {@code (cell, seam, axis)}, that the declared map EQUALS the measured breakdown the
 * compiler prints ({@code IRExpressionCompiler.rankedBreakdown}: {@code key=count} tokens ranked
 * count-descending, or the literal {@code none}). Modelled on the chaos loader's five properties: a
 * classpath resource that is an {@code AssertionError} when absent (never a silent empty map), {@code #}
 * comments and blank lines skipped, a fail-loud parser naming {@code resource:line}, a duplicate row
 * refused, the map frozen before publication. The key is the SEAM ({@code FUNCTION} / {@code RULE} /
 * {@code DATA_RULE}), never an {@code ElementKind} — the RULE seam is measured inside {@code pojo_comparison}
 * and no {@code ElementKind} names it (the groundwork's finding; the {@code POJO} refusal in the unit test is
 * the lock). Since v3.3 seat 3 (D54) the third seam is {@code DATA_RULE} — the condition bodies through the IR
 * compiler, measured inside {@code datarule_comparison} — and the WORD names both that seam and the file kind
 * of the same name the byte gate compares; the distinction stands (a seam is where the IR compiler's counters
 * are read, a kind is a family of generated files), it is simply the one seam whose name a kind shares, so
 * nobody "fixes" it either way.
 *
 * <p>Two verdicts, by direction: a measured token absent from the declared map, or measured above declared,
 * is {@code NEW DECLINE} — a regression, never absorbed by appending the row; a declared token absent from
 * the measured map, or declared above measured, is {@code HEALED} — the row is deleted or shrunk in the
 * healing seat's own commit (the set is SHRINK-ONLY). The comparison is over maps, so it is order-insensitive
 * (a count change reorders {@code rankedBreakdown}'s tokens) and {@code assertEquals} prints both sides.
 */
final class IrDeclineRegister {

    static final String RESOURCE = "/d11-ir-declines.txt";
    static final Set<String> SEAMS = Set.of("FUNCTION", "RULE", "DATA_RULE");
    static final Set<String> AXES = Set.of("site", "family", "untargeted");
    /** The five {@code recordDecline} tags — a CLOSED set; a sixth means the compiler grew a decline seat. */
    static final Set<String> SITES = Set.of("adapterGap", "postPinGuard", "ruleDelegation", "aliasResolution", "leafEmitter");

    private IrDeclineRegister() {
    }

    /** The register key of one axis of one seam of one cell: {@code "<cell> <SEAM> <axis>"}. */
    static String key(String cell, String seam, String axis) {
        return cell + " " + seam + " " + axis;
    }

    /** The committed resource ({@value #RESOURCE}), loaded and frozen; an {@code AssertionError} when it is absent. */
    static Map<String, Map<String, Integer>> load() {
        return load(RESOURCE);
    }

    /** A register resource by classpath name, loaded and frozen; an {@code AssertionError} when the resource is absent. */
    static Map<String, Map<String, Integer>> load(String resource) {
        try (InputStream stream = IrDeclineRegister.class.getResourceAsStream(resource)) {
            if (stream == null) {
                throw new AssertionError("D11 IR-share decline register not found: " + resource
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
     * A count token: decimal digits only (no sign, no space), non-negative, and EXACT - a leading zero on a
     * multi-digit count is refused, because the row grammar says "the measured count, exact" and {@code 0140}
     * is a second spelling of one value (PR #637 round 1, cq NIT-3). The one parser both grammars use.
     *
     * <p>The empty guard is unreachable from either caller - {@link #parse} splits a TRIMMED row on
     * {@code \s+} so no field is empty, and {@link #parseBreakdown} refuses a token whose {@code =} is its
     * last character before calling here - and it is kept, not deleted, because without it an empty token
     * would fall through the digit loop into {@code Integer.parseInt("")} and be reported as an OVERFLOW,
     * which is the wrong cause. {@link D11IrDeclineRegisterTest} reaches it by a direct call (PR #637 round 2,
     * cq SF-3).
     */
    static int digitsInt(String text, String what) {
        if (text.isEmpty()) {
            throw new AssertionError(what + " - the count is empty");
        }
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < '0' || ch > '9') {
                throw new AssertionError(what + " - the count must be decimal digits (no sign, no space): '" + text + "'");
            }
        }
        if (text.length() > 1 && text.charAt(0) == '0') {
            throw new AssertionError(what + " - the count is exact: a leading zero on a multi-digit count is refused"
                    + " (two spellings of one value is a drift surface): '" + text + "'");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new AssertionError(what + " - the count overflows an int: '" + text + "'");
        }
    }

    /**
     * Parses the register: {@code key -> token -> count}, every refusal naming {@code name:line} and the
     * offending text. The map and its values are frozen ({@code Map.copyOf}).
     */
    static Map<String, Map<String, Integer>> parse(BufferedReader reader, String name) throws IOException {
        Map<String, Map<String, Integer>> raw = new LinkedHashMap<>();
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
            if (f.length != 5) {
                throw new AssertionError(where + " - malformed row (expected five fields '<corpus>/<version> <SEAM> <axis> <token> <count>', got "
                        + f.length + "): " + trimmed);
            }
            String[] cellParts = f[0].split("/", -1);
            if (cellParts.length != 2 || cellParts[0].isEmpty() || cellParts[1].isEmpty()) {
                throw new AssertionError(where + " - the cell must be '<corpus>/<version>' with both halves non-empty: " + f[0]);
            }
            if (!SEAMS.contains(f[1])) {
                throw new AssertionError(where + " - the seam must be one of " + new TreeSet<>(SEAMS)
                        + " (the three IR seams - NOT an ElementKind, though DATA_RULE names both the seam measured inside"
                        + " datarule_comparison and the file kind; the RULE seam is measured inside pojo_comparison): " + f[1]);
            }
            if (!AXES.contains(f[2])) {
                throw new AssertionError(where + " - the axis must be one of " + new TreeSet<>(AXES) + ": " + f[2]);
            }
            if ("site".equals(f[2]) && !SITES.contains(f[3])) {
                throw new AssertionError(where + " - a site token must be one of the five recordDecline tags " + new TreeSet<>(SITES)
                        + " (a sixth tag means IRExpressionCompiler grew a decline seat - teach this register deliberately): " + f[3]);
            }
            int count = digitsInt(f[4], where);
            if (count == 0) {
                throw new AssertionError(where + " - the count must be positive (a zero row is the ABSENCE of a row - two spellings of one state is a drift surface): " + trimmed);
            }
            Map<String, Integer> axisMap = raw.computeIfAbsent(key(f[0], f[1], f[2]), k -> new LinkedHashMap<>());
            if (axisMap.putIfAbsent(f[3], count) != null) {
                throw new AssertionError(where + " - duplicate row (the same cell, seam, axis and token declared twice): " + trimmed);
            }
        }
        Map<String, Map<String, Integer>> frozen = new LinkedHashMap<>();
        raw.forEach((k, v) -> frozen.put(k, Map.copyOf(v)));
        return Map.copyOf(frozen);
    }

    /**
     * Parses one {@code rankedBreakdown} print — {@code key=count} tokens in any order, or the literal
     * {@code none} (the empty map). Refuses a token that is not {@code key=int}.
     */
    static Map<String, Integer> parseBreakdown(String text) {
        Map<String, Integer> out = new LinkedHashMap<>();
        String t = text == null ? "" : text.trim();
        if (t.isEmpty() || "none".equals(t)) {
            return out;
        }
        for (String tok : t.split("\\s+")) {
            int eq = tok.indexOf('=');
            if (eq < 1 || eq == tok.length() - 1) {
                throw new AssertionError("D11 IR-share gate: a breakdown token is not key=count: '" + tok + "' in: " + text);
            }
            String k = tok.substring(0, eq);
            int c = digitsInt(tok.substring(eq + 1), "D11 IR-share gate: breakdown token '" + tok + "' in: " + text);
            if (out.putIfAbsent(k, c) != null) {
                throw new AssertionError("D11 IR-share gate: a breakdown token repeats: '" + k + "' in: " + text);
            }
        }
        return out;
    }

    /**
     * The verdict for one axis: {@code null} when the declared map equals the measured one; otherwise the
     * message — {@code NEW DECLINE} when any measured token is absent from, or above, the declared map (that
     * direction wins the headline when both occur), else {@code HEALED} — naming every differing token.
     */
    static String verdict(String cell, String seam, String axis, Map<String, Integer> declaredIn, Map<String, Integer> measuredIn) {
        // a zero-valued token is the absence of a token on either side (a breakdown never prints one; the register refuses one)
        Map<String, Integer> declared = new LinkedHashMap<>();
        declaredIn.forEach((k, v) -> { if (v != 0) { declared.put(k, v); } });
        Map<String, Integer> measured = new LinkedHashMap<>();
        measuredIn.forEach((k, v) -> { if (v != 0) { measured.put(k, v); } });
        if (declared.equals(measured)) {
            return null;
        }
        Set<String> tokens = new TreeSet<>(declared.keySet());
        tokens.addAll(measured.keySet());
        StringBuilder news = new StringBuilder();
        StringBuilder heals = new StringBuilder();
        for (String tok : tokens) {
            Integer d = declared.get(tok);
            Integer m = measured.get(tok);
            int dv = d == null ? 0 : d;
            int mv = m == null ? 0 : m;
            if (mv > dv) {
                news.append(news.length() == 0 ? "" : "; ").append("measured ").append(tok).append("=").append(mv)
                        .append(", declared ").append(d == null ? "absent" : d.toString());
            } else if (dv > mv) {
                heals.append(heals.length() == 0 ? "" : "; ").append("declared ").append(tok).append("=").append(dv)
                        .append(", measured ").append(m == null ? "absent" : m.toString());
            }
        }
        String where = cell + " " + seam + " " + axis;
        String both = " (declared " + new TreeMap<>(declared) + "; measured " + new TreeMap<>(measured) + ")";
        if (news.length() > 0) {
            return "D11 IR-share gate: NEW DECLINE - a regression. " + where + ": " + news
                    + ". The IR route lost coverage it had at the last merge. Do not append the row to d11-ir-declines.txt to make this"
                    + " green - find the adapter / emitter arm that stopped firing."
                    + (heals.length() > 0 ? " Also healed on the same axis: " + heals + "." : "") + both;
        }
        return "D11 IR-share gate: HEALED - delete or shrink the row. " + where + ": " + heals
                + ". The set is SHRINK-ONLY: delete the row (or lower the count) in the healing seat's own commit, in the same commit as the heal."
                + both;
    }

    /** The {@code "<cell> <SEAM>"} keys the register declares (any axis) — the wholeness check's population. */
    static Set<String> declaredKeys(Map<String, Map<String, Integer>> declared) {
        Set<String> out = new TreeSet<>();
        for (String k : declared.keySet()) {
            int last = k.lastIndexOf(' ');
            out.add(k.substring(0, last));
        }
        return out;
    }

    /**
     * The print's own CONSERVATIONS for one seam of one cell, decided corpus-free: the site breakdown and the
     * family breakdown each sum to {@code irDeclined} - the {@code recordDecline} single-seat law, one decline
     * recorded at exactly one site and attributed to exactly one family - and the untargeted breakdown sums to
     * its own printed total. Returns {@code null} when all three hold; otherwise ONE message naming every
     * relation that failed, with both sides and the offending breakdown.
     *
     * <p>Checked BEFORE the register comparison, and for a reason: a breakdown that does not sum to its own
     * total is a broken PRINT, and comparing it against the register would report the compiler's own counting
     * bug as a coverage move. Lifted out of {@code D11CorpusRegressionTest}'s three inline asserts so it has
     * cases of its own (PR #637 round 2, spec SF-5: "the two conservation asserts have no case and no lane").
     */
    static String conservation(String cell, String seam, int declined, Map<String, Integer> sites,
                               Map<String, Integer> families, int untargetedTotal, Map<String, Integer> untargeted) {
        StringBuilder bad = new StringBuilder();
        appendShortfall(bad, "declined by site", declined, sites, "the recordDecline single-seat law");
        appendShortfall(bad, "declined by family", declined, families, "the recordDecline single-seat law");
        appendShortfall(bad, "untargeted visits", untargetedTotal, untargeted, "the print's own total");
        if (bad.length() == 0) {
            return null;
        }
        return "D11 IR-share gate: the print does not CONSERVE. " + cell + " " + seam + ": " + bad
                + ". A breakdown that does not sum to its own printed total is a broken print - fix the emitter's"
                + " counters before reading the register, which cannot tell a counting bug from a coverage move.";
    }

    private static void appendShortfall(StringBuilder bad, String what, int printed, Map<String, Integer> m, String law) {
        int summed = sum(m);
        if (printed == summed) {
            return;
        }
        bad.append(bad.length() == 0 ? "" : "; ").append(what).append(": the breakdown sums to ").append(summed)
                .append(" but the print declares ").append(printed).append(" (").append(law).append("; breakdown ")
                .append(new TreeMap<>(m)).append(")");
    }

    /** The sum of a breakdown's counts. */
    static int sum(Map<String, Integer> m) {
        int s = 0;
        for (int v : m.values()) {
            s += v;
        }
        return s;
    }
}
