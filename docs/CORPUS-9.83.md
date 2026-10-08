# Public CDM walkthrough: Rune DSL 9.83.0

This walkthrough uses public CDM 5.38.0 and the released Rune DSL 9.83.0 baseline. It does not require access to the private model repositories used in some historical measurements. Every command below was executed from the assembled review repository on 22 September 2026; the environment, results and named exceptions are recorded in [Source snapshot](SOURCE-SNAPSHOT.md).

## Exact inputs

| Input | Pin |
|---|---|
| CDM repository | `https://github.com/finos/common-domain-model.git` |
| CDM tag / commit | `5.38.0` / `788f63af47723ca2ef2ec8d24f31c01a4ac4505f` |
| Rune builtin source repository | `https://github.com/finos/rune-dsl.git` |
| Rune tag / commit | `9.83.0` / `5fb8d697911d2ca62532d1801265b67752d03ef9` |
| CDM compiler property | `rosetta.dsl.version=9.83.0` |
| Replacement implementation | Source revision recorded in [Source snapshot](SOURCE-SNAPSHOT.md); final exported commit recorded separately |

The public tag refs were resolved during documentation preparation. Verify the checked-out SHAs before using their contents; stop if they differ. A `git rev-parse HEAD` run inside a directory without its own `.git` can report the enclosing repository's SHA, so also check the clone root.

[corpus-lock.json](corpus-lock.json) is the machine-readable acquisition lock. The exact pinned CDM root/source POMs were also read directly from the public commit: compiler 9.83.0, bundle 11.120.2, the default generation goal and Java output path were confirmed. Their Git-blob SHA-256 values are in the lock. This verifies configuration, not dependency resolution or successful execution.

## Acquire the two inputs

From the review repository root, into initially absent directories:

```sh
git -c core.autocrlf=false clone --depth 1 --branch 5.38.0 https://github.com/finos/common-domain-model.git test-corpus/cdm/cdm-5.38.0
git -C test-corpus/cdm/cdm-5.38.0 rev-parse --show-toplevel
git -C test-corpus/cdm/cdm-5.38.0 rev-parse HEAD
git -c core.autocrlf=false clone --depth 1 --branch 9.83.0 https://github.com/finos/rune-dsl.git test-corpus/rune-dsl-builtins
git -C test-corpus/rune-dsl-builtins rev-parse --show-toplevel
git -C test-corpus/rune-dsl-builtins rev-parse HEAD
```

If those paths already exist, inspect them instead of overwriting or assuming they match. The builtin files are under `test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model`. The source models are under `test-corpus/cdm/cdm-5.38.0/rosetta-source/src/main/rosetta`.

Inspect the CDM root POM and confirm its compiler property is 9.83.0. Preserve its project version `0.0.0.master-SNAPSHOT` and the existing Maven model-resource filtering; the Git tag 5.38.0 is a separate version. Do not substitute a newer Rune checkout because the model version appears similar. Preserve the CDM and builtin licences; do not copy other model corpora without their applicable rights.

## Mode 0: generate the independent original Rune reference

In a separate shell, change to `test-corpus/cdm/cdm-5.38.0` and run:

```sh
mvn -B -ntp -pl rosetta-source -am process-sources -Dmaven.build.cache.enabled=false
```

Use the released compiler dependencies selected by that pinned CDM POM. Do not pass its repository-specific `settings.xml`, which selects a private mirror. The expected Java root is `rosetta-source/src/generated/java`. Require successful generation and a nonempty output tree, then record source/output path counts and SHA-256 inventories.

