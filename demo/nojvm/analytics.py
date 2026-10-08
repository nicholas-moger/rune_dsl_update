"""
Rune model analytics with NO JVM.

Parses every *.rosetta file under the given roots using the ANTLR4 Python3
target generated from the fork's own grammars, builds the CONTRACTS.md
section 5 symbol INDEX with a parse-tree walk, and answers Q1-Q4.

The point of the lane: Xtext/EMF needs a warmed JVM, a Guice injector and a
global `EcoreUtil.resolveAll` before a single question can be asked. The
fork's grammar is a plain ANTLR4 grammar with ZERO embedded actions and ZERO
semantic predicates, so the same model is parseable, indexable and queryable
from a bare Python interpreter.

Usage (after gen-parser + setup-venv, see README.md):

  py -3 analytics.py --roots "../corpus/builtins;../corpus/cdm-5.38.0" \
      --receipt ../receipts/analytics.nojvm.json

Exit codes: 0 ok, 1 run failure, 2 environment not prepared.
"""

from __future__ import annotations

# Startup measurement must be the very first thing that happens.
import time as _time

_T_PERF0 = _time.perf_counter()   # monotonic origin for every duration below
_T_WALL0 = _time.time()           # wall clock at interpreter-ready

import argparse
import json
import os
import platform
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

from queries import (  # noqa: E402  (path bootstrap must precede the import)
    Attr, Decl, ModelIndex,
    KIND_ANNOTATION, KIND_BASIC_TYPE, KIND_BODY, KIND_CHOICE, KIND_CORPUS,
    KIND_ENUM, KIND_FUNC, KIND_LIBRARY_FUNCTION, KIND_META_TYPE,
    KIND_RECORD_TYPE, KIND_REPORT, KIND_RULE, KIND_RULE_SOURCE, KIND_SEGMENT,
    KIND_SYNONYM_SOURCE, KIND_TYPE, KIND_TYPE_ALIAS,
    aux_counts, walk_stats,
)

# Rune expressions nest arbitrarily deeply (the `expression` rule has ~90
# alternatives and chains freely). The generated parser handles left recursion
# iteratively, but tree traversal below is depth-sensitive, so give CPython
# headroom before touching a CDM/DRR tree.
sys.setrecursionlimit(max(sys.getrecursionlimit(), 10000))

LANE = "plus-nojvm"
DEFAULT_GEN_DIR = _HERE.parent / "work" / "nojvm-gen"
DEFAULT_VENV_DIR = _HERE.parent / "work" / "venv"
ANTLR_RUNTIME_VERSION = "4.13.2"


# ==========================================================================
# CONTRACTS section 3 - runner stdout protocol
# ==========================================================================

def demo(ev: str, **kw: Any) -> None:
    """Emit one `##DEMO## {json}` protocol line. ASCII only (CONTRACTS 0.8)."""
    payload: Dict[str, Any] = {"ev": ev}
    payload.update(kw)
    sys.stdout.write("##DEMO## " + json.dumps(payload, separators=(",", ":"),
                                              ensure_ascii=True) + "\n")
    sys.stdout.flush()


def log(msg: str) -> None:
    sys.stdout.write(msg + "\n")
    sys.stdout.flush()


# ==========================================================================
# Process-level measurements (best effort; omitted silently when unavailable)
# ==========================================================================

def _process_start_epoch() -> Optional[float]:
    """Epoch seconds at which THIS process was created, or None."""
    try:
        if os.name == "nt":
            import ctypes
            import ctypes.wintypes as wt

            class FILETIME(ctypes.Structure):
                _fields_ = [("dwLowDateTime", wt.DWORD),
                            ("dwHighDateTime", wt.DWORD)]

            k32 = ctypes.WinDLL("kernel32", use_last_error=True)
            k32.GetCurrentProcess.restype = wt.HANDLE
            k32.GetProcessTimes.argtypes = [wt.HANDLE, ctypes.POINTER(FILETIME),
                                            ctypes.POINTER(FILETIME),
                                            ctypes.POINTER(FILETIME),
                                            ctypes.POINTER(FILETIME)]
            created, exited, kern, user = (FILETIME(), FILETIME(),
                                           FILETIME(), FILETIME())
            ok = k32.GetProcessTimes(k32.GetCurrentProcess(),
                                     ctypes.byref(created), ctypes.byref(exited),
                                     ctypes.byref(kern), ctypes.byref(user))
            if not ok:
                return None
            ticks = (created.dwHighDateTime << 32) | created.dwLowDateTime
            # FILETIME: 100ns units since 1601-01-01 UTC.
            return ticks / 1e7 - 11644473600.0
        # POSIX: /proc/self/stat field 22 is starttime in clock ticks since boot.
        raw = Path("/proc/self/stat").read_text(encoding="ascii", errors="replace")
        tail = raw[raw.rindex(")") + 1:].split()      # skip comm, which may hold spaces
        start_ticks = int(tail[19])                   # field 22 == tail[19] after (pid, comm, state)
        hz = os.sysconf("SC_CLK_TCK")
        uptime = float(Path("/proc/uptime").read_text().split()[0])
        return (time.time() - uptime) + start_ticks / float(hz)
    except Exception:
        return None


