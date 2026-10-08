package com.regnosys.rosetta.harness.cache;

import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Test-only stub {@link ReflectiveInvocationContext} that exposes a caller-
 * supplied argument list. Used to drive the package-visible seams of
 * {@link CachedMatrixExtension} without standing up the full JUnit
 * lifecycle. Only the methods actually used by the extension are
 * implemented; others delegate to sensible stubs.
 */
final class FakeReflectiveContext implements ReflectiveInvocationContext<Method> {

    private final List<Object> arguments;
    private final Class<?> targetClass;

    private FakeReflectiveContext(List<Object> arguments, Class<?> targetClass) {
        this.arguments = arguments;
        this.targetClass = targetClass;
    }

    static FakeReflectiveContext withArgs(List<Object> arguments) {
        return new FakeReflectiveContext(arguments, FakeReflectiveContext.class);
    }

    static FakeReflectiveContext withArgsAndTargetClass(List<Object> arguments, Class<?> targetClass) {
        return new FakeReflectiveContext(arguments, targetClass);
    }

    @Override
    public Class<?> getTargetClass() {
        return targetClass;
    }

    @Override
    public Method getExecutable() {
        throw new UnsupportedOperationException("FakeReflectiveContext.getExecutable not stubbed");
    }

    @Override
    public List<Object> getArguments() {
        return arguments;
    }

    @Override
    public Optional<Object> getTarget() {
        return Optional.empty();
    }
}
