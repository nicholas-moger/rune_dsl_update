#!/usr/bin/env bash
# Run legacy rune/rosetta-maven-plugin per cell. Overwrites the cell's goldens
# directory with current-legacy emission. Snapshot the goldens first via
# snapshot-goldens.sh.
#
# Per-corpus emission layout (mirrors D11CorpusRegressionTest#resolveGoldensDir):
#   iso20022/*  → rosetta-source/target/classes/generated/java
#   others      → rosetta-source/src/generated/java
#
# Per-cell timing + Maven exit code recorded in the log via a trailing
# [summary] line; a marker file at docs/harness/logs/regen-<cell>.done is
# written on successful regen so the diff step can verify regen actually ran
# (avoids false-clean diff when snapshot exists but regen was skipped).
#
# Usage: ./run-legacy-regen.sh [cell-relative-path]
#   No arg → regen all cells in cells.txt (serial — one JVM at a time per project policy)
#   1 arg  → regen just that cell (e.g. cdm/cdm-6.15.0)
#
# Exit code: 0 if every cell succeeded; non-zero if ANY cell failed. The
# per-cell rc is preserved in the corresponding log; the summary counts
# failures and propagates.

# Intentional `-e` omission (Copilot R12 F7 — comment refreshed): this
# script omits `-e` because the regen subshell uses `rc=$?` to capture
# Maven's exit code and propagate it to FAILED_CELLS. With `-e` enabled,
# a non-zero `mvn` rc would abort before the rc capture.
# diff-as-cloned-vs-regen.sh also uses `set -uo pipefail` (no -e) for a
# similar rc-capture pattern; snapshot-goldens.sh and restore-goldens.sh
# use the stricter `set -euo pipefail`. All filesystem operations in
# this script + diff are individually rc-guarded — see Copilot R9 F1/F4/
# F5/F6 + R10 F2 + R11 S1/S2/I1/I2 + R12 F2 sweeps.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# Env-var overrides for integration testing (Copilot R14 F6). Defaults
# preserve production behavior; integration tests redirect to a synthetic
# tmp tree + PATH-based mvn mock so the destructive workflow (rm -rf
# java/, mvn invocation, marker write) is exercised without touching
# real corpus state.
TEST_CORPUS="${TEST_CORPUS:-$REPO_ROOT/test-corpus}"
CELLS_FILE="${CELLS_FILE:-$SCRIPT_DIR/cells.txt}"
LOG_DIR="${LOG_DIR:-$REPO_ROOT/docs/harness/logs}"
# Fail-fast on log-dir creation because `set -e` is off; otherwise an
# unwritable LOG_DIR would silently no-op and downstream `> "$log"` would
# then fail per-cell with confusing messages.
mkdir -p "$LOG_DIR" || { echo "[FAIL] cannot create log dir: $LOG_DIR" >&2; exit 1; }

# Refuse to run if cells.txt is missing/unreadable. Without this, the
# all-cells loop's input redirection would fail silently (no `set -e`),
# leaving zero iterations and reporting success despite no work done.
if [ ! -r "$CELLS_FILE" ]; then
    echo "[FAIL] cells list unreadable at: $CELLS_FILE" >&2
    exit 1
fi

# Pre-flight mvn discovery BEFORE any destructive per-cell work
# (Copilot R17 S1). The per-cell regen path does `rm -rf $java_dir`
# BEFORE invoking mvn — so on a machine without Maven (or with a
# broken PATH), the script would delete the live goldens then have
# mvn fail with rc=127. In all-cells mode the `regen_cell || true`
# loop would continue clearing every snapshotted cell. Fail-fast at
# script start instead.
if ! command -v mvn >/dev/null 2>&1; then
    echo "[FAIL] mvn not found on PATH — refusing to run because the per-cell" >&2
    echo "       regen path deletes goldens BEFORE invoking mvn. Install" >&2
    echo "       Maven or set PATH to include it, then re-run." >&2
    exit 1
fi