def _peak_rss_mb() -> Optional[float]:
    """Peak resident set size in MiB, or None when not cheaply available."""
    try:
        if os.name == "nt":
            import ctypes
            import ctypes.wintypes as wt

            class PMC(ctypes.Structure):
                _fields_ = [("cb", wt.DWORD), ("PageFaultCount", wt.DWORD),
                            ("PeakWorkingSetSize", ctypes.c_size_t),
                            ("WorkingSetSize", ctypes.c_size_t),
                            ("QuotaPeakPagedPoolUsage", ctypes.c_size_t),
                            ("QuotaPagedPoolUsage", ctypes.c_size_t),
                            ("QuotaPeakNonPagedPoolUsage", ctypes.c_size_t),
                            ("QuotaNonPagedPoolUsage", ctypes.c_size_t),
                            ("PagefileUsage", ctypes.c_size_t),
                            ("PeakPagefileUsage", ctypes.c_size_t)]

            k32 = ctypes.WinDLL("kernel32", use_last_error=True)
            k32.GetCurrentProcess.restype = wt.HANDLE
            fn = getattr(k32, "K32GetProcessMemoryInfo", None)
            if fn is None:
                fn = ctypes.WinDLL("psapi", use_last_error=True).GetProcessMemoryInfo
            counters = PMC()
            counters.cb = ctypes.sizeof(PMC)
            if not fn(k32.GetCurrentProcess(), ctypes.byref(counters),
                      ctypes.sizeof(PMC)):
                return None
            return round(counters.PeakWorkingSetSize / (1024.0 * 1024.0), 1)
        import resource
        peak = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
        # Linux reports KiB; macOS reports bytes.
        scale = 1024.0 if sys.platform == "darwin" else 1.0
        return round(peak * scale / (1024.0 * 1024.0), 1)
    except Exception:
        return None


def _ms(t0: float, t1: Optional[float] = None) -> int:
    return int(round(((_time.perf_counter() if t1 is None else t1) - t0) * 1000.0))


# ==========================================================================
# Environment bootstrap - generated parser + antlr4 runtime
# ==========================================================================

def _venv_site_packages(venv: Path) -> List[Path]:
    hits: List[Path] = []
    if not venv.is_dir():
        return hits
    win = venv / "Lib" / "site-packages"
    if win.is_dir():
        hits.append(win)
    lib = venv / "lib"
    if lib.is_dir():
        for child in sorted(lib.iterdir()):
            sp = child / "site-packages"
            if sp.is_dir():
                hits.append(sp)
    return hits


def prepare_environment(gen_dir: Path, venv_dir: Path,
                        venv_fallback: bool = True) -> Tuple[Any, Any, Any]:
    """Put the generated parser + antlr4 runtime on sys.path and import them.

    Returns (RosettaLexer, RosettaParser, antlr module namespace).
    Raises SystemExit(2) with an actionable message when unprepared.
    """
    if not gen_dir.is_dir():
        demo("error", message=f"generated parser directory not found: {gen_dir}")
        log("")
        log("The ANTLR Python3 sources have not been generated yet. Run (needs java,")
        log("integrator step): demo/nojvm/gen-parser.ps1   (or gen-parser.sh)")
        raise SystemExit(2)
    missing = [n for n in ("RosettaLexer.py", "RosettaParser.py")
               if not (gen_dir / n).is_file()]
    if missing:
        demo("error", message=f"incomplete generated parser in {gen_dir}: "
                              f"missing {', '.join(missing)}")
        log("Re-run demo/nojvm/gen-parser.ps1 (or gen-parser.sh).")
        raise SystemExit(2)

    if str(gen_dir) not in sys.path:
        sys.path.insert(0, str(gen_dir))

    try:
        import antlr4  # noqa: F401
    except ImportError:
        added = False
        if venv_fallback:
            for sp in _venv_site_packages(venv_dir):
                if str(sp) not in sys.path:
                    sys.path.insert(0, str(sp))
                    added = True
        if not added:
            _antlr_missing(venv_dir)
        try:
            import antlr4  # noqa: F401
        except ImportError:
            _antlr_missing(venv_dir)

    import antlr4
    from RosettaLexer import RosettaLexer          # type: ignore
    from RosettaParser import RosettaParser        # type: ignore
    return RosettaLexer, RosettaParser, antlr4


def _antlr_missing(venv_dir: Path) -> None:
    demo("error", message="antlr4-python3-runtime is not importable")
    log("")
    log(f"Install it with: demo/nojvm/setup-venv.ps1   (creates {venv_dir})")
    log(f"or manually:     py -3 -m pip install antlr4-python3-runtime=="
        f"{ANTLR_RUNTIME_VERSION}")
    raise SystemExit(2)


