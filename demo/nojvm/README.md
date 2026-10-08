# Lane C - Model analytics with NO JVM

**The claim:** the Rune model can be parsed, indexed, walked and queried with no JVM
anywhere in the loop.

**Why it holds:** Xtext/EMF needs a warmed JVM, a Guice injector and a global
`EcoreUtil.resolveAll` before a single question can be asked. The fork's parser is driven
by two plain ANTLR4 grammar files:

- `rune-parser/src/main/antlr4/com/regnosys/rosetta/parser/RosettaLexer.g4` (299 lines)
- `rune-parser/src/main/antlr4/com/regnosys/rosetta/parser/RosettaParser.g4` (915 lines)

Both contain **zero embedded target actions**, **zero `@members`/`@header` blocks** and
**zero semantic predicates** (verified by reading both files in full). Nothing in them is
Java. Point ANTLR's Python3 backend at the *same* grammar files the Java engine uses and
you get a working parser for CPython, unmodified. Grammar portability is not a claim about
this demo; it is a property of how the fork's parser front end was built.

The one step that needs `java` is **parser generation**, which is a build-time step run
once by the integrator. Query time is a bare CPython process with a single pure-Python
dependency.

---

## Files

| File | What it is |
|---|---|
| `gen-parser.ps1` / `gen-parser.sh` | Run the ANTLR4 tool jar over the two grammars, Python3 target, into `demo/work/nojvm-gen/`. **Needs java. Integrator step.** |
| `setup-venv.ps1` / `setup-venv.sh` | Create `demo/work/venv/` and install `antlr4-python3-runtime==4.13.2` and, for `ai_context.py`, `tiktoken==0.14.0`. |
| `ai_context.py` | The AI structural-context experiment ([Experiments § 5](../../docs/EXPERIMENTS.md#5-ai-structural-context-no-jvm)). Run it from here with the venv's interpreter, which holds `tiktoken`: `../work/venv/Scripts/python.exe ai_context.py --roots "..." --receipt <file>` on Windows, `../work/venv/bin/python ai_context.py ...` on Linux and macOS. |
| `analytics.py` | Parse the corpus, build the CONTRACTS section 5 index, run Q1-Q4, emit `##DEMO##` events and the receipt. |
| `queries.py` | Q1-Q4 and the walk statistics, with **no ANTLR dependency** - importable and unit-testable on its own. |

Everything generated or installed lands under `demo/work/` (gitignored). This directory
stays committed-clean.

---

## Commands for the integrator

Run from `demo/nojvm/`. PowerShell on the left, POSIX/Git-Bash equivalent below each.

### 1. Generate the parser (needs java; once)

```powershell
./gen-parser.ps1
```
```sh
./gen-parser.sh
```

Defaults: jar `demo/work/antlr-4.13.2-complete.jar` (falls back to
`demo/work/antlr-complete.jar`), grammars from `rune-parser/`, output
`demo/work/nojvm-gen/`. Override positionally (`sh`) or by name
(`-AntlrJar`, `-GrammarDir`, `-OutDir`).

Produces a flat package: `RosettaLexer.py`, `RosettaParser.py`,
`RosettaParserListener.py`, `RosettaParserVisitor.py`, plus `.tokens`/`.interp`.

### 2. Create the venv (needs network, or a staged offline bundle; once)

Installs the pinned ANTLR Python runtime and, for `ai_context.py`, the pinned `tiktoken` (0.14.0) into `demo/work/venv/`, then checks that both import and that `tiktoken` loads its `o200k_base` encoding. Pass `-NoTiktoken` / set `NO_TIKTOKEN=1` to install the ANTLR runtime only: `analytics.py` needs nothing more, `ai_context.py` then cannot run.

```powershell
./setup-venv.ps1
```
```sh
./setup-venv.sh
# Git Bash on Windows, where python3 is often only the Store alias:  ./setup-venv.sh '' py
```

**Offline hosts.** Stage two things on a connected machine with the same operating system, CPU architecture and Python version as the offline host (`pip download` picks wheels for the interpreter it runs under, and `tiktoken` and `regex` ship platform-specific wheels):

1. the wheels: the ANTLR runtime, `tiktoken` and its dependencies (`regex`, `requests`, `charset_normalizer`, `idna`, `urllib3`, `certifi`);
2. the tokenizer's encoding data: `tiktoken` downloads `o200k_base` on first use and caches it, and both the setup check and every `ai_context.py` run need that file. Wheels alone are not enough.

On the connected machine, from `demo/nojvm/` after a normal setup:

```powershell
..\work\venv\Scripts\python.exe -m pip download antlr4-python3-runtime==4.13.2 tiktoken==0.14.0 -d C:\offline\wheels
$env:TIKTOKEN_CACHE_DIR = 'C:\offline\tiktoken-cache'
..\work\venv\Scripts\python.exe -c "import tiktoken; tiktoken.get_encoding('o200k_base')"
```
```sh
# Git Bash on Windows: ../work/venv/Scripts/python.exe in place of ../work/venv/bin/python
../work/venv/bin/python -m pip download antlr4-python3-runtime==4.13.2 tiktoken==0.14.0 -d /path/to/offline/wheels
TIKTOKEN_CACHE_DIR=/path/to/offline/tiktoken-cache ../work/venv/bin/python -c "import tiktoken; tiktoken.get_encoding('o200k_base')"
```

Copy both directories to the offline host. Point `TIKTOKEN_CACHE_DIR` at the copied cache for the setup and for every later `ai_context.py` run (tiktoken checks the cached file's SHA-256 before using it):

```powershell
$env:TIKTOKEN_CACHE_DIR = 'C:\offline\tiktoken-cache'
./setup-venv.ps1 -WheelDir C:\offline\wheels
```
```sh
export TIKTOKEN_CACHE_DIR=/path/to/offline/tiktoken-cache
WHEEL_DIR=/path/to/offline/wheels ./setup-venv.sh
```

Without the staged cache an offline setup stops at the encoding check with a message naming `TIKTOKEN_CACHE_DIR`. This procedure was checked on Windows with network access blocked for the Python processes, not on an air-gapped machine.

### 3. Run the analytics (no JVM)

Full merged corpus, receipt written:

```powershell
py -3 analytics.py --roots "../corpus/builtins;../corpus/cdm-5.38.0;../corpus/iso20022-1.38.0;../corpus/drr-6.34.1" --receipt ../receipts/analytics.nojvm.json
```
```sh
py -3 analytics.py --roots "../corpus/builtins;../corpus/cdm-5.38.0;../corpus/iso20022-1.38.0;../corpus/drr-6.34.1" --receipt ../receipts/analytics.nojvm.json
```

`analytics.py` adds `demo/work/nojvm-gen/` to `sys.path` itself, and if `antlr4` is not
already importable it borrows the venv's `site-packages` - so plain `py -3` works after
step 2 with no activation. `demo/work/venv/Scripts/python.exe analytics.py ...` works too.

**Development loop:** add `--limit 25` to parse only the first 25 files (about 7 % of the
corpus) and get a full result set in a few seconds.

#### Options that matter

| Flag | Effect |
|---|---|
| `--limit N` | Parse only the first N files. Development aid; the full corpus is the default. |
| `--target-type Party` | Q3's target type simple name. Default `Party`. |
| `--receipt PATH` | Write the CONTRACTS section 2 receipt (section `analytics`, lane `plus-nojvm`). |
| `--command "..."` | Record this exact string as the receipt's `command`, so it can be made byte-equal to the `demo/runs.json` entry. |
| `--dump-index PATH` | Write the symbol index as TSV (`name/kind/namespace/file`) - **use this to reconcile a cross-lane disagreement.** |
| `--ll-only` | Disable the SLL fast path. **Positive control:** results must be identical to the default run, wall time longer. |
| `--no-count-nodes` | Skip the full parse-tree node census (`aux.treeNodes`). |
| `--gen-dir`, `--venv` | Point at non-default locations. |
| `--progress-every N` | Override the progress-event interval. |

Exit codes: `0` ok, `1` run failure, `2` environment not prepared (with the exact command
to fix it printed).

---

## The queries (CONTRACTS section 5)

| | Meaning in one line |
|---|---|
| **INDEX** | Every declaration name mapped to its kind, namespace and source file. |
| **Q1** | How many data types, enums, functions and reporting rules the model declares. |
| **Q2** | How many data types carry any `[metadata ...]` annotation, on the type itself or on any of its attributes. |
| **Q3** | How many distinct data types hold an attribute whose declared type is `Party` - a reverse-reference query over the whole model. |
| **Q4** | The longest `extends` inheritance chain among data types, counted in edges (a root type is 0). |

Only **non-builtin** declarations are counted: builtins are the `com.rosetta` /
`com.rosetta.*` namespaces (`demo/corpus/builtins/`), which are parsed but never counted,
exactly as CONTRACTS section 1 specifies.

`results` **must agree digit-for-digit with the `legacy-emf` and `plus-jvm` lanes** - that
agreement is the correctness proof for this lane. If it does not, run `--dump-index` on
this lane and diff against lane A's index before touching any query code.

---

## Grammar rule bindings

Discovered by reading the two `.g4` files. These are the productions the tree walk binds to:

| Concept | Grammar rule | Shape |
|---|---|---|
| namespace | `rosettaModel` | `NAMESPACE (qualifiedName \| STRING)` |
| data type | `dataType` | `TYPE validID (EXTENDS qualifiedName)? COLON ... attribute* condition*` |
| choice | `choice` | `CHOICE validID COLON ... choiceOption*` |
| enum | `enumeration` | `ENUM validID (EXTENDS qualifiedName)? ...` |
| function | `function` | `FUNC validID (LPAREN validID COLON enumValueReference RPAREN)? (EXTENDS qualifiedName)? ...` |
| reporting rule | `rosettaRule` | `(REPORTING \| ELIGIBILITY) RULE validID (FROM typeCall)? COLON ...` |
| attribute | `attribute` | `OVERRIDE? validID typeCall rosettaCardinality? ... annotationRef*` |
| annotation use | `annotationRef` | `LBRACK validID (validID annotationQualifier*)? RBRACK` |
| type reference | `typeCall` | `qualifiedName (LPAREN typeCallArgument+ RPAREN)?` |
| extends target | (token) `EXTENDS` | located by **token type**, never by matching the text `"extends"` |

Also indexed as declarations (they appear in `aux.declsByKind`, not in Q1):
`rosettaTypeAlias`, `annotationDecl`, `rosettaBasicType`, `rosettaRecordType`,
`rosettaMetaType`, `rosettaLibraryFunction`, `rosettaSynonymSource`,
`rosettaExternalSynonymSource`, `rosettaExternalRuleSource`, `rosettaReport`,
`rosettaBody`, `rosettaCorpus`, `rosettaSegment`.

The walk inspects **direct children only**, dispatching on the context class name. That is
what keeps it exact: an `annotationRef` nested in a `condition` body is not a type-level
annotation, and an `attribute` belonging to a `function`'s `inputs:` block is not a data
type's attribute. A descendant search would silently conflate both.

---

## Assumptions

Every one of these is a place where a different-but-defensible reading exists. They are the
first things to check if the three lanes disagree.

1. **Q1 `rules` counts reporting *and* eligibility rules together.** They are one grammar
   production (`rosettaRule`) and one model class, distinguished only by the leading
   `REPORTING`/`ELIGIBILITY` token. The split is reported separately as
   `aux.reportingRules` / `aux.eligibilityRules` so a mismatch is instantly attributable.
   In this corpus the eligibility rules are a small minority.
2. **Q1 `types` counts `dataType` only** - not `choice`, not `typeAlias`, not `basicType`
   or `recordType`. `choice` is a distinct top-level keyword with its own production.
   *This corpus contains no `choice` declarations at all*, so the reading cannot change
   the number here, but it would on a corpus that has them. Counts for every other kind
   are in `aux.declsByKind`.
3. **Q2 tests the annotation head name `metadata`**, i.e. the first `validID` inside an
   `annotationRef`, matching `annotation metadata:` in
   `demo/corpus/builtins/annotations.rosetta`. Any qualifier (`key`, `scheme`,
   `reference`, `id`, `location`, `address`, `template`) counts. Annotations on
   `condition` bodies do **not** count - CONTRACTS says "on the type or any attribute".
4. **Q3 matches the attribute type's simple name**, so both `Party` and
   `cdm.base.staticdata.party.Party` count, and `PartyRole` does not. Scoped to `dataType`
   declarations, so function inputs/outputs and annotation bodies are excluded.
5. **Q4 resolves `extends` by exact qualified name first, then by simple name.** The corpus
   uses *partially* qualified supertypes (`extends common.CommonTransactionReport`), which
   only the simple-name step can resolve. Where a simple name is ambiguous across
   namespaces, same-namespace wins, then same-file, then first - and the fallback is
   counted in `aux.ambiguousExtends`. `aux.unresolvedExtends` and `aux.cycles` should both
   be 0; if they are not, Q4 is understated and the reason is in the index dump.
6. **The `extends` token is found by token type, not by text.** Same for `REPORTING` /
   `ELIGIBILITY` / `STRING`. No regex and no string matching is used for structural
   analysis anywhere in this lane, per the repo's engineering standards.
7. **Identifiers are un-escaped**: the grammar allows `^type` as an identifier
   (`ID : '^'? [a-zA-Z_] ...`), and the leading `^` is stripped so the index holds the real
   name. Qualified names are rebuilt from their `validID` children rather than by splitting
   `getText()`, so an escaped segment survives.
8. **`namespace "quoted.form"`** is accepted (the grammar allows a `STRING` there); the
   token's own quote characters are stripped.
9. **`parseMs` and `indexMs` are accumulated per file** - each tree is parsed, indexed, then
   dropped. Retaining all 351 trees would cost multiple GB in Python and distort
   `peakRssMb`. Both phase totals stay exact; only simultaneous residency is given up.
   This is stated in the receipt's `notes`.
10. **`aux.nodesVisited` counts node *inspections*, not distinct nodes** - the extractor
    makes several targeted passes over a declaration's direct children, so a child examined
    twice counts twice. The distinct total is `aux.treeNodes` (a separate full census,
    timed separately as `aux.treeWalkMs` and excluded from `parseMs`/`indexMs`).

---

## Performance

Python's ANTLR runtime is pure Python and roughly an order of magnitude slower than the
Java one. The design compensates:

- **Two-stage parsing.** Stage one runs `PredictionMode.SLL` with `BailErrorStrategy`;
  ANTLR's guarantee is that SLL either produces the same parse as LL or errors, so a bail
  is simply re-parsed under full LL. `aux.sllBailouts` reports how often that happened
  (expected: 0). `--ll-only` forces LL everywhere and **must produce identical results** -
  that is the positive control on the optimisation, not an assumption about it.
- **Shared DFA cache.** The generated parser holds `decisionsToDFA` and
  `sharedContextCache` as *class* attributes, so every file after the first reuses the
  prediction DFA built by its predecessors. The 351st file is markedly cheaper than the 1st;
  this is why `--limit N` under-represents steady-state throughput.
- **Trees are dropped as soon as they are indexed**, bounding peak RSS.
- **Progress** is emitted every `max(25, total // 250)` files, which keeps the event count
  bounded on a corpus of any size while staying informative on this one.

**Expected runtime - estimate, not a measurement.** This lane has not been run end to end
(writing it is a no-java, no-network task by contract). The corpus is 351 files / ~9 MB, and
`RosettaParser.g4` has a ~90-alternative left-recursive `expression` rule, which is the
expensive part. Budget **a few minutes** for the full corpus on a warm machine, and treat
these as the shape to check rather than a target:

- Parser generation: seconds.
- `--limit 25`: seconds, dominated by ATN deserialisation (`aux.parserReadyMs`).
- Full corpus: minutes, dominated by `parseMs`.

**Calibrate before the demo:** run `--limit 25`, read `aux.parserReadyMs` and `parseMs`, and
extrapolate - then run the full corpus once and take the real receipt. Do not quote an
estimate on the dashboard.

### Startup and memory

- `metrics.startupMs` - process start to interpreter ready, measured from the OS process
  creation time (`GetProcessTimes` on Windows, `/proc/self/stat` on Linux). This is the
  honest counterpart to the JVM lanes' `startupMs`.
- `aux.parserReadyMs` - process start to *generated parser importable*, i.e. including
  ANTLR ATN deserialisation. This is the closer analogue of "injector ready", and it is
  the number to put beside the JVM figure.
- `metrics.peakRssMb` - self-measured in-process (`GetProcessMemoryInfo` / `getrusage`).

All three are omitted rather than guessed if the platform will not supply them.

### A note on C acceleration

`antlr4-python3-runtime` is **pure Python**; there is no optional C extension in the package
itself, so there is nothing to switch on. The known accelerator for ANTLR's Python target is
the third-party **`speedy-antlr-tool`**, which generates a C++ extension that performs the
parse and hands back a Python parse tree. It is deliberately **not** used here: it needs a
C++ toolchain and the ANTLR C++ runtime, which would reintroduce exactly the kind of heavy
build step this lane exists to eliminate. The dependency list stays at one pure-Python
package.

---

## Output contract

`##DEMO##` protocol lines (CONTRACTS section 3), one JSON object per line:

```
##DEMO## {"ev":"start","id":"analytics.nojvm","detail":"lane=plus-nojvm roots=4 gen=nojvm-gen"}
##DEMO## {"ev":"metric","key":"parserReadyMs","value":...}
##DEMO## {"ev":"metric","key":"firstFileMs","value":...}
##DEMO## {"ev":"progress","n":25,"of":351}
##DEMO## {"ev":"done","metrics":{...}}
##DEMO## {"ev":"error","message":"..."}
```

Everything else on stdout is ordinary log, ASCII only. The `done` metrics block is exactly
the CONTRACTS section 4 `analytics` contract -
`lane / files / parseMs / indexMs / q1Ms..q4Ms / startupMs? / peakRssMb? / results` - plus an
`aux` object holding everything else measured (diagnostics, splits, node counts, the deepest
`extends` chain by name). Nothing in `aux` is part of the cross-lane contract.

## Checking the query logic without ANTLR

```sh
py -3 queries.py
```

Runs 12 checks against a hand-built fixture that exercises every Q1-Q4 branch, including
three negative controls (drop a `[metadata]` annotation, break an `extends` edge, introduce
an inheritance cycle) that prove the checks can fail. No ANTLR, no generated parser, no
corpus needed - so a query-semantics regression is catchable in a second.
