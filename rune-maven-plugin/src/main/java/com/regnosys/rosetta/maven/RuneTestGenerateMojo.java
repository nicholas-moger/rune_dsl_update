package com.regnosys.rosetta.maven;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code testGenerate} goal — the released 9.83.0 plugin's test-build entry
 * (phase {@code generate-test-sources}, test-scope dependency resolution),
 * executed on the fork pipeline. No measured consumer invokes it (CDM 6.20.6 /
 * DRR 6.34.1 bind {@code generate} only); it completes the released goal
 * surface. See {@link AbstractRuneGeneratorMojo} for the parameter surface.
 */
@Mojo(name = "testGenerate", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES,
        requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
public class RuneTestGenerateMojo extends AbstractRuneGeneratorMojo {

    /** Project test classpath — the released test mojo's library-model scan space. */
    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    /** Model test source roots (released default: the test compile source roots). */
    @Parameter(defaultValue = "${project.testCompileSourceRoots}", required = true)
    private List<String> sourceRoots;

    @Override
    protected List<String> getClasspathElements() {
        // Mirror the released test mojo: drop the project's own test output
        // directory and empty entries, preserving order.
        Set<String> elements = new LinkedHashSet<>(classpathElements);
        elements.remove(getProject().getBuild().getTestOutputDirectory());
        elements.removeIf(e -> e == null || e.isBlank());
        return new ArrayList<>(elements);
    }

    @Override
    protected List<String> getSourceRoots() {
        return sourceRoots;
    }

    @Override
    protected void addOutputToProject(String outputDirectory) {
        getLog().debug("Adding output folder " + outputDirectory + " to test compile roots");
        getProject().addTestCompileSourceRoot(outputDirectory);
    }
}
