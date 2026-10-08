---
code: RUNE-016
name: INVALID_RUNE_ANNOTATION_ARG
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-016: Invalid rune annotation argument

The parser found a malformed argument inside a rune annotation's parenthesis list.
Common causes: missing `=` between name and value, wrong literal type, trailing
comma.

## Example

```rosetta
namespace com.test

@feature(name "missing-equals")        // <-- '=' missing
type Bar:
  x string (1..1)
```

## Diagnostic output

```
3:14 mismatched input '"missing-equals"' expecting '='
```

## Remediation

Use the form `name = value` with an explicit `=`:

```rosetta
@feature(name = "strictTypes")
type Bar:
  x string (1..1)
```

Acceptable literal forms for the value: STRING, INT, BIG_DECIMAL, `True` / `False`,
or a `qualifiedName` reference (e.g. `target = my.pkg.Type`).

## Severity

ERROR — parsing cannot recover; the type/choice/enum/function/rule declaration carrying
the malformed annotation is rejected.

## Current routing

`RUNE-016` is **reserved** for grammar-rule-aware classification. Today the generic
ANTLR error path classifies the example above as `RUNE-001` (UnexpectedToken) or
`RUNE-009` (MismatchedInput). Future `RuneErrorClassifier` refinement will route
rune-annotation-arg-specific failures here.

## Related

- RUNE-019 (invalid rune annotation **name** — different problem class)
- U008 (Rune annotations feature description)
