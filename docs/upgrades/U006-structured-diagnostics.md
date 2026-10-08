---
id: U006
subject: Structured parser diagnostics
since_fork_version: "0.1.0"
since_pr_at_spec: 36
since_pr_actual: null
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D9"]
related_audit_hooks: ["H1"]
related_parity_constraints: []
doc_path: U006-structured-diagnostics.md
---

# U006 — Structured parser diagnostics

## Concept

Replaces the legacy `RosettaErrorListener` (string-list output) with
`RuneDiagnosticListener`, which emits structured `ParserDiagnostic`
records carrying typed `RuneErrorCode` (RUNE-001..099 band) routed
through the existing sealed `RDiagnostic` hierarchy. Legacy
`errors(): List<String>` continues to populate via
`ParserDiagnostic.toLegacyString()` for byte-equal round-trip with
existing consumers.

## Why

The legacy `RosettaErrorListener` produced unstructured `List<String>`
output that consumers had to parse with regex (`line:col message`) to
extract diagnostic information. Tooling consumers (LSP server, IDE
integrations, downstream validators) need typed access to error code +
range + severity. D9 banded codes into `RUNE-NNN` ranges (parser
001-099, scope/linker 100-199, validation 300-399) so tooling can
classify diagnostics without string parsing.

## Structural changes

| Surface | Type |
|---|---|
| `com.regnosys.rosetta.parser.diagnostics.RuneErrorCode` | sealed interface, 19 concrete records spanning RUNE-001..019 (RUNE-016..019 are P1.4.2 grammar-group additions for H2/H3/H4 surfaces) |
| `com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic` | record, implements `RDiagnostic` |
| `com.regnosys.rosetta.parser.diagnostics.RuneErrorClassifier` | ANTLR exception → code mapper |
| `com.regnosys.rosetta.parser.RuneDiagnosticListener` | `BaseErrorListener` subclass — wires three ANTLR callbacks: `syntaxError` → `Severity.ERROR`; `reportAmbiguity` → `Severity.INFO` (RUNE-006); `reportContextSensitivity` → `Severity.INFO` (RUNE-007) |
| `com.regnosys.rosetta.parser.RosettaParseResult.diagnostics(): List<RDiagnostic>` | additive accessor |
| `com.regnosys.rosetta.symbols.diagnostics.DiagnosticCategory.PARSER` | additive enum value |
| `com.regnosys.rosetta.symbols.diagnostics.RDiagnostic` | gains default `Optional<RuneErrorCode> code()` + adds `ParserDiagnostic` to permits |

`ParserDiagnostic` lives in `com.regnosys.rosetta.symbols.diagnostics`
(not `com.regnosys.rosetta.parser.diagnostics`) — sealed cross-package
permits require a named module; rune-parser is in the unnamed module.
Documented in §15.B of the design spec.

### Severity mapping

`Severity` enum is unchanged; spec §15.A drift resolution mapped both
ambiguity callbacks to `INFO` because the landed enum has no `HINT`
value.

### Implementation notes (drift §15.B)

- `ParserDiagnostic` record component is `errorCode` (not `code`) to
  avoid Java-record auto-accessor signature conflict with the
  `RDiagnostic.code(): Optional<RuneErrorCode>` contract method. Access
  raw value via `.errorCode()`; access wrapped contract value via
  `.code()`.
- The `toLegacyString()` format is `line:col message` (NOT `line N:M
  message` — spec snippet had a typo; implementation matches the actual
  `RosettaErrorListener` format).

## BC Story

- `RosettaErrorListener` class — unchanged + `@Deprecated(since="0.1.0",
  forRemoval=false)`. Removal not scheduled.
- `RosettaParseResult.errors(): List<String>` — unchanged signature +
  `@Deprecated` on accessor; populated in lockstep with `diagnostics()`
  via `ParserDiagnostic.toLegacyString()` byte-equal round-trip.
- `RosettaParseResult` 2-arg constructor `(ParseTree, List<String>)` —
  preserved as a `@Deprecated(forRemoval=false)` secondary constructor
  that delegates to the canonical 3-arg form with empty `diagnostics`.
  Pre-P1.4.1c callers keep compiling without changes. Locked by
  `RosettaParseResultLegacyConstructorTest`.
- `AstBuilder` (only internal `errors()` consumer) compiles unchanged.

### Critical contract: `errors()` is ERROR-only

The legacy `errors()` list contains ONLY `Severity.ERROR` diagnostics.
INFO-severity diagnostics — `reportAmbiguity` (RUNE-006) and
`reportContextSensitivity` (RUNE-007) — appear ONLY in `diagnostics()`.

This is a hard contract: `AstBuilder` treats `errors()` non-empty as a
fatal parse failure. Bleeding INFO into `errors()` breaks
`AstCorpusRegressionTest` because real DRR fixtures trigger ambiguity
callbacks. Locked by
`RosettaParseResultDiagnosticsTest.infoDiagnosticConstructedDirectlyDoesNotBleedToErrors`.

### Sealed permits surface

