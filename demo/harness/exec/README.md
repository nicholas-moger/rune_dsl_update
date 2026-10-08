# demo/harness/exec - the Execution section (lane B)

Runs the **fork-generated DRR 6.34.1 Java closure** on **two runtime legs** and measures five
workloads. The generated code is the same on both legs, byte for byte; only the runtime under
it changes. That is the whole design: if the code is identical and only the runtime differs,
any difference in the numbers is attributable to the runtime.

| | `legacy-rt` | `plus-rt` |
|---|---|---|
| runtime (direct) | `com.regnosys.rosetta:com.regnosys.rosetta.lib:9.83.0` | `org.finos.rune:rune-runtime:0.0.1-SNAPSHOT` |
| runtime (resolves to) | `org.finos.rune:rune-runtime:9.83.0` | (the fork itself) |
| jackson | `2.17.1` | `2.18.9` |
| commons-lang3 | `3.14.0` | `3.18.0` |
| guice | `6.0.0` | `6.0.0` (held equal) |
| xtend.lib on classpath | yes (xtext 2.38.0, pulled by the upstream runtime) | no (the fork removed it) |

Every one of those pins is a **direct dependency** of the leg's maven profile *and* is repeated
in that profile's `dependencyManagement`, so no transitive path can move it. Guice is held
equal on purpose: it is the injector both legs share, which leaves runtime + jackson +
commons-lang3 as the only moving parts. The xtend row is a real, disclosed difference, not an
oversight - the fork genuinely dropped that dependency.

The pins are written into `src/main/resources/demo-exec-leg.properties` by maven resource
filtering and read back at runtime by `LegPins`. **A jar therefore always reports the
dependency set it was actually built with**, and refuses to run under the wrong `--leg`. They
also land verbatim in every receipt's `notes`.

---

## 1. Build (two jars, one per leg)

Each profile builds into **its own directory** and attaches **its own shaded classifier**, so
the two jars coexist and neither build can shade classes compiled against the other leg's
runtime.

```sh
# leg 1
mvn -f demo/harness/exec/pom.xml -P legacy-rt clean package
#   -> demo/harness/exec/target/legacy-rt/demo-harness-exec-1.0.0-legacy-rt.jar

# leg 2 (needs the hermetic repo, where the integrator installed the fork engine)
mvn -f demo/harness/exec/pom.xml -P plus-rt clean package \
    -Dmaven.repo.local=demo/work/.m2
#   -> demo/harness/exec/target/plus-rt/demo-harness-exec-1.0.0-plus-rt.jar
```

`clean` is safe on both: it only removes that profile's own build directory.

**Building with no `-P` is rejected at runtime**, not silently allowed - the pins stay `UNSET`
and `LegPins.requireLeg` refuses, because an unattributable jar is worse than a build error.

Shorthands used below:

```sh
LEGACY=demo/harness/exec/target/legacy-rt/demo-harness-exec-1.0.0-legacy-rt.jar
PLUS=demo/harness/exec/target/plus-rt/demo-harness-exec-1.0.0-plus-rt.jar
```

---

## 2. Prerequisite: the generated tree

Before anything here runs, the integrator must have produced the fork-generated closure at
`demo/work/gen-m1/` (lane A, `generate --mode m1`). This lane never generates and never
parses; it compiles and executes what lane A emitted.

Point it elsewhere with `--src DIR` or `-Ddemo.gen=DIR` - e.g. at
`demo/work/build-legacy-out/` to cross-check that both toolchains' output executes
identically.

---

## 3. Verb: `setup` (once per leg)

Compiles `demo/work/gen-m1/` into `demo/work/exec-classes/<leg>/` **against that leg's
runtime**.

```sh
java -Xmx4g -jar $LEGACY setup --leg legacy-rt
java -Xmx4g -jar $PLUS   setup --leg plus-rt
```

### Exact classpath note

