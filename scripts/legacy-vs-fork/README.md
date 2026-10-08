# Legacy-vs-Fork Byte-Diff Harness

**Scope:** the scripts in this directory measure the legacy upstream
rune-maven-plugin emission against the as-cloned goldens (legacy-vs-
legacy goldens-staleness check). The downstream **fork-vs-legacy**
comparison is performed by the JUnit `D11CorpusRegressionTest` against
the goldens these scripts validate; the scripts themselves do NOT
invoke the fork generator (Copilot R15 F1 — clarified scope).

## Why

Prior cluster classifications (CODEGEN_DRIFT / VERSION_DELTA / TRANSITIVE_CDM_GAP /
M7B_PAUSED / PERMANENT) in `d11-known-divergent.txt` were partially derived from
reasoning about waiver entry patterns rather than from running the legacy
plugin and comparing outputs. That approach produced misclassifications
(see the development plan "2026-05-17b-resume-p2.1.2b-codegen-gap-discovery"
for the full retrospective).

This harness produces empirical ground-truth in two stages:
1. **In-scope here:** per-cell file-level diffs between what the legacy
   upstream plugin emits today and what the fork's goldens contain
   (which were emitted by some past legacy plugin run when the cell
   was cloned). This validates whether the goldens themselves are
   stale before any classification work proceeds.
2. **Downstream (D11):** once goldens are validated, the fork-vs-legacy
   comparison runs via `D11CorpusRegressionTest` which invokes the
   fork generator and diffs its emission against the goldens.

## Per-corpus goldens layout

D11 resolves goldens differently per corpus
(see `D11CorpusRegressionTest#resolveGoldensDir`). The diagrams below
distinguish `<goldens-parent>` (the directory whose subtree is `java/`)
from `<goldens-parent>/java/` (the actual `.java` files D11 reads):

| Corpus | `<goldens-parent>` | Goldens subtree |
|---|---|---|
| `iso20022/*` | `rosetta-source/target/classes/generated` | `<goldens-parent>/java/` |
| `cdm/*`, `drr/*`, `rune-fpml/*` | `rosetta-source/src/generated` | `<goldens-parent>/java/` |

Snapshot paths are formed by suffixing `<goldens-parent>` (NOT the `java/`
subtree) with `-as-cloned` — e.g. `rosetta-source/src/generated-as-cloned/`.
The scripts' shared `goldens_parent_dir()` helper returns the parent,
mirroring D11 + the snapshot convention uniformly.

## Flow

```
[cell src/main/rosetta/]            [cell <goldens-parent>/java/]
       │                                       │
       │ (1) snapshot the parent dir           │
       │     to <goldens-parent>-as-cloned/    │
       ▼                                       ▼
[legacy rune-maven-plugin]  ──emits──▶  [<goldens-parent>/java/]
       (per cell pom)                          │
                                               │
                                               ▼
                          (2) diff <goldens-parent>-as-cloned/java/
                              vs <goldens-parent>/java/  →  drift report

[Fork's D11CorpusRegressionTest]
       │
       │ uses <goldens-parent>/java/ as goldens
       │ emits via fork to temp dir
       │ compares per-file
       ▼
   (3) fork-vs-current-legacy gap report
```

After step (2):
- If diff is **empty** → goldens accurately reflect today's legacy output;
  D11 measurements are valid; waiver classifications can be re-derived
  from existing D11 mismatch data without further regen.
- If diff is **non-empty** → goldens have drifted since the cell was cloned;
  must use current-legacy emission as the comparison reference;
  re-run D11 against the regenerated goldens.

After step (3):
- Per-cell file-level fork-vs-legacy gap data
- Drives empirical re-classification of waiver entries

## Scripts

