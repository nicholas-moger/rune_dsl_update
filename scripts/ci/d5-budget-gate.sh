#!/usr/bin/env bash
# d5-budget-gate.sh — enforces D5 (local) tiered wall-clock budgets
# against cell-timing artefacts uploaded by CI matrix cells.
#
# D5 (rebaseline-log, local) tier matrix — per matrix cell (each
# `${prefix}-<corpus>-<version>.seconds` artefact corresponds to one
# matrix cell run, which may include multiple test methods/classes):
#
#   prefix='cell'       (corpus-regression in rune-parser: CorpusParseTest +
#                        StructuralComparisonTest +
#                        BackCompatCorpusOrthogonalityTest +
#                        AstCorpusRegressionTest — last two added in P1.7
#                        PR-3 T5)
#     pull_request → 900s   (per-PR <15 min — was 300s pre-PR-3 T5;
#                            raised to 900s when matrix scope expanded
#                            from 2 tests to 4. DRR cells observed
#                            817s + 830s on cc92096; 900s gives ~10%
#                            headroom + matches the d11-cell tier.)
#     schedule     → 1800s  (nightly <30 min)
#     other        → 3600s  (push / release / manual <60 min)
#
#   prefix='d11-cell'   (P1.7 PR-2 d11-corpus-regression:
#                        D11CorpusRegressionTest in rune-java-generator)
#     pull_request → 900s   (per-PR <15 min — accommodates D11 ~6 min
#                            + mvn process-sources ~30s-5 min (ISO dominates)
#                            + slack; PR #85 unified goldens step,
#                            was ISO-only mvn install pre-rebaseline)
#     schedule     → 2700s  (nightly <45 min)
#     other        → 3600s  (push / release / manual <60 min)
#
# A cell exceeding its tier's budget is a silent performance regression —
# this script surfaces it as a CI failure (::error::).
#
# Usage:
#   d5-budget-gate.sh <event_name> <timings_dir> [prefix]
#
# Arguments:
#   event_name   — GitHub event name (workflows pass ${{ github.event_name }};
#                  recognised values: 'pull_request' (PR tier) and 'schedule'
#                  (nightly tier). Any other value — e.g. 'push',
#                  'workflow_dispatch', 'release' — falls through to the
#                  3600s tier per the case-statement default arm.
#   timings_dir  — directory containing <prefix>-<corpus>-<version>.seconds files
#   prefix       — optional file prefix; default 'cell' (existing tier).
#                  Use 'd11-cell' for the P1.7 PR-2 D11 dispatch tier.
#
# Exit code:
#   0 — all cells within budget (or no cells present)
#   1 — at least one cell exceeded its tier's budget, OR an artefact
#       was malformed (non-integer content)
set -euo pipefail

EVENT="${1:-}"
DIR="${2:-.timings}"
PREFIX="${3:-cell}"

# Validate PREFIX exactly before tier selection. Permissive glob (`*-cell)`,
# `*-d11-cell)`) on a combined `$EVENT-$PREFIX` key would silently accept any
# prefix ending in `-cell` (e.g. typo `d11-cel-cell`) and pair it with the
# fall-through 3600s arm; the for-loop glob below (`${PREFIX}-*.seconds`)
# would then match no files and the gate would silently pass (FAIL=0).
# Tighten to exact-match validation up-front per Copilot R5-F1.
case "$PREFIX" in
    cell|d11-cell) ;;
    *)
        echo "::error::d5-budget-gate.sh: unknown prefix '$PREFIX' (expected 'cell' or 'd11-cell')"
        exit 1
        ;;
esac

# PREFIX now guaranteed to be exactly 'cell' or 'd11-cell'. Per-tier budget:
case "$PREFIX" in
    cell)
        case "$EVENT" in
            pull_request) BUDGET=900 ;;   # per-PR <15 min — was 300s pre-PR-3 T5; raised when corpus-regression scope expanded from 2 tests (CorpusParseTest + StructuralComparisonTest) to 4 (added BackCompatCorpusOrthogonalityTest + AstCorpusRegressionTest)
            schedule)     BUDGET=1800 ;;  # nightly <30 min
            *)            BUDGET=3600 ;;  # push / release / manual <60 min
        esac
        ;;
    d11-cell)
        case "$EVENT" in
            pull_request) BUDGET=900 ;;   # per-PR <15 min (D11 dispatch tier)
            schedule)     BUDGET=2700 ;;  # nightly <45 min
            *)            BUDGET=3600 ;;  # push / release / manual <60 min
        esac
        ;;
esac

FAIL=0
shopt -s nullglob
for f in "$DIR"/${PREFIX}-*.seconds; do
    [ -f "$f" ] || continue
    secs=$(tr -d '[:space:]' < "$f")
    # Validate the artefact content — integer seconds only.
    if ! [[ "$secs" =~ ^[0-9]+$ ]]; then
        echo "::error::D5 gate: $(basename "$f") is not a valid integer seconds value: '$secs'"
        FAIL=1
        continue
    fi
    cell=$(basename "$f" .seconds | sed "s/^${PREFIX}-//")
    if [ "$secs" -gt "$BUDGET" ]; then
        echo "::error::D5 drift: cell $cell took ${secs}s (> ${BUDGET}s ${EVENT} ${PREFIX} budget)"
        FAIL=1
    fi
done
exit "$FAIL"
