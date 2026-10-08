package com.regnosys.rosetta.maven;

import java.util.ArrayList;
import java.util.List;

/**
 * XML-mappable stand-in for {@code org.eclipse.xtext.maven.Language}, the bean
 * the released 9.83.0 rosetta-maven-plugin accepts as each entry of its
 * {@code languages} parameter. The fork plugin declares its own bean with the
 * same configurable element names ({@code setup}, {@code outputConfigurations},
 * {@code javaSupport}) so a consumer pom's existing {@code <language>} block
 * maps unchanged.
 *
 * <p>{@code setup} carries the upstream Xtext standalone-setup class name. The
 * fork does not run Xtext: the canonical Java value
 * ({@link #ROSETTA_JAVA_SETUP}) selects the fork's Java pipeline, and any other
 * value (the per-language profile setups — DAML, TypeScript, Scala, ...) is
 * rejected with a clear error, since those generators are out of fork scope
 * (see the development audit "2026-07-19-leg-s-swap-recon-ledger" § Leg P
 * sizing: a swap consumer keeps non-Java profiles on the upstream plugin).
 */
public class Language {

    /**
     * The upstream setup class name that selects the Java pipeline — the value
     * every measured consumer passes for the default-build generate execution
     * (CDM 6.20.6 + DRR 6.34.1 rosetta-source poms).
     */
    public static final String ROSETTA_JAVA_SETUP = "com.regnosys.rosetta.RosettaStandaloneSetup";

    private String setup;
    private List<OutputConfiguration> outputConfigurations = new ArrayList<>();
    /**
     * Xtext's per-language Java-linking toggle. The released mojo force-sets it
     * to {@code false} for Rosetta before launching; the fork pipeline has no
     * Java-linking stage at all, so the value is accepted and ignored.
     */
    private boolean javaSupport = true;

    public String getSetup() {
        return setup;
    }

    public void setSetup(String setup) {
        this.setup = setup;
    }

    public List<OutputConfiguration> getOutputConfigurations() {
        return outputConfigurations;
    }

    public void setOutputConfigurations(List<OutputConfiguration> outputConfigurations) {
        this.outputConfigurations = outputConfigurations == null ? new ArrayList<>() : outputConfigurations;
    }

    public boolean isJavaSupport() {
        return javaSupport;
    }

    public void setJavaSupport(boolean javaSupport) {
        this.javaSupport = javaSupport;
    }
}
