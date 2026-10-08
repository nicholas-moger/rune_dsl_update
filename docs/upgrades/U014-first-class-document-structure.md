---
title: U014 — First-class document-structure constructs (paragraph / page / appendix / footnote / docCorpus)
status: STUB (drafted at P2.0.2; full body content lands at P2.1.4 alongside W36 implementation)
type: enhancement
introduced: P2.0.2 (STUB) → P2.1.4 (full)
---

# U014 — First-class document-structure constructs

> **STUB notice:** this manifest is a STUB authored at P2.0.2 W36 design
> lock per spec § 9. The 9 canonical sections below carry brief stub
> content; full body content requires W36 implementation at P2.1.4 (concrete
> grammar productions land at parser:195-215 + 681-735, AST classes,
> SymbolResolver extension, validators, codegen templates, test classes).
> **Mechanical landing** (`UpgradeManifestStructureTest.ALLOWED_FILES`
> broaden to 14; `docs/features/INDEX.md` card; `docs/bc-verification.md`
> per-feature locks entry; `docs/upgrades/README.md` + `docs/upgrades/index.json`
> entries) is INCLUDED at P2.0.2 to keep the 3 lock tests GREEN under
> the new U014 file.

## Concept

<STUB — full body at P2.1.4. Brief: lift overloaded `segment` machinery
to 5 first-class RRootElement kinds (paragraph / page / appendix /
footnote / docCorpus) per W36 wishlist. Typed cross-references via
SymbolId + DocStructureKind enum replace string-matched lookups.>

## Why

<STUB — full body at P2.1.4. Brief: typed cross-refs + IDE jump-to-
definition + structural validators + refactor-safety per W36 wishlist
text at docs/p0/wishlist.md:182. D29 confirmed parity-relevant
at P2.0.1 (constraint #22 added at P2.0.2). Adding new RRootElement
kinds AFTER codegen orchestration stabilises = multi-layer rewrite per
deep-dive #2 § 4.1; cheapest moment is during P2.1 rebaseline.>

## Structural changes

<STUB — full body at P2.1.4. Brief: 5 grammar productions
(rosettaDocCorpus / rosettaAppendix / rosettaPage / rosettaParagraph /
rosettaFootnote at parser rootElement); 5 RRootElement subclasses
(RDocCorpus / RAppendix / RPage / RParagraph / RFootnote; 18→23 kinds);
5 ST4 templates (java-doccorpus.stg etc); ValidationErrorCode sealed
sister interface for RUNE-301..305; 5-scope architecture extension
(D33 STUB at P2.0.2 → final at P2.1.4); 7 coordinated edit points per
spec § 3.>

## BC Story

<STUB — full body at P2.1.4. Brief: deprecation-free coexistence per
D31 BC-primary invariant; existing rosettaSegment + rosettaSegmentRef
syntax UNCHANGED; 5 segment AST classes at rune-parser/.../regulatory/
(RDocReference / RRegulatoryReferenceArg / RRegulatoryDocumentReference /
RSegmentDef / RSegmentRef) all UNCHANGED; W36 is purely additive new
top-level alternative; BackCompatCorpusOrthogonalityTest gains 5
negative-direction assertions; NEW BackCompatSegmentSyntaxParsesTest
adds positive-direction.>

## How to use

<STUB — full body at P2.1.4. Brief: declare via `docCorpus MiFIR_RTS_2024
"MiFIR RTS 2024" { appendix App1 "Operational definitions" { paragraph
Para_4_5 "4.5" } }`; cross-reference via existing regulatoryReference
named-args `(docCorpus = "MiFIR_RTS_2024", appendix = "App1", paragraph =
"Para_4_5")`. Old `segment` syntax keeps working forever.>

## Migration

<STUB — full body at P2.1.4. Brief: no forced migration — opt-in for
new fixtures; deprecation-free coexistence per D31. W14 wishlist
auto-rewriter (`segment → W36`) is future-state (out of P2.1.4 scope).>

## Test coverage

<STUB — full body at P2.1.4. Brief: 8 NEW test classes
(BackCompatSegmentSyntaxParsesTest, W36LexerTokenTest,
W36GrammarParseTest, W36AstShapeTest, W36SymbolResolutionTest,
W36ValidatorTest, W36CodegenTest, RDocStructureCrossRefSealednessTest)
+ 6 EXTEND classes (BackCompatCorpusOrthogonalityTest +5 assertions,
BCVerificationCoverageTest, FeaturesIndexCoverageTest,
UpgradeManifestStructureTest ALLOWED_FILES, RuneSymbolKindTest +5
switch arms, StructuralComparisonTest baselines regen 234→299).
D11CorpusRegressionTest unchanged (POJO-bucket route).>

## Coverage

<STUB — full body at P2.1.4. Brief: 13 cells × 23 RRootElement kinds
= 299 StructuralComparisonTest baselines (file ~336 lines with
comments). D11 POJO-bucket route — W36 records emit into existing 14
cells × 4 element kinds matrix; KNOWN_DIVERGENT initially empty for
W36 outputs.>

## Cross-references

- **W36 wishlist** — `docs/p0/wishlist.md:182`. Verbatim text
  driving this manifest.
- **D29** — `the development decision log` D29 entry (local). Parity
  classification confirmed at P2.0.1.
- **D33 STUB** — `the development decision log` D33 entry (local).
  5-scope architecture extension placeholder; final at P2.1.4.
- **Constraint #22** — `docs/p0/parity-constraints.md` constraint list.
  AST hierarchy supports first-class document-structure kinds.
- **P2.0.2 design spec** —
  `docs/superpowers/specs/2026-05-08-w36-first-class-document-structure-design.md`
  (local). 16-section design lock.
- **P2.0.1 deep-dives** — the development audit "02-ast-and-symbol-tables"
  § 4.1 + the development audit "05-structured-refs-pra" § 4.1.
- **U013** — `docs/upgrades/U013-ir-mediated-java-codegen.md`. STUB
  precedent (4-key frontmatter + STUB notice + per-section
  `<STUB — Brief: ...>` paragraphs).
- **P2.1 parent spec** —
  `docs/superpowers/specs/2026-05-08-p2.1-parity-rebaseline-design.md`
  (local). P2.1.4 implementation slot.
