package demo.harness.analytics;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal {@code --flag value} argument parser.
 *
 * <p>A flag may be repeated ({@code --src a --src b}); values accumulate. A single value may
 * also carry several entries separated by {@code ';'} ({@code --src "a;b;c"}), which is the
 * form the demo contract section 7A specifies. {@code ';'} is the ONLY separator honoured:
 * {@link java.io.File#pathSeparator} is {@code ':'} on POSIX, which would split a Windows
 * drive letter, and {@code ','} can legitimately occur inside a path.
 *
 * <p>Boolean flags are written {@code --flag} with no value (the next token starting with
 * {@code --} ends them) and read back with {@link #has(String)}.
 */
public final class Args {

    private final Map<String, List<String>> values = new LinkedHashMap<>();
    private List<String> raw = List.of();

    private Args() {
    }

    /**
     * The argv this instance was parsed from, verbatim and in order (including the verb and
     * anything before {@code from}). Used to reconstruct a reproducible command line for a
     * receipt when the wrapper did not supply one with {@code --command}.
     */
    public List<String> raw() {
        return raw;
    }

    /** Parses {@code argv} starting at {@code from} (typically 1, after the verb). */
    public static Args parse(String[] argv, int from) {
        Args args = new Args();
        args.raw = List.of(argv);
        for (int i = from; i < argv.length; i++) {
            String token = argv[i];
            if (!token.startsWith("--")) {
                throw new IllegalArgumentException(
                        "unexpected positional argument '" + token + "' (expected --flag)");
            }
            String name = token.substring(2);
            String inline = null;
            int eq = name.indexOf('=');
            if (eq >= 0) {
                inline = name.substring(eq + 1);
                name = name.substring(0, eq);
            }
            List<String> bucket = args.values.computeIfAbsent(name, k -> new ArrayList<>());
            if (inline != null) {
                bucket.add(inline);
            } else if (i + 1 < argv.length && !argv[i + 1].startsWith("--")) {
                bucket.add(argv[++i]);
            }
            // else: a valueless boolean flag; the empty bucket records its presence.
        }
        return args;
    }

    /** Whether the flag appeared at all (with or without a value). */
    public boolean has(String name) {
        return values.containsKey(name);
    }

    /** The flag's single value, or {@code fallback} when absent. */
    public String get(String name, String fallback) {
        List<String> bucket = values.get(name);
        if (bucket == null || bucket.isEmpty()) {
            return fallback;
        }
        return bucket.get(bucket.size() - 1);
    }

    /** The flag's single value; fails loudly when absent. */
    public String require(String name) {
        String value = get(name, null);
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("missing required argument --" + name);
        }
        return value;
    }

    /** Every value of the flag, with {@code ';'}-separated entries expanded, in order. */
    public List<String> all(String name) {
        List<String> out = new ArrayList<>();
        for (String raw : values.getOrDefault(name, List.of())) {
            for (String part : raw.split(";")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    out.add(trimmed);
                }
            }
        }
        return out;
    }

    /** {@link #all(String)} as paths; fails loudly when the flag contributed nothing. */
    public List<Path> requirePaths(String name) {
        List<Path> paths = paths(name);
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("missing required argument --" + name);
        }
        return paths;
    }

    /** {@link #all(String)} as paths (possibly empty). */
    public List<Path> paths(String name) {
        List<Path> out = new ArrayList<>();
        for (String s : all(name)) {
            out.add(Path.of(s));
        }
        return out;
    }

    /** The flag's single value as a path; fails loudly when absent. */
    public Path requirePath(String name) {
        return Path.of(require(name));
    }

    /** The flag's single value as a path, or {@code null} when absent. */
    public Path optionalPath(String name) {
        String value = get(name, null);
        return value == null || value.isEmpty() ? null : Path.of(value);
    }
}
