package com.regnosys.rosetta.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The released 9.83.0 rosetta-maven-plugin's generate-goal parameter surface
 * (every consumer-configurable parameter name, type and default from the
 * released {@code META-INF/maven/plugin.xml}), executed on the fork pipeline
 * via {@link RunePluginRunner}. A consumer pom swaps the plugin GAV and keeps
 * its whole {@code <configuration>} block unchanged.
 *
 * <p><b>Honored parameters</b> — {@code sourceRoots}, {@code classpathElements}
 * + {@code classPathLookupFilter} (the library-model channel: the
 * {@code rune-runtime} jar's {@code basictypes.rosetta}/{@code
 * annotations.rosetta}), {@code rosettaConfig} (namespace accept-list +
 * doNotPrune), {@code languages} (single entry; Java setup only),
 * {@code failOnValidationError}, {@code addOutputDirectoriesToCompileSourceRoots}.
 *
 * <p><b>Accepted-and-ignored parameters</b> (Xtext build-machinery knobs with
 * no fork counterpart — the fork always runs a full in-memory build and never
 * compiles Java or writes Xtext index state): {@code incrementalXtextBuild},
 * {@code clusteringConfig}, {@code compilerSourceLevel},
 * {@code compilerTargetLevel}, {@code compilerSkipAnnotationProcessing},
 * {@code compilerPreserveInformationAboutFormalParameters},
 * {@code javaSourceRoots}, {@code tmpClassDirectory},
 * {@code writeClasspathConfiguration}, {@code classpathConfigurationLocation},
 * {@code writeStorageResources}. Each is declared so an existing consumer
 * configuration parses; none changes fork behavior.
 *
 * <p>The released descriptor's {@code mojoExecution}/{@code pluginDependencies}
 * parameters are read-only Maven plumbing (default-bound, not consumer
 * surface) and are not redeclared here.
 */
public abstract class AbstractRuneGeneratorMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter
    private List<Language> languages;

    @Parameter
    private String classPathLookupFilter;

    @Parameter
    private String rosettaConfig;

    @Parameter(defaultValue = "true")
    private boolean addOutputDirectoriesToCompileSourceRoots = true;

    @Parameter(defaultValue = "true")
    private Boolean failOnValidationError = Boolean.TRUE;

    // --- accepted-and-ignored (see class javadoc) ---

    @Parameter(defaultValue = "true")
    private boolean incrementalXtextBuild;

    @Parameter
    private ClusteringConfig clusteringConfig;

    @Parameter(property = "maven.compiler.source", defaultValue = "1.6")
    private String compilerSourceLevel;

    @Parameter(property = "maven.compiler.target", defaultValue = "1.6")
    private String compilerTargetLevel;

    @Parameter(defaultValue = "false")
    private Boolean compilerSkipAnnotationProcessing;

    @Parameter(defaultValue = "false")
    private Boolean compilerPreserveInformationAboutFormalParameters;

    @Parameter(defaultValue = "${project.compileSourceRoots}", required = true)
    private List<String> javaSourceRoots;

    @Parameter(defaultValue = "${project.build.directory}/xtext-temp")
    private String tmpClassDirectory;

    @Parameter(defaultValue = "false")
    private boolean writeClasspathConfiguration;

    @Parameter(defaultValue = "${project.build.directory}/xtext.classpath")
    private String classpathConfigurationLocation;

    @Parameter(defaultValue = "false")
    private boolean writeStorageResources;

    /** Subclass hook: compile vs test classpath. */
    protected abstract List<String> getClasspathElements();

    /** Subclass hook: compile vs test source roots. */
    protected abstract List<String> getSourceRoots();

    /** Subclass hook: compile vs test source-root registration. */
    protected abstract void addOutputToProject(String outputDirectory);

    protected final MavenProject getProject() {
        return project;
    }

    @Override
    public final void execute() throws MojoExecutionException {
        Language language = resolveSingleLanguage();
        List<OutputConfiguration> outputs = language.getOutputConfigurations();
        if (outputs == null || outputs.size() != 1 || outputs.get(0).getOutputDirectory() == null) {
            throw new MojoExecutionException(
                    "Exactly one outputConfiguration with an outputDirectory is required "
                            + "for the Java pipeline; got: "
                            + (outputs == null ? 0 : outputs.size()));
        }
        String configuredOutput = outputs.get(0).getOutputDirectory();

        if (addOutputDirectoriesToCompileSourceRoots) {
            // Mirror the released mojo: register the CONFIGURED string; Maven
            // resolves relative entries against the project basedir later.
            addOutputToProject(configuredOutput);
        }

        Path baseDir = project.getBasedir().toPath();
        Path outputDir = baseDir.resolve(configuredOutput).normalize();

        RosettaConfigFile config = loadConfig();
        List<Path> roots = new ArrayList<>();
        for (String root : getSourceRoots()) {
            if (root == null || root.isBlank()) {
                continue;
            }
            Path rootPath = Path.of(root);
            if (Files.isDirectory(rootPath)) {
                roots.add(rootPath);
            } else {
                getLog().info("Skipping non-existent source root: " + root);
            }
        }
        if (roots.isEmpty()) {
            throw new MojoExecutionException("No existing sourceRoots to build: " + getSourceRoots());
        }

        RunePluginRunner runner = new RunePluginRunner(
                roots,
                getClasspathElements(),
                classPathLookupFilter,
                config,
                outputDir,
                Boolean.TRUE.equals(failOnValidationError),
                new RunePluginRunner.RunnerLog() {
                    @Override
                    public void info(String message) {
                        getLog().info(message);
                    }

                    @Override
                    public void warn(String message) {
                        getLog().warn(message);
                    }

                    @Override
                    public void error(String message) {
                        getLog().error(message);
                    }
                });
        try {
            runner.run();
        } catch (RunePluginRunner.ValidationFailedException e) {
            // The released mojo's failure message, byte-for-byte.
            throw new MojoExecutionException("Execution failed due to a severe validation error.");
        } catch (IOException e) {
            throw new MojoExecutionException("Rune generation failed: " + e.getMessage(), e);
        }
    }

    private Language resolveSingleLanguage() throws MojoExecutionException {
        if (languages == null || languages.isEmpty()) {
            // The released mojo's empty-languages failure message, byte-for-byte.
            throw new MojoExecutionException("Only one language supported by the Rosetta Plugin.");
        }
        Language language = languages.get(0);
        if (!Language.ROSETTA_JAVA_SETUP.equals(language.getSetup())) {
            throw new MojoExecutionException(
                    "Unsupported language setup '" + language.getSetup() + "': the fork plugin "
                            + "implements the Java pipeline (" + Language.ROSETTA_JAVA_SETUP + ") "
                            + "only. Keep non-Java language profiles on the upstream plugin.");
        }
        return language;
    }

    private RosettaConfigFile loadConfig() throws MojoExecutionException {
        if (rosettaConfig == null || rosettaConfig.isBlank()) {
            return RosettaConfigFile.defaults();
        }
        Path yml = Path.of(rosettaConfig);
        if (!Files.isRegularFile(yml)) {
            throw new MojoExecutionException("rosettaConfig file not found: " + rosettaConfig);
        }
        return RosettaConfigFile.load(yml);
    }
}
