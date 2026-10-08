---
id: U018
subject: Fork-owned runtime (rune-runtime) — the released 9.83.0 org.finos.rune:rune-runtime rebuilt from released sources on the fork toolchain; ABI-identical (227/227 class entries, 2,454/2,454 javap declaration lines), xtend dependency removed
since_fork_version: "0.1.0"
since_pr_at_spec: 459
since_pr_actual: 459
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D42"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U018-fork-owned-runtime.md
---

# U018 — Fork-owned runtime (`rune-runtime`)

## Concept

A new standalone jar module, `org.finos.rune:rune-runtime:0.0.1-SNAPSHOT`
(the development placeholder coordinate — releases stamp versions at tag
time, `1.0.0` from v1.0.0),
that rebuilds the released 9.83.0 Rune DSL Java runtime — the library every
generated tree links against (`RosettaModelObject`, the mapper/expression
spine, validators, meta, qualify, reports, the annotation surface) and the
carrier of the builtin models (`model/basictypes.rosetta`,
`model/annotations.rosetta`) that the U017 maven plugin's
`classPathLookupFilter` channel reads. Source basis: the RELEASED
`rune-runtime-9.83.0-sources.jar` from Maven Central (SHA-1
`12d271da33f8611c6fcb0dbad2823c933b540691`), taken verbatim — 146 Java files
covering 100% of the released 148 outer classes / 227 class files — with ONE
deliberate deviation: `MapperMaths` (upstream's only xtend-built class) ships
as its xtend-generated Java form with the two `org.eclipse` references
replaced by `java.lang` equivalents, removing the xtend runtime dependency
from the fork artifact. This is Leg R (R2) of the drop-in parity program: the
consumer-pom swap is a dependency-GAV edit (`com.regnosys.rosetta:
com.regnosys.rosetta.lib` → `org.finos.rune:rune-runtime`), the same literal
form as the U017 plugin swap.

## Why

The drop-in mission ("swap out rune dsl in a CDM or DRR project and execute
all functionality including validation… there is an actual runtime surface in
rune dsl as well") requires the fork to OWN every artifact on the seam. After
Legs S/V/P, the runtime was the last released artifact the mission consumed
un-owned. The R-ledger (2026-07-19) sized R2 as "the biggest un-started
block"; the PR #459 measurement (the R2 sizing audit,
`docs/audits/2026-07-23-r2-runtime-sizing.md`) collapsed it: the real
artifact behind the `com.regnosys.rosetta.lib` shim is 13.2K LOC of released
Apache-2.0 source with a published sources jar, one xtend file, four
dependency pins, and an ABI surface fully measurable by javap. Owning it
closes the BC envelope: the fork controls the bytes generated code links
against, while remaining signature-identical for every released binary
consumer (rosetta-common, cdm-java, iso20022, rosetta-testing,
ingest-test-framework — their 81-class demand union proven satisfied).

## Structural changes

- New module `rune-runtime/` (repo root): `pom.xml` (GAV
  `org.finos.rune:rune-runtime:0.0.1-SNAPSHOT`, `maven.compiler.release=8` —
  the released floor; deps = guice 6.0.0 + the released parent's
  dependencyManagement pins, with security-patch deviations since PR #490:
  commons-lang3 3.18.0 (was 3.14.0, CVE-2025-48924) and jackson-databind +
  jackson-datatype-jdk8 2.18.9 (was 2.17.1; the advisories are all databind's —
  jdk8 moves in lockstep via the shared version property; CVE-2026-54512/-54513
  HIGH et al.); NO xtend.lib).
- `src/main/java`: the 146 released source files verbatim (19 packages,
  `com.rosetta.model.lib.*` + `com.rosetta.model.metafields` +
  `com.rosetta.util.*` + `com.rosetta.lib.postprocess` +
  `com.regnosys.rosetta.lib.labelprovider`), except `MapperMaths.java`
  (de-xtended: `StringConcatenation` → `StringBuilder` on the error-message
  paths, message bytes identical; the four `@XbaseGenerated` markers on
  private dispatch methods dropped — ABI-invisible).
- `src/main/resources`: `model/annotations.rosetta`,
  `model/basictypes.rosetta`, `default-formatting-options.json` — extracted
  byte-for-byte from the released BINARY jar.
- `src/test/java`: the 6 upstream runtime test classes (1,405 LOC, from the
  upstream checkout; all compile against the released API) + the fork's
  `RuntimeSurfaceLockTest` (9 locks).
- No change to rune-parser / rune-java-generator / rune-maven-plugin code.

## BC Story

The fork jar is a binary drop-in for the released runtime: the class-file
entry set is 227/227 identical (including `MapperMaths`'s nested/synthetic
set), and the `javap -p` declared-member surface is identical in both
directions (2,454 declaration lines each; per-section compare 179/179; global
sorted line-multiset delta zero — banked `target-459-r2-abi-javap.log`).
Binary consumers compiled against the released jar (rosetta-common 11.120.2 ·
cdm-java 5.37.0 · iso20022 1.37.0 · rosetta-testing · ingest-test-framework —
the measured 81-class union, constant-pool + reflection channels) link
against the fork jar by construction of that receipt. The three resources are
byte-identical, so the U017 plugin's builtin-model channel serves identical
bytes from either jar (control-proven live at PR #459: CDM regeneration output
is byte-identical under released vs fork runtime). The deliberate deviations
are ABI-invisible: private-method annotation removal + an internal string
builder swap with byte-identical messages (locked).

## How to use

In a consumer pom, replace the runtime dependency:

```xml
<!-- released -->
<dependency>
    <groupId>com.regnosys.rosetta</groupId>
    <artifactId>com.regnosys.rosetta.lib</artifactId>
</dependency>
<!-- fork -->
<dependency>
    <groupId>org.finos.rune</groupId>
    <artifactId>rune-runtime</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

Maven version mediation resolves any transitive released
`org.finos.rune:rune-runtime:9.83.0` (via the empty `com.regnosys.rosetta.lib`
shim other dependencies still pull) to the nearest-declared fork version; the
shim jar itself carries zero classes. The U017 plugin's version-agnostic
`classPathLookupFilter` path regex (`.*org[\\/]finos[\\/]rune[\\/]
rune-runtime.*\.jar`) matches the fork jar unchanged. Build the module with
`mvn -f rune-runtime/pom.xml install`.

## Migration

None. Fork-side consumers change nothing (the module is additive); a
consumer project opts in via the dependency-GAV edit above and reverts by
restoring the released dependency line.

## Test coverage

92/92 green (`target-459-runtime-test{1,2}.log`): the 6 upstream classes (83
tests — expression NullSafe equality/operators/parameterized, MapperTest,
RosettaPathTest, BreadthFirstSearch) + `RuntimeSurfaceLockTest` (9): SHA-256
byte-pins for the three resources against the released bytes · the
no-`org/eclipse` contract pin on the compiled `MapperMaths.class` bytes (the
de-xtend must not regress) · released message-byte pins on the three
de-xtended error paths (`Cant add two random (String, BigDecimal) together` ·
`Cant subtract two strings together` · `Cant divide two strings`) · an
add-computes positive · the 19-package representative census. The ABI receipt
itself is the banked javap log (re-derivable: `javap -p` both jars, all 227
classes; the lock-test layer deliberately excludes it — it needs the released
jar in m2, which CI lacks).

## Coverage

The runtime demand measured over the full corpus baseline (the R2 sizing
audit §2): generated code (34,686 goldens, all five cells) demands 64/148
classes; handwritten consumer code 38; the binary-consumer union 81; the
grand union 89; the jar-internal bytecode closure 116; 32 classes are
mission-dead but shipped anyway (drop-in completeness — pre-compiled
third-party code may link them). The live compile gate at PR #459 exercised
the CDM seam end-to-end (fork plugin + fork runtime) and exposed a
PRE-EXISTING, runtime-independent, CDM-only 3-file plugin-composition
generation divergence (1 semantic `MapAsset.java` + 2 cosmetic package-info
order artifacts) — the #460 charter; the DRR plugin tree measured
6,675/6,675 byte-perfect in the same session. PR #460 closed the charter
(the semantic divergence healed at the generator's guard-resolution seats;
the order artifacts recorded — see U017 § BC Story) and ran the R2
behavioral endgame: the full CDM (2,014/0/0/3) and DRR (84 · 26 ·
4,190/15) consumer suites GREEN-to-baseline through the fork plugin with
THIS runtime on the classpath (`dependency:list` receipts banked in
`target-460-{cdm,drr}-combined1.log`), per-class tuples identical to the
released-stack baselines (99 + 85 classes).

## Cross-references

- The R2 sizing audit: `docs/audits/2026-07-23-r2-runtime-sizing.md` (the
  measurement this module implements; demand tables, closure, drift findings,
  live-gate record).
- U017 (`U017-drop-in-maven-plugin.md`): the plugin whose builtin channel
  this jar carries; the swap-seam counterpart.
- The Leg-S recon ledger: `docs/audits/2026-07-19-leg-s-swap-recon-ledger.md`
  §§ R-ledger (the R1 verdict + the R2 ABI-seam constraint this module
  satisfies).
- `docs/bc-verification.md` § Per-feature locks — the U018 bullet (the lock
  inventory + layer narration).
- CHANGELOG anchor: `## PR #459` (`docs/internal/CHANGELOG.md`).
