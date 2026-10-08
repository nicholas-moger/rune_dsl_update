# Rune Diagnostics

Per-code documentation for `RuneErrorCode` variants emitted by the rune-dsl-plus parser, linker, and validator.

## Banding scheme

| Band | Category | Owner |
|---|---|---|
| RUNE-001..099 | parser | `RuneDiagnosticListener` (P1.4.1c) |
| RUNE-100..199 | scope/linker | future (linker re-emit) |
| RUNE-200..299 | type system | future |
| RUNE-300..399 | validation | future (M6 validator re-emit) |
| RUNE-400..499 | codegen | future |
| RUNE-900..999 | internal | future |

## Index

See [`codes-registry.json`](./codes-registry.json) for the canonical list.

## Status

- **ready** — page is hand-written with example input + diagnostic output + remediation.
- **stub** — page has YAML front-matter only; content is best-effort placeholder text. Filled in as corpus parse-error data accumulates.

## Validation

`scripts/ci/diagnostic-doc-validator.sh` runs in CI and:
- Hard-fails if any code in `codes-registry.json` has no corresponding `.md` file.
- Hard-fails if any `.md` file is not registered.
- Schema-validates `codes-registry.json` against `schema.json`.
- Cross-checks against the sealed `RuneErrorCode` interface (every variant has a registry entry).
- Reports `doc_status: stub` count in CI summary; does NOT fail CI on stubs.

## Spec reference

P1.4.1c §9 in `docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md` (local-only).

## Related

- Manifest entry [U006](../upgrades/U006-structured-diagnostics.md) — structured parser diagnostics.
- Sealed interface: `com.regnosys.rosetta.parser.diagnostics.RuneErrorCode`.
- Listener: `com.regnosys.rosetta.parser.RuneDiagnosticListener`.
- Diagnostic record: `com.regnosys.rosetta.symbols.diagnostics.ParserDiagnostic`.
