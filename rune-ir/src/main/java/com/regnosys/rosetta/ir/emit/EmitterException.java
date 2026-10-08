package com.regnosys.rosetta.ir.emit;

/**
 * Signals that an emitter cannot lower a given IR construct — an unsupported {@code IRExprKind},
 * or a within-arm shape outside the backend's slice (e.g. a list-op flavour it does not yet handle).
 *
 * <p>Unchecked: a decline is a coverage/programming fact for a backend author to resolve, not a
 * recoverable runtime condition. This is the SPI's single, self-describing decline signal — distinct
 * from the JDK's {@link UnsupportedOperationException} so a consumer can catch an emitter decline
 * specifically.
 */
public class EmitterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message a human-readable description naming the emitter and the unsupported construct */
    public EmitterException(String message) {
        super(message);
    }
}
