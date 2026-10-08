---
id: U001
subject: SourceRange byte offsets
since_fork_version: "0.1.0"
since_pr_at_spec: 35
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D13"]
related_audit_hooks: ["H5"]
related_parity_constraints: ["#12"]
doc_path: U001-bytewise-source-range.md
---

# U001 — SourceRange byte offsets

## Concept

`SourceRange` carries UTF-8 byte offsets (`startOffset` / `endOffset`) into
the source file alongside the existing line/column coordinates. Byte
offsets are populated when the AST is built via the 2-arg
`AstBuilder(String fileName, String content)` constructor; the
deprecated 1-arg constructor returns sentinel `OFFSETS_UNKNOWN = -1`.

## Why

D13 locked byte offsets as the source-position storage strategy: invariant
under encoding changes, aligned with Source Map v3 (ECMA-426) and LLVM
DWARF, and required for incremental compilation primitives in P1.4.4.

## Structural changes

| Surface | Type |
|---|---|
| `SourceRange` canonical record constructor | 5-arg → 7-arg (binary break for direct `new SourceRange(...)` callers) |
| `SourceRange.startOffset(): int` | accessor — UTF-8 byte offset, `-1` sentinel when unset |
| `SourceRange.endOffset(): int` | accessor — UTF-8 byte offset, half-open `[start, end)` |
| `SourceRange.hasByteOffsets(): boolean` | predicate — prefer over raw `-1` checks |
| `SourceRange.OFFSETS_UNKNOWN` | constant — sentinel value (`-1`); may change in a future minor version |
| `SourceRange.of(startToken, stopToken, file, charToByteTable)` | new canonical factory used internally by `AstBuilder` |
| `SourceRange.of5Arg(file, sl, sc, el, ec)` | legacy bridge, `@Deprecated(since = "0.1.0", forRemoval = false)`, returns sentinel offsets |
| `AstBuilder(String fileName)` | existing 1-arg ctor — produces sentinel offsets |
| `AstBuilder(String fileName, String content)` | new 2-arg ctor — builds `int[]` char-to-byte projection table once and threads it through to `SourceRange.of(...)` at every node-creation site; produces populated byte offsets |

## BC Story

The 5-arg → 7-arg canonical-constructor change is binary-incompatible for
any code calling `new SourceRange(file, sl, sc, el, ec)` directly.
Verified fork-internal-only at spec time (3 production call-sites + 2
test sites, all in `rune-parser`, all migrated in PR P1.4.1a). External
consumers compiling against 0.0.x JARs (none exist today — this is the
first release) will need to recompile against 0.1.0+ headers.

The deprecated `SourceRange.of5Arg` legacy bridge preserves source-compat
for any caller that still wants the old shape; it returns sentinel
offsets so legacy call-sites continue to work without populated byte
offsets. `AstBuilder`'s 1-arg constructor remains supported for the same
reason.

## How to use

### Preferred constructor (populates byte offsets)

```java
String content = Files.readString(file);
AstBuilder b = new AstBuilder(file.toString(), content);
RModel model = b.build((RosettaParser.RosettaModelContext) parseResult.tree());
```

### Reading offsets

```java
if (range.hasByteOffsets()) {
    int start = range.startOffset();
    int end = range.endOffset();
    // half-open [start, end), UTF-8 bytes into file
}
```

Never branch on raw `-1` checks; the sentinel value may change in a future
minor version. Use `hasByteOffsets()` predicate.

### Constructor migration

Previous (5-arg) signature is now a binary break for direct
`new SourceRange(...)` callers. Replacements:

```java
// New canonical factory (preferred — used internally by AstBuilder)
SourceRange r = SourceRange.of(startToken, stopToken, file, charToByteTable);

// Legacy bridge (deprecated, returns sentinel offsets)
SourceRange r = SourceRange.of5Arg(file, sl, sc, el, ec);
// equivalent: new SourceRange(file, sl, sc, el, ec, -1, -1)
```

## Migration

No migration required for compliant consumers — the 1-arg `AstBuilder`
constructor + the deprecated `SourceRange.of5Arg` bridge keep legacy
shapes working with sentinel offsets.

For consumers that need populated byte offsets:

1. Read source content as `String` before building the AST.
2. Use the 2-arg `AstBuilder(fileName, content)` constructor.
3. Read offsets via `range.hasByteOffsets()` + `range.startOffset()` /
   `range.endOffset()`.

## Test coverage

- `SourceRangeByteOffsetTest` — direct unit-level coverage of
  `SourceRange.of(startToken, stopToken, file, charToByteTable)` factory
  + `hasByteOffsets()` predicate semantics.
- `SourceRangePropertyTest` — Jqwik property tests over generated
  `SourceRange` instances exercising offset invariants.
- `CharToByteOffsetsTest` — coverage of the `CharToByteOffsets` projection
  table that maps character indices to UTF-8 byte offsets.
- `AstCorpusByteOffsetTest` — corpus-wide byte-offset population gate;
  every AstBuilder-produced node receives offsets when built via the 2-arg
  constructor over the populated test corpus.
- `AstBuilderSourceRangesTest` — `AstBuilder` source-range population
  semantics (carryover from the M-track work that preceded the byte-offset
  surface).

## Coverage

Byte-offset population coverage spans every AST node produced by the
`AstBuilder` pipeline when invoked via the 2-arg `AstBuilder(fileName, content)`
constructor. The 1-arg constructor path remains supported and produces
sentinel `OFFSETS_UNKNOWN = -1` offsets — every `SourceRange` instance
carries the offset slot, but only the 2-arg path populates it.

The deprecated `SourceRange.of5Arg(...)` factory is the last legacy shape
that still produces sentinel offsets without going through `AstBuilder`;
its retention is intentional (binary-compat bridge) but additive
new code should use the canonical 7-arg shape via
`SourceRange.of(startToken, stopToken, file, charToByteTable)`.

## Cross-references

- **D13** (`the development decision log`, local) — locks byte-offset
  storage strategy.
- **Audit doc §2.6 + §6 Q7** — original audit-hook framing.
- **ECMA-426 (TC39):** https://tc39.es/ecma426/ — Source Map v3 spec.
- **LSP 3.17 `positionEncoding`:** server advertises `["utf-16", "utf-8"]`,
  client picks; conversion at the LSP boundary.
- **U002** — Frozen AST after linker (sibling P1.4.1 surface).
- **`docs/features/INDEX.md`** — catalogue card for U001.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
