# Recorded results and their limits

This is the retained detailed record for the 22 September 2026 code snapshot. Start with the [current guide](EVIDENCE.md) for its present interpretation and scope. The commands and measurements below retain their original validation dates; they are not new runs.

These are banked development results, not newly executed tests of the curated review package. The source inspected for this documentation is identified in [Source snapshot](SOURCE-SNAPSHOT.md). [Compact receipts and report extracts](evidence/README.md) are included with source provenance, hashes and declared path redactions. A later export must retain that evidence and update any changed claims together.

## Compatibility

On **each** compatibility generation route—replacement without IR and IR-assisted compatibility Java—the recorded 25-release Rune 9.83.0 matrix contains **174,129 identical files, zero differing files and 12 named non-emissions, from 174,141 reference files per route**. Line endings are normalised by the declared comparator. This is output evidence, not proof of wholly native IR generation.

Historical release evidence: commit `1448f0a7d8d9c1b6bee9a84a8d56e896e3e10083`, tagged 2 September 2026. The authentic compact [Mode 1 rows](evidence/compatibility-25cell-off.txt) and [Mode 2 rows](evidence/compatibility-25cell-on.txt) contain all 25 cells × 11 file kinds per route; the inventory records their original hash and the hash of the LF form, which is the release's matrix digest. The release's abbreviated matrix digest is `d47fa88f`. These are retained results, not reruns at the documentation inspection revision.

The complete release scope is CDM 5.38.0, 5.39.0, 6.20.2–6.20.6, 6.21.0, 6.22.0, 6.23.0; DRR 5.61.0, 6.34.1, 6.35.0–6.38.0, 7.0.0–7.3.0; ISO 20022 1.38.0; Rune FpML 1.6.0, 2.0.0, 2.1.0, 2.1.1. [Corpus](CORPUS-9.83.md) expands the versions and identifies the dependency substitution; `test-corpus/corpus-cells.tsv` defines the resolved inputs.

The 12 non-emissions are `ReferenceWithMetaVoid.java`, once in each of eight CDM 6.x and four DRR 7.x cells. The retained waiver describes an upstream wrapper for an unresolved type; these missing outputs must not be counted as identical. See the [compatibility report extract](evidence/compatibility-source.txt) and `rune-java-generator/src/test/resources/d11-known-divergent.txt`. The extract is not the full original run log.

The authored chaos 1.1.0 population is separate. At the historical 3.2 close, both compatibility routes recorded 11,811 of 12,169 reference files identical, 53 named byte gaps, 316 named refusals and one named missing output, with zero silent emissions. Those categories are different accounting views and **must not be summed as disjoint file counts**. This was explicit accounting of outstanding cases, not universal adversarial compatibility. Later work must be read from its own receipts/registers.

## Source-to-Java generation

| Measurement | Released upstream 9.83 | Replacement Mode 1 | Scope |
|---|---:|---:|---|
| Cold JVM to written Java, median of 3 interleaved runs | 91.263 s | 14.492 s | 6.30× speedup for this generation task |
| Peak resident memory in that measurement | 1,531 MiB | 2,112 MiB | Memory increased; this is not an improvement claim |
| Clean Maven source generation, median of 3 sequential runs | 45.622 s | 19.154 s | 2.38× for this build entry point |

Both experiments use the combined CDM 5.38.0, ISO 20022 1.38.0 and DRR 6.34.1 model set with builtins. The direct driver parsed 351 source files and produced 19,219 Java files; all 19,219 matched after line-ending normalisation. Dependencies were warm; the cold measurement includes fresh-JVM startup through final write and excludes downstream Java compilation/tests.

The Maven measurement cleaned output on each run and included Maven/JVM startup. Of 19,219 paths, 19,216 were identical; three `package-info.java` files differed in description ordering. The replacement Maven output matched its direct-driver output. This is source generation, not a full build or incremental-edit benchmark. Do not multiply the two speedup ratios.

Environment: recorded on 20 August 2026, Windows 11 Pro Insider, Intel Core Ultra 9 285K, JDK 21.0.8. Retained receipts: [reference generation](evidence/codegen.legacy.json), [Mode 1](evidence/codegen.m1.json), [direct comparison](evidence/diff.legacy-vs-m1.json), [reference Maven](evidence/build.legacy.clean.json), [replacement Maven](evidence/build.plus.clean.json) and [Maven comparison](evidence/build.paritydiff.json). Their commands and host fields govern the precise comparison.

## Runtime changes

These are separate runtime changes measured on reference-generated DRR Java, not a claim that native IR emission caused the gains. Each paired experiment used six JMH forks per leg; one operation invoked 23 surviving report functions over synthetic populated inputs.

| Change | Before → after | Measured speedup | Allocation change |
|---|---:|---:|---:|
| U020: lazy failure text | 8,527.481 → 2,454.674 ms/op | 3.47× | −66.0% |
| U021: deferred mapper-path lineage | 2,617.369 → 2,031.881 ms/op | 1.29× | −18.1% |
| U022: deferred mapper-path construction | 1,970.522 → 1,260.064 ms/op | 1.56× | −11.57% |

The baselines differ. These ratios are neither cumulative nor multiplicative. The [U020](evidence/runtime-U020-source.txt), [U021](evidence/runtime-U021-source.txt) and [U022](evidence/runtime-U022-source.txt) extracts preserve the source-reported measurements and uncertainty. They are not raw JMH logs. Reproduction additionally requires the exact inputs, generated code, environment and old/new runtime variants; the extracts alone do not supply that complete experiment.

