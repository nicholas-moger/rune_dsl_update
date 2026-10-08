# Testing and interpreting results

The tests answer different questions. Keep their evidence separate: accepted syntax, diagnostics, generated-file equality, Java compilation, program behaviour, performance and native IR ownership are not interchangeable.

## Test layers

| Layer | What it checks | Boundary |
|---|---|---|
| Parser and semantic fixtures | Grammar, AST shape, linking, type/cardinality analysis and diagnostics | Local examples do not establish all model releases or editor behaviour |
| Reference comparisons | Selected upstream acceptance/diagnostic behaviour | Oracle/compiler version, input population and permitted differences must be recorded |
| D11 generated-file regression | Actual outputs against independent upstream goldens, by model/version/file kind | Named mismatches, missing output and no-golden cases stay visible |
| Generated Java compilation | Whether generated sources compile against the intended runtime/dependency closure | Compilation alone does not establish runtime semantics |
| Paired execution | Values, errors and validation behaviour of reference versus changed implementations | Only executed cases and declared input populations are covered |
| IR reconciliation and writer accounting | Source facts agree with IR; actual file ownership, refusals and fallback remain explicit | Shadow rendering and provider selection do not equal active native emission |
| Adversarial corpus | Unusual placements/combinations, collisions, nested expressions and inheritance | Separate from real releases; declared gaps remain |
| Benchmarks/context experiments | Precisely defined time, allocation or context-size measurements | No extrapolation to all applications or AI answer quality |

The API/ABI comparison is report-only and uses an upstream parser baseline of 9.58.1, not 9.83.0. The downstream DRR build is advisory. Neither should be described as a blocking, complete 9.83 compatibility gate.

## First review run

Use [Building](BUILDING.md) for corpus-free checks and [CORPUS-9.83.md](CORPUS-9.83.md) for one public real-model cell. Expand to the catalogue only when the required source, golden and dependency inputs are available. Do not assume the full internal model collection is public.