class _CountingErrorListener:
    """Silent syntax-error counter (replaces ANTLR's console listener)."""

    def __init__(self) -> None:
        self.count = 0
        self.first: Optional[str] = None

    def syntaxError(self, recognizer, offendingSymbol, line, column, msg, e):
        self.count += 1
        if self.first is None:
            self.first = f"line {line}:{column} {msg}"

    def reportAmbiguity(self, *a):            # pragma: no cover - diagnostics only
        pass

    def reportAttemptingFullContext(self, *a):   # pragma: no cover
        pass

    def reportContextSensitivity(self, *a):      # pragma: no cover
        pass


# ==========================================================================
# Parse-tree extraction - the CONTRACTS section 5 INDEX
# ==========================================================================
#
# Grammar bindings (rune-parser/src/main/antlr4/.../RosettaParser.g4):
#
#   namespace        rosettaModel      NAMESPACE (qualifiedName | STRING)
#   data type        dataType          TYPE validID (EXTENDS qualifiedName)? ...
#   choice           choice            CHOICE validID ...
#   enum             enumeration       ENUM validID (EXTENDS qualifiedName)? ...
#   function         function          FUNC validID ... (EXTENDS qualifiedName)? ...
#   reporting rule   rosettaRule       (REPORTING|ELIGIBILITY) RULE validID ...
#   attribute        attribute         OVERRIDE? validID typeCall card? ... annotationRef*
#   annotation use   annotationRef     LBRACK validID (validID annotationQualifier*)? RBRACK
#   type reference   typeCall          qualifiedName (LPAREN ... RPAREN)?
#
# The walk inspects DIRECT children only, by context class name, so that an
# `annotationRef` nested in a `condition` body, or an `attribute` belonging to
# a `function` rather than a `dataType`, can never be mistaken for a
# declaration-level one. Unknown subtrees are counted and skipped, never fatal.

CTX_VALID_ID = "ValidIDContext"
CTX_QNAME = "QualifiedNameContext"
CTX_TYPE_CALL = "TypeCallContext"
CTX_ATTRIBUTE = "AttributeContext"
CTX_ANNOTATION_REF = "AnnotationRefContext"
CTX_ROOT_ELEMENT = "RootElementContext"

# rootElement child context class -> declaration kind
ROOT_KINDS: Dict[str, str] = {
    "DataTypeContext": KIND_TYPE,
    "ChoiceContext": KIND_CHOICE,
    "EnumerationContext": KIND_ENUM,
    "FunctionContext": KIND_FUNC,
    "RosettaRuleContext": KIND_RULE,
    "RosettaTypeAliasContext": KIND_TYPE_ALIAS,
    "AnnotationDeclContext": KIND_ANNOTATION,
    "RosettaBasicTypeContext": KIND_BASIC_TYPE,
    "RosettaRecordTypeContext": KIND_RECORD_TYPE,
    "RosettaMetaTypeContext": KIND_META_TYPE,
    "RosettaLibraryFunctionContext": KIND_LIBRARY_FUNCTION,
    "RosettaSynonymSourceContext": KIND_SYNONYM_SOURCE,
    "RosettaExternalSynonymSourceContext": KIND_SYNONYM_SOURCE,
    "RosettaExternalRuleSourceContext": KIND_RULE_SOURCE,
    "RosettaReportContext": KIND_REPORT,
    "RosettaBodyContext": KIND_BODY,
    "RosettaCorpusContext": KIND_CORPUS,
    "RosettaSegmentContext": KIND_SEGMENT,
}

# Declaration kinds whose grammar rule carries an `EXTENDS qualifiedName` that
# denotes a supertype (and where that qualifiedName is a DIRECT child).
EXTENDS_KINDS = {KIND_TYPE, KIND_ENUM, KIND_FUNC}

# Declaration kinds whose direct `attribute` children belong to the declaration.
ATTRIBUTE_KINDS = {KIND_TYPE, KIND_FUNC, KIND_ANNOTATION}


