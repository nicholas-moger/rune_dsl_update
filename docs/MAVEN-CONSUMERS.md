# Build a CDM, DRR or other Rune 9.83 project

The consumer project runs generation through Maven. Clone this review repository, install its compiler artifacts locally, then select them in the consumer's Java-generation plugin. A GitHub source URL is not a Maven artifact repository: Maven cannot resolve jars directly from the Git clone. A hosted Maven package registry is a separate distribution option; none is required for this local review.

The guide covers four configurations: **0 original Rune DSL; 1 Java parser → AST → parity Java; 2 Java via IR; 3 optimised Java via IR**. Mode 0 uses released artifacts without building the replacement modules. Modes 1–3 use the local replacement plugin; Mode 2 includes temporary AST fallbacks, and Mode 3's emission remains AST-hosted with IR-selected optimisations.

This recipe was checked against the implementation and the pinned CDM 5.38.0 POMs. For the current drop the three replacement routes were exercised through `examples/review` and the Mode 0 reference build and D11 comparison of the [walkthrough](CORPUS-9.83.md); the consumer-mode CDM builds described here were not executed, as [Source snapshot § Not executed](SOURCE-SNAPSHOT.md#not-executed-from-this-export) records.

## 1. Install the implementation

This installation is for Modes 1–3. Mode 0 is configured separately below.

Clone the private review repository using your authorised GitHub account:

```sh
git clone https://github.com/nicholas-moger/rune_dsl_update.git
cd rune_dsl_update
git rev-parse HEAD
```

Follow the ordered module installation in [Building](BUILDING.md). Maven coordinates are `org.finos.rune:*:0.0.1-SNAPSHOT`; the GitHub account name is not part of them. Record the revision because successive review drops currently reuse these SNAPSHOT versions. Use the same Maven local repository for installation and the consumer build.

## 2. Keep separate consumer checkouts

Complete the [public CDM acquisition and reference instructions](CORPUS-9.83.md) first. Keep that clone unchanged. From that CDM clone, create one checkout of the same pin per replacement mode:

```sh
git worktree add --detach ../cdm-review-m1 788f63af47723ca2ef2ec8d24f31c01a4ac4505f
git worktree add --detach ../cdm-review-m2 788f63af47723ca2ef2ec8d24f31c01a4ac4505f
git worktree add --detach ../cdm-review-m3 788f63af47723ca2ef2ec8d24f31c01a4ac4505f
```

Use initially absent destinations. The names identify separate output trees; they do not select modes. Preserve the tag's actual Maven project version, `0.0.0.master-SNAPSHOT`, and resource filtering. The tag `5.38.0` is not the generated model's version stamp; do not apply a release-version rewrite for this comparison.

CDM 5.38.0 explicitly configures `clean` to remove `src/generated` and `src/test/generated/java`. Preserve the independent reference first; the commands below clean only the separate, disposable replacement checkouts. For other projects, inspect their clean configuration: generated Java outside `target` is not removed automatically. Do not mix output from different routes.

## 3. Replace the Java-generation plugin

Apply this change only in the Mode 1, 2 and 3 consumer checkouts. Keep the Mode 0 checkout unchanged.

In CDM 5.38.0, replace the normal Java-generation `com.regnosys.rosetta:rosetta-maven-plugin` block under `rosetta-source/pom.xml` → `build/plugins` with this block. Do not append a second active Java-generation execution. Leave other profiles and the parent-managed upstream plugin intact for their original purposes.

```xml
<plugin>
  <groupId>org.finos.rune</groupId>
  <artifactId>rune-maven-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <dependencies>
    <dependency>
      <groupId>org.finos.rune</groupId>
      <artifactId>rune-ir-java</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>org.finos.rune</groupId>
      <artifactId>rune-ir-java-optimised</artifactId>
      <version>0.0.1-SNAPSHOT</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <id>default-cli</id>
      <phase>generate-sources</phase>
      <goals><goal>generate</goal></goals>
      <configuration>
        <rosettaConfig>${project.basedir}/src/main/resources/rosetta-config.yml</rosettaConfig>
        <sourceRoots>
          <sourceRoot>${project.basedir}/target/classes/cdm/rosetta</sourceRoot>
        </sourceRoots>
        <classPathLookupFilter>.*org[\\/]finos[\\/]rune[\\/]rune-runtime.*\.jar</classPathLookupFilter>
        <failOnValidationError>true</failOnValidationError>
        <languages>
          <language>
            <setup>com.regnosys.rosetta.RosettaStandaloneSetup</setup>
            <outputConfigurations>
              <outputConfiguration>
                <outputDirectory>${project.basedir}/src/generated/java</outputDirectory>
              </outputConfiguration>
            </outputConfigurations>
          </language>
        </languages>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Both providers may be on the plugin classpath; the JVM flags select a route. These dependencies belong **inside the plugin**, not merely in application dependencies. The plugin's own POM lists them for tests only, so consumers must add them explicitly.

Preserve CDM's existing `initialize` resource-copy execution, which filters model versions into `target/classes/cdm/rosetta` before generation. Keep its generated-source registration and downstream compiler/test settings. Raw `src/main/rosetta` would bypass filtering. Anchor source/configuration paths to `${project.basedir}`, especially with `mvn -f`.

The setup class name is an accepted configuration identifier here, not an instruction to instantiate Xtext. Do not copy upstream compiler or custom Xtext generator jars into the replacement plugin dependencies: overlapping package names do not make their AST classes interchangeable.

## 4. Select the runtime separately

For the first compiler comparison, retain the consumer's released 9.83 runtime and companion libraries. CDM declares the legacy `com.regnosys.rosetta:com.regnosys.rosetta.lib` coordinate; confirm that it resolves the builtin carrier `org.finos.rune:rune-runtime:9.83.0`:

This is mandatory for the unchanged Mode 0 baseline. For Modes 1–3 it holds the runtime constant while comparing compiler output. The new runtime can then be measured as an independent change.

```sh
mvn -B -ntp -pl rosetta-source dependency:tree -Dincludes=org.finos.rune:rune-runtime
```

The plugin scans the **project compile classpath** for that jar using `classPathLookupFilter`. A blank filter loads no library models. Do not also add builtin files on disk as source roots.

To test the adapted runtime as a separate variant, install `rune-runtime/pom.xml` from this review clone and add a direct project dependency on `org.finos.rune:rune-runtime:0.0.1-SNAPSHOT` in each module compiling or testing generated code. Check the dependency tree to confirm exactly one runtime version resolves. Do not change the shared `rosetta.dsl.version` property to the snapshot: it can also select upstream compiler/test artifacts with no replacement at that coordinate. Compiler mode and runtime version are separate experiment dimensions.

The snapshot runtime's lazy failure messages and deferred path representations are built into its implementation, not protected by a global legacy-mode flag. It also removes the Xtend runtime dependency and changes dependency pins. A true Mode 0 therefore selects the released jar and the consumer's original resolved dependency set; it does not try to disable individual optimisations in the new jar. Restart/rebuild with the selected dependency set. Existing `legacy-rt` / `plus-rt` profiles in `demo/harness/exec/pom.xml` demonstrate separate runtime packaging, but do not create those profiles in a consumer project.

## Mode 0: original Rune DSL

Use the untouched pinned consumer clone from the corpus guide. Its original POM already selects the released compiler and runtime. Do not apply the replacement plugin block or the snapshot runtime dependency. From that CDM clone's root:

```sh
mvn -B -ntp -pl rosetta-source -am process-sources -Dmaven.build.cache.enabled=false
mvn -B -ntp -pl rosetta-source dependency:tree -Dincludes=org.finos.rune:rune-runtime
```

Require the executed plugin to be `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0`, with `org.finos.rune:rune-runtime:9.83.0` resolved for the project. Keep the original companion-library dependency set and record the full dependency tree when comparing execution. There should be no replacement plugin/provider or local snapshot runtime in the corresponding build/application classpaths. Output is `rosetta-source/src/generated/java`.

To compile and test, use `verify` instead of `process-sources`. Preserve the pristine reference and run the corpus verifier before changing POMs or cleaning generated sources. The untouched checkout is a real original Rune build, not Mode 1 with IR disabled. This gives Mode 0 through existing Maven artifact selection; a unified four-way profile selector in the review implementation remains unimplemented.

## 5. Generate, then compile and test

From the root of each corresponding patched CDM checkout, use one command:

```sh
# Mode 1: replacement parser, compatibility Java, no IR
mvn -B -ntp -pl rosetta-source -am clean process-sources -Dmaven.build.cache.enabled=false -Drosetta.generator.ir=false -Drosetta.generator.ir.optimised=false

# Mode 2: IR compatibility Java, including explicit temporary AST fallbacks
mvn -B -ntp -pl rosetta-source -am clean process-sources -Dmaven.build.cache.enabled=false -Drosetta.generator.ir=true -Drosetta.generator.ir.optimised=false

# Mode 3: IR-selected optimised Java
mvn -B -ntp -pl rosetta-source -am clean process-sources -Dmaven.build.cache.enabled=false -Drosetta.generator.ir=false -Drosetta.generator.ir.optimised=true
```

Use a fresh Maven invocation per route. Pass the switches on the command line: an ordinary Maven `<properties>` entry alone is not an established substitute for the JVM system properties the generator reads. [Modes](MODES.md) gives the required log headers.

Output: `rosetta-source/src/generated/java`. Save the full log, POM diff, revisions, dependency tree, source/output counts and output hashes. To compile and run the module tests, replace `process-sources` with `verify` while retaining both mode switches; then exercise the consumer's usual wider integration suite with the same selection. Record actual modules/tests, rather than treating generation alone as full CDM/DRR acceptance.

## DRR and other 9.83 projects

Replace the Java-generation plugin in the module owning generation, retaining that release's preparation steps, dependency pins, namespace filters and source/output roots.

In the inspected DRR 6.34.1 POM, the module is also `rosetta-source`, but the prepared source root is `${project.basedir}/target/classes/drr/rosetta`. Its preceding steps unpack dependency models and filter them together with DRR sources. Preserve those steps; raw DRR sources alone omit required models. Its root POM declares Rune 9.83.0, CDM 5.37.0 and ISO 20022 1.37.0. Retain those pins for a normal DRR build. The historical corpus's CDM 5.38.0 / ISO 1.38.0 substitutions are a separate recorded comparison, not recommended POM changes.

Obtain DRR from your authorised project source and record its commit/dependency closure; this guide does not assume a publicly accessible DRR clone. For other projects, use the actual generation module instead of `-pl rosetta-source` and verify the compiler pin is 9.83.0. A different DSL 9 release is a separate compatibility case.

## Integration boundaries

- The plugin implements the normal Java setup. Formatting, custom Xtext setups and non-Java profiles retain their existing implementations.
- The YAML configuration currently supplies `generators.namespaces` and `generators.doNotPrune`. Other sections may parse without being implemented. CDM's tabulator configuration is not evidence of tabulator generation support.
- Review custom generators and additional output families in each project. The 11-kind compatibility corpus does not certify arbitrary extra outputs or the whole application build.
- The current Maven runner dispatches its supported generation passes through IR hooks. Use it for consumer testing; the older demo driver does not integrate every hook.
- Complete upstream/replacement build and editor coexistence remains contribution work. These separate-checkout recipes exercise current compiler routes without claiming complete in-place migration.

## References

- [Pinned CDM Java-generation POM](https://github.com/finos/common-domain-model/blob/788f63af47723ca2ef2ec8d24f31c01a4ac4505f/rosetta-source/pom.xml) and [parent POM](https://github.com/finos/common-domain-model/blob/788f63af47723ca2ef2ec8d24f31c01a4ac4505f/pom.xml).
- [Maven local and remote repositories](https://maven.apache.org/guides/introduction/introduction-to-repositories.html).
- Implementation: `rune-maven-plugin/pom.xml`, `RunePluginRunner`, `AbstractRuneGeneratorMojo`, `RosettaConfigFile`; selection in `rune-java-generator` → `generator/java/spi/IRGeneration.java`.
