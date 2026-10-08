# The chaos seed format (the committed contract an editor must know)

Every `seed-sNN.rosetta` here is INPUT to `../expand.py` (whose module
docstring is the full normative definition — this page is the editor's
summary; the two must agree). The expander treats a seed as marker-delimited
BLOCKS and never parses Rune, so the format rules are enforced by loud
`SeedFormatError`s at expansion time:

- **LF-only** — a `\r` anywhere refuses (the A5 axis owns encodings;
  `.gitattributes` pins `seeds/** -text` so checkouts cannot rewrite them).
- **Header** — the first block is `//== header` and its first two lines are
  EXACTLY `namespace chaos.<family>.seed` and `version "0.0.0"` (rewritten
  wholesale per variant). Header lines AFTER those are kept verbatim and may
  hold the seed's own imports followed by qualifiable-config lines
  (`isProduct root …;` — s22); expander-generated imports are inserted
  BEFORE the header tail so the grammar's Import*-then-config order holds in
  every variant.
- **Blocks** — `//== element <Name>` (1..6 per seed, the reorderable units;
  a block may hold several declarations), at most one `//== helper <Name>`,
  and `//== collision <Name> kind=<k>` (s16/s17; kinds unique per seed).
- **The helper block is SELF-CONTAINED** (references nothing else in the
  seed) and is REFERENCED by the main content: the A2 import-style axis
  relocates it to its own namespace and renders it with an EMPTY header
  tail, so any outward reference or header dependency breaks those variants.
- **The first element block is the SLIM CARRIER** for A4/A5 (helper + first
  element only), so it too must stand alone with the helper.
- **Naming** — machine ids are lowercase (`s01`…`s34` the shape families,
  `s90`…`s99` the base-only band; filenames, axes.tsv applicability);
  prose labels are uppercase (`S01`, the charter § 2.2 and
  `../would-have-caught.md`). Declarations carry a family prefix
  (`C1…`/`C34…`, `C90…`) so cross-family collisions stay the deliberate
  business of the collision blocks. Avoid lexer keywords as attribute/input
  names (`tag`, `label`, `first`, and the exponent letter `e` have all
  refused — the l1 logs remember).
- **Generation 2 (`chaos-1.1.0`, v3.2 seat 10 — D49 in the local decision
  log):** the 22 gen-1 seeds are the regression belt and are NOT edited (a
  gen-1 file must expand byte-identical — the seat's `--check-against` over
  the old cell says so); fresh shapes go into FRESH families. Collision block
  kinds: `type` / `choice` / `meta` / `enum` (A6, the rival in `.rival`),
  `fn` (A6: a rival FUNCTION named like a type), `pkg` (A8: a rival TYPE
  named like one of the seed's functions, placed in the `.functions`
  sub-namespace so the two generators address ONE Java path). The s9x band
  is base-only (absent from every axis's applicability): one shape per file —
  `s90`–`s94`, `s96`, `s97` a released-plugin refusal each, EXPECTED under
  `REFUSAL-PARITY` and judged ALONE by the pin script; `s95` ORACLE-CLEAN (the
  ERRATUM of v3.2 seat 10 round 1, cq SF-3: `seed-s95.rosetta`'s docstring still
  predicts the refusal ("upstream refuses ('Duplicate name.')") the measurement
  REFUTED — the plugin admits the duplicate name and renders `(a0, a1)`; the text is
  baked into the committed cell and its golden, so the seed edit is banked for the
  second round (it moves the sink digest and re-pins s95's goldens); this line and
  `fork-diagnostics.tsv`'s s95 row carry the measured verdict —
  duplicate closure-parameter name upstream accepts); `s98` MOJO-CRASH (L1-clean,
  the generator throws — `../expectations/mojo-refused.tsv`, judged alone too);
  `s99` ORACLE-CLEAN with goldens that do NOT compile (the java.lang shadow —
  the cell's `noncompiling-goldens.txt` register; one java.lang-named probe
  per package, or it poisons every sibling class) — the fork's own verdict
  pinned per file in `../expectations/fork-diagnostics.tsv`.
- **The fresh axes (A7 / A8 / A9) are content-blind like the old ones**: A7
  renames the FILE (`aaa-` / `zz-` prefixes on a slim carrier), A8 relocates
  a `kind=pkg` block, A9 rewrites leading whitespace to tabs or wraps the
  text in comment trivia — none reads Rune. A seed whose first element block
  ends inside a multi-line construct is NOT A9-safe: keep doc strings and
  string literals on one line (every gen-1 seed already does).
- **`axes.tsv` lists the 34 non-band families five times (A1–A5)** — `s01`–`s34`,
  the 22 gen-1 seeds AND the twelve fresh generation-2 families (the band
  s90–s99 is what the five copies exclude; round 3, rule6 SF-1) — since the
  `*` applicability was made explicit at chaos-1.1.0. The round-1 review
  (cq NIT-5) asked for an `exclude=` column or `*`-minus-a-band-list;
  DECLINED at seat 10: either form changes the expander's input, moves the
  sink digest and re-pins the whole cell's goldens — banked for the next
  seat that re-pins (the second round's first), never a docs-only change.
- **Shape batteries** — the charter § 2.2b meta/cardinality dimensions are
  recorded in the element doc-strings ("charter 2.2b"); editing one edits a
  MEASURED dimension: re-run `expand.py`, the L1 admission, the warnings
  census, `ChaosCoverageCensusTest` (whose pins name the expander digest)
  and `pin-chaos-goldens.sh`, in that order.
