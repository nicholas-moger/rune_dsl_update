---
code: RUNE-017
name: INVALID_FILE_HEADER_FIELD
category: PARSER
since: "0.1.0"
status: active
doc_status: ready
---

# RUNE-017: Invalid fileHeader field

The parser found a malformed field inside a `fileHeader:` block. Common causes:
duplicate `version`, unknown sub-keyword, malformed `experimental` list bracket.

## Example

```rosetta
fileHeader:
  version "1.0"
  version "2.0"               // <-- only one versionField permitted

namespace com.test
type Foo:
  a string (1..1)
```

## Diagnostic output

```
3:2 extraneous input 'version' expecting {OVERRIDE, NAMESPACE, ...}
```

## Remediation

The grammar permits each fileHeader field at most once: `version`, `depends-on`,
`experimental`. Use a single `version` line and combine multiple values via the
list form for the relevant field:

```rosetta
fileHeader:
  version "2.0"
  depends-on "cdm.base", "iso20022.fpml"
  experimental: [strictTypes, multiVersion]
```

## Severity

ERROR — parsing cannot recover; the model header is rejected and downstream parsing may
also fail.

## Current routing

`RUNE-017` is **reserved** for grammar-rule-aware classification. Today the generic
ANTLR error path classifies the example above as `RUNE-001` or `RUNE-003`
(ExtraneousInput). Future `RuneErrorClassifier` refinement will route fileHeader-field-
specific failures here.

## Related

- U009 (File-level header block feature description)
