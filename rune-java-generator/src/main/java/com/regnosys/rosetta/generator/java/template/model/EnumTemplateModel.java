package com.regnosys.rosetta.generator.java.template.model;

import java.util.List;

/**
 * Template model for Java enum generation from a Rosetta enumeration.
 *
 * <p>Covers all corpus variations across CDM (271 enums), DRR (102),
 * ISO-20022 (453), and rune-fpml (155). Fields are pre-computed in the
 * generator's {@code buildModel()} method — templates contain zero logic.
 *
 * <p>Rendered by {@code java-enum.stg}. The template produces byte-identical
 * output to the upstream Xtend EnumGenerator (D11 contract).
 */
public class EnumTemplateModel {

    private final String packageName;
    private final String enumName;
    private final String version;
    private final String javadoc;
    private final List<String> imports;
    private final List<EnumValueModel> values;
    private final boolean hasSynonyms;

    public EnumTemplateModel(String packageName, String enumName, String version,
                              String javadoc, List<String> imports,
                              List<EnumValueModel> values, boolean hasSynonyms) {
        this.packageName = packageName;
        this.enumName = enumName;
        this.version = version;
        this.javadoc = javadoc;
        this.imports = List.copyOf(imports);
        this.values = List.copyOf(values);
        this.hasSynonyms = hasSynonyms;
    }

    public String getPackageName() { return packageName; }
    public String getEnumName() { return enumName; }
    public String getVersion() { return version; }
    public String getJavadoc() { return javadoc; }
    public List<String> getImports() { return imports; }
    public List<EnumValueModel> getValues() { return values; }
    public boolean getHasSynonyms() { return hasSynonyms; }
}
