---
id: U008
subject: Rune annotations (@-prefix)
since_fork_version: "0.1.0"
since_pr_at_spec: 37
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: ["H2"]
related_parity_constraints: ["#7"]
doc_path: U008-rune-annotations.md
---

# U008: Rune annotations (@-prefix)

## Concept

A new `@-prefix` annotation syntax for rune-fork-specific features. Co-exists with
the existing legacy bracket-style `[annotation qualifier "value"]` system; both
mechanisms remain fully supported. Annotations are parsed and stored on the AST
but carry **no runtime semantics** in this PR (Layer-1 only) — downstream
consumers (validators, code generators, the W13 feature runtime) are not yet
wired. Activation of `@experimental(feature="strict-types")` and similar markers
is the future W13 hook.

## Why

The legacy bracket-style annotation system was designed for the FpML / CDM
upstream and embeds vendor-specific qualifiers in the surface grammar. The
rune fork needs a clean namespace for fork-specific features (W13 feature
flags, `@experimental`, `@deprecated`, `@since`, future ergonomic markers)
without contention against upstream qualifiers. The `@`-prefix syntax was
chosen to mirror Java / Kotlin / TypeScript decorator conventions familiar to
target tool consumers, and uses a single previously-unused lexer character
(`AT`) to avoid grammar conflicts.

## Structural changes

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.ast.annotations.RRuneAnnotation` | mutable AST class, extends `RNode` |
| `com.regnosys.rosetta.ast.annotations.RRuneAnnotationArg` | mutable AST class, extends `RNode` |
| `RRuneAnnotation.annotationName(): String` | accessor |
| `RRuneAnnotation.arguments(): List<RRuneAnnotationArg>` | live list |
| `RRuneAnnotationArg.name(): String` | accessor |
| `RRuneAnnotationArg.valueAsString(): String` | accessor |
| `com.regnosys.rosetta.ast.RRootElement.runeAnnotations(): List<RRuneAnnotation>` | hoisted slot, live list |
| `com.regnosys.rosetta.ast.RRootElement.addRuneAnnotation(RRuneAnnotation)` | mutator with `checkMutable()` guard |
| `AT` lexer token | `'@'` |
| `runeAnnotation`, `runeAnnotations`, `runeAnnotationArgs`, `runeAnnotationArg`, `runeAnnotationLiteral` | new parser rules |

### Grammar

```antlr
runeAnnotations    : runeAnnotation+ ;
runeAnnotation     : AT qualifiedName (LPAREN runeAnnotationArgs? RPAREN)? ;
runeAnnotationArgs : runeAnnotationArg (COMMA runeAnnotationArg)* ;
runeAnnotationArg  : validID EQ runeAnnotationLiteral ;
runeAnnotationLiteral
    : literal
    | qualifiedName
    ;
```

`runeAnnotationLiteral` reuses the existing `literal` rule (STRING / INT_LITERAL /
BIG_DECIMAL / TRUE / FALSE) plus `qualifiedName` for symbol-style values like
`@feature(target = my.pkg.Type)`.

### Diagnostics

`RUNE-016` (Invalid rune annotation argument) and `RUNE-019` (Invalid rune
annotation name) are **reserved** code points for grammar-rule-aware
classification. Today the generic ANTLR error path classifies malformed input
as `RUNE-001` (UnexpectedToken) or `RUNE-009` (MismatchedInput). Future
`RuneErrorClassifier` refinement (with rule-context awareness) will route
grammar-rule-specific failures to RUNE-016/019. The sealed-permits records
exist now so downstream consumers can reference these codes by string
identity. See `docs/diagnostics/RUNE-016.md` and `RUNE-019.md`.

## BC Story

- **100% additive.** Every existing `.rosetta` file in the 13-cell CATALOGUE
  (1,628 fixtures when the local working tree is refreshed) parses to an
  identical AST.
- **Default empty list.** Legacy callers see `runeAnnotations().isEmpty() == true`
  on every pre-existing input.
- **AT lexer token introduction.** The `'@'` character was unused prior to this
  PR; no existing input is reclassified.
- **Untouched legacy.** Existing bracket-style `annotation` / `annotationRef` /
  `annotationDecl` / `annotationQualifier` / `annotationPathExpression` rules
  remain unchanged.
- **No existing AST class signature changes.** New methods are additive.
- **japicmp Layer-1 stays non-breaking.** New surfaces report as ADDED entries
  under the D5 budget.

## How to use

```rosetta
@experimental
type Foo:
  field1 string (1..1)

