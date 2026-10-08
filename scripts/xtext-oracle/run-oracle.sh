#!/usr/bin/env bash
# v3.1 C1 — build (if needed) and run THE UPSTREAM RESOLUTION ORACLE.
#
# Dumps how the RELEASED rune-lang 9.83.0 resolves every cross-reference in the
# given sources. That dump is the ground truth the fork's Layer-1 resolution
# spec is pinned to; regenerating it is how a spec branch gets its citation.
#
# The build output is target/classes plus ~39 MB of COPIED dependency jars in
# target/lib — not an uber-jar; dependencies are copied, not shaded (see the
# pom). Both live in target/ (gitignored). Only the TSV dumps are committed.
#
#   scripts/xtext-oracle/run-oracle.sh --out <file.tsv> <src.rosetta|dir>...
#   scripts/xtext-oracle/run-oracle.sh --rebuild ...        # FULL rebuild: recompile + re-copy deps
#
# ⚠️ A normal run builds ONLY when target/classes or target/lib is missing. If
# you edit the oracle's Java under scripts/xtext-oracle/src/, you MUST pass
# --rebuild or the run silently executes the previously-compiled classes.
#   scripts/xtext-oracle/run-oracle.sh --context <dir> ...  # resolve-only inputs
#   scripts/xtext-oracle/run-oracle.sh --issues <file.tsv> ... # v3.2 seat 8: every validation issue of every
#                                                             # severity with its line / column / offset / length
#                                                             # (a fork diagnostic ANCHOR measured against upstream's)
#
# Builtins default to the in-repo copies, which the #410 oracle work verified
# line-ending-identical to org.finos.rune:rune-runtime:9.83.0's own models.

set -u

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
CLASSES="$HERE/target/classes"
LIB="$HERE/target/lib"

# Git Bash reports MSYS-style paths (/f/...) which the JVM cannot read, and it
# does NOT rewrite them inside a -cp value because the `;` separator stops it
# looking like a path. Convert explicitly where the tool exists; elsewhere the
# paths are already native.
to_native() {
    if command -v cygpath >/dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi
}

# The classpath separator belongs to the JVM's platform, not the shell's. Under
# Git Bash/Cygwin we hand the JVM Windows-style paths (above), so it wants `;`;
# everywhere else `:`. Hardcoding `;` made the whole classpath ONE entry on
# Linux/macOS, and the oracle then failed to boot in a way that reads like a
# missing dependency (Copilot R1, PR #566).
if command -v cygpath >/dev/null 2>&1; then CPSEP=';'; else CPSEP=':'; fi

# `py` is the Windows Python launcher and does not exist elsewhere. Without a
# fallback the signature strip below dies on any non-Windows host — and that
# step is load-bearing: seven separately-signed Eclipse jars share the package
# org.eclipse.core.runtime, so an unstripped copy cannot boot.
if command -v py >/dev/null 2>&1; then PY=(py -3)
elif command -v python3 >/dev/null 2>&1; then PY=(python3)
elif command -v python >/dev/null 2>&1; then PY=(python)
else
    echo "FATAL: no Python found (tried py -3, python3, python) — the oracle" >&2
    echo "       cannot strip jar signatures and therefore cannot boot." >&2
    exit 5
fi
BUILTINS="${ORACLE_BUILTINS:-$REPO/test-corpus/rune-dsl-builtins/rune-runtime/src/main/resources/model}"

# THE POSITIVE CONTROL, ON EVERY RUN. A model with a deliberate type error that
# upstream must reject; if it comes back clean the validator is dead and the
# oracle exits non-zero WITHOUT writing a dump. It is passed by default rather
# than offered as an option, because the failure it guards against — validation
# returning zero issues for every input — looks exactly like success.
CONTROL="${ORACLE_CONTROL:-$HERE/oracle-positive-control.rosetta}"

REBUILD=0
ARGS=()
OUT=""
ISSUES=""
prev=""
for a in "$@"; do
    if [ "$a" = "--rebuild" ]; then
        REBUILD=1
    else
        # Remember --out's value so the header check below can read the dump the
        # driver was told to write - and --issues's (v3.2 seat 8 round 3): the issues
        # file carries its own pinned header and gets the same guard.
        if [ "$prev" = "--out" ]; then OUT="$a"; fi
        if [ "$prev" = "--issues" ]; then ISSUES="$a"; fi
        ARGS+=("$a")
    fi
    prev="$a"
done

if [ ! -f "$CONTROL" ]; then
    echo "FATAL: positive control missing at $CONTROL" >&2
    echo "       The oracle does not run unvalidated (ORACLE_CONTROL overrides)." >&2
    exit 6
fi

if [ ! -d "$BUILTINS" ]; then
    echo "FATAL: builtins not found at $BUILTINS" >&2
    echo "       (override with ORACLE_BUILTINS=<dir>)" >&2
    exit 3
fi

