---
code: RUNE-003
name: EXTRANEOUS_INPUT
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-003: Extraneous input

The parser found a token that should not be present at this position and recovered by skipping it.

## Example

```rosetta
namespace com.test
type Foo:
  a : string (1..1)   // unexpected ':' between attribute name and type
```

## Diagnostic output

```
3:4 extraneous input ':' expecting ID
```

## Remediation

Remove the unexpected token. In Rosetta, attribute syntax is `name type cardinality` — no colon between name and type.
