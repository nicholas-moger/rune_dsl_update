# Experiments and reproduction

The recorded results are in [Evidence](EVIDENCE.md) with their receipts under [docs/evidence](evidence/README.md). This guide gives, for each experiment, the entry point in this repository, its inputs and dependencies, the command shape, what it reports, and how far it can be reproduced from this snapshot. Historical results stay attached to their original revision and date; nothing here is a new measurement unless a later drop records one.

## 1. Source-to-Java generation (direct drivers)

**Question.** How long does one cold JVM take to turn a merged model set into written Java files, with the released Rune 9.83.0 compiler and with the replacement (Mode 1)?

| | Released 9.83.0 | Replacement Mode 1 |
|---|---|---|
| Entry point | `demo/harness/codegen/legacy` — `demo.harness.legacy.Main generate` | `demo/harness/codegen` — `demo.harness.codegen.Main generate --mode m1` |
| Build | `mvn -f demo/harness/codegen/legacy/pom.xml package`, then strip jar signatures in its `target/lib` with `scripts/xtext-oracle/strip-jar-signatures.py` (the two drivers must never share a classpath; the legacy README explains the EMF pin and the copy-not-shade rule) | `mvn -f demo/harness/codegen/pom.xml package` after the module bootstrap in [Building](BUILDING.md) |
| Inputs | CDM 5.38.0 + ISO 20022 1.38.0 + DRR 6.34.1 sources (349 model files) plus the two builtin files (`demo/corpus/builtins`), 351 files in total | the same |
| Command shape | `java -cp "<legacy classes>;<legacy lib>/*" demo.harness.legacy.Main generate --src "<cdm>;<iso>;<drr>" --builtins demo/corpus/builtins --out <out> --filter all` | `java -cp "<classes>;<lib>/*" demo.harness.codegen.Main generate --mode m1 --src "<cdm>;<iso>;<drr>" --builtins demo/corpus/builtins --out <out> --filter all` |
| Reported | `files`, `wallMs` (JVM start to last file written, so JVM boot is included), `jvmStartToFirstFileMs`, per-phase times; `peakRssMb` sampled by the outer wrapper | the same, plus the route the receipt records |
| Recorded result | 91.263 s median, 19,219 files, 1,531 MiB peak ([receipt](evidence/codegen.legacy.json)) | 14.492 s median, 19,219 files, 2,112 MiB peak ([receipt](evidence/codegen.m1.json)) |

Method as recorded: three cold runs per mode, a fresh JVM and an emptied output tree per run, runs interleaved across the modes, headline = median, warm dependency jars, no other JVM running. The two trees were compared with `demo/build/diff-trees.ps1 -NormalizeEol`: 19,219 of 19,219 identical after line-ending normalisation ([receipt](evidence/diff.legacy-vs-m1.json)).

