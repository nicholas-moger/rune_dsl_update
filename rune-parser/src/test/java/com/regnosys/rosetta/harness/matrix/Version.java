package com.regnosys.rosetta.harness.matrix;

import java.util.Objects;

/**
 * Semantic version axis of the scope matrix.
 *
 * <p>Corpora use slightly different version conventions (CDM uses clean
 * {@code major.minor.patch}; DRR uses {@code major.minor.patch-asc.N} /
 * {@code -dev.N}; rune-fpml uses {@code major.minor.patch}). {@code qualifier}
 * captures any trailing {@code -asc.96} / {@code -dev.111} / {@code -SNAPSHOT}
 * suffix verbatim so no information is lost.
 *
 * <p>Part of P1.2 test-harness infrastructure (audit hook H13). Used in
 * {@link MatrixCoordinate}.
 *
 * @param major     major component, e.g. {@code 6} in {@code 6.16.0}
 * @param minor     minor component
 * @param patch     patch component
 * @param qualifier suffix after the patch version ({@code ""} if none), e.g.
 *                  {@code "asc.96"} in {@code 7.0.0-asc.96}. The leading {@code -}
 *                  is stripped; {@code ""} means no qualifier.
 */
public record Version(int major, int minor, int patch, String qualifier)
        implements Comparable<Version> {

    public Version {
        Objects.requireNonNull(qualifier, "qualifier must not be null (use \"\" for none)");
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException(
                    "version components must be non-negative: " + major + "." + minor + "." + patch);
        }
    }

    /**
     * Parse a version string of the form {@code major.minor.patch} with an
     * optional {@code -qualifier} suffix. Examples:
     * <ul>
     *   <li>{@code "6.16.0"}         → {@code Version(6, 16, 0, "")}</li>
     *   <li>{@code "7.0.0-asc.96"}   → {@code Version(7, 0, 0, "asc.96")}</li>
     *   <li>{@code "1.36.0"}         → {@code Version(1, 36, 0, "")}</li>
     * </ul>
     *
     * <p>Parses by splitting on literal {@code .} and {@code -} delimiters — no
     * regex, per project engineering standard.
     *
     * @throws IllegalArgumentException on malformed input
     */
    public static Version parse(String raw) {
        Objects.requireNonNull(raw, "raw must not be null");
        String core = raw;
        String qualifier = "";
        int dashIdx = raw.indexOf('-');
        if (dashIdx >= 0) {
            core = raw.substring(0, dashIdx);
            qualifier = raw.substring(dashIdx + 1);
            if (qualifier.isEmpty()) {
                // Reject trailing dash — would make display() non-invertible.
                throw new IllegalArgumentException("trailing '-' with empty qualifier: " + raw);
            }
        }
        int firstDot = core.indexOf('.');
        int secondDot = firstDot >= 0 ? core.indexOf('.', firstDot + 1) : -1;
        if (firstDot < 0 || secondDot < 0) {
            throw new IllegalArgumentException(
                    "version must be major.minor.patch[-qualifier]: " + raw);
        }
        try {
            int major = Integer.parseInt(core.substring(0, firstDot));
            int minor = Integer.parseInt(core.substring(firstDot + 1, secondDot));
            int patch = Integer.parseInt(core.substring(secondDot + 1));
            return new Version(major, minor, patch, qualifier);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("non-numeric component in version: " + raw, e);
        }
    }

    /**
     * Natural ordering: lexicographic on {@code (major, minor, patch, qualifier)}.
     * Qualifier ordering is plain {@link String#compareTo} — not semver pre-release
     * precedence. Sufficient for grouping-by-version in reports; the harness does
     * not rely on qualifier precedence for correctness.
     */
    @Override
    public int compareTo(Version o) {
        int c = Integer.compare(major, o.major);
        if (c != 0) return c;
        c = Integer.compare(minor, o.minor);
        if (c != 0) return c;
        c = Integer.compare(patch, o.patch);
        if (c != 0) return c;
        return qualifier.compareTo(o.qualifier);
    }

    /** Canonical string form — inverse of {@link #parse(String)}. */
    public String display() {
        return qualifier.isEmpty()
                ? major + "." + minor + "." + patch
                : major + "." + minor + "." + patch + "-" + qualifier;
    }
}
