---
code: RUNE-005
name: FAILED_PREDICATE
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
antlr_exception: FailedPredicateException
---

# RUNE-005: Failed predicate

The parser matched the token sequence syntactically but a semantic predicate (a runtime check embedded in the grammar) failed.

## Example

Currently no productions in `Rosetta.g4` use semantic predicates that would surface this code at runtime. Reserved for future grammar extensions (e.g., context-sensitive identifier rules).

## Remediation

Inspect the predicate name in the diagnostic message. It is typically a guard against context-incorrect identifiers (e.g., reserved word in a position where it's not allowed).