class Extractor:
    """Walks parse trees and fills a ModelIndex. Tolerant by construction."""

    def __init__(self, parser_cls: Any, index: ModelIndex) -> None:
        self.P = parser_cls
        self.index = index
        self.tok_extends = getattr(parser_cls, "EXTENDS", None)
        self.tok_string = getattr(parser_cls, "STRING", None)
        self.tok_reporting = getattr(parser_cls, "REPORTING", None)
        self.tok_eligibility = getattr(parser_cls, "ELIGIBILITY", None)
        self.reporting_rules = 0
        self.eligibility_rules = 0

    # -- primitives ---------------------------------------------------------

    def _kids(self, ctx: Any) -> Tuple[Any, ...]:
        kids = getattr(ctx, "children", None) or ()
        self.index.nodes_visited += len(kids)
        return tuple(kids)

    @staticmethod
    def _cname(node: Any) -> str:
        return node.__class__.__name__

    def _first(self, ctx: Any, cls_name: str) -> Optional[Any]:
        for c in self._kids(ctx):
            if self._cname(c) == cls_name:
                return c
        return None

    def _all(self, ctx: Any, cls_name: str) -> List[Any]:
        return [c for c in self._kids(ctx) if self._cname(c) == cls_name]

    @staticmethod
    def _ident(ctx: Any) -> str:
        """Identifier text with the keyword-escape prefix removed (`^type` -> `type`)."""
        txt = ctx.getText()
        return txt[1:] if txt.startswith("^") else txt

    def _qname_parts(self, ctx: Any) -> List[str]:
        """Segments of a `qualifiedName`, read from its ValidIDContext children.

        Read off the tree rather than by splitting getText(), so an escaped
        segment (`a.^type.C`) yields the real identifiers.
        """
        return [self._ident(c) for c in self._kids(ctx)
                if self._cname(c) == CTX_VALID_ID]

    def _token_type(self, node: Any) -> Optional[int]:
        sym = getattr(node, "symbol", None)
        return None if sym is None else sym.type

    # -- annotationRef ------------------------------------------------------

    def _annotation_ref(self, ctx: Any) -> Tuple[str, str]:
        """`[metadata scheme]` -> ("metadata", "scheme"); `[calculation]` -> ("calculation", "")."""
        ids = [self._ident(c) for c in self._kids(ctx)
               if self._cname(c) == CTX_VALID_ID]
        head = ids[0] if ids else ""
        qual = ids[1] if len(ids) > 1 else ""
        return head, qual

    def _annotations_of(self, ctx: Any) -> Tuple[Tuple[str, ...], Tuple[str, ...]]:
        heads: List[str] = []
        quals: List[str] = []
        for ref in self._all(ctx, CTX_ANNOTATION_REF):
            head, qual = self._annotation_ref(ref)
            if head:
                heads.append(head)
            if qual:
                quals.append(qual)
        return tuple(heads), tuple(quals)

    # -- attribute ----------------------------------------------------------

    def _attribute(self, ctx: Any) -> Attr:
        name_ctx = self._first(ctx, CTX_VALID_ID)
        name = self._ident(name_ctx) if name_ctx is not None else ""
        qname, simple = "", ""
        tc = self._first(ctx, CTX_TYPE_CALL)
        if tc is not None:
            qn = self._first(tc, CTX_QNAME)
            if qn is not None:
                parts = self._qname_parts(qn)
                if parts:
                    qname = ".".join(parts)
                    simple = parts[-1]
        heads, quals = self._annotations_of(ctx)
        return Attr(name=name, type_qname=qname, type_simple=simple,
                    annotations=heads, annotation_quals=quals)

    # -- extends ------------------------------------------------------------

    def _extends(self, ctx: Any) -> Tuple[Optional[str], Optional[str]]:
        """The `EXTENDS qualifiedName` target, located by TOKEN TYPE, not by text."""
        seen_extends = False
        for c in self._kids(ctx):
            if self._token_type(c) == self.tok_extends and self.tok_extends is not None:
                seen_extends = True
                continue
            if seen_extends and self._cname(c) == CTX_QNAME:
                parts = self._qname_parts(c)
                if parts:
                    return ".".join(parts), parts[-1]
                return None, None
        return None, None

    # -- rosettaModel -------------------------------------------------------

    def _namespace(self, model_ctx: Any) -> str:
        qn = self._first(model_ctx, CTX_QNAME)
        if qn is not None:
            return ".".join(self._qname_parts(qn))
        # `namespace "quoted.name"` - strip the STRING token's own delimiters.
        for c in self._kids(model_ctx):
            if self._token_type(c) == self.tok_string and self.tok_string is not None:
                txt = c.getText()
                return txt[1:-1] if len(txt) >= 2 else txt
        return ""

    def extract(self, model_ctx: Any, file_label: str) -> int:
        """Index one file's parse tree. Returns the declaration count."""
        namespace = self._namespace(model_ctx)
        added = 0
        for root in self._all(model_ctx, CTX_ROOT_ELEMENT):
            for child in self._kids(root):
                kind = ROOT_KINDS.get(self._cname(child))
                if kind is None:
                    self.index.unknown_elements += 1
                    continue
                try:
                    self.index.add(self._declaration(child, kind, namespace,
                                                     file_label))
                    added += 1
                except Exception:
                    # Tolerance rule: a malformed subtree costs one declaration,
                    # never the run.
                    self.index.unknown_elements += 1
        return added

    def _declaration(self, ctx: Any, kind: str, namespace: str,
                     file_label: str) -> Decl:
        name = ""
        name_ctx = self._first(ctx, CTX_VALID_ID)
        if name_ctx is not None:
            name = self._ident(name_ctx)
        elif kind == KIND_SEGMENT:
            # `segment rationale` - the name is a keyword token, not a validID.
            kids = self._kids(ctx)
            if len(kids) > 1:
                name = kids[1].getText()

        ext_q, ext_s = (self._extends(ctx) if kind in EXTENDS_KINDS else (None, None))
        heads, quals = self._annotations_of(ctx)
        attrs = ([self._attribute(a) for a in self._all(ctx, CTX_ATTRIBUTE)]
                 if kind in ATTRIBUTE_KINDS else [])

        eligibility = False
        if kind == KIND_RULE:
            eligibility = self._tally_rule(ctx)

        start = getattr(ctx, "start", None)
        return Decl(name=name, kind=kind, namespace=namespace, file=file_label,
                    extends_qname=ext_q, extends_simple=ext_s,
                    annotations=heads, annotation_quals=quals,
                    attributes=attrs,
                    line=getattr(start, "line", 0) if start is not None else 0,
                    eligibility=eligibility)

    def _tally_rule(self, ctx: Any) -> bool:
        """Split `rosettaRule` into reporting vs eligibility by leading token type.

        Returns True for an eligibility rule, so the declaration itself carries the
        split - CONTRACTS section 5's Q1 counts REPORTING rules only, matching the
        JVM lanes' Queries.java.
        """
        for c in self._kids(ctx):
            tt = self._token_type(c)
            if tt is None:
                continue
            if tt == self.tok_reporting:
                self.reporting_rules += 1
                return False
            if tt == self.tok_eligibility:
                self.eligibility_rules += 1
                return True
        return False


