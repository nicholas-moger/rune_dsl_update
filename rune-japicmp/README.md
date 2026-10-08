# rune-japicmp

**Layers 1 + 3** of the rune-dsl-plus four-layer parity gate (see the development decision log D1 — the decision log is gitignored per `/docs/decisions/` in `.gitignore`). Compares fork jars against upstream baselines using `com.github.siom79.japicmp:japicmp-maven-plugin`.

- **Layer 1** (parser ABI) — compares `rune-parser.jar` against pre-rename upstream Maven Central baseline `com.regnosys.rosetta:com.regnosys.rosetta:9.58.1` (D17, same local log). Landed P1.3.1 (PR #28). REPORT-ONLY through P1.x + P2.0.x.
- **Layer 3** (output-side jars) — compares HEAD `rune-java-generator.jar` against baseline `rune-java-generator.jar` captured from post-P2.0.3-close main HEAD `edda5ea`. Landed P2.1.1 T5 (REPORT-ONLY). Flips to ENFORCING at P2.1.6 (subject to spec § 6 R10 budget mitigation). **In this review snapshot the baseline cannot be rebuilt:** `edda5ea` is a development-repository commit that is not in this history, and no baseline jar is included (§ Layer 3 below).

## Scope note (P1.3.1) — REPORT-ONLY mode

D1 line 23-25 frames japicmp as gating "generated JAR on the same `.rosetta` inputs" (output-side parity between our `rune-java-generator` output and upstream's). `rune-java-generator` coverage is currently 4/~18 element kinds, so output-side gating over the full corpus is P2 scope. P1.3.1 delivers the INFRASTRUCTURE SCAFFOLDING with the placeholder pairing `rune-parser.jar` vs `com.regnosys.rosetta:9.58.1`.

**Enforcement is OFF** in P1.3.1. The pom's `<breakBuildOnBinaryIncompatibleModifications>` and `<breakBuildOnSourceIncompatibleModifications>` are both `false`. The gate runs on every invocation, writes its report to `target/japicmp/`, and returns `BUILD SUCCESS` regardless of diff count. This is because japicmp's `<includes>` filter the report output, NOT the comparison set — the full upstream tool-surface Xtext-era classes are genuinely absent from the fork, which would produce hundreds of `CLASS_REMOVED` diffs under enforcement. That is not a parity signal; it is the fork's deliberate re-implementation surfacing through an interface-side compare that D1 does not actually ask for.

Enforcement lights up in P2 when the gate is re-pointed at output-side jars (e.g., generated CDM POJOs from our `rune-java-generator` vs upstream-generated same). That's a two-file change: flip the two break-build flags back to `true`, and swap the `<oldVersion>`/`<newVersion>` pair to the output jars.

## Invoke locally

### Layer 1 (parser ABI) — default

Baseline resolves from Maven Central on first run (network access needed then); no install step required. This procedure runs from the review snapshot as written.

```bash
# Build the under-test rune-parser jar
mvn -f rune-parser/pom.xml package -DskipTests

# Run the gate (Layer 1 only; Layer 3 is skipped by default)
mvn -f rune-japicmp/pom.xml verify
```

### Layer 3 (output-side jars) — needs a supplied baseline jar

**Not runnable from this review snapshot alone.** Layer 3 compares the `rune-java-generator` jar built from this tree with a baseline `rune-java-generator` jar that the development repository built from its own commit `edda5ea` (the post-P2.0.3 close). That commit is not in this repository's history, so the development procedure that rebuilt the baseline from a worktree of it cannot be followed here, and no baseline jar is included. The execution stays skipped by default (`japicmp.layer3.skip=true`); activating it without the jar stops the build with `The path '…/.japicmp-baselines/baseline-rune-java-generator.jar' does not point to an existing file`.

**Prerequisite:** the baseline jar itself, that is the `rune-java-generator` jar built at development commit `edda5ea`, obtained from the development repository's maintainers with its provenance (the commit and the build command). A jar built from any other revision is a different comparison and must not be reported as this layer. With that jar in hand:

```bash
# 1. Place the supplied baseline (the directory is gitignored)
mkdir -p rune-japicmp/.japicmp-baselines
cp <supplied-baseline>.jar rune-japicmp/.japicmp-baselines/baseline-rune-java-generator.jar

# 2. Build the under-test rune-java-generator.jar
mvn -Dmaven.build.cache.enabled=false -f rune-parser/pom.xml install -DskipTests
mvn -Dmaven.build.cache.enabled=false -f rune-java-generator/pom.xml package -DskipTests

# 3. Run both gates (Layer 1 + Layer 3)
mvn -f rune-japicmp/pom.xml verify -Djapicmp.layer3.skip=false
```

In the development repository a hosted workflow (not included) built the baseline and ran these steps; this snapshot has no hosted CI ([CI](../docs/CI.md)).

## Output

- Layer 1 report: `rune-japicmp/target/japicmp/api-compat-gate.xml` (machine) and `api-compat-gate.html` (human). Filenames are driven by the `<id>api-compat-gate</id>` on the plugin execution in `pom.xml`.
- Layer 3 report: `rune-japicmp/target/japicmp/japicmp-layer-3-output-jars.xml` (machine) and `.html` (human). Filenames are driven by the `<id>japicmp-layer-3-output-jars</id>` on the plugin execution.
- In P1.3.1 report-only mode (Layer 1) + P2.1.1 report-only mode (Layer 3), the gates do **not** fail the build on binary- or source-incompatible diffs; they record them in the report and still return `BUILD SUCCESS` because the `breakBuildOn*Modifications` flags are `false` on both executions. Layer 3 enforcement is toggled on at P2.1.6 per the spec § 6 R10 budget (a single-line edit).

## Ignore-list discipline

Every `<exclude>` in `pom.xml` must carry an XML comment referencing a D-entry in the development decision log. The development reference note that sets out this discipline is not included in this snapshot. In this snapshot's pom the `<includes>` and `<excludes>` of both executions are not applied ([Source snapshot, finding 5](../docs/SOURCE-SNAPSHOT.md#findings-during-validation)).

## Rebaseline

Bump `<upstream.baseline.version>` in `pom.xml` (and the group/artifact if upstream publishes post-rename coordinates) in a dedicated rebaseline change. The development repository's pull-request template for such a change is not included.
