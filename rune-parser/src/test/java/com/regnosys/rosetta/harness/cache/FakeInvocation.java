package com.regnosys.rosetta.harness.cache;

import org.junit.jupiter.api.extension.InvocationInterceptor;

/**
 * Test-only fake {@link InvocationInterceptor.Invocation} that records
 * whether it was proceeded-with or skipped. Enforces the real
 * {@code Invocation<Void>} contract: exactly one of {@link #proceed()}
 * or {@link #skip()} is called, and only once. A second call throws
 * {@link IllegalStateException}, so interceptor bugs that accidentally
 * re-dispatch are caught immediately instead of silently passing.
 */
final class FakeInvocation implements InvocationInterceptor.Invocation<Void> {

    interface Body {
        void run() throws Throwable;
    }

    private final Body body;
    boolean proceeded = false;
    boolean skipped = false;

    FakeInvocation(Body body) {
        this.body = body;
    }

    @Override
    public Void proceed() throws Throwable {
        if (proceeded) throw new IllegalStateException("proceed() called twice");
        if (skipped) throw new IllegalStateException("proceed() called after skip()");
        proceeded = true;
        body.run();
        return null;
    }

    @Override
    public void skip() {
        if (skipped) throw new IllegalStateException("skip() called twice");
        if (proceeded) throw new IllegalStateException("skip() called after proceed()");
        skipped = true;
    }
}
