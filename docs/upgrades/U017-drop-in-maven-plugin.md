---
id: U017
subject: Drop-in Maven plugin (rune-maven-plugin) — the released 9.83.0 rosetta-maven-plugin generate/testGenerate surface on the fork pipeline; GAV-only pom swap proven on CDM + DRR
since_fork_version: "0.1.0"
since_pr_at_spec: 458
since_pr_actual: 458
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D42"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U017-drop-in-maven-plugin.md
---

# U017 — Drop-in Maven plugin (`rune-maven-plugin`)

## Concept

A new standalone Maven-plugin module, `org.finos.rune:rune-maven-plugin`
(packaging `maven-plugin`), that exposes the released 9.83.0
`rosetta-maven-plugin`'s build-goal surface — goal names (`generate`,
`testGenerate`), every consumer-configurable parameter name, type and
default from the released `META-INF/maven/plugin.xml`, the
`languages/outputConfigurations` XML bean shapes, the
`rosetta-config.yml` contract (`generators.namespaces` accept-list with
upstream-exact segment-wise semantics + `generators.doNotPrune`), and the
`classPathLookupFilter` library-model channel (the `rune-runtime` jar's
`basictypes.rosetta`/`annotations.rosetta`) — executed entirely on the
fork's own pipeline: `AstBuilder` parse → `RWorkspace.build` link +
validate → the diagnostic stream in the released plugin's issue-line
format → the eleven D11-proven generator passes → tree write. This is
Leg P of the drop-in parity program: the swap into a real CDM or DRR
build is a plugin-GAV change in the consumer pom; the whole execution
`<configuration>` block carries over unchanged.

## Why

