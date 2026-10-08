# CI process

This review repository ships **no hosted CI workflow**. The development repository's workflows are manual-dispatch only, clone the private model corpora with a repository secret and lint that repository's own workflow set; none of them can run from this repository's inputs and ordinary credentials, so none is included. A green badge would be a claim without a run.

Validation of a review drop is therefore a **local, serial chain** run by the person preparing the drop, with the results recorded in [Source snapshot](SOURCE-SNAPSHOT.md) and [Changelog](../CHANGELOG.md). Reviewers reproduce the same commands.

## Tiers

| Tier | Runs where | Inputs | What it establishes |
|---|---|---|---|
| Bootstrap and small checks | Local, any clone | Repository only | The modules install in order; the seam and parser/generator entry tests pass ([Building](BUILDING.md)) |
| Small three-mode example | Local, any clone | Repository only | The Maven plugin generates, the generated Java compiles and runs on each replacement route ([Building § Run the small three-mode example](BUILDING.md#run-the-small-three-mode-example)) |
| Public one-cell walkthrough | Local, needs network | Public CDM 5.38.0 and Rune 9.83.0 clones | Independent upstream reference regenerated; both compatibility routes compared on one real model ([Corpus](CORPUS-9.83.md)) |
| Chaos gate | Local, needs the public builtin clone | Committed chaos cell | Adversarial regression on parsing, binding and generation ([Chaos gate](CHAOS-GATE.md)) |
| Full compatibility matrix | Local, needs authorised corpora | All 25 real-model cells and their dependency closures | The 25-release byte comparison on both routes ([Testing](TESTING.md)); **not** reproducible from public inputs alone |
| Optimised execution | Local, needs authorised corpora | Function-bearing cells and generated overlays | Paired execution of reference and optimised Java ([Testing](TESTING.md)) |
| Integrity checks | Local, any clone | Repository only | Structural lints and register/manifest checks ([Testing § Integrity checks](TESTING.md#integrity-checks)) |

Required JDK and Maven: see [Building](BUILDING.md). Python 3 is needed for the corpus verifier, the chaos expander check and the byte-diff catalogue tools (`check-jsonschema` for the catalogue validator); a POSIX Bash for the shell lints (Git Bash on Windows, see [Testing § Integrity checks](TESTING.md#integrity-checks)).

## Reports

Maven test reports are under each module's `target/surefire-reports/`. The corpus verifier prints JSON. The compile gate writes `rune-java-generator/target/compile-gate/<cell>/compile-gate.json`. The tree comparison script writes a JSON receipt under the directory you name. Keep the full logs of a drop's validation with its record; a pipe that shows only the tail of a log must not hide a failed process.

## Failure handling

A failed required step is a blocker for the drop, reported with its cause. Reference goldens, manifests and exception registers are never regenerated to make a run pass; a justified baseline change is a separate, reviewed change ([Testing](TESTING.md)).

## Status of this drop

The commands actually executed for the current drop, their environment and results, and every tier that was **not** executed, are listed in [Source snapshot](SOURCE-SNAPSHOT.md). Hosted CI status: none.
