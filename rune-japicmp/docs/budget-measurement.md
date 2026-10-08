# japicmp gate — wall-clock budget

D5 pins API-compat at <2 minutes (120 seconds) per PR. This file records the most recent local measurement and the rule for re-measurement.

## Latest measurement (2026-04-24)

- Host: developer machine (Windows 11, 24-core, 127 GB RAM, Temurin JDK 21.0.8, Maven 3.9.11) — CI measurement follows separately in each `japicmp-gate` workflow run.
- Posture: P1.3.1 report-only (both `breakBuildOn*` flags false in `rune-japicmp/pom.xml`; the gate writes the report, never fails the build).
- Runs: 3 consecutive `mvn -f rune-japicmp/pom.xml verify -q` with `target/` wiped between runs (cold local cache; baseline jar stays resolved in `~/.m2/repository`). Cold-run penalty would be higher if the baseline jar weren't already resolved; that happens once per workstation, not per gate run.
- Observed wall-clock: **4.965 s / 4.998 s / 4.963 s** (all three runs essentially identical; no first-run outlier because baseline jar and plugin are already cached). Mean **≈ 4.98 s** — well inside the 120-second D5 budget (two orders of magnitude headroom).
- Baseline jar download from Maven Central: separate from the 120s measurement — happens during Maven's ordinary dependency resolution on first run (not timed here); cached via `actions/setup-java@v5 cache: maven` in CI.

## Re-measurement rule

Re-measure whenever:
- The `<includes>` scope widens (e.g., to multiple packages or toward output-side jars when P2 re-targets).
- The plugin version changes.
- A new ignore-list block expands scope traversal time materially.
- CI workflow timeout triggers (the workflow pins `timeout-minutes: 3` on the gate step; a timeout is the signal to re-measure).
- P2 flips `breakBuildOn*Modifications` back to `true` — runtime itself does not change, but if a break occurs, the timing around report-generation may shift.

A budget regression is a P1.3.1-class issue; document it with the same D-entry discipline as an ignore-list addition.