def count_tree_nodes(root: Any) -> int:
    """Total nodes in a parse tree, counted iteratively.

    Iterative on purpose: a recursive count over a deeply chained CDM/DRR
    expression can exhaust the interpreter stack, and the whole point of this
    lane is that nothing here falls over without a JVM to lean on.
    """
    total = 0
    stack: List[Any] = [root]
    while stack:
        node = stack.pop()
        total += 1
        kids = getattr(node, "children", None)
        if kids:
            stack.extend(kids)
    return total


# ==========================================================================
# Parsing
# ==========================================================================

class ParseDriver:
    """Two-stage (SLL then LL) ANTLR parsing with a shared DFA cache.

    Stage one runs the fast SLL prediction mode with the bail error strategy.
    ANTLR's guarantee is that SLL either produces the same parse as LL or
    reports an error, so a bail is re-parsed under full LL. Both stages share
    the class-level `decisionsToDFA` cache, which is what makes file number
    350 far cheaper than file number 1.

    `--ll-only` forces stage two for every file; running both ways must give
    identical results, which is the positive control on the optimisation.
    """

    def __init__(self, lexer_cls: Any, parser_cls: Any, antlr: Any,
                 ll_only: bool = False) -> None:
        self.lexer_cls = lexer_cls
        self.parser_cls = parser_cls
        self.antlr = antlr
        self.ll_only = ll_only
        pm = getattr(antlr, "PredictionMode", None)
        if pm is None:                                # older runtime layouts
            from antlr4.atn.PredictionMode import PredictionMode as pm  # type: ignore
        self.PredictionMode = pm
        from antlr4.error.ErrorStrategy import (BailErrorStrategy,
                                                DefaultErrorStrategy)
        self.BailErrorStrategy = BailErrorStrategy
        self.DefaultErrorStrategy = DefaultErrorStrategy
        self.InputStream = getattr(antlr, "InputStream", None)
        if self.InputStream is None:
            from antlr4.InputStream import InputStream as _is  # type: ignore
            self.InputStream = _is
        self.CommonTokenStream = getattr(antlr, "CommonTokenStream", None)
        if self.CommonTokenStream is None:
            from antlr4.CommonTokenStream import CommonTokenStream as _cts  # type: ignore
            self.CommonTokenStream = _cts
        self.sll_bailouts = 0

    def parse(self, text: str, listener: _CountingErrorListener) -> Any:
        lexer = self.lexer_cls(self.InputStream(text))
        lexer.removeErrorListeners()
        lexer.addErrorListener(listener)
        tokens = self.CommonTokenStream(lexer)
        parser = self.parser_cls(tokens)
        parser.removeErrorListeners()

        if not self.ll_only:
            parser._interp.predictionMode = self.PredictionMode.SLL
            parser._errHandler = self.BailErrorStrategy()
            try:
                return parser.rosettaModel()
            except Exception:
                self.sll_bailouts += 1
                parser.reset()

        parser.addErrorListener(listener)
        parser._errHandler = self.DefaultErrorStrategy()
        parser._interp.predictionMode = self.PredictionMode.LL
        return parser.rosettaModel()