`RDiagnostic` adds `ParserDiagnostic` as a third permitted variant
alongside `LinkingDiagnostic` and `ValidationDiagnostic`. Any external
consumer doing exhaustive `switch (diag) { case LinkingDiagnostic l →
...; case ValidationDiagnostic v → ...; }` would fail to compile against
the new JAR. **No external consumers exist today** (this is the first
consumer-visible release; `rune-parser-consumer-smoke/` smoke module is
the canary). Will require explicit baseline waiver when P2 flips
`japicmp` to `breakBuildOn*=true`.

`DiagnosticCategory` enum gains `PARSER` — additive (Java enum extension
is byte-compatible at the JVM level for additive ordinal-end values).
No `japicmp` surface impact.

## How to use

### New consumer (typed access)

```java
RosettaParser parser = ...;
parser.removeErrorListeners();
parser.addErrorListener(new RuneDiagnosticListener(filePath));
RosettaParseResult result = parser.parseAndBuild();

for (RDiagnostic d : result.diagnostics()) {
    if (d instanceof ParserDiagnostic pd) {
        // typed access to error code + range + severity
        String codeStr = pd.code().map(RuneErrorCode::code).orElse("(no code)");
        int startLine = pd.range().startLine();
        int startCol = pd.range().startColumn();
        Severity sev = pd.severity();
        // ...
    }
}
```

### Legacy consumer (string-list, still works)

```java
RosettaParseResult result = ...;
for (String err : result.errors()) {  // ERROR-only; INFO bypasses this list
    // legacy "line:col message" format unchanged
}
```

## Migration

| Old | New |
|---|---|
| `result.errors(): List<String>` | `result.diagnostics(): List<RDiagnostic>` (each is a `ParserDiagnostic`) |
| `new RosettaErrorListener()` | `new RuneDiagnosticListener(filePath)` |
| `errors.add("line:col message")` | `parser.addErrorListener(new RuneDiagnosticListener(...))` |
| string parsing for `line:col` | direct field access (`d.range().startLine()`, `d.code().get().code()`) |

Pre-existing consumers don't need to migrate — `errors()` continues to
populate via the legacy bridge.

## Test coverage

- `RuneDiagnosticListenerTest` — listener subclass behavior: ANTLR
  `syntaxError` callback dispatch and `Severity.ERROR` mapping. INFO
  severity routing is covered by sibling tests below.
- `ParserDiagnosticTest` — record contract: `errorCode` /
  `code()` accessor differentiation, range population, severity
  routing.
- `RuneErrorCodeTest` — sealed interface enumeration coverage:
  RUNE-001..019 records + their string codes.
- `RosettaParseResultDiagnosticsTest` — locks the
  `errors()`-is-ERROR-only contract:
  `infoDiagnosticConstructedDirectlyDoesNotBleedToErrors` is the
  canonical test method; broader coverage of `diagnostics()` accessor
  semantics. The INFO-severity routing (RUNE-006 ambiguity,
  RUNE-007 context-sensitivity) ends up exercised here too via
  direct-construction paths.
- `RosettaParseResultLegacyConstructorTest` — locks the 2-arg legacy
  constructor `RosettaParseResult(ParseTree, List<String>)` delegation
  to the canonical 3-arg form.
- `RDiagnosticDefaultCodeTest` — covers the `RDiagnostic.code()`
  default method behavior on each permitted variant.

## Coverage

The structured diagnostic surface covers the parse phase. The parser
band (RUNE-001..099) is the only band populated today; the design
reserves higher bands (100s scope/linker, 200s type system, 300s
validation, 400s codegen, 900s internal) for future work. Today's
populated codes: 19 concrete records spanning RUNE-001..019. RUNE-016
through RUNE-019 are the P1.4.2 grammar-group additions backing the
H2/H3/H4 surfaces (file headers, regulatory-reference named-args,
rune annotations); the generic ANTLR error path still classifies
malformed input that doesn't hit the rule-context heuristic as
RUNE-001 (UnexpectedToken) or RUNE-009 (MismatchedInput).

### Future work

- RUNE-100..199 (scope/linker codes) — future PR will override
  `RDiagnostic.code()` on `LinkingDiagnostic`.
- RUNE-300..399 (validation codes) — future PR for
  `ValidationDiagnostic`.
- Source-range byte-offset population on `ParserDiagnostic` — listener
  doesn't currently have access to the `int[] charToByte` table; offsets
  are `OFFSETS_UNKNOWN`. Follow-on PR can wire through (interacts with
  U001 byte-offset surface).

## Cross-references

- **Audit hook H1** — the development audit "p1-parser-architectural-audit".
- **Spec §6 + §7 + §15.A + §15.B** —
  `docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md`
  (local).
- **D9** (`the development decision log`, local) — RUNE-NNN banding
  scheme.
- **Per-code docs:** `docs/diagnostics/RUNE-001.md` … `RUNE-019.md`
  (one Markdown file per concrete record; 19 files total today).
- **U001** — SourceRange byte offsets — interacts with future-work
  byte-offset wiring on `ParserDiagnostic`.
- **U007** — LSP SymbolKind projection — sibling P1.4.1c surface.
- **U008** — Rune annotations — defines grammar surface that future
  RUNE-016/019 classification will route on.
- **`docs/features/INDEX.md`** — catalogue card for U006.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
