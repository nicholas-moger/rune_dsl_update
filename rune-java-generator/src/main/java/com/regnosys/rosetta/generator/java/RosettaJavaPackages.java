package com.regnosys.rosetta.generator.java;

import com.rosetta.util.DottedPath;

/**
 * Well-known Java package names used by the code generators.
 * The default namespace matches upstream's {@code RosettaScopeProvider.LIB_NAMESPACE}.
 */
public class RosettaJavaPackages {

    public static final DottedPath DEFAULT_NAMESPACE = DottedPath.splitOnDots("com.rosetta.model");

    public DottedPath defaultNamespace() {
        return DEFAULT_NAMESPACE;
    }

    public DottedPath defaultLib() {
        return defaultNamespace().child("lib");
    }

    public DottedPath defaultLibFunctions() {
        return defaultLib().child("functions");
    }
}
