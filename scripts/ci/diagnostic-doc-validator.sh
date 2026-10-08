#!/usr/bin/env bash
# Validates docs/diagnostics/codes-registry.json against schema.json + checks:
#   - schema validity (best-effort if jsonschema python package available)
#   - every entry's doc file exists
#   - every RUNE-NNN.md is registered
#   - every sealed RuneErrorCode variant has a registry entry
#   - reports stub count (non-fatal)
#
# Usage:
#   diagnostic-doc-validator.sh                # validate production docs
#   diagnostic-doc-validator.sh --root DIR     # validate a fixture dir
#
# Spec: P1.4.1c §9 in docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md (local-only).

set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
ROOT="$REPO_ROOT/docs/diagnostics"
CODE_FILE="$REPO_ROOT/rune-parser/src/main/java/com/regnosys/rosetta/parser/diagnostics/RuneErrorCode.java"
if [[ "${1:-}" == "--root" ]]; then
    if [[ $# -lt 2 || -z "${2:-}" ]]; then
        echo "::error::--root requires a directory argument" >&2
        echo "Usage: diagnostic-doc-validator.sh [--root DIR]" >&2
        exit 2
    fi
    ROOT="$2"
fi

REGISTRY="$ROOT/codes-registry.json"
SCHEMA="$ROOT/schema.json"

[[ -f "$REGISTRY" ]] || { echo "::error::missing $REGISTRY"; exit 1; }
[[ -f "$SCHEMA" ]] || { echo "::error::missing $SCHEMA"; exit 1; }

PYTHON=
for cand in python3 python py; do
    if command -v "$cand" >/dev/null 2>&1 && "$cand" --version >/dev/null 2>&1; then
        PYTHON="$cand"
        break
    fi
done
if [[ -z "$PYTHON" ]]; then
    echo "::error::no working python3/python interpreter on PATH" >&2
    exit 1
fi

export DIAG_DOC_ROOT="$ROOT"
export DIAG_CODE_FILE="$CODE_FILE"

"$PYTHON" - <<'PYEOF'
import json, os, re, sys

ROOT = os.environ['DIAG_DOC_ROOT']
CODE_FILE = os.environ['DIAG_CODE_FILE']
REGISTRY = os.path.join(ROOT, 'codes-registry.json')
SCHEMA = os.path.join(ROOT, 'schema.json')

with open(REGISTRY, encoding='utf-8') as f: registry = json.load(f)
with open(SCHEMA, encoding='utf-8') as f: schema = json.load(f)

# Optional: JSON Schema 2020-12 validation
try:
    from jsonschema import Draft202012Validator
    errs = list(Draft202012Validator(schema).iter_errors(registry))
    if errs:
        print('::error::codes-registry.json fails schema validation:', file=sys.stderr)
        for e in errs:
            print(f'  - {e.message} at {list(e.path)}', file=sys.stderr)
        sys.exit(1)
except ImportError:
    print('::warning::jsonschema not installed; skipping JSON-schema validation', file=sys.stderr)

fail = 0

if not isinstance(registry, dict):
    print(f'::error::codes-registry.json root must be a JSON object, got {type(registry).__name__}', file=sys.stderr)
    sys.exit(1)

codes = registry.get('codes')
if not isinstance(codes, list):
    print(f'::error::"codes" must be a list, got {type(codes).__name__}', file=sys.stderr)
    sys.exit(1)

# 1. Every registered code has a corresponding .md file
registered_codes = set()
DOC_PATTERN = re.compile(r'^RUNE-\d{3}\.md$')
for idx, c in enumerate(codes):
    if not isinstance(c, dict):
        print(f'::error::codes[{idx}] must be an object, got {type(c).__name__}', file=sys.stderr)
        fail = 1
        continue
    code = c.get('code')
    doc = c.get('doc')
    if not code:
        print(f'::error::codes[{idx}] missing required "code" field', file=sys.stderr)
        fail = 1
        continue
    if code in registered_codes:
        print(f'::error::codes[{idx}] duplicate code {code}', file=sys.stderr)
        fail = 1
    registered_codes.add(code)
    if not doc or not DOC_PATTERN.fullmatch(doc):
        print(f'::error::{code} has invalid doc field "{doc}"', file=sys.stderr)
        fail = 1
        continue
    doc_path = os.path.join(ROOT, doc)
    if not os.path.exists(doc_path):
        print(f'::error::{code} doc file {doc_path} not found', file=sys.stderr)
        fail = 1

# 2. Every RUNE-NNN.md file is registered
for fname in os.listdir(ROOT):
    if DOC_PATTERN.fullmatch(fname):
        code_from_file = fname[:-3]  # strip .md
        if code_from_file not in registered_codes:
            print(f'::error::{fname} is not registered in codes-registry.json', file=sys.stderr)
            fail = 1

# 3. Every sealed RuneErrorCode variant has a registry entry
# Loose regex tolerates formatting variations (line breaks, extra whitespace,
# `return` on its own line). Asserts non-empty match-set so a future reformat
# can't silently turn this cross-check into a no-op.
if os.path.exists(CODE_FILE):
    with open(CODE_FILE, encoding='utf-8') as f:
        source = f.read()
    sealed_codes = set(re.findall(r'return\s+"(RUNE-\d{3})"', source))
    if not sealed_codes:
        print(f'::error::sealed-interface cross-check: no RUNE-NNN codes matched in {CODE_FILE}; '
              f'regex may have drifted from source format', file=sys.stderr)
        fail = 1
    for v in sealed_codes:
        if v not in registered_codes:
            print(f'::error::sealed RuneErrorCode variant {v} has no registry entry', file=sys.stderr)
            fail = 1
else:
    print(f'::warning::{CODE_FILE} not found; skipping sealed-interface cross-check', file=sys.stderr)

# 4. Stub count summary (non-fatal)
stub_count = sum(1 for c in codes if isinstance(c, dict) and c.get('doc_status') == 'stub')
print(f'::notice::diagnostic-doc-validator: {stub_count} stub pages (non-fatal)', file=sys.stderr)

if fail:
    sys.exit(1)
print('diagnostic-doc-validator: OK')
PYEOF
