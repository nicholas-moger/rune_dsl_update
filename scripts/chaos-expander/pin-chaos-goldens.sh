#!/usr/bin/env bash
# Pin the chaos cell's byte-goldens from the RELEASED 9.83.0
# rosetta-maven-plugin (v3.2 PR-1; charter § 2.1's golden provenance law).
# A clone of scripts/holdout-oracle/run-holdout-oracle.sh's staging pattern
# with ONE group (the whole chaos corpus) and the § 4b exclusion contract.
#
#   1. stage the expander SINK (target/chaos-expander/work/ by default; pass
#      a dir to read the committed cell instead) RAW into a fresh scaffold -
#      MINUS the golden-free union: the EXPECTED census
#      (expectations/oracle-diagnostics.tsv) - the files the released plugin
#      REFUSES; each exclusion is printed and counted. (v3.2 seat 9, PR #630,
#      F13 / D48: the split-PARTNER closure that used to ride along - "a p1
#      whose p2 is upstream-invalid has no definable golden either" - was a
#      HARNESS INFERENCE the oracle refuted: judged alone, chaos-s17-x36enum-p1
#      has 8 deterministic goldens, byte-identical to the fork's emissions
#      (the hold-out group x36enum-split-p1); only the invalid half is
#      excluded now, and the whole-corpus invocation stays clean because the
#      mojo resolves imports per file - its all-or-nothing gate fires on a
#      link ERROR, not on a dangling wildcard import.)
#   2. run the released plugin TWICE from scratch;
#   3. byte-compare the two runs (determinism gate - a nondeterministic
#      oracle cannot pin goldens);
#   4. install run 1 as the pinned goldens (default
#      target/chaos-expander/work-goldens/; pass a second arg to install
#      into the cell instead - PR-2's freshness re-derivation does).
#
# THE ORACLE LAW: the released JARs in ~/.m2 are the oracle - never the
# vendored rune-dsl/ source. THE SINGLE-WRITER LAW (charter § 2.1): chaos
# goldens change ONLY via this script; a hand-edited golden is a defect.
#
# Usage (from the repo root):
#   scripts/chaos-expander/pin-chaos-goldens.sh [src-dir] [goldens-dir]
#   scripts/chaos-expander/pin-chaos-goldens.sh --judge-alone [src-dir]
#
# THE JUDGED-ALONE MODE (v3.2 seat 10, chaos-1.1.0 - D49; the seat-9 banking
# "every EXPECTED file judged ALONE once, as p1 was"): every file the EXPECTED
# census excludes is staged ALONE into its own scaffold and put to the released
# plugin ONCE; the verdict (ACCEPTED goldens=N | REFUSED <first ERROR line>) is
# written per file to expectations/judged-alone.tsv - the committed receipt
# that a file's golden-free status is the ORACLE's verdict on that file, never
# an inference from its company (the F13 lesson: p1 had 8 goldens all along).
# Pins nothing; run it before the pin run at every re-admission.

set -u

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JUDGE_ALONE=0
if [ "${1:-}" = "--judge-alone" ]; then
    JUDGE_ALONE=1
    shift
fi
LAB="$REPO_ROOT/scripts/chaos-expander"
SRC_DIR="${1:-$REPO_ROOT/target/chaos-expander/work}"
GOLDENS_DIR="${2:-$REPO_ROOT/target/chaos-expander/work-goldens}"
TEMPLATE="$LAB/pom-template.xml"
EXPECTATIONS="$LAB/expectations/oracle-diagnostics.tsv"
WORK="$REPO_ROOT/target/chaos-goldens/work"

if [ ! -d "$SRC_DIR" ]; then
    echo "[FAIL] no source dir at $SRC_DIR (run expand.py first, or point at the cell)" >&2
    exit 1
fi

# THE DELETION GUARD (the cq review's MF-2, shared law with expand.py's
# write_out): this script rm -rf's GOLDENS_DIR, so that path must never be
# the frozen baseline, never test-corpus outside the chaos cell, and must
# either not exist yet, be empty, or hold only .java goldens (the single
# thing this writer writes).
case "$GOLDENS_DIR" in
    (*corpus-baseline-9.83*)
        echo "[FAIL] REFUSED: the goldens dir may never be under corpus-baseline-9.83 ($GOLDENS_DIR)" >&2
        exit 1;;
    (*test-corpus*)
        case "$GOLDENS_DIR" in
            (*test-corpus/chaos/chaos-*) ;;
            (*)
                echo "[FAIL] REFUSED: under test-corpus/ only the chaos cell's golden dir is writable ($GOLDENS_DIR)" >&2
                exit 1;;
        esac;;
esac
if [ -d "$GOLDENS_DIR" ]; then
    strangers=$(find "$GOLDENS_DIR" -type f ! -name '*.java' | head -5)
    if [ -n "$strangers" ]; then
        echo "[FAIL] REFUSED: $GOLDENS_DIR holds non-golden content - this writer clears only dirs it wrote:" >&2
        echo "$strangers" >&2
        exit 1
    fi