The javac classpath is this JVM's own `java.class.path`. Launched with `java -jar <shaded
jar>` that is **exactly one entry: the shaded jar**, which contains the leg's runtime, jackson,
commons-lang3 and guice at their pinned versions and nothing else. That is the entire
mechanism by which "compiled against the leg" is true - there is no second source of runtime
classes to leak in. Add more with `-Ddemo.exec.extraCp=<paths>` if a closure ever needs it.

`-Xmx4g` is a recommendation, not a requirement: the DRR closure is a large single javac
invocation. If the compile fails, the **full** diagnostic list is written to
`demo/work/exec-classes/<leg>/compile-errors.txt` (the first five also print), so triage does
not need a second multi-minute run.

Options: `--src DIR`, `--exclude TOKEN` (drop sources whose path contains TOKEN - counted, never
silently dropped), `--force` (ignore the up-to-date marker).

### The up-to-date marker

`.compiled-marker` records source count, exclusions, a **content stamp** (SHA-256 over every
source's relative path, size and mtime) **and the leg's pins**. The reference benchmark keys
its marker on source count alone, which is sound there because golden drift is gated by the
frozen-corpus manifest test - nothing gates `gen-m1`, and a regenerated tree can easily have
the same file count. The pins are in the identity so classes built against one runtime can
never be reused for the other; `run` re-checks them before executing anything.

---

## 4. Verb: `run` (one measurement)

```sh
java -jar $PLUS run --leg plus-rt --workload report --ops 20 --warmup 5 \
     --receipt demo/receipts/execution.report.plus-rt.json \
     --command "<the exact line from demo/runs.json>"
```

| option | default | meaning |
|---|---|---|
| `--leg` | required | must match the jar's own leg |
| `--workload` | required | `populate` / `serialise` / `validate` / `report` / `projection` |
| `--ops` | 20 | measured ops (must be positive) |
| `--warmup` | 5 | untimed ops first |
| `--limit` | 0 | 0 = every member; otherwise a deterministic stratified subset |
| `--depth` | 2 | builder-population depth for synthetic inputs |
| `--receipt` | - | write the CONTRACTS section 2 receipt here |
| `--command` | - | the exact reproducible line the receipt should record |

`--command` matters: CONTRACTS section 8 makes `demo/runs.json` the single source for command
lines, and every receipt's `command` must equal what it holds. Without it the runner
reconstructs the line from argv **and says so in the notes**, rather than passing a guess off
as the real thing.

Emits `##DEMO##` `start` / `progress` / `metric` / `done` lines (CONTRACTS section 3) plus
ordinary log. Metrics carry the section 4 execution keys - `leg`, `workload`, `ops`,
`warmupOps`, `meanMs`, `p95Ms`, `errors` - plus `requestedOps`, `minMs`, `maxMs`, `unitsPerOp`,
`unitLabel`, `treeStamp`, `checksum` and the nested `census`. `allocMbPerOp` appears **only**
when the JVM exposed per-thread allocation counters; it is omitted rather than zeroed
otherwise.

### The five workloads

| workload | one op is | notes |
|---|---|---|
| `populate` | build one instance of each of N model types through their generated builders | default N = 200, stratified across the whole closure |
| `serialise` | write + re-parse each of N populated objects (`value -> String -> JsonNode`) | objects built once in setup; see section 6 |
| `validate` | every usable XMeta registry's cardinality validator, type-format validator and data rules over populated instances | the `CdmValidationSweepBenchmark` pattern |
| `report` | evaluate every probe-surviving `*ReportFunction` once | the `DrrRuleEvaluationBenchmark` pattern; heaviest and most representative |
| `projection` | evaluate every probe-surviving function under `drr.projection..functions` | many take **zero** arguments (`Create_TradeReportHeader`) - handled as a valid call |

---

## 5. Verb: `live` (the dashboard's sample flow)

```sh
java -jar $PLUS live --leg plus-rt --ops 3 --limit 25
```

One small pass of each workload in order - **create -> serialise -> validate -> report ->
projection** - streaming a `metric` event per step and a `progress` event per completed step.
A failing step is reported and the pass continues, so the dashboard shows the steps that
worked *and* the reason the others did not.

It also writes **one real serialised object** to `demo/work/live-sample.json`, with its
provenance beside it, so the site can display an actual model instance rather than a mock:

```json
{
  "leg": "plus-rt",
  "runtime": "org.finos.rune:rune-runtime:0.0.1-SNAPSHOT",
  "serialiser": "jackson-rune-annotated",
  "type": "cdm.base.staticdata.party.Party",
  "serialisedChars": 1234,
  "treeStamp": "a1b2c3d4e5f60718",
  "inputs": "synthetic (deterministic builder population)",
  "value": { "...": "the object" }
}
```

`live` is **not** a benchmark: no warmup, tiny op counts. Its receipt says so.

---

## 6. The serialisation decision (and why it went the way it did)

The generated POJOs carry **no Jackson annotations at all** - they carry `@RuneAttribute` /
`@RosettaAttribute` on the implementation class's getters. Something has to bridge that.