## AI-context experiment: what changed and why

The question was whether a development assistant asking **structural model questions** could receive a compact index of declarations and relationships instead of complete source files. For example, a type/dependency question may need names, attributes and references, while whole source files also contain descriptions, mappings, comments and unrelated rule bodies.

The experiment represented the **same 351 parsed files and 7,661 declarations** in two ways:

| Representation | Tokens under `o200k_base` |
|---|---:|
| Complete source input | 1,894,552 |
| Structural digest | 284,248 |

That is **6.67× smaller**, measured with tiktoken 0.14.0 in a Python parser/index experiment. The digest deliberately omits expression/rule bodies, descriptions, documentation references, mappings, synonyms and comments. Those omissions make it unsuitable as a complete substitute for source when reasoning about business logic or documentation.

The analysis attributes 51.1% of the raw token baseline to removed prose/mapping material. Against source with that material stripped, the digest was 3.26× smaller. Thus the result combines selecting less information with a more compact structural representation; it is not a like-for-like representation of all program meaning.

No LLM call was made. No answer-quality, reasoning-time or production SDK/Java-IR improvement was measured. The result motivates structured query/context services for future agent work; it does not show that Xtext/EMF could not expose equivalent information.

Primary evidence: [AI context receipt](evidence/ai.context.json); the source implementation is `demo/nojvm/ai_context.py` and `demo/nojvm/queries.py`. The receipt records Python 3.13.5 and the August 2026 run. Its separate dependency-slice experiment must not be silently substituted for the full 351-file comparison above.

## Current IR maturity (the choice milestone)

At the exported revision the legacy Java route writes **93,703 of 174,141 generated Java files (53.8%)** under the historical IR-fed writer-ownership measure on the 25 real-model cells, counted by file kind: enums 4,542; data types 17,585 and their derived files 71,096; choices 80 and their derived files 400. The denominator is every generated Java file of the 25 pinned Rune 9.83.0 releases on that route, over the 11 file kinds; the chaos cell is excluded. The count comes from the committed fallback register (`rune-java-generator/src/test/resources/d11-ir-fallbacks.txt`, 12 rows, all on the chaos cell) and the per-cell writer lines of the development validation chain at that revision, which are included verbatim as [ir-writers-32f8c9ec4.txt](evidence/ir-writers-32f8c9ec4.txt) (enum, data-type and choice writers per cell) with the shadow comparisons, readiness and verdict lines in [ir-unit-shadows-32f8c9ec4.txt](evidence/ir-unit-shadows-32f8c9ec4.txt); it is not a new run of this repository. Existing helpers/templates remain dependencies. This is not independent IR-emission certification. It is a file-ownership share, not overall project completion: the metafield wrappers, package-info files and label providers (2,505 files) and the function, rule and data-rule file bodies (77,933 files) are still written by the existing generator even where the IR handles their expressions.

The comparison scopes behind the choice milestone are separate populations and must not be collapsed into one figure:

| Historical check and stated scope | Population | Result |
|---|---|---|
| Choice POJO shadow against the existing generator's own render | 237 choices over 26 cells (80 real-model, 157 chaos) | 235 identical + 2 without a golden; the two are chaos cases the corpus holds no file for; 0 differing, 0 refused |
| The five derived members of each choice (validators ×3, meta, deep-path) | 237 choices per member | identical on every member, with their own per-member populations |
| Type-unit verdicts | 19,553 attempted units = every data type and choice of the 26 cells | attempted equals the host's own count on every cell; refusals: 12, all chaos datatype collision cases |
| Reported compatibility rings (IR on and off) | the five pinned ring cells, 34,686 files | 34,685 identical, 0 differing, 1 named non-emission (`ReferenceWithMetaVoid`) on both routes |
| Full 25-cell matrix, both routes | 174,141 reference files per route | 174,129 identical, 0 differing, 12 named non-emissions per route |

The first three rows are supported by the IR-on writer/shadow extracts linked above. The ring row is reported in the [compatibility source extract](evidence/compatibility-source.txt); its own historical scope applies, and the IR-on writer/shadow extracts alone do not establish the IR-off ring. The full 25-cell, two-route figures are supported separately by the historical Mode 1 and Mode 2 compatibility receipts linked in Compatibility; their original revision and date still apply. The [snapshot record](SOURCE-SNAPSHOT.md) states what was rerun on the assembled export.

## Optimised-route paired execution (the exported revision)

The optimised route's gate (`OptimisedNavigationPairGateTest`, [Experiments § 4](EXPERIMENTS.md#4-optimised-generation-mode-3)) was run at the exported revision on 22 September 2026: **150,831 paired executions at zero behaviour difference over 21 function-bearing cells**, the 10 CDM and 10 DRR releases of the matrix plus the authored chaos 1.1.0 cell (ISO 20022 and Rune FpML carry no function classes and are outside this gate; the population is therefore not the 25-cell file meter's). The per-cell census, with the function-class counts, the two declared non-function classes of the chaos cell and the zero divergence columns, is [optimised-pair-census-32f8c9ec4.txt](evidence/optimised-pair-census-32f8c9ec4.txt); the chaos cell's declared exceptions are `rune-ir-java-optimised/src/test/resources/chaos-optimised-route-broken.txt`. This is execution equivalence of reference and optimised Java on synthesised inputs, not byte identity and not a claim about Mode 3 on inputs outside the harness. Keep historical performance experiments attached to their original scope rather than implying they were rerun at this revision.
