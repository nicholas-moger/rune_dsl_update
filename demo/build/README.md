# demo/build - the Build section (CONTRACTS section 7E)

Two tiny Maven projects generate Java from the **same** merged demo corpus with the
**two** toolchains, plus scripts to time them and byte-compare the results.

| | legacy leg | plus leg |
|---|---|---|
| project | `org.finos.rune.demo:demo-build-legacy:1.0.0` | `org.finos.rune.demo:demo-build-plus:1.0.0` |
| pom | `build-legacy/pom.xml` | `build-plus/pom.xml` |
| plugin | `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0` | `org.finos.rune:rune-maven-plugin:0.0.1-SNAPSHOT` |
| goal / phase | `generate` / `generate-sources` | `generate` / `generate-sources` |
| output | `demo/work/build-legacy-out/` | `demo/work/build-plus-out/` |

Everything else - source roots, `rosetta-config.yml`, the `<languages>` block, the
setup class name, `incrementalXtextBuild` - is **identical**. That is the point.

## 1. Prerequisites

`demo/work/bootstrap.ps1` must have completed. The build lane needs, in the
hermetic repo `demo/work/.m2`:

* `org.finos.rune:{rune-parser, rune-java-generator, rune-runtime, rune-maven-plugin}:0.0.1-SNAPSHOT`
* `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0`
* `com.regnosys.rosetta:com.regnosys.rosetta.lib:9.83.0` (+ its transitive
  `org.finos.rune:rune-runtime:9.83.0`)

All present as of the recorded bootstrap run. The `rune-common EXIT=1` line in
`demo/work/bootstrap.status` does **not** block this lane: neither `rune-parser`
nor `rune-java-generator` depends on `rune-common`, and those two (plus snakeyaml)
are the fork plugin's entire dependency set.

## 2. Exact commands

Run these from the repo root. **One JVM at a time** - `measure-build.ps1` is a
straight-line loop that blocks on each Maven process, so do not run two copies.

```
pwsh -File demo/build/measure-build.ps1
pwsh -File demo/build/diff-trees.ps1
```

Useful variants:

```
pwsh -File demo/build/measure-build.ps1 -Toolchain plus -Runs 1
pwsh -File demo/build/measure-build.ps1 -RepoLocal <REPO>\demo\work\.m2 -Runs 3
pwsh -File demo/build/measure-build.ps1 -Toolchain legacy -MavenArgs '-Pcp-filter'
pwsh -File demo/build/diff-trees.ps1 -AlsoAgainst demo/work/gen-m1
```

### The four Maven command lines, for `demo/runs.json`

`measure-build.ps1` issues exactly these, and writes the same string into each
receipt's `command` field (CONTRACTS section 2 requires the two to match). `<REPO>`
= the absolute repo root, `<M2>` = the `-RepoLocal` value.

```
mvn.cmd -B -f <REPO>/demo/build/build-legacy/pom.xml clean generate-sources -Dmaven.repo.local=<M2>
mvn.cmd -B -f <REPO>/demo/build/build-legacy/pom.xml generate-sources       -Dmaven.repo.local=<M2>
mvn.cmd -B -f <REPO>/demo/build/build-plus/pom.xml   clean generate-sources -Dmaven.repo.local=<M2>
mvn.cmd -B -f <REPO>/demo/build/build-plus/pom.xml   generate-sources       -Dmaven.repo.local=<M2>
```

Anything passed via `-MavenArgs` is appended to every invocation **and** to the
recorded `command`, so the receipt stays reproducible.

## 3. The receipts

Written to `demo/receipts/`, UTF-8 without BOM, LF endings.

| file | `id` | what it is |
|---|---|---|
| `build.legacy.clean.json` | `build.legacy.clean` | legacy: empty output tree + `mvn clean generate-sources` |
| `build.legacy.incremental.json` | `build.legacy.incremental` | legacy: `generate-sources` again, no clean, tree already populated |
| `build.plus.clean.json` | `build.plus.clean` | fork: same as above |
| `build.plus.incremental.json` | `build.plus.incremental` | fork: same as above |
| `build.paritydiff.json` | `build.paritydiff` | SHA-256 comparison of the two output trees |

### Build receipt metrics (CONTRACTS section 4)

```
{"toolchain":"legacy|plus","kind":"clean|incremental","wallMs":N,"runsMs":[...],"files":N,"exit":0}
```

* **`wallMs`** - the **median** of `runsMs`, in milliseconds. Median, not mean, so a
  single scheduling hiccup cannot move the headline number.
* **`runsMs`** - every individual run, in order, so anyone can check the spread.
* **`files`** - `*.java` files under the leg's output directory, counted after the
  last run of that kind.
* **`exit`** - the Maven exit code. A non-zero value is recorded honestly and the
  receipt is still written; the script itself then exits 1.
* **`durationMs`** (top level) - the **sum** of that kind's runs, i.e. the real time
  the receipt cost. It is deliberately not the same number as `wallMs`.

### What "clean" and "incremental" honestly mean

