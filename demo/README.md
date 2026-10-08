# demo — harness drivers and experiment inputs

This directory holds the drivers and small inputs behind the recorded experiments in `docs/EXPERIMENTS.md` and `docs/EVIDENCE.md`. It is not a supported command-line interface for the compiler; the Maven plugin is the supported entry point (`docs/MAVEN-CONSUMERS.md`).

| Path | Purpose |
|---|---|
| `harness/codegen/` | The replacement code-generation driver (`demo.harness.codegen.Main`): direct generation over a merged corpus, `ir-dump` and `ast-dump` for the spotlight model. Its `legacy/` sub-module is a separate driver for the released Rune 9.83.0 compiler; the two are never on one classpath. |
| `harness/exec/` | The execution harness: compiles a generated tree against one of two runtime legs (`legacy-rt` released runtime, `plus-rt` adapted runtime) and runs five workloads plus JMH benchmarks. |
| `build/` | Two small Maven consumers that generate Java from the same inputs with the released and the replacement plugin, plus `diff-trees.ps1` for byte comparison of two generated trees. |
| `nojvm/` | The Python ANTLR4 lane: parser generation from the same `.g4` grammars, model analytics and the structural-context (`ai_context.py`) experiment. |
| `spotlight/` | The one-file spotlight model used by the IR/AST dumps. |
| `corpus/builtins/` | The two Rune builtin model files (see `corpus/LICENSE-NOTE.md`). |

The merged demo corpus the drivers were measured on (CDM 5.38.0, ISO 20022 1.38.0, DRR 6.34.1) is not committed: reviewers acquire the models at the pins in `test-corpus/corpus-cells.tsv` and `docs/CORPUS-9.83.md` and point the drivers at them. All generated output and caches land under `demo/work/` (ignored).
