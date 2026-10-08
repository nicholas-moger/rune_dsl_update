# rune-dsl-plus consumer upgrade manifest

Index of consumer-visible API additions / changes / removals on top of the
rune-dsl baseline. Each consumer-visible change adds one entry, and a PR
may add multiple entries. Validator:
`scripts/ci/upgrade-manifest-validate.sh`.

## Compatibility policy

rune-dsl-plus is currently pre-1.0 (`0.x.y`). U-entry `Available` status is
best-effort during 0.x; minor-version-bump breaks are possible if parity
work demands. Hard SemVer commitment begins at 1.0.0 — at which point
`Available` becomes a binary-compat contract enforced by japicmp gate
(which P2 work will flip to `breakBuildOn*=true`).

**Removal trigger.** Removal triggered only by parity-incompatible refactor
(P2+ scope). Minimum 1 minor-version notice between status transitions:
`Available → Deprecated → Removed`. Each transition documents the version
where it occurs in the U-entry.

## Status meanings

- `Available` — supported, no migration required for existing callers
- `Deprecated` — marked `@Deprecated(since=..., forRemoval=false)`; new
  code should use the replacement; old code keeps working
- `Removed` — symbol no longer exists; consumers must migrate

## Migration depth

- `None` — additive change, no consumer action required
- `Optional` — consumers may migrate for new functionality, no requirement
- `Required-by-vN.M` — consumers must migrate before upgrading to vN.M

## Entries

