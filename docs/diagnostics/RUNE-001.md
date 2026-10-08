---
code: RUNE-001
name: UNEXPECTED_TOKEN
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
antlr_exception: InputMismatchException
---

# RUNE-001: Unexpected token

The parser found a token that doesn't match what the grammar expected at this position.

## Example

```rosetta
namespace com.test
type Foo:
  a string (1..1)
  b string 1..1)   // missing opening paren
```

## Diagnostic output

```
4:11 mismatched input '1' expecting '('
```

## Remediation

Add the missing token (here, `(`) before the cardinality. Rosetta cardinality is always parenthesised: `(min..max)`.

## Related

- RUNE-002 (missing token)
- RUNE-003 (extraneous input)
- RUNE-009 (mismatched input — fallback when offending token type is uncertain)