@experimental(feature = "strict-types")
choice Bar:
  Foo
  Baz

@experimental
enum Status:
  ACTIVE
  RETIRED

@experimental
func Compute:
  inputs: x int (1..1)
  output: result int (1..1)
```

Read access on the AST:

```java
RDataType data = ...;
List<RRuneAnnotation> ann = data.runeAnnotations();
for (RRuneAnnotation a : ann) {
    String name = a.annotationName();          // "experimental"
    for (RRuneAnnotationArg arg : a.arguments()) {
        String key = arg.name();               // "feature"
        String value = arg.valueAsString();    // "strict-types"
    }
}
```

## Migration

No migration required — additive surface only. New code may opt in to rune
annotations where appropriate. Existing `.rosetta` corpus files compile
unchanged.

## Test coverage

- `RuneAnnotationParseTest` — parses each grammar branch and asserts AST
  population.
- `RuneAnnotationASTTest` — exercises `RRootElement.runeAnnotations()` and
  `addRuneAnnotation` mutator semantics + `checkMutable()` guard.
- `BackCompatCorpusOrthogonalityTest` — locks the additive-orthogonality
  invariant (runs in CI per cell post-P1.7 PR-3 T5 via the
  corpus-regression matrix; matrix aggregate covers all 1,628 fixtures
  when fully populated locally — see `docs/bc-verification.md`
  top-of-doc CI/local table): every legacy file parses with empty
  `runeAnnotations()` list.
- `AstCorpusRegressionTest` — AST well-formedness gate (lexer → parser →
  AstBuilder pipeline integrity: non-null root, parent-pointer wiring).
  Per `docs/bc-verification.md` Layer 2A.

## Coverage

### Attach-site coverage (P1.4.2 ship)

This release attaches `runeAnnotations?` as an optional prefix to **5 of 18**
RRootElement subclasses:

| Attached (5) | Not attached (13) |
|---|---|
| `RDataType` (data) | `RReport`, `RTypeAlias`, `RBasicType`, `RRecordType`, |
| `RChoice` | `RMetaType`, `RLibraryFunction`, `RAnnotation` (decl), |
| `REnumeration` | `RSynonymSource`, `RExternalSynonymSource`, |
| `RFunction` | `RExternalRuleSource`, `RBody`, `RCorpus`, `RSegmentDef` |
| `RRule` (via U011) | |

**Coverage rationale.** The 5 attached kinds are the user-authored top-level
constructs that a working `.rosetta` author writes annotations on in practice.
Vendor-supplied kinds (basic / record / meta types, library functions) are
typically defined in `rune-runtime/builtins` and rarely take user-authored
annotations.

**Forward-extension plan.** The 13 unattached kinds will be re-evaluated
against corpus-audit findings under D22 (P1.4.3 decision-log entry).
Extension is purely additive — landing a `runeAnnotations?` prefix on
`RReport` (for example) does not break any existing fixture because all
existing legacy fixtures have empty annotation slots
(`BackCompatCorpusOrthogonalityTest` locks this invariant across 1,628
files when populated).

### Hoist coverage

The `runeAnnotations()` slot on `RRootElement` is **hoisted to the parent
class**, so all 18 concrete `RRootElement` subclasses (including the 13 not
listed above) carry the slot as reserved AST surface. Only the 5 listed sites
parse the prefix in this PR; others inherit an always-empty list. Future H2
expansion to additional sites does not require AST surface changes.

`annotationDecl` is **not** an attach site — rune annotations decorate
user-facing declarations, not legacy bracket-annotation **definitions**.

## Cross-references

- **U011** — Rule-level rune annotations (free reuse of this infrastructure).
- **D22** — IR-scaffolding envelope (`the development decision log`, local) —
  governs the planned attach-site extension PR.
- **Spec:** `docs/superpowers/specs/2026-04-28-p1.4.2-grammar-group-design.md`
  § 4.
- **Audit:** the development audit "p1-parser-architectural-audit" § 3.1 H2 entry.
- **Diagnostics:** `docs/diagnostics/RUNE-016.md`, `RUNE-019.md`.
