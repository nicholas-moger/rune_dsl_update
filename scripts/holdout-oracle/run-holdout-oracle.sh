#!/usr/bin/env bash
# W42 sweep leg B slice 2 — pin hold-out byte-goldens from the RELEASED
# 9.83.0 rosetta-maven-plugin (PR #410).
#
# Per hold-out group under rune-java-generator/src/test/resources/holdout/:
#   1. stage the group's fixture models RAW (no filtering) into a fresh
#      scaffold instantiated from pom-template.xml;
#   2. run the released plugin TWICE from scratch;
#   3. byte-compare the two runs (determinism gate — any diff fails the
#      group; a nondeterministic oracle cannot pin goldens);
#   4. install run 1's output as the group's goldens under
#      rune-java-generator/src/test/resources/holdout-goldens/<group>/.
#
# THE ORACLE LAW: the released JARs in ~/.m2 are the oracle — never the
# vendored rune-dsl/ source (0.0.0.main-SNAPSHOT, post-9.83). See
# pom-template.xml header for the full configuration provenance.
#
# Usage (from the repo root):
#   scripts/holdout-oracle/run-holdout-oracle.sh [group ...]
# With no arguments, all groups are processed. Logs land in
# target/holdout-oracle/work/<group>/run{1,2}.log.

set -u

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
HOLDOUT_DIR="$REPO_ROOT/rune-java-generator/src/test/resources/holdout"
GOLDENS_DIR="$REPO_ROOT/rune-java-generator/src/test/resources/holdout-goldens"
TEMPLATE="$REPO_ROOT/scripts/holdout-oracle/pom-template.xml"
WORK_ROOT="$REPO_ROOT/target/holdout-oracle/work"

FAILED=0
TOTAL_FILES=0
SUMMARY=""

# Formatter-INPUT fixtures with deliberately unresolvable references: the
# released plugin REFUSES them (severe validation error — upstream's mojo
# validation gate), so no golden is definable. The no-args sweep skips them
# (Seat-1 #410 OBS-3: the default invocation should exit 0); name a group
# explicitly to reproduce the refusal on demand.
ORACLE_REJECTED="formatting-nestedConstructor formatting-onlyExists"

if [ "$#" -gt 0 ]; then
    GROUP_LIST=("$@")
