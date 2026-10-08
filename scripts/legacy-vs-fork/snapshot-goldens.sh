#!/usr/bin/env bash
# Snapshot per-cell goldens before any legacy regen overwrites them.
#
# Per-corpus goldens layout (mirrors D11CorpusRegressionTest#resolveGoldensDir):
#   iso20022/*  → rosetta-source/target/classes/generated/java
#   others      → rosetta-source/src/generated/java
#
# Snapshot target lives one directory up from the .../java leaf, named with
# `-as-cloned` suffix on the parent (e.g. src/generated → src/generated-as-cloned,
# target/classes/generated → target/classes/generated-as-cloned).
#
# Usage: ./snapshot-goldens.sh [cell-relative-path]
#   No arg → snapshot all cells in cells.txt
#   1 arg  → snapshot just that cell (e.g. cdm/cdm-6.15.0)
#
# Idempotency + skip behavior:
#   - "Already snapshotted" cells (snapshot dir present) are silently skipped
#     in BOTH single-cell and all-cells modes — that's idempotent re-runs.
#   - "No goldens to snapshot" cells (e.g. `cdm/cdm-master-d11` not cloned
#     locally) are skipped PER-CELL in all-cells mode and the run still
#     succeeds. In SINGLE-CELL mode, requesting such a cell now FAILS the
#     fail-on-zero acceptance gate (the user explicitly asked for work that
#     could not happen). See Copilot R8 F6 + R9 F3.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# Env-var overrides for integration testing (Copilot R14 F8). Defaults
# preserve the production behavior; integration tests redirect to a
# synthetic tmp tree so destructive cp/rm workflows can be exercised
# without touching the real test-corpus or docs/harness state.
TEST_CORPUS="${TEST_CORPUS:-$REPO_ROOT/test-corpus}"
CELLS_FILE="${CELLS_FILE:-$SCRIPT_DIR/cells.txt}"
LOG_DIR="${LOG_DIR:-$REPO_ROOT/docs/harness/logs}"

# Refuse to run if cells.txt is missing/unreadable. Without this, the
# all-cells loop's input redirection would fail silently (no `set -e`),
# leaving zero iterations and reporting success despite no work done.
if [ ! -r "$CELLS_FILE" ]; then
    echo "[FAIL] cells list unreadable at: $CELLS_FILE" >&2
    exit 1
fi

# Source shared validation + path-resolution helpers (Copilot R10 F3).
# Single source of truth for validate_cell_arg + goldens_parent_dir
# across all 4 harness scripts; eliminates drift surface that previously
# required parallel fixes in every script.
# shellcheck source=./common.sh
. "$SCRIPT_DIR/common.sh"

# Track per-cell outcomes so the script fails-fast as an acceptance gate
# when zero cells were processed (acceptance-gate semantics — a run that
# validates nothing must not pass; user requested a cell that got skipped
# should also fail).
SNAPSHOTTED=0
ALREADY=0
NO_INPUT=0