**Reproduction status.** The drivers, the comparison script and the builtins are in this repository. The DRR and ISO 20022 inputs are not: acquire them at the pins in `test-corpus/corpus-cells.tsv` (the catalogue's `drr 6.34.1` row resolves CDM 5.38.0 and ISO 20022 1.38.0) and point `--src` at them. Timings are machine-specific; a new run is a new measurement with its own receipt.

## 2. Source-to-Java generation (Maven entry point)

**Question.** The same generation through each compiler's Maven plugin, as a consumer build sees it.

| | Released 9.83.0 | Replacement Mode 1 |
|---|---|---|
| Entry point | `demo/build/build-legacy/pom.xml` (`com.regnosys.rosetta:rosetta-maven-plugin:9.83.0`) | `demo/build/build-plus/pom.xml` (`org.finos.rune:rune-maven-plugin:0.0.1-SNAPSHOT`) |
| Command | `mvn -B -f demo/build/build-legacy/pom.xml clean generate-sources -Dmaven.repo.local=<review-only cache>` | `mvn -B -f demo/build/build-plus/pom.xml clean generate-sources -Dmaven.repo.local=<review-only cache>` |
| Recorded result | 45.622 s median of three, 19,219 files ([receipt](evidence/build.legacy.clean.json)) | 19.154 s median of three, 19,219 files ([receipt](evidence/build.plus.clean.json)) |

Both POMs read the same `demo/build/rosetta-config.yml` and the same three source roots under `demo/corpus`; `demo/build/README.md` documents every configuration decision, including why the released plugin runs without `classPathLookupFilter` and the replacement with it. Of 19,219 paths, 19,216 were identical and three `package-info.java` files differed in description ordering ([receipt](evidence/build.paritydiff.json)). Wall clock includes Maven and JVM start-up; the dependency cache was warm. **Reproduction status:** as in § 1 — the DRR and ISO inputs are reviewer-acquired, and `demo/corpus/<cell>` directories must be created from them (the POMs expect `cdm-5.38.0`, `iso20022-1.38.0` and `drr-6.34.1` there).

## 3. Runtime changes (generated Java held fixed)

**Question.** Does a change to the runtime library alone change execution cost, with the generated Java unchanged?

Three runtime changes were measured, each with the same reference-generated DRR Java, six JMH forks per leg, one operation invoking 23 surviving report functions over synthetic populated inputs:

| Change | Entry point in `rune-runtime` | Recorded |
|---|---|---|
| U020 lazy failure text | `ComparisonResult` failure messages built lazily | 8,527.481 → 2,454.674 ms/op, allocation −66.0% ([extract](evidence/runtime-U020-source.txt)) |
| U021 deferred mapper-path lineage | parent-linked `MapperPath` | 2,617.369 → 2,031.881 ms/op, −18.1% ([extract](evidence/runtime-U021-source.txt)) |
| U022 deferred mapper-path construction | `AbstractMapperItem` path built on demand | 1,970.522 → 1,260.064 ms/op, −11.57% ([extract](evidence/runtime-U022-source.txt)) |

The benchmark is `rune-benchmarks/src/main/java/org/finos/rune/benchmarks/DrrRuleEvaluationBenchmark.java` over a compiled DRR closure prepared by `CorpusClasses` / `ReflectivePopulator` in the same module; `demo/harness/exec` is the packaged two-leg variant (`legacy-rt` released runtime, `plus-rt` adapted runtime) with its own JMH benchmarks and the five workloads described in `demo/harness/exec/README.md`. The design notes of each change are `docs/upgrades/U020-…`, `U021-…` and `U022-…`.

**Reproduction status.** The "before" leg of each pair was the runtime at the commit preceding that change; this snapshot carries only the current runtime. What can be reproduced here is released-9.83.0 runtime versus the current adapted runtime through `demo/harness/exec` (one jar per leg), with reviewer-acquired DRR 6.34.1 and its closure. The three ratios are neither cumulative nor multiplicative; the extracts preserve the reported uncertainty but are not raw JMH logs.

## 4. Optimised generation (Mode 3)

**Question.** Does the optimised route's Java behave as the reference route's Java?

| Item | Where |
|---|---|
| Emission | `rune-ir-java-optimised` (`OptimisedIRGenerationProviderImpl`); `OptimisedNavigationEmissionTest` |
| Paired execution | `rune-equivalence/src/test/java/org/finos/rune/equivalence/OptimisedNavigationPairGateTest.java` with `PairHarness`, `InstanceSynthesizer`, `ReflectiveDeepCompare` |
| Commands | [Testing](TESTING.md) rows 10 and the chaos-gate guide's optimised notes |
| Recorded | the pair gate's census of 150,831 paired executions at zero behaviour difference (drawn on the README coverage matrix's optimised column); its declared exceptions in `rune-ir-java-optimised/src/test/resources/chaos-optimised-route-broken.txt` |

The gate needs the function-bearing real-model cells, their dependency closures and the generated overlays under `rune-ir-java-optimised/target/optimised-tree/`; it is not narrowed by the one-cell D11 scope properties. **Reproduction status:** requires authorised corpora; the small example in [Building](BUILDING.md) exercises Mode 3 generation, compilation and behaviour on the fixture only. A separate Mode 3 performance comparison is not recorded in this snapshot.

## 5. AI structural context (no JVM)

**Question.** How many tokens does an assistant need for structural model questions when given a declaration/relationship digest instead of the full source?

| Item | Where |
|---|---|
| Entry point | `demo/nojvm/ai_context.py` (the digest and the `o200k_base` token counts via `tiktoken`); `demo/nojvm/queries.py` (the structural queries, unit-checkable with `py -3 queries.py`) |
| Parser | ANTLR4 Python3 target generated from the same `.g4` grammars by `demo/nojvm/gen-parser.ps1` / `.sh`; virtual environment by `setup-venv.ps1` / `.sh` |
| Command | from `demo/nojvm/`, with the venv's interpreter: `../work/venv/Scripts/python.exe ai_context.py --roots "<builtins>;<cdm>;<iso>;<drr>" --receipt <file>` on Windows (PowerShell or Git Bash); `../work/venv/bin/python ai_context.py ...` on Linux and macOS |
| Recorded | 1,894,552 → 284,248 tokens over 351 files and 7,661 declarations (6.67×); 51.1% of the baseline attributed to removed prose/mapping material, 3.26× against source with that material stripped ([receipt](evidence/ai.context.json)) |

