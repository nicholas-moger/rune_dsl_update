#!/usr/bin/env bash
# Shared helpers sourced by all 4 harness scripts (snapshot-goldens.sh,
# run-legacy-regen.sh, diff-as-cloned-vs-regen.sh, restore-goldens.sh).
#
# Single source of truth for the two security-sensitive functions that
# previously lived as drift-prone duplicate blocks in every script
# (Copilot R10 F3-F6). Keeps validation + path resolution in lock-step
# across all 4 entry points so future fixes can't drift between them.
#
# Caller contract (must set BEFORE sourcing this file):
#   - TEST_CORPUS — abs path to the test-corpus root
#   - CELLS_FILE  — abs path to cells.txt (already readability-verified)
#
# This file is sourced, not executed. It must not contain `set -e` or
# `exit` at top level — the calling script owns the shell environment.

# Reject path traversal + ensure the arg is a known cell from cells.txt.
# Critical because callers run `cp -r`, `rm -rf`, `mvn`, and diff against
# paths derived from this arg; an unvalidated `..` could escape the cell.
#
# Primary defense: case-pattern rejects path-traversal/absolute/newline
# input BEFORE the awk lookup. The awk literal field-1 comparison (NOT
# grep -E) prevents regex metacharacters in user-supplied single-cell
# args from matching cells they should not. In the all-cells loop the
# awk check is tautological (every row was read FROM cells.txt) but the
# case-pattern guard is the actual security gain there. Skip comments +
# blank lines to mirror the iteration loop.
validate_cell_arg() {
    local cell_rel="$1"
    # Allowed-character grammar (Copilot R13 F2 — supersedes R12 F6):
    # cells.txt entries match `[a-zA-Z0-9._/-]+` exactly. Anything else
    # (regex metas like `.*`, shell metas like `;|&$`, whitespace,
    # newlines, control chars) gets rejected by the `*[!...]*` clause.
    # This means callers can rely on the case pattern as the
    # gatekeeper without needing the awk cells.txt lookup as a
    # secondary defense against regex/shell-meta attacks. The
    # explicit `*..*`, `/*`, `*//*`, `*$'\n'*` clauses are kept for
    # self-documentation but are now subsumed by the character class.
    case "$cell_rel" in
        ""|*..*|/*|*//*|*$'\n'*|*[[:space:]]*|*[!a-zA-Z0-9._/-]*)
            echo "[FAIL] invalid cell path: '$cell_rel'"; exit 1 ;;
    esac
    if ! awk -F'|' -v cell="$cell_rel" '
        /^[[:space:]]*#/ {next}
        /^[[:space:]]*$/ {next}
        $1 == cell {f=1; exit}
        END {exit !f}
    ' "$CELLS_FILE"; then
        echo "[FAIL] cell '$cell_rel' is not listed in cells.txt"
        exit 1
    fi
}

# Per-corpus goldens parent dir (the directory whose subdir is "java/").
# Mirrors D11CorpusRegressionTest#resolveGoldensDir.
#   iso20022/* → rosetta-source/target/classes/generated
#   others     → rosetta-source/src/generated
goldens_parent_dir() {
    local cell_rel="$1"
    local corpus="${cell_rel%%/*}"
    if [ "$corpus" = "iso20022" ]; then
        echo "$TEST_CORPUS/$cell_rel/rosetta-source/target/classes/generated"
    else
        echo "$TEST_CORPUS/$cell_rel/rosetta-source/src/generated"
    fi
}

# Validate that a goldens-parent-shaped directory ($1) actually contains
# a usable java/ subtree with at least one .java file (Copilot R15
# F4-F7 + S3). Returns 0 if valid, 1 otherwise. Used by all 4 scripts
# to prevent treating an empty/malformed snapshot or regen tree as
# valid — without this, downstream cp/rm/diff can act on directories
# that look right (presence check passes) but contain no comparable
# content (false-clean diff verdicts, regen overwriting goldens with
# no restorable baseline, restore replacing live goldens with empty
# snapshot, etc.).
snapshot_has_java() {
    local dir="$1"
    [ -d "$dir/java" ] || return 1
    # Find any .java file (recursive). Short-circuit on first match
    # via `head -n 1` to keep this O(1) for large trees.
    local first
    first=$(find "$dir/java" -name '*.java' -type f 2>/dev/null | head -n 1)
    [ -n "$first" ]
}
