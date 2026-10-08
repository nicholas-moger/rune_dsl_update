package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for {@code package-info.java} generation.
 * The simplest model — just a package name and description strings.
 *
 * <p>Rendered by {@code java-package-info.stg}. Each description produces
 * a Javadoc paragraph block with the upstream format:
 * <pre>
 * *[tab]
 * *[tab]Description text
 * *[tab]&lt;p&gt;
 * </pre>
 */
public class PackageInfoModel {

    private final String packageName;
    private final List<String> descriptions;

    public PackageInfoModel(String packageName, List<String> descriptions) {
        this.packageName = packageName;
        this.descriptions = List.copyOf(descriptions);
    }

    public String getPackageName() { return packageName; }
    public List<String> getDescriptions() { return descriptions; }
}
