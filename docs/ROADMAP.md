# Development phases and run-mode readiness

**Active: 3.3 — complete native legacy Java emission through IR. Next: 3.3.1 — code simplification.**

This detailed plan describes the 22 September code snapshot and its intended completion. The [visual planning guide](phasing-progress.md) is the entry point; this page is not a live builder log.

Run modes 0–3 describe execution configurations. Development phases describe milestones and are separate from Maven artifact versions. The implemented milestones below have their original evidence scopes; they do not imply completion of every later backend, editor or consumer integration.

| Phase | Status | Delivered work or intended outcome |
|---|---|---|
| 1.0 | Delivered | Replacement parser, AST, direct parity Java generation, Maven plugin and adapted runtime. Foundation of Mode 1 at the 9.83 baseline. |
| 2.0 | Delivered | Shared IR and the additive Java route. Foundation of Mode 2; this milestone did not deliver native IR emission for every Java file family. |
| 3.0 | Delivered | Optimised Java emitter and runtime improvements. Foundation of Mode 3; the emitter remains AST-hosted with IR-selected transformations. |
| 3.1 | Delivered | Semantic-resolution work and broader pinned-model regression coverage across the two parity routes. |
| 3.2 | Delivered | Versioned adversarial corpus and chaos checks, including explicit accounting for remaining differences and refusals. |
| **3.3** | **ACTIVE** | Complete native emission of the remaining Java file families on Mode 2. IR-fed enum, datatype-unit and choice-unit writers own 93,703 of 174,141 reference files (53.8%) across 25 real cells × 11 file kinds, excluding chaos, with legacy helpers/templates still involved; the metafield wrappers, package-info files, label providers and the function/rule/data-rule file bodies still require work. |
| **3.3.1** | **Next** | Simplify code and consolidate responsibilities while retaining accepted behaviour and regression checks. |
| 3.4 | Planned | Shared SDK and LSP/editor services, current Rune language support, and exploration of project compatibility profiles. Design services for later headless access. |
| 3.5 | Planned | Agent/MCP access through the same SDK and workspace services. |

Mode 0 uses the released Rune 9.83 compiler and runtime; it is the upstream baseline, not an implementation phase of this contribution. [Run modes](MODES.md) gives the mode/readiness mapping and artifact selection.

## What closes the active phase

Phase 3.3 replaces remaining AST-based emission with native IR emitters for the declared legacy Java output families. Acceptance requires independent output comparisons, accounted-for populations and refusals, retained semantic/adversarial checks, and verified routing through the supported generation entry points. A provider being selected is not the same as all files being emitted natively from IR. The completed route must become the default within the selected replacement compiler; this does not change the separate upstream compiler. Explicit decline and adversarial exit conditions must be met, and independent-emission certification remains separately reported.

The [snapshot accounting](phasing-progress.md#reading-the-progress-measures) reports the documented milestone. Its file-ownership measure is not overall project completion. [Evidence](EVIDENCE.md) and [Source snapshot](SOURCE-SNAPSHOT.md) distinguish recorded results from validation still required on the final review export.

## Later acceptance and exploratory work

Phase 3.3.1 preserves behaviour while simplifying the implementation. Phase 3.4 must establish the agreed SDK/editor workflows, test post-9.83 language changes and assess profile compatibility across source semantics, generated APIs and runtime dependencies. Phase 3.5 exposes those shared operations without a separate semantic implementation.

**Full IR hosting for optimised Java has no assigned phase in this contribution sequence and is not active.** The optimised emitter delivered in 3.0 is available today; rehosting it fully on IR is a separate future task with compilation and functional-comparison acceptance. This is why Mode 3 remains WIP as an end-to-end IR route.

Python through the SDK, additional C#/Rust generation and model bridges are exploratory further contributions. Their scope and sequencing remain to be assigned; they are not implied deliverables of the currently active phase.
