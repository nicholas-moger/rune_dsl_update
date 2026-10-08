---
id: U004
subject: AstInterfaceRegistry visitor pattern
since_fork_version: "0.1.0"
since_pr_at_spec: 35
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: ["H9"]
related_parity_constraints: []
doc_path: U004-ast-interface-registry.md
---

# U004 — AstInterfaceRegistry visitor pattern

## Concept

`AstInterfaceRegistry<T>` is a typed dispatch helper that routes calls to
per-marker-interface handlers. Register handler functions per marker
interface; calling `dispatch(node)` selects the handler whose interface
the node implements via `Class#isInstance` matching. A wrapping
`AstVisitor<T>.withInterfaceRegistry(reg)` consults the registry first
and falls back to the base visitor when no handler matches.

## Why

`ir-design.md` §5 (skill reference) records the rationale: the rune-parser
codebase has a number of cross-cutting walks (validation, codegen,
diagnostics) that previously dispatched via `instanceof` chains. Those
chains grow over time and become hard to read; the registry encodes the
same dispatch declaratively while keeping `instanceof` available where
the chain stays small. The PR-A regression captured in
`feedback_dual_read_flaw.md` (memory) was driven in part by ad-hoc
dispatch fragility — the registry pattern reduces the surface where that
class of bug can land.

## Structural changes

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.ast.util.AstInterfaceRegistry<T>` | new public class — generic typed registry |
| `AstInterfaceRegistry.register(Class<I>, Function<I, T>)` | builder method — fluent registration; returns `this` |
| `AstInterfaceRegistry.dispatch(Object)` | `T` accessor — `null` return reserved for "no registered interface matches"; throws `IllegalStateException` on multiple-handler ambiguity; throws `IllegalStateException` when a handler returns `null` |
| `AstVisitor.withInterfaceRegistry(AstInterfaceRegistry<T>)` | new default method — returns wrapping visitor that consults registry first, falls back to base visitor's `visit()` |

## BC Story

100% additive at the API surface. `AstVisitor` adds a default method;
existing visitor implementations are unaffected. New consumers may opt
in to the registry where it improves readability.

The handler-non-null contract reserves `null` exclusively for "no
registered interface matches" so that
`AstVisitor.withInterfaceRegistry(reg)` can fall through to the base
visitor unambiguously. A handler that returns `null` triggers
`IllegalStateException` — fail-loud rather than silent. Multi-interface
ambiguity (a node implementing two registered interfaces) is also
fail-loud: `dispatch` throws `IllegalStateException` listing the
matches.

## How to use

```java
AstInterfaceRegistry<String> reg = new AstInterfaceRegistry<String>()
    .register(HasName.class, n -> "name:" + n.name())
    .register(HasShape.class, s -> "shape:" + s.shape());

String result = reg.dispatch(node);   // null if no match
```

Wrap an `AstVisitor` to consult the registry first:

```java
AstVisitor<String> base = ...;
AstVisitor<String> withReg = base.withInterfaceRegistry(reg);
String out = withReg.visit(node);
```

For `Optional<X>` semantics (legitimate "absent value" handlers),
parameterise the registry with `Optional<X>`:

```java
AstInterfaceRegistry<Optional<String>> opt = new AstInterfaceRegistry<>()
    .register(HasName.class, n -> Optional.of(n.name()))
    .register(HasMaybeName.class, n -> Optional.empty()); // legitimate absent
```

## Migration

Existing `instanceof` chains keep working; switch to the registry only
where the chain has grown unwieldy or readers benefit from declarative
handler registration. No deprecation; no removal schedule.

If a node implements two registered interfaces, `dispatch` throws
`IllegalStateException` listing the matches:

```java
reg.register(HasName.class, ...).register(HasShape.class, ...);
reg.dispatch(namedAndShaped);
// → IllegalStateException: Multiple handlers match NamedAndShaped: HasName, HasShape
```

Callers wanting "first match wins" semantics should combine the
interfaces into a single combined interface (or wrap the registry).

## Test coverage

- `AstInterfaceRegistryTest` (under
  `rune-parser/src/test/java/com/regnosys/rosetta/ast/registry/`) —
  locks the registry contract: registration semantics, `isInstance`
  routing, multi-handler ambiguity throws, handler-non-null contract,
  `null`-return-by-handler fail-loud, registry-first /
  base-visitor-fallback ordering when wrapped via
  `AstVisitor.withInterfaceRegistry(reg)`.

## Coverage

The registry is generic (`<T>`) — the same surface can be parameterised
to any return type. The supplied `AstVisitor.withInterfaceRegistry`
default method is the canonical wiring path for the visitor pattern; ad
hoc `dispatch(...)` calls are also supported for non-visitor consumers.

Adoption today is opt-in. Existing `instanceof` chains in
`rune-parser` and `rune-java-generator` remain in place and have not
been migrated wholesale; per-call-site migration is part of follow-on
work as call-sites are touched.

## Cross-references

- **`ir-design.md` §5** (skill reference, local) — original design
  rationale for declarative handler registration.
- **Audit doc §3 hook H9** — original audit-hook framing
  (the development audit "p1-parser-architectural-audit").
- **`feedback_dual_read_flaw.md`** memory (local) — PR-A regression
  driven in part by ad-hoc dispatch fragility; the registry pattern
  reduces the surface where this class of bug can land.
- **U001 / U002 / U003** — sibling P1.4.1 internal-hardening surfaces.
- **`docs/features/INDEX.md`** — catalogue card for U004.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
