---
id: U007
subject: LSP SymbolKind projection
since_fork_version: "0.1.0"
since_pr_at_spec: 36
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D8"]
related_audit_hooks: ["H12"]
related_parity_constraints: []
doc_path: U007-lsp-symbol-kinds.md
---

# U007 — LSP SymbolKind projection

## Concept

`RuneSymbolKind` exposes LSP 3.17 SymbolKind constants (raw `int`
values matching the LSP wire encoding) and a `forNode(RNode): int`
dispatch that maps named-symbol AST subclasses to their LSP kind.
Consumers serialise the returned value directly into LSP
`textDocument/documentSymbol` responses without any wrapping conversion
layer.

## Why

LSP `textDocument/documentSymbol` requires a `SymbolKind` `int` value
per symbol. Without a centralised mapping, each LSP integration would
have to re-derive the projection from `RNode` subclass to LSP kind.
D8 records the canonical projection so server implementations stay
consistent across tooling. P4 (LSP server) is the primary consumer
target; this surface is the projection it will read from.

## Structural changes

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.lsp.RuneSymbolKind` | new final class |
| `RuneSymbolKind` constants | 26 LSP 3.17 SymbolKind `int` values |
| `RuneSymbolKind.forNode(RNode): int` | static dispatch over the named-symbol RNode subclasses; throws `IllegalArgumentException` on structural-only nodes |

### Mapping table

| RNode subclass | SymbolKind | Constant value |
|---|---|---|
| `RDataType` | CLASS | 5 |
| `REnumeration` | ENUM | 10 |
| `RChoice` | INTERFACE | 11 |
| `RFunction` | FUNCTION | 12 |
| `REnumValue` | ENUM_MEMBER | 22 |
| `RAttribute` | FIELD | 8 |
| `RBasicType` | CLASS | 5 |
| `RRecordType` | STRUCT | 23 |
| `RTypeAlias` | CLASS | 5 |

Structural-only RNode subclasses (expressions, options, etc.) are
unmapped — `forNode` throws `IllegalArgumentException`. This is by
design: fail-loud on hierarchy additions catches missing mappings at
runtime.

## BC Story

100% additive. No existing API changes. The class is new; nothing
existed prior. `japicmp` Layer-1 reports the new class + methods as
ADDED entries under the D5 budget — non-breaking for downstream
consumers.

The structural-only fail-loud semantic is the deliberate design
contract; consumers calling `forNode` on a structural-only node receive
an `IllegalArgumentException` rather than a default value. This forces
new RNode subclasses (when added) to have an explicit decision recorded
in the mapping table — silent default-fall-through would mask the
hierarchy growth.

## How to use

```java
import com.regnosys.rosetta.lsp.RuneSymbolKind;

RNode node = ...;  // typically an RDataType, REnumeration, ...
int symbolKind = RuneSymbolKind.forNode(node);
// serialise directly into LSP textDocument/documentSymbol response
```

For structural-only nodes (e.g., expressions), guard with an
`instanceof` check or catch the `IllegalArgumentException`:

```java
try {
    int kind = RuneSymbolKind.forNode(node);
    // ... emit symbol
} catch (IllegalArgumentException e) {
    // structural-only node — do not emit as a documentSymbol
}
```

## Migration

No migration required — the surface is net-new. New consumer code may
adopt `RuneSymbolKind.forNode` where it builds LSP responses or tooling
metadata. Existing AST-walking code that serialises symbol-kind values
ad-hoc may switch to `RuneSymbolKind.forNode` for consistency.

## Test coverage

- `RuneSymbolKindTest` (under
  `rune-parser/src/test/java/com/regnosys/rosetta/lsp/`) — locks the
  projection table: each of the 9 mapped `RNode` subclass → SymbolKind
  pairs returns the documented `int` value; structural-only subclasses
  throw `IllegalArgumentException`; all 26 constant values match
  LSP 3.17 wire encoding.

## Coverage

The mapping covers 9 of the named-symbol `RNode` subclasses today (per
the Mapping table above). Structural-only subclasses are deliberately
unmapped — fail-loud on hierarchy growth.

P4 LSP server work will wire `RuneSymbolKind.forNode` into
`textDocument/documentSymbol` response building when the LSP track
opens. Until then, the surface is locked but unconsumed in production
code paths.

## Cross-references

- **Audit hook H12** — the development audit "p1-parser-architectural-audit".
- **Spec §6.6** —
  `docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md`
  (local).
- **D8** (`the development decision log`, local) — LSP symbol-kind
  mapping decision.
- **LSP spec:**
  https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#textDocument_documentSymbol
- **U005** — SymbolId opaque-handle cross-references — sibling P1.4.1
  surface; future LSP integrations will use SymbolId for definition
  links alongside SymbolKind.
- **U006** — Structured parser diagnostics — sibling P1.4.1c surface;
  LSP tooling consumes both diagnostics + document symbols.
- **`docs/features/INDEX.md`** — catalogue card for U007.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
