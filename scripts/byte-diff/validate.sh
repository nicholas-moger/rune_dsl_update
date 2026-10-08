#!/usr/bin/env bash
# validate.sh — schema-validate every byte-diff catalogue entry against
# docs/byte-diff-catalogue/schema.json (D5 (local) 14-field contract).
#
# Invoked two ways:
#   1. byte-diff-catalogue.yml schema-validate job — no args, validates
#      every committed entry under docs/byte-diff-catalogue/ except
#      index.json (generator-owned) and schema.json (the schema itself).
#   2. Smoke test — pass entry path(s) as argv (one or more).
#
# Requires: check-jsonschema on PATH (CI workflow installs it via
# `pip install check-jsonschema`).
set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
SCHEMA="$REPO_ROOT/docs/byte-diff-catalogue/schema.json"

if [[ ! -f "$SCHEMA" ]]; then
    echo "::error::validate.sh: schema not found at $SCHEMA" >&2
    exit 1
fi

if [[ $# -eq 0 ]]; then
    # Validate every entry under docs/byte-diff-catalogue/ excluding
    # index.json and schema.json. -print0 / read -d '' for paths with
    # spaces (defensive — shouldn't occur in our tree).
    mapfile -d '' -t FILES < <(
        find "$REPO_ROOT/docs/byte-diff-catalogue" -type f -name '*.json' \
            ! -name 'index.json' ! -name 'schema.json' -print0 2>/dev/null
    )
    if [[ ${#FILES[@]} -eq 0 ]]; then
        echo "::warning::validate.sh: no entry files under docs/byte-diff-catalogue/" >&2
        exit 0
    fi
else
    FILES=("$@")
fi

if ! command -v check-jsonschema >/dev/null 2>&1; then
    echo "::error::validate.sh: check-jsonschema not on PATH (install: pip install check-jsonschema)" >&2
    exit 1
fi

# In check-jsonschema 0.28.x, format validation (date, uuid, etc.) is
# enabled by default for known formats — no flag needed. Use
# `--disable-formats` to opt out, which we do not want.
check-jsonschema --schemafile "$SCHEMA" "${FILES[@]}"
