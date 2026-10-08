package com.regnosys.rosetta.ir.json;

/**
 * Thrown when JSON text or an IR JSON document is malformed or violates the v1 wire
 * contract (bad version, unknown kind/member, duplicate key, missing required member).
 * The message names the offending position or document path.
 */
public final class IRJsonException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IRJsonException(String message) {
        super(message);
    }
}
