# Chaos gate: adversarial compiler regression checks

**Phase 3.2 delivered the gate. It remains regression coverage during active phase 3.3.** It exercises authored language combinations and placements beyond the patterns found in production models, then compares the results with independent upstream expectations.

The corpus is generated deterministically from versioned seeds. A failing case has a stable identity and reproducible bytes. The checks run as part of defined validation runs; this is not a background process continually inventing random programs.

## What is varied

| Dimension | Examples and purpose |
|---|---|
| Declaration order | Reorder declarations to expose accidental dependence on traversal order. |
| Imports and namespaces | Qualified, wildcard, aliased and unused imports; split related declarations across namespaces/files. |
| File placement and load order | Give the same source an early- or late-sorting filename; test conflicting output-package shapes. |
| Naming collisions | Type, choice, enum, metadata and function names that compete across namespaces or for one Java output path. |
| Language combinations | Nested expressions, alias/type relationships, inheritance, metadata and cardinality combinations. |
| Source representation | Missing/versioned declarations, line endings, byte-order marks, non-ASCII text, tabs and comments. |

The documented `chaos-1.1.0` cell contains **988 Rune source files from 44 seed families**, with nine declared expansion axes applied to their specified families. It is the separate authored cell alongside the 25 pinned real-model releases. The catalogue selects exactly one active chaos version. These are case-population figures, not a claim to cover every possible Rune program or grammar alternative.

## Four checks and their references

| Check | What it establishes | Main implementation/data |
|---|---|---|
| 1. Upstream acceptance | Released Rune 9.83 diagnoses each case as expected. Deliberately invalid inputs and upstream generator failures are identified, rather than silently removed. | `scripts/xtext-oracle/`, `scripts/chaos-expander/l1-gate.py`, `expectations/oracle-diagnostics.tsv`, warning and individually judged-case records |
| 2. Name binding | The replacement resolves the declared reference kinds to the same targets as the pinned upstream dump, except for explicitly registered differences. | `ChaosResolutionConformanceTest`, `chaos-1.1.0-upstream-9.83.0.tsv`, `chaos-l2-expected-differences.txt` |
| 3. Generated Java | Compare Modes 1 and 2 against independently generated upstream files and their declared exception sets. | `D11CorpusRegressionTest`, `chaos-expected-divergence.txt`, `chaos-golden-free.txt` |
| 4. Refusal accounting | Expected parser/generator refusals are named and checked; unsupported cases must not silently produce unaccounted-for output. Controls exercise the refusal sites. | `ChaosForkDiagnosticsGateTest`, the generator's `SilentDegradation` register and its witness tests |

The upstream oracle and Java goldens are independent references; the replacement implementation must not generate its own expected answers. Routine regressions use the pinned references. Creating or changing a corpus version requires renewed upstream admission and independent golden generation, including reproducibility checks.

The binding comparison has an explicit reference-kind scope; it is not a claim to compare every semantic fact. `ChaosCoverageCensusTest` separately compares parser-rule node kinds in the expanded chaos corpus and the real corpora. That census does not distinguish every token alternative or prove exhaustive combination coverage.

## What makes a gate fail

- A new error, unexpected refusal, changed binding, output difference or missing output outside the declared expectations.
- A stale row in the registers that assert exact expected sets, including binding differences, parser refusals and golden-free accounting. A fix must update its evidence and exception row together.
- A changed source population, unexpected coverage change or drift between the expanded cases and committed fixtures.
- A failure control that no longer demonstrates the relevant check can fail.

A recorded refusal and a byte difference are different outcomes. Compilation evidence is recorded for divergent outputs where required; an equality comparison alone does not prove behaviour. The historical 3.2 milestone closed with named remaining differences/refusals, not universal byte equality. Current outcomes must be read from the selected revision's runs and registers; historical counts must not be presented as newly measured.

**Byte-waiver staleness is a separate review step.** D11 currently tolerates a waived file that becomes identical. With `-Dd11.dump-now-matching=true` it prints `FLIP-OUT` candidates, but this diagnostic does not automatically fail the test or remove stale waivers. Review those candidates and reconcile the waiver/evidence explicitly. The focused commands below enable that diagnostic; they do not turn it into an assertion.

