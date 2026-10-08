#!/usr/bin/env bash
# Validates docs/upgrades/index.json against schema.json + checks:
#   - schema validity (best-effort if jsonschema python package available)
#   - every entry's doc_path file exists
#   - filename Uxxx-... matches the entry's id
#   - every Uxxx reference in body markdown resolves to a known entry
#   - allow-listed entries (U008, U011, U012) carry the 9 mandated
#     sections under the new template adopted 2026-04-29
#     (per spec § 8 fix C1 — explicitly allow-listed scope, not retroactive
#     to U001-U007/U009/U010). Mirrors UpgradeManifestStructureTest so
#     docs-only PRs that ci.yml skips via paths-ignore still get gated.
#
# Usage:
#   upgrade-manifest-validate.sh                   # validate production manifest
#   upgrade-manifest-validate.sh --root DIR        # validate a fixture dir

set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
ROOT="$REPO_ROOT/docs/upgrades"
if [[ "${1:-}" == "--root" ]]; then
    if [[ $# -lt 2 || -z "${2:-}" ]]; then
        echo "::error::--root requires a directory argument" >&2
        echo "Usage: upgrade-manifest-validate.sh [--root DIR]" >&2
        exit 2
    fi
    ROOT="$2"
fi

INDEX="$ROOT/index.json"
SCHEMA="$ROOT/schema.json"

[[ -f "$INDEX" ]] || { echo "::error::missing $INDEX"; exit 1; }
[[ -f "$SCHEMA" ]] || { echo "::error::missing $SCHEMA"; exit 1; }

# Prefer python3 (CI runners + macOS + most Linux); fall back to python
# (some Windows / minimal environments). On Windows, "command -v python3"
# may locate a Microsoft Store redirect shim that exits non-zero on --version,
# so we probe with --version, not just PATH presence.
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

# Pass ROOT via env var (not string interpolation) so paths containing
# single quotes / backslashes don't break the embedded Python source.
# The single-quoted heredoc delimiter ('PYEOF') prevents bash from
# interpolating $vars or backticks inside the Python body.
export UPGRADE_MANIFEST_ROOT="$ROOT"

"$PYTHON" - <<'PYEOF'
import json, re, sys, os

ROOT = os.environ['UPGRADE_MANIFEST_ROOT']
INDEX = os.path.join(ROOT, 'index.json')
SCHEMA = os.path.join(ROOT, 'schema.json')

with open(INDEX, encoding='utf-8') as f: doc = json.load(f)
with open(SCHEMA, encoding='utf-8') as f: schema = json.load(f)

# Optional: JSON Schema 2020-12 validation if jsonschema available
try:
    from jsonschema import Draft202012Validator
    errs = list(Draft202012Validator(schema).iter_errors(doc))
    if errs:
        print('::error::index.json fails schema validation:', file=sys.stderr)
        for e in errs:
            print(f'  - {e.message} at {list(e.path)}', file=sys.stderr)
        sys.exit(1)
except ImportError:
    print('::warning::jsonschema not installed; skipping JSON-schema validation (lint passes still apply)', file=sys.stderr)

fail = 0

# Defensive shape + key access: when jsonschema is not installed (optional
# dep), malformed input would otherwise raise AttributeError/KeyError with
# a stack trace. Emit user-facing ::error::... messages instead so the
# lint stays actionable even without jsonschema. These checks duplicate
# schema.json's top-level required + type constraints; keep in sync.

# Root must be a JSON object.
if not isinstance(doc, dict):
    print(f'::error::index.json root must be a JSON object, got {type(doc).__name__}', file=sys.stderr)
    sys.exit(1)

# fork_baseline_version: required string matching schema's semver pattern
# ^\d+\.\d+\.\d+$. Schema also makes it required at top level.
SEMVER_PATTERN = re.compile(r'^\d+\.\d+\.\d+$')
fbv = doc.get('fork_baseline_version')
if fbv is None:
    print('::error::index.json missing required "fork_baseline_version" field', file=sys.stderr)
    fail = 1
elif not isinstance(fbv, str):
    print(f'::error::fork_baseline_version must be a string, got {type(fbv).__name__}', file=sys.stderr)
    fail = 1
elif not SEMVER_PATTERN.fullmatch(fbv):
    print(f'::error::fork_baseline_version "{fbv}" does not match semver pattern ^\\d+\\.\\d+\\.\\d+$', file=sys.stderr)
    fail = 1

# entries: required list at top level.
if 'entries' not in doc:
    print('::error::index.json missing required "entries" field', file=sys.stderr)
    sys.exit(1)
entries = doc['entries']
if not isinstance(entries, list):
    print(f'::error::"entries" must be a list, got {type(entries).__name__}', file=sys.stderr)
    sys.exit(1)

known_ids = set()
for idx, e in enumerate(entries):
    if not isinstance(e, dict):
        print(f'::error::entry [{idx}] must be an object, got {type(e).__name__}', file=sys.stderr)
        fail = 1
        continue
    eid = e.get('id')
    if not eid:
        print(f'::error::entry [{idx}] missing required "id" field', file=sys.stderr)
        fail = 1
        continue
    if eid in known_ids:
        print(f'::error::entry {eid} duplicate id at index [{idx}]', file=sys.stderr)
        fail = 1
    known_ids.add(eid)

# Manual filename pattern (matches schema.json's doc_path pattern). Also
# acts as a path-traversal guard — any '..', '/', '\\' would fail this regex
# before doc_path reaches os.path.join.
DOC_PATH_PATTERN = re.compile(r'^U\d{3}-[a-z0-9-]+\.md$')

for idx, e in enumerate(entries):
    if not isinstance(e, dict):
        # Already reported above; skip per-entry checks.
        continue
    eid = e.get('id')
    doc_path = e.get('doc_path')
    if not eid:
        # Already reported above; skip per-entry checks.
        continue
    # Type guards — without these, .startswith() / .startswith(...) on a
    # non-string raises AttributeError instead of an actionable ::error::.
    if not isinstance(eid, str):
        print(f'::error::entry [{idx}] id must be a string, got {type(eid).__name__}', file=sys.stderr)
        fail = 1
        continue
    if not doc_path:
        print(f'::error::entry {eid} missing required "doc_path" field', file=sys.stderr)
        fail = 1
        continue
    if not isinstance(doc_path, str):
        print(f'::error::entry {eid} doc_path must be a string, got {type(doc_path).__name__}', file=sys.stderr)
        fail = 1
        continue

    # Manual enum checks — these are also enforced by JSON Schema (see
    # docs/upgrades/schema.json), but we duplicate them in the no-jsonschema
    # defensive path so fixtures + validators behave identically across CI
    # environments regardless of whether the optional jsonschema package is
    # installed. Keep this list in sync with schema.json's enum properties.
    valid_status = {"Available", "Deprecated", "Removed"}
    valid_risk = {"low", "medium", "high"}
    if e.get('status') not in valid_status:
        print(f'::error::entry {eid} status "{e.get("status")}" not in {sorted(valid_status)}', file=sys.stderr)
        fail = 1
    if e.get('future_deprecation_risk') not in valid_risk:
        print(f'::error::entry {eid} future_deprecation_risk "{e.get("future_deprecation_risk")}" not in {sorted(valid_risk)}', file=sys.stderr)
        fail = 1

    # Filename matches id + path-traversal guard.
    # Reject any doc_path that doesn't match the schema-declared filename
    # regex (^U\d{3}-[a-z0-9-]+\.md$). This catches both id-mismatch
    # (Copilot R7) AND path-escape attempts like "U001-../../../etc/passwd"
    # before doc_path is fed to os.path.join (Copilot R8).
    if not DOC_PATH_PATTERN.fullmatch(doc_path):
        print(f'::error::entry {eid} doc_path "{doc_path}" does not match required filename pattern '
              f'^U\\d{{3}}-[a-z0-9-]+\\.md$', file=sys.stderr)
        fail = 1
        continue
    if not doc_path.startswith(f'{eid}-'):
        print(f'::error::entry {eid} doc_path "{doc_path}" does not start with "{eid}-"', file=sys.stderr)
        fail = 1

    # Doc file exists
    full = os.path.join(ROOT, doc_path)
    if not os.path.isfile(full):
        print(f'::error::entry {eid} doc_path "{doc_path}" does not exist at {full}', file=sys.stderr)
        fail = 1
        continue

    # Cross-link integrity
    with open(full, encoding='utf-8') as f:
        body = f.read()
    refs = set(re.findall(r'U\d{3}', body)) - {eid}
    unknown = refs - known_ids
    for ref in sorted(unknown):
        print(f'::error::entry {eid} ({doc_path}) references unknown {ref}', file=sys.stderr)
        fail = 1

# Template-section check (P1.4.3 T17). Allow-listed entries must carry the
# 9 mandated `## ` sections introduced 2026-04-29. Mirrors the JUnit
# `UpgradeManifestStructureTest`. Skipped for any allow-listed entry whose
# doc_path is absent under ROOT — fixtures may legitimately omit them.
#
# Allow-list keyed on entry id (post-Copilot R8-F1): resolve the actual
# doc_path via index.json so a future rename of an allow-listed entry
# (with matching index.json update) cannot silently bypass this gate.
# Filename-keyed allow-listing would have skipped a renamed entry.
TEMPLATE_ALLOW_LIST_IDS = frozenset(("U008", "U011", "U012"))
TEMPLATE_REQUIRED_SECTIONS = ("## Concept",
                              "## Why",
                              "## Structural changes",
                              "## BC Story",
                              "## How to use",
                              "## Migration",
                              "## Test coverage",
                              "## Coverage",
                              "## Cross-references")

allow_listed_doc_paths = [
    e['doc_path'] for e in entries
    if isinstance(e, dict)
    and e.get('id') in TEMPLATE_ALLOW_LIST_IDS
    and isinstance(e.get('doc_path'), str)
]

for fname in allow_listed_doc_paths:
    fpath = os.path.join(ROOT, fname)
    if not os.path.isfile(fpath):
        # Fixture dir without this allow-listed entry — skip silently.
        # (doc_path-existence is already enforced by the per-entry pass
        # earlier; if we got here without that pass failing, the file
        # exists or is genuinely fixture-omitted.)
        continue
    with open(fpath, encoding='utf-8') as f:
        body = f.read()
    for section in TEMPLATE_REQUIRED_SECTIONS:
        if section not in body:
            print(f'::error::{fname} missing required template section "{section}" '
                  f'(allow-listed under spec § 8 C1; see U006 depth benchmark)',
                  file=sys.stderr)
            fail = 1

# Orphan detection (added at PR #75 docs rationalisation per the U015-at-PR
# #73 catch). Every U-NNN markdown file in ROOT must have a corresponding
# entry in index.json. This catches the reverse of the existing per-entry
# doc_path-exists check: a manifest file landed without its index.json row
# (U015 backfill catch at PR #73 R2 F3 was this exact class).
known_doc_paths = {
    e['doc_path']
    for e in entries
    if isinstance(e, dict) and isinstance(e.get('doc_path'), str)
}
# Loose detector for "looks like a U-NNN .md filename" — used as the
# first filter on listdir output. Anything that passes this then routes
# by `DOC_PATH_PATTERN.fullmatch` (the canonical strict pattern at the
# top of this file) so we have a single source of truth on valid U-NNN
# filename shape. A file that matches the loose prefix but fails the
# strict pattern is a schema violation (uppercase / underscore / dot
# in the subject) and is reported separately from "orphan, no entry".
LOOSE_UNNN_PREFIX = re.compile(r'^U[0-9]{3}-')
orphans = []
schema_violations = []
for fname in sorted(os.listdir(ROOT)):
    if not fname.endswith('.md'):
        continue
    if not LOOSE_UNNN_PREFIX.match(fname):
        continue
    if not os.path.isfile(os.path.join(ROOT, fname)):
        # Defensive: prefix matches the U-NNN naming scheme but a
        # directory or symlink named the same way could land in listdir
        # output. Only treat actual files as candidates.
        continue
    if not DOC_PATH_PATTERN.fullmatch(fname):
        # Looks like U-NNN (U<3-digit>- prefix + .md suffix) but violates
        # the canonical filename pattern (lowercase + digits + hyphens
        # only). Distinct from "orphan, no entry" — flag separately.
        schema_violations.append(fname)
        continue
    if fname in known_doc_paths:
        continue
    orphans.append(fname)
for v in schema_violations:
    print(f'::error::U-NNN manifest filename violates DOC_PATH_PATTERN: {v} '
          f'(expected ^U\\d{{3}}-[a-z0-9-]+\\.md$ — lowercase subject, digits, hyphens only)',
          file=sys.stderr)
if schema_violations:
    fail = 1
if orphans:
    for o in orphans:
        print(f'::error::orphan U-NNN manifest file with no index.json entry: {o} '
              f'(every U-NNN .md must have a corresponding entry; see PR #73 R2 F3)',
              file=sys.stderr)
    fail = 1

if fail:
    sys.exit(1)

print(f'upgrade-manifest-validate: OK ({len(entries)} entries validated)')
PYEOF