fi

if [ ! -f "$EXPECTATIONS" ]; then
    echo "[FAIL] no EXPECTED census at $EXPECTATIONS - without it the golden-free union is undefined (§ 4b)" >&2
    exit 1
fi

rm -rf "$WORK" || exit 1
mkdir -p "$WORK/model" || exit 1

# The golden-free union (charter § 4b): EXPECTED files, then the closure of
# their split-partners (same variant stem: everything before a trailing -pN).
# ORACLE-CLEAN rows ('-' message) are recorded CLEAN verdicts, not
# exclusions - those files stay staged and golden-bearing.
mapfile -t EXPECTED < <(grep -v '^#' "$EXPECTATIONS" | awk -F'\t' 'NF>=3 && $3!="-"{print $1}' | sort -u)
if [ "${#EXPECTED[@]}" -eq 0 ]; then
    echo "[FAIL] the EXPECTED census yielded ZERO exclusion rows - an empty union means the census is" >&2
    echo "       missing or malformed, not that everything is golden-bearing (the cq review's SF-7)" >&2
    exit 1
fi
declare -A EXCLUDE
# EXPECTED files only (v3.2 seat 9, F13 / D48): the split-partner closure that
# used to add every -pN sibling of an excluded part as EXPECTED-PARTNER was an
# inference the released plugin refuted (x36enum-p1 alone: 8 goldens). A
# partner the plugin cannot judge is a NAMED verdict of its own in the census,
# never a closure computed here.
for f in "${EXPECTED[@]}"; do
    EXCLUDE["$f"]="EXPECTED"
done
# THE MOJO-REFUSED VERDICTS (v3.2 seat 10, chaos-1.1.0 - D49; charter § 4b: the two upstream
# oracles CAN disagree): files the validator admits but the released plugin's GENERATOR
# cannot produce goldens for (a generator exception refuses the WHOLE invocation) - a second
# exclusion set, each row a RECORDED verdict in expectations/mojo-refused.tsv, judged alone
# below like every EXPECTED file.
MOJO_REFUSED="$LAB/expectations/mojo-refused.tsv"
if [ -f "$MOJO_REFUSED" ]; then
    mapfile -t MOJO_ROWS < <(grep -v '^#' "$MOJO_REFUSED" | awk -F'\t' 'NF>=3{print $1"\t"$2}')
    for row in ${MOJO_ROWS[@]+"${MOJO_ROWS[@]}"}; do   # round 1 (cq NIT-6): an EMPTY array under set -u (bash < 4.4)
        f="${row%%	*}"
        v="${row#*	}"
        EXCLUDE["$f"]="$v"
        EXPECTED+=("$f")
    done
fi