snapshot_cell() {
    local cell_rel="$1"
    local goldens
    goldens=$(goldens_parent_dir "$cell_rel")
    local java_dir="$goldens/java"
    local snap="${goldens}-as-cloned"

    # Idempotency check FIRST (Copilot R12 F1): if a snapshot already
    # exists, treat as ALREADY regardless of whether the java/ tree is
    # currently present. The original ordering checked java/ first and
    # would mis-skip-as-NO_INPUT during recovery from an interrupted
    # regen that wiped goldens/java/ but left the snapshot intact.
    #
    # Validate snapshot has java/ tree with .java files (Copilot R15
    # F4): if the existing $snap dir is malformed (empty, no java/,
    # no .java files), do NOT treat it as a valid snapshot — auto-
    # clean and re-create. This script OWNS snapshot creation; a
    # malformed one indicates a previous failed run that we should
    # recover from.
    if [ -d "$snap" ]; then
        if snapshot_has_java "$snap"; then
            echo "[skip] $cell_rel — already snapshotted at $snap"
            ALREADY=$((ALREADY + 1))
            return 0
        fi
        echo "[WARN] $cell_rel — existing snapshot at $snap is malformed (no java/ or no .java files); re-creating" >&2
        rm -rf "$snap"
    fi

    if [ ! -d "$java_dir" ]; then
        echo "[skip] $cell_rel — no goldens at $java_dir"
        NO_INPUT=$((NO_INPUT + 1))
        return 0
    fi

    echo "[snap] $cell_rel — copying $goldens → $snap"
    # Stage to a `.tmp.$$` suffix and atomic-rename on success so a partial
    # `cp -r` (disk-full, perm error, ctrl-C) cannot leave a corrupt
    # `$snap` directory that a later rerun would treat as complete and skip
    # (Copilot R9 F7). The staging dir is in the same parent so the rename
    # is filesystem-atomic. Sweep any stale `.tmp.<otherpid>` litter from
    # prior crashed runs first (R9 indep NIT F3). Glob expansion stays safe
    # because `$snap` cannot contain whitespace under cells.txt's grammar
    # (validate_cell_arg's case pattern rejects whitespace).
    rm -rf "${snap}".tmp.* 2>/dev/null || true
    local staging="${snap}.tmp.$$"
    # Verify staging path is absent before cp (Copilot R15 S1): PID
    # reuse + a stale `.tmp.$$` surviving the cleanup above (perm error
    # ignored via `|| true`) would make `cp -r "$goldens" "$staging"`
    # copy INTO the existing dir rather than CREATE it, and the later
    # mv would promote that mixed-contents dir as a valid snapshot.
    # Fail-fast instead.
    if [ -e "$staging" ]; then
        echo "[FAIL] $cell_rel — stale staging dir survived cleanup at $staging" >&2
        exit 1
    fi
    if ! cp -r "$goldens" "$staging"; then
        echo "[FAIL] $cell_rel — cp -r failed; cleaning partial staging dir" >&2
        rm -rf "$staging"
        exit 1
    fi
    if ! mv "$staging" "$snap"; then
        echo "[FAIL] $cell_rel — atomic rename failed: $staging → $snap" >&2
        rm -rf "$staging"
        exit 1
    fi

    # Clear prior regen marker FIRST (Copilot R15 S2): with `set -euo
    # pipefail` on, an unguarded `find | wc` failure would abort BEFORE
    # the marker clear runs, leaving the exact fail-open state the
    # marker-clear is meant to prevent (snapshot just created, but
    # stale marker still says "regen has run against this snapshot").
    # Marker clear is FS-cheap and idempotent; do it first.
    local marker="$LOG_DIR/regen-$(echo "$cell_rel" | tr '/' '-').done"
    rm -f "$marker"

    local count
    count=$(find "$snap" -name '*.java' | wc -l)
    echo "       $count .java files snapshotted (any prior regen marker cleared)"
    SNAPSHOTTED=$((SNAPSHOTTED + 1))
    return 0
}

if [ $# -gt 1 ]; then
    echo "[FAIL] too many arguments ($#); usage: $(basename "$0") [cell-relative-path]" >&2
    exit 2
fi

SINGLE_CELL_MODE=0
if [ $# -eq 1 ]; then
    SINGLE_CELL_MODE=1
    validate_cell_arg "$1"
    snapshot_cell "$1"
else
    while IFS='|' read -r cell version; do
        [[ "$cell" =~ ^[[:space:]]*# ]] && continue
        [ -z "$cell" ] && continue
        # Re-run path-safety check on each row — if cells.txt has been
        # edited to inject `..` / absolute paths / regex metas, validate
        # rejects before any `cp -r` (Copilot R8 F4).
        validate_cell_arg "$cell"
        snapshot_cell "$cell"
    done < "$CELLS_FILE"
fi

# Acceptance gate: fail when zero cells were processed-or-already-snapshotted.
# Idempotent reruns (ALREADY > 0) are OK — they prove the snapshot exists.
# Single-cell mode: requested cell must have processed or already-existed.
# All-cells mode: at least one cell must have processed or already-existed.
if [ "$SNAPSHOTTED" -eq 0 ] && [ "$ALREADY" -eq 0 ]; then
    if [ "$SINGLE_CELL_MODE" -eq 1 ]; then
        echo "[FAIL] requested cell was skipped (no goldens to snapshot)" >&2
    else
        echo "[FAIL] no cells snapshotted (NO_INPUT=$NO_INPUT — test-corpus appears absent)" >&2
    fi
    exit 1
fi
