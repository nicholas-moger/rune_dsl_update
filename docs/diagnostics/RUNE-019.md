---
code: RUNE-019
name: INVALID_RUNE_ANNOTATION_NAME
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-019: Invalid rune annotation name

The parser found a rune annotation `@…` followed by a token that is not a valid
`qualifiedName`. Common causes: numeric prefix (`@123foo`), starting with
punctuation, illegal characters.

## Example

```rosetta
namespace com.test

@123notAValidName               // <-- annotation name must be a validID-shaped identifier
type Bar:
  x string (1..1)
```

## Diagnostic output

```
3:1 mismatched input '123notAValidName' expecting validID
```

## Remediation

Use a valid identifier (and optionally a dotted `qualifiedName`):

```rosetta
@experimental
type Bar:
  x string (1..1)
```

Or with a namespace prefix:

```rosetta
@my.fork.experimental
type Bar:
  x string (1..1)
```

## Severity

ERROR — parsing cannot recover; the surrounding declaration is rejected.

## Current routing

`RUNE-019` is **reserved** for grammar-rule-aware classification. Today the generic
ANTLR error path classifies the example above as `RUNE-001` (UnexpectedToken) or
`RUNE-009` (MismatchedInput). Future `RuneErrorClassifier` refinement will route
rune-annotation-name-specific failures here.

## Related

- RUNE-016 (invalid rune annotation **argument** — different problem class)
- U008 (Rune annotations feature description)