if [ "$REBUILD" = "1" ] || [ ! -d "$LIB" ] || [ ! -d "$CLASSES" ]; then
    # A BUILD MUST ACTUALLY REBUILD. The dependency copy OVERWRITES into
    # target/lib without cleaning it, so without this the run keeps: (a) the
    # `.signatures-stripped` marker, leaving freshly-copied SIGNED jars
    # unstripped while claiming they were done — the exact "done marker written
    # when nothing was done" disease that made this script fail to boot once
    # already; and (b) the previous version of any bumped dependency, so two
    # versions of the same artifact sit on the classpath (Copilot R2, PR #566).
    # BOTH halves of the problem above are handled on EVERY build path, not just
    # --rebuild. The build also fires when target/classes is missing while
    # target/lib survives (a partial clean, an interrupted run) — and on that
    # path Maven re-copies into an uncleaned target/lib just the same, so both
    # (a) and (b) apply exactly as they do to --rebuild.
    #
    # R10 closed (a), the marker, for all three conditions but left (b), the
    # stale versions, gated on --rebuild — fixing one half of a two-part comment
    # while editing that very comment (Copilot R10 then R13, PR #566).
    if [ -d "$LIB" ]; then
        rm -rf "$LIB"
    fi
    echo "building the oracle (offline, ~1 min first time)..." >&2
    # The build-cache extension MUST be off here, for the same reason it is off
    # on every other mvn run in this repo: with target/classes already present it
    # short-circuits the whole build, INCLUDING the dependency copy — so a
    # --rebuild that had just cleaned target/lib left it empty and the oracle
    # could not boot. Silent-skip dressed as a successful build.
    ( cd "$HERE" && mvn -o -q -DskipTests package "-Dmaven.build.cache.enabled=false" ) || {
        echo "FATAL: oracle build failed. The 9.83.0 artifacts must be in ~/.m2." >&2
        exit 4
    }
fi
if [ ! -d "$LIB" ]; then
    echo "FATAL: $LIB missing after build" >&2
    exit 4
fi

# Signature stripping. MUST NOT be a silent no-op: the first version used
# `zip -d ... || true`, `zip` is absent on this box, so nothing was stripped and
# a "done" marker was written anyway — after which the oracle failed to boot in a
# way that looked like a classpath problem.
if [ ! -f "$LIB/.signatures-stripped" ]; then
    "${PY[@]}" "$HERE/strip-jar-signatures.py" "$(to_native "$LIB")" || {
        echo "FATAL: could not strip jar signatures — the oracle cannot boot" >&2
        exit 5
    }
    touch "$LIB/.signatures-stripped"
fi

# The oracle FAILING must never look like the oracle finding nothing — that was
# precisely the superseded in-process harness's defect. Any non-zero exit, or a
# dump whose pinned header is absent, is a hard failure here.
# --builtins converts like every other path handed to the JVM. It worked without
# this: MSYS rewrites a lone POSIX-looking argument on its way to a native
# program, which is why only the `-cp` value (whose `;` separator defeats that
# rewrite) strictly needs the explicit call. But --control right beside it was
# already converted explicitly, so the pair disagreed, and leaning on an
# implicit shell rewrite is a dependency worth not having. to_native is a no-op
# where cygpath is absent (Copilot R11, PR #566).
java -cp "$(to_native "$CLASSES")${CPSEP}$(to_native "$LIB")/*" org.finos.rune.oracle.RosettaResolutionOracle --builtins "$(to_native "$BUILTINS")" --control "$(to_native "$CONTROL")" "${ARGS[@]}"
rc=$?
if [ "$rc" != "0" ]; then
    if [ "$rc" = "6" ]; then
        echo "FATAL: THE POSITIVE CONTROL FAILED — upstream's validator is not answering." >&2
        echo "       No dump was written. Every 'upstream is silent' reading is suspect" >&2
        echo "       until this passes again." >&2
    fi
    echo "FATAL: oracle exited $rc — the dump is NOT valid, do not commit it" >&2
    exit "$rc"
fi

# THE HEADER CHECK, which the comment above has been promising. Exit 0 alone does
# not prove a dump was written: a redirection that silently truncated, a driver
# that returned before writing, or a stale file left from an earlier run all
# present as success to the shell. The pinned header is the one token only a
# completed run emits, so a dump without it is treated exactly like a non-zero
# exit. Until now this was asserted in a comment and enforced nowhere — the
# unwired-gate shape (Copilot R16, PR #566).
if [ -n "$OUT" ]; then
    if ! head -n 1 "$OUT" 2>/dev/null | grep -q '^# rune-xtext-resolution-oracle '; then
        echo "FATAL: $OUT carries no pinned oracle header — the dump is NOT valid," >&2
        echo "       do not commit it. The run exited 0, so this is a WRITE failure," >&2
        echo "       not a resolution failure." >&2
        exit 7
    fi
fi
# The --issues file (v3.2 seat 8 round 3): the same law - a completed run writes a pinned
# header there too, and a stale or truncated file reads as "upstream reports nothing".
if [ -n "$ISSUES" ]; then
    if ! head -n 1 "$ISSUES" 2>/dev/null | grep -q '^# rune-xtext-resolution-oracle .* issues='; then
        echo "FATAL: $ISSUES carries no pinned oracle issues header — the issue dump is NOT valid." >&2
        exit 7
    fi
fi
