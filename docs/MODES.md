# Run modes 0–3

There are four documented run configurations. Mode 0 selects released Rune DSL; Modes 1–3 select the replacement compiler. Selection uses Maven plugin/runtime artifacts and, for the replacement routes, IR flags. There is not yet a single `mode=0..3` switch.

| Route | Parser and generation | Readiness |
|---|---|---|
| Mode 0: original Rune DSL | Released Rune 9.83.0: Xtext/EMF, original generators and runtime | **Ready — upstream 9.83 baseline**; no replacement phase |
| Mode 1: parity Java from AST | Rune Java parser (ANTLR4) → Java AST → parity Java | **Ready — phase 1.0 delivered**, within the tested 9.83 Java-generation scope; strengthened by 3.1 and 3.2 |
| Mode 2: Java via IR | AST → IR → parity Java, with explicit temporary AST-generator fallbacks | **WIP — phase 3.3 ACTIVE**; IR introduced in 2.0, native emission of remaining Java file families now being completed |
| Mode 3: optimised Java via IR | IR-selected optimisations; Java emission remains AST-hosted | **WIP — phase 3.0 emitter delivered**; full IR rehosting has no assigned phase and is not active |

Mode identifiers describe how to run the compiler; phase identifiers describe development milestones. **Phase 3.3 is active; 3.3.1 simplification is next.** See [the phase roadmap](ROADMAP.md). Phase 3.4 is SDK/LSP work; it is not the current identifier for optimised-Java IR rehosting.

| Route | Maven selection |
|---|---|
| 0 | Original consumer POM, released compiler plugin and released runtime dependency set |
| 1 | Replacement plugin; both IR properties `false` |
| 2 | Replacement plugin; `rosetta.generator.ir=true`; optimised property `false`; `rune-ir-java` plugin dependency |
| 3 | Replacement plugin; `rosetta.generator.ir.optimised=true`; reference property `false`; `rune-ir-java-optimised` plugin dependency |

Mode 1 is the delivered direct Java-generation route for the pinned 9.83 baseline; its [parity evidence](EVIDENCE.md) gives the model/file-kind population and named exceptions. This does not claim completion of the SDK/editor or every consumer's custom generators. Mode 2's fallbacks use the replacement AST generators, not an Xtext JVM. Mode 3 deliberately changes generated implementations and needs compilation and behavioural checks rather than legacy byte equality.

## Mode 0 and runtime selection

True Mode 0 requires both the released compiler and released runtime. The consumer keeps `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0` and the original dependency set, including `org.finos.rune:rune-runtime:9.83.0` (often through `com.regnosys.rosetta:com.regnosys.rosetta.lib:9.83.0`). It does not use the local snapshot runtime.

The adapted runtime implements lazy failure messages, parent-linked paths and deferred path construction directly. It has no general runtime flag that restores all original internals; removing Xtend dependencies and changing dependency versions are also artifact/build choices. Turning both IR flags off therefore cannot restore Mode 0. Select the released artifacts instead.

The existing execution harness demonstrates separate runtime selection through `legacy-rt` and `plus-rt` Maven profiles in `demo/harness/exec/pom.xml`. Each packages a different runtime/dependency set. The runtime choice is made at build/classpath level, not by toggling already-loaded classes. Those profile names belong to the harness; they are not profiles already installed in CDM or DRR.

That harness defaults to replacement-generated `gen-m1` sources even for `legacy-rt`. It demonstrates runtime selection independently of compiler selection; it is not by itself evidence of a full Mode 0 build. Record the actual generated-source origin as well as the resolved/loaded runtime. Its declared profile metadata alone cannot exclude extra classpath entries.

Its `--leg` argument verifies which jar was built; it does not switch the runtime inside that jar. Also, not every harness labelled legacy uses an untouched dependency graph: `demo/harness/codegen/legacy/pom.xml` overrides EMF common to 2.44.0. For the unchanged Mode 0 recipe, use the pinned consumer's original POM and record what it resolves, rather than assuming every historical baseline has identical dependencies.

