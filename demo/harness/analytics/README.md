# demo-harness-analytics — the fork analytics lane

`org.finos.rune.demo:demo-harness-analytics:1.0.0` · main class `demo.harness.analytics.Main` ·
Java 21 · depends on **`rune-parser` only**.

Answers the query set below (section 5 of the demo contract, a development document not included here) over the merged demo corpus and reports the §4
`analytics` metrics.

```
mvn -f demo/harness/analytics/pom.xml -Dmaven.repo.local=demo/work/.m2 package
java -cp "demo/harness/analytics/target/classes;demo/harness/analytics/target/lib/*" \
     demo.harness.analytics.Main plus-jvm \
     --src "demo/corpus/cdm-5.38.0;demo/corpus/iso20022-1.38.0;demo/corpus/drr-6.34.1" \
     --builtins demo/corpus/builtins \
     [--receipt demo/receipts/analytics.plus-jvm.json] [--command "<exact line>"]
```

---

## The `legacy-emf` lane is a different artifact

It ships in **`demo-harness-legacy`**, under `demo.harness.analytics.legacy.Main`. This is
forced, not a preference: the fork and upstream `rune-lang:9.83.0` both own the Java package
`com.regnosys.rosetta.*` with incompatible class shapes (verified: `parser`, `rosetta`, `types`,
`utils` and `validation` sub-packages exist in both `rune-parser:0.0.1-SNAPSHOT` and
`rune-lang:9.83.0`), so one module declaring both dependencies produces a classpath on which
Guice dies at graph construction — the failure recorded in `scripts/xtext-oracle/pom.xml`.

Invoking `legacy-emf` here prints the alternative command and exits `2`.

**The comparison is unaffected.** `Queries.java`, `Decl.java` and `AnalyticsRun.java` are
duplicated into the legacy module **byte-for-byte apart from their package line**, so both
lanes execute literally the same query code over their own extractor's output, and emit the
identical metric shape. A disagreement is therefore always located in an extractor
(`ForkExtractor` here, `EmfExtractor` there), never in a query.

---

## The §5 query semantics — the cross-lane contract

Defined once, in `Queries.java`. **the no-JVM Python lane (`demo/nojvm/analytics.py`) must reproduce these rules**,
which is why every one of them is answerable without a resolver.

### Population

Every `.rosetta` file loaded, **excluding the two builtins by FILE NAME**
(`basictypes.rosetta`, `annotations.rosetta`). File name, not namespace: it is the rule
upstream's own `RosettaGenerator.ignoredFiles` uses, it needs no resolver, and a grammar-only
lane can apply it. The builtins are still **loaded** — they must be, or nothing resolves — they
are simply not **counted**.

### INDEX

declaration **simple name** → (kind, namespace, file). Keyed by simple name because that is the
only key every lane can produce. Simple names are not unique across a merged corpus, so the
first entry wins and `indexCollisions` reports how often that happened rather than hiding it.

### Kinds

`TYPE` is a `type` declaration **only**. `choice` is its own kind and is **never counted as a
data type** — the fork models it as a separate AST class while upstream's EMF `Choice` extends
`Data`, so "everything that is a Data" would make the lanes disagree by construction. The
grammar has two productions and the queries follow the grammar. `FUNCTION` includes dispatch
functions. Rules split into `REPORTING_RULE` and `ELIGIBILITY_RULE`.

### Q1 — counts by kind

`types` = `TYPE`, `enums` = `ENUM`, `functions` = `FUNCTION`, `rules` = **`REPORTING_RULE`
only** (eligibility rules excluded).

### Q2 — `metaAnnotatedTypes`

`TYPE` declarations carrying **any** `[metadata ...]` annotation on the type itself or on any
of its attributes. The annotation **name** is matched (`metadata`); the qualifier (`key`,
`scheme`, `reference`, `id`, `address`, `location`, `template`) is deliberately unrestricted —
"ANY `[metadata ...]`".

Both lanes read the same thing: the fork's `RAnnotationRef.annotationName()` is `"metadata"`
with `qualifierName()` `"key"` (`AstBuilder:1888/1892`), and EMF's
`AnnotationRef.getAnnotation().getName()` is `"metadata"` with `getAttribute().getName()`
`"key"`.

### Q3 — `referencesToTarget`

**Distinct** `TYPE` declarations having at least one attribute whose declared type's **simple
name** is exactly `Party`. A type with three `Party` attributes counts once. Simple-name
matching, so `Party` and `cdm.base.staticdata.party.Party` both match and no lane needs a
resolver to agree.

### Q4 — `deepestExtendsChain`

The largest number of `extends` **edges** above any `TYPE`; a type with no super-type scores 0.
The walk follows the declared super-type's **simple name** through the index. An edge to a name
that is not an indexed `TYPE` — a choice, or a name a lane could not place — **still counts as
one edge and then ends the walk**, so the answer never depends on how completely a lane
resolved. A cycle stops the walk at the repeated name and increments `extendsCycles`.

---

## Metrics

the receipt's `analytics` keys (section 4 of the demo contract) first, then measured extras:

```
lane, files, parseMs, indexMs, q1Ms, q2Ms, q3Ms, q4Ms, startupMs, results,
peakHeapMb, peakNonHeapMb, indexSize, indexCollisions, extendsCycles,
linkMs (plus-jvm) | resolveMs (legacy-emf), declarations, wallMs, exit
```

`results` is exactly the contract shape and **must agree across all three lanes** — that is the
correctness cross-check.

- `files` counts the **non-builtin** population.
- `parseMs` is parse only. The semantic load is reported separately: `linkMs` here
  (`RWorkspace.build`), `resolveMs` there (`EcoreUtil.resolveAll`). Keeping them apart makes
  parse-vs-parse and semantics-vs-semantics both comparable.
- `startupMs` is process start → engine ready. On the legacy lane that is Guice injector
  construction plus EPackage registration. On this lane there is no injector, so the harness
  forces initialisation of `AstBuilder` and `RWorkspace` and measures to that point — a real
  measurement rather than a no-op. **The number being small is the result, not a missing
  measurement.**
- **`peakRssMb` is deliberately absent.** A JVM cannot observe its own RSS portably:
  `MemoryPoolMXBean` peaks are heap/non-heap usage (a different, always-smaller quantity) and
  `com.sun.management.OperatingSystemMXBean` exposes *system* memory, not this process's peak.
  Publishing either under that name would be fabricating a field, which the demo contract forbids.
  The two peaks that *are* measurable are reported under their true names, `peakHeapMb` and
  `peakNonHeapMb`; the outer PowerShell wrapper supplies the real `peakRssMb` from
  `PeakWorkingSet64`.

### An honest note on this lane

The four queries as specified need only source-text names — declared super-type, declared
attribute type, annotation name — so the fork could answer all of them from the **parse alone**,
with no linking at all. The workspace is built anyway and reported as `linkMs`, so that the
comparison against the legacy lane's `resolveAll` is like-for-like: both lanes do a full
semantic load before being asked anything.

---

## Protocol and exit codes

`##DEMO##` events per §3: `start`, `metric`, `progress` (every 250 parsed files), `done`,
`error`; every line flushed immediately, all output ASCII. Exit `0` success, `2` usage error,
`3` parse failures (counts would be wrong, so nothing is reported), `4` unexpected failure.
