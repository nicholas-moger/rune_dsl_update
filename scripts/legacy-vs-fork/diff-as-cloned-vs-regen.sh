#!/usr/bin/env bash
# Per cell, diff the as-cloned snapshot of goldens against the legacy-regen output.
# Reports accurate per-file counts (NOT relying on `diff -r --brief`'s aggregate
# behaviour for directory-only-on-one-side cases).
#
# Per-corpus goldens layout (mirrors D11CorpusRegressionTest#resolveGoldensDir):
#   iso20022/*  → rosetta-source/target/classes/generated/java
#   others      → rosetta-source/src/generated/java
#
# Usage: ./diff-as-cloned-vs-regen.sh [cell-relative-path]
#   No arg → diff all cells in cells.txt
#   1 arg  → diff just that cell
#
# Cell classification (each cell falls into exactly one bucket):
#   skipped         — cell not cloned locally (no pom.xml AND no goldens dir);
#                     does not affect exit code
#   missing-prereq  — cell cloned locally but snapshot or regen missing;
#                     forces exit 1 (acceptance-gate semantics)
#   drift           — diff non-empty (any only-in-A/only-in-B/content-differs > 0);
#                     forces exit 1 (acceptance-gate semantics)
#   clean           — diff empty; contributes 0 to exit code
#
# Exit code: 0 iff every non-skipped cell is clean.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
# Env-var overrides for integration testing (Copilot R14 F5). Defaults
# preserve production behavior; integration tests redirect to a synthetic
# tmp tree + isolated SUMMARY/DIFF_DIR so the missing-prereq + drift
# workflows are exercised without touching the user's real harness
# evidence files.
TEST_CORPUS="${TEST_CORPUS:-$REPO_ROOT/test-corpus}"
CELLS_FILE="${CELLS_FILE:-$SCRIPT_DIR/cells.txt}"
DIFF_DIR="${DIFF_DIR:-$REPO_ROOT/docs/harness/diffs}"
LOG_DIR="${LOG_DIR:-$REPO_ROOT/docs/harness/logs}"
SUMMARY="${SUMMARY:-$REPO_ROOT/docs/harness/as-cloned-vs-regen-summary.md}"
# `set -e` is intentionally off in this script (mirroring run-legacy-regen.sh
# rc-capture pattern), so guard filesystem ops explicitly — otherwise an
# unwritable target would silently no-op and the harness would still report
# DIFFED counts for cells whose diff log + summary entries were never written.
mkdir -p "$DIFF_DIR" || { echo "[FAIL] cannot create diff dir: $DIFF_DIR" >&2; exit 1; }

# Refuse to run if cells.txt is missing/unreadable. Without this, the
# all-cells loop's input redirection would fail silently (no `set -e`),
# leaving zero iterations and reporting success despite no work done.
if [ ! -r "$CELLS_FILE" ]; then
    echo "[FAIL] cells list unreadable at: $CELLS_FILE" >&2
    exit 1
fi

# Source shared validation + path-resolution helpers (Copilot R10 F5).
# Single source of truth for validate_cell_arg + goldens_parent_dir
# across all 4 harness scripts; eliminates drift surface that previously
# required parallel fixes in every script.
#
# Guarded source (Copilot R13 F4): `set -e` is off in this script, so a
# missing/unreadable common.sh would silently fail-the-source and let
# downstream code run into command-not-found for validate_cell_arg +
# goldens_parent_dir, bypassing the security-sensitive validator. Fail-
# fast on source failure instead.
# shellcheck source=./common.sh
if ! . "$SCRIPT_DIR/common.sh"; then
    echo "[FAIL] cannot source common.sh: $SCRIPT_DIR/common.sh" >&2
    exit 1
fi

MISSING_PREREQ=0
DRIFT_CELLS=0
DIFFED=0
NO_INPUT=0

# Append a summary row with a WARN-on-failure guard (Copilot R14 F4 —
# sweep gap from R11 S1 / R12 F2). The success path uses a stricter
# guard at end-of-diff_cell because losing the success row would mean
# DIFFED++ without proof of work (acceptance-gate failure). Skip and
# error paths use this helper because the cell-level outcome is
# already tracked by NO_INPUT/MISSING_PREREQ counters — a dropped row
# is documentation loss but not a verdict change.
summary_row() {
    if ! echo "$1" >> "$SUMMARY"; then
        echo "[WARN] cannot append summary row to $SUMMARY (row dropped: $1)" >&2
    fi
}