| File | Purpose |
|---|---|
| `snapshot-goldens.sh` | Per-cell backup of `<goldens-parent>` to `<goldens-parent>-as-cloned` |
| `run-legacy-regen.sh` | Per cell: `mvn process-sources -pl rosetta-source -am -DskipTests` (unified across all 4 corpora — fires both `rosetta-maven-plugin` for CDM/DRR/ISO at `generate-sources` and `rune-maven-plugin` for rune-fpml at `process-sources`) |
| `diff-as-cloned-vs-regen.sh` | Per cell: enumerate per-file diff; report only-in-A / only-in-B / content-differs |
| `restore-goldens.sh` | Per cell: revert goldens from snapshot |
| `cells.txt` | 5 D11 cells (PR #85 rebaseline; uniform rune-dsl 9.83.0) |

## Cell list

5 D11 cells at uniform rune-dsl 9.83.0 (PR #85 rebaseline 2026-05-28; cdm-master dropped — moving to v10 track):

| Cell | rosetta.dsl.version | deps |
|---|---|---|
| cdm/cdm-5.38.0 | 9.83.0 | - |
| cdm/cdm-6.20.6 | 9.83.0 | - |
| drr/drr-6.34.1 | 9.83.0 | cdm 5.37.0 + iso20022 1.37.0 (transitive via `target/parent-dependency/`) |
| iso20022/iso20022-1.38.0 | 9.83.0 | - |
| rune-fpml/rune-fpml-2.0.0 | 9.83.0 | - |

Source of truth: each cell's top-level `pom.xml` `<rosetta.dsl.version>` property.

## Repo state assumptions

- Java 21 installed (verified: 21.0.8)
- Maven 3.9.x installed (verified: 3.9.11)
- `~/.m2/repository/` already has all required plugin + dependency JARs from
  prior D11 test runs. Post-PR-#85 (uniform rune-dsl 9.83.0), the required
  build-side plugin is `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0`
  (used by CDM/DRR/ISO cells) + `org.finos.rune:rune-maven-plugin:9.83.0`
  (used by rune-fpml; bound to `process-sources`). Both plus the resolved
  rune-dsl 9.83.0 lib + transitive CDM/ISO deps are cached locally after
  the first `mvn -pl rosetta-source -am process-sources` run per cell
  (the unified command — see Scripts table above).
- Cells with `target/parent-dependency/` populated from prior `mvn initialize`
  (each cell's pom does this via maven-dependency-plugin)
- Existing goldens populated under the corpus-specific path (see "Per-corpus
  goldens layout" above)

## Discipline

- Do NOT pollute upstream cell git state. Snapshots are gitignored at the
  parent repo level. The harness-specific patterns in `.gitignore` are:
  - `**/rosetta-source/src/generated-as-cloned/` (cdm/drr/rune-fpml)
  - `**/rosetta-source/target/classes/generated-as-cloned/` (iso20022)
  - `/docs/harness/logs/`
  - `/docs/harness/diffs/`
  - `/docs/harness/*-summary.md` (matches `as-cloned-vs-regen-summary.md` plus future summary files)

  KNOWN LIMITATION: each
  `test-corpus/<cell>/` is itself a clone of an upstream repo
  (e.g. `finos/common-domain-model`), and the parent's `.gitignore` does NOT
  reach into the child clone's git index. Snapshots created under
  `test-corpus/<cell>/rosetta-source/src/generated-as-cloned/` will therefore
  appear as untracked files when you `git status` inside the child clone.
  Treat these as transient working artifacts; do not commit them upstream.
  (Future improvement option: move snapshots to a sibling location outside
  the child clone, e.g. `tmp/harness-snapshots/<cell>/`.)
- One JVM at a time (per `feedback_one_jvm_at_a_time.md` (local)); script
  orchestration is serial unless explicitly parallelized.
- `-Dmaven.build.cache.enabled=false` to avoid build-cache interference.
- Maven failure in any cell causes `run-legacy-regen.sh` to exit non-zero;
  similarly, missing snapshots in `diff-as-cloned-vs-regen.sh` exit non-zero
  so the harness can be used as an acceptance gate without false-passes.
