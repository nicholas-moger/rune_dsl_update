#!/bin/sh
# Create demo/work/venv/ and install the ANTLR Python runtime into it.
#
# The runtime version MUST match the ANTLR tool version used by gen-parser.sh
# exactly (4.13.2). The generated parser calls checkVersion("4.13.2") at
# construction time; a mismatch only warns, but a mismatched serialised ATN is
# a real failure mode, so the pin is exact rather than a range.
#
# antlr4-python3-runtime is pure Python with no transitive dependencies, which
# is the whole dependency footprint of this lane.
#
# Usage: setup-venv.sh [venv-dir] [python]
# Env:   WHEEL_DIR=/path/to/wheels   offline install (pip --no-index --find-links)
#        RECREATE=1                  delete and rebuild the venv
#        UPGRADE_PIP=1               upgrade pip inside the venv first
#        NO_TIKTOKEN=1               skip the pinned tiktoken (ai_context.py needs it)
#        TIKTOKEN_CACHE_DIR=/path    tiktoken's encoding cache; an offline host needs
#                                    o200k_base staged there (README.md step 2)

set -eu

TIKTOKEN_VERSION=0.14.0
ANTLR_RUNTIME_VERSION=4.13.2

HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEMO_DIR=$(dirname -- "$HERE")

VENV_DIR=${1:-"$DEMO_DIR/work/venv"}
PYTHON=${2:-python3}
WHEEL_DIR=${WHEEL_DIR:-}
RECREATE=${RECREATE:-}
UPGRADE_PIP=${UPGRADE_PIP:-}

command -v "$PYTHON" >/dev/null 2>&1 || {
    echo "ERROR: python interpreter not found: $PYTHON" >&2
    exit 1
}

if [ -n "$RECREATE" ] && [ -d "$VENV_DIR" ]; then
    echo "[setup-venv] removing existing venv: $VENV_DIR"
    rm -rf "$VENV_DIR"
fi

# Windows layout (Git Bash) uses Scripts/, POSIX uses bin/.
venv_python() {
    if [ -x "$VENV_DIR/bin/python" ]; then
        echo "$VENV_DIR/bin/python"
    elif [ -x "$VENV_DIR/Scripts/python.exe" ]; then
        echo "$VENV_DIR/Scripts/python.exe"
    else
        echo ''
    fi
}

if [ -z "$(venv_python)" ]; then
    echo "[setup-venv] creating venv at $VENV_DIR"
    "$PYTHON" -m venv "$VENV_DIR"
else
    echo "[setup-venv] reusing existing venv at $VENV_DIR"
fi

VENV_PY=$(venv_python)
if [ -z "$VENV_PY" ]; then
    echo "ERROR: venv python not found under $VENV_DIR" >&2
    exit 1
fi

if [ -n "$UPGRADE_PIP" ]; then
    "$VENV_PY" -m pip install --upgrade pip
fi

PKG="antlr4-python3-runtime==$ANTLR_RUNTIME_VERSION"
echo "[setup-venv] installing $PKG"
if [ -n "$WHEEL_DIR" ]; then
    [ -d "$WHEEL_DIR" ] || { echo "ERROR: WHEEL_DIR not found: $WHEEL_DIR" >&2; exit 1; }
    echo "[setup-venv] offline install from $WHEEL_DIR"
    "$VENV_PY" -m pip install --disable-pip-version-check \
        --no-index --find-links "$WHEEL_DIR" "$PKG"
else
    "$VENV_PY" -m pip install --disable-pip-version-check "$PKG"
fi

# The AI structural-context experiment (ai_context.py) counts exact tokens with
# tiktoken; install it into the same venv unless NO_TIKTOKEN is set (an offline
# install needs the wheels of tiktoken AND its dependencies - regex, requests,
# charset_normalizer, idna, urllib3, certifi - in WHEEL_DIR; `pip download
# tiktoken==0.14.0 -d <dir>` collects them - and the staged encoding cache below).
if [ -z "${NO_TIKTOKEN:-}" ]; then
    TIKTOKEN_PKG="tiktoken==$TIKTOKEN_VERSION"
    echo "[setup-venv] installing $TIKTOKEN_PKG"
    if [ -n "$WHEEL_DIR" ]; then
        "$VENV_PY" -m pip install --disable-pip-version-check \
            --no-index --find-links "$WHEEL_DIR" "$TIKTOKEN_PKG"
    else
        "$VENV_PY" -m pip install --disable-pip-version-check "$TIKTOKEN_PKG"
    fi
fi

# tiktoken downloads the o200k_base encoding on first use and caches it; an
# offline host must be given a staged cache through TIKTOKEN_CACHE_DIR.
if [ -z "${NO_TIKTOKEN:-}" ]; then
    if [ -n "${TIKTOKEN_CACHE_DIR:-}" ]; then
        echo "[setup-venv] tiktoken encoding cache: $TIKTOKEN_CACHE_DIR (TIKTOKEN_CACHE_DIR)"
    elif [ "${TIKTOKEN_CACHE_DIR+set}" = set ]; then
        echo '[setup-venv] tiktoken encoding cache: disabled (TIKTOKEN_CACHE_DIR is empty, so tiktoken always downloads)'
    else
        echo '[setup-venv] tiktoken encoding cache: the default one (TIKTOKEN_CACHE_DIR not set)'
        if [ -n "$WHEEL_DIR" ]; then
            echo '[setup-venv] offline install: o200k_base must already be cached; see README.md step 2'
        fi
    fi
fi

# Verify the runtime imports (and tiktoken with its o200k_base encoding when installed).
"$VENV_PY" - <<'PYCHECK'
import antlr4, sys
from antlr4.Recognizer import Recognizer
print("antlr4 runtime OK, python " + sys.version.split()[0])
try:
    import tiktoken
except ImportError:
    print("tiktoken not installed (NO_TIKTOKEN set): ai_context.py needs it")
else:
    try:
        tiktoken.get_encoding("o200k_base")
    except Exception as exc:
        print("ERROR: tiktoken could not load the o200k_base encoding: " + type(exc).__name__)
        print("It is downloaded on first use; without network, set TIKTOKEN_CACHE_DIR to a cache")
        print("staged on a connected machine (demo/nojvm/README.md, step 2).")
        sys.exit(3)
    print("tiktoken " + tiktoken.__version__ + " OK (o200k_base)")
PYCHECK

"$VENV_PY" -m pip show antlr4-python3-runtime | grep -E '^(Name|Version|Location):' || true

echo ''
echo '[setup-venv] done.'
echo "[setup-venv] venv python: $VENV_PY"
echo '[setup-venv] analytics.py finds this venv automatically; you can also run'
echo "[setup-venv]   $VENV_PY analytics.py --roots ..."
