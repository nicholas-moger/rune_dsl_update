#!/usr/bin/env bash
# Lint: production Java files (src/main) must not use regex against
# structured language content (rosetta source / generated Java / grammar).
# Per project CLAUDE.md "no regex on structured content" rule.
#
# Allowlist mechanism: a comment containing
#   ci-allowlist: regex-on-structured-content
# exempts a site when it sits on the SAME line, on the PREVIOUS line, or
# anywhere in the comment block (consecutive comment-only lines) that
# IMMEDIATELY precedes the site. The reach ends at the first code line after
# the block — a marker followed by an unrelated code line covers nothing
# further down (the stale-marker fixture pins this).
#
# Comment-only lines are never sites: a line whose first non-blank characters
# are `//` or `/*`, and — INSIDE an open `/* … */` block only — a line whose
# first character is the javadoc-style `*` continuation. So a javadoc sentence
# that MENTIONS `Pattern.compile("…")` is prose, not a call, while a CODE line
# that happens to start with `*` (a multiplication continuation) is still a
# site. The block state is line-classified: it opens on a `/*` line that does
# not close on the same line and closes on any line containing `*/`; an
# unstarred interior line of a block is comment-only by that state.
# String literals are NOT masked — a line that ASSEMBLES the text
# `Pattern.compile(` into generated Java still trips the lint and carries a
# marker saying so (TypeFormatValidatorGenerator); the lint is a literal
# substring scan by design (cheap, no parser, no false negatives on real calls).
# KNOWN RESIDUE (stated, not hidden): a line inside a Java TEXT BLOCK that
# begins with `//` is classified as a comment and skipped — the scan has no
# string state. No production file carries such a line; the self-test's
# negative fixtures pin the `*`-continuation class.
#
# v3.1 close-out (2026-08-28): the seven standing violations — 0 genuine per
# the close census — were dispositioned here: two were lint mechanics (a marker
# three lines up inside its own comment block; a javadoc mention) fixed by the
# block reach + the comment-only skip above; five were real calls on the
# generator's OWN text (a List#replaceAll(UnaryOperator), three private-use-area
# sentinel scans, one string assembly) and now carry markers with their reason.
#
# Usage:
#   no-regex-on-structured-content.sh                # lint repo prod tree
#   no-regex-on-structured-content.sh --root PATH   # lint a specific path

set -euo pipefail

REPO_ROOT=$(git rev-parse --show-toplevel 2>/dev/null || pwd)
ROOT="$REPO_ROOT"
SCAN_DEFAULT_PATHS="rune-parser/src/main/java rune-java-generator/src/main/java rune-python-runtime/src/main/java"

if [[ "${1:-}" == "--root" ]]; then
    if [[ $# -lt 2 || -z "${2:-}" ]]; then
        echo "::error::--root requires a path argument" >&2
        echo "Usage: no-regex-on-structured-content.sh [--root PATH]" >&2
        exit 2
    fi
    shift
    ROOT="$1"
fi

MARKER='ci-allowlist: regex-on-structured-content'

if [[ -f "$ROOT" ]]; then
    FILES=("$ROOT")
elif [[ "$ROOT" != "$REPO_ROOT" ]]; then
    # Custom --root pointing to a directory: scan all .java under it
    mapfile -d $'\0' -t FILES < <(find "$ROOT" -name "*.java" -type f -print0 2>/dev/null)
else
    # Default: scan only fork-owned src/main/java trees
    FILES=()
    for d in $SCAN_DEFAULT_PATHS; do
        [[ -d "$REPO_ROOT/$d" ]] || continue
        while IFS= read -r -d $'\0' f; do
            FILES+=("$f")
        done < <(find "$REPO_ROOT/$d" -name "*.java" -type f -print0)
    done
fi

[[ ${#FILES[@]} -eq 0 ]] && { echo "no-regex-on-structured-content: no files to lint at $ROOT"; exit 0; }

# Use awk index() for literal substring matching (avoids POSIX ERE issues
# with escaped parens). Each of the 4 forbidden tokens checked separately.
# FNR==1 resets the per-file state (portable across mawk + gawk; mawk on
# Ubuntu runners does not support gawk's BEGINFILE).
#   prevMarker  - the marker was on the previous line (code or comment)
#   blockMarker - the marker is somewhere in the comment block being read;
#                 cleared by the first non-comment line, which is the one
#                 line the block can cover
#   inBlock     - inside an open `/* ... */` block (opened by a `/*` line that
#                 did not close; closed by any line containing `*/`) — the
#                 only state in which a leading `*` means "comment"
# Comment-only lines are classified by their first non-blank characters (and
# the block state) and are skipped as sites (`next`) after updating the marker
# state.
VIOLATIONS=$(awk -v m="$MARKER" '
    FNR == 1 { prevMarker = 0; blockMarker = 0; inBlock = 0 }
    {
        currMarker = (index($0, m) > 0)
        line = $0
        sub(/^[[:space:]]+/, "", line)
        head2 = substr(line, 1, 2)
        isComment = 0
        if (head2 == "//") isComment = 1
        else if (head2 == "/*") { isComment = 1; if (index($0, "*/") == 0) inBlock = 1 }
        else if (inBlock) { isComment = 1; if (index($0, "*/") > 0) inBlock = 0 }
        if (isComment) {
            if (currMarker) blockMarker = 1
            prevMarker = currMarker
            next
        }
        hit = 0
        if (index($0, "Pattern.compile") > 0) hit = 1
        else if (index($0, ".matches(") > 0) hit = 1
        else if (index($0, ".replaceAll(") > 0) hit = 1
        else if (index($0, ".matcher(") > 0) hit = 1
        if (hit && !currMarker && !prevMarker && !blockMarker) {
            printf "%s:%d: %s\n", FILENAME, FNR, $0
        }
        prevMarker = currMarker
        blockMarker = 0
    }
' "${FILES[@]}")

if [[ -n "$VIOLATIONS" ]]; then
    echo "::error::no-regex-on-structured-content: unmarked regex sites in production Java"
    echo "$VIOLATIONS"
    count=$(echo "$VIOLATIONS" | wc -l)
    echo "::error::$count violation(s)"
    exit 1
fi

echo "no-regex-on-structured-content: OK (${#FILES[@]} files scanned, 0 violations)"
exit 0
