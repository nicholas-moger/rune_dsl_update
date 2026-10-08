# Review and contribution workflow

This is a private review repository for a proposed contribution to FINOS Rune DSL. Access is granted individually by the repository owner; the [LICENSE](LICENSE) permits authorised collaborators to clone, build and run the implementation locally to review it and give feedback, and it does not permit redistribution, derivative works or contributions to this repository without the owner's written permission. Those terms stand until the work is contributed and relicensed under FINOS governance.

Start with the [guide](README.md), [current plan](docs/phasing-progress.md) and [known limitations](docs/LIMITATIONS-AND-PLAN.md). The existing licence terms remain in force.

## Raising findings

Use the repository's Issues and Discussions. A useful finding names the trigger (input, command, revision), the affected path, the observable consequence and the supporting evidence (log lines, Surefire report, comparison receipt). Distinguish a confirmed defect from a design trade-off and from an untested hypothesis. Architectural questions and design alternatives are welcome as Discussions; [Design context](docs/DESIGN-CONTEXT.md) and [Limitations and plan](docs/LIMITATIONS-AND-PLAN.md) give the current reasoning and the questions already open.

## Evidence expected with a proposed change

Changes are proposed as review comments or patches to the owner rather than pushed here. A proposal should say which generation route and which test tier it affects, include the smallest reproducible failure where there is one, and report the actual executed and skipped populations of the suites it ran. Reference goldens, manifests and exception registers are not refreshed to remove a failure; a baseline change is its own reviewed proposal. See [Testing](docs/TESTING.md) and [AGENTS.md](AGENTS.md).

## Review cadence

Code drops follow validated milestones. Each is recorded in [CHANGELOG.md](CHANGELOG.md) with its source revision, capability changes, executed checks and known limitations. Documentation-only updates identify the unchanged code snapshot separately; they do not count as new code delivery.

## Discussion topics

Two topics are open for discussion with maintainers beyond the compiler work itself: compatibility of the future Python target with the existing Python runtime, and integration of the XSD importer with the replacement front end. Neither is implemented in this snapshot.
