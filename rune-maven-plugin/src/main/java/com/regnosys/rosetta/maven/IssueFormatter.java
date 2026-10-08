package com.regnosys.rosetta.maven;

import com.regnosys.rosetta.symbols.diagnostics.RDiagnostic;

/**
 * Formats one fork diagnostic as the released 9.83.0 plugin's issue line — the
 * payload the Xtext standalone builder logs per issue, i.e.
 * {@code Issue.IssueImpl#toString()}:
 *
 * <pre>SEVERITY:message (file:/C:/path/to/file.rosetta line : 7 column : 6)</pre>
 *
 * <p>Reference bytes: the banked V0 oracle streams
 * ({@code target/439-v0-oracle/v0-oracle-cdm6-warnings.txt} +
 * {@code target/440-v0-oracle-drr/v0-oracle-drr-warnings.txt}) — every line is
 * {@code [WARNING] } (the Maven logger prefix, supplied by the mojo log level,
 * not by this formatter) followed by exactly this payload.
 *
 * <p>The URI form is EMF's {@code URI.createFileURI(absolutePath)}: scheme +
 * single slash + the absolute path with forward slashes
 * ({@code file:/F:/...} on Windows, {@code file:/home/...} would render as
 * {@code file:/home/...}). The fork stores {@link RDiagnostic#range()} file
 * strings as absolute forward-slashed paths (the runner normalizes at parse
 * time), so the formatter only prefixes the scheme.
 */
public final class IssueFormatter {

    private IssueFormatter() {
    }

    /** The upstream payload for one diagnostic; the caller picks the log level. */
    public static String format(RDiagnostic d) {
        return d.severity().name() + ":" + d.message()
                + " (" + fileUri(d.range().file())
                + " line : " + d.range().startLine()
                + " column : " + d.range().startCol() + ")";
    }

    /**
     * EMF {@code createFileURI} rendering of an absolute path: forward slashes,
     * {@code file:/} prefix (single slash — the drive letter or root follows
     * immediately, matching every banked V0 line).
     */
    public static String fileUri(String file) {
        String forward = file.replace('\\', '/');
        return forward.startsWith("/") ? "file:" + forward : "file:/" + forward;
    }
}
