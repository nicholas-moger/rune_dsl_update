# Build the review implementation

This is the retained detailed record for the 22 September 2026 code snapshot. Start with the [current guide](BUILDING.md) for its present interpretation and scope. The commands and measurements below retain their original validation dates; they are not new runs.

For **Mode 0 (original Rune DSL)**, use the released compiler/runtime in the consumer's original POM; follow [the Mode 0 instructions](MAVEN-CONSUMERS.md#mode-0-original-rune-dsl). The module installation below is for Modes 1–3. Installing local SNAPSHOT artifacts does not replace the separately versioned released 9.83 artifacts.

The bootstrap, the three-mode example and the small initial checks below were executed from the assembled export for the current drop; the environment and results are in [Source snapshot § Executed checks](SOURCE-SNAPSHOT.md#executed-checks-and-results).

## Prerequisites

- JDK 21, with `JAVA_HOME` and `PATH` pointing to the same installation. Some modules emit Java 8 bytecode; that does not make JDK 8 a supported build JDK.
- Maven 3.9.11 as the documentation baseline, Git, and access to the Maven repositories declared in the POMs. No minimum-version claim is inferred from this baseline.
- PowerShell 7 for the supplied tree-comparison script. The Maven commands also work in a POSIX shell and do not require a manually assembled Java classpath.
- Python 3 only for optional evidence/oracle scripts that request it. The compiler itself is Java.

Run from the repository root. There is no root reactor POM or Maven wrapper in this implementation.

```sh
java -version
javac -version
mvn -version
git rev-parse HEAD
git status --short
```

The root `.mvn` configuration and individual POMs affect resource use. Parser/generator tests default to multiple forks; the small checks below limit those modules to one fork. Do not start simultaneous full corpus suites on a machine already running compiler validation.

## Install the compiler and Maven plugin

Install in this order. `-DskipTests` is used only to establish the inter-module dependencies; this bootstrap is not test evidence.

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-parser/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-ir/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-ir-java/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-ir-java-optimised/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-runtime/pom.xml install -DskipTests
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-maven-plugin/pom.xml install -DskipTests
```

Stop on a failed command. All seven modules above use `org.finos.rune:*:0.0.1-SNAPSHOT`; that coordinate does not guarantee the installed jars are from the same source revision. Reinstall prerequisites after source changes. Both IR providers must be installed before the plugin because its test dependencies are resolved even with `-DskipTests`. Test profiles load installed provider jars, so merely compiling a sibling can leave an older provider in use. The adapted runtime is installed for the small example below and for consumer experiments that select it; the generator itself still depends on the released `org.finos.rune:rune-runtime:9.83.0`.

## Run the small three-mode example

`examples/review` is a real Maven consumer of the installed modules. It generates Java from the plugin's authored fixture (`rune-maven-plugin/src/test/resources/route-fixture/model/route.rosetta`: an enum, data types with a type-format constraint, a condition, a scheme-annotated attribute, a choice and an echo function), compiles the result against the adapted runtime and runs three tests: the generated function returns its input, the generated type-format validator accepts a valid value and rejects a value over the declared limit. The route is selected only by the system properties on the command line; `review.mode` names the output directory so the three outputs coexist under `examples/review/target/`.

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f examples/review/pom.xml clean verify -Dreview.mode=m1 -Drosetta.generator.ir=false -Drosetta.generator.ir.optimised=false
mvn -B -ntp -Dmaven.build.cache.enabled=false -f examples/review/pom.xml clean verify -Dreview.mode=m2 -Drosetta.generator.ir=true -Drosetta.generator.ir.optimised=false
mvn -B -ntp -Dmaven.build.cache.enabled=false -f examples/review/pom.xml clean verify -Dreview.mode=m3 -Drosetta.generator.ir=false -Drosetta.generator.ir.optimised=true
```

For each command require: the route header in the log (`IR route: OFF` for Mode 1, the `IRGenerationProviderImpl` provider for Mode 2, the `OptimisedIRGenerationProviderImpl` provider for Mode 3, as listed in [Modes](MODES.md)); generated sources under `target/<mode>/generated-sources/rune`; and `Tests run: 3, Failures: 0, Errors: 0, Skipped: 0` in the Surefire summary. Modes 1 and 2 produce the same file set and bytes for this fixture; Mode 3 is accepted on compilation and the behavioural checks, not on byte equality. Provider selection on this fixture is not evidence about native file ownership on real models; that accounting is in [Evidence](EVIDENCE.md).

Then switch to your CDM, DRR or other pinned 9.83 checkout and follow [Consumer POM integration](MAVEN-CONSUMERS.md). Maven resolves the installed artifacts; pointing a POM at a GitHub source URL or local Git directory does not replace this installation step.

For a clean dependency-resolution check, add an absolute `-Dmaven.repo.local=...` path to **every** Maven command in the walkthrough, using a new review-only directory. Keep that setting consistent for installation, tests and harness packaging. Record the directory choice in the run receipt; do not publish machine-specific paths in documentation. Dependency acquisition and warm-cache performance measurements are different checks.

Some existing test helpers do not honour that override: `HoldOutCompileGateTest`, `WiderPoolCompileGateTest` and `UpstreamPortHarness` look for the released runtime jar under the user's default `.m2/repository`. A custom-cache build can therefore skip a compile check or read a pre-existing jar. Record the actual jar and hash; fully isolated-cache validation remains incomplete until that lookup is addressed. Do not silently populate a second cache and call the run isolated.

## Run a small initial check

These checks use local fixtures and avoid the full external corpus:

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-parser/pom.xml test -Dtest=PrecedenceVerificationTest -Dpr-speed.forkCount=1 -Dpr-speed.perForkHeap=2g
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Dtest=JavaCodeGeneratorTest -Dpr-speed.forkCount=1 -Dpr-speed.perForkHeap=2g
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-ir-java/pom.xml test -Dtest=IRGenerationSeamTest
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-ir-java-optimised/pom.xml test -Dtest=OptimisedGenerationSeamTest
```

Read the Surefire results and actual test counts. The seam tests exercise route selection; these four commands do not certify model compatibility or native IR closure. Continue with [Modes](MODES.md) and the [public CDM walkthrough](CORPUS-9.83.md). Before running a whole module suite, acquire the public builtin clone and its worktree and read [Testing § Core suites in a partial corpus](TESTING.md#core-suites-in-a-partial-corpus): the corpus guards fail by design without the full catalogue.

## Additional modules

| Purpose | Installation order after the compiler bootstrap |
|---|---|
| Generated-program execution | `rune-runtime`, then `demo/harness/exec` with profile `plus-rt` |
| Optional diagnostic/demo driver | `demo/harness/codegen` with `package`; not the primary three-mode consumer entry point |
| Downstream parser check | `rune-parser-consumer-smoke` |
| Runtime benchmarks/equivalence | `rune-runtime`, `rune-benchmarks`, then `rune-equivalence`; read their test prerequisites first |

Use `mvn -f <module>/pom.xml install` for a module, retaining the cache/local-repository settings above. The generator's default runtime dependency is the released `org.finos.rune:rune-runtime:9.83.0`. Installing the modified local runtime does not silently replace that coordinate. The `plus-rt` execution harness intentionally selects the modified snapshot runtime.

The parser's optional `xtext-harness` profile needs separate upstream snapshot artifacts. It is not required for the public bootstrap. The Maven plugin now dispatches its supported Java-generation passes through the IR provider hooks. Add provider dependencies to the consumer plugin and inspect route logs as described in [Modes](MODES.md).

## Diagnose a failure

| Symptom | Check |
|---|---|
| Local SNAPSHOT dependency missing | Install earlier modules at the same revision, with the same local-repository setting |
| IR flag set but existing route selected | Provider jar/service descriptor and the actual host entry point; a flag alone is insufficient |
| Corpus tests green with no meaningful population | Surefire skipped counts, source/golden directories and corpus-specific assumptions |
| Missing builtin types | Correct 9.83 builtin files; avoid loading duplicate builtin namespaces |
| Upstream/fork class-linkage errors | Keep the released upstream compiler and replacement parser on separate classpaths |
| Excessive memory or overlapping outputs | Active build processes, fork settings and distinct per-route output directories |

Do not weaken comparisons or regenerate reference outputs merely to make a build green. Retain the original failure and report whether it is a source defect, an input/dependency gap or an environment issue.
