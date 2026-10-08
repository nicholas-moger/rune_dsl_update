#!/usr/bin/env bash
# Restore per-cell goldens from the snapshot created by snapshot-goldens.sh.
# Reverses run-legacy-regen.sh's overwrite of the goldens directory.
#
# Per-corpus goldens layout (mirrors D11CorpusRegressionTest#resolveGoldensDir):
#   iso20022/*  → rosetta-source/target/classes/generated/java
#   others      → rosetta-source/src/generated/java
#
# Usage: ./restore-goldens.sh [cell-relative-path]
#   No arg → restore all cells that have snapshots
#   1 arg  → restore just that cell
#
# Removes the snapshot after restoring. Idempotent (silently skips cells
# without a snapshot).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# Env-var overrides for integration testing (Copilot R14 F7). Defaults
# preserve production behavior; integration tests redirect to a synthetic
# tmp tree so the destructive restore path (mv snapshot → goldens) is
# exercised without depending on real corpus state.
TEST_CORPUS="${TEST_CORPUS:-$REPO_ROOT/test-corpus}"
CELLS_FILE="${CELLS_FILE:-$SCRIPT_DIR/cells.txt}"
LOG_DIR="${LOG_DIR:-$REPO_ROOT/docs/harness/logs}"

# Refuse to run if cells.txt is missing/unreadable. Without this, the
# all-cells loop's input redirection would fail silently and report success.
if [ ! -r "$CELLS_FILE" ]; then
    echo "[FAIL] cells list unreadable at: $CELLS_FILE" >&2
    exit 1
fi

# Source shared validation + path-resolution helpers (Copilot R10 F6).
# Single source of truth for validate_cell_arg + goldens_parent_dir
# across all 4 harness scripts; eliminates drift surface that previously
# required parallel fixes in every script. validate_cell_arg is
# especially critical here because restore_cell calls `rm -rf` on a
# path derived from cell_rel.
# shellcheck source=./common.sh
. "$SCRIPT_DIR/common.sh"

restore_cell() {
    local cell_rel="$1"
    local goldens
    goldens=$(goldens_parent_dir "$cell_rel")
    local snap="${goldens}-as-cloned"

    if [ ! -d "$snap" ]; then
        echo "[skip] $cell_rel — no snapshot at $snap"
        return 0
    fi

    # Validate snapshot is restorable BEFORE the destructive `rm -rf
    # $goldens` below (Copilot R15 F6). If $snap exists but is empty/
    # malformed, the previous code would replace live goldens with an
    # empty snapshot, destroying user state. Fail-fast and let the
    # user resolve the malformed snapshot manually.
    if ! snapshot_has_java "$snap"; then
        echo "[FAIL] $cell_rel — refusing to restore: snapshot at $snap is malformed (no java/ or no .java files)"
        echo "       Remove the snapshot dir manually if you intended to discard it; do NOT trust it as a restore source."
        return 1
    fi

    if [ -d "$goldens" ]; then
        rm -rf "$goldens"
    fi
    mv "$snap" "$goldens"

    # Clear the regen marker: restore overwrites the regen output that the
    # marker was attesting to. Without this, a later diff against a fresh
    # snapshot+regen cycle could trust a stale marker and fail-open.
    local marker="$LOG_DIR/regen-$(echo "$cell_rel" | tr '/' '-').done"
    rm -f "$marker"

    echo "[restore] $cell_rel — restored from snapshot (regen marker cleared)"
}

if [ $# -gt 1 ]; then
    echo "[FAIL] too many arguments ($#); usage: $(basename "$0") [cell-relative-path]" >&2
    exit 2
fi

if [ $# -eq 1 ]; then
    validate_cell_arg "$1"
    restore_cell "$1"
else
    while IFS='|' read -r cell version; do
        [[ "$cell" =~ ^[[:space:]]*# ]] && continue
        [ -z "$cell" ] && continue
        # Re-run path-safety check on each row — if cells.txt has been
        # edited to inject `..` / absolute paths / regex metas, validate
        # rejects before any destructive `rm -rf` (Copilot R8 F3).
        validate_cell_arg "$cell"
        restore_cell "$cell"
    done < "$CELLS_FILE"
fi

# Restore is intentionally idempotent: zero cells restored (because no
# snapshots exist) is a valid no-op, not a failure. No acceptance-gate
# fail-on-zero check needed here — that would conflict with the documented
# "Idempotent (silently skips cells without a snapshot)" contract in the
# usage header.