The full D11 compatibility commands, **after provisioning the complete corpus**, are:

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Dtest=D11CorpusRegressionTest -Dcorpus.required=true -Dpr-speed.forkCount=1
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Pir-on -Dtest=D11CorpusRegressionTest -Dcorpus.required=true -Dpr-speed.forkCount=1
```

Install the corresponding IR provider prerequisites first. These runs include the active catalogue and authored chaos cell according to the committed catalogue; they are not quick smoke checks. Record every visited cell/kind and actual skipped populations. `corpus.required` is a guard used by parts of the harness, not a universal override of every JUnit assumption. A deliberately scoped run cannot be reported as full-matrix evidence.

The standard generator suite excludes the heavy `compile-gate` tag. `D11CompileGateTest` uses independently generated golden classes and per-cell upstream classpaths prepared by `scripts/compile-gate/resolve-classpaths.sh`. Its `cdm5` method covers CDM 5.38.0. Merely enabling `-Drune.excludedGroups=none` without those inputs is not a complete setup. This gate principally classifies divergent files against the golden closure, rather than proving every mode's behaviour.

Despite its name, this test is a **diagnostic report**: it does not assert all reported compilation outcomes. Read `closureClean`, generation failures and noncompiling results in `rune-java-generator/target/compile-gate/cdm-5.38.0/compile-gate.json` and require the agreed clean/exception outcome explicitly. Its directly constructed generators exercise the existing compatibility writer. Maven exit zero alone does not establish compilation acceptance or all-mode coverage.

`OptimisedNavigationEmissionTest` and `OptimisedNavigationPairGateTest` provide deeper optimised emission/execution checks. The paired gate expects generated overlays and model dependencies; its function-bearing cell set is not narrowed by D11's `d11.corpus`/`d11.version` properties. `optnav.required` does not turn an absent source corpus into a populated run. Do not advertise these full-matrix tests as an already supported one-cell public walkthrough.

## Named registers and evidence

Inspect these files before interpreting a difference:

- `rune-java-generator/src/test/resources/d11-known-divergent.txt`: named generated-output differences and the upstream-defect waiver.
- `rune-java-generator/src/test/resources/d11-ir-declines.txt`: registered IR declines.
- The IR fallback register and its `D11IrFallbackRegisterTest`: existing-renderer ownership and expected fallback.
- `test-corpus/corpus-cells.tsv`: model/compiler versions and resolved dependency closures.
- Corpus baseline manifests and their tests: frozen source/golden identities.

A changed baseline is a reviewed decision, not a remedy for a failing comparison. Never regenerate expected goldens from the replacement implementation. Do not automatically update manifests during an ordinary review run.

## What the chaos gate means

**Phase 3.2 delivered the gate; it remains active regression coverage during phase 3.3.** The dedicated [Chaos gate guide](CHAOS-GATE.md) explains the input transformations, four checks, failure rules, independent references and focused reviewer commands.

The authored chaos corpus deliberately puts constructs outside the combinations typically exercised by production models: deep/nested operations, unusual declaration ordering, alias/type relationships, inheritance and name/path collisions. This tests assumptions in parsing, resolution and generation. It is not a guarantee that every random program is supported, nor merely a file-order shuffle.

The historical 3.2 closure accounted for remaining differences and refusals rather than achieving universal equality on that corpus. Current checks keep those classes named, compare reference behaviour and use controls that demonstrate refusals can actually fire. See [Evidence](EVIDENCE.md) for the exact historical scope.

## What counts as IR progress

There are separate milestones: IR facts exist; those facts reconcile; an emitter renders identical shadow output; its whole output unit is eligible; the host selects it; and it becomes the actual writer. Only the last applicable step counts towards strict native-file ownership. A large fact count or a ready POJO member cannot be substituted for a completed data-type unit.

The type unit contains the POJO plus its derived files. A unit-level refusal preserves the existing route for the unit. Shared wrappers and namespace files have different ownership boundaries. Count legitimate no-file decisions separately from missing output. Report any completion percentage with its exact revision, corpus/version matrix, routes, file families, denominator, exclusions and retained fallback.

## Required run record

Record source revision, corpus/compiler/dependency pins, Java/Maven/OS, exact command, cache policy, provider identity, discovered/executed/skipped populations, exit status and named exceptions. Preserve the underlying failure when a rerun is needed. A pipe that displays the end of output must not hide a failed process.

Existing development merge evidence uses a serial local validation chain with build cache disabled; hosted workflows are manual-dispatch. Historical receipts are evidence for their stated revision and environment, not proof that the final curated export or a hosted CI run passed.

## Command reference and validation order

All commands below run from the repository root after [Building](BUILDING.md). Add `-B -ntp -Dmaven.build.cache.enabled=false` to Maven commands and retain the same local-repository setting. Use one parser/generator fork when sharing a workstation. Ordinary test reports are in the named module's `target/surefire-reports/`. Which of these commands were executed on the assembled export for the current drop, with their results, is recorded in [Source snapshot § Executed checks](SOURCE-SNAPSHOT.md#executed-checks-and-results); the rest are source-inspected.

| Order / check | Command | Prerequisite, acceptance and limits |
|---|---|---|
| 1. Core suites | `mvn -f rune-parser/pom.xml test`, then the same `test` goal for `rune-java-generator`, `rune-ir`, `rune-ir-java`, `rune-ir-java-optimised`, `rune-runtime`, `rune-maven-plugin` | Build dependencies first. Tests assert their contracts; record all skips and required external fixtures. A public one-cell checkout is insufficient for full-suite corpus claims, and several suites **fail by design** in that layout; see [Core suites in a partial corpus](#core-suites-in-a-partial-corpus). |
| 2. Parser reference fixtures | `mvn -f rune-parser/pom.xml test "-Dtest=CorpusParseTest,StructuralComparisonTest,BackCompatCorpusOrthogonalityTest,AstCorpusRegressionTest,CorpusDiagnosticGateTest"` | Corpus/builtin/dependency roots and tracked structural/diagnostic references. Corpus-absent assumptions can skip checks; not a live oracle run. These tests do not all honour D11's scope flags. |
| 3. Property sample | `mvn -f rune-parser/pom.xml test "-Dtest=SubtypePropertyTest,ResolutionPropertyTest,SourceRangePropertyTest,SymbolIdPropertyTest"` | Record jqwik tries/seeds and any failures. This named sample is not all property/fuzz coverage. Included suites need not be repeated if step 1 already produced equivalent evidence. |
| 4. Held-out checks | `mvn -f rune-java-generator/pom.xml test "-Dtest=HoldOutGenerationTest,HoldOutByteCompareTest,HoldOutCompileGateTest"` | Retain `holdout`/`holdout-goldens` test resources, 9.83 builtins, system javac and released runtime jar. Inspect `target/holdout-compile-gate/`, Surefire and named compile exceptions. Missing prerequisites can skip. The compiler helper has the default-cache limitation described below. |
| 5. Compatibility matrix | Both D11 commands above, or the explicitly scoped public recipe | Independent source/golden inputs, installed provider and full resolved dependency closure. Require complete intended cell/kind population and exact registered differences/declines/fallback. |
| 6. CDM compilation report | `bash scripts/compile-gate/resolve-classpaths.sh`, then `mvn -f rune-java-generator/pom.xml test "-Dtest=D11CompileGateTest#cdm5" -Drune.excludedGroups=none` | Bash and independent CDM goldens. Resolver scans five fixed cells and skips absent POMs. **Report-only compilation outcomes** require explicit evaluation as described above. Internal Maven calls need consistent cache configuration. |
| 7. Downstream parser smoke | `mvn -f rune-parser-consumer-smoke/pom.xml test` | Installed parser; three local consumer tests, no external corpus requirement. Checks the parser-facing Java API, not all generated-model APIs. |
| 8. Plugin integration test | `mvn -f rune-maven-plugin/pom.xml test -Dtest=RunePluginRunnerTest` | Installed parser/generator and `test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model`. Skips when builtins are absent. Tests the runner; an actual consumer Maven invocation and alternate-mode dispatch remain separate. |
| 9. API/ABI report | `mvn -f rune-japicmp/pom.xml verify` | Packaged parser. **Report-only**, against 9.58.1. Read `rune-japicmp/target/japicmp/api-compat-gate.xml` and companions. Optional layer 3 needs a separately supplied historical generator jar; a squash repository cannot reconstruct it from local history. |
| 10. Optimised execution | `mvn -f rune-ir-java-optimised/pom.xml test -Dtest=OptimisedNavigationEmissionTest`, then `mvn -f rune-equivalence/pom.xml test -Dtest=OptimisedNavigationPairGateTest -Doptnav.required=true` | Install runtime and benchmarks first. All function-bearing cells, goldens, transitive dependencies and emitted overlays under `rune-ir-java-optimised/target/optimised-tree/` are required. Read census, named cases and Surefire; absent corpus can still skip. |
| 11. Downstream application | Build an authorised DRR checkout with the replacement plugin per [Consumer POM integration](MAVEN-CONSUMERS.md) and run its own module tests | **Advisory**; private access and dependencies required. The development repository's community-build scripts are not included because they need a private registry credential. Inspect the effective dependency tree to prove replacement selection. |
| 12. Integrity checks | The standalone checks listed under [Integrity checks](#integrity-checks) below | Repository only. Each check exits non-zero on a finding; the `.test` companions prove the check can fail. |

The order makes dependencies and evidence explicit; do not rerun an unchanged suite merely because it appears in both a focused entry and a full suite. Preserve the existing required release checks and record which run supplies each result.

### Core suites in a partial corpus

The module suites presuppose the two public acquisitions of the [chaos gate guide](CHAOS-GATE.md): the Rune 9.83.0 builtin clone at `test-corpus/rune-dsl-builtins` and its detached worktree at `rune-dsl`. Without them the parser, generator and IR suites report dozens of failures that are mostly missing builtin types and builtin-dependent chaos inputs (measured on the assembled export: 10 + 1, 51 and 1 + 1, of which the corpus guards below account for 4 + 1). Acquire both before reading any suite result.

The committed chaos cell means `test-corpus/` is never empty, so the corpus guards always treat the checkout as a corpus installation and assert the committed catalogue, baseline manifest and census expectations for the whole 25-release population. They fail loudly instead of skipping silently, because a quietly narrowed population is exactly the failure they exist to catch. In any public layout the following therefore fail by design, each naming the cells or the population it could not see; the [snapshot record](SOURCE-SNAPSHOT.md) has the executed counts for both layouts.

| Module | Test | Builtins and worktree only | Plus the CDM 5.38.0 walkthrough cell |
|---|---|---|---|
| `rune-parser` | `Corpus983BaselineManifestTest.workingCorpusMatchesBaselineManifest` | fails: the working corpus differs from the frozen 25-release baseline manifest | fails |
| `rune-parser` | `CorpusCatalogueTest.activeCellsAndTheirClosuresArePresent` | fails: active catalogue cells are absent on disk | fails |
| `rune-parser` | `LambdaSwitchShapeCensusTest`, `SwitchGuardLiteralKindCensusTest` | fail: the switch censuses expect the full-corpus population | fail |
| `rune-parser` | `SymbolTableSnapshotTest.snapshots_match` | skips | fails: the CDM symbol-table snapshot was recorded over the full CDM population |
| `rune-ir` | `CorpusPythonDeclineCensusTest`, `CorpusSnakeCollisionCensusTest` | fail: the censuses refuse to trust a population smaller than the recorded one | fail |
| `rune-ir-java-optimised` | `OptimisedNavigationEmissionTest.conversionLawsHoldPerCell` | skips | fails: active catalogue cells are not staged; the gate fails rather than skipping them |
| `rune-java-generator` | `ChoiceObjectGeneratorTest.choice_pojo_emits_bytematch_against_latest_cdm_Asset` | skips | fails: reads the latest CDM cell of the catalogue, not the walkthrough cell |
| `rune-java-generator` | `D11CorpusRegressionTest.xmeta_comparison`, `D11CorpusRegressionTest.packageinfo_comparison`, `GoldenInputOrderPinTest` | skip | fail only when the regenerated reference chose a different ordering for `BusinessEventMeta.java` or a package-info. The failure stays; reading it as ordering-only needs evidence for both sides: the reference classified by the corpus verifier (or, for a package-info, the pin test) **and** the replacement's file, dumped by D11, matching a witnessed form or differing only by moved entries ([Corpus](CORPUS-9.83.md#compare-the-two-compatibility-routes); [the order-dependent files](CORPUS-9.83.md#files-that-depend-on-the-generating-machines-enumeration-order)) |
| `rune-parser` | `RRootElementCorpusOccurrenceTest` (restored with this drop; counts the real-model cells plus the two builtin models, never the builtin clone's test fixtures) | skips (no real-model cell) | fails by design on CDM 5.38.0 alone, naming the four registered kinds that cell does not declare (`RReport`, `RRule`, `RExternalRuleSource`, `RChoice`); it passes only over a population that declares every registered kind, the full catalogue |

Every other test in those suites, and the whole of `rune-ir-java`, `rune-runtime`, `rune-maven-plugin` and `rune-parser-consumer-smoke`, is expected green in both layouts. Any failure outside this table is a finding. Do not stage further cells or edit registers to turn these rows green; provision the full catalogue as [Corpus](CORPUS-9.83.md#the-broader-historical-matrix) describes, or read them as the declared partial-corpus outcome.

The held-out compiler helpers currently locate `rune-runtime-9.83.0.jar` in the user's default Maven cache, even with a different `maven.repo.local`. Check the actual jar hash and skipped counts. The `scripts/holdout-oracle/run-holdout-oracle.sh` script rewrites committed goldens and is **not** an ordinary test command.

### Integrity checks

The development repository ran an aggregate gate runner mixing semantic lints with presentation gates over its own README and charts. The presentation gates are not applicable to this repository's documents and are not included. The semantic and register checks are retained as standalone commands, run from the repository root with Bash and Python 3. Use a POSIX Bash that runs the scripts as checked out (Git Bash/MSYS on Windows, or a Linux clone); on Windows, `bash` inside PowerShell may be WSL, which rejects a CRLF checkout with `$'\r': command not found`. The `.test` companions call `git` and need the repository's own `.git`, so they run in a clone, not in an exported tree. `scripts/byte-diff/validate.sh` needs the `check-jsonschema` Python package on `PATH`, and `generate_index.py.test` is a Bash script despite its name (`bash scripts/byte-diff/generate_index.py.test`).

| Check | Command | What it guards |
|---|---|---|
| No regex over structured content | `bash scripts/ci/no-regex-on-structured-content.sh` (`.test` proves it fails on the fixtures) | The engineering rule that language content is parsed, not pattern-matched |
| Corpus verifier controls | `python3 docs/verify-public-corpus-controls.py` | The public corpus verifier's seven classifications each fire (eight controls: the two forms of an accepted ordering are separate cases), and `rawBytesIdentical` is exact byte equality |
| One writer per seam | `python3 scripts/ci/one-writer-seam-lint.py` (`.test`) | A generated file family has exactly one writer on the IR route |
| Refusal propagation | `python3 scripts/ci/refusal-propagation-lint.py` | A refusal site cannot be swallowed on its way to the generation error |
| Evidence-API ledger | `python3 scripts/ci/evidence-api-ledger.py` (`.test`) | Every evidence-producing test API call is ledgered in `scripts/ci/evidence-api-ledger.tsv` |
| Upgrade manifests | `bash scripts/ci/upgrade-manifest-validate.sh` (`.test`) | The `docs/upgrades` manifests keep the nine-section template and their index |
| Diagnostic code docs | `bash scripts/ci/diagnostic-doc-validator.sh` | Every `RuneErrorCode` has a registered, documented code page |
| Byte-diff catalogue | `bash scripts/byte-diff/validate.sh`, `python3 scripts/byte-diff/generate_index.py` (`.test`), `bash scripts/ci/d5-budget-gate.sh` (`.test`) | Catalogue entries validate against the schema, the index regenerates identically, the budget holds |

The document-contract JUnit tests (`FeaturesIndexCoverageTest`, `BCVerificationCoverageTest`, `UpgradeManifestStructureTest` in `rune-parser`; `RuneErrorCodeTest`) lock the retained engineering documents (`docs/features/INDEX.md`, `docs/bc-verification.md`, `docs/upgrades/`, `docs/diagnostics/`) to the code. Tests that locked the layout of internal audit documents were retired with those documents; the [snapshot record](SOURCE-SNAPSHOT.md) lists them.
