---
id: U003
subject: RNode metadata slot
since_fork_version: "0.1.0"
since_pr_at_spec: 35
since_pr_actual: null
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D21"]
related_audit_hooks: ["H6"]
related_parity_constraints: ["#17"]
doc_path: U003-rnode-metadata-slot.md
---

# U003 — RNode.metadata immutable slot

## Concept

`RNode` carries a `private final Map<String, Object> metadata` field
exposed via `node.metadata()`. The map is defensively copied at
construction and is unmodifiable on read. The slot is reserved for
future-phase IR augmentation passes (per `ir-design.md` §1
"augment AST, not replace") — populating logic lands in later
P1.4.x phases.

## Why

D21 reserves the slot for future-phase IR augmentation passes per the
"augment AST, not replace" pattern. P1.4.1 scaffolds the slot;
P1.4.3 IR scaffolding (D21) populates it via the parallel IR
type surface.

## Structural changes

| Surface | Type |
|---|---|
| `RNode` private final field `metadata` | `Map<String, Object>`, defensively copied at construction, unmodifiable on read |
| `RNode.metadata(): Map<String, Object>` | accessor — guaranteed non-null; empty by default |
| `RNode()` constructor | NONE — empty metadata, NONE source range |
| `RNode(SourceRange range)` constructor | NONE — empty metadata, given range |
| `RNode(SourceRange range, Map<String,Object> m)` constructor | NONE — populated, defensive copy; `null` argument coerced to shared empty-map singleton |

The 3-constructor cascade preserves source-compat for existing subclasses
(none of which call `super(...)` explicitly today).

## BC Story

100% additive at the API surface. Existing subclasses inherit the new
field via the no-arg constructor (default path); none of the 110
concrete `RNode` subclasses call `super(...)` explicitly today, so
none need modification. New methods are net-new.

`japicmp` Layer-1 reports the new methods + field as ADDED entries under
the D5 budget — non-breaking for downstream consumers.

`null` metadata arguments to the 3-arg constructor are coerced to a
shared empty-map singleton (no NPE, no allocation) — defensive against
caller mistakes without breaking source-compat.

## How to use

```java
Map<String, Object> meta = node.metadata();
// guaranteed non-null; empty by default
Object resolvedSymbol = meta.get("resolvedSymbol");
if (resolvedSymbol != null) {
    // ...
}
```

Population is owned by phase-specific passes (e.g. the IR augmentation
pass scaffolded under D21 / U012). Until those passes wire population
logic, every `node.metadata()` returns the empty-map singleton.

## Migration

No change required. New code can read `node.metadata()` and treat it as
empty until populated by phase-specific passes. Existing AST traversal
code is unaffected.

## Test coverage

The `RNode.metadata()` slot does NOT have a dedicated contract test today.
Its semantic guarantees (non-null, defensive-copy at construction,
unmodifiable on read, `null`-arg coercion to shared empty-map singleton)
are enforced at the implementation level only.

The parallel `IRNode.metadata()` surface (U012 — since PR #464 homed in
the `rune-ir` module) IS contract-tested via
`rune-ir/src/test/java/com/regnosys/rosetta/ir/contract/MetadataContractTest.java`
— that test locks `IRNode.metadata()`'s non-null + unmodifiable contract
semantics on `RecordIRNode`. It provides a template for a future
`RNode.metadata()` companion contract test (a P2 candidate per D21
records-everywhere aspiration). U003's surface today is parallel-scaffolded
but unpopulated; population logic + a dedicated companion test are part of
later P1.4.x or P2 work.

## Coverage

The metadata slot is hoisted to `RNode` itself, so all 110 concrete
`RNode` subclasses inherit the slot. Population in P1.4.1 is `null` /
empty across the board — the slot is reserved (D21) but not yet
populated by any pass. P1.4.3 IR scaffolding (U012 / D21) introduces
the parallel IR type surface that future augmentation passes will
populate via `metadata()`.

## Cross-references

- **D21** (`the development decision log`, local) — IR-scaffolding
  envelope; reserves `metadata` slot as the future-pass attachment point.
- **U012** — IR scaffolding type surface (P1.4.3) — the parallel IR
  surface that future augmentation passes use the metadata slot to
  attach payloads to `RNode` instances.
- **Audit doc §3 hook H6** — original audit-hook framing.
- **`ir-design.md` §1** — augment-pattern (the architectural decision
  D21 implements).
- **U001** — SourceRange byte offsets (sibling P1.4.1 surface).
- **U002** — Frozen AST after linker (sibling P1.4.1 surface).
- **`docs/features/INDEX.md`** — catalogue card for U003.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