Modes 1–3 can be tested against the released runtime first and the adapted runtime separately. Original compiler plus adapted runtime is a useful mixed configuration, but is not an unchanged Mode 0 baseline. Record compiler, runtime and companion-library versions for every comparison. Do not put both versions of the runtime classes on one application classpath.

## Build and invoke from a real project

1. For Mode 0, keep the consumer's released POM and use the original-build command in [Consumer POM integration](MAVEN-CONSUMERS.md#mode-0-original-rune-dsl).
2. For Modes 1–3, [install the compiler modules](BUILDING.md), then follow that guide's POM replacement and exact mode commands.
3. Keep model dependencies, resource filtering and downstream steps consistent across separate consumer checkouts. CDM 5.38.0 is the public example; DRR differences and other 9.83 projects are covered.

A GitHub URL or source-folder path does not replace Maven artifact coordinates. The consumer resolves the artifacts installed locally. A profile label or output-directory name alone does not select a route.

## Verify which route actually ran

For Mode 0, confirm the executed plugin is the released `com.regnosys.rosetta:rosetta-maven-plugin:9.83.0` and the application dependency tree contains the released runtime, with no snapshot replacement. The upstream plugin does not print the replacement's IR route header.

For Modes 1–3, require the following Maven plugin log header:

| Mode | Required log text |
|---|---|
| 1 | `IR route: OFF` |
| 2 | `IR route: ON (provider com.regnosys.rosetta.generator.java.ir.IRGenerationProviderImpl)` |
| 3 | `IR route: ON (provider com.regnosys.rosetta.generator.java.optimised.OptimisedIRGenerationProviderImpl)` |

Never enable both flags. A missing provider can allow generation to continue on Mode 1 with a warning: Maven exit zero alone does not prove Mode 2 or Mode 3 ran. Reject a requested-IR run whose route header is `OFF`, reports `NO provider`, or emits the missing-provider warning on stderr. Require nonzero source/output populations, inspect errors and per-family dispatch, and retain the complete log.

Provider selection does not mean every file is natively emitted from IR. At the choice milestone, IR-fed enum, datatype-unit and choice-unit writers own 93,703 of 174,141 reference files across the 25 real-model cells and 11 file kinds, excluding chaos. They retain legacy helpers/templates; this is historical ownership, not independent-emission certification. In that snapshot, other families use compatibility generation in whole or in part. A unit comprises the POJO and its five derived members (three validators, the meta registry and the deep-path utility). If a member refuses, the whole unit uses compatibility generation. The 25 real-model cells had no unit refusals at that milestone; 12 named datatype collision cases remain in the separate chaos cell. See [the guide's planning entry](../README.md#current-ir-capabilities) and [Evidence](EVIDENCE.md).

## Compare and compile

Compare Mode 1 and Mode 2 with independent upstream output over the same inputs, model-version filtering, namespace configuration and supported output families. The [corpus guide](CORPUS-9.83.md) gives pinned CDM inputs and D11 commands.

Compile generated Java and run the consumer's own tests for each mode. Keep the runtime fixed initially; test the adapted runtime separately as described in [the consumer guide](MAVEN-CONSUMERS.md). Optimised Java needs behavioural tests; its existing evidence has its own [scope](EVIDENCE.md). Full consumer-project compilation and integration acceptance are separate from corpus generation comparisons.

## Test profiles and diagnostic tools

The generator module's Surefire profiles `ir-on` and `ir-optimised-on` load provider jars and set flags for **compiler tests**. They are not existing profiles in CDM or DRR. Passing `-Pir-on` in an unmodified consumer does not select this plugin's route.

The existing `demo.harness.codegen.Main` is a diagnostic/demo entry point, not the SDK. Its `ir-dump` command can inspect the IR after packaging `demo/harness/codegen/pom.xml`. It directly constructs some generators instead of integrating every current provider hook; do not use that driver's generation result as proof of Maven-plugin native emission. Python declaration dumping is a prototype, not a complete Python business-code backend.

The SDK, editor/LSP surface and complete upstream/replacement integration remain planned contribution work. The current Maven plugin can exercise the three replacement routes independently of that future work.
