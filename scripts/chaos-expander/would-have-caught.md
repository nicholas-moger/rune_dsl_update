# The would-have-caught table (charter § 2.2b — Rule 4 applied to the threat model)

Every one of the **29 v3.1 mechanism families** (`target/v31-close-census.md`
(local), the census the whole C2 programme burned down) mapped to the chaos
seed/axis whose **mechanism class** provokes it. This is the SEMANTIC half of
the adversarial-power claim — the § 2.4 node-kind census proves grammar
coverage; this table proves the corpus reaches the *shape conjunctions* the
real bugs lived in.

**What a row claims:** the chaos corpus contains source whose shape is the
family's TRIGGER (the Rune-side conjunction the defective emitter path fired
on) — verified by authoring + the L1 admission. Whether the FORK diverges on
it is L3/L2's verdict in PR-2; a row here is reach, never a parity claim.

**Status key:** REACHED = the original 22-family corpus already carried
the shape · ENRICHED = added by the census-driven enrichment rounds of THIS
PR (commits 5–6) · RECORDED = a leg deliberately not carried, with its reason
and its § 9 evolution disposition.

| fam | mechanism class (the Rune-side trigger) | chaos carrier | status |
|---|---|---|---|
| F1 | a rule whose extracted value is META-annotated while the declared/reported output is bare | `C6Venue` (rule over the `[metadata scheme]` attr `venue`) | ENRICHED |
| F2 | non-ASCII characters reaching emitted Java literals (labels, displayNames, docs) | A5 `a5uni` (doc-string injection + the appended `A5UniProbe` label / `A5UniProbeEnum` displayName probes, ×22 files) | ENRICHED |
| F3 | an int literal at a `number`-typed seat (conditional arm) | `C5Seats` (`else 0` arms against `opt number`) | REACHED |
| F4 | a BARE function call as an if-condition (Boolean hoist seam) | `C4Gate` (`if C4IsBig(final) then …`) | ENRICHED |
| F5 | a beyond-long integer literal (BigInteger hoist) | `C4Gate` (`9999999999999999999999999`, 84 bits) | ENRICHED |
| F6 | a rule-call result typed by the callee's meta + cardinality | `C6Compose` (`extract C6Id then extract item + …`) | ENRICHED |
| F7 | `then` pipes inside lambda interiors / alias bodies (hoist-sink seams) | `C5Forms.lambdaThen` (then-chain inside an extract lambda) + every alias-body `then` in `C5Forms`/`C9Sieve` | ENRICHED |
| F8 | a hoisted conditional local that must take the CONSUMER's type + cardinality | `C19Pick` (conditionals at ctor-field seats, one arm `empty`) + `C5Seats` (set-seat arms) | ENRICHED |
| F9 | conditional arms of SIBLING SUBTYPES (wildcard/common-supertype decl) | `C1Widen` (`if b then l else m` over the `C1Base` chain) | ENRICHED |
| F10 | the `default` family — (a) MULTI operand; (c) collapsing right | (a) `C5Forms.fallback` (`then default [0]`); (c) `C5Preds` (`pool first default 0`); rule-seat `C6First` | REACHED |
| F10′ | legs (b) meta LEFT operand, (d) bare-enum default re-wrap | RECORDED: not carried; both are meta/enum × default conjunctions the v3.2 census can price — a § 9 feedback row if PR-2's gate run leaves them unwitnessed | RECORDED |
| F11 | chain-step methods following the RECEIVER's rendered cardinality | `C5Chain` (list-of-list hops), `C6Legs` (filter inside a rule), `C20Pipeline` | REACHED |
| F12 | a conditional consumed INSIDE a lambda body (statement sink) | `C5Forms.lambdaIte` (`extract x [ if … then … else … ]`) | ENRICHED |
| F13 | meta wrappers consumed at bare seats (deref ladder) | `C9Pick` (reference-annotated → bare typed output) + `C9Sieve` (meta at FILTER-PREDICATE and first/kid legs) | REACHED + ENRICHED |
| F14 | lambda/hoist NAMING (shadow-adjacent classes) | `C20Shade` (a name re-used across SIBLING lambdas; near-colliding nested names). The DIRECT nested shadow was probed and **upstream REFUSES it** — `Duplicate name.` (the l1-run8 receipt, local) — reverted so s20 keeps goldens; a dedicated refusal-parity probe is a § 9 candidate | ENRICHED + RECORDED (shadow leg) |
| F15 | switch/enum renders — (a) choice type guards; (b) extract-embedded switch; (c) bare enum comparand vs the pipe item | (a) `C18Pull`; (b) `C18ToKind.switched`; (c) `C17Sift` (`filter item = Buy` beside the qualified twin) | REACHED + ENRICHED |
| F15′ | leg (d) disguised implicit-input-attribute → choice-option chain head | RECORDED: needs an implicit-input attr whose type is a choice navigated by bare option at a rule seat — a precise conjunction deferred to the v3.2 census's feedback row (§ 9) rather than guessed at | RECORDED |
| F16 | deep-feature (`->>`) DeepPathUtil wiring, incl. lambda-interior receivers | `C3Deep` conditions (`pick ->> text`) + `C3Commons` (`extract item ->> text`) | REACHED + ENRICHED |
| F17 | same-simple-name claims in one compilation scope | S16's A6 battery: rival TYPE / rival CHOICE / rival METATYPE (+ the A2xA6 / A3xA6 interaction rows; the x36enum-p2 capture is already a measured upstream answer) | REACHED |
| F17′ | the type-vs-FUNCTION same-name flavour | RECORDED: A6 carries 3 authored collision kinds per the charter cap; the function flavour is the first § 9 growth candidate | RECORDED |
| F18 | Rune input names that collide with Java reserved words (escape seams) | `C4Gate` (`final` / `static` / `interface` inputs — legal Rune IDs, Java keywords) | ENRICHED |
| F19 | ctor list-field copy semantics for DATA-type elements | `C19Make` (`parts: [p, C19Part{…}]`) | REACHED |
| F20 | `to-enum` at a SINGLE assignment sink (collapse seam) | `C18ToKind` (`set k: raw to-enum C18KindEnum`) | ENRICHED |
| F21 | boolean-logic (ComparisonResult-shaped) conditional arms | `C5Forms.boolArm` (`if … then (a and b) else (c < 0)`) | ENRICHED |
| F22 | a RULE symbol referenced as a value | `C6Compose` (`extract C6Id` — the rule name as the extract body) | ENRICHED |
| F23 | then-rung decl item type where the chain carries META elements | `C9Sieve.firstRef` (`byRefs then filter … then first` over `[metadata reference]` (0..*)) | ENRICHED |
| F24 | ctor as-key reference fed by a BARE value | `C19Make` (`partRef: p as-key`, `p` a bare input) | REACHED |
| F25 | a function CALL as a navigation receiver inside a rule's conditional arm | `C6NavCall` (`C6MarkOf(item) -> caption default …` in the then-arm) | ENRICHED |
| F26 | as-key SET with a meta-wrapper deref (segment-set path) | `C19Pick` (`set w -> partRef: p as-key`) | ENRICHED |
| F27 | only-element collapse re-wrapped at a conditional Mapper arm | `C5Forms.soloArm` (`if … then (items only-element -> opt) else (others first -> opt)`) | ENRICHED |
| F28 | a meta-annotated scalar through an if/else LADDER (meta-blind item type) | `C9Sieve` (the three-rung ladder over `coded` / `codes` / `firstRef -> kid`, all meta-carrying) | ENRICHED |
| F29 | type witnesses registered inside a BLOCK lambda (import survival) | `C18ToKind.pulled` (choice switch with type guards INSIDE an extract lambda) | ENRICHED |

## The count

29 families, every one carried (the spec review's MF-6 corrected the first
draft's 6 + 20 arithmetic): **6 REACHED whole by the original corpus (F3,
F10, F11, F17, F19, F24) + 3 REACHED with legs ENRICHED (F13, F15, F16) +
20 ENRICHED (the rest) = 29, 0 unreached** — plus 4 RECORDED rows covering 5
legs on otherwise-carried families (F10′ carries both b and d; F15′ d; F17′
fn-flavour; F14's direct shadow), each with a named § 9 disposition, one of
them (the shadow) closed by a MEASURED upstream refusal rather than a guess.

The incentive check the charter § 2.2b demands: nothing here was softened to
keep PR-2 green — every enrichment lands as corpus the L3 gate must match
byte-for-byte, and the four recorded legs are recorded because their
conjunction needs design (not because they might find something).
