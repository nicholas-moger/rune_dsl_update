# Limitations and plan


This is an evaluation of a compiler evolution, not a completed migration proposal. Status is tied to the [exported revision](SOURCE-SNAPSHOT.md); upcoming milestones are not counted as delivered.

## Current limitations

- **Diagnostic reference documentation is incomplete.** Ten of the 19 parser diagnostic entries remain stubs without full examples or remediation guidance; see the [diagnostics index](diagnostics/README.md). This is an existing documentation gap, separate from diagnostic behaviour in the compiler.

- **Compatibility is pinned.** Rune DSL 9.83.0 is the reference. Later syntax, semantics and generated APIs need explicit adoption and tests. No general configurable 9.x/10.x compatibility claim is made.
- **IR closure is incomplete.** IR-fed enum, data-type and choice writers own 93,703 of 174,141 reference files across the 25 real-model cells and 11 file kinds, excluding chaos. This historical ownership measure still includes legacy helpers/templates; it is not independent-emission or certification credit. In this snapshot, shared metafield wrappers, namespace `package-info` output, label providers and the file bodies of functions, rules and data-rule conditions still use the existing generator.
- **Entry points differ.** The Maven plugin dispatches its supported passes through the IR provider hooks; the older demo driver still constructs some generators directly. An IR flag alone does not prove end-to-end native IR generation: read the writer accounting. See [Modes](MODES.md).
- **Optimisation is scoped.** Implemented optimisations have measured populations and retained differences; neither identical source nor universal performance improvement is promised.
- **The adverse-case set has declared gaps.** Named byte differences, refusals, missing outputs and excluded reference cases remain visible in their registers. They must not be merged into a general “all compatible” statement.
- **Editor parity is future work.** Internal semantic APIs and prototypes are not a packaged language server or a supported SDK. Replacing Xtext/EMF also means taking responsibility for the services it provided.
- **Other targets and bridges are not product commitments.** Python work is partial/prototype scope; Rust, C# and model-bridge integrations (other metamodels and intermediate representations) need their own semantics, APIs, runtimes and acceptance tests. An IR representation alone does not supply them.
- **The AI experiment measures context representation.** It does not evaluate an LLM, validate every binding or prove that the old architecture could not expose structured information.
- **Fresh-export validation is separate.** Development receipts do not prove this curated repository builds from an empty dependency cache. See [Snapshot](SOURCE-SNAPSHOT.md) and [Building](BUILDING.md).

## Proposed sequence

| Phase | Objective | Completion evidence |
|---|---|---|
| 3.3 — current | Close the reference Java route over IR | All file families on the completed production IR route, default enablement within the replacement, decline-register and adversarial exit conditions, preserved compatibility and route evidence; [full criteria](ROADMAP.md#what-closes-the-active-phase) |
| 3.3.1 — proposed | Simplify code and consolidate responsibilities | Smaller, clearer implementation without changed accepted behaviour; retained checks and reviewed diffs |
| 3.4 — proposed | SDK, LSP and editor services | Shared compiler/workspace APIs; early end-to-end editor use while APIs evolve; verified agreed editor workflows before a supported release |
| 3.5 — proposed | Agent/MCP surface | Programmatic operations using SDK semantics, clear diagnostics and reproducible checks; no second language implementation |

The phase transition is an owner decision. These are the proposed review priorities; historical numbering in older engineering documents is not an additional delivery promise.

Full rehosting of optimised Java on IR remains work to schedule explicitly, with the functional comparison tests retained. A Python generator developed through the SDK, further target languages and model bridges are exploratory further contributions with no assigned phase, as [Roadmap](ROADMAP.md) states; a Python exercise would have to prove the extension surface with Python runtime/semantic coverage and execution tests before any sequencing. Same-repository coexistence with upstream, its default route and a controlled alternate configuration must be designed and demonstrated before any transition. Neither responsibility disappears when SDK work begins.

## SDK and editor acceptance

Capture the actual upstream workflows before implementation. Start with shared workspace, parsing, binding, type, diagnostic and dependency services; build an early LSP/editor slice alongside those APIs rather than discovering editor requirements after API stabilisation.

Acceptance should cover open/edit/close, incomplete and erroneous input, cross-file updates, cancellation and stale-result handling, navigation, references, completion, rename and formatting as agreed with the maintainers. Compare usefulness and responsiveness with the existing experience. Batch compiler tests alone cannot establish editor parity.

Use representative post-9.83 maintenance changes to test how easily the SDK supports evolution. Add a small external generator exercise during SDK development; use the later Python target as the fuller extension test. Document changes that still require core edits rather than presenting every change as a plug-in capability.

## Questions for the maintainers

Which compatibility surfaces must remain stable for existing consumers? Which framework/editor behaviours would be most costly to replace? Which recent language changes are the best maintenance exercises? What is the smallest useful coexistence arrangement for evaluating this work without disrupting current development?