# Helper: emit each non-empty line from input string; empty input → no output.
# Used to feed `comm` via process substitution. `echo "$var"` would emit a
# phantom blank line when `$var` is empty, inflating `comm | wc -l` counts.
emit_lines() {
    [ -z "$1" ] && return 0
    printf '%s\n' "$1"
}

# Truncate the summary file with a fresh header. Called ONLY after argument
# validation succeeds, so a rejected invocation (e.g. multi-arg, unknown
# cell, regex bypass) preserves the last successful harness output.
init_summary() {
    # Guard the truncate-and-write at the single chokepoint for writeability.
    # If the filesystem is unwritable, fail-fast here so the per-cell appends
    # below don't silently no-op and produce a "DIFFED but no summary entry"
    # inconsistency. Subsequent `>> "$SUMMARY"` appends share this FS and
    # would surface the same error via mvn/cmp/find — guarding only the
    # init keeps the fix surface tight (R9 indep IMPORTANT).
    if ! {
        echo "# Per-cell as-cloned vs legacy-regen diff summary"
        echo
        echo "Generated: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
        echo
        echo "| Cell | as-cloned files | regen files | only-in-as-cloned | only-in-regen | content-differs | verdict |"
        echo "|---|---|---|---|---|---|---|"
    } > "$SUMMARY"; then
        echo "[FAIL] cannot write summary: $SUMMARY" >&2
        exit 1
    fi
}

