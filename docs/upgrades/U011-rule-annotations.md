---
id: U011
subject: Rule-level rune annotations
since_fork_version: "0.1.0"
since_pr_at_spec: 37
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: ["H11"]
related_parity_constraints: ["#9"]
doc_path: U011-rule-annotations.md
---

# U011: Rule-level rune annotations

## Concept

Free reuse of the U008 rune-annotation infrastructure on `reporting rule` and
`eligibility rule` declarations. Zero new AST surface beyond what U008
introduced — this entry exists to document the additional grammar attach
site.

## Why

Rule declarations are first-class user-authored top-level constructs. A
strict reading of U008's "user-authored" attach-site rationale required a
separate documentation entry once `RRule` joined the attached set during
P1.4.2 implementation. Tracking the H11 attach as a distinct U-NNN keeps the
upgrade-manifest signal honest: an `RRootElement` kind landing as an attach
site is itself a documented event, even when the underlying machinery is
already shipped.

## Structural changes

```antlr
rosettaRule : runeAnnotations? (REPORTING | ELIGIBILITY) RULE validID ... ;
```

Both `reporting rule` and `eligibility rule` variants accept the optional
prefix.

### AST surface

`runeAnnotations()` on `RRule` — inherited from `RRootElement` per U008
hoist. **No class-specific accessor.** The slot existed on every
`RRootElement` subclass since U008; H11 grammar attaches the parser-side
collection of values into that pre-existing slot.

### Diagnostics

Reuses U008's `RUNE-016` (Invalid rune annotation argument) and `RUNE-019`
(Invalid rune annotation name) reservations. Today the generic ANTLR error
path classifies malformed input as `RUNE-001` (UnexpectedToken) or `RUNE-009`
(MismatchedInput). Same fallback semantics for rule-level annotations as for
the other H2 attach sites.

## BC Story

- **100% additive.** Every existing rule definition parses identically with
  empty `runeAnnotations()` list.
- **Verified** against `BackCompatCorpusOrthogonalityTest` — the
  additive-orthogonality gate that asserts every legacy rule definition
  produces an empty `runeAnnotations()` list (runs in CI per cell post-P1.7
  PR-3 T5 via the corpus-regression matrix; matrix aggregate covers all
  1,628 fixtures when fully populated locally — see
  `docs/bc-verification.md` top-of-doc CI/local table).
  `AstCorpusRegressionTest` provides AST well-formedness coverage as a
  separate gate (Layer 2; also runs in CI per cell post-P1.7 PR-3 T5).
- **No new AST classes.** Strictly a grammar attach-site addition; the AST
  slot was hoisted in U008.
- **japicmp Layer-1 stays non-breaking.** No new public surface; report
  unchanged for this entry.

## How to use

```rosetta
@experimental
reporting rule TradeId
    return data Trade -> tradeIdentifier

@experimental(feature = "preview")
eligibility rule IsReportable
    filter data Trade -> isCleared = True
```

Read access on the AST is identical to U008 — `RRule.runeAnnotations()`
returns the live list inherited from `RRootElement`.

## Migration

No migration required — purely additive grammar attach site. Existing rule
definitions compile unchanged.

## Test coverage

- `RuleAnnotationParseTest` — parses both `reporting rule` and `eligibility
  rule` branches with single + multi-value annotation prefixes.
- `BackCompatCorpusOrthogonalityTest` — locks the additive-orthogonality
  invariant (runs in CI per cell post-P1.7 PR-3 T5 via the
  corpus-regression matrix; matrix aggregate covers all 1,628 fixtures
  when fully populated locally — see `docs/bc-verification.md`
  top-of-doc CI/local table): every legacy rule definition has empty
  `runeAnnotations()` list.
- `AstCorpusRegressionTest` — AST well-formedness gate (lexer → parser
  → AstBuilder pipeline integrity). Per `docs/bc-verification.md` Layer 2A.

## Coverage

### Attach-site coverage (P1.4.2 ship)

`RRule` is **1 of the 5 attached kinds** in P1.4.2's H2/H11 ship — see U008
"Attach-site coverage" for the full 5-of-18 table and rationale. This entry
documents the H11 grammar wiring side; U008 owns the AST surface side.

| Attached via this U-entry | Other H2 sites (U008) |
|---|---|
| `reporting rule` (`RRule`) | `RDataType` |
| `eligibility rule` (`RRule`) | `RChoice` |
| | `REnumeration` |
| | `RFunction` |

**Forward-extension plan.** Same as U008 — the 13 unattached kinds will be
re-evaluated against corpus-audit findings under D22 (P1.4.3 decision-log
entry, local).

## Cross-references

- **U008** — Rune annotations (foundation feature this entry depends on).
  Source of the AST surface; this U-entry adds the grammar attach.
- **D22** — IR-scaffolding envelope (`the development decision log`, local) —
  governs the planned attach-site extension PR.
- **Spec:** `docs/superpowers/specs/2026-04-28-p1.4.2-grammar-group-design.md`
  § 7.
- **Audit:** the development audit "p1-parser-architectural-audit" § 3.1 H11 entry.
- **Diagnostics:** `docs/diagnostics/RUNE-016.md`, `RUNE-019.md`.
