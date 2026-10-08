---
id: U010
subject: regulatoryReference named-arg list
since_fork_version: "0.1.0"
since_pr_at_spec: 37
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: ["H4"]
related_parity_constraints: ["#8"]
doc_path: U010-regulatory-reference-named-args.md
---

# U010 — regulatoryReference named-arg list

## Concept

An optional `(name = "value", ...)` suffix after the positional segments
of a `regulatoryReference` clause inside a `docReference` block. Lets
authors carry typed metadata (jurisdiction, effective date, applicability
scope, etc.) on regulatory citations without changing the existing
positional grammar.

## Why

The existing positional `regulatoryReference` grammar accommodates the
common citation shape (publisher + body + id) but offers no extension
point for jurisdiction-level metadata that downstream regulatory
tooling needs (effective dates, applicability scopes, jurisdictional
overrides). P1.4.2 audit hook H4 framed the gap: extend the surface
without breaking existing positional usage. The named-arg suffix is
LL(*)-safe vs the existing `documentRationale*` continuation and
preserves backwards compatibility for every legacy fixture.

## Structural changes

### Grammar surface

```antlr
docReference
    : LBRACK (REGULATORY_REFERENCE | DOC_REFERENCE)
      (FOR annotationPathExpression)?
      regulatoryDocumentReference
      regulatoryReferenceArgs?            // NEW
      documentRationale*
      ...
    ;

regulatoryReferenceArgs : LPAREN namedArg (COMMA namedArg)* RPAREN ;
namedArg                : validID EQ STRING ;
```

LL(*) safe: the optional `LPAREN` is unique vs `documentRationale`'s
`RATIONALE` / `RATIONALE_AUTHOR` keywords. Closing `RPAREN` returns
control to `docReference`.

### AST surface

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.ast.regulatory.RRegulatoryReferenceArg` | mutable class, extends `RNode` |
| `RRegulatoryReferenceArg.name(): String` | accessor |
| `RRegulatoryReferenceArg.value(): String` | accessor (raw string from STRING token) |
| `RDocReference.namedArgs(): List<RRegulatoryReferenceArg>` | live list accessor |
| `RDocReference.addNamedArg(RRegulatoryReferenceArg)` | mutator with `checkMutable()` guard |

### Diagnostics

`RUNE-018` (Invalid regulatoryReference argument) is the dedicated code
point for regulatoryReference-arg-specific failures. Today the generic
ANTLR error path classifies malformed `(k="v",...)` args as `RUNE-001`
(UnexpectedToken) or `RUNE-009` (MismatchedInput); future
`RuneErrorClassifier` refinement (with rule-context awareness) will
route regulatoryReference-arg-specific failures to RUNE-018. See
`docs/diagnostics/RUNE-018.md`.

## BC Story

100% additive — legacy positional regulatoryReference syntax parses
unchanged. Verified against `BackCompatCorpusOrthogonalityTest` (1,628
fixtures when populated) — the additive-orthogonality gate that
asserts every legacy fixture's `RDocReference.namedArgs() == empty
list`. `AstCorpusRegressionTest` provides AST well-formedness coverage
as a separate gate (per `docs/bc-verification.md` Layer 2).

Default empty list — consumers seeing `namedArgs().isEmpty()` see
today's behaviour.

Co-exists with `documentRationale*` —
`[regulatoryReference ESMA ... (jurisdiction="EU") rationale "..."]` is
fully supported.

## How to use

```rosetta
type Foo:
    [regulatoryReference ESMA EMIR "Article 9"
        (jurisdiction = "EU", effectiveDate = "2020-06-18")
        rationale "Mandatory clearing requirements"]
    field1 string (1..1)
```

Read access on the AST:

```java
RDocReference doc = ...;
List<RRegulatoryReferenceArg> args = doc.namedArgs();
for (RRegulatoryReferenceArg arg : args) {
    String name = arg.name();      // "jurisdiction"
    String value = arg.value();    // "EU"
}
```

## Migration

No migration required — additive surface only. Authors may opt in by
adding named-args after the positional segments where useful.

## Test coverage

- `RegulatoryReferenceArgsE2eTest` (under
  `rune-parser/src/test/java/com/regnosys/rosetta/parser/`) — end-to-end
  parsing + AST population coverage for the named-arg suffix on
  `regulatoryReference`: single-arg, multi-arg, presence alongside
  `documentRationale`.
- `BackCompatCorpusOrthogonalityTest` — locks the additive-orthogonality
  invariant: every legacy fixture's `RDocReference.namedArgs() == empty
  list` (~1,628 fixtures when populated).
- `AstCorpusRegressionTest` — AST well-formedness gate.

## Coverage

This release is **Layer-1 only.** Values are stored verbatim as strings.
Typed reconstruction (parsing dates / numbers from STRING values) is
out of scope for the parser — that's a validation-phase concern on the
typed AST. Specifically: `effectiveDate = "2020-06-18"` is stored as
the literal string `"2020-06-18"`; conversion to a `LocalDate` happens
in the validation phase or in downstream consumer code.

## Cross-references

- **Spec § 6** —
  `docs/superpowers/specs/2026-04-28-p1.4.2-grammar-group-design.md`
  (local).
- **Audit § 3.1 H4 entry** —
  the development audit "p1-parser-architectural-audit".
- **Diagnostics:** `docs/diagnostics/RUNE-018.md`.
- **U006** — Structured parser diagnostics — defines the `RuneErrorCode`
  surface that hosts RUNE-018.
- **U009** — File-level header block — sibling P1.4.2 grammar-group
  surface.
- **`docs/features/INDEX.md`** — catalogue card for U010.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