* **clean** = the output tree is deleted and `mvn clean` wipes the module `target/`.
  It is **not** a cold network: the dependency cache in `-RepoLocal` is already warm
  on purpose, because a download race is not what this compares.
* **incremental** = a second `generate-sources` with no `clean`, over an output tree
  the preceding run just filled. It measures **re-generation, not true
  incrementality**: neither plugin persists an Xtext index between JVMs
  (`writeStorageResources` defaults to false, and both poms set
  `incrementalXtextBuild=false`), so the second run repeats the whole
  parse/link/generate in a fresh JVM. The pair is still worth showing - the delta is
  what `clean` costs, and a large gap would mean one toolchain *is* caching
  something.
* Both numbers include JVM startup and Maven's own plugin resolution.

These sentences are copied verbatim into every receipt's `notes`.

### Parity-diff metrics

```
{"identicalFiles":N,"differingFiles":N,"onlyInA":N,"onlyInB":N,
 "totalPaths":N,"filesInA":N,"filesInB":N,
 "firstDiffering":[...],"firstOnlyInA":[...],"firstOnlyInB":[...],
 "alsoAgainst":{"root":"...","aVsC":{...},"bVsC":{...}}}
```

A = legacy tree, B = plus tree. A path counts as **identical** only if both files
have the same length *and* the same SHA-256; length is checked first purely as a
cheap early-out. Paths are compared **case-sensitively** (ordinal), because Java
package paths are, even though NTFS is not. Sample lists are capped by `-ShowFirst`
(default 20).

`alsoAgainst` appears only with `-AlsoAgainst <dir>`; inside it, `onlyInB` means
"only in the third tree".

**Differences are the result, not a failure.** `diff-trees.ps1` exits 0 whether the
trees match or not. It exits 1 only when a directory it was told to read is missing.

## 4. Configuration decisions, and the evidence behind them

The full reasoning is in each pom's header comment; the short version:

**Parameter names were verified, not guessed.** The 9.83.0 `generate` surface was
read from the cached plugin's own descriptor -
`rosetta-maven-plugin-9.83.0.jar -> META-INF/maven/plugin.xml`, unzipped to
`demo/work/tmp/rosetta-maven-plugin-9.83.0/`. The full parameter table is
transcribed in `build-legacy/pom.xml`'s header. The fork plugin's surface was read
from its source (`rune-maven-plugin/src/main/java/.../{RuneGenerateMojo,
AbstractRuneGeneratorMojo,Language,OutputConfiguration}.java`), which is a
deliberate clone of that descriptor.

**Models come from `<sourceRoots>`, not from project source roots.** plugin.xml says
so in as many words: *"The default value is a reference to the project's
`${project.compileSourceRoots}`. When adding a new entry the default value will be
overwritten not extended."* So the three corpus roots are wired directly and no
build-helper plumbing is needed. Paths are **absolute**
(`${project.basedir}/../../corpus/...`) because both mojos resolve these strings
with `Path.of` / `new File`, which would otherwise be CWD-relative - and
`measure-build.ps1` invokes Maven with `-f` from elsewhere.

**`demo/corpus/builtins` is deliberately NOT a fourth source root.** The plugin does
not auto-provide builtins from its own realm: its model scan runs over
`classpathElements`, which is read-only-bound to `${project.compileClasspathElements}`
- the **project's** compile classpath. So each pom declares the runtime jar as a
project dependency, and that jar carries the two builtin models
(`model/basictypes.rosetta`, `model/annotations.rosetta` - verified inside both
`rune-runtime-9.83.0.jar` and `rune-runtime-0.0.1-SNAPSHOT.jar`). Adding the
directory as well would declare `com.rosetta.model` **twice**. This is exactly what
the real CDM 6.20.6 and DRR 6.34.1 consumer poms do.

The builtins are never *generated* on either leg: the fork skips them by file name
(`GeneratorModel.IGNORED_FILES` = `{model-no-code-gen, basictypes, annotations}.rosetta`,
applied inside `shouldGenerate()` before the namespace filter), and Xtext generates
only source resources, never classpath ones. That is what makes the two trees
comparable at all.

*(Note for anyone diffing: `demo/corpus/builtins/*.rosetta` are CRLF copies of the
same bytes the jars carry with LF - identical line for line, including the literal
unfiltered `version "${project.version}"` in both. Since builtins are never
generated, the ending difference cannot reach any output byte.)*

**`classPathLookupFilter` is set on the plus leg and omitted on the legacy leg.**
This is the one asymmetry, and each side is the proven-correct choice:

* **plus - mandatory.** `RunePluginRunner.java:180-183` returns an *empty* library
  model list when the filter is null or blank. Omit it and the builtins never load.
  The value used is the CDM/DRR consumer regex character for character,
  `.*org[\\/]finos[\\/]rune[\\/]rune-runtime.*\.jar`, which full-matches the fork
  runtime jar's path in the hermetic repo.
