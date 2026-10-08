package com.regnosys.rosetta.maven;

/**
 * XML-mappable stand-in for the Xtext {@code OutputConfiguration} bean the
 * released 9.83.0 rosetta-maven-plugin accepts inside
 * {@code languages/language/outputConfigurations}. The fork plugin declares its
 * own bean with the same configurable element names, so a consumer pom's
 * existing {@code <outputConfiguration>} block maps unchanged.
 *
 * <p>Only the elements the released plugin's real consumers configure are
 * declared ({@code name} + {@code outputDirectory} — measured across the CDM
 * 6.20.6 and DRR 6.34.1 rosetta-source poms, all executions incl. the
 * per-language profiles; see
 * the development audit "2026-07-19-leg-s-swap-recon-ledger" § Leg P). An
 * unrecognized element fails Maven configuration loudly, which is the honest
 * surface for knobs the fork pipeline does not model (Xtext cleanup semantics
 * etc.).
 */
public class OutputConfiguration {

    /** Xtext's default output-configuration name; consumers rarely set it. */
    public static final String DEFAULT_OUTPUT_NAME = "DEFAULT_OUTPUT";

    private String name = DEFAULT_OUTPUT_NAME;
    private String outputDirectory;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}