The drop-in mission ("swap out rune dsl in a CDM or DRR project and
execute all functionality including validation") needs the fork invocable
as the build's codegen entry point, not only as a library. The Leg S
recon (PR #439/#440) proved the swap by pre-placing fork output and
neutralising the released plugin's execution; this module replaces both
mechanical moves with the literal pom swap the program charter names.
The mojo interface was measured at both consumer seams first
(`docs/audits/2026-07-19-leg-s-swap-recon-ledger.md` § Leg P) and the
implementation is proven against the released plugin's own banked output.

## Structural changes

New module `rune-maven-plugin/` (no existing module touched by the
module itself): `RuneGenerateMojo` + `RuneTestGenerateMojo` (thin goal
classes over `AbstractRuneGeneratorMojo`, which declares the full
released parameter surface), `RunePluginRunner` (the Maven-free pipeline
core), `Language`/`OutputConfiguration`/`ClusteringConfig` (XML-mappable
stand-ins for the upstream bean shapes), `NamespaceFilter` (arm-for-arm
port of the upstream `generators.namespaces` semantics),
`RosettaConfigFile` (snakeyaml reader), `IssueFormatter` (the released
issue-line payload: `SEVERITY:message (file:/… line : N column : M)`).
The released plugin's `format` goal is deliberately NOT exposed: the fork
has no formatter surface, the goal is an opt-in profile in every measured
consumer (never part of the default build the swap targets), and an
honest "goal does not exist" beats a stub.

## BC Story

The plugin is a NEW artifact — no fork API changes. The generated-tree
contract is measured by the mojo-seam oracle (PR #459's law): the
FULL-TREE byte diff against the frozen corpus goldens (CRLF-normalized —
goldens CRLF, plugin LF) plus the consumer `mvn compile` outcome — never
`git status` (vacuous: the consumer repos do not commit
`src/generated/java`). Post-#460 state: DRR 6,675/6,675 byte-perfect;
CDM `mvn compile` GREEN with EXACTLY three expected divergences —
`ReferenceWithMetaVoid.java` golden-only (the PERMANENT cdm6 waiver) and
the two `package-info.java` description-ORDER artifacts
(`cdm/observable/asset` + `cdm/product/asset/floatingrate`): content-
identical fragments in a different order, javac-neutral. The order class
is upstream-NONDETERMINISTIC — the released stack collects source URIs
into a hash-ordered set (`PathTraverser.findAllResourceUris`), so the
fragment order depends on absolute paths; the #460 released-plugin probe
on the recon layout reproduced ONE of the two goldens and a THIRD
distinct order for the other (receipts:
`target-460-released-plugin-probe.log` + the banked
`target-460-released-*-pkginfo.txt` files). The fork plugin's sorted
source walk is deterministic and layout-independent by design — the
divergence is recorded, not reproduced. Consumer compatibility is to the
RELEASED plugin's configuration surface: a pom written for
`com.regnosys.rosetta:rosetta-maven-plugin:9.83.0` parses and runs
against this plugin with only the GAV line changed (the plugin-level
`<dependencies>` block that feeds the upstream plugin its generator
implementations is dropped — the fork plugin carries its own). Non-Java
language profiles keep the upstream plugin (the fork rejects non-Java
`setup` values with a clear error instead of silently generating
nothing).

## How to use

In the consumer's `rosetta-source/pom.xml` plugin block, replace the GAV:

```xml
<plugin>
    <groupId>org.finos.rune</groupId>
    <artifactId>rune-maven-plugin</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <executions>… the existing execution configuration, unchanged …</executions>
</plugin>
```

(`0.0.1-SNAPSHOT` is the development placeholder coordinate the swap
receipts were measured at; releases stamp versions at tag time — from
v1.0.0 use the release version, per
`docs/releases/v1.0.0.md`.)

`mvn generate-sources` then parses the configured `sourceRoots` (+ the
`rune-runtime` jar's builtin models via `classPathLookupFilter`), prints
the validation stream in the released line format, fails the build on
validation ERRORs (`failOnValidationError`, default true), and writes the
generated tree under `outputConfigurations/outputDirectory`, namespace-
filtered per `rosetta-config.yml`.

## Migration

None — additive artifact. Existing fork consumers are unaffected;
upstream-plugin consumers opt in with the GAV swap above.

## Test coverage

`rune-maven-plugin` unit locks (18): `NamespaceFilterTest` (6 —
upstream-exact generic/specific/empty/segment-boundary semantics),
`RosettaConfigFileTest` (5 — the measured consumer YAML shapes +
doNotPrune normalization + the YAML global-tag rejection pin
[SafeConstructor: the config is pure data, tags never instantiate
types]), `IssueFormatterTest` (2 — the payload
byte-locked against a banked V0 oracle line + the EMF file-URI form),
`RunePluginRunnerTest` (5 — end-to-end on a mini corpus with the REAL
builtin models zipped into a temp jar through the classPathLookupFilter
channel: clean generation, the upstream-format warning line, the
validation-error build failure, namespace emission scoping, the
sibling-prefix source-scope boundary negative). The live
proof is the swap re-proof pair recorded in
`docs/internal/CHANGELOG.md` § PR #458 — both corpora byte-identical on
trees AND diagnostic streams.

## Coverage

CDM 6.20.6 + DRR 6.34.1 rosetta-source builds (the two mission corpora),
measured at the #459/#460 seam oracle (full-tree bytes vs the corpus
goldens, CRLF-normalized + the compile outcome): CDM `mvn compile` GREEN
from an EMPTIED generated tree — 5,902 files, the 801-line warning
stream byte-identical to the banked V0 oracle (columns included), and
the tree diff `golden 5903 | live 5902 | golden-only 1 | differing 2` =
exactly the recorded expected set (RWMVoid waiver + the two
package-info order artifacts — see BC Story); DRR `mvn -B test` baseline-identical
through plugin + fork runtime — tree 6,675/6,675 byte-perfect, stream
3,786/3,786, suite figures identical to the #440 released-stack baseline
(the reactor ends on the same 15 `SynonymIngestExpectationTest`
failures as that baseline — upstream's own Windows path-separator
artifact, attribution tuple-identical). Receipts: `target-460-cdm-compile2.log` +
`target-460-plugin-tree-diff{,-drr}.txt` +
`target-460-drr-combined1.log`.

iso20022 1.38.0 + rune-fpml 2.0.0 (the remaining two corpora), measured
at the #461 seam with a released-stack control pair (both cells are
self-contained single-root builds; fpml 2.0.0's cdm-payload copy step
is vestigial — no unpack execution exists and the model has zero
`import cdm`): control and swap both `mvn -B test` GREEN from pristine
trees; trees byte-perfect vs the corpus goldens under BOTH stacks
(iso 8,597/8,597 · fpml 9,561/9,561 — zero missing/extra/differing,
CRLF-normalized; no package-info order artifacts on either corpus,
consistent with the #413 cdm-only record); per-class suite tuples
identical control-vs-swap (iso 74/0/0/0 across 5 classes; fpml
818/0/0/0 across 2 classes); dependency:list receipts show the fork
runtime replacing the released one (iso via the direct-dep form; fpml
via the root `dependencyManagement` version — the module-level-only
override left the tests module on the released 9.83.0 through the
management pin, the swap1/swap2 receipt pair records the lesson).
The swap forms differ by consumer era: iso20022 uses the
`com.regnosys.rosetta:rosetta-maven-plugin` GAV → the #458 literal GAV
swap; **rune-fpml 2.0.0 already consumes upstream's RENAMED
`org.finos.rune:rune-maven-plugin` GAV at 9.83.0 — the exact GA this
module ships under — so post-rename consumers swap by VERSION alone**
(plus the plugin-dependencies block drop both eras). One cosmetic log
delta: on a consumer with no `rosettaConfig` the released plugin logs
"No configuration file was found. Falling back to the default
configuration." (6 lines at fpml); the fork plugin falls back silently
— model-diagnostic streams are 0 ≡ 0 on both corpora. Receipts:
`target-461-{iso,fpml}-{control1,swap1,swap2}.log` (fpml swap1 = the
management-pin lesson round; iso has no swap2) +
`target-461-{iso,fpml}-tree-diff{-control,}.txt`. With this pair the
drop-in claim is measured on ALL FOUR corpora of the matrix.

## Cross-references

- `docs/audits/2026-07-19-leg-s-swap-recon-ledger.md` § "Leg P sizing" +
  § "Leg P additions" — the measured mojo interface this module implements.
- `docs/internal/CHANGELOG.md` § PR #458 — the swap re-proof record and
  the two seam-measured heal waves (issue anchors + the eq rows) the live
  proof exposed.
- Memory `project_validation_parity_go.md` — the drop-in parity program
  charter (Leg P).
