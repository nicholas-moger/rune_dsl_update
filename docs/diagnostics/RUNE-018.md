---
code: RUNE-018
name: INVALID_REGULATORY_REFERENCE_ARG
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-018: Invalid regulatoryReference argument

The parser found a malformed argument inside a `regulatoryReference(k = "v", ...)`
list. The grammar requires `validID = STRING` per argument; numeric values, missing
`=`, or non-quoted RHS values are rejected.

## Example

```rosetta
namespace com.test

type Trade:
  [regulatoryReference ESMA MiFIR_RTS article "1" (jurisdiction = 42)]   // <-- value must be STRING
  tradeId string (1..1)
```

## Diagnostic output

```
4:64 mismatched input '42' expecting STRING
```

## Remediation

Quote the value:

```rosetta
[regulatoryReference ESMA MiFIR_RTS article "1" (jurisdiction = "EU", effective = "2018-01-03")]
```

Layer-1 (P1.4.2) only stores values as raw strings; typed reconstruction (parsing dates
or numbers from STRING values) is reserved for downstream validation.

## Severity

ERROR — parsing cannot recover; the surrounding declaration's docReference is rejected.

## Current routing

`RUNE-018` is **reserved** for grammar-rule-aware classification. Today the generic
ANTLR error path classifies the example above as `RUNE-001` (UnexpectedToken) or
`RUNE-009` (MismatchedInput). Future `RuneErrorClassifier` refinement will route
regulatoryReference-arg-specific failures here.

## Related

- U010 (regulatoryReference named-arg list feature description)
