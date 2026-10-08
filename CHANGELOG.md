# Review releases

One entry per published review drop: the revision, what changed in capability, the checks run and the known limitations. Run details are in [Source snapshot](docs/SOURCE-SNAPSHOT.md).

## Documentation guide — 8 October 2026

The guide now introduces model-driven development, the existing Rune architecture and CDM/DRR workflows, the contribution design, phase 3.3 completion criteria, scoped measurements and the proposed standalone SDK. Responsive diagrams and the retained developer-interface concept support the explanation. The index links to build, evidence and provenance material.

The compiler remains the **22 September 2026 snapshot**, with Rune DSL 9.83.0 as its compatibility baseline. Historical writer ownership is distinguished from independent emission and certification. The completed September fresh-clone check is carried forward from its retained handback and logs; no compiler tests or benchmarks were rerun for this documentation update.

Documentation, links, diagrams and package integrity were reviewed. Source, build configuration, tests, retained evidence, licence terms and agent instructions are unchanged. The repository is presented as one curated root commit, with detailed build, evidence and preparation records retained in current supporting documents. Source presentation/PDF attachments and private planning records are not included.

## Drop 1 — 22 September 2026: the choice milestone

**Source revision:** `82e7921352111c4b0ae38fd37b8f8c89402f64c7` (the development main line at the choice milestone; its tree equals validated development commit `32f8c9ec4b337c5d3d33c9158e8410dd2225b36e`). Compatibility baseline Rune DSL 9.83.0.

**Preparation:** an initial snapshot and two corrections, each reviewed independently before the next, then a final wording pass on the snapshot record and this changelog. The repository carries the final result as one commit; the corrections are listed below for the record.

**Capability at this revision**

- Parser, semantic services and both compatibility generation routes (Mode 1 without IR, Mode 2 through IR) at the recorded 25-release byte-comparison result: 174,129 identical, 0 differing, 12 named non-emissions per route ([Evidence](docs/EVIDENCE.md)).
- IR-fed writer ownership of enums, data types and choices with their derived files (historical measure, retaining legacy helpers/templates): 93,703 of 174,141 generated Java files (53.8%) on the compatibility IR route across 25 real release cells × 11 file kinds, excluding chaos; remaining file families still use existing writers ([Evidence § Current IR maturity](docs/EVIDENCE.md#current-ir-maturity-the-choice-milestone)).
- The chaos gate (phase 3.2) retained as active regression coverage ([Chaos gate](docs/CHAOS-GATE.md)).
- Mode 3 (IR-selected optimised Java, AST-hosted emission) at its delivered scope ([Modes](docs/MODES.md)).

**Packaging for review** (details in [Source snapshot § Packaging differences](docs/SOURCE-SNAPSHOT.md#packaging-differences))

- Development journals, plans, audits, internal ledgers, release notes, research notes, the demo corpus payloads and the hosted workflows are not included; two tests that only locked the layout of excluded audit documents, the `AuditDocGenerator` helper and a smoke test of an excluded hosted workflow were retired, and a third audit test was retired with its document while its two assertions that are not about that document were restored as two tests.
- New for reviewers: `examples/review` (a Maven consumer that generates, compiles and executes the plugin's fixture on each replacement route), `docs/CI.md`, `docs/EXPERIMENTS.md`, `CONTRIBUTING.md`, the corpus verifier (raw bytes first, then line-ending-only differences and witnessed orderings), and the retained engineering documents' links to excluded records rendered as plain text.

**Checks run on the assembled export** (Windows 11 Pro, JDK 21.0.8, Maven 3.9.11, isolated Maven repository and empty settings, build cache off)

- Bootstrap of the seven modules; the three-mode example (47 files each, 3 tests each, Modes 1 and 2 byte-identical, Mode 3 identical after line-ending normalisation on this fixture); the negative control of the example test.
- The public CDM 5.38.0 walkthrough: independent Mode 0 regeneration with the released 9.83.0 plugin and runtime, the corpus verifier (4,037 files: on the builder's regeneration, 4,035 identical and 2 package-info files in a witnessed alternative ordering; on the regeneration made during the review of the initial snapshot, 4,034 identical and 3 in witnessed orderings, those two package-info files and `BusinessEventMeta.java`), both D11 routes on the builder's regeneration (every kind identical at the expected counts except package information: 28 identical + the same 2 order-only mismatches, reproduced by the pin test), the report-only CDM compile gate (closure clean, 0 divergent, 0 generation failures).
- The chaos gate subset (expander check, parser gates, D11 chaos off and on), the plugin runner tests, the document-contract tests, the retained integrity lints, and the module suites in two public layouts (an untracked copy of the export with the builtin clone and worktree only; the one-cell walkthrough installation), with the by-design corpus-guard failures of each layout named in the snapshot record.

**First correction (same drop, after the independent review of the initial snapshot)**

- Attribution: the upstream Apache-2.0 licence and NOTICE added under `licenses/` with a root `NOTICE` naming the included components and the modified runtime files.
- Public walkthrough: the corpus verifier reports exact raw-byte equality, accepts only orderings witnessed by independent released-generator runs (now including the `BusinessEventMeta` qualifier-block case found by the review), ships its failure controls, and the guide's expected D11 outcome names both affected kinds.
- Tests: the registry-consistency and corpus-occurrence assertions of the retired audit test restored as two tests; the retirement record corrected; four corpus-failure messages point at the public walkthrough instead of an excluded script.
- Experiments: the POSIX parser-generation script's classpath initialisation fixed; the setup scripts install the pinned tokenizer; the model-access comparison's driver module and receipts restored with a reproduction section; the input count corrected.
- Evidence: current per-cell writer, shadow and verdict extracts and the 21-cell paired-execution census added and linked; the provenance inventory covers every evidence file (22 entries).
- Documentation: Python removed from the numbered phase table; the historical community-build layer marked as such with the exported procedure; the coverage diagram re-laid and render-checked.

**Second correction (same drop, after the independent review of the first correction)**

- Public walkthrough: an ordering-only reading of a D11 mismatch now needs evidence for both the reference and the replacement's own bytes; D11 stays strict.
- Experiments: the POSIX parser-generation script hands the Windows `java` Windows paths under Git Bash (run with the real ANTLR tool on Windows and Linux); the AI commands use the venv's interpreter; the offline procedure stages the tokenizer's encoding cache and states its verified scope.
- API/ABI guide: the historical Layer 3 baseline procedure is marked unavailable from this snapshot, with its prerequisite named; Layer 1 was run as documented, and the backward-compatibility guide's Layer 3.1 and 3.2 sections and the module guide's ignore-list rule now point to the finding that the pom's include and exclude filters are not applied.

**Known limitations of this drop**

- The DRR, ISO 20022 and Rune FpML cells, the 25-cell matrix, the optimised paired gate and the held-out compile tier were not executed from the export; the recorded results for them are historical ([Source snapshot](docs/SOURCE-SNAPSHOT.md)).
- No hosted CI ([CI](docs/CI.md)).
- The package-info and `BusinessEventMeta` output of the released generator depends on the generating machine's file enumeration order; the walkthrough documents how the verifier (witnessed forms only) and the D11 comparison report it ([Corpus](docs/CORPUS-9.83.md#files-that-depend-on-the-generating-machines-enumeration-order)).
- In a partial-corpus installation several corpus census and catalogue guards fail by design ([Testing](docs/TESTING.md#core-suites-in-a-partial-corpus)).
- Loading of the thin `CLAUDE.md` import was not verified in a reviewer installation.
