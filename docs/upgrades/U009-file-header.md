---
id: U009
subject: File-level header block
since_fork_version: "0.1.0"
since_pr_at_spec: 37
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: ["H3"]
related_parity_constraints: ["#11"]
doc_path: U009-file-header.md
---

# U009 — File-level header block

## Concept

An optional `fileHeader:` block at the very start of a `.rosetta` source
file, before the `namespace` declaration. Carries file-level metadata:
version, dependencies, experimental feature opt-ins. Distinct from the
existing namespace-level `versionDecl` — both mechanisms co-exist and
serve different purposes (file-level vs namespace-level).

## Why

The existing namespace-level `versionDecl` was designed for the FpML / CDM
upstream and embeds version metadata inside the namespace surface. The
rune fork needs a parallel file-level surface that can carry metadata
that doesn't fit cleanly into a single namespace declaration —
specifically: cross-namespace `depends-on` lists and per-file
experimental feature opt-ins. P1.4.2 audit hook H3 captures the design
constraint: keep the file-level surface grammatically independent of
`versionDecl` so the two co-exist without ambiguity.

## Structural changes

### Grammar surface

```antlr
rosettaModel : fileHeader? OVERRIDE? NAMESPACE ... ;

fileHeader        : FILE_HEADER COLON versionField? dependsOnField? experimentalField? ;
versionField      : VERSION STRING ;
dependsOnField    : DEPENDS_ON STRING (COMMA STRING)* ;
experimentalField : EXPERIMENTAL COLON LBRACK validID (COMMA validID)* RBRACK ;
```

New lexer tokens: `FILE_HEADER` (`'fileHeader'`), `DEPENDS_ON`
(`'depends-on'`), `EXPERIMENTAL` (`'experimental'`).

### AST surface

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.ast.RFileHeader` | mutable class, extends `RNode` |
| `RFileHeader.version(): Optional<String>` | accessor |
| `RFileHeader.dependsOn(): List<String>` | live list |
| `RFileHeader.experimental(): List<String>` | live list |
| `com.regnosys.rosetta.ast.model.RModel.fileHeader(): Optional<RFileHeader>` | accessor |
| `RModel.setFileHeader(RFileHeader)` | mutator with `checkMutable()` guard |

### Diagnostics

`RUNE-017` (Invalid fileHeader field) is the dedicated code point for
fileHeader-specific failures. Today the generic ANTLR error path
classifies malformed `fileHeader:` fields as `RUNE-001` (UnexpectedToken)
or `RUNE-009` (MismatchedInput); future `RuneErrorClassifier` refinement
(with rule-context awareness) will route fileHeader-specific failures to
RUNE-017. See `docs/diagnostics/RUNE-017.md`.

## BC Story

100% additive — files without `fileHeader:` produce `Optional.empty()`
on `RModel.fileHeader()`. Verified against
`BackCompatCorpusOrthogonalityTest` (1,628 fixtures when populated) —
the additive-orthogonality gate that asserts every legacy fixture's
`RModel.fileHeader() == Optional.empty()`. `AstCorpusRegressionTest`
provides AST well-formedness coverage as a separate gate (per
`docs/bc-verification.md` Layer 2).

Existing namespace-level `versionDecl` (`RModel.version()`) unaffected.
The two versions are grammatically distinct (file-level vs
namespace-level) and represent different concepts.

Soft-keywords `fileHeader` and `experimental` admitted to `validID` so
existing identifiers in non-`fileHeader` positions continue to lex as
IDs. `depends-on` is hyphenated and cannot collide with any existing
identifier (regex excludes `-`).

## How to use

```rosetta
fileHeader:
    version "1.0.0"
    depends-on "core-domain", "regulatory-extensions"
    experimental: [strict-types, byte-offsets]

namespace com.example.contracts

type Foo:
    field1 string (1..1)
```

Read access on the AST:

```java
RModel model = ...;
Optional<RFileHeader> header = model.fileHeader();
header.ifPresent(h -> {
    Optional<String> ver = h.version();
    List<String> deps = h.dependsOn();
    List<String> features = h.experimental();
});
```

## Migration

No migration required — additive surface only. Authors may opt in by
adding a `fileHeader:` block at the top of new files; legacy files are
unaffected.

## Test coverage

- `FileHeaderE2eTest` (under
  `rune-parser/src/test/java/com/regnosys/rosetta/parser/`) — end-to-end
  parsing + AST population coverage for the `fileHeader:` block: each of
  the optional fields (version / dependsOn / experimental), combined
  presence, and additive interaction with `versionDecl`.
- `BackCompatCorpusOrthogonalityTest` — locks the additive-orthogonality
  invariant: every legacy fixture's `RModel.fileHeader() ==
  Optional.empty()` (~1,628 fixtures when populated).
- `AstCorpusRegressionTest` — AST well-formedness gate (lexer → parser →
  AstBuilder pipeline integrity).

## Coverage

This release is **Layer-1 only.** Field values are parsed and stored.
The `experimental` list does **not** activate any features in this PR —
that's the W13 feature-registry runtime, deferred to P2.5 per the
2026-05-06 scope-honest closeout. `dependsOn` is metadata only; no
dependency resolution or fetching is wired.

## Cross-references

- **Spec § 5** —
  `docs/superpowers/specs/2026-04-28-p1.4.2-grammar-group-design.md`
  (local).
- **Audit § 3.1 H3 entry** —
  the development audit "p1-parser-architectural-audit".
- **Diagnostics:** `docs/diagnostics/RUNE-017.md`.
- **U006** — Structured parser diagnostics — defines the `RuneErrorCode`
  surface that hosts RUNE-017.
- **U010** — regulatoryReference named-arg list — sibling P1.4.2
  grammar-group surface.
- **W13 (deferred to P2.5)** — feature-registry runtime that activates
  `@experimental(...)` markers; this U009 surface is the parser-side
  source for `experimental` opt-ins.
- **`docs/features/INDEX.md`** — catalogue card for U009.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