def discover(roots: List[Path]) -> List[Path]:
    """All *.rosetta under the roots, de-duplicated, in stable order."""
    seen: Dict[str, Path] = {}
    for root in roots:
        if not root.is_dir():
            log(f"WARN root not found, skipped: {root}")
            continue
        for f in sorted(root.rglob("*.rosetta")):
            key = str(f.resolve()).lower() if os.name == "nt" else str(f.resolve())
            seen.setdefault(key, f)
    return sorted(seen.values(), key=lambda p: str(p).replace("\\", "/"))


def label_of(path: Path, roots: List[Path]) -> str:
    """A short, OS-neutral, root-relative label for the index."""
    for root in roots:
        try:
            return f"{root.name}/{path.relative_to(root).as_posix()}"
        except ValueError:
            continue
    return path.name


# ==========================================================================
# Main
# ==========================================================================

def parse_args(argv: List[str]) -> argparse.Namespace:
    ap = argparse.ArgumentParser(
        prog="analytics.py",
        description="Rune model analytics with no JVM (ANTLR4 Python3 target).")
    ap.add_argument("--roots", required=True,
                    help="';'-separated corpus roots (builtins MUST be one of them)")
    ap.add_argument("--limit", type=int, default=0,
                    help="parse only the first N files (development aid; 0 = all)")
    ap.add_argument("--receipt", default=None,
                    help="write a CONTRACTS section 2 receipt JSON to this path")
    ap.add_argument("--target-type", default="Party",
                    help="Q3 target type simple name (default: Party)")
    ap.add_argument("--gen-dir", default=str(DEFAULT_GEN_DIR),
                    help=f"generated ANTLR Python3 sources (default: {DEFAULT_GEN_DIR})")
    ap.add_argument("--venv", default=str(DEFAULT_VENV_DIR),
                    help="venv to borrow antlr4-python3-runtime from when it is "
                         "not already importable")
    ap.add_argument("--no-venv-fallback", action="store_true",
                    help="do not add the venv's site-packages to sys.path")
    ap.add_argument("--ll-only", action="store_true",
                    help="disable SLL stage one (positive control: results must "
                         "be identical, wall time longer)")
    ap.add_argument("--no-count-nodes", action="store_true",
                    help="skip the full parse-tree node census (reported as "
                         "aux.treeNodes; costs a few percent of wall time)")
    ap.add_argument("--id", default="analytics.nojvm", help="receipt id")
    ap.add_argument("--command", default=None,
                    help="exact command line to record in the receipt "
                         "(must equal the demo/runs.json entry)")
    ap.add_argument("--dump-index", default=None,
                    help="write the symbol index as TSV (name/kind/namespace/file) "
                         "for cross-lane reconciliation")
    ap.add_argument("--progress-every", type=int, default=0,
                    help="progress event interval in files (0 = auto)")
    return ap.parse_args(argv)


def split_roots(spec: str) -> List[Path]:
    return [Path(p.strip()).expanduser() for p in spec.split(";") if p.strip()]


