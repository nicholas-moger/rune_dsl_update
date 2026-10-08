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
 * The {@code generate} goal — the released 9.83.0 plugin's main-build entry
 * (phase {@code generate-sources}, compile-scope dependency resolution),
 * executed on the fork pipeline. See {@link AbstractRuneGeneratorMojo} for the
 * parameter surface.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class RuneGenerateMojo extends AbstractRuneGeneratorMojo {

    /** Project compile classpath — the released mojo's library-model scan space. */
    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpathElements;

    /**
     * Model source roots. The released default is the project's compile source
     * roots; every measured consumer overrides it with the Maven-filtered
     * {@code target/classes/<model>/rosetta} copy.
     */
    @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
    private List<String> sourceRoots;

    @Override
    protected List<String> getClasspathElements() {
        // Mirror the released mojo: drop the project's own (not yet compiled)
        // output directories and empty entries, preserving order.
        Set<String> elements = new LinkedHashSet<>(classpathElements);
        elements.remove(getProject().getBuild().getOutputDirectory());
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
        getLog().debug("Adding output folder " + outputDirectory + " to compile roots");
        getProject().addCompileSourceRoot(outputDirectory);
    }
}