# Source shared validation + path-resolution helpers (Copilot R10 F4).
# Single source of truth for validate_cell_arg + goldens_parent_dir
# across all 4 harness scripts; eliminates drift surface that previously
# required parallel fixes in every script.
#
# Guarded source (Copilot R13 F3): `set -e` is off in this script, so a
# missing/unreadable common.sh would silently fail-the-source and let
# downstream code run into command-not-found for validate_cell_arg +
# goldens_parent_dir, bypassing the security-sensitive validator. Fail-
# fast on source failure instead.
# shellcheck source=./common.sh
if ! . "$SCRIPT_DIR/common.sh"; then
    echo "[FAIL] cannot source common.sh: $SCRIPT_DIR/common.sh" >&2
    exit 1
fi

# Track per-cell outcomes. Acceptance-gate semantics: a run that
# regenerates zero cells must NOT report success (Copilot R8 F7).
FAILED_CELLS=0
REGENERATED=0
NO_INPUT=0

regen_cell() {
    local cell_rel="$1"
    local cell_dir="$TEST_CORPUS/$cell_rel"
    local log="$LOG_DIR/legacy-regen-$(echo "$cell_rel" | tr '/' '-').log"
    local marker="$LOG_DIR/regen-$(echo "$cell_rel" | tr '/' '-').done"

    if [ ! -f "$cell_dir/pom.xml" ]; then
        echo "[skip] $cell_rel — no pom.xml (cell not cloned locally)"
        NO_INPUT=$((NO_INPUT + 1))
        return 0
    fi

    # Refuse to regen unless an as-cloned snapshot exists. Two sub-cases:
    #   (a) no goldens java/ tree AND no snapshot → cell out of harness scope
    #       (snapshot-goldens.sh checks "$java_dir", not "$goldens"; it skips
    #       cells whose generated/ parent exists but lacks the java/ subtree).
    #       Count as NO_INPUT, return 0.
    #   (b) java/ tree present AND snapshot missing → user forgot to snapshot.
    #       Refuse and count as failure so the user fixes the workflow.
    # This closes Copilot R8 F2 (no-goldens cell previously ran mvn + wrote
    # marker, leaving the diff phase with no comparison anchor) AND Copilot
    # R10 F2 (prereq asymmetry vs snapshot-goldens.sh — checking $goldens
    # would mis-classify a partial cell with generated/ but no java/ as a
    # FAILED missing-snapshot, even though snapshot-goldens.sh would have
    # skipped it as NO_INPUT).
    local goldens snap java_dir
    goldens="$(goldens_parent_dir "$cell_rel")"
    snap="${goldens}-as-cloned"
    java_dir="${goldens}/java"
    if [ ! -d "$snap" ]; then
        if [ ! -d "$java_dir" ]; then
            echo "[skip] $cell_rel — no goldens at $java_dir, no snapshot possible (cell out of harness scope)"
            NO_INPUT=$((NO_INPUT + 1))
            return 0
        fi
        echo "[FAIL] $cell_rel — refusing to regen: no as-cloned snapshot at $snap"
        echo "       Run snapshot-goldens.sh first (one-shot per cell)."
        FAILED_CELLS=$((FAILED_CELLS + 1))
        return 1
    fi

    # Validate snapshot is restorable BEFORE the destructive `rm -rf
    # $java_dir` below (Copilot R15 F5). The previous check only
    # verified $snap exists; an empty/malformed snapshot would let mvn
    # run after we cleared the live goldens, leaving NO restorable
    # baseline if regen also fails.
    if ! snapshot_has_java "$snap"; then
        echo "[FAIL] $cell_rel — refusing to regen: snapshot at $snap is malformed (no java/ or no .java files)"
        echo "       Remove the snapshot dir and re-run snapshot-goldens.sh to rebuild it."
        FAILED_CELLS=$((FAILED_CELLS + 1))
        return 1
    fi

    # Clear any stale marker from a previous run. The marker is only re-written
    # on successful regen below, so its absence ⇒ "no successful regen yet" and
    # diff-as-cloned-vs-regen.sh will refuse to diff. `set -e` is off in this
    # script, so check the rm explicitly — without this, a permission error
    # would silently leave the stale marker behind (Copilot R9 F6).
    if ! rm -f "$marker"; then
        echo "[FAIL] $cell_rel — cannot clear stale marker: $marker" >&2
        FAILED_CELLS=$((FAILED_CELLS + 1))
        return 1
    fi

    # Clear the existing goldens java/ tree before mvn emits. Without this,
    # any file legacy NO LONGER emits (e.g. a deletion since the goldens were
    # cloned) remains overlaid in the regen tree, and the later diff cannot
    # report it as `only-in-as-cloned` — masking deletions as a false-clean
    # result. `set -e` is off, so check the rm-rf explicitly (Copilot R9 F5).
    if [ -d "$java_dir" ]; then
        if ! rm -rf "$java_dir"; then
            echo "[FAIL] $cell_rel — cannot clear goldens java/ tree: $java_dir" >&2
            FAILED_CELLS=$((FAILED_CELLS + 1))
            return 1
        fi
    fi

    echo "[run] $cell_rel — mvn process-sources -pl rosetta-source -am (log: $log)"
    local start end rc elapsed count
    start=$(date +%s)
    (
        # Guard the cd so a missing/inaccessible cell directory cannot regen
        # the wrong project from the caller's cwd. rc=97 distinguishes
        # cd-failure from mvn-failure for the outer handler.
        cd "$cell_dir" || { echo "[FAIL] $cell_rel — cannot cd to $cell_dir" >&2; exit 97; }
        # process-sources is the unified superset across all 4 corpora
        # (PR #85 R2-F1; class-of-issue extension of R1's F3 sweep):
        # CDM/DRR/ISO use `com.regnosys.rosetta:rosetta-maven-plugin` bound
        # at `generate-sources`; rune-fpml uses `org.finos.rune:rune-maven-plugin`
        # bound at `process-sources`. `generate-sources` alone would silent-skip
        # rune-fpml's plugin (now in the cell list at uniform 9.83.0).
        mvn process-sources -pl rosetta-source -am \
            -Dmaven.build.cache.enabled=false -DskipTests -B -ntp \
            > "$log" 2>&1
    )
    rc=$?
    end=$(date +%s)
    elapsed=$((end - start))

    # Append a structured summary to the log so users inspecting the log
    # later see rc + elapsed alongside the Maven output (the header
    # contract promised both). Warn-only on append failure — mvn already
    # ran and the cell-level success/fail bookkeeping doesn't depend on
    # this log line being written (R9 indep NIT F2).
    printf '\n[summary] cell=%s rc=%s elapsed=%ss\n' "$cell_rel" "$rc" "$elapsed" >> "$log" \
        || echo "[WARN] $cell_rel — could not append summary to log: $log" >&2

    if [ "$rc" -eq 0 ]; then
        # Validate mvn ACTUALLY emitted a java/ tree with .java files
        # at the expected goldens path (Copilot R16 F1). The previous
        # code wrote the marker + REGENERATED++ on any rc=0, including
        # the silent-skip / wrong-output-path failure modes where mvn
        # succeeds but emits nothing usable. Aligning this with the
        # diff-stage MALFORMED REGEN check at the run-stage gate
        # prevents the marker from being written without an emission.
        if ! snapshot_has_java "$goldens"; then
            echo "       FAIL: mvn OK in ${elapsed}s but no .java emitted at $java_dir" >&2
            echo "              (possible silent-skip or wrong output path)" >&2
            FAILED_CELLS=$((FAILED_CELLS + 1))
            return 1
        fi
        # Same FS-guard class as the diff script counts (R11 indep sweep
        # extension N2). With `set -e` off, an unguarded find failure
        # would leave $count empty + the success echo would print "
        # .java files emitted", a confusing artifact. Capture rc + emit
        # a placeholder on failure rather than aborting — mvn already
        # succeeded AND emission is validated above, so the marker
        # write should proceed.
        if ! count=$(find "$java_dir" -name '*.java' 2>/dev/null | wc -l); then
            count="?"
            echo "[WARN] $cell_rel — could not count .java files at $java_dir" >&2
        fi
        # Marker file written here only on success; cleared at function start
        # so a failed run leaves no marker behind. diff-as-cloned-vs-regen.sh
        # refuses to diff when the marker is absent. `set -e` is off, so the
        # marker write rc must be checked explicitly — otherwise a write
        # failure would still report the cell as REGENERATED while the diff
        # would silently refuse to run (Copilot R9 F4).
        if ! date -u +%Y-%m-%dT%H:%M:%SZ > "$marker"; then
            echo "       FAIL: mvn OK in ${elapsed}s but cannot write marker: $marker" >&2
            FAILED_CELLS=$((FAILED_CELLS + 1))
            return 1
        fi
        echo "       OK in ${elapsed}s — $count .java files emitted at $java_dir (marker: $marker)"
        REGENERATED=$((REGENERATED + 1))
        return 0
    else
        echo "       FAIL rc=$rc in ${elapsed}s — see $log"
        FAILED_CELLS=$((FAILED_CELLS + 1))
        return "$rc"
    fi
}