else
    GROUP_LIST=()
    for d in "$HOLDOUT_DIR"/*/; do
        g="$(basename "$d")"
        case " $ORACLE_REJECTED " in
            *" $g "*) echo "[skip] $g — oracle-rejected (invalid-reference formatter fixture; name it explicitly to reproduce the refusal)";;
            *) GROUP_LIST+=("$g");;
        esac
    done
fi

run_group() {
    local group="$1"
    local src="$HOLDOUT_DIR/$group"
    local work="$WORK_ROOT/$group"

    # Fail LOUD on names the pom substitution cannot carry safely (Copilot #422
    # R1 class): the @GROUP@/@ROSETTA_CONFIG@ replacements are sed literals
    # inside XML, so `&` (sed's whole-match metachar), the `|` delimiter and
    # XML-significant characters would corrupt the instantiated pom silently.
    # Every repo-authored group name is [A-Za-z0-9-]; anything else is a
    # staging mistake, not a case to accommodate.
    case "$group" in
        (*[!A-Za-z0-9-]*)
            echo "[FAIL] $group — group names must be [A-Za-z0-9-] (the pom substitution contract)" >&2
            return 1;;
    esac

    if [ ! -d "$src" ]; then
        echo "[FAIL] $group — no such hold-out group under $HOLDOUT_DIR" >&2
        return 1
    fi

    rm -rf "$work" || { echo "[FAIL] $group — cannot clear work dir" >&2; return 1; }
    mkdir -p "$work/model" || return 1

    # Stage the fixtures RAW — bytes must reach the oracle exactly as they
    # reach the fork's loader (no Maven filtering, no rewriting).
    cp -r "$src"/. "$work/model/" || { echo "[FAIL] $group — fixture copy failed" >&2; return 1; }

    # Per-group oracle configuration (PR #422): a group MAY carry a
    # rosetta-config.yml (e.g. func-extends-valid enables upstream's
    # experimental `scopes` feature, which `func ... extends` requires).
    # It is oracle CONFIGURATION, not a model source: move it out of the
    # staged model dir to the scaffold root and hand it to the mojo via
    # <rosettaConfig>. The mojo resolves that string with java.nio against
    # the mvn process cwd, so pass an ABSOLUTE path — in native form on
    # Windows (cygpath -m), since no MSYS argument conversion applies to
    # file content. Groups without the file run exactly as before (the
    # placeholder collapses to an empty line). The fork-side loader is
    # untouched by the file: it enumerates *.rosetta only.
    local config_element=""
    if [ -f "$work/model/rosetta-config.yml" ]; then
        mv "$work/model/rosetta-config.yml" "$work/rosetta-config.yml" || return 1
        local config_path="$work/rosetta-config.yml"
        if command -v cygpath >/dev/null 2>&1; then
            config_path="$(cygpath -m "$config_path")" || return 1
        fi
        # Fail LOUD if the checkout path defeats the substitution (Copilot #422
        # R1): `&` is sed's whole-match metachar in the replacement, `|` is the
        # delimiter, and < > " ' would need XML escaping the literal
        # substitution deliberately does not do. A checkout under a plain path
        # is the supported configuration; anything else must abort, never
        # corrupt the pom silently.
        case "$config_path" in
            (*[\&\|\<\>\"\']*)
                echo "[FAIL] $group — the oracle config path contains sed/XML-unsafe characters (one of & | < > \" '):" >&2
                echo "        $config_path" >&2
                echo "        Re-clone the repo under a plain path — the pom substitution cannot carry these safely." >&2
                return 1;;
        esac
        config_element="<rosettaConfig>$config_path</rosettaConfig>"
        echo "[note] $group — oracle config staged ($config_path)"
    fi

    # Instantiate the scaffold pom. @GROUP@ and @ROSETTA_CONFIG@ are fixed
    # placeholders in a template this repo authors — literal substitutions,
    # not structural editing of foreign content. The @ROSETTA_CONFIG@
    # replacement uses `|` as the sed delimiter (the value is a path
    # containing `/`; group names and work paths contain neither `|` nor
    # `&`).
    sed -e "s/@GROUP@/$group/" -e "s|@ROSETTA_CONFIG@|$config_element|" "$TEMPLATE" > "$work/pom.xml" || return 1

    local run out
    for run in 1 2; do
        rm -rf "$work/src"
        echo "[run] $group — oracle run $run (log: $work/run$run.log)"
        if ! mvn -f "$work/pom.xml" generate-sources -B -ntp > "$work/run$run.log" 2>&1; then
            echo "[FAIL] $group — oracle run $run failed; see $work/run$run.log" >&2
            return 1
        fi
        out="$work/src/generated/java"
        if [ ! -d "$out" ]; then
            # A successful mvn run with no output dir is a legitimate
            # ZERO-emission group (alias-only models — the slice-1 census
            # pinned two such groups at 0). Materialise an empty run dir so
            # the determinism diff and the goldens install still operate.
            echo "[note] $group — run $run emitted zero files (no $out)"
            mkdir -p "$work/run$run" || return 1
        else
            mv "$out" "$work/run$run" || return 1
        fi
    done

    # Determinism gate: the two runs must byte-agree (file sets + content).
    if ! diff -r "$work/run1" "$work/run2" > "$work/determinism.diff" 2>&1; then
        echo "[FAIL] $group — the two oracle runs differ; see $work/determinism.diff" >&2
        return 1
    fi

    # Install run 1 as the group's pinned goldens (delete-then-copy — the
    # goldens dir is single-owner, this script).
    rm -rf "$GOLDENS_DIR/$group" || return 1
    mkdir -p "$GOLDENS_DIR/$group" || return 1
    cp -r "$work/run1"/. "$GOLDENS_DIR/$group/" || return 1

    local count
    count=$(find "$GOLDENS_DIR/$group" -type f -name '*.java' | wc -l | tr -d ' ')
    TOTAL_FILES=$((TOTAL_FILES + count))
    SUMMARY="$SUMMARY$group: $count files, deterministic x2\n"
    echo "[ok] $group — $count golden files pinned"
    return 0
}

for group in "${GROUP_LIST[@]}"; do
    if ! run_group "$group"; then
        FAILED=$((FAILED + 1))
    fi
done

echo ""
echo "=== hold-out oracle summary ==="
printf "%b" "$SUMMARY"
echo "total golden files: $TOTAL_FILES; failed groups: $FAILED"
[ "$FAILED" -eq 0 ] || exit 1
