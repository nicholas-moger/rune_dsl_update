package com.regnosys.rosetta.generator.java.scoping;

import java.util.Objects;

import javax.lang.model.SourceVersion;

import com.rosetta.util.DottedPath;

/**
 * Java package name with keyword escaping. Ensures each segment of the
 * package path is a valid Java identifier.
 */
public class JavaPackageName {
    private final DottedPath packageName;

    private JavaPackageName(DottedPath packageName) {
        this.packageName = packageName;
    }

    public static JavaPackageName escape(DottedPath unescapedPackageName) {
        String[] escapedNames = unescapedPackageName.stream()
                .map(JavaPackageName::escape)
                .toArray(String[]::new);
        return new JavaPackageName(DottedPath.of(escapedNames));
    }

    public static JavaPackageName splitOnDotsAndEscape(String unescapedPackageName) {
        return escape(DottedPath.splitOnDots(unescapedPackageName));
    }

    private static String escape(String name) {
        if (!SourceVersion.isIdentifier(name)) {
            throw new IllegalStateException(
                    "`" + name + "` cannot be escaped to a valid Java identifier");
        }
        while (!SourceVersion.isName(name)) {
            name = "_" + name;
        }
        return name;
    }

    public DottedPath getName() {
        return packageName;
    }

    @Override
    public String toString() {
        return packageName.withDots();
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        JavaPackageName other = (JavaPackageName) obj;
        return Objects.equals(packageName, other.packageName);
    }
}