if [ $# -gt 1 ]; then
    echo "[FAIL] too many arguments ($#); usage: $(basename "$0") [cell-relative-path]" >&2
    exit 2
fi

SINGLE_CELL_MODE=0
if [ $# -eq 1 ]; then
    SINGLE_CELL_MODE=1
    validate_cell_arg "$1"
    # Signal-style propagation (Copilot R14 F2): the original
    # `if ! regen_cell` collapsed all non-zero rc to exit 1, masking
    # SIGINT/SIGTERM from a Ctrl-C'd mvn run. Class-of-issue extension
    # of R13 F5 (all-cells loop). Capture rc + propagate signal rc as-is
    # so callers can distinguish interrupts from ordinary mvn failures.
    regen_cell "$1"
    rc=$?
    if [ "$rc" -ge 128 ]; then
        echo "[FAIL] $1 — interrupted by signal (rc=$rc), aborting harness run" >&2
        exit "$rc"
    fi
    if [ "$rc" -ne 0 ]; then
        echo ""
        echo "FAILED: 1 cell did not regenerate cleanly."
        exit 1
    fi
else
    while IFS='|' read -r cell version; do
        [[ "$cell" =~ ^[[:space:]]*# ]] && continue
        [ -z "$cell" ] && continue
        # Re-run path-safety check on each row — if cells.txt has been
        # edited to inject `..` / absolute paths / regex metas, validate
        # rejects before any mvn run (Copilot R8 F1).
        validate_cell_arg "$cell"
        # Capture rc explicitly so we can distinguish signal-style exits
        # (≥128, e.g. 130=SIGINT, 143=SIGTERM) from ordinary mvn failures
        # (1..127, already accounted in FAILED_CELLS by regen_cell).
        # `set -e` is off, so a non-zero return from regen_cell does NOT
        # abort the script — we capture rc directly without `|| true`
        # (which would have zeroed rc before capture, defeating the
        # purpose). With the previous swallow (Copilot R13 F5), a Ctrl-C
        # during a long mvn run let the loop launch the next mvn build
        # instead of aborting promptly. Now we abort on signal-style rc
        # but keep iterating past ordinary build failures.
        regen_cell "$cell"
        rc=$?
        if [ "$rc" -ge 128 ]; then
            echo "[FAIL] $cell — interrupted by signal (rc=$rc), aborting harness run" >&2
            exit "$rc"
        fi
    done < "$CELLS_FILE"
    echo ""
    if [ "$FAILED_CELLS" -gt 0 ]; then
        echo "FAILED: $FAILED_CELLS cell(s) did not regenerate cleanly."
        exit 1
    fi
fi

# Acceptance-gate fail-on-zero: a run that regenerates zero cells must
# NOT report success — the diff phase has no input. Single-cell mode that
# resolved to a NO_INPUT skip is also a failure (user asked for work that
# could not happen). Closes Copilot R8 F6 + F7.
if [ "$REGENERATED" -eq 0 ]; then
    if [ "$SINGLE_CELL_MODE" -eq 1 ]; then
        echo "FAILED: requested cell produced no regen output (NO_INPUT=$NO_INPUT)." >&2
    else
        echo "FAILED: zero cells regenerated (NO_INPUT=$NO_INPUT — test-corpus appears absent)." >&2
    fi
    exit 1
fi

if [ "$SINGLE_CELL_MODE" -eq 0 ]; then
    echo "All cells regenerated: REGENERATED=$REGENERATED, NO_INPUT=$NO_INPUT."
fi