## Review and reproduce the checks

Use the [build prerequisites](BUILDING.md), Python 3 and the pinned Rune 9.83 builtin sources from [Corpus setup](CORPUS-9.83.md). The authored chaos fixtures, seed/axis definitions, oracle records, expected outcomes and required tests are in this repository; the real CDM/DRR corpora remain reviewer-supplied. The commands below were executed from the assembled export for the current drop; results are in [Source snapshot § Executed checks](SOURCE-SNAPSHOT.md#executed-checks-and-results).

The current parser tests look for those same builtins in two locations: the binding test uses `test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model`, while the fork-diagnostics test's shared helper uses `rune-dsl/rune-runtime/src/main/resources/model`. After acquiring the pinned builtin clone through the corpus guide, supply the second location as a detached worktree of that same public commit. From the review repository root, with `rune-dsl` initially absent:

```sh
git -C test-corpus/rune-dsl-builtins worktree add --detach ../../rune-dsl 5fb8d697911d2ca62532d1801265b67752d03ef9
git -C rune-dsl rev-parse --show-toplevel
git -C rune-dsl rev-parse HEAD
```

Require the reported root to be this review checkout's `rune-dsl` directory and the SHA to match the command. If that path already exists, inspect it instead of replacing it. This is a reviewer-local builtin-source worktree, not content to commit or a build of the original compiler. It accounts explicitly for the current harness path inconsistency; neither test should be reported as passing with its required builtins absent.

From the repository root, check that the expander reproduces the committed chaos source bytes:

```sh
python3 scripts/chaos-expander/expand.py --check-against test-corpus/chaos/chaos-1.1.0/rosetta-source/src/main/rosetta
```

This comparison does not replace committed sources or goldens. Require exit zero and the expected nonempty file population. Read the active version from `test-corpus/corpus-cells.tsv`; if a later reviewed corpus version is selected, use that version's inputs and expectations together.

Run the focused parser/refusal and binding checks:

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-parser/pom.xml test "-Dtest=ChaosForkDiagnosticsGateTest,ChaosResolutionConformanceTest" -Dcorpus.required=true -Dpr-speed.forkCount=1
```

Then compare Java generation on the two parity routes, after installing the providers:

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Dtest=D11CorpusRegressionTest -Dd11.corpus=chaos -Dd11.version=1.1.0 -Dcorpus.required=true -Dpr-speed.forkCount=1 -Dd11.dump-now-matching=true -Drosetta.generator.ir=false -Drosetta.generator.ir.optimised=false
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Pir-on -Dtest=D11CorpusRegressionTest -Dd11.corpus=chaos -Dd11.version=1.1.0 -Dcorpus.required=true -Dpr-speed.forkCount=1 -Dd11.dump-now-matching=true -Drosetta.generator.ir.optimised=false
```

Inspect the named classes' Surefire reports, source/file populations, mode accounting, declared differences and refusals. Missing inputs can still cause assumptions/skips in some tests; Maven success without the intended executed population is not acceptance. Keep the full logs and source/corpus pins.

These commands are a focused regression subset: they reuse pinned upstream admission and do not rerun every refusal witness, the full real-corpus coverage census, compile classification or optimised execution. The complete validation run must retain those applicable checks from [Testing](TESTING.md). Mode 3's compilation and paired-execution checks are separate from the Mode 1/2 byte gate.

For the coverage census, materialise the expander's default scratch output with `python3 scripts/chaos-expander/expand.py` (`python` on Windows), then run `ChaosCoverageCensusTest` only with its full real-corpus comparison inputs available. Do not regenerate expected goldens or diagnostic registers to make a regression pass. `pin-chaos-goldens.sh` is a baseline-maintenance tool, not an ordinary review test.

## Where it fits in CI

The development validation process reruns chaos regression checks alongside the real-model checks during phase 3.3. A scoped local command proves only its declared subset. This repository has no hosted CI; the chaos checks are part of the local validation chain of each drop, and a green badge is not evidence. See [CI](CI.md) and the [validation record](SOURCE-SNAPSHOT.md) for the export status.