The digest omits expression and rule bodies, descriptions, documentation references, mappings, synonyms and comments; it is a selective structural context, not a substitute for source. No language model was called and no answer quality was measured. The receipt's separate dependency-slice task (a type's closure) is a different measurement and is not the headline. **Reproduction status:** the scripts are in this repository; the inputs are reviewer-acquired as in § 1 (349 model files plus the two builtins). `setup-venv.ps1` / `setup-venv.sh` install the ANTLR Python runtime **and** `tiktoken` 0.14.0 (the version the receipt records) into `demo/work/venv/`, so the command above uses that venv's interpreter; another interpreter works only if `tiktoken` is installed for it. `tiktoken` downloads its `o200k_base` encoding on first use: an offline host needs the encoding cache staged as well as the wheels ([`demo/nojvm/README.md`](../demo/nojvm/README.md), step 2). The POSIX parser-generation script was corrected twice in this drop: its classpath is initialised before use, and under Git Bash it hands the Windows `java` Windows paths. It was run with the real ANTLR 4.13.2 tool under Git Bash on Windows and under `dash` on Linux; the setup scripts and this command were run on Windows with network access blocked for Python, on the plugin's small route fixture, as [Source snapshot](SOURCE-SNAPSHOT.md) records. Those are functionality checks, not a repeat of the recorded token measurement.

## 6. Model access (the three analytics lanes)

**Question.** How long does each path take to load the merged model set and answer four structural questions (declaration counts by kind; data types with a `[metadata …]` annotation; distinct data types with a `Party` attribute; the deepest `extends` chain), and do the answers agree?

| Lane | Entry point | Recorded (20 August 2026) |
|---|---|---|
| `plus-jvm` — this implementation's parser | `demo/harness/analytics` (`demo.harness.analytics.Main plus-jvm`), depends on `rune-parser` only | wall 7.676 s; startup 92 ms; parse 4,465 ms; workspace build 3,084 ms; peak RSS 2,239 MiB ([receipt](evidence/analytics.plus-jvm.json)) |
| `legacy-emf` — the released 9.83.0 Xtext/EMF stack | `demo/harness/codegen/legacy` (`demo.harness.analytics.legacy.Main legacy-emf`), the upstream classpath | wall 90.799 s; startup 926 ms; parse 2,256 ms; `EcoreUtil.resolveAll` 87,587 ms; peak RSS 1,483 MiB ([receipt](evidence/analytics.legacy-emf.json)) |
| `plus-nojvm` — the Python prototype | `demo/nojvm/analytics.py` (ANTLR4 Python3 parser generated from the same grammars) | wall 106.6 s; parse 106,088 ms; no memory figure was sampled for this lane ([receipt](evidence/analytics.nojvm.json)) |

All three lanes returned the same answers (types 2,243; enums 763; functions 1,649; reporting rules 2,272; 206 metadata-annotated types; 26 types referencing `Party`; deepest chain 5) over the same 349 model files (the two builtins loaded but not counted), which is the correctness cross-check of the comparison. The query code is defined once (`Queries.java` in the analytics module) and duplicated byte-for-byte apart from its package line into the legacy module, so a disagreement could only sit in an extractor. The lanes measure different things and are reported as such: the JVM lanes' startup and semantic load (`linkMs` here, `resolveMs` there) are first-class figures, not netted out; the Python lane's parse time is the cost of a bare CPython process with a generated parser. This is an access-path comparison, not a claim about generated code or about answer quality.

**Reproduction status.** Build the fork lane with `mvn -f demo/harness/analytics/pom.xml package` after the module bootstrap in [Building](BUILDING.md), against the same local repository the bootstrap installed into (the README's command line adds `-Dmaven.repo.local=demo/work/.m2`, the demo's own repository; use it only if the bootstrap did); the legacy lane as in § 1; the Python lane as `demo/nojvm/README.md` describes. Inputs are reviewer-acquired as in § 1. Timings are machine-specific; a new run is a new measurement with its own receipt. The receipts' `command` fields name the demo's corpus directories as run on 20 August 2026.

## Reading any of these

Each experiment answers one question over one population with one method. Generation speed, runtime execution cost, optimised-route equivalence and context size are different measures; the ratios do not combine, and none of them is evidence of compiler correctness, which [Testing](TESTING.md) covers separately.
