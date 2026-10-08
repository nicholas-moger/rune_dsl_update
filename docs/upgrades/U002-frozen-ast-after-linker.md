---
id: U002
subject: Frozen AST after linker
since_fork_version: "0.1.0"
since_pr_at_spec: 35
since_pr_actual: null
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D12"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U002-frozen-ast-after-linker.md
---

# U002 — Frozen AST after linker

## Concept

After `RWorkspace.build(files)` completes (pass-7 validation), every AST
node in the workspace is frozen via `RNode.freeze()`. Subsequent calls to
any setter on a frozen node throw `IllegalStateException` instead of
silently mutating workspace state.

## Why

D12 locked the freeze convention as the P1 concession against full
record-everywhere immutability (deferred to P2). Hard exception (not
`assert`) so consumer JVMs without `-ea` get the protection.

## Structural changes

| Surface | Type |
|---|---|
| `RNode.freeze()` | new public method — idempotent, recurses into `children()` |
| `RNode.isFrozen()` | new public method — query freeze state |
| `RNode.checkMutable()` | new `protected final` guard helper for setter implementations; throws `IllegalStateException` when frozen |
| Every setter on `RNode` and its ~110 concrete subclasses | guarded — calls `checkMutable()` as the first statement of its body |

## BC Story

100% additive at the API surface — no method signatures changed; new
methods are net-new. Compliant consumers (production code that does
not mutate AST nodes after `RWorkspace.build()`) see no behavior change.

Non-compliant consumers (post-build mutation) now hit
`IllegalStateException` instead of silently corrupting workspace state.
This converts a latent bug class into an explicit fail-fast.

`japicmp` Layer-1 reports the new methods as ADDED entries under the D5
budget — non-breaking for downstream consumers.

## How to use

```java
RLinkingResult result = RWorkspace.build(files);
RModel model = result.workspace().files().get(0);

// Read access is unaffected — getters work as before.
List<RDataType> types = model.dataTypes();

// Write access throws post-build:
model.setSourceRange(SourceRange.NONE);   // ← throws IllegalStateException
```

The error message identifies the offending class and source range:

```
java.lang.IllegalStateException: RNode mutated post-freeze: RDataType at
  SourceRange[file=foo.rosetta, startLine=10, startCol=1, endLine=20, endCol=1, ...]
```

If you need to mutate the AST, do it during the `AstBuilder` phase (pre-build),
not after `RWorkspace.build()` returns.

## Migration

No change required for compliant consumers — production code does not
mutate AST nodes after `RWorkspace.build()`. If you see a new
`IllegalStateException` post-upgrade, the call site is performing a
mutation that was always undefined behaviour; the freeze flag turns
that latent bug into an explicit failure.

The fix is to perform the mutation pre-`build()` (during the AstBuilder
phase). No deprecation shim needed because no compliant consumer
depends on post-build mutation.

## Test coverage

- `RNodeFreezeTest` — direct coverage of `freeze()` / `isFrozen()` /
  `checkMutable()` semantics on `RNode` and a representative subclass set;
  asserts setter-call exceptions post-freeze.
- `RNodeChildrenCoverageTest` — reflective sweep across every concrete
  `RNode` subclass that locks the `children()` declaration-coverage
  invariant: each subclass declares a `children()` method returning a
  `List`, and the children-field reflective walk recognises `RNode` /
  `Collection<? extends RNode>` typed fields. This is NOT a freeze test
  itself, but freeze recursion (see `RNodeFreezeTest`) depends on this
  invariant — if a subclass omits `children()`, `freeze()` cannot recurse
  into it.
- `BindFreezeResolveLifecycleTest` — end-to-end lifecycle test exercising
  the bind-freeze-resolve sequence as `RWorkspace.build(files)` drives
  it.

## Coverage

The freeze flag guards **setter methods only**. Collection-returning
getters on AST nodes (e.g., `RDataType.attributes()`, `RFunction.inputs()`,
etc.) typically expose the underlying `List` / `ArrayList` directly, per
the class-level "list fields exposed directly" note on `RNode`. A caller
that does `node.attributes().add(newItem)` can still mutate the AST
post-freeze **without** hitting `checkMutable()`.

Treat returned collections as read-only. Wrapping every collection getter
in an unmodifiable view at freeze time is deferred to a future PR
(records-everywhere aspiration in P2 — see `ir-design.md` §4.5).

The freeze convention covers all 110 concrete `RNode` subclasses; every
declared setter is guarded.

## Cross-references

- **D12** (`the development decision log`, local) — locks freeze
  convention as the P1 concession against full immutability.
- **Audit doc §6 Q6** — original audit-hook framing.
- **`ir-design.md` §4.5** — records-everywhere aspiration deferred to P2
  (the unmodifiable-collection-views follow-up).
- **U001** — SourceRange byte offsets (sibling P1.4.1 surface).
- **U003** — RNode metadata slot (sibling P1.4.1 surface, complementary
  to freeze convention).
- **`docs/features/INDEX.md`** — catalogue card for U002.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