if [ "$JUDGE_ALONE" = "1" ]; then
    RECEIPT="$LAB/expectations/judged-alone.tsv"
    ALONE="$WORK/alone"
    rm -rf "$ALONE" || exit 1
    mkdir -p "$ALONE" || exit 1
    # JUDGE_ONLY="<file> <file>..." re-judges the named files alone and keeps every
    # other row of the standing receipt (a single re-admitted file need not re-run
    # the whole band); the body is re-sorted by file name after the merge.
    KEEP=""
    if [ -n "${JUDGE_ONLY:-}" ]; then
        KEEP="$WORK/judged-keep.tsv"
        grep -v '^#' "$RECEIPT" > "$KEEP" 2>/dev/null || true
        read -r -a EXPECTED <<< "$JUDGE_ONLY"
    fi
    {
        echo "# Every EXPECTED file of oracle-diagnostics.tsv judged ALONE by the released 9.83.0"
        echo "# rosetta-maven-plugin (v3.2 seat 10, chaos-1.1.0 - D49; the seat-9 exclusion class"
        echo "# retired): ONE mojo invocation per file, staged with nothing else - the verdict is the"
        echo "# oracle's on THAT file. Written by pin-chaos-goldens.sh --judge-alone; never typed."
        echo "# file<TAB>verdict<TAB>detail   (ACCEPTED goldens=N | REFUSED <first ERROR line>)"
    } > "$RECEIPT"
    for f in "${EXPECTED[@]}"; do
        d="$ALONE/${f%.rosetta}"
        mkdir -p "$d/model" || exit 1
        cp "$SRC_DIR/$f" "$d/model/$f" || exit 1
        cp "$TEMPLATE" "$d/pom.xml" || exit 1
        # round 2 (cq SF-9): the mojo prints through the JVM's platform encoding - on Windows the console codepage
        # turned the BOM diagnostic's non-ASCII into `?` in 34 rows of judged-alone.tsv; force UTF-8 on the writer
        if MAVEN_OPTS="${MAVEN_OPTS:-} -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8" mvn -f "$d/pom.xml" generate-sources -B -ntp > "$d/run.log" 2>&1; then
            n=$(find "$d/src/generated/java" -type f -name '*.java' 2>/dev/null | wc -l | tr -d ' ')
            printf '%s\tACCEPTED\tgoldens=%s\n' "$f" "$n" >> "$RECEIPT"
        else
            first=$(grep -m1 '^\[ERROR\] ERROR:' "$d/run.log" | sed -e 's/^\[ERROR\] ERROR://' -e 's/ (file:.*//')
            if [ -n "$first" ]; then
                printf '%s\tREFUSED\t%s\n' "$f" "$first" >> "$RECEIPT"
            else
                # no validator ERROR line: the GENERATOR failed - the § 4b two-oracles class
                crash=$(grep -m1 -E '^(com\.regnosys|java\.lang|Caused by: ).*Exception' "$d/run.log" | cut -c1-200)
                printf '%s\tCRASHED\t%s\n' "$f" "${crash:-mojo failed without an ERROR: or Exception line - see run.log}" >> "$RECEIPT"
            fi
        fi
        echo "[alone] $f -> $(tail -1 "$RECEIPT" | cut -f2,3)"
    done
    if [ -n "$KEEP" ]; then
        for f in "${EXPECTED[@]}"; do
            # round 1 (cq NIT-6) + round 2 (cq NIT-5): fixed-string AND field-anchored ($1 != f); awk exits 0 when nothing
            # matches, so the file is always rewritten and the && mv fires (the grep -v form this replaced exited 1 when it
            # selected NOTHING - every row matched - leaving the && mv unfired; round 4, cq NIT-2)
            # round 3 (cq NIT-3): awk expands escape sequences in -v assignments - the names are the expander's
            # chaos-sNN-*.rosetta, escape-free by construction, so -v f="$f" compares byte-for-byte here
            awk -F'\t' -v f="$f" '$1 != f' "$KEEP" > "$KEEP.tmp" && mv "$KEEP.tmp" "$KEEP"
        done
        cat "$KEEP" >> "$RECEIPT"
        { grep '^#' "$RECEIPT"; grep -v '^#' "$RECEIPT" | sort -t '	' -k1,1; } > "$RECEIPT.tmp" && mv "$RECEIPT.tmp" "$RECEIPT"
    fi
    echo "[ok] judged alone: ${#EXPECTED[@]} EXPECTED files -> $RECEIPT"
    exit 0
fi

staged=0
excluded=0
for src in "$SRC_DIR"/*.rosetta; do
    f="$(basename "$src")"
    if [ -n "${EXCLUDE[$f]:-}" ]; then
        echo "[exclude] $f (${EXCLUDE[$f]})"
        excluded=$((excluded + 1))
        continue
    fi
    cp "$src" "$WORK/model/$f" || exit 1
    staged=$((staged + 1))
done
echo "[stage] $staged files staged, $excluded excluded (the golden-free union)"

cp "$TEMPLATE" "$WORK/pom.xml" || exit 1

for run in 1 2; do
    rm -rf "$WORK/src"
    echo "[run] chaos golden oracle run $run (log: $WORK/run$run.log)"
    if ! mvn -f "$WORK/pom.xml" generate-sources -B -ntp > "$WORK/run$run.log" 2>&1; then
        echo "[FAIL] oracle run $run failed - a mojo refusal on an L1-clean file is a" >&2
        echo "       NAMED golden-free verdict to record (charter § 4b), never a workaround." >&2
        echo "       See $WORK/run$run.log" >&2
        exit 1
    fi
    out="$WORK/src/generated/java"
    if [ ! -d "$out" ]; then
        echo "[note] run $run emitted zero files"
        mkdir -p "$WORK/run$run" || exit 1
    else
        mv "$out" "$WORK/run$run" || exit 1
    fi
done

if ! diff -r "$WORK/run1" "$WORK/run2" > "$WORK/determinism.diff" 2>&1; then
    echo "[FAIL] the two oracle runs differ (see $WORK/determinism.diff) -" >&2
    echo "       ORACLE-NONDETERMINISTIC is a named class (charter § 6); record it." >&2
    exit 1
fi

rm -rf "$GOLDENS_DIR" || exit 1
mkdir -p "$GOLDENS_DIR" || exit 1
cp -r "$WORK/run1"/. "$GOLDENS_DIR/" || exit 1

count=$(find "$GOLDENS_DIR" -type f -name '*.java' | wc -l | tr -d ' ')
if [ "$count" -eq 0 ]; then
    echo "[FAIL] ZERO goldens pinned - an empty golden set is a failed oracle boot or a drifted" >&2
    echo "       output path, never a clean pin (the l1-gate empty-dump law, applied here - MF-3)" >&2
    exit 1
fi
echo "[ok] chaos goldens pinned: $count .java files, deterministic x2, from $staged staged models -> $GOLDENS_DIR"