**`com.regnosys:rosetta-common` does have the canonical mapper.** Verified, not assumed:
`com/regnosys/rosetta/common/serialisation/RosettaObjectMapper.class` is present in the user's
local repository, and its static factory is `getNewRosettaObjectMapper()`.

**It is nonetheless not a default dependency, because it would destroy leg attribution.**
`rosetta-common:11.124.2` depends on `org.finos.rune:rune-runtime` *and*
`com.regnosys.rosetta:rune-maven-plugin` at its own pinned `rune.dsl.version` (9.85.1). Adding
it puts a **third** rune-runtime on the classpath beside the leg's, in the same packages,
resolved first-wins - which is precisely the confusion the two leg profiles exist to prevent.

So the **default is mode 2**: a plain Jackson mapper built from the **leg's own pinned
jackson**, taught the runtime's annotations by `RuneAnnotationIntrospector`, so property names
come out as the real Rune names (`@data`, `@ref`, `@key`, `@type`) rather than bean names. Zero
extra dependencies; the leg's dependency set stays exactly what the profile pinned. Since
jackson is itself one of the differences under measurement, this is also the honest choice.

Date/time scalars are serialised via their ISO `toString()` by a tiny in-module module, rather
than by pulling `jackson-datatype-jsr310` - again, to keep each leg's classpath exactly what
its profile declares.

**Mode 1 is available** for an integrator who wants canonical JSON and accepts the caveat:

```sh
mvn -f demo/harness/exec/pom.xml -P plus-rt,rosetta-common-serde package \
    -Dmaven.repo.local=demo/work/.m2
```

The profile excludes `rune-runtime` and `rune-maven-plugin` from rosetta-common. Detection is
**reflective**, so nothing recompiles to switch modes: if the class is on the classpath it
wins. Force either way with `-Ddemo.exec.serialiser=jackson|rosetta-common|auto` (default
`auto`; `rosetta-common` fails loudly when the class is absent, rather than quietly falling
back).

**Which mode ran is always reported** - as a `##DEMO##` metric, in the census, in the receipt
notes and in `live-sample.json`. Comparing two legs across *different* modes would be
meaningless, so that is the first thing to check when reading a legacy-vs-plus serialise delta.

**"Round trip" means `value -> String -> JsonNode`** - identical in both modes and on both legs.
It deliberately stops short of rebuilding the model object: model-object *deserialisation*
needs builder-aware deserialisers that only the canonical mapper has, which would make a full
round trip available on one mode only and the two legs incomparable the moment the modes
differed.

---

## 7. JMH

`report` and `validate` have JMH benchmarks - `Fork(2)`, warmup 3 x 2s, measurement 5 x 2s,
`AverageTime` in ms, `@Threads(1)`. They require a completed `setup` and will **not** compile
inside a trial (that would charge javac to the first iteration).

```sh
java -cp $PLUS org.openjdk.jmh.Main demo.harness.exec.jmh.ReportEvaluationBenchmark -prof gc
java -cp $LEGACY org.openjdk.jmh.Main demo.harness.exec.jmh.ValidationSweepBenchmark -prof gc
```

Note it is `-cp ... org.openjdk.jmh.Main`, not `-jar`: the manifest main class is the verb CLI.

**The leg is the jar, not a flag.** `-Dexec.leg=<leg>` is optional and acts as an *assertion* -
if present and disagreeing with the jar's pins, the trial fails rather than producing a
mislabelled score. Defaulting to the jar's own leg also side-steps the forked-JVM
property-propagation trap: a `@Fork(2)` child that did not inherit the property still measures
the right thing.

`-p limit=500` takes a deterministic stratified subset for a shorter trial; the census then
records **both** figures, so a subset can never be mistaken for the whole.

JMH receipts add `"jmh":{"forks":2,"warmupIt":3,"measureIt":5,"scoreError":x}` per CONTRACTS
section 4 - the wrapper assembles that from JMH's own output.

---

## 8. Reading the numbers honestly

Stated plainly, because a demo number without its caveats is not a measurement:

1. **Inputs are synthetic.** Deterministic builder population, not real trade samples. Every
   receipt says so. They are built **outside** the measured loop.
2. **Reflection is inside the measurement**, identically on both legs. The number compares two
   runtimes doing the same reflective work; it is not the cost of hand-written calling code.
3. **`run` is wall-clock loop timing** from a single warm JVM. The JMH benchmarks are the
   statistically defensible half. Both are reported; neither is disguised as the other.