diff_cell() {
    local cell_rel="$1"
    local cell_dir="$TEST_CORPUS/$cell_rel"
    local goldens
    goldens=$(goldens_parent_dir "$cell_rel")
    local as_cloned="${goldens}-as-cloned"
    local regen="$goldens"
    local java_dir="${goldens}/java"
    local diff_log="$DIFF_DIR/diff-$(echo "$cell_rel" | tr '/' '-').log"

    # "Not cloned locally" detection: no pom.xml + no goldens dir + no snapshot.
    # Matches the skip semantics in snapshot-goldens.sh and run-legacy-regen.sh
    # so an optional cell (e.g. cdm/cdm-master-d11) does not force a hard fail
    # PER-CELL. The all-cells-all-skipped case is caught by the fail-on-zero
    # check at script end (Copilot R8 F6).
    if [ ! -f "$cell_dir/pom.xml" ] && [ ! -d "$regen" ] && [ ! -d "$as_cloned" ]; then
        echo "[skip] $cell_rel — cell not cloned locally (no pom.xml / goldens / snapshot)"
        summary_row "| $cell_rel | - | - | - | - | - | skipped (not cloned) |"
        NO_INPUT=$((NO_INPUT + 1))
        return 0
    fi

    # "Out of harness scope" detection (Copilot R11 F1): the cell is cloned
    # locally (pom.xml present) but has no goldens java/ tree AND no
    # as-cloned snapshot — matching the per-kind NO_INPUT skip that
    # snapshot-goldens.sh and run-legacy-regen.sh take when $java_dir is
    # absent. Without this branch the diff step would mis-classify the
    # cell as missing-prereq even though the previous two stages
    # legitimately skipped it.
    if [ ! -d "$java_dir" ] && [ ! -d "$as_cloned" ]; then
        echo "[skip] $cell_rel — pom.xml present but no goldens java/ tree and no snapshot (cell out of harness scope)"
        summary_row "| $cell_rel | - | - | - | - | - | skipped (out of harness scope) |"
        NO_INPUT=$((NO_INPUT + 1))
        return 0
    fi

    if [ ! -d "$as_cloned" ]; then
        echo "[FAIL] $cell_rel — no as-cloned snapshot (run snapshot-goldens.sh first)"
        summary_row "| $cell_rel | MISSING SNAPSHOT | - | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Validate snapshot has java/ tree with .java files (Copilot R15 F7).
    # Without this, an empty/malformed -as-cloned dir would pass the
    # existence check above, then the find enumeration would return 0
    # files, and if regen is also empty/malformed the diff would
    # report "clean" (0 only-in-A, 0 only-in-B, 0 differs) — a
    # false-clean verdict masking what is actually a destroyed baseline.
    if ! snapshot_has_java "$as_cloned"; then
        echo "[FAIL] $cell_rel — as-cloned snapshot at $as_cloned is malformed (no java/ or no .java files)"
        summary_row "| $cell_rel | MALFORMED SNAPSHOT | - | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    if [ ! -d "$regen" ]; then
        echo "[FAIL] $cell_rel — no regen output (run run-legacy-regen.sh first)"
        summary_row "| $cell_rel | - | MISSING REGEN | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Same validation for the regen side (Copilot R15 S3): if regen
    # ran but mvn emitted no java/ tree, the parent dir still exists
    # so the check above passes. Without validating java/, the diff
    # could pair an empty regen with the (already-validated) snapshot
    # and produce a misleading "drift / only-in-as-cloned=N" verdict
    # instead of flagging the regen as malformed.
    if ! snapshot_has_java "$regen"; then
        echo "[FAIL] $cell_rel — regen output at $regen is malformed (no java/ or no .java files)"
        summary_row "| $cell_rel | - | MALFORMED REGEN | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Regen-ran detection: immediately after snapshot-goldens.sh runs, `regen`
    # points at the original goldens dir which already exists. Running diff at
    # that point would falsely report "clean" because no regen has overwritten
    # the goldens. run-legacy-regen.sh writes a per-cell marker on successful
    # regen; if the marker is missing, refuse to diff so the acceptance gate
    # cannot fail-open.
    local marker="$LOG_DIR/regen-$(echo "$cell_rel" | tr '/' '-').done"
    if [ ! -f "$marker" ]; then
        echo "[FAIL] $cell_rel — no legacy regen marker at $marker"
        echo "       Run run-legacy-regen.sh first."
        summary_row "| $cell_rel | - | NO REGEN MARKER | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Same FS-guard class as Copilot R11 suppressed-S2 (ac_list/rg_list
    # capture below); R11 indep sweep extension I1 flagged these counts
    # too. With `set -e` off, an unguarded find failure would produce a
    # silent 0/truncated count that corrupts the per-cell summary row.
    local ac_count rg_count
    if ! ac_count=$(find "$as_cloned" -name '*.java' | wc -l); then
        echo "[FAIL] $cell_rel — cannot count as-cloned files: $as_cloned" >&2
        summary_row "| $cell_rel | COUNT FAIL | - | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi
    if ! rg_count=$(find "$regen" -name '*.java' | wc -l); then
        echo "[FAIL] $cell_rel — cannot count regen files: $regen" >&2
        summary_row "| $cell_rel | - | COUNT FAIL | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Per-file diff: list every .java file present on each side, classify into
    # only-in-A / only-in-B / differs / identical. `diff -r --brief` aggregates
    # directory-only-on-one-side into a single line, which undercounts files
    # under sparse trees. So we enumerate files explicitly.
    #
    # `set -e` is off, so capture each subshell rc explicitly (Copilot R11
    # suppressed-S2). Without these guards a partial or empty list from a
    # mid-walk permission flip or disappearing tree would silently produce
    # incorrect only-in / content-differs counts. `set -o pipefail` is on
    # so `find | sort`'s rc reflects find's failure too.
    local ac_list rg_list
    if ! ac_list=$(cd "$as_cloned" && find . -name '*.java' | sort); then
        echo "[FAIL] $cell_rel — cannot enumerate as-cloned tree: $as_cloned" >&2
        summary_row "| $cell_rel | ENUMERATION FAIL | - | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi
    if ! rg_list=$(cd "$regen" && find . -name '*.java' | sort); then
        echo "[FAIL] $cell_rel — cannot enumerate regen tree: $regen" >&2
        summary_row "| $cell_rel | - | ENUMERATION FAIL | - | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Same FS-guard class as I1 + suppressed-S2; R11 indep sweep extension
    # I2 flagged these comm captures too. `comm` operates on in-memory
    # strings (lower FS-failure surface than find above) but process
    # substitution open-failure under exotic conditions still propagates
    # a non-zero rc through pipefail. With `set -e` off, an unguarded
    # failure would produce silent 0 counts. Guard for consistency.
    local only_ac only_rg differs
    if ! only_ac=$(comm -23 <(emit_lines "$ac_list") <(emit_lines "$rg_list") | wc -l); then
        echo "[FAIL] $cell_rel — cannot compute only-in-as-cloned set" >&2
        summary_row "| $cell_rel | $ac_count | $rg_count | COMM FAIL | - | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi
    if ! only_rg=$(comm -13 <(emit_lines "$ac_list") <(emit_lines "$rg_list") | wc -l); then
        echo "[FAIL] $cell_rel — cannot compute only-in-regen set" >&2
        summary_row "| $cell_rel | $ac_count | $rg_count | $only_ac | COMM FAIL | - | missing-prereq |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    # Common files: byte-compare each. Collect the differing paths so the
    # per-cell drift log can list them (count alone leaves users unable to
    # inspect which common files actually differ).
    # cmp returns 0=identical, 1=content differs, 2=open/io error,
    # >=128=signal-style (SIGINT=130, SIGTERM=143). Original R12 F3
    # collapsed all >=2 to missing-prereq; R14 F3 splits signal-style
    # rc as its own propagation path so Ctrl-C during a long compare
    # aborts the harness instead of being swallowed by the all-cells
    # `diff_cell || true` pattern. Class extension of R13 F5.
    differs=0
    local differs_list=""
    local cmp_rc
    while IFS= read -r rel; do
        [ -z "$rel" ] && continue
        cmp -s "$as_cloned/$rel" "$regen/$rel"
        cmp_rc=$?
        if [ "$cmp_rc" -eq 0 ]; then
            : # identical, no-op
        elif [ "$cmp_rc" -eq 1 ]; then
            differs=$((differs + 1))
            differs_list+="${rel}"$'\n'
        elif [ "$cmp_rc" -ge 128 ]; then
            echo "[FAIL] $cell_rel — cmp interrupted by signal (rc=$cmp_rc) on $rel" >&2
            summary_row "| $cell_rel | $ac_count | $rg_count | $only_ac | $only_rg | CMP SIGNAL | interrupted |"
            return "$cmp_rc"
        else
            echo "[FAIL] $cell_rel — cmp read error rc=$cmp_rc on $rel" >&2
            summary_row "| $cell_rel | $ac_count | $rg_count | $only_ac | $only_rg | CMP ERROR | missing-prereq |"
            MISSING_PREREQ=$((MISSING_PREREQ + 1))
            return 1
        fi
    done < <(comm -12 <(emit_lines "$ac_list") <(emit_lines "$rg_list"))

    local verdict
    if [ "$only_ac" -gt 0 ] || [ "$only_rg" -gt 0 ] || [ "$differs" -gt 0 ]; then
        verdict="drift"
        DRIFT_CELLS=$((DRIFT_CELLS + 1))
    else
        verdict="clean"
    fi

    # Per-cell evidence log. `set -e` is off, so guard the block-redirect
    # rc explicitly (Copilot R11 suppressed-S1). Without this, an unwritable
    # $diff_log would silently skip the evidence write while the script
    # still appends a summary row + increments DIFFED, falsely reporting a
    # clean cell with no inspectable per-cell log.
    if ! {
        echo "Cell: $cell_rel"
        echo "as-cloned: $as_cloned ($ac_count files)"
        echo "regen:     $regen ($rg_count files)"
        echo "only-in-as-cloned: $only_ac"
        echo "only-in-regen:     $only_rg"
        echo "content-differs:   $differs"
        echo "verdict:           $verdict"
        echo
        if [ "$verdict" = "drift" ]; then
            echo "--- DETAILS ---"
            comm -23 <(emit_lines "$ac_list") <(emit_lines "$rg_list") | awk '{print "ONLY_AS_CLONED:  "$0}'
            comm -13 <(emit_lines "$ac_list") <(emit_lines "$rg_list") | awk '{print "ONLY_REGEN:      "$0}'
            emit_lines "${differs_list%$'\n'}" | awk '{print "CONTENT_DIFFERS: "$0}'
        fi
    } > "$diff_log"; then
        echo "[FAIL] $cell_rel — cannot write diff log: $diff_log" >&2
        summary_row "| $cell_rel | $ac_count | $rg_count | $only_ac | $only_rg | $differs | log-write-fail |"
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi

    echo "[diff] $cell_rel — ac=$ac_count rg=$rg_count only-ac=$only_ac only-rg=$only_rg differs=$differs verdict=$verdict (log: $diff_log)"
    # Final summary append on success path. Same FS-guard class as R11
    # I1/I2 + S1/S2 sweep (Copilot R12 F2). Without this, an append
    # failure to $SUMMARY would silently leave the acceptance-gate
    # summary missing the validated cell while DIFFED still increments.
    if ! echo "| $cell_rel | $ac_count | $rg_count | $only_ac | $only_rg | $differs | $verdict |" >> "$SUMMARY"; then
        echo "[FAIL] $cell_rel — cannot append summary row to: $SUMMARY" >&2
        MISSING_PREREQ=$((MISSING_PREREQ + 1))
        return 1
    fi
    DIFFED=$((DIFFED + 1))
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
    init_summary
    # Signal-style propagation (Copilot R14 F3 — sibling of regen.sh's
    # R13 F5 / R14 F2). Capture rc + propagate signal-style exits from
    # the cmp loop (return ≥128) so Ctrl-C during single-cell diff
    # aborts cleanly rather than swallowing the signal as ordinary fail.
    diff_cell "$1"
    rc=$?
    if [ "$rc" -ge 128 ]; then
        echo "[FAIL] $1 — diff interrupted by signal (rc=$rc), aborting harness run" >&2
        exit "$rc"
    fi
else
    init_summary
    while IFS='|' read -r cell version; do
        [[ "$cell" =~ ^[[:space:]]*# ]] && continue
        [ -z "$cell" ] && continue
        # Re-run path-safety check on each row — if cells.txt has been
        # edited to inject `..` / absolute paths / regex metas, validate
        # rejects before any filesystem enumeration (Copilot R8 F1 sweep).
        validate_cell_arg "$cell"
        diff_cell "$cell"
        rc=$?
        # Signal-style propagation in the all-cells loop too: Ctrl-C
        # during a long compare in cell N should abort the harness,
        # not silently continue to cell N+1 (Copilot R14 F3 + R13 F5).
        if [ "$rc" -ge 128 ]; then
            echo "[FAIL] $cell — diff interrupted by signal (rc=$rc), aborting harness run" >&2
            exit "$rc"
        fi
    done < "$CELLS_FILE"
fi

echo ""
echo "Summary written to: $SUMMARY"
echo "Counts: DIFFED=$DIFFED, NO_INPUT=$NO_INPUT, MISSING_PREREQ=$MISSING_PREREQ, DRIFT_CELLS=$DRIFT_CELLS"
EXIT_CODE=0
if [ "$MISSING_PREREQ" -gt 0 ]; then
    echo "WARNING: $MISSING_PREREQ cell(s) missing snapshot or regen — diff incomplete."
    EXIT_CODE=1
fi
if [ "$DRIFT_CELLS" -gt 0 ]; then
    echo "WARNING: $DRIFT_CELLS cell(s) show drift between as-cloned and legacy-regen — goldens may be stale."
    EXIT_CODE=1
fi
# Acceptance-gate fail-on-zero (Copilot R8 F6): a diff run that validates
# zero cells must NOT report success. Both single-cell and all-cells modes
# fail when DIFFED == 0 — single-cell because the user asked for one and
# got none; all-cells because the harness cannot be used as an acceptance
# gate if it can pass without checking anything.
if [ "$DIFFED" -eq 0 ]; then
    if [ "$SINGLE_CELL_MODE" -eq 1 ]; then
        echo "FAILED: requested cell produced no diff (NO_INPUT=$NO_INPUT, MISSING_PREREQ=$MISSING_PREREQ)." >&2
    else
        # Include both counts so the dominant failure mode is unambiguous:
        # NO_INPUT high → test-corpus absent; MISSING_PREREQ high → cells
        # cloned but snapshot/marker missing for every one of them.
        echo "FAILED: zero cells diffed (NO_INPUT=$NO_INPUT, MISSING_PREREQ=$MISSING_PREREQ — test-corpus absent if NO_INPUT dominates; snapshot/marker missing if MISSING_PREREQ dominates)." >&2
    fi
    EXIT_CODE=1
fi
exit "$EXIT_CODE"
