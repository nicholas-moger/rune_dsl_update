---
code: RUNE-002
name: MISSING_TOKEN
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-002: Missing token

The parser inserted a synthetic token to recover from a missing required token.

## Example

```rosetta
namespace com.test
type Foo
  a string (1..1)   // missing colon after type name
```

## Diagnostic output

```
2:8 missing ':' at 'a'
```

## Remediation

Insert the missing token reported by the diagnostic (here, `:` after the type name).
