#!/bin/sh
# Generate the ANTLR4 Python3 parser for the Rune grammar.
#
# Runs the ANTLR4 tool jar over the fork's OWN grammars
# (rune-parser/src/main/antlr4/com/regnosys/rosetta/parser/*.g4) with
# -Dlanguage=Python3, emitting a flat package into demo/work/nojvm-gen/.
#
# This is the ONE step in the no-JVM lane that needs java, and it is a
# build-time step: nothing at query time touches a JVM. Run it once.
#
# The grammars are portable precisely because they carry ZERO embedded target
# actions and ZERO semantic predicates - the same .g4 that drives the Java
# engine drives CPython unmodified.
#
# RosettaParser.g4 declares `options { tokenVocab = RosettaLexer; }`, so the
# lexer must be processed first and RosettaLexer.tokens must be findable. Both
# grammars go in one invocation (the ANTLR tool topologically sorts them by
# tokenVocab) with -lib pointing at the output directory; if that ever stops
# working the script falls back to two ordered invocations.
#
# Usage: gen-parser.sh [antlr-jar] [grammar-dir] [out-dir]

set -eu

HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEMO_DIR=$(dirname -- "$HERE")
REPO_DIR=$(dirname -- "$DEMO_DIR")

ANTLR_JAR=${1:-}
ANTLR_CP=

# Under Git Bash, MSYS2 or Cygwin the `java` on PATH is a Windows program: it
# splits -cp on ';' and cannot open POSIX paths such as /f/... or /c/..., and
# the shell rewrites a path argument only when the whole argument is one path,
# never the entries of a ';'-separated list. So on those shells every path
# handed to java goes through `cygpath -w` first. On Linux, macOS and other
# POSIX systems paths and the ':' separator are passed through unchanged.
# (WSL reports Linux; there a Linux java is needed on PATH, as for any Linux.)
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*)
        WINDOWS_JAVA=1
        command -v cygpath >/dev/null 2>&1 || {
            echo "ERROR: cygpath not found; it converts the paths handed to the Windows java." >&2
            exit 1
        }
        ;;
    *)
        WINDOWS_JAVA=
        ;;
esac

java_path() {
    if [ -n "$WINDOWS_JAVA" ]; then
        cygpath -w -- "$1"
    else
        printf '%s\n' "$1"
    fi
}

# The ANTLR4 TOOL depends on the ANTLR3 runtime and StringTemplate4; a true
# "-complete" jar bundles them, the plain org.antlr:antlr4 tool jar does not.
# Append the Maven-cached companions when present (harmless if already bundled).
build_antlr_cp() {
    local m2="$HOME/.m2/repository" sep=: rel repo
    if [ -n "$WINDOWS_JAVA" ]; then sep=';'; fi
    ANTLR_CP=$(java_path "$ANTLR_JAR")
    for rel in "org/antlr/antlr4-runtime/4.13.2/antlr4-runtime-4.13.2.jar" \
               "org/antlr/antlr-runtime/3.5.3/antlr-runtime-3.5.3.jar" \
               "org/antlr/ST4/4.3.4/ST4-4.3.4.jar" \
               "org/abego/treelayout/org.abego.treelayout.core/1.0.3/org.abego.treelayout.core-1.0.3.jar"; do
        for repo in "$DEMO_DIR/work/.m2" "$m2"; do
            if [ -f "$repo/$rel" ]; then ANTLR_CP="$ANTLR_CP$sep$(java_path "$repo/$rel")"; break; fi
        done
    done
}
GRAMMAR_DIR=${2:-"$REPO_DIR/rune-parser/src/main/antlr4/com/regnosys/rosetta/parser"}
OUT_DIR=${3:-"$DEMO_DIR/work/nojvm-gen"}

# CONTRACTS 7C names the jar antlr-4.13.2-complete.jar; accept the short name too.
if [ -z "$ANTLR_JAR" ]; then
    for cand in "$DEMO_DIR/work/antlr-4.13.2-complete.jar" \
                "$DEMO_DIR/work/antlr-complete.jar"; do
        if [ -f "$cand" ]; then ANTLR_JAR=$cand; break; fi
    done
fi
if [ -z "$ANTLR_JAR" ] || [ ! -f "$ANTLR_JAR" ]; then
    echo "ERROR: ANTLR tool jar not found. Looked for:" >&2
    echo "  $DEMO_DIR/work/antlr-4.13.2-complete.jar" >&2
    echo "  $DEMO_DIR/work/antlr-complete.jar" >&2
    echo "The integrator downloads it during bootstrap (ANTLR 4.13.2)." >&2
    exit 1