def main(argv: List[str]) -> int:
    args = parse_args(argv)
    started_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    proc_start = _process_start_epoch()
    startup_ms: Optional[int] = None
    if proc_start is not None:
        candidate = int(round((_T_WALL0 - proc_start) * 1000.0))
        if 0 <= candidate < 600000:          # reject a clock-skew reading
            startup_ms = candidate

    roots = split_roots(args.roots)
    gen_dir = Path(args.gen_dir).expanduser().resolve()
    venv_dir = Path(args.venv).expanduser()

    demo("start", id=args.id, detail=f"lane={LANE} roots={len(roots)} "
                                     f"gen={gen_dir.name}")

    t_import = _time.perf_counter()
    lexer_cls, parser_cls, antlr = prepare_environment(
        gen_dir, venv_dir, venv_fallback=not args.no_venv_fallback)
    import_ms = _ms(t_import)
    parser_ready_ms = _ms(_T_PERF0)
    demo("metric", key="parserReadyMs", value=parser_ready_ms)

    files = discover(roots)
    if args.limit and args.limit > 0:
        files = files[:args.limit]
    total = len(files)
    if total == 0:
        demo("error", message=f"no *.rosetta files found under: {args.roots}")
        return 1
    log(f"[nojvm] {total} source files; antlr4 runtime + generated parser ready "
        f"in {parser_ready_ms} ms (import {import_ms} ms)")

    interval = args.progress_every if args.progress_every > 0 else max(25, total // 250)

    index = ModelIndex()
    driver = ParseDriver(lexer_cls, parser_cls, antlr, ll_only=args.ll_only)
    extractor = Extractor(parser_cls, index)

    parse_ms = 0.0
    index_ms = 0.0
    walk_ms = 0.0
    tree_nodes = 0
    count_nodes = not args.no_count_nodes
    first_file_ms: Optional[int] = None
    hard_failures = 0

    for n, path in enumerate(files, start=1):
        listener = _CountingErrorListener()
        label = label_of(path, roots)
        try:
            text = path.read_text(encoding="utf-8-sig", errors="replace")
        except OSError as exc:
            log(f"WARN unreadable {label}: {exc}")
            index.parse_errors += 1
            index.error_files.append(label)
            continue
        index.bytes_read += len(text)

        t0 = _time.perf_counter()
        try:
            tree = driver.parse(text, listener)
            hard_failures = 0
        except Exception as exc:                     # never fatal
            parse_ms += (_time.perf_counter() - t0) * 1000.0
            log(f"WARN parse failed {label}: {type(exc).__name__}: {exc}")
            index.parse_errors += 1
            index.error_files.append(label)
            hard_failures += 1
            # A run-time API mismatch fails EVERY file identically. Abort rather
            # than grinding out 351 identical errors and a zeroed result set.
            if hard_failures >= 5:
                demo("error", message=f"aborting after {hard_failures} consecutive "
                                      f"hard parse failures; last: "
                                      f"{type(exc).__name__}: {exc}")
                log("Hint: check that the generated parser and the installed "
                    "antlr4-python3-runtime are BOTH 4.13.2 (setup-venv + gen-parser).")
                return 1
            continue
        t1 = _time.perf_counter()
        parse_ms += (t1 - t0) * 1000.0
        if first_file_ms is None:
            first_file_ms = _ms(_T_PERF0)
            demo("metric", key="firstFileMs", value=first_file_ms)

        if listener.count:
            index.parse_errors += listener.count
            index.error_files.append(f"{label} ({listener.count}: {listener.first})")
            log(f"WARN {listener.count} syntax error(s) in {label}: {listener.first}")

        # Index this file, then drop the tree: the phase totals stay honest and
        # peak RSS stays bounded (351 retained trees is multiple GB in Python).
        t2 = _time.perf_counter()
        try:
            extractor.extract(tree, label)
        except Exception as exc:                     # never fatal
            log(f"WARN index failed {label}: {type(exc).__name__}: {exc}")
            index.unknown_elements += 1
        index_ms += (_time.perf_counter() - t2) * 1000.0

        if count_nodes:
            t3 = _time.perf_counter()
            tree_nodes += count_tree_nodes(tree)
            walk_ms += (_time.perf_counter() - t3) * 1000.0

        index.files += 1
        del tree

        if n % interval == 0 or n == total:
            demo("progress", n=n, of=total)

    index.sll_bailouts = driver.sll_bailouts

    # -- queries ------------------------------------------------------------
    from queries import (q1_counts_by_kind, q2_metadata_annotated_types,
                         q3_references_to_target, q4_deepest_extends_chain)

    t = _time.perf_counter(); q1 = q1_counts_by_kind(index); q1_ms = _ms(t)
    t = _time.perf_counter(); q2 = q2_metadata_annotated_types(index); q2_ms = _ms(t)
    t = _time.perf_counter()
    q3 = q3_references_to_target(index, args.target_type)
    q3_ms = _ms(t)
    t = _time.perf_counter(); q4 = q4_deepest_extends_chain(index); q4_ms = _ms(t)

    results = {"q1": q1, "q2": q2, "q3": q3, "q4": q4}
    aux = aux_counts(index, args.target_type)
    aux.update({
        "parseErrors": index.parse_errors,
        "errorFiles": index.error_files[:20],
        "sllBailouts": index.sll_bailouts,
        "llOnly": bool(args.ll_only),
        "bytesParsed": index.bytes_read,
        "filesIndexed": index.files,
        "reportingRules": extractor.reporting_rules,
        "eligibilityRules": extractor.eligibility_rules,
        "parserReadyMs": parser_ready_ms,
        "antlrImportMs": import_ms,
        "python": platform.python_version(),
        "predictionMode": "LL" if args.ll_only else "SLL-then-LL",
    })
    if count_nodes:
        aux["treeNodes"] = tree_nodes
        aux["treeWalkMs"] = int(round(walk_ms))

    # CONTRACTS section 4: `files` is the NON-builtin population (the JVM lanes
    # report 349); the raw parsed total (incl. the two builtins) stays in
    # aux.filesIndexed. wallMs = process start -> now when the process-start
    # clock was readable, else interpreter start -> now (the difference is
    # startupMs, which is then also absent - consistent, never guessed).
    builtin_files = len({d.file for d in index.decls if d.builtin})
    now_epoch = time.time()
    if proc_start is not None and startup_ms is not None:
        wall_ms = int(round((now_epoch - proc_start) * 1000.0))
    else:
        wall_ms = int(round((now_epoch - _T_WALL0) * 1000.0))
    metrics: Dict[str, Any] = {
        "lane": LANE,
        "files": index.files - builtin_files,
        "parseMs": int(round(parse_ms)),
        "indexMs": int(round(index_ms)),
        "q1Ms": q1_ms, "q2Ms": q2_ms, "q3Ms": q3_ms, "q4Ms": q4_ms,
        "wallMs": wall_ms,
        "results": results,
    }
    if startup_ms is not None:
        metrics["startupMs"] = startup_ms
    peak = _peak_rss_mb()
    if peak is not None:
        metrics["peakRssMb"] = peak
    metrics["aux"] = aux

    duration_ms = _ms(_T_PERF0)

    # -- human summary ------------------------------------------------------
    log("")
    log("=== no-JVM model analytics =========================================")
    log(f"  files parsed      : {index.files} ({index.bytes_read // 1024} KiB)")
    log(f"  parse             : {metrics['parseMs']} ms"
        f"   index: {metrics['indexMs']} ms"
        f"   nodes inspected: {walk_stats(index)['nodesVisited']}")
    if count_nodes:
        log(f"  parse-tree census : {tree_nodes} nodes "
            f"({int(round(walk_ms))} ms, not counted in parse/index)")
    log(f"  Q1 by kind        : types={q1['types']} enums={q1['enums']} "
        f"functions={q1['functions']} rules={q1['rules']}   ({q1_ms} ms)")
    log(f"  Q2 [metadata]     : {q2['metaAnnotatedTypes']} data types   ({q2_ms} ms)")
    log(f"  Q3 -> {args.target_type:<12}: {q3['referencesToTarget']} data types "
        f"  ({q3_ms} ms)")
    log(f"  Q4 extends depth  : {q4['deepestExtendsChain']} edges   ({q4_ms} ms)")
    chain = aux.get("deepestExtendsChainPath") or []
    if chain:
        log(f"     deepest chain  : {' -> '.join(chain)}")
    log(f"  symbols           : {len(index.symbols)} "
        f"(collisions {index.name_collisions})")
    log(f"  parse errors      : {index.parse_errors}   "
        f"unknown elements: {index.unknown_elements}   "
        f"SLL bailouts: {index.sll_bailouts}")
    if startup_ms is not None:
        log(f"  interpreter start : {startup_ms} ms (process start -> ready)")
    log(f"  parser ready      : {parser_ready_ms} ms (process start -> ATN loaded)")
    if peak is not None:
        log(f"  peak RSS          : {peak} MiB")
    log(f"  total wall        : {duration_ms} ms")
    log("====================================================================")

    if args.dump_index:
        dump = Path(args.dump_index)
        dump.parent.mkdir(parents=True, exist_ok=True)
        with dump.open("w", encoding="utf-8", newline="\n") as fh:
            fh.write("name\tkind\tnamespace\tfile\n")
            for nm in sorted(index.symbols):
                kind, ns, src = index.symbols[nm]
                fh.write(f"{nm}\t{kind}\t{ns}\t{src}\n")
        log(f"[nojvm] symbol index written to {dump}")

    if args.receipt:
        write_receipt(Path(args.receipt), args, started_at, duration_ms, metrics)
        log(f"[nojvm] receipt written to {args.receipt}")

    demo("done", metrics=metrics)
    return 0


def write_receipt(path: Path, args: argparse.Namespace, started_at: str,
                  duration_ms: int, metrics: Dict[str, Any]) -> None:
    if args.command:
        command = args.command
    else:
        command = "py -3 " + " ".join(
            (f'"{a}"' if (" " in a or ";" in a) else a) for a in sys.argv)
    notes = (
        "No JVM at query time: the ANTLR4 Python3 parser is generated from the "
        "fork's own grammars (zero embedded actions, zero semantic predicates), "
        "so the whole run is a bare CPython process. parseMs and indexMs are "
        "accumulated per file - each tree is indexed and then dropped, which "
        "keeps peak RSS bounded and leaves both phase totals exact. "
        f"Prediction mode: {metrics['aux']['predictionMode']} "
        f"({metrics['aux']['sllBailouts']} SLL bailouts). "
        "startupMs is process start -> interpreter ready; aux.parserReadyMs is "
        "process start -> generated parser importable (the ATN load), which is "
        "the closer analogue of the JVM lanes' injector-ready figure. "
        "peakRssMb is self-measured in-process and excludes the interpreter's "
        "own teardown."
    )
    receipt = {
        "id": args.id,
        "section": "analytics",
        "title": "Model analytics - no JVM (Python + ANTLR4)",
        "command": command,
        "startedAt": started_at,
        "durationMs": duration_ms,
        "host": {
            "os": f"{platform.system()} {platform.release()}",
            "cpu": platform.processor() or platform.machine(),
            "python": platform.python_version(),
        },
        "metrics": metrics,
        "notes": notes,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="\n") as fh:
        json.dump(receipt, fh, indent=2, ensure_ascii=True, sort_keys=False)
        fh.write("\n")


if __name__ == "__main__":
    try:
        sys.exit(main(sys.argv[1:]))
    except SystemExit:
        raise
    except KeyboardInterrupt:
        demo("error", message="interrupted")
        sys.exit(130)
    except Exception as exc:                          # pragma: no cover
        demo("error", message=f"{type(exc).__name__}: {exc}")
        raise