| ID | Subject | Since | Status | Migration | Risk | Detail |
|---|---|---|---|---|---|---|
| U001 | SourceRange byte offsets | 0.1.0 | Available | Optional | low | [→](U001-bytewise-source-range.md) |
| U002 | Frozen AST after linker | 0.1.0 | Available | None | low | [→](U002-frozen-ast-after-linker.md) |
| U003 | RNode.metadata immutable slot | 0.1.0 | Available | None | low | [→](U003-rnode-metadata-slot.md) |
| U004 | AstInterfaceRegistry visitor pattern | 0.1.0 | Available | Optional | low | [→](U004-ast-interface-registry.md) |
| U005 | SymbolId opaque-handle cross-references | 0.1.0 | Available | Optional | medium | [→](U005-symbolid-cross-references.md) |
| U006 | Structured parser diagnostics | 0.1.0 | Available | Optional | low | [→](U006-structured-diagnostics.md) |
| U007 | LSP SymbolKind projection | 0.1.0 | Available | Optional | low | [→](U007-lsp-symbol-kinds.md) |
| U008 | Rune annotations (@-prefix) | 0.1.0 | Available | Optional | low | [→](U008-rune-annotations.md) |
| U009 | File-level header block | 0.1.0 | Available | Optional | low | [→](U009-file-header.md) |
| U010 | regulatoryReference named-arg list | 0.1.0 | Available | Optional | low | [→](U010-regulatory-reference-named-args.md) |
| U011 | Rule-level rune annotations | 0.1.0 | Available | Optional | low | [→](U011-rule-annotations.md) |
| U012 | IR (Intermediate Representation) scaffolding — type surface only | 0.1.0 | Available | None | low | [→](U012-ir-scaffolding.md) |
| U013 | IR-mediated Java codegen (STUB) | 0.1.0 | Available | None | low | [→](U013-ir-mediated-java-codegen.md) |
| U014 | First-class document structure (STUB) | 0.1.0 | Available | None | low | [→](U014-first-class-document-structure.md) |
| U015 | Top-level `choice` POJO codegen — parallel ChoiceObjectGenerator + resolver hardening | 0.1.0 | Available | None | low | [→](U015-choice-pojo-codegen.md) |
| U016 | Rule / Report / LabelProvider Java codegen — scaffolding (Phase X) + emission + typing milestone (Phase X1 closed PR #79); byte-flip → engine phase | 0.1.0 | Available | None | low | [→](U016-rule-report-labelprovider-codegen.md) |
| U017 | Drop-in Maven plugin (`rune-maven-plugin`) — the released 9.83.0 goal surface on the fork pipeline; GAV-swap proven on all four corpora | 0.1.0 | Available | None | low | [→](U017-drop-in-maven-plugin.md) |
| U018 | Fork-owned runtime (`rune-runtime`) — the released 9.83.0 runtime rebuilt ABI-identical (227/227); xtend dependency removed | 0.1.0 | Available | None | low | [→](U018-fork-owned-runtime.md) |
| U019 | Optimised IR-routed Java codegen — the flag-gated route 3 (`rune-ir-java-optimised` + the `OptimisedIRGenerationProvider` seam); the FUNCTION seam flipped at PR #539 (navigation chains — tranche 1; tranche 2 terminal-multi at PR #540; family 2 meta chains at PR #541; the member-injection seam + the § 11 stream re-price measured out at PR #542 — the STREAM decline permanent; family 3 THE RULE FACE at PR #544 — the rule/report window flip + item-rooted ladders, −581.2 MB/op = −1.89% disjoint) | 0.1.0 | Available | None | low | [→](U019-optimised-ir-java-codegen.md) |
| U020 | Lazy comparison-failure text (`rune-runtime`) — the deferred error channel in `ComparisonResult` (dual-field carrier + package-private lazy factories); 68 formatter seats defer message construction until observed, byte-identical at every live reading seat; the PR #547 paired 6-fork A/B: the DRR report run 3.47× faster, allocation −66.0% | 0.1.0 | Available | None | low | [→](U020-lazy-failure-text-runtime.md) |
| U021 | Deferred MapperPath lineage (`rune-runtime`) — the cons-cell path (O(1) per hop) + the lazy PathElement + the memoized toAttributeName cache; every corpus-reachable observation byte-identical; the PR #550 paired 6-fork A/B: the DRR report run 1.29× faster, allocation −18.1% | 0.1.0 | Available | None | low | [→](U021-deferred-mapper-path-lineage.md) |
| U022 | Seed-deferred MapperPath construction (`rune-runtime`) — the item stores a package-private path seed in place of the built node; getPath() builds memoized on first observation; the error-share/upcast corners seed-shared; every corpus-reachable observation byte-identical; the PR #553 paired 6-fork A/B: the DRR report run 1.56× faster (iteration-disjoint), allocation −11.57% | 0.1.0 | Available | None | low | [→](U022-seed-deferred-mapper-path-construction.md) |
| U023 | Value-typed alias seams (optimised route) — the § 6.3 re-typing program's emitter tranches (API-quality, NO workload perf claim): T1 (PR #558) re-types the 231 whole-body-one-chain members to bare `T` / `List<? extends T>` seams with ladder-value bodies, the structural `MapperS.of`/`MapperC.<X>of` bridge at every invocation seat (evaluate-arg/assignment strips restore the bare value), and the alias-rooted § 9a ladder at operation-seat chain roots; T2 (PR #559) extends the flip to the 587 chain-headed + APPLY-headed members — the direct call composition at every APPLY body, the disclosed boundary fallback at every chain-headed body, two element/cardinality strip guards, T2 chain roots kept on the bridge; T3 (v3 PR-24) extends it to the 720 CONDITIONAL-headed + tail-kind members — the decorated return/switch ladders (per-rung value decoration, § 2 value empties), the decorated then-hoist, DIRECT ctors/collapses, the per-kind boundary fallback, three further consumer guards, the § 14f capture MANDATORY-armed and green ×2; T4 (v3 PR-25) completes the flip population with the 220 careful-class members — the dispatch case units atomic BY CONSTRUCTION on the dispatch-base thread (per-case-unit O5p attribution), the 29 D5 rows on RENDER-AUTHORITY (the seam string alone is the S/C bit; the belt + O5p pair are the reconciliation receipts), the 79 oracle-live D4 rows with every enumerated seat on the standing forms and their disguised chains converting at operation seats (alias-root pins 38/313/127) — zero new emission code, zero new guards; the § 14f capture MANDATORY-armed AGAIN (the 9th D2 row) and green ×2 (1,758 of 1,820 landed = the FULL flip population); only D1/D3 stay Mapper-typed per the plan § 6 (the 62-row residue); T5 (v3 PR-26) closed the program — the aggregate O5 report (3,516 protected signature-delta rows across 604 of the 651 seam files) + the 62-row residual census per member + the consolidated migration notes at the development audit "2026-08-13-p3-63-alias-retyping-o5-report", the T4 generated-body witnesses folded into the emission gate | 0.1.0 | Available | None | low | [→](U023-value-typed-alias-seams.md) |