* **legacy - omitted.** Xtext's rule is the inverse: unset means *scan every*
  classpath entry. This repo's own probe (recorded at
  `scripts/holdout-oracle/pom-template.xml:22-29`) found that on this box the CDM
  regex "matched 0 of the 22 classpath entries ... builtins then fail to resolve and
  the build fails loudly", while matching the jar path in isolation. With the filter
  omitted, `rune-runtime` is the only model-bearing jar on this project's classpath
  (checked: the shim's whole compile-scope closure is rune-runtime + xtend.lib,
  guice, commons-lang3, jackson-databind, jackson-datatype-jdk8, none of which carry
  `.rosetta` entries), so the lookup result and the generated bytes are the same.
  `scripts/holdout-oracle/pom-template.xml` - this repo's byte-golden oracle for the
  released 9.83.0 plugin - omits it for that reason and is the configuration proven
  to work here.

**`rosetta-config.yml` is shared.** Both poms point `rosettaConfig` at
`demo/build/rosetta-config.yml`. Its shape is copied from the real CDM/DRR consumer
files; the accept-list is `cdm.*`, `iso20022.*`, `drr.*`, `com.rosetta.model` - the
measured complete set of top-level namespace segments in `demo/corpus`, i.e.
CONTRACTS section 1's "everything except builtins" stated positively.

**Packaging is `jar`, and nothing is ever compiled.** `jar` (not `pom`) matches every
real consumer and this repo's proven oracle scaffold, so
`${project.compileClasspathElements}` and the lifecycle binding behave exactly as
upstream. Nothing compiles because the measured command stops at `generate-sources`,
which precedes `compile`; `maven.main.skip` / `maven.test.skip` in both poms make
that safe even if someone runs `mvn install` by hand.

## 5. If something goes wrong

**Legacy build fails on unresolved builtin types** (cannot resolve `string`,
`metadata`, ...): the classpath scan found no models. Re-enable the consumer regex:

```
pwsh -File demo/build/measure-build.ps1 -Toolchain legacy -MavenArgs '-Pcp-filter'
```

or directly `mvn.cmd -B -f demo/build/build-legacy/pom.xml -Pcp-filter clean generate-sources -Dmaven.repo.local=<M2>`.
Maven profiles can only add or override configuration, never remove it, which is why
omission is the base state and the filter is the opt-in.

**Plus build reports 0 library models / unresolved builtins**: the filter did not
match. Check the runtime jar's real path in `demo/work/.m2` against
`.*org[\\/]finos[\\/]rune[\\/]rune-runtime.*\.jar` (a full match, not a search).

**Per-run Maven logs** are at `demo/work/build-<toolchain>-<kind>-run<N>.log` and
`.err`. Both are gitignored.

**Receipts appear but with `exit` non-zero**: that is by design - read the log named
in the `##DEMO##` error line.

## 6. Open items for the integrator

* `// INTEGRATOR:` **THIS WHOLE DIRECTORY IS CURRENTLY GITIGNORED - fix before
  committing.** The repo-root `.gitignore` line 83 is `build/`, sitting in the
  *Python* section (next to `dist/`, `.venv/`, `*.egg-info/`); it was never meant
  for this path, but `build/` matches a directory named `build` at any depth, so
  `demo/build/` is excluded and git will not even descend into it. Confirmed:

  ```
  $ git check-ignore -v demo/build/build-legacy/pom.xml
  .gitignore:83:build/    demo/build/build-legacy/pom.xml
  ```

  There are no `demo` negations anywhere in `.gitignore`. Fixing it is outside this
  lane's write scope (CONTRACTS section 0.2), so it is left to you. Either works -
  the negation has to re-include the **directory**, because git cannot re-include a
  file whose parent directory is excluded:

  * add `!/build/` to `demo/.gitignore` (keeps the demo self-contained; a deeper
    `.gitignore` takes precedence for paths beneath it), or
  * add `!demo/build/` to the root `.gitignore` *after* line 83.

  `git add -f demo/build` would get this one commit in but leaves the trap armed for
  everyone after you.

The rest are things this lane could not settle without running Maven, which
CONTRACTS section 0 forbids:

* `// INTEGRATOR:` **The legacy leg has never been executed here.** The
  omit-the-filter decision rests on this repo's recorded probe, not on a run of
  *this* pom. First green run is the confirmation; `-Pcp-filter` is the fallback.
* `// INTEGRATOR:` **`incrementalXtextBuild=false` is set on both legs** for byte
  determinism (matching the repo's 9.83.0 oracle). If you would rather measure the
  plugin's default behaviour, drop it from `build-legacy/pom.xml` - the fork accepts
  and ignores it either way, so the plus leg is unaffected.
* `// INTEGRATOR:` **Do not add `com.regnosys.rosetta:com.regnosys.rosetta:9.83.0`
  as a project dependency** on the legacy leg. It is not needed (the plugin carries
  `org.finos.rune:rune-lang:9.83.0` in its own realm), and with the filter omitted
  every project-classpath jar is scanned for models.
* `// INTEGRATOR:` **File counts are unknown until the first run.** Nothing in this
  directory hard-codes an expected file count, and `diff-trees.ps1` asserts nothing -
  the numbers are the result.
* `// INTEGRATOR:` **`demo/work/gen-m1` is lane A's output.** `-AlsoAgainst` is wired
  and tested, but the three-way comparison only means something once lane A has run.
