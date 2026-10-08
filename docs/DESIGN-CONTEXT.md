# Design context and primary references

A shared semantic IR creates a reusable foundation for code generation, model interchange and future language targets. This guide explains those benefits, the compiler principles behind them and where to inspect their application in Rune. Current delivery status belongs in [Modes](MODES.md), the [snapshot record](SOURCE-SNAPSHOT.md) and [Roadmap](ROADMAP.md).

## Why a shared IR matters strategically

The MLIR research describes reusable compiler infrastructure spanning domains, abstraction levels and targets. It provides a concrete precedent for sharing representation and transformation infrastructure instead of building each compiler path independently. See [Lattner et al., MLIR: A Compiler Infrastructure for the End of Moore's Law (2020)](https://arxiv.org/abs/2002.11054).

For Rune, the strategic benefits are:

- **Portability:** carry resolved Rune meaning across target languages and execution environments through a shared semantic representation.
- **More code-generation options:** reuse the frontend's understanding of types, expressions and rules when adding Java, Python, C# or Rust emitters. New targets can focus on their language and runtime mappings.
- **Bridging opportunities:** expose explicit types, cardinalities, references and annotations as inputs to mappings into other metamodels, DSLs or intermediate representations.
- **Future flexibility:** give new backends a defined representation to consume, reducing the need to rebuild language analysis when target languages, libraries or deployment requirements change.
- **Optimisation and tooling:** provide a common place for semantic transformations and structured queries, supporting optimised generation, SDK services and agent access.

These are the architectural reasons for investing in IR. Realising each target or bridge requires a defined semantic mapping and validation; their delivery status remains separate from the representation's strategic value. A Java implementation language does not itself make an IR Java-specific: neutrality depends on the representation's contracts and its separation from target-specific emission.

## Compilation and runtime

For the generated-Java routes described here, IR construction, analysis and emission occur during compilation. The resulting Java runs with its selected runtime libraries; executing it adds no IR interpretation or translation stage. The IR is compiler infrastructure, not an extra layer traversed on each business-function call.

The inspected `rune-runtime` production dependencies contain no Rune IR module. `RunePluginRunner` selects IR generation during the build, and emitters such as `IRDataTypeEmitter` produce Java source. Compilation time and memory remain separate measurements. Execution performance depends on the emitted Java and runtime implementation, including any optimisations applied; the separation above is an architectural fact, not a new claim that compilation overhead was measured as negligible. See [Build and Maven integration](MAVEN-CONSUMERS.md) and [Evidence](EVIDENCE.md).

## What AST and IR mean here

An abstract syntax tree (AST) represents the language's declarations and expressions without retaining every punctuation token. LLVM's frontend tutorial first builds an AST, then generates a separate IR from its nodes. This is an established separation of compiler stages. See [LLVM: parser and AST](https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/LangImpl02.html) and [LLVM: AST to IR](https://llvm.org/docs/tutorial/MyFirstLanguageFrontend/LangImpl03.html).

In this project, the Java AST represents Rune source constructs; workspace services resolve names and supply semantic information. The additional IR makes the facts needed by analysis and generation explicit. The distinction is practical: what each representation contains and which consumers depend on it. An AST is itself an intermediate representation in the broad sense; "IR" here names the separate semantic layer used by the new generation route. See [Architecture](ARCHITECTURE.md).

## Abstraction levels and incremental migration

MLIR's Toy tutorial progressively converts operations between dialects and reuses optimisations at the appropriate level. That demonstrates why a compiler need not collapse all domain information into one low-level form immediately. See [MLIR: partial lowering for optimisation](https://mlir.llvm.org/docs/Tutorials/Toy/Ch-5/).

Rune's temporary AST-generator fallbacks are a migration mechanism, not equivalent to MLIR dialect conversion. An IR node can exist while the old generator still writes the Java. Native emission requires the new writer to consume the required IR facts independently, with its actual output and writer population checked. Shadow equality establishes a comparison result; activation is a separate fact. [Testing](TESTING.md) and [Modes](MODES.md) explain these boundaries.

The development investment is in adapters, representation contracts and verification. Those components establish reusable, independently testable boundaries for subsequent targets and tooling. This rationale does not imply that the previous framework could not support an IR, other generators or agent integration.

## Related implementation choices

| Choice | Primary source and relevant principle | Responsibility retained by Rune |
|---|---|---|
| ANTLR4 | [ANTLR](https://www.antlr.org/) generates a parser and parse-tree support from a grammar. | AST construction, linking, semantic validation and editor behaviour remain implementation work. Parser generation is not a complete language workbench. |
| StringTemplate 4 | [StringTemplate](https://www.stringtemplate.org/) explicitly supports source-code generation. | Templates format output; Java helpers and semantic services determine what should be emitted. Template suitability does not prove generated-program correctness. |
| SDK and LSP | [Microsoft's LSP documentation](https://microsoft.github.io/language-server-protocol/) defines communication between language servers and editors so language services can be reused across clients. | The planned SDK supplies shared services; LSP exposes appropriate editor operations. Incomplete-input handling, navigation, rename and responsiveness need their own implementation and parity tests. A completed generation IR is not a complete editor model. |

## Where to inspect the application in Rune

Use these entry points to trace the relevant behaviour in the selected source snapshot.

| Question | Source or evidence starting point |
|---|---|
| What is the parsed source model, and where are names resolved? | `rune-parser`: `com.regnosys.rosetta.ast.model.RModel` and `com.regnosys.rosetta.symbols.RWorkspace` |
| What facts are represented explicitly? | `rune-ir`: `com.regnosys.rosetta.ir.adapter.IRTypeNode`, `com.regnosys.rosetta.ir.core.IRFunction`, and the `ir.expr` package |
| Are declaration and derived facts checked against the existing implementation? | `rune-ir-java`: `IRModelReconciler` and `IRDerivedFactsReconciler` |
| Which writer actually owns an output, and when is a unit refused? | `rune-ir-java`: `IRTypeUnit`, `IRTypeUnitWiring`, `IRUnitPass` and the member emitters |
| Where does generation still depend on the AST pipeline? | `rune-ir-java`: `IRFunctionGenerator` and `IRExpressionCompiler`; inspect delegated rendering and declines rather than relying on class names |
| What checks the generated result? | `rune-java-generator`: `D11CorpusRegressionTest`; [Testing](TESTING.md), [Chaos gate](CHAOS-GATE.md) and [Evidence](EVIDENCE.md) |

MLIR's own guidance emphasises representation specifications, verifiers, inspectable forms and tests. That is a useful review standard for a custom IR. Check each Rune claim against actual representation fields, transformation behaviour and independent expectations; external precedent is supporting context, not validation. See [MLIR: compiler infrastructure](https://mlir.llvm.org/#compiler-infrastructure).

References checked 22 September 2026. Recheck them when the relevant design changes; fresh browsing is not required for every routine build. The sources provide technical background, not Rune results or a reconstructed history of which papers informed each original decision. This page adds no LLVM or MLIR dependency and makes no new completion, performance or AI answer-quality claim.