fi

command -v java >/dev/null 2>&1 || {
    echo "ERROR: java is not on PATH. Parser generation is the only step that needs it." >&2
    exit 1
}

LEXER_G4="$GRAMMAR_DIR/RosettaLexer.g4"
PARSER_G4="$GRAMMAR_DIR/RosettaParser.g4"
for g in "$LEXER_G4" "$PARSER_G4"; do
    [ -f "$g" ] || { echo "ERROR: grammar not found: $g" >&2; exit 1; }
done

mkdir -p "$OUT_DIR"
OUT_DIR=$(CDPATH= cd -- "$OUT_DIR" && pwd)

# Delete only what this script generates - never the directory itself.
for stale in RosettaLexer.py RosettaLexer.tokens RosettaLexer.interp \
             RosettaParser.py RosettaParser.tokens RosettaParser.interp \
             RosettaParserListener.py RosettaParserVisitor.py; do
    rm -f "$OUT_DIR/$stale"
done

# Absolute grammar paths keep the ANTLR output flat (relative paths with
# directories get mirrored under -o). -Xexact-output-dir enforces it anyway.
LEXER_ABS=$(CDPATH= cd -- "$(dirname -- "$LEXER_G4")" && pwd)/$(basename -- "$LEXER_G4")
PARSER_ABS=$(CDPATH= cd -- "$(dirname -- "$PARSER_G4")" && pwd)/$(basename -- "$PARSER_G4")

# The classpath and the same paths in the form java reads (unchanged on POSIX).
build_antlr_cp
OUT_J=$(java_path "$OUT_DIR")
LEXER_J=$(java_path "$LEXER_ABS")
PARSER_J=$(java_path "$PARSER_ABS")

# Absolute grammar paths already make ANTLR write flat into -o;
# -Xexact-output-dir is belt-and-braces, so the fallback drops it in case an
# ANTLR build ever stops recognising the option. Functions, not a word-split
# string, so a path containing spaces survives.
run_antlr_base() {
    java -cp "$ANTLR_CP" org.antlr.v4.Tool -Dlanguage=Python3 -o "$OUT_J" -lib "$OUT_J" \
         -visitor -listener -long-messages "$@"
}
run_antlr_exact() {
    java -cp "$ANTLR_CP" org.antlr.v4.Tool -Dlanguage=Python3 -o "$OUT_J" -lib "$OUT_J" \
         -visitor -listener -long-messages -Xexact-output-dir "$@"
}

echo '##DEMO## {"ev":"start","id":"nojvm.genparser","detail":"ANTLR 4.13.2 -> Python3"}'
echo "[gen-parser] jar     : $ANTLR_JAR"
echo "[gen-parser] cp      : $ANTLR_CP"
echo "[gen-parser] grammars: $GRAMMAR_DIR"
echo "[gen-parser] out     : $OUT_DIR"

rc=0
run_antlr_exact "$LEXER_J" "$PARSER_J" || rc=$?

check_missing() {
    missing=''
    for f in RosettaLexer.py RosettaParser.py RosettaLexer.tokens RosettaParser.tokens; do
        [ -f "$OUT_DIR/$f" ] || missing="$missing $f"
    done
    echo "$missing"
}

MISSING=$(check_missing)
if [ "$rc" -ne 0 ] || [ -n "$MISSING" ]; then
    echo "[gen-parser] single-invocation run incomplete (exit $rc; missing:$MISSING)."
    echo '[gen-parser] falling back to two ordered invocations (lexer, then parser).'
    run_antlr_base "$LEXER_J"
    run_antlr_base "$PARSER_J"
    MISSING=$(check_missing)
    if [ -n "$MISSING" ]; then
        echo "ERROR: generation incomplete; still missing:$MISSING" >&2
        exit 1
    fi
fi

echo ''
echo '[gen-parser] generated:'
ls -l "$OUT_DIR" | awk '/Rosetta/ { printf "  %-32s %9s bytes\n", $9, $5 }'

echo ''
echo '[gen-parser] done. Next: setup-venv.sh, then analytics.py'
printf '##DEMO## {"ev":"done","metrics":{"outDir":"%s"}}\n' "$OUT_DIR"