Mode 0 also retains the released runtime and original consumer dependency set. Selecting the replacement runtime would create a separate mixed configuration. Verify runtime resolution using [the Mode 0 instructions](MAVEN-CONSUMERS.md#mode-0-original-rune-dsl).

This is an independent upstream build, not a regeneration of expected output by the replacement. A reference build failure is a blocker to the comparison; do not replace the reference with replacement output or label a skipped comparison successful.

### Verify the acquired inputs and regenerated reference

Return to the review root and, with Python 3 available, run:

```sh
python3 docs/verify-public-corpus.py
```

(`python` on Windows; the helper needs only the standard library and `git` on `PATH`.)

The read-only helper checks independent clone roots, exact commits, unchanged tracked inputs, source-file presence, unexpected Rune inputs, the pinned POM blobs and the [retained CDM manifest](evidence/cdm-5.38.0.sha256). It compares **87 Rune sources and 3,950 reference Java files** (4,037 rows): the sources against the Git blob bytes of the pinned commit, the Java against the retained reference that the released 9.83.0 plugin generated on Windows. Each file is classified from its raw bytes first: `identical`, `lineEndingOnly` (raw bytes differ, CRLF→LF-normalised content equal), `acceptedOrdering` (see below), `unverifiedOrdering`, `different`, `missing` or `extra`. `rawBytesIdentical` is true only when every file is byte-identical to the retained reference; an accepted ordering or a line-ending difference makes it false while `passed` can still be true. `docs/verify-public-corpus-controls.py` proves each of the seven classifications fires (eight synthetic controls, including a witnessed alternative ordering and an LF-converted reference, both of which must report `rawBytesIdentical: false`, and an extra file, which must fail).

Expected report on a Windows checkout acquired with `core.autocrlf=false`: 4,037 expected and observed; `identical` 4,034 or more; `lineEndingOnly: 0`; up to three `acceptedOrdering` entries, each naming the witnessing run; empty `unverifiedOrdering`, `missing`, `extra` and `different` lists; `rawBytesIdentical` true only if your generator happened to reproduce the retained ordering of all three order-dependent files; `passed: true`; exit 0. The two independent regenerations recorded for this drop read 4,035 identical + 2 accepted orderings and 4,034 identical + 3 accepted orderings (`rawBytesIdentical: false` in both). On an LF platform the deterministic reference Java files are expected under `lineEndingOnly` and an accepted ordering carries `lineEndingDiffers: true`, still with `passed: true`; a **source** file under `lineEndingOnly` means the clone applied line-ending conversion and must be re-acquired as instructed above. Exit 1 means a content or population difference, or an unverified ordering, to investigate; exit 2 means a prerequisite, pin or manifest problem. Save the JSON output with the run record.

The scope is the declared main-source and generated-Java roots; Maven's duplicate `target` copies, the three CDM test models under `tests/` and unrelated files are outside it. A raw-byte difference that disappears under line-ending normalisation is reported with its count, never silently normalised; a content difference is reported and fails the run. Do not regenerate the manifest or the witnessed-variant list to make a run pass.

### Files that depend on the generating machine's enumeration order

Three files of this cell came out differently from independent runs of the released generator on the same pinned sources: `cdm/event/common/meta/BusinessEventMeta.java` and the `package-info.java` files of `cdm.observable.asset.calculatedrate` and `cdm.product.asset.floatingrate`. In each the released generator lists elements in the order in which it enumerated the source files of a namespace, which is the enumeration order of the generating machine: the namespace descriptions in a package-info, and the 37 `Qualify_*` registrations of `BusinessEventMeta`, whose two source-file blocks (11 declarations from `event-common-func.rosetta`, 26 from `event-qualification-func.rosetta`) swap position while every registration and its multiplicity stay the same. The development repository replays the retained order for its own goldens through recorded input-order pins, and its harness comments record the same block movement for other CDM releases.

The verifier accepts, for those files only, the forms that an independent released-generator run actually produced: the retained reference (`R`) and the two independent 22 September 2026 regenerations (`W1`, `W2`), listed with their raw and normalised hashes in [the witnessed-variant list](evidence/cdm-5.38.0.witnessed-variants.tsv) and attributed to the runs recorded in [the Mode 0 witness record](evidence/cdm-5.38.0.mode0-witnesses.json). Nothing in that list was rendered by this implementation. A form no released-generator run has produced is reported as `unverifiedOrdering` and the run does not pass: compare the file by hand against the retained one (the difference must be a moved block of entries with equal membership and multiplicity, as recorded for the witnessed forms, where a list's trailing separator stays with its position), keep both files and the diff with your run record, and treat the case as a new witness only after that inspection. A fourth file, the `cdm.observable.asset` package-info, has two source-file definitions and is therefore order-dependent in principle, but every recorded run produced the retained form; a different ordering of it would be reported as `different` until witnessed.

Retain this clone and its verification receipt unchanged. Apply the replacement plugin edits only to separate consumer checkouts, as described in [Consumer POM integration](MAVEN-CONSUMERS.md). The strict helper is expected to reject intentionally changed POMs; do not weaken its checks to accommodate the experiment. CDM's clean configuration removes generated sources, so preserve the reference before any clean operation.

The separate legacy demo driver is another reference-generation entry point, documented in `demo/harness/codegen/legacy/README.md`. It uses released 9.83 artifacts with an explicit EMF dependency override and unshaded copied dependencies. Follow its disposable-copy signature handling if using that driver; never strip signatures in the Maven cache. Keep original and replacement classes off the same classpath.

## Compare the two compatibility routes

Return to the review repository root and complete [Building](BUILDING.md), including installed provider jars. Run:

```sh
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Dtest=D11CorpusRegressionTest -Dd11.corpus=cdm -Dd11.version=5.38.0 -Dcorpus.required=true -Dpr-speed.forkCount=1
mvn -B -ntp -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml test -Pir-on -Dtest=D11CorpusRegressionTest -Dd11.corpus=cdm -Dd11.version=5.38.0 -Dcorpus.required=true -Dpr-speed.forkCount=1
```

Check the CDM cell's emitted comparison rows, executed/skipped tests and route accounting. Record identical, different, no-golden, missing-output and refusal counts by file kind. The intended claim is **CDM 5.38.0, Rune 9.83.0, both compatibility routes, the file kinds actually visited**. It is not a rerun of the 25-release matrix or evidence that every IR emitter is native.

Missing inputs can trigger assumptions in parts of the test harness. A successful Maven exit with skipped or empty comparisons is not acceptance. Do not refresh the committed global baseline manifest from a one-cell checkout; that would redefine the wider reference population.

The retained historical [Mode 1](evidence/compatibility-25cell-off.txt) and [Mode 2](evidence/compatibility-25cell-on.txt) rows give the following expected CDM 5.38.0 population **per route**:

| File kind | Expected = identical |
|---|---:|
| POJO, type-format validator, cardinality validator, only-exists validator, metadata (`XMETA`) | 555 each |
| Enum | 208 |
| Function | 377 |
| Data rule | 448 |
| Metafield wrapper | 89 |
| Deep-path utility | 23 |
| Package information | 30 |
| **Total across 11 kinds** | **3,950** |

The expected CDM result is zero differing, no-golden or missing files in every kind except the two kinds that carry the enumeration-order-dependent files above: metadata (`XMETA`) and package information. There, a freshly regenerated reference can differ from the replacement output in `BusinessEventMeta.java` and in up to three package-info files, because the replacement replays the retained ordering; each such file is reported as a mismatch by the kind's D11 method (`xmeta_comparison`, `packageinfo_comparison`) and the Maven run exits 1 on those methods while every other kind is asserted as usual. D11 stays strict: such a mismatch is reported and is never turned into a pass. It may be recorded as an ordering-only difference only when **both** sides are established independently, because the verifier above checks the acquired reference alone and says nothing about the replacement's bytes:

1. **The reference side.** The verifier classified the acquired file as `acceptedOrdering` (a form a released-generator run produced), or, for a package-info, the same module's `GoldenInputOrderPinTest` (run with `-Dtest=GoldenInputOrderPinTest` in place of the D11 test name, same properties) reports an input order that reproduces the acquired golden byte-for-byte.
2. **The candidate side.** Re-run that route's D11 command with `-Dd11.dump-waivered-dir=<absolute directory>` added. For every mismatching file D11 then writes the replacement's output to `<directory>/cdm_5.38.0_<KIND>/gen/` and the acquired reference to the matching `golden/` folder (for example `cdm_5.38.0_PACKAGE_INFO`), each file named by its path with `/` replaced by `~`, both after D11's line-ending normalisation (CRLF and CR become LF). The SHA-256 of the `gen/` file must equal the normalised hash (fourth column) of a row for the same file in [the witnessed-variant list](evidence/cdm-5.38.0.witnessed-variants.tsv), whose paths carry the `cdm/cdm-5.38.0/rosetta-source/src/generated/java/` prefix. For the `cdm.observable.asset` package-info, which has no row there, it must equal the normalised hash (third column) of its row in [the manifest](evidence/cdm-5.38.0.sha256). The replacement replays the retained ordering, so its file is expected to match the retained form.

If the candidate matches no such form, treat the file as a real difference unless an inspection of the exact `golden/` against `gen/` diff shows that entries only changed position: the same qualifier registrations of `BusinessEventMeta`, or the same namespace descriptions of a package-info, each with the same multiplicity, and no other byte different. Compare list entries, not raw lines: a list's separator stays with its position, so in the two witnessed `BusinessEventMeta` forms two registrations trade their trailing comma. Keep that diff with the run record. For example, a replacement `BusinessEventMeta.java` with one of its 37 `Qualify_*` registrations removed is a real difference even while the verifier classifies a witnessed reference as `acceptedOrdering`: its hash matches no listed form and its diff removes an entry. A mismatch in any other file is always a real difference. None of the 12 wider-matrix non-emission waivers applies to this cell.

During the export validation both routes reported every other kind identical at the counts above; on the builder's regeneration package information read 28 identical + 2 order-only mismatches, and on the independent review's regeneration metadata read 554 + 1 and package information 28 + 2, all of them witnessed forms on the reference side. The candidate side of the builder's two package-info mismatches was checked in the correction pass: both dumped replacement files match the retained form. The [snapshot record](SOURCE-SNAPSHOT.md) has the run records. Native writer/shadow/fallback counts are separate and depend on the implementation revision.

## Produce inspectable Java from all three modes

Follow [Consumer POM integration](MAVEN-CONSUMERS.md): install the compiler modules, create separate CDM checkouts from the verified pin, replace the active Java plugin, and invoke each route through CDM's Maven lifecycle. This preserves source filtering, builtin loading and generated-source registration. The output in each checkout is `rosetta-source/src/generated/java`.

Check the route header and per-family output counts. Mode 3 output may differ and requires compilation/behaviour checks; a byte-diff cannot establish its correctness. Project-specific extra generators and tabulators are outside the 11-kind corpus comparison and need separate integration acceptance.

For an explicit comparison of two generated trees, the existing script can write a separate receipt:

```sh
pwsh -File demo/build/diff-trees.ps1 -A test-corpus/cdm/cdm-review-m1/rosetta-source/src/generated/java -B test-corpus/cdm/cdm-review-m2/rosetta-source/src/generated/java -NormalizeEol -ReceiptDir target/reviewer-receipts -Id reviewer.cdm.m1-vs-m2 -Title "CDM 5.38.0: replacement Mode 1 vs Mode 2"
```

Run that comparison from the review repository root after following the documented worktree layout. The script reports differences as data and can exit zero when trees differ. Read `identicalFiles`, `differingFiles`, `onlyInA` and `onlyInB`; require a nonempty population and explain all differences. `-NormalizeEol` explicitly normalises line endings. This compares newly generated consumer outputs; the unchanged upstream reference and the recorded D11 rows are separate evidence.

The consumer guide explains how to continue to compilation and module tests. The rule/function execution check of all three routes is the small example of [Building](BUILDING.md#run-the-small-three-mode-example), executed for the current drop; a one-cell execution exercise on a real model through the consumer builds was not executed and is listed in [Source snapshot § Not executed](SOURCE-SNAPSHOT.md#not-executed-from-this-export). The existing full optimised paired gate has broader corpus prerequisites and is not narrowed by D11 scope flags.

## The broader historical matrix

The canonical source is `test-corpus/corpus-cells.tsv`. Its 25 catalogue releases at compiler 9.83.0 are:

| Model | Releases |
|---|---|
| CDM | 5.38.0, 5.39.0, 6.20.2, 6.20.3, 6.20.4, 6.20.5, 6.20.6, 6.21.0, 6.22.0, 6.23.0 |
| DRR | 5.61.0, 6.34.1, 6.35.0, 6.36.0, 6.37.0, 6.38.0, 7.0.0, 7.1.0, 7.2.0, 7.3.0 |
| ISO 20022 | 1.38.0 |
| Rune FpML | 1.6.0, 2.0.0, 2.1.0, 2.1.1 |

Authored chaos 1.1.0 is a separate 26th cell. Resolution-only dependency cells are not extra catalogue coverage. `test-corpus/CATALOGUE.md` is the reader's introduction to the TSV; the historical five-cell regression set (CDM 5.38.0, CDM 6.20.6, DRR 6.34.1, ISO 20022 1.38.0, Rune FpML 2.0.0) is a subset of this population, not a separate catalogue.

One resolved dependency substitution is material: DRR 6.34.1 declares CDM 5.37.0 and ISO 1.37.0, while the catalogue uses CDM 5.38.0 and ISO 1.38.0 for that comparison. Other cells also have their own pinned transitive models. Read the TSV's declared/resolved closure before reproducing or extending a matrix claim. Do not describe the entire historical population as public or automatically available to a fresh clone.
