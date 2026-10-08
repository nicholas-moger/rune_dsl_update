# demo-harness-legacy — the upstream Xtext 9.83.0 driver

`org.finos.rune.demo:demo-harness-legacy:1.0.0` · Java 21 · **two** main classes:

| main class | verb | section |
|---|---|---|
| `demo.harness.legacy.Main` | `generate` | CONTRACTS §4 `codegen` |
| `demo.harness.analytics.legacy.Main` | `legacy-emf` | CONTRACTS §4 `analytics` |

Both run against the **released upstream 9.83.0 artifacts** — never the vendored
`rune-dsl/` source tree, which sits at a later dev version whose grammar has removed syntax
9.83.0 still accepts.

---

## Why this is a separate module, and why the analytics lane lives here

The fork and upstream `rune-lang:9.83.0` **both own the Java package
`com.regnosys.rosetta.*`** with incompatible class shapes. Verified: the sub-packages
`parser`, `rosetta`, `types`, `utils` and `validation` exist in both
`org.finos.rune:rune-parser:0.0.1-SNAPSHOT` and `org.finos.rune:rune-lang:9.83.0`. Put both on
one classpath and Guice dies at graph construction with `IncompatibleClassChangeError` — the
failure recorded at length in `scripts/xtext-oracle/pom.xml`, which is the in-repo precedent
this module copies.

So the two lanes that need the upstream classpath — legacy generation and legacy EMF analytics
— ship together here, and the fork lanes ship in `demo-harness-codegen` /
`demo-harness-analytics`. The **query code is shared verbatim**: `Queries.java`, `Decl.java`
and `AnalyticsRun.java` are duplicated into this module byte-for-byte apart from their package
line, so the two analytics lanes execute literally the same query logic over their own
extractor's output. Any disagreement is therefore located in an extractor, never in a query.

---

## Coordinates and the two pins

`CONTRACTS.md` §0.5 names the upstream GAV as `com.regnosys.rosetta:com.regnosys.rosetta:9.83.0`,
which is what the pom declares. Note that artifact is a **1,716-byte shim containing only its
own pom**; its single dependency is `org.finos.rune:rune-lang:9.83.0` (3.5 MB), where every
class actually lives, which in turn pulls `org.finos.rune:rune-runtime:9.83.0` carrying
`model/basictypes.rosetta` and `model/annotations.rosetta` as classpath resources. All three
must resolve. **No `-sources.jar` is published for any of them** (the parent pom sets
`maven-source-plugin` to `phase none`), so every API used here was verified by reading class
constant pools out of the cached jars.

Two pins, both inherited from `scripts/xtext-oracle/pom.xml`, both required:

1. **`org.eclipse.emf:org.eclipse.emf.common` pinned forward to `2.44.0`, declared FIRST.**
   rune-lang 9.83.0 resolves `emf.common` to 2.30.0 (Xtext 2.38.0's direct dep, nearest-wins)
   while `emf.ecore` lands at 2.42.0 via `ecore.xmi` 2.37.0. The newer ecore calls
   `CommonUtil.newURL(String)`, absent from 2.30.0 → **`NoSuchMethodError` before anything
   parses**.
2. **Copy dependencies, never shade.** An uber-jar cannot carry two files at one path and the
   Eclipse/EMF stack ships a `plugin.properties` in almost every artifact; shading keeps one and
   drops the rest, which breaks EMF's diagnostic message lookup
   (`_UI_DiagnosticRoot_diagnostic` MissingResource) and makes the **validator return zero
   issues on every input** — indistinguishable from upstream having no complaints.

### Signature stripping (integrator step, required)

Because the jars are copied, **seven separately-signed Eclipse platform jars contribute classes
to the package `org.eclipse.core.runtime`** (equinox.common, osgi, core.runtime, core.jobs,
equinox.registry, equinox.preferences, core.contenttype). The JVM rejects that mix with:

```
SecurityException: class "org.eclipse.core.runtime.OperationCanceledException"'s signer
information does not match signer information of other classes in the same package
```

Strip `META-INF/*.SF`, `*.DSA`, `*.RSA`, `*.EC` from every jar **in `target/lib`** after
packaging. `scripts/xtext-oracle/strip-jar-signatures.py` already does exactly this. **Never**
strip them inside the local Maven repository.

---

## Build and run

```
mvn -f demo/harness/codegen/legacy/pom.xml -Dmaven.repo.local=demo/work/.m2 package
# then strip signatures in demo/harness/codegen/legacy/target/lib
java -cp "demo/harness/codegen/legacy/target/classes;demo/harness/codegen/legacy/target/lib/*" \
     demo.harness.legacy.Main generate --src "..." --out "..."
```

Expect one `SLF4J: No SLF4J providers were found` line on stderr — no logging binding is
declared, deliberately, so the module adds no resolution requirement beyond the three upstream
GAVs. It is a no-op warning.

---

## `generate` — the legacy codegen leg

```
generate --src "dir;dir;..." --out <dir>
         [--builtins <dir> | --builtins-ignored]
         [--filter drr|all] [--tolerate-errors]
         [--receipt <file>] [--command "<exact line>"]
```

Runs the same five-call lifecycle the upstream Maven plugin runs, minus the
`StandaloneBuilder` scaffolding it needs only in order to *be* a Maven plugin:

```java
gen.beforeAllGenerate(resourceSet, fsa, ctx);       // Rune extension, NOT on IGenerator2
for (r : subjects) gen.beforeGenerate(r, fsa, ctx);
for (r : subjects) gen.doGenerate(r, fsa, ctx);
for (r : subjects) gen.afterGenerate(r, fsa, ctx);  // emits package-info.java
gen.afterAllGenerate(resourceSet, fsa, ctx);        // Rune extension
```

`beforeAllGenerate` / `afterAllGenerate` are Rune additions that `IGenerator2` does not
declare, which is the entire reason upstream ships `RuneStandaloneBuilder` — a subclass whose
only job is to reflect into Xtext's `StandaloneBuilder` and `GeneratorDelegate` to reach them.
Calling `RosettaGenerator` directly makes them ordinary calls and drops the
`xtext-maven-plugin` / `maven-core` dependency tree entirely. **Skipping `afterGenerate` would
silently lose every `package-info.java`** — that is the pass it performs.

**Builtins.** `--builtins <dir>` loads `basictypes.rosetta` + `annotations.rosetta` from disk as
plain `file:` URIs (recommended; this is what the in-repo oracle does). With neither flag they
are extracted from `rune-runtime` on the classpath into a temp directory and loaded the same
way. `--builtins-ignored` loads none, which makes every basic type and `[metadata ...]`
annotation unresolved; it exists only so the flag named in the lane spec is honoured, and it
warns loudly.

Loading them from disk is safe because `RosettaBuiltinsService.getModel(...)` documents and
implements a fallback: if `resourceSet.getResource(classpathUri, false)` returns null it scans
the resource set for any resource whose URI ends with the same file name. What is **not**
optional is that the two `.rosetta` files exist as classpath *resources* — that service's
`basicTypesURL`/`annotationsURL` fields are `Objects.requireNonNull`-guarded and the service is
`@Inject`-ed into the scope provider, so a missing resource kills the **injector** before
anything is parsed. If you see an NPE at startup, `rune-runtime` is missing from `target/lib`.

**Output configurations** come from `injector.getInstance(IOutputConfigurationProvider.class)`
(the stable Xtext interface; `RosettaRuntimeModule` binds the Rosetta implementation to it),
rather than by constructing `RosettaOutputConfigurationProvider` directly. `DEFAULT_OUTPUT` is
pointed at `--out`; any other configuration is placed under `<out>/<config name>`.
`canClearOutputDirectory` is forced **off** — a demo must never be able to wipe a directory the
operator pointed it at.

**Filter.** Upstream's own `RosettaGenerator.shouldGenerate` consults `rosetta-config.yml` on
the thread context classloader and, finding none, generates everything; it independently
ignores `basictypes.rosetta`, `annotations.rosetta` and `model-no-code-gen.rosetta` by file
name. `--filter` selects subject resources here instead of shipping a config file, so upstream's
default stays untouched and the choice stays visible on the command line. **Caveat for
`--filter drr`:** `afterGenerate`'s package-info pass iterates the models in the *resource set*,
not the subject list, so package-info output may be wider than the filter. For `--filter all` —
the default and the comparable case — this does not arise. **Do not put a stray
`rosetta-config.yml` on this module's classpath**; upstream would pick it up and silently narrow
generation.

**Metrics.** `mode` (`legacy`), `files`, `wallMs`, `jvmStartToFirstFileMs`, `exit`, then
measured extras: `filter`, `sourceFiles`, `generatedModels`, `generateFileCalls`, `startupMs`,
`parseMs`, `resolveMs`, `generateMs`, `resourceErrors`.

- `jvmStartToFirstFileMs` comes from `CountingFsa`, a `JavaIoFileSystemAccess` subclass that
  overrides **only** the three-argument `generateFile(String, String, CharSequence)`. That is
  deliberate: `AbstractFileSystemAccess` implements the two-argument form by delegating to the
  three-argument one, and `JavaIoFileSystemAccess` overrides only the three-argument one — so
  hooking just that sees every write exactly once, and hooking both would double-count.
- `files` counts `.java` files **on disk** under `--out`; `generateFileCalls` counts what the
  generator asked for. If they differ the run says so rather than picking a winner (a re-run
  over a non-empty output directory legitimately leaves earlier files in place).
- `peakRssMb` is absent — CONTRACTS §4 assigns it to the outer wrapper.

**Errors.** Syntax and linking errors found on the corpus resources after `resolveAll` are
reported as `##DEMO## error` events and abort the run with exit `3`, matching the fork leg.
`--tolerate-errors` continues anyway.

---

## `legacy-emf` — the legacy analytics leg

```
java -cp "...classes;...lib/*" demo.harness.analytics.legacy.Main \
     legacy-emf --src "dir;dir;..." [--builtins <dir> | --builtins-ignored]
                [--tolerate-errors] [--receipt <file>] [--command "<exact line>"]
```

Same load + `resolveAll`, then an EMF walk that builds the §5 index and answers Q1–Q4. See
`demo/harness/analytics/README.md` for the full query semantics — they are defined once, in
`Queries.java`, and that file is shared by both lanes.

**The JVM + EMF startup and `resolveAll` cost being visible is the point.** `startupMs` (process
start → Guice injector ready) and `resolveMs` (`EcoreUtil.resolveAll`) are reported as
first-class numbers, not netted out.

EMF facts the walk depends on, all verified against the 9.83.0 jar:

- `RosettaModel.getName()` **is** the namespace. There is no `getNamespace()`.
- `attr.getTypeCall().getType().getName()` is the idiom; there is no `getType()` on the typed
  element itself.
- `Choice extends Data` and `FunctionDispatch extends Function` — subtype tests come first.
  `Choice` is **not** counted as a data type (the fork models it as a separate AST class, so
  counting it as a `Data` would make the lanes disagree by construction).
- `RosettaRule.isEligibility()` distinguishes reporting from eligibility rules.
- `RosettaReport` has **no** `getName()`; it is unnamed and skipped, as it is in the fork lane.
- A `[metadata scheme]` annotation is one `AnnotationRef` with
  `getAnnotation().getName() == "metadata"` and `getAttribute().getName() == "scheme"`. Q2
  matches the annotation name only.

Two upstream traps the walk deliberately avoids: **`Choice.getConditions()` mutates the
resource** (it lazily inserts a synthetic one-of condition) and **`ChoiceOption.getName()`
derives from the node model and can NPE**. Neither is called. The walk also uses
`model.getElements()` rather than `eAllContents()`, which sidesteps the duplicate-visit trap
where a `Choice` hands back each `ChoiceOption` twice.

Every cross-reference read is `eIsProxy()`-guarded. Where a proxy would cost an answer — an
unresolved super-type or attribute type — the source text is recovered from the node model
(`NodeModelUtils.findNodesForFeature` / `getTokenText`, with the feature looked up **by name**
on `eClass()` so no `SimplePackage.Literals` constant is hard-coded), and the number of
recoveries is reported as `unresolvedSuperTypes` / `unresolvedAttributeTypes`. On a clean
corpus both are 0; if the lanes ever disagree, those two counters are where to start.

---

## `// INTEGRATOR:` notes left in the source

- `CountingFsa` — the two-argument `JavaIoFileSystemAccess` super constructor
  `(IResourceServiceProvider.Registry, IEncodingProvider)` is **verified present** in Xtext
  2.38.0 and annotated `@com.google.inject.Inject`. A four-argument overload taking trace
  providers also exists; if a future Xtext drops the short form, take the extra two from the
  injector as well.
- `Main` — `injector.getInstance(IResourceServiceProvider.Registry.class)` and
  `injector.getInstance(IEncodingProvider.class)` are **verified bound** by
  `org.eclipse.xtext.service.DefaultRuntimeModule` (`bindIResourceServiceProvider$Registry`,
  `configureRuntimeEncodingProvider`). If the Registry binding ever disappears,
  `IResourceServiceProvider.Registry.INSTANCE` is the equivalent static singleton —
  `RosettaStandaloneSetupGenerated.register(injector)` populates exactly that one.
- `pom.xml` — an explicit `org.finos.rune:rune-runtime:9.83.0` dependency is present but
  commented out; uncomment only if the transitive path through `rune-lang` ever breaks.
