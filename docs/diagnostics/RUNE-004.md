---
code: RUNE-004
name: NO_VIABLE_ALTERNATIVE
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
antlr_exception: NoViableAltException
---

# RUNE-004: No viable alternative

The parser could not find any grammar alternative that matches the current token sequence.

## Example

```rosetta
namespace com.test
func Calc:
  inputs:
    a number (1..1)
  output:
    r string (1..1)
  set @ : "fn"   // '@' is not a viable expression start
```

## Diagnostic output

```
7:6 no viable alternative at input '@'
```

## Remediation

Replace the unrecognized construct with a valid expression. Rosetta expressions start with identifiers, literals, parentheses, or specific keywords (`if`, `switch`, etc.).