4. **Probe failures are expected and counted.** Synthetic inputs cannot satisfy every condition
   in a regulatory rule tree, so functions whose conditions throw drop out of the measured set.
   **Compare the survivor censuses across legs before comparing any timing** - different
   survivor sets are different work, and a timing delta that is really a census delta is the
   easiest way to mislead yourself here.
5. **Every census reconciles.** `validate`'s drop-out counters sum with `usablePairs` to
   `considered`; `populate` separates top-level counts from the populator's own all-levels
   counters, which recurse into nested objects and do *not* reconcile against the top-level
   columns. They are named apart (`...TopLevel` vs `...AllLevels`) so they cannot be read as
   one split.
6. **A failed op is not a timing sample.** It increments `errors`; `requestedOps` and `ops`
   together account for every attempt.
7. **The checksum exists to defeat dead-code elimination.** The plain `run` loop has no
   Blackhole, so `runOnce()` returns a value derived from the work and the caller consumes it.

---

## 9. Layout

```
demo/harness/exec/
  pom.xml                          two leg profiles + optional rosetta-common-serde, shade
  README.md                        this file
  src/main/resources/
    demo-exec-leg.properties       FILTERED - the active profile's pins
  src/main/java/demo/harness/exec/
    Main.java                      verbs: setup | run | live; arg parsing; receipts
    Demo.java                      ##DEMO## protocol (CONTRACTS section 3)
    Receipt.java                   CONTRACTS section 2 receipt writer
    LegPins.java                   reads the filtered pins; enforces --leg
    DemoPaths.java                 every path, from one anchor
    Timing.java                    mean / p95 / alloc-per-op
    corpus/
      CorpusClasses.java           in-JVM javac + content-stamped marker  (adapted)
      GenTree.java                 which tree ran, and its stamp          (adapted)
      ReflectivePopulator.java     deterministic builder population       (adapted)
      ModelClosure.java            loader + default Guice injector + class census
    serde/
      RuneJson.java                serialiser mode detection + fallback mapper
      RuneAnnotationIntrospector.java  @RuneAttribute-aware Jackson naming
    workload/
      Workload.java  Workloads.java
      PopulateWorkload.java  SerialiseWorkload.java  ValidateWorkload.java
      FunctionWorkload.java        report + projection (same machinery, different filter)
    jmh/
      JmhSupport.java  ReportEvaluationBenchmark.java  ValidationSweepBenchmark.java
```

Adapted from `rune-benchmarks/src/main/java/org/finos/rune/benchmarks/` (`corpus/CorpusClasses`,
`corpus/BenchTree`, `corpus/ReflectivePopulator`, `DrrRuleEvaluationBenchmark`,
`CdmValidationSweepBenchmark`), with the corpus root parameterised to the demo tree and the
class output split per leg.

Nothing in this lane writes outside `demo/work/` (plus the `--receipt` path the integrator
names) and nothing reads outside `demo/`. It never invokes maven and never parses `.rosetta`.

---

## 10. For the integrator

**Order.** Build both jars -> `setup` each leg -> `run` / `live` / JMH.

**Recommended smoke test** before wiring the dashboard, cheapest first:

```sh
java -jar $PLUS setup --leg plus-rt
java -jar $PLUS run --leg plus-rt --workload populate  --ops 5 --warmup 2 --limit 50
java -jar $PLUS run --leg plus-rt --workload serialise --ops 5 --warmup 2 --limit 50
java -jar $PLUS live --leg plus-rt --ops 2 --limit 10
```

Check the census lines look sane (non-zero survivors, drop-out reasons that reconcile) before
committing to full-population runs.

**Things worth watching, flagged rather than hidden:**

- `report` / `projection` setup Guice-instantiates every candidate. `projection` has ~669
  candidates in the DRR closure, each with a large `@Inject` graph, so setup is slow; use
  `--limit` for interactive runs and 0 for the receipt run.
- The full `validate` sweep is large. Time one op with a `--limit` first and scale from there.
- `guice` **must** stay 6.0.0. `ValidatorFactory.Default` uses `javax.inject.@Inject` field
  injection, which Guice 7.x no longer honours - the field would stay null and the factory
  would NPE. The pin is load-bearing, not cosmetic.
- If `setup` OOMs, raise `-Xmx`; if it fails to compile, read
  `demo/work/exec-classes/<leg>/compile-errors.txt` before re-running.
