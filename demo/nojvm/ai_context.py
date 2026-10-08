"""
LLM context cost by representation - with NO JVM.

The question this lane answers is narrow and measurable: *how many tokens does
it cost to give a language model enough context to reason about this model
corpus?* - and how does that cost change when the context comes from the
fork's parser instead of from raw text.

It reuses lane C's machinery exactly: the ANTLR4 Python3 parser generated from
the fork's own grammars (`demo/work/nojvm-gen/`), the demo-contract section 5
index, and the Q1-Q4 query semantics in `queries.py`. No JVM is started, no
network call is made, and every number printed is computed from files on disk.

Three tasks, each comparing representations of THE SAME information:

  Task 1  WHOLE-MODEL STRUCTURAL CONTEXT
          (a) every *.rosetta file concatenated verbatim
          (b) a structural digest emitted from the parse trees
  Task 2  ONE TYPE AND ITS TRANSITIVE CLOSURE
          (a) the whole text of every *.rosetta file holding a closure member
          (b) the generated Java for the same declarations (demo/work/gen-m1)
          (c) the digest restricted to the closure
  Task 3  ANSWERING THE FOUR STRUCTURAL QUESTIONS (demo-contract section 5)
          (a) read the corpus yourself   (b) ask the index that already parsed it

Token counts are EXACT `tiktoken` counts (default encoding `o200k_base`,
recorded in the receipt); characters and UTF-8 bytes are reported beside every
one of them so nothing rests on the tokenizer alone.

Usage (after gen-parser + setup-venv, see README.md), from demo/nojvm/ with the
venv's interpreter - `../work/venv/Scripts/python.exe` on Windows (PowerShell or
Git Bash), `../work/venv/bin/python` on Linux and macOS:

  ../work/venv/Scripts/python.exe ai_context.py --roots "../corpus/builtins;../corpus/cdm-5.38.0;../corpus/iso20022-1.38.0;../corpus/drr-6.34.1" \
      --receipt ../receipts/ai.context.json

`tiktoken` must be importable. `setup-venv.ps1` / `setup-venv.sh` install it
(pinned) into that venv beside `antlr4-python3-runtime`; another interpreter
such as `py -3` works only if tiktoken is installed for it too. tiktoken fetches
its o200k_base encoding on first use; on a host without network, set
TIKTOKEN_CACHE_DIR to a staged cache (README.md, step 2).

Exit codes: 0 ok, 1 run failure, 2 environment not prepared.
"""

from __future__ import annotations

# Startup measurement must be the very first thing that happens.
import time as _time

_T_PERF0 = _time.perf_counter()   # monotonic origin for every duration below
_T_WALL0 = _time.time()           # wall clock at interpreter-ready

import argparse
import dataclasses
import json
import os
import platform
import sys
import time
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence, Set, Tuple

_HERE = Path(__file__).resolve().parent
if str(_HERE) not in sys.path:
    sys.path.insert(0, str(_HERE))

from queries import (  # noqa: E402  (path bootstrap must precede the import)
    Attr, Decl, ModelIndex,
    KIND_ANNOTATION, KIND_BASIC_TYPE, KIND_BODY, KIND_CHOICE, KIND_CORPUS,
    KIND_ENUM, KIND_FUNC, KIND_LIBRARY_FUNCTION, KIND_META_TYPE,
    KIND_RECORD_TYPE, KIND_REPORT, KIND_RULE, KIND_RULE_SOURCE, KIND_SEGMENT,
    KIND_SYNONYM_SOURCE, KIND_TYPE, KIND_TYPE_ALIAS,
    aux_counts, is_builtin_namespace, results_block,
)
from analytics import (  # noqa: E402
    CTX_ANNOTATION_REF, CTX_ATTRIBUTE, CTX_QNAME, CTX_TYPE_CALL, CTX_VALID_ID,
    DEFAULT_GEN_DIR, DEFAULT_VENV_DIR,
    Extractor, ParseDriver, _CountingErrorListener,
    _ms, _peak_rss_mb, _process_start_epoch,
    demo, discover, label_of, log, prepare_environment, split_roots,
)

# The `expression` rule is deeply left-recursive; give CPython headroom exactly
# as analytics.py does before touching a CDM/DRR tree.
sys.setrecursionlimit(max(sys.getrecursionlimit(), 10000))

LANE = "plus-nojvm-ai"
DEFAULT_ENCODING = "o200k_base"
DEFAULT_GEN_JAVA = _HERE.parent / "work" / "gen-m1"
DEFAULT_OUT_DIR = _HERE.parent / "work" / "ai"

DIGEST_FILE = "model-digest.txt"
SLICE_FILE = "closure-slice.txt"
ANSWERS_FILE = "answers.json"
WALK_FILE = "walk-trace.txt"
BREAKDOWN_FILE = "function-breakdown.txt"
IMPACT_FILE = "impact-report.txt"


# ==========================================================================
# Peak resident set size
# ==========================================================================

def peak_rss_mb() -> Optional[float]:
    """Peak working set in MiB, or None when the platform will not supply it.

    `analytics._peak_rss_mb` returns None on this host: it calls
    `K32GetProcessMemoryInfo` without declaring `argtypes`, so the pseudo-handle
    from `GetCurrentProcess()` does not survive the marshalling. That module is
    another lane's committed source and is not edited from here, so this is a
    local, argtypes-declared implementation with the shared helper kept as the
    fallback. Never guessed - omitted if both fail.
    """
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
            k32.GetCurrentProcess.argtypes = []
            k32.GetCurrentProcess.restype = wt.HANDLE
            fn = k32.K32GetProcessMemoryInfo
            fn.argtypes = [wt.HANDLE, ctypes.POINTER(PMC), wt.DWORD]
            fn.restype = wt.BOOL
            counters = PMC()
            counters.cb = ctypes.sizeof(PMC)
            if not fn(k32.GetCurrentProcess(), ctypes.byref(counters),
                      ctypes.sizeof(PMC)):
                return _peak_rss_mb()
            return round(counters.PeakWorkingSetSize / (1024.0 * 1024.0), 1)
    except Exception:
        pass
    return _peak_rss_mb()


# ==========================================================================
# Token / character / byte measurement
# ==========================================================================

@dataclass(slots=True)
class Measure:
    """One representation's exact size in three units."""
    tokens: int = 0
    chars: int = 0
    bytes: int = 0

    def as_dict(self, **extra: Any) -> Dict[str, Any]:
        out: Dict[str, Any] = {"tokens": self.tokens, "chars": self.chars,
                               "bytes": self.bytes}
        out.update(extra)
        return out


class Tokenizer:
    """Exact tiktoken counting. No estimation anywhere in this file."""

    def __init__(self, encoding_name: str) -> None:
        try:
            import tiktoken
        except ImportError:
            demo("error", message="tiktoken is not importable")
            log("")
            log("This lane counts EXACT tokens; there is no estimation fallback.")
            log("Run this script with the venv's interpreter (demo/nojvm/setup-venv.ps1 or")
            log(".sh installs a pinned tiktoken into demo/work/venv/), or install it for the")
            log("interpreter you are running:  python -m pip install tiktoken==0.14.0")
            raise SystemExit(2)
        self.name = encoding_name
        self.version = getattr(tiktoken, "__version__", "unknown")
        try:
            self.enc = tiktoken.get_encoding(encoding_name)
        except Exception as exc:
            demo("error", message=f"tiktoken encoding {encoding_name!r} "
                                  f"unavailable: {type(exc).__name__}: {exc}")
            log("The BPE file is fetched once and cached; this run had neither "
                "a cache nor a network.")
            log("Offline: set TIKTOKEN_CACHE_DIR to a cache staged on a connected "
                "machine (demo/nojvm/README.md, step 2).")
            raise SystemExit(2)
        self.ms = 0.0
        self.calls = 0
        self.chars = 0

    def measure(self, text: str) -> Measure:
        """Exact token/char/byte size of `text`.

        `encode_ordinary` is used deliberately: it treats every byte of the
        input as ordinary text, so a literal `<|endoftext|>` inside a corpus
        file is counted as the characters it is rather than raising or
        collapsing to one special token.
        """
        t0 = _time.perf_counter()
        n = len(self.enc.encode_ordinary(text))
        self.ms += (_time.perf_counter() - t0) * 1000.0
        self.calls += 1
        self.chars += len(text)
        return Measure(tokens=n, chars=len(text),
                       bytes=len(text.encode("utf-8")))


def ratio(baseline: Measure, fork: Measure) -> Optional[float]:
    if fork.tokens <= 0:
        return None
    return round(baseline.tokens / fork.tokens, 2)


def reduction_pct(baseline: Measure, fork: Measure) -> Optional[float]:
    if baseline.tokens <= 0:
        return None
    return round((1.0 - fork.tokens / baseline.tokens) * 100.0, 2)


# ==========================================================================
# The richer model records the digest needs
# ==========================================================================
#
# `queries.Attr` / `queries.Decl` are the cross-lane contract and are NOT
# modified here. These subclasses add exactly what a structural digest needs
# and nothing else, so the index this file builds is still the index
# `analytics.py` builds - which is what lets the Q1-Q4 answers below be
# checked against the committed analytics receipt.

@dataclass(slots=True)
class RichAttr(Attr):
    card: str = ""                                   # "1..1", "0..*", "" if absent
    direction: str = ""                              # "" | "in" | "out" (functions)
    ann_pairs: Tuple[Tuple[str, str], ...] = ()      # [("metadata","scheme")]


@dataclass(slots=True)
class RichDecl(Decl):
    ann_pairs: Tuple[Tuple[str, str], ...] = ()
    enum_values: Tuple[str, ...] = ()
    choice_options: Tuple[str, ...] = ()
    rule_from: str = ""
    header_text: str = ""                            # minor kinds: header as written
    rune_annotations: Tuple[str, ...] = ()           # `@Foo` form (0 in this corpus)
    target_type: str = ""                            # typeAlias / metaType target
    report_from: str = ""                            # rosettaReport: FROM typeCall
    report_type: str = ""                            # rosettaReport: WITH TYPE qname
    # -- body-derived, for tasks 5 and 6 ------------------------------------
    # These come from the EXPRESSION subtrees. They are NAME-level facts read
    # off the parse tree; nothing here is type-resolved by the parser, and the
    # receipt says so. `features_read` in particular is a set of feature NAMES
    # with no receiver type attached.
    callees: Tuple[str, ...] = ()                    # #FunctionCallExpr qualifiedNames
    features_read: Tuple[str, ...] = ()              # #FeatureCallExpr / #DeepFeatureCallExpr
    symbol_refs: Tuple[str, ...] = ()                # #SymbolRefExpr qualifiedNames
    conditions: Tuple[str, ...] = ()
    aliases: Tuple[str, ...] = ()
    operations: Tuple[str, ...] = ()                 # `set out -> a -> b`


# Context classes beyond the ones analytics.py already binds.
CTX_CARD = "RosettaCardinalityContext"
CTX_ENUM_VALUE = "RosettaEnumValueContext"
CTX_CHOICE_OPTION = "ChoiceOptionContext"
CTX_RUNE_ANNOTATIONS = "RuneAnnotationsContext"
CTX_RUNE_ANNOTATION = "RuneAnnotationContext"
CTX_SEGMENT = "SegmentContext"

# Expression alternatives the body walk binds to. Labelled alternatives of the
# `expression` rule (RosettaParser.g4 lines 294-392), so each is its own
# context class and can be recognised without looking at any text.
CTX_FUNCTION_CALL = "FunctionCallExprContext"        # qualifiedName ( args )
CTX_FEATURE_CALL = "FeatureCallExprContext"          # expression -> validID
CTX_DEEP_FEATURE_CALL = "DeepFeatureCallExprContext"  # expression ->> validID
CTX_SYMBOL_REF = "SymbolRefExprContext"              # bare qualifiedName
CTX_CONDITION = "ConditionContext"
CTX_POST_CONDITION = "PostConditionContext"
CTX_SHORTCUT = "ShortcutDeclarationContext"
CTX_OPERATION = "OperationContext"

# Subtrees the digest deliberately does not read. Every one of these is named
# in the receipt's `notes` as a declared drop.
SKIP_SUBTREES = frozenset({
    "DefinableContext",              # <"description">
    "DocReferenceContext",
    "RosettaSynonymContext",
    "ClassSynonymContext",
    "RosettaEnumSynonymContext",
    "RosettaExternalClassContext",   # synonym-source / rule-source bodies
    "RosettaExternalEnumContext",
    "LabelAnnotationContext",
    "RuleReferenceAnnotationContext",
    "ConditionContext",
    "PostConditionContext",
    "OperationContext",
    "ShortcutDeclarationContext",
    "ExprWithThenContext",
    "ExpressionContext",
    "AnnotationRefContext",          # rendered separately, in pair form
})

# Subtrees that carry DOCUMENTATION and MAPPING content rather than structure.
# Used only by the attribution pass below, which exists to answer the obvious
# objection to task 1: "your reduction is just deleting the documentation".
SPAN_DOC_CONTEXTS = frozenset({
    "DefinableContext",              # <"description">
    "DocReferenceContext",
    "RosettaSynonymContext",
    "ClassSynonymContext",
    "RosettaEnumSynonymContext",
    "RosettaExternalSynonymContext",
    "RosettaExternalClassContext",
    "RosettaExternalEnumContext",
    "LabelAnnotationContext",
    "RuleReferenceAnnotationContext",
})

# Kinds rendered from their header token text rather than by a bespoke printer.
HEADER_TEXT_KINDS = frozenset({
    KIND_TYPE_ALIAS, KIND_META_TYPE, KIND_BASIC_TYPE, KIND_RECORD_TYPE,
    KIND_LIBRARY_FUNCTION, KIND_REPORT, KIND_BODY, KIND_CORPUS, KIND_SEGMENT,
    KIND_SYNONYM_SOURCE, KIND_RULE_SOURCE,
})

# Declaration kinds whose subtree carries an expression body worth reading.
BODY_FACT_KINDS = frozenset({KIND_FUNC, KIND_RULE, KIND_TYPE, KIND_CHOICE})

# Declaration kinds an attribute's declared type can resolve to.
TYPE_LIKE_KINDS = frozenset({
    KIND_TYPE, KIND_CHOICE, KIND_ENUM, KIND_TYPE_ALIAS, KIND_BASIC_TYPE,
    KIND_RECORD_TYPE, KIND_META_TYPE,
})

KIND_ORDER: Dict[str, int] = {
    KIND_TYPE: 0, KIND_CHOICE: 1, KIND_ENUM: 2, KIND_TYPE_ALIAS: 3,
    KIND_FUNC: 4, KIND_RULE: 5, KIND_REPORT: 6, KIND_ANNOTATION: 7,
    KIND_BASIC_TYPE: 8, KIND_RECORD_TYPE: 9, KIND_META_TYPE: 10,
    KIND_LIBRARY_FUNCTION: 11, KIND_SYNONYM_SOURCE: 12, KIND_RULE_SOURCE: 13,
    KIND_BODY: 14, KIND_CORPUS: 15, KIND_SEGMENT: 16,
}

KIND_KEYWORD: Dict[str, str] = {
    KIND_TYPE: "type", KIND_CHOICE: "choice", KIND_ENUM: "enum",
    KIND_FUNC: "func", KIND_RULE: "rule", KIND_ANNOTATION: "annotation",
}


class RichExtractor(Extractor):
    """`analytics.Extractor` plus the structural detail a digest needs.

    Every added read uses a NON-COUNTING accessor, so `index.nodes_visited`
    stays bit-for-bit what `analytics.py` reports and the Q1-Q4 answers this
    run produces are the same object the analytics lane produces.
    """

    def __init__(self, parser_cls: Any, index: ModelIndex) -> None:
        super().__init__(parser_cls, index)
        self.tok_inputs = getattr(parser_cls, "INPUTS", None)
        self.tok_output = getattr(parser_cls, "OUTPUT", None)
        self.tok_type = getattr(parser_cls, "TYPE", None)
        self.tok_source = getattr(parser_cls, "SOURCE", None)
        self.attrs_without_card = 0
        self.rune_annotation_count = 0

    # -- non-counting primitives -------------------------------------------

    @staticmethod
    def _kids_nc(ctx: Any) -> Tuple[Any, ...]:
        return tuple(getattr(ctx, "children", None) or ())

    def _first_nc(self, ctx: Any, cls_name: str) -> Optional[Any]:
        for c in self._kids_nc(ctx):
            if c.__class__.__name__ == cls_name:
                return c
        return None

    def _all_nc(self, ctx: Any, cls_name: str) -> List[Any]:
        return [c for c in self._kids_nc(ctx)
                if c.__class__.__name__ == cls_name]

    def _qname_text(self, ctx: Any) -> str:
        parts = [self._ident(c) for c in self._kids_nc(ctx)
                 if c.__class__.__name__ == CTX_VALID_ID]
        return ".".join(parts)

    def _typecall_text(self, ctx: Any) -> str:
        """A `typeCall`'s target as written, parameters included when present."""
        qn = self._first_nc(ctx, CTX_QNAME)
        base = self._qname_text(qn) if qn is not None else ""
        args = [c for c in self._kids_nc(ctx)
                if c.__class__.__name__ == "TypeCallArgumentContext"]
        if not args:
            return base
        return base + "(" + ", ".join(a.getText() for a in args) + ")"

    def _ann_pairs(self, ctx: Any) -> Tuple[Tuple[str, str], ...]:
        """`[metadata scheme]` -> (("metadata","scheme"),) - PAIRED, not flattened.

        `Decl.annotations` / `Decl.annotation_quals` are two independent tuples,
        so a declaration carrying `[metadata key]` and `[rootType]` loses the
        pairing there. The digest needs the pairing, so it is read again here.
        """
        out: List[Tuple[str, str]] = []
        for ref in self._all_nc(ctx, CTX_ANNOTATION_REF):
            ids = [self._ident(c) for c in self._kids_nc(ref)
                   if c.__class__.__name__ == CTX_VALID_ID]
            if ids:
                out.append((ids[0], ids[1] if len(ids) > 1 else ""))
        return tuple(out)

    def _rune_annotations(self, ctx: Any) -> Tuple[str, ...]:
        """The `@Foo` annotation form. Counted so 'none in this corpus' is a
        measurement rather than an assumption."""
        holder = self._first_nc(ctx, CTX_RUNE_ANNOTATIONS)
        if holder is None:
            return ()
        out: List[str] = []
        for ann in self._all_nc(holder, CTX_RUNE_ANNOTATION):
            qn = self._first_nc(ann, CTX_QNAME)
            if qn is not None:
                out.append("@" + self._qname_text(qn))
        self.rune_annotation_count += len(out)
        return tuple(out)

    def _header_text(self, ctx: Any) -> str:
        """Every token of a declaration's header, descriptions and bodies aside.

        Used for the declaration kinds with no attribute list of their own
        (`report`, `corpus`, `body`, `segment`, `synonym source`, `rule source`,
        `typeAlias`, `basicType`, `recordType`, `metaType`, library functions).
        Driven entirely by the parse tree - the walk skips a subtree by its
        CONTEXT CLASS, never by matching text.
        """
        parts: List[str] = []

        def walk(node: Any) -> None:
            cls = node.__class__.__name__
            if cls in SKIP_SUBTREES:
                return
            kids = getattr(node, "children", None)
            if not kids:
                txt = node.getText()
                if txt and txt != "<EOF>":
                    parts.append(txt[1:] if txt.startswith("^") else txt)
                return
            for k in kids:
                walk(k)

        walk(ctx)
        return " ".join(parts)

    def _segment_path(self, ctx: Any) -> str:
        """`-> a -> b` from an `operation`'s nested `segment` chain."""
        parts: List[str] = []
        cur = ctx
        while cur is not None:
            vid = self._first_nc(cur, CTX_VALID_ID)
            if vid is not None:
                parts.append(self._ident(vid))
            cur = self._first_nc(cur, CTX_SEGMENT)
        return " -> ".join(parts)

    def _body_facts(self, ctx: Any) -> Dict[str, Tuple[str, ...]]:
        """Name-level facts from a declaration's expression subtrees.

        Iterative (a chained CDM expression will exhaust a recursive walk) and
        driven entirely by labelled-alternative CONTEXT CLASSES, so no text is
        matched anywhere. Everything returned is a NAME as written: no callee
        is resolved to a declaration here and no feature call carries its
        receiver's type. Resolution happens later, against the index, and the
        residue is counted and reported rather than hidden.
        """
        callees: List[str] = []
        features: List[str] = []
        symbols: List[str] = []
        conditions: List[str] = []
        aliases: List[str] = []
        operations: List[str] = []
        stack: List[Any] = [ctx]
        first = True
        while stack:
            node = stack.pop()
            cls = node.__class__.__name__
            if not first:
                if cls == CTX_FUNCTION_CALL:
                    qn = self._first_nc(node, CTX_QNAME)
                    if qn is not None:
                        callees.append(self._qname_text(qn))
                elif cls in (CTX_FEATURE_CALL, CTX_DEEP_FEATURE_CALL):
                    vid = self._first_nc(node, CTX_VALID_ID)
                    if vid is not None:
                        features.append(self._ident(vid))
                elif cls == CTX_SYMBOL_REF:
                    qn = self._first_nc(node, CTX_QNAME)
                    if qn is not None:
                        symbols.append(self._qname_text(qn))
                elif cls in (CTX_CONDITION, CTX_POST_CONDITION):
                    vid = self._first_nc(node, CTX_VALID_ID)
                    conditions.append(self._ident(vid) if vid is not None
                                      else "(unnamed)")
                elif cls == CTX_SHORTCUT:
                    vid = self._first_nc(node, CTX_VALID_ID)
                    if vid is not None:
                        aliases.append(self._ident(vid))
                elif cls == CTX_OPERATION:
                    vid = self._first_nc(node, CTX_VALID_ID)
                    seg = self._first_nc(node, CTX_SEGMENT)
                    tgt = self._ident(vid) if vid is not None else "?"
                    if seg is not None:
                        tgt += " -> " + self._segment_path(seg)
                    operations.append(tgt)
            first = False
            kids = getattr(node, "children", None)
            if kids:
                stack.extend(kids)
        return {"callees": tuple(callees), "features_read": tuple(features),
                "symbol_refs": tuple(symbols), "conditions": tuple(conditions),
                "aliases": tuple(aliases), "operations": tuple(operations)}

    def _report_header(self, ctx: Any) -> Tuple[str, str]:
        """A `rosettaReport`'s `FROM typeCall` and `WITH TYPE qualifiedName`.

        Located by walking the direct children in order and remembering the
        last keyword TOKEN TYPE seen - the grammar puts several bare
        `qualifiedName` children side by side (`when`, `using standard`,
        `with type`, `with source`) and only their position distinguishes them.
        """
        from_t = ""
        type_q = ""
        pending_type = False
        for c in self._kids_nc(ctx):
            sym = getattr(c, "symbol", None)
            if sym is not None:
                if self.tok_type is not None and sym.type == self.tok_type:
                    pending_type = True
                elif self.tok_source is not None and sym.type == self.tok_source:
                    pending_type = False
                continue
            cls = c.__class__.__name__
            if cls == CTX_TYPE_CALL and not from_t:
                from_t = self._typecall_text(c)
            elif cls == CTX_QNAME and pending_type and not type_q:
                type_q = self._qname_text(c)
                pending_type = False
        return from_t, type_q

    def _function_directions(self, ctx: Any) -> List[str]:
        """`in`/`out` per direct `attribute` child, by INPUTS/OUTPUT token order."""
        dirs: List[str] = []
        cur = "in"
        for c in self._kids_nc(ctx):
            sym = getattr(c, "symbol", None)
            if sym is not None:
                if self.tok_inputs is not None and sym.type == self.tok_inputs:
                    cur = "in"
                elif self.tok_output is not None and sym.type == self.tok_output:
                    cur = "out"
                continue
            if c.__class__.__name__ == CTX_ATTRIBUTE:
                dirs.append(cur)
        return dirs

    # -- overrides ----------------------------------------------------------

    def _attribute(self, ctx: Any) -> Attr:
        base = super()._attribute(ctx)          # counts nodes exactly as lane C does
        card = ""
        cc = self._first_nc(ctx, CTX_CARD)
        if cc is not None:
            txt = cc.getText()
            card = (txt[1:-1] if txt.startswith("(") and txt.endswith(")")
                    else txt)
        else:
            self.attrs_without_card += 1
        return RichAttr(
            **{f.name: getattr(base, f.name) for f in dataclasses.fields(Attr)},
            card=card, ann_pairs=self._ann_pairs(ctx))

    def _declaration(self, ctx: Any, kind: str, namespace: str,
                     file_label: str) -> Decl:
        base = super()._declaration(ctx, kind, namespace, file_label)
        rd = RichDecl(**{f.name: getattr(base, f.name)
                         for f in dataclasses.fields(Decl)})
        rd.ann_pairs = self._ann_pairs(ctx)
        rd.rune_annotations = self._rune_annotations(ctx)

        if kind == KIND_ENUM:
            values: List[str] = []
            for ev in self._all_nc(ctx, CTX_ENUM_VALUE):
                vid = self._first_nc(ev, CTX_VALID_ID)
                if vid is not None:
                    values.append(self._ident(vid))
            rd.enum_values = tuple(values)
        elif kind == KIND_CHOICE:
            opts: List[str] = []
            for co in self._all_nc(ctx, CTX_CHOICE_OPTION):
                tc = self._first_nc(co, CTX_TYPE_CALL)
                if tc is not None:
                    opts.append(self._typecall_text(tc))
            rd.choice_options = tuple(opts)
        elif kind == KIND_RULE:
            tc = self._first_nc(ctx, CTX_TYPE_CALL)
            rd.rule_from = self._typecall_text(tc) if tc is not None else ""
        elif kind == KIND_FUNC:
            dirs = self._function_directions(ctx)
            for i, a in enumerate(rd.attributes):
                if i < len(dirs) and isinstance(a, RichAttr):
                    a.direction = dirs[i]
        elif kind in (KIND_TYPE_ALIAS, KIND_META_TYPE):
            tc = self._first_nc(ctx, CTX_TYPE_CALL)
            rd.target_type = self._typecall_text(tc) if tc is not None else ""
        elif kind == KIND_REPORT:
            rd.report_from, rd.report_type = self._report_header(ctx)

        if kind in BODY_FACT_KINDS:
            facts = self._body_facts(ctx)
            rd.callees = facts["callees"]
            rd.features_read = facts["features_read"]
            rd.symbol_refs = facts["symbol_refs"]
            rd.conditions = facts["conditions"]
            rd.aliases = facts["aliases"]
            rd.operations = facts["operations"]

        if kind in HEADER_TEXT_KINDS:
            rd.header_text = self._header_text(ctx)
        return rd


# ==========================================================================
# The digest renderer
# ==========================================================================

DIGEST_HEADER = (
    "# rune structural digest v1 - emitted from the parse trees by "
    "demo/nojvm/ai_context.py\n"
    "# KEEPS  namespace; declaration kind + name; supertype; every attribute's\n"
    "#        name, declared type and cardinality; [annotations] on both;\n"
    "#        enum values; choice options; function in/out split; rule input\n"
    "#        type; and the header line of every other declaration kind.\n"
    "# DROPS  descriptions <\"...\">, [docReference ...], synonyms and mappings\n"
    "#        (including synonym-source and rule-source bodies), comments, and\n"
    "#        EVERY expression body: conditions, aliases, operations,\n"
    "#        post-conditions and reporting-rule logic.\n"
    "# FORM   `@ns X` opens a namespace; `-` attribute, `=` enum value,\n"
    "#        `|` choice option, `>` function input, `<` function output.\n"
)


def _anns(pairs: Sequence[Tuple[str, str]], rune: Sequence[str] = ()) -> str:
    out = ["".join(("[", h, " ", q, "]")) if q else "".join(("[", h, "]"))
           for h, q in pairs]
    out.extend(rune)
    return (" " + " ".join(out)) if out else ""


def _attr_line(a: Attr, marker: str) -> str:
    card = getattr(a, "card", "")
    pairs = getattr(a, "ann_pairs", ())
    bits = [marker, a.name or "?", a.type_qname or "?"]
    if card:
        bits.append(card)
    return " ".join(bits) + _anns(pairs)


def render_decl(d: Decl) -> List[str]:
    """One declaration as digest lines."""
    kind = d.kind
    ann = _anns(getattr(d, "ann_pairs", ()), getattr(d, "rune_annotations", ()))
    lines: List[str] = []

    if kind in (KIND_TYPE, KIND_CHOICE, KIND_ANNOTATION):
        head = f"{KIND_KEYWORD[kind]} {d.name}"
        if d.extends_qname:
            head += f" : {d.extends_qname}"
        lines.append(head + ann)
        for a in d.attributes:
            lines.append(_attr_line(a, "-"))
        for opt in getattr(d, "choice_options", ()):
            lines.append(f"| {opt}")
    elif kind == KIND_ENUM:
        head = f"enum {d.name}"
        if d.extends_qname:
            head += f" : {d.extends_qname}"
        lines.append(head + ann)
        for v in getattr(d, "enum_values", ()):
            lines.append(f"= {v}")
    elif kind == KIND_FUNC:
        head = f"func {d.name}"
        if d.extends_qname:
            head += f" : {d.extends_qname}"
        lines.append(head + ann)
        for a in d.attributes:
            lines.append(_attr_line(
                a, "<" if getattr(a, "direction", "") == "out" else ">"))
    elif kind == KIND_RULE:
        head = ("eligibility rule " if d.eligibility else "rule ") + d.name
        frm = getattr(d, "rule_from", "")
        if frm:
            head += f" from {frm}"
        lines.append(head + ann)
    else:
        text = getattr(d, "header_text", "")
        if text:
            lines.append(text + ann)
        else:
            lines.append(f"{kind} {d.name}".rstrip() + ann)
        for a in d.attributes:
            lines.append(_attr_line(a, "-"))
    return lines


def _sort_key(d: Decl) -> Tuple[str, int, str, int]:
    return (d.namespace, KIND_ORDER.get(d.kind, 99), d.name, d.line)


def render_digest(decls: Sequence[Decl], header: str) -> str:
    out: List[str] = [header.rstrip("\n")]
    ns = None
    for d in sorted(decls, key=_sort_key):
        if d.namespace != ns:
            ns = d.namespace
            out.append("")
            out.append(f"@ns {ns}" if ns else "@ns (none)")
        out.extend(render_decl(d))
    return "\n".join(out) + "\n"


# ==========================================================================
# Attribution: how much of the baseline is documentation, not structure?
# ==========================================================================

def doc_spans(tree: Any, comment_types: Tuple[int, ...]) -> List[Tuple[int, int]]:
    """Half-open character intervals of documentation / mapping content.

    Two sources, both structural - a context CLASS for prose and mapping
    subtrees, and a token TYPE for comments (which the lexer puts on the hidden
    channel, so they survive in the token stream). No text is matched anywhere.
    The walk is iterative: a recursive one can exhaust the stack on a deeply
    chained CDM expression.
    """
    spans: List[Tuple[int, int]] = []
    stack: List[Any] = [tree]
    while stack:
        node = stack.pop()
        if node.__class__.__name__ in SPAN_DOC_CONTEXTS:
            st = getattr(node, "start", None)
            sp = getattr(node, "stop", None)
            if st is not None and sp is not None:
                spans.append((st.start, sp.stop + 1))
            continue                       # the whole subtree is documentation
        kids = getattr(node, "children", None)
        if kids:
            stack.extend(kids)
    if comment_types:
        parser = getattr(tree, "parser", None)
        stream = parser.getTokenStream() if parser is not None else None
        for t in getattr(stream, "tokens", ()) or ():
            if t.type in comment_types:
                spans.append((t.start, t.stop + 1))
    return spans


def strip_spans(text: str, spans: List[Tuple[int, int]]) -> str:
    """`text` with every span removed, overlaps merged."""
    if not spans:
        return text
    spans.sort()
    out: List[str] = []
    pos = 0
    for a, b in spans:
        if b <= pos:
            continue
        if a > pos:
            out.append(text[pos:a])
        pos = max(pos, b)
    out.append(text[pos:])
    return "".join(out)


# ==========================================================================
# Type resolution + transitive closure
# ==========================================================================

class TypeResolver:
    """Resolve a declared type name to a declaration.

    Same resolution order as `queries._resolve_parent` (exact qualified name,
    then unique simple name, then same-namespace, then same-file, then first)
    so the closure and the Q4 chain agree about what `extends X` points at. The
    candidate pool is widened from data types to every TYPE-LIKE kind, because
    an attribute's declared type may be an enum, a type alias or a basic type.
    """

    def __init__(self, decls: Sequence[Decl]) -> None:
        self.by_qname: Dict[str, Decl] = {}
        self.by_simple: Dict[str, List[Decl]] = {}
        for d in decls:
            if d.kind not in TYPE_LIKE_KINDS or not d.name:
                continue
            self.by_qname.setdefault(d.qname, d)
            self.by_simple.setdefault(d.name, []).append(d)
        self.unresolved = 0
        self.ambiguous = 0
        self.resolutions = 0

    def reset_stats(self) -> None:
        self.unresolved = self.ambiguous = self.resolutions = 0

    def stats(self) -> Dict[str, int]:
        return {"resolutions": self.resolutions, "unresolved": self.unresolved,
                "ambiguous": self.ambiguous}

    def resolve(self, qname: str, simple: str, origin: Decl) -> Optional[Decl]:
        if not simple:
            return None
        self.resolutions += 1
        if qname:
            hit = self.by_qname.get(qname)
            if hit is not None:
                return hit
        cands = self.by_simple.get(simple)
        if not cands:
            self.unresolved += 1
            return None
        if len(cands) == 1:
            return cands[0]
        for c in cands:
            if c.namespace == origin.namespace:
                return c
        for c in cands:
            if c.file == origin.file:
                return c
        self.ambiguous += 1
        return cands[0]


def closure_of(root: Decl, resolver: TypeResolver,
               include_builtins: bool = False) -> List[Decl]:
    """Transitive closure of `root` over supertypes and attribute types.

    Walks `extends` edges and every attribute's declared type, plus a choice's
    options and an enum's (absent) members, until nothing new is reachable.
    Builtin declarations (`com.rosetta*`) terminate the walk by default: the
    fact that an attribute is a `string` needs no further context, and the
    builtin namespaces are ungenerated, so including them would put files in
    the baseline that no reader would ever paste.
    """
    seen: Dict[int, Decl] = {id(root): root}
    frontier: List[Decl] = [root]
    while frontier:
        d = frontier.pop()
        refs: List[Tuple[str, str]] = []
        if d.extends_simple:
            refs.append((d.extends_qname or "", d.extends_simple))
        for a in d.attributes:
            if a.type_simple:
                refs.append((a.type_qname, a.type_simple))
        for opt in getattr(d, "choice_options", ()):
            simple = opt.split("(")[0].split(".")[-1]
            refs.append((opt.split("(")[0], simple))
        for qn, simple in refs:
            hit = resolver.resolve(qn, simple, d)
            if hit is None or id(hit) in seen:
                continue
            if hit.builtin and not include_builtins:
                continue
            seen[id(hit)] = hit
            frontier.append(hit)
    return list(seen.values())


# ==========================================================================
# TASK 4 - walking the model
# ==========================================================================
#
# What an agent actually does with an implemented model: start at a report or
# a rule and follow the attributes down to the leaf fields, carrying the type
# and cardinality at every hop. Without something that has parsed the model
# this is manual reading; with an index it is a graph walk.

@dataclass(slots=True)
class Hop:
    depth: int
    path: str                # a -> b -> c
    attr: str
    type_qname: str
    card: str
    outcome: str             # descend | leaf-enum | leaf-basic | unresolved
                             # | cycle | depth-capped
    file: str = ""


def walk_type(root: Decl, resolver: TypeResolver, max_depth: int,
              max_hops: int) -> Tuple[List[Hop], Dict[str, Any]]:
    """Depth-first walk of a type's attribute tree down to its leaves.

    Explicit stack, on-path cycle guard, depth cap and hop cap - a CDM type
    graph is cyclic (Party holds things that hold Party) and an unguarded walk
    does not terminate. Every stop is CLASSIFIED, so the trace says why it
    stopped rather than silently ending.
    """
    hops: List[Hop] = []
    touched: Dict[int, Decl] = {id(root): root}
    stats = {"cycles": 0, "depthCapped": 0, "unresolved": 0, "leaves": 0,
             "hopCapped": 0}
    # stack entries: (decl, path-prefix, depth, tuple of decl-ids on the path)
    stack: List[Tuple[Decl, str, int, Tuple[int, ...]]] = [
        (root, root.name, 0, (id(root),))]
    while stack:
        decl, prefix, depth, on_path = stack.pop()
        for a in reversed(decl.attributes):
            if len(hops) >= max_hops:
                stats["hopCapped"] += 1
                break
            path = f"{prefix} -> {a.name}"
            target = resolver.resolve(a.type_qname, a.type_simple, decl)
            if target is None:
                outcome, tgt_file = "unresolved", ""
                stats["unresolved"] += 1
            else:
                touched.setdefault(id(target), target)
                tgt_file = target.file
                if target.kind in (KIND_TYPE, KIND_CHOICE) and not target.builtin:
                    if id(target) in on_path:
                        outcome = "cycle"
                        stats["cycles"] += 1
                    elif depth + 1 >= max_depth:
                        outcome = "depth-capped"
                        stats["depthCapped"] += 1
                    else:
                        outcome = "descend"
                        stack.append((target, path, depth + 1,
                                      on_path + (id(target),)))
                elif target.kind == KIND_ENUM:
                    outcome = "leaf-enum"
                    stats["leaves"] += 1
                else:
                    outcome = "leaf-basic"
                    stats["leaves"] += 1
            hops.append(Hop(depth=depth, path=path, attr=a.name,
                            type_qname=a.type_qname or a.type_simple,
                            card=a.card if isinstance(a, RichAttr) else "",
                            outcome=outcome, file=tgt_file))
    stats["hops"] = len(hops)
    stats["maxDepth"] = max((h.depth for h in hops), default=0) + 1 if hops else 0
    stats["declsTouched"] = len(touched)
    stats["filesCrossed"] = len({d.file for d in touched.values() if d.file})
    return hops, stats


def decl_label(d: Decl) -> str:
    """A stable name for a declaration, including the unnamed ones.

    `rosettaReport` has no `validID` in the grammar, so its `qname` ends in a
    bare dot. Reports are identified by their file and line instead of being
    printed as a dangling namespace.
    """
    if d.name:
        return d.qname
    return f"{d.namespace}.({d.kind}@{d.file}:{d.line})"


def render_walk(root: Decl, report: Optional[Decl], hops: Sequence[Hop],
                stats: Dict[str, Any], max_depth: int,
                files_crossed: int) -> str:
    head = [
        "# model walk - emitted by demo/nojvm/ai_context.py from the parse trees",
        f"# root type   : {root.qname}",
    ]
    if report is not None:
        head.append(f"# reached via : {report.header_text or decl_label(report)}")
        head.append(f"#               declared in {report.file}:{report.line}")
    head += [
        f"# hops {stats['hops']} | max depth {stats['maxDepth']} (cap "
        f"{max_depth}) | declarations touched {stats['declsTouched']} | "
        f"files crossed {files_crossed}",
        f"# leaves {stats['leaves']} | cycles stopped {stats['cycles']} | "
        f"depth-capped {stats['depthCapped']} | unresolved {stats['unresolved']}",
        "# each line: <indent by depth> attribute  Type  cardinality  [outcome]",
        "",
    ]
    body = [f"{'  ' * h.depth}{h.attr} {h.type_qname} {h.card} [{h.outcome}]"
            for h in hops]
    return "\n".join(head + body) + "\n"


# ==========================================================================
# TASK 5 - breaking down a function
# ==========================================================================

def function_breakdown(fn: Decl, resolver: TypeResolver,
                       by_name: Dict[str, List[Decl]]
                       ) -> Tuple[Dict[str, Any], List[Decl]]:
    """Everything an agent needs about one function, plus what it touches.

    Callees are resolved by NAME against the declaration index (functions and
    rules), because the parser records a call as a `qualifiedName`, not as a
    binding. Names that resolve to nothing are counted and listed, never
    dropped. Attributes read are feature-call NAMES with no receiver type -
    that would need full type inference, which this lane does not do.
    """
    inputs = [a for a in fn.attributes
              if getattr(a, "direction", "") != "out"]
    outputs = [a for a in fn.attributes
               if getattr(a, "direction", "") == "out"]

    touched: Dict[int, Decl] = {id(fn): fn}
    typed: List[Dict[str, str]] = []
    for a in fn.attributes:
        t = resolver.resolve(a.type_qname, a.type_simple, fn)
        if t is not None:
            touched.setdefault(id(t), t)
        typed.append({"name": a.name, "type": a.type_qname or a.type_simple,
                      "card": getattr(a, "card", ""),
                      "direction": getattr(a, "direction", ""),
                      "resolved": t is not None})

    resolved_callees: List[Tuple[str, Decl, str]] = []
    unresolved_callees: List[str] = []
    for name in dict.fromkeys(fn.callees):        # de-duplicated, order kept
        simple = name.split(".")[-1]
        cands = [d for d in by_name.get(simple, ())
                 if d.kind in (KIND_FUNC, KIND_RULE, KIND_LIBRARY_FUNCTION)]
        hit, how = pick_by_name(cands, name, fn)
        if hit is None:
            unresolved_callees.append(name)
        else:
            resolved_callees.append((name, hit, how))
            touched.setdefault(id(hit), hit)

    detail = {
        "function": fn.qname,
        "file": fn.file,
        "inputs": [t for t in typed if t["direction"] != "out"],
        "outputs": [t for t in typed if t["direction"] == "out"],
        "callees": [{"name": n, "resolvesTo": d.qname, "kind": d.kind,
                     "matchedBy": how} for n, d, how in resolved_callees],
        "unresolvedCallees": unresolved_callees,
        "attributesRead": sorted(set(fn.features_read)),
        "conditions": list(fn.conditions),
        "aliases": list(fn.aliases),
        "operations": list(fn.operations),
        "counts": {
            "inputs": len(inputs), "outputs": len(outputs),
            "calleeSites": len(fn.callees),
            "distinctCallees": len(set(fn.callees)),
            "calleesResolved": len(resolved_callees),
            "calleesUnresolved": len(unresolved_callees),
            "attributeReadSites": len(fn.features_read),
            "distinctAttributesRead": len(set(fn.features_read)),
            "conditions": len(fn.conditions),
            "aliases": len(fn.aliases),
            "operations": len(fn.operations),
            "distinctDeclarationsTouched": len(touched),
            "distinctFilesTouched": len({d.file for d in touched.values() if d.file}),
        },
    }
    return detail, list(touched.values())


def render_breakdown(d: Dict[str, Any]) -> str:
    out = [
        "# function breakdown - emitted by demo/nojvm/ai_context.py",
        f"# {d['function']}   (file {d['file']})",
        "# callees are resolved by NAME against the declaration index;",
        "# attributes read are feature-call NAMES with NO receiver type -",
        "# this lane does no type inference and does not pretend to.",
        "",
        f"func {d['function']}",
        "  inputs:",
    ]
    for a in d["inputs"]:
        out.append(f"    > {a['name']} {a['type']} {a['card']}"
                   + ("" if a["resolved"] else "   [type UNRESOLVED]"))
    out.append("  output:")
    for a in d["outputs"]:
        out.append(f"    < {a['name']} {a['type']} {a['card']}"
                   + ("" if a["resolved"] else "   [type UNRESOLVED]"))
    out.append(f"  calls ({len(d['callees'])} resolved, "
               f"{len(d['unresolvedCallees'])} unresolved):")
    for c in d["callees"]:
        out.append(f"    -> {c['name']}   [{c['kind']} {c['resolvesTo']}"
                   f", matched by {c['matchedBy']}]")
    for n in d["unresolvedCallees"]:
        out.append(f"    -> {n}   [UNRESOLVED]")
    out.append(f"  reads {len(d['attributesRead'])} distinct attribute names "
               f"({d['counts']['attributeReadSites']} sites, name-based):")
    for a in d["attributesRead"]:
        out.append(f"    . {a}")
    if d["aliases"]:
        out.append("  aliases:")
        out.extend(f"    = {a}" for a in d["aliases"])
    if d["conditions"]:
        out.append("  conditions:")
        out.extend(f"    ? {c}" for c in d["conditions"])
    if d["operations"]:
        out.append("  assigns:")
        out.extend(f"    := {o}" for o in d["operations"])
    return "\n".join(out) + "\n"


# ==========================================================================
# TASK 6 - impact / reverse reference
# ==========================================================================

REF_ATTRIBUTE = "attribute"
REF_EXTENDS = "extends"
REF_CHOICE = "choiceOption"
REF_ALIAS = "typeAliasTarget"
REF_RULE_FROM = "ruleFrom"
REF_REPORT_FROM = "reportFrom"
REF_REPORT_TYPE = "reportType"
REF_FUNC_IN = "functionInput"
REF_FUNC_OUT = "functionOutput"
REF_CALLS = "calls"
REF_RULE_REF = "referencesRule"


def pick_by_name(cands: Sequence[Decl], name: str, src: Decl
                 ) -> Tuple[Optional[Decl], str]:
    """Resolve a NAME to one declaration, reporting HOW it was resolved.

    The same preference chain the type resolver uses (exact qualified name,
    then a unique candidate, then same-namespace, then same-file, then first),
    but the outcome is returned so a caller can tell a certainty from a guess.
    DRR carries the same rule simple-name across many regulation namespaces,
    so the same-namespace step is what makes this usable at all.
    """
    if not cands:
        return None, "none"
    for c in cands:
        if c.qname == name:
            return c, "exact"
    if len(cands) == 1:
        return cands[0], "unique"
    for c in cands:
        if c.namespace == src.namespace:
            return c, "sameNamespace"
    for c in cands:
        if c.file == src.file:
            return c, "sameFile"
    return cands[0], "first"


def build_reverse_index(decls: Sequence[Decl], resolver: TypeResolver,
                        by_name: Dict[str, List[Decl]],
                        stats: Dict[str, int]
                        ) -> Dict[int, List[Tuple[Decl, str]]]:
    """target declaration id -> [(referrer, reference kind), ...].

    Two classes of edge, and the difference matters:

    TYPE-RESOLVED edges (attribute, extends, choice option, type-alias target,
    rule `from`, report `from`/`with type`) come from grammar positions that
    hold a `typeCall` or a `qualifiedName` denoting a type. These are as exact
    as the resolver is.

    NAME-RESOLVED edges (`calls`, `referencesRule`) come from expressions. The
    parser records a call as a `qualifiedName`, not a binding, so the callee is
    matched by NAME against the declaration index. Bare symbol references are
    admitted ONLY when the name resolves to a reporting rule and is not one of
    the referrer's own inputs or aliases - which removes the obvious false
    positive of a local name shadowing a declaration. Both counts are reported
    separately so a reader can discount them.

    What is still NOT here: a feature call's receiver type. That needs type
    inference this lane does not do, so `x -> y` produces no edge at all.
    """
    rev: Dict[int, List[Tuple[Decl, str]]] = {}

    def add(target: Optional[Decl], src: Decl, kind: str) -> None:
        if target is not None and id(target) != id(src):
            rev.setdefault(id(target), []).append((src, kind))

    for d in decls:
        if d.extends_simple:
            add(resolver.resolve(d.extends_qname or "", d.extends_simple, d),
                d, REF_EXTENDS)
        for a in d.attributes:
            if not a.type_simple:
                continue
            if d.kind == KIND_FUNC:
                kind = (REF_FUNC_OUT if getattr(a, "direction", "") == "out"
                        else REF_FUNC_IN)
            else:
                kind = f"{REF_ATTRIBUTE}:{a.name}"
            add(resolver.resolve(a.type_qname, a.type_simple, d), d, kind)
        for opt in getattr(d, "choice_options", ()):
            base = opt.split("(")[0]
            add(resolver.resolve(base, base.split(".")[-1], d), d, REF_CHOICE)
        tgt = getattr(d, "target_type", "")
        if tgt:
            base = tgt.split("(")[0]
            add(resolver.resolve(base, base.split(".")[-1], d), d, REF_ALIAS)
        frm = getattr(d, "rule_from", "")
        if frm:
            base = frm.split("(")[0]
            add(resolver.resolve(base, base.split(".")[-1], d), d, REF_RULE_FROM)
        rf = getattr(d, "report_from", "")
        if rf:
            base = rf.split("(")[0]
            add(resolver.resolve(base, base.split(".")[-1], d), d, REF_REPORT_FROM)
        rt = getattr(d, "report_type", "")
        if rt:
            add(resolver.resolve(rt, rt.split(".")[-1], d), d, REF_REPORT_TYPE)

        # -- name-resolved expression edges --------------------------------
        local = {a.name for a in d.attributes} | set(getattr(d, "aliases", ()))
        for name in dict.fromkeys(getattr(d, "callees", ())):
            simple = name.split(".")[-1]
            cands = [c for c in by_name.get(simple, ())
                     if c.kind in (KIND_FUNC, KIND_RULE, KIND_LIBRARY_FUNCTION)]
            hit, how = pick_by_name(cands, name, d)
            if hit is None:
                stats["callsUnresolved"] = stats.get("callsUnresolved", 0) + 1
                continue
            stats["callsResolved"] = stats.get("callsResolved", 0) + 1
            stats[f"calls_{how}"] = stats.get(f"calls_{how}", 0) + 1
            # A `first`-of-several pick is a GUESS. It stays in the answer -
            # a missed edge is worse than a noisy one when the question is
            # "what breaks" - but it is labelled `?` so the report shows which
            # edges are guesses instead of presenting them as facts.
            add(hit, d, REF_CALLS + ("?" if how == "first" else ""))
        for name in dict.fromkeys(getattr(d, "symbol_refs", ())):
            simple = name.split(".")[-1]
            if simple in local:
                continue
            cands = [c for c in by_name.get(simple, ()) if c.kind == KIND_RULE]
            hit, how = pick_by_name(cands, name, d)
            if hit is None:
                continue
            stats["ruleRefs"] = stats.get("ruleRefs", 0) + 1
            stats[f"ruleRefs_{how}"] = stats.get(f"ruleRefs_{how}", 0) + 1
            add(hit, d, REF_RULE_REF + ("?" if how == "first" else ""))
    return rev


def impact_of(target: Decl, rev: Dict[int, List[Tuple[Decl, str]]],
              max_depth: int) -> Tuple[List[Tuple[int, Decl, str]], Dict[str, Any]]:
    """Breadth-first reverse closure: everything affected by changing `target`."""
    seen: Dict[int, int] = {id(target): 0}
    order: List[Tuple[int, Decl, str]] = []
    frontier: List[Decl] = [target]
    depth = 0
    direct = 0
    while frontier and depth < max_depth:
        depth += 1
        nxt: List[Decl] = []
        for d in frontier:
            for src, kind in rev.get(id(d), ()):
                if id(src) in seen:
                    continue
                seen[id(src)] = depth
                order.append((depth, src, kind))
                nxt.append(src)
                if depth == 1:
                    direct += 1
        frontier = nxt
    by_kind: Dict[str, int] = {}
    by_ref: Dict[str, int] = {}
    for _dep, src, kind in order:
        by_kind[src.kind] = by_kind.get(src.kind, 0) + 1
        head = kind.split(":")[0]
        by_ref[head] = by_ref.get(head, 0) + 1
    stats = {
        "directReferences": direct,
        "transitiveReferences": len(order),
        "depthReached": max((d for d, _s, _k in order), default=0),
        "depthCap": max_depth,
        "byKind": dict(sorted(by_kind.items())),
        "byReferenceKind": dict(sorted(by_ref.items())),
        "filesAffected": len({s.file for _d, s, _k in order if s.file}),
    }
    return order, stats


def render_impact(target: Decl, attr_name: str, mentions: int,
                  order: Sequence[Tuple[int, Decl, str]],
                  stats: Dict[str, Any]) -> str:
    head = [
        "# impact analysis - emitted by demo/nojvm/ai_context.py",
        f"# question: if I change attribute `{attr_name}` on type "
        f"{target.qname}, what is affected?",
        f"# direct references {stats['directReferences']} | transitive "
        f"{stats['transitiveReferences']} | depth reached "
        f"{stats['depthReached']} (cap {stats['depthCap']}) | files "
        f"{stats['filesAffected']}",
        "# granularity: TYPE, not attribute - a type edge means the "
        "declaration names",
        "#   this type, so a change to its shape reaches it. `calls` and "
        "`referencesRule`",
        "#   edges are NAME-resolved from expressions. A feature call's "
        "receiver type is",
        f"#   NOT resolved, so `x -> y` yields no edge; separately {mentions} "
        "feature-call",
        f"#   sites in the corpus use the NAME `{attr_name}`, which is a name "
        "count only.",
        "# each line: <depth> <kind> <qualified name>   via <reference kind>",
        "",
    ]
    body = [f"{d} {s.kind} {decl_label(s)}   via {k}" for d, s, k in order]
    return "\n".join(head + body) + "\n"


def pct_rank(values: Sequence[float], chosen: float) -> Optional[float]:
    if not values:
        return None
    below = sum(1 for v in values if v < chosen)
    return round(100.0 * below / len(values), 1)


# ==========================================================================
# Generated-Java mapping (demo/work/gen-m1)
# ==========================================================================

class JavaTree:
    """Maps `namespace` + declaration name to the generated Java files.

    `a.b.c` + `X` -> `a/b/c/X.java`, plus every `.java` under `a/b/c/meta/` and
    `a/b/c/validation/` (recursively) whose file stem starts with `X`.

    Prefix matching alone over-reaches: `PartyRoleValidator.java` starts with
    `Party`. Attribution is therefore LONGEST-PREFIX over the declaration names
    that actually exist in that namespace, so `PartyRoleValidator` is charged
    to `PartyRole`, and the count of files that prefix-matched but were handed
    to a longer owner is reported as `overmatchAvoided`. Over-charging would
    inflate the baseline this lane is measured against.
    """

    SUBDIRS = ("meta", "validation")

    def __init__(self, root: Path, names_by_ns: Dict[str, List[str]]) -> None:
        self.root = root
        self.available = root.is_dir()
        self.names_by_ns = {ns: sorted(set(v), key=len, reverse=True)
                            for ns, v in names_by_ns.items()}
        self._listing: Dict[str, List[Path]] = {}
        self.overmatch_avoided = 0
        self.missing_primary = 0

    def _extras(self, ns: str) -> List[Path]:
        cached = self._listing.get(ns)
        if cached is not None:
            return cached
        base = self.root.joinpath(*ns.split(".")) if ns else self.root
        found: List[Path] = []
        for sub in self.SUBDIRS:
            d = base / sub
            if d.is_dir():
                found.extend(sorted(p for p in d.rglob("*.java") if p.is_file()))
        self._listing[ns] = found
        return found

    def files_for(self, ns: str, name: str) -> Tuple[Optional[Path], List[Path]]:
        """(the declaration's own class, its meta/validation companions)."""
        if not self.available or not name:
            return None, []
        base = self.root.joinpath(*ns.split(".")) if ns else self.root
        primary: Optional[Path] = base / f"{name}.java"
        if primary is not None and not primary.is_file():
            primary = None
            self.missing_primary += 1
        companions: List[Path] = []
        owners = self.names_by_ns.get(ns, ())
        for p in self._extras(ns):
            stem = p.stem
            if not stem.startswith(name):
                continue
            owner = next((o for o in owners if stem.startswith(o)), name)
            if owner != name:
                self.overmatch_avoided += 1
                continue
            companions.append(p)
        return primary, companions


# ==========================================================================
# CLI
# ==========================================================================

def parse_args(argv: List[str]) -> argparse.Namespace:
    ap = argparse.ArgumentParser(
        prog="ai_context.py",
        description="LLM context cost by representation, with no JVM.")
    ap.add_argument("--roots", required=True,
                    help="';'-separated corpus roots (builtins MUST be one of them)")
    ap.add_argument("--limit", type=int, default=0,
                    help="parse only the first N files (fast calibration; 0 = all)")
    ap.add_argument("--receipt", default=None,
                    help="write a CONTRACTS section 2 receipt JSON to this path")
    ap.add_argument("--id", default="ai.context", help="receipt id")
    ap.add_argument("--command", default=None,
                    help="exact command line to record in the receipt "
                         "(must equal the demo/runs.json entry)")
    ap.add_argument("--encoding", default=DEFAULT_ENCODING,
                    help=f"tiktoken encoding name (default: {DEFAULT_ENCODING})")
    ap.add_argument("--target-type", default="Party",
                    help="Q3 target type simple name (default: Party)")
    ap.add_argument("--gen-java", default=str(DEFAULT_GEN_JAVA),
                    help=f"generated Java tree for task 2b "
                         f"(default: {DEFAULT_GEN_JAVA})")
    ap.add_argument("--receipts-dir", default=str(_HERE.parent / "receipts"),
                    help="sibling analytics receipts to QUOTE cross-lane "
                         "figures from (nothing is quoted if absent)")
    ap.add_argument("--out-dir", default=str(DEFAULT_OUT_DIR),
                    help=f"where the digest and closure slice are written "
                         f"(default: {DEFAULT_OUT_DIR})")
    ap.add_argument("--task2-type", default=None,
                    help="force task 2's root type (simple or qualified name); "
                         "default: chosen by walking the parsed model")
    ap.add_argument("--min-closure", type=int, default=20,
                    help="smallest acceptable task-2 closure (default 20)")
    ap.add_argument("--max-closure", type=int, default=80,
                    help="largest acceptable task-2 closure (default 80)")
    ap.add_argument("--walk-max-depth", type=int, default=6,
                    help="task 4: attribute-tree depth cap (default 6)")
    ap.add_argument("--walk-max-hops", type=int, default=20000,
                    help="task 4: hop cap (default 20000)")
    ap.add_argument("--walk-report", default=None,
                    help="task 4: force the report (name or qualified name)")
    ap.add_argument("--task5-func", default=None,
                    help="task 5: force the function (name or qualified name)")
    ap.add_argument("--task6-type", default=None,
                    help="task 6: force the impact-analysis type")
    ap.add_argument("--impact-max-depth", type=int, default=12,
                    help="task 6: reverse-closure depth cap (default 12)")
    ap.add_argument("--gen-dir", default=str(DEFAULT_GEN_DIR),
                    help=f"generated ANTLR Python3 sources (default: {DEFAULT_GEN_DIR})")
    ap.add_argument("--venv", default=str(DEFAULT_VENV_DIR),
                    help="venv to borrow antlr4-python3-runtime from")
    ap.add_argument("--no-venv-fallback", action="store_true",
                    help="do not add the venv's site-packages to sys.path")
    ap.add_argument("--ll-only", action="store_true",
                    help="disable SLL stage one (positive control)")
    ap.add_argument("--progress-every", type=int, default=0,
                    help="progress event interval in files (0 = auto)")
    return ap.parse_args(argv)


# ==========================================================================
# Main
# ==========================================================================

def main(argv: List[str]) -> int:
    args = parse_args(argv)
    started_at = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    proc_start = _process_start_epoch()
    startup_ms: Optional[int] = None
    if proc_start is not None:
        candidate = int(round((_T_WALL0 - proc_start) * 1000.0))
        if 0 <= candidate < 600000:
            startup_ms = candidate

    roots = split_roots(args.roots)
    gen_dir = Path(args.gen_dir).expanduser().resolve()
    venv_dir = Path(args.venv).expanduser()
    out_dir = Path(args.out_dir).expanduser()
    java_root = Path(args.gen_java).expanduser()

    demo("start", id=args.id,
         detail=f"lane={LANE} roots={len(roots)} enc={args.encoding}")
    load_sibling_lanes(Path(args.receipts_dir).expanduser())

    tok = Tokenizer(args.encoding)
    log(f"[ai] tiktoken {tok.version}, encoding {tok.name}")

    lexer_cls, parser_cls, antlr = prepare_environment(
        gen_dir, venv_dir, venv_fallback=not args.no_venv_fallback)
    parser_ready_ms = _ms(_T_PERF0)
    demo("metric", key="parserReadyMs", value=parser_ready_ms)

    files = discover(roots)
    if args.limit and args.limit > 0:
        files = files[:args.limit]
    total = len(files)
    if total == 0:
        demo("error", message=f"no *.rosetta files found under: {args.roots}")
        return 1
    log(f"[ai] {total} source files; parser ready in {parser_ready_ms} ms")

    # ---------------------------------------------------------------- parse
    interval = args.progress_every if args.progress_every > 0 else max(25, total // 250)
    index = ModelIndex()
    driver = ParseDriver(lexer_cls, parser_cls, antlr, ll_only=args.ll_only)
    extractor = RichExtractor(parser_cls, index)

    texts: Dict[str, str] = {}          # label -> exact text that was parsed
    label_path: Dict[str, Path] = {}
    stripped: Dict[str, str] = {}       # label -> same text, documentation removed
    comment_types = tuple(t for t in (getattr(parser_cls, "LINE_COMMENT", None),
                                      getattr(parser_cls, "BLOCK_COMMENT", None))
                          if t is not None)
    parse_ms = 0.0
    index_ms = 0.0
    io_ms = 0.0
    strip_ms = 0.0
    hard_failures = 0

    for n, path in enumerate(files, start=1):
        listener = _CountingErrorListener()
        label = label_of(path, roots)
        t_io = _time.perf_counter()
        try:
            text = path.read_text(encoding="utf-8-sig", errors="replace")
        except OSError as exc:
            log(f"WARN unreadable {label}: {exc}")
            index.parse_errors += 1
            index.error_files.append(label)
            continue
        io_ms += (_time.perf_counter() - t_io) * 1000.0
        texts[label] = text
        label_path[label] = path
        index.bytes_read += len(text)

        t0 = _time.perf_counter()
        try:
            tree = driver.parse(text, listener)
            hard_failures = 0
        except Exception as exc:
            parse_ms += (_time.perf_counter() - t0) * 1000.0
            log(f"WARN parse failed {label}: {type(exc).__name__}: {exc}")
            index.parse_errors += 1
            index.error_files.append(label)
            hard_failures += 1
            if hard_failures >= 5:
                demo("error", message=f"aborting after {hard_failures} consecutive "
                                      f"hard parse failures; last: "
                                      f"{type(exc).__name__}: {exc}")
                return 1
            continue
        parse_ms += (_time.perf_counter() - t0) * 1000.0

        if listener.count:
            index.parse_errors += listener.count
            index.error_files.append(f"{label} ({listener.count}: {listener.first})")
            log(f"WARN {listener.count} syntax error(s) in {label}: {listener.first}")

        t2 = _time.perf_counter()
        try:
            extractor.extract(tree, label)
        except Exception as exc:
            log(f"WARN index failed {label}: {type(exc).__name__}: {exc}")
            index.unknown_elements += 1
        index_ms += (_time.perf_counter() - t2) * 1000.0

        t3 = _time.perf_counter()
        try:
            stripped[label] = strip_spans(text, doc_spans(tree, comment_types))
        except Exception as exc:
            log(f"WARN doc-span attribution failed {label}: "
                f"{type(exc).__name__}: {exc}")
            stripped[label] = text          # attribute nothing rather than guess
        strip_ms += (_time.perf_counter() - t3) * 1000.0

        index.files += 1
        del tree
        if n % interval == 0 or n == total:
            demo("progress", n=n, of=total)

    index.sll_bailouts = driver.sll_bailouts
    demo("metric", key="declarations", value=len(index.decls))
    log(f"[ai] parsed {index.files} files -> {len(index.decls)} declarations "
        f"({int(parse_ms)} ms parse, {int(index_ms)} ms index)")

    # ------------------------------------------------------------- digest
    t_dig = _time.perf_counter()
    digest_text = render_digest(index.decls, DIGEST_HEADER)
    digest_ms = int(round((_time.perf_counter() - t_dig) * 1000.0))

    # ------------------------------------------------------------- task 1
    # Same population on both sides: every parsed file, every parsed
    # declaration - builtins included. Files are joined by a single newline
    # and carry NO added filename headers, which keeps the baseline as small
    # as it honestly can be.
    baseline_text = "\n".join(texts[k] for k in sorted(texts))
    m_base = tok.measure(baseline_text)
    m_digest = tok.measure(digest_text)

    # Attribution, NOT a representation anyone would paste: the same source
    # with descriptions, docReferences, synonyms/mappings and comments cut out,
    # so the reduction can be split into "dropped the prose" and "re-encoded
    # the structure" instead of leaving the first to be assumed.
    nodocs_text = "\n".join(stripped.get(k, texts[k]) for k in sorted(texts))
    m_nodocs = tok.measure(nodocs_text)
    del nodocs_text

    task1 = {
        "baselineSource": m_base.as_dict(files=len(texts)),
        "forkDigest": m_digest.as_dict(declarations=len(index.decls)),
        "ratio": ratio(m_base, m_digest),
        "tokensSaved": m_base.tokens - m_digest.tokens,
        "reductionPct": reduction_pct(m_base, m_digest),
        "attribution": {
            "sourceWithoutDocs": m_nodocs.as_dict(),
            "tokensThatWereDocumentation": m_base.tokens - m_nodocs.tokens,
            "documentationShareOfBaselinePct": (
                round(100.0 * (m_base.tokens - m_nodocs.tokens) / m_base.tokens, 2)
                if m_base.tokens else None),
            "tokensFromStructuralReEncoding": m_nodocs.tokens - m_digest.tokens,
            "ratioVsSourceWithoutDocs": ratio(m_nodocs, m_digest),
            "removed": "descriptions <\"...\">, [docReference ...], synonyms "
                       "and mappings, and //- and /* */ comments; located by "
                       "parse-tree context class and by comment TOKEN TYPE on "
                       "the hidden channel, never by matching text",
            "note": "an analytical artifact, not a usable representation - it "
                    "exists only to say how much of the task-1 reduction is "
                    "prose removal and how much is re-encoding",
        },
    }
    demo("metric", key="task1BaselineTokens", value=m_base.tokens)
    demo("metric", key="task1DigestTokens", value=m_digest.tokens)
    demo("metric", key="task1Ratio", value=task1["ratio"])
    del baseline_text

    # ------------------------------------------------------------- task 2
    t_cl = _time.perf_counter()
    resolver = TypeResolver(index.decls)
    candidates = [d for d in index.decls
                  if d.kind == KIND_TYPE and not d.builtin and d.attributes]
    sizes: Dict[int, int] = {}
    closures: Dict[int, List[Decl]] = {}
    for d in candidates:
        cl = closure_of(d, resolver)
        closures[id(d)] = cl
        sizes[id(d)] = len(cl)
    sweep_stats = resolver.stats()

    file_bytes = {lbl: len(t.encode("utf-8")) for lbl, t in texts.items()}
    _decl_bytes: Dict[int, int] = {}

    def decl_bytes(d: Decl) -> int:
        cached = _decl_bytes.get(id(d))
        if cached is None:
            cached = len("\n".join(render_decl(d)).encode("utf-8")) + 1
            _decl_bytes[id(d)] = cached
        return cached

    def band_byte_ratio(cl: Sequence[Decl]) -> Optional[float]:
        """Source-file bytes / digest bytes for one closure.

        ONE instrument for the chosen root and for every band candidate: both
        sides count the same way (whole files vs the sum of the rendered
        declaration lines, digest header and `@ns` lines excluded from both).
        Measuring the chosen root against the real slice file while measuring
        the candidates without the header would have made the chosen root look
        systematically more conservative than it is.
        """
        src_b = sum(file_bytes.get(lbl, 0) for lbl in {x.file for x in cl})
        dig_b = sum(decl_bytes(x) for x in cl)
        return (src_b / dig_b) if dig_b else None

    def band_key(d: Decl) -> Tuple[int, int, str]:
        ns = d.namespace
        pref = 0 if ns.startswith("drr.") else (1 if ns.startswith("cdm.") else 2)
        return (pref, -sizes[id(d)], d.qname)

    in_band = [d for d in candidates
               if args.min_closure <= sizes[id(d)] <= args.max_closure]
    forced = None
    if args.task2_type:
        want = args.task2_type
        forced = next((d for d in candidates
                       if d.qname == want or d.name == want), None)
        if forced is None:
            log(f"WARN --task2-type {want!r} matched no data type; "
                f"falling back to automatic selection")
    if forced is not None:
        root_decl = forced
    elif in_band:
        root_decl = sorted(in_band, key=band_key)[0]
    elif candidates:
        root_decl = max(candidates, key=lambda d: (sizes[id(d)], d.qname))
    else:
        root_decl = None

    task2: Dict[str, Any] = {}
    slice_text = ""
    if root_decl is None:
        log("WARN no data type with attributes was parsed; task 2 skipped")
        task2 = {"skipped": "no candidate data type in the parsed population"}
    else:
        # Recompute the chosen root's closure on reset counters, so the
        # resolver stats reported for it are ITS OWN and not the cumulative
        # total of the 2,000-odd candidate closures the band sweep walked.
        resolver.reset_stats()
        closure = closure_of(root_decl, resolver)
        root_stats = resolver.stats()
        assert len(closure) == sizes[id(root_decl)], "closure not reproducible"
        closure_ms = int(round((_time.perf_counter() - t_cl) * 1000.0))

        # (a) whole files holding any closure member
        closure_labels = sorted({d.file for d in closure})
        src_parts = [texts[l] for l in closure_labels if l in texts]
        m_files = tok.measure("\n".join(src_parts))

        # (c) the digest restricted to the closure
        slice_header = (
            DIGEST_HEADER
            + f"# SLICE  transitive closure of {root_decl.qname} over supertypes\n"
              f"#        and attribute types: {len(closure)} declarations in\n"
              f"#        {len(closure_labels)} source files.\n")
        slice_text = render_digest(closure, slice_header)
        m_slice = tok.measure(slice_text)

        # (b) the generated Java for the same declarations
        names_by_ns: Dict[str, List[str]] = {}
        for d in index.decls:
            if d.name:
                names_by_ns.setdefault(d.namespace, []).append(d.name)
        jt = JavaTree(java_root, names_by_ns)
        java_block: Optional[Measure] = None
        java_primary_block: Optional[Measure] = None
        java_files: List[Path] = []
        primary_files: List[Path] = []
        java_note = ""
        if not jt.available:
            java_note = f"generated Java tree not found at {java_root}"
            log(f"WARN {java_note}; task 2b skipped")
        else:
            seen_java: Set[str] = set()
            for d in sorted(closure, key=_sort_key):
                primary, companions = jt.files_for(d.namespace, d.name)
                for p in ([primary] if primary is not None else []) + companions:
                    key = str(p).lower()
                    if key in seen_java:
                        continue
                    seen_java.add(key)
                    java_files.append(p)
                    if p is primary:
                        primary_files.append(p)

            def read_all(paths: Sequence[Path]) -> str:
                chunks: List[str] = []
                for p in paths:
                    try:
                        chunks.append(p.read_text(encoding="utf-8",
                                                  errors="replace"))
                    except OSError as exc:
                        log(f"WARN unreadable {p}: {exc}")
                return "\n".join(chunks)

            java_block = tok.measure(read_all(java_files))
            # The narrower, more favourable-to-Java variant: ONLY each
            # declaration's own class, no meta/ or validation/ companions.
            # A careful reader would paste this, not the whole package - so
            # reporting only the wider set would overstate the baseline.
            java_primary_block = tok.measure(read_all(primary_files))

        by_kind: Dict[str, int] = {}
        for d in closure:
            by_kind[d.kind] = by_kind.get(d.kind, 0) + 1

        # Adversarial control: where does the CHOSEN root sit among every
        # in-band candidate? Byte ratios only (cheap), same two representations.
        band_stats: Dict[str, Any] = {"candidates": len(in_band)}
        chosen_ratio = band_byte_ratio(closure)
        if in_band and chosen_ratio is not None:
            band_ratios = sorted(r for r in (band_byte_ratio(closures[id(d)])
                                             for d in in_band) if r is not None)
            if band_ratios:
                mid = len(band_ratios) // 2
                below = sum(1 for r in band_ratios if r < chosen_ratio)
                band_stats.update({
                    "byteRatioMin": round(band_ratios[0], 2),
                    "byteRatioMedian": round(band_ratios[mid], 2),
                    "byteRatioMax": round(band_ratios[-1], 2),
                    "chosenByteRatio": round(chosen_ratio, 2),
                    "chosenPercentile": round(100.0 * below / len(band_ratios), 1),
                    "instrument": "whole source-file bytes / summed rendered "
                                  "declaration bytes; identical on both sides",
                })

        task2 = {
            "type": root_decl.qname,
            "typeFile": root_decl.file,
            "closureDecls": len(closure),
            "closureInBand": bool(args.min_closure <= len(closure) <= args.max_closure),
            "closureByKind": dict(sorted(by_kind.items())),
            "closureFiles": len(closure_labels),
            "selection": ("forced by --task2-type" if forced is not None else
                          "largest closure inside the band, drr. preferred, "
                          "then cdm., ties by qualified name"),
            "baselineSourceFiles": m_files.as_dict(files=len(closure_labels)),
            "forkDigestSlice": m_slice.as_dict(declarations=len(closure)),
            "closureMs": closure_ms,
            "bandStats": band_stats,
            "closureTypeRefs": root_stats,
            "candidateSweepTypeRefs": sweep_stats,
        }
        if java_block is not None:
            task2["baselineGeneratedJava"] = java_block.as_dict(
                files=len(java_files),
                declsWithNoPrimaryJava=jt.missing_primary,
                prefixOvermatchAvoided=jt.overmatch_avoided,
                scope="each declaration's own class plus its meta/ and "
                      "validation/ companions; metafields/ and functions/ "
                      "excluded")
            task2["ratioGeneratedJava"] = ratio(java_block, m_slice)
            if java_primary_block is not None:
                task2["baselineGeneratedJavaPrimaryOnly"] = \
                    java_primary_block.as_dict(
                        files=len(primary_files),
                        scope="ONLY each declaration's own class - the "
                              "narrowest defensible Java baseline")
                task2["ratioGeneratedJavaPrimaryOnly"] = ratio(
                    java_primary_block, m_slice)
        else:
            task2["baselineGeneratedJava"] = None
            task2["generatedJavaNote"] = java_note
        task2["ratioSourceFiles"] = ratio(m_files, m_slice)
        demo("metric", key="task2ClosureDecls", value=len(closure))
        demo("metric", key="task2SourceRatio", value=task2["ratioSourceFiles"])
        if java_block is not None:
            demo("metric", key="task2JavaFiles", value=len(java_files))

    # ------------------------------------------------------------- task 3
    t_q = _time.perf_counter()
    answers = results_block(index, args.target_type)
    q_ms = int(round((_time.perf_counter() - t_q) * 1000.0))
    answers_text = json.dumps(answers, indent=2, ensure_ascii=True) + "\n"
    m_answers = tok.measure(answers_text)
    task3 = {
        "baseline": m_base.as_dict(files=len(texts),
                                   note="identical to task 1 (a)"),
        "fork": m_answers.as_dict(),
        "ratio": ratio(m_base, m_answers),
        "answers": answers,
        "queryMs": q_ms,
    }
    demo("metric", key="task3Ratio", value=task3["ratio"])

    # ==================================================================
    # Tasks 4-6: what an agent can DO with the model once it is indexed.
    # ==================================================================
    by_name: Dict[str, List[Decl]] = {}
    for d in index.decls:
        if d.name:
            by_name.setdefault(d.name, []).append(d)

    def files_of(decls: Sequence[Decl]) -> Tuple[Measure, List[str]]:
        """Whole text of every file holding one of `decls` - the by-hand read."""
        labels = sorted({d.file for d in decls if d.file and d.file in texts})
        return tok.measure("\n".join(texts[l] for l in labels)), labels

    # ------------------------------------------------------------- task 4
    t_w = _time.perf_counter()
    resolver.reset_stats()
    walk_cands: List[Tuple[Decl, Decl, int]] = []
    for r in index.decls:
        if r.kind != KIND_REPORT or not getattr(r, "report_type", ""):
            continue
        rt = resolver.resolve(r.report_type, r.report_type.split(".")[-1], r)
        if rt is None or rt.kind not in (KIND_TYPE, KIND_CHOICE):
            continue
        _h, s = walk_type(rt, resolver, args.walk_max_depth, args.walk_max_hops)
        walk_cands.append((r, rt, s["hops"]))

    task4: Dict[str, Any] = {}
    walk_text = ""
    if not walk_cands:
        task4 = {"skipped": "no report declaration with a resolvable "
                            "`with type` in the parsed population"}
        log("WARN task 4 skipped: no walkable report in this population")
    else:
        forced4 = None
        if args.walk_report:
            forced4 = next((c for c in walk_cands
                            if args.walk_report in (c[0].name, c[0].qname,
                                                    c[1].name, c[1].qname)), None)
        ordered = sorted(walk_cands, key=lambda c: (c[2], c[1].qname))
        chosen4 = forced4 or ordered[len(ordered) // 2]
        rep, root_t, _ = chosen4
        resolver.reset_stats()
        hops, wstats = walk_type(root_t, resolver, args.walk_max_depth,
                                 args.walk_max_hops)
        walk_stats4 = resolver.stats()
        seen_lbl = {rep.file, root_t.file}
        for h in hops:
            if h.file:
                seen_lbl.add(h.file)
        walk_labels = sorted(l for l in seen_lbl if l in texts)
        walk_text = render_walk(root_t, rep, hops, wstats, args.walk_max_depth,
                                len(walk_labels))
        m_walk = tok.measure(walk_text)
        m_walk_src = tok.measure("\n".join(texts[l] for l in walk_labels))
        hop_counts = [c[2] for c in walk_cands]
        task4 = {
            "report": (rep.header_text or rep.qname)[:160],
            "reportFile": rep.file,
            "reportFrom": rep.report_from,
            "rootType": root_t.qname,
            "hops": wstats["hops"],
            "maxDepth": wstats["maxDepth"],
            "depthCap": args.walk_max_depth,
            "declarationsTouched": wstats["declsTouched"],
            "filesCrossed": len(walk_labels),
            "leaves": wstats["leaves"],
            "cyclesStopped": wstats["cycles"],
            "depthCapped": wstats["depthCapped"],
            "unresolvedHops": wstats["unresolved"],
            "hopCapHit": bool(wstats["hopCapped"]),
            "baselineSourceFiles": m_walk_src.as_dict(files=len(walk_labels)),
            "forkWalk": m_walk.as_dict(hops=wstats["hops"]),
            "ratio": ratio(m_walk_src, m_walk),
            "selection": ("forced by --walk-report" if forced4 is not None else
                          "MEDIAN hop count among every report whose `with "
                          "type` resolves - deliberately not the largest"),
            "candidates": len(walk_cands),
            "hopsMin": min(hop_counts), "hopsMax": max(hop_counts),
            "selectionPercentile": pct_rank([float(h) for h in hop_counts],
                                            float(wstats["hops"])),
            "typeRefs": walk_stats4,
            "walkMs": int(round((_time.perf_counter() - t_w) * 1000.0)),
        }
        demo("metric", key="task4Hops", value=wstats["hops"])
        demo("metric", key="task4FilesCrossed", value=len(walk_labels))
        demo("metric", key="task4Ratio", value=task4["ratio"])

    # ------------------------------------------------------------- task 5
    t_f = _time.perf_counter()
    resolver.reset_stats()
    fn_cands = [d for d in index.decls
                if d.kind == KIND_FUNC and not d.builtin and d.callees]
    task5: Dict[str, Any] = {}
    breakdown_text = ""
    if not fn_cands:
        task5 = {"skipped": "no non-builtin function with a call in its body"}
        log("WARN task 5 skipped: no function with callees in this population")
    else:
        def fscore(d: Decl) -> int:
            return (len(set(d.callees)) + len(set(d.features_read))
                    + len(d.conditions))
        forced5 = None
        if args.task5_func:
            forced5 = next((d for d in fn_cands
                            if args.task5_func in (d.name, d.qname)), None)
        # "Non-trivial body" is part of the brief, so the pick comes from the
        # TOP QUARTILE by body complexity - and then the MEDIAN of that
        # quartile, not its maximum. The resulting overall percentile is
        # recorded so the narrowing is visible rather than implied.
        ordered5 = sorted(fn_cands, key=lambda d: (fscore(d), d.qname))
        band5 = ordered5[(len(ordered5) * 3) // 4:] or ordered5
        fn = forced5 or band5[len(band5) // 2]
        detail, touched5 = function_breakdown(fn, resolver, by_name)
        fn_stats = resolver.stats()
        breakdown_text = render_breakdown(detail)
        m_bd = tok.measure(breakdown_text)
        m_bd_src, bd_labels = files_of(touched5)
        scores = [float(fscore(d)) for d in fn_cands]

        # Corpus-wide callee-resolution census. One function resolving all of
        # its callees says nothing about the method; this says how often it
        # works, and how often it works only by SIMPLE NAME with more than one
        # candidate - which is a match, not a certainty.
        how_counts: Dict[str, int] = {}
        for d in index.decls:
            for nm in dict.fromkeys(getattr(d, "callees", ())):
                simple = nm.split(".")[-1]
                cands = [c for c in by_name.get(simple, ())
                         if c.kind in (KIND_FUNC, KIND_RULE,
                                       KIND_LIBRARY_FUNCTION)]
                _hit, how = pick_by_name(cands, nm, d)
                how_counts[how] = how_counts.get(how, 0) + 1
        c_unres = how_counts.get("none", 0)
        c_res = sum(v for k, v in how_counts.items() if k != "none")
        c_amb = how_counts.get("first", 0)
        task5 = {
            "function": fn.qname,
            "functionFile": fn.file,
            "counts": detail["counts"],
            "baselineSourceFiles": m_bd_src.as_dict(files=len(bd_labels)),
            "forkBreakdown": m_bd.as_dict(),
            "ratio": ratio(m_bd_src, m_bd),
            "selection": ("forced by --task5-func" if forced5 is not None else
                          "MEDIAN of the TOP QUARTILE by body-complexity "
                          "score (distinct callees + distinct attribute names "
                          "read + conditions) among every non-builtin "
                          "function that calls something - the brief asked "
                          "for a non-trivial body, so the band is narrowed "
                          "and the overall percentile is recorded"),
            "candidates": len(fn_cands),
            "scoreChosen": fscore(fn),
            "scoreMin": int(min(scores)), "scoreMax": int(max(scores)),
            "selectionPercentile": pct_rank(scores, float(fscore(fn))),
            "typeRefs": fn_stats,
            "extraction": "callees resolved by NAME against the declaration "
                          "index; attributes read are feature-call NAMES with "
                          "NO receiver type - this lane does no type inference",
            "corpusCalleeResolution": {
                "distinctCalleeNames": c_res + c_unres,
                "resolved": c_res,
                "unresolved": c_unres,
                "guessedFirstOfSeveral": c_amb,
                "byMatchKind": dict(sorted(how_counts.items())),
                "note": "counted over every declaration in the corpus, not "
                        "just the chosen function. `exact` is a qualified-name "
                        "match, `unique` the only candidate, `sameNamespace` / "
                        "`sameFile` are preference steps, and `first` is a "
                        "GUESS among several same-named candidates - those "
                        "edges are marked `?` in the impact report",
            },
            "breakdownMs": int(round((_time.perf_counter() - t_f) * 1000.0)),
        }
        demo("metric", key="task5Callees", value=detail["counts"]["distinctCallees"])
        demo("metric", key="task5Ratio", value=task5["ratio"])

    # ------------------------------------------------------------- task 6
    t_i = _time.perf_counter()
    resolver.reset_stats()
    edge_stats: Dict[str, int] = {}
    rev = build_reverse_index(index.decls, resolver, by_name, edge_stats)
    rev_stats = resolver.stats()
    rev_stats.update(edge_stats)
    feature_counts: Dict[str, int] = {}
    for d in index.decls:
        for f in d.features_read:
            feature_counts[f] = feature_counts.get(f, 0) + 1

    imp_cands = [d for d in index.decls
                 if d.kind == KIND_TYPE and not d.builtin and d.attributes]
    task6: Dict[str, Any] = {}
    impact_text = ""
    if not imp_cands:
        task6 = {"skipped": "no non-builtin data type in the parsed population"}
        log("WARN task 6 skipped: no candidate type")
    else:
        def indeg(d: Decl) -> int:
            return len(rev.get(id(d), ()))
        forced6 = None
        if args.task6_type:
            forced6 = next((d for d in imp_cands
                            if args.task6_type in (d.name, d.qname)), None)
        target = forced6 or max(imp_cands, key=lambda d: (indeg(d), d.qname))
        attr = max(target.attributes,
                   key=lambda a: (feature_counts.get(a.name, 0), a.name))
        order, istats = impact_of(target, rev, args.impact_max_depth)
        impact_text = render_impact(target, attr.name,
                                    feature_counts.get(attr.name, 0),
                                    order, istats)
        m_imp = tok.measure(impact_text)
        degrees = [float(indeg(d)) for d in imp_cands]
        task6 = {
            "type": target.qname,
            "typeFile": target.file,
            "attribute": attr.name,
            "attributeType": attr.type_qname or attr.type_simple,
            "attributeNameMentionsInExpressions": feature_counts.get(attr.name, 0),
            "directReferences": istats["directReferences"],
            "transitiveReferences": istats["transitiveReferences"],
            "depthReached": istats["depthReached"],
            "depthCap": istats["depthCap"],
            "filesAffected": istats["filesAffected"],
            "shareOfAllDeclarationsPct": round(
                100.0 * istats["transitiveReferences"] / max(1, len(index.decls)), 1),
            "byKind": istats["byKind"],
            "byReferenceKind": istats["byReferenceKind"],
            "baselineWholeCorpus": m_base.as_dict(
                files=len(texts),
                note="the whole corpus - without an index an agent must read "
                     "everything to be sure it has missed no reference"),
            "forkImpactReport": m_imp.as_dict(),
            "ratio": ratio(m_base, m_imp),
            "selection": ("forced by --task6-type" if forced6 is not None else
                          "HIGHEST direct in-degree among non-builtin data "
                          "types - the least favourable choice for the ratio, "
                          "because it produces the largest answer"),
            "candidates": len(imp_cands),
            "inDegreeChosen": indeg(target),
            "inDegreeMax": int(max(degrees)),
            "selectionPercentile": pct_rank(degrees, float(indeg(target))),
            "granularity": "type edges (attribute / extends / choiceOption / "
                           "typeAliasTarget / ruleFrom / reportFrom / "
                           "reportType) are grammar-positional; `calls` and "
                           "`referencesRule` are NAME-resolved from "
                           "expressions; a feature call's receiver type is "
                           "not resolved, so `x -> y` yields no edge",
            "edgeStats": edge_stats,
            "typeRefs": rev_stats,
            "impactMs": int(round((_time.perf_counter() - t_i) * 1000.0)),
        }
        demo("metric", key="task6DirectRefs", value=istats["directReferences"])
        demo("metric", key="task6TransitiveRefs",
             value=istats["transitiveReferences"])
        demo("metric", key="task6Ratio", value=task6["ratio"])

    # ------------------------------------------------------------- outputs
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / DIGEST_FILE).write_text(digest_text, encoding="utf-8", newline="\n")
    log(f"[ai] digest written to {out_dir / DIGEST_FILE}")
    if slice_text:
        (out_dir / SLICE_FILE).write_text(slice_text, encoding="utf-8", newline="\n")
        log(f"[ai] closure slice written to {out_dir / SLICE_FILE}")
    (out_dir / ANSWERS_FILE).write_text(answers_text, encoding="utf-8", newline="\n")
    for name, body in ((WALK_FILE, walk_text), (BREAKDOWN_FILE, breakdown_text),
                       (IMPACT_FILE, impact_text)):
        if body:
            (out_dir / name).write_text(body, encoding="utf-8", newline="\n")
            log(f"[ai] {name} written to {out_dir / name}")

    # ------------------------------------------------------------- metrics
    walk = aux_counts(index, args.target_type)
    now_epoch = time.time()
    if proc_start is not None and startup_ms is not None:
        wall_ms = int(round((now_epoch - proc_start) * 1000.0))
    else:
        wall_ms = int(round((now_epoch - _T_WALL0) * 1000.0))

    aux: Dict[str, Any] = {
        "tiktokenVersion": tok.version,
        "tokenizeCalls": tok.calls,
        "charsTokenized": tok.chars,
        "python": platform.python_version(),
        "predictionMode": "LL" if args.ll_only else "SLL-then-LL",
        "sllBailouts": index.sll_bailouts,
        "parseErrors": index.parse_errors,
        "errorFiles": index.error_files[:20],
        "unknownElements": index.unknown_elements,
        "parserReadyMs": parser_ready_ms,
        "ioMs": int(round(io_ms)),
        "attributionWalkMs": int(round(strip_ms)),
        "attributesWithoutCardinality": extractor.attrs_without_card,
        "runeAtAnnotations": extractor.rune_annotation_count,
        "choiceDeclarations": len(index.of_kind(KIND_CHOICE)),
        "declsByKind": walk["declsByKind"],
        "builtinDeclarations": walk["builtinDeclarations"],
        "attributes": walk["attributes"],
        "namespaces": walk["namespaces"],
        "siblingLanes": _SIBLING_LANES or None,
        "digestPath": str((out_dir / DIGEST_FILE).as_posix()),
        "closureSlicePath": (str((out_dir / SLICE_FILE).as_posix())
                             if slice_text else None),
        "limit": args.limit or None,
    }

    builtin_files = len({d.file for d in index.decls if d.builtin})
    metrics: Dict[str, Any] = {
        "lane": LANE,
        "encoding": tok.name,
        "files": index.files - builtin_files,
        "filesParsed": index.files,
        "declarations": len(index.decls),
        "parseMs": int(round(parse_ms)),
        "indexMs": int(round(index_ms)),
        "digestMs": digest_ms,
        "tokenizeMs": int(round(tok.ms)),
        "wallMs": wall_ms,
        "task1": task1,
        "task2": task2,
        "task3": task3,
        "task4": task4,
        "task5": task5,
        "task6": task6,
    }
    if startup_ms is not None:
        metrics["startupMs"] = startup_ms
    peak = peak_rss_mb()
    if peak is not None:
        metrics["peakRssMb"] = peak
    metrics["aux"] = aux

    duration_ms = _ms(_T_PERF0)

    # ------------------------------------------------------------- summary
    log("")
    log("=== LLM context cost by representation (no JVM) =====================")
    log(f"  encoding          : {tok.name} (tiktoken {tok.version}) - EXACT counts")
    log(f"  corpus            : {index.files} files, {len(index.decls)} declarations")
    log("")
    log("  TASK 1  whole-model structural context")
    log(f"    (a) source, all {len(texts)} files : "
        f"{m_base.tokens:>10,} tok  {m_base.chars:>10,} ch  {m_base.bytes:>10,} B")
    log(f"    (b) fork structural digest    : "
        f"{m_digest.tokens:>10,} tok  {m_digest.chars:>10,} ch  {m_digest.bytes:>10,} B")
    log(f"        ratio a/b = {task1['ratio']}x   ({task1['reductionPct']}% fewer tokens)")
    _att = task1["attribution"]
    log(f"        attribution: {_att['tokensThatWereDocumentation']:,} tok "
        f"({_att['documentationShareOfBaselinePct']}%) were descriptions, "
        f"synonyms and comments;")
    log(f"                     {_att['tokensFromStructuralReEncoding']:,} tok "
        f"came from re-encoding the structure "
        f"(source-without-docs / digest = {_att['ratioVsSourceWithoutDocs']}x)")
    if root_decl is not None:
        log("")
        log(f"  TASK 2  {task2['type']}  closure = {task2['closureDecls']} "
            f"declarations in {task2['closureFiles']} files")
        log(f"    (a) whole source files        : "
            f"{task2['baselineSourceFiles']['tokens']:>10,} tok")
        gj = task2.get("baselineGeneratedJava")
        gp = task2.get("baselineGeneratedJavaPrimaryOnly")
        if gj:
            log(f"    (b) generated Java ({gj['files']:>4} files): "
                f"{gj['tokens']:>10,} tok")
            if gp:
                log(f"        own classes only ({gp['files']:>4} files): "
                    f"{gp['tokens']:>10,} tok  -> ratio b'/c = "
                    f"{task2['ratioGeneratedJavaPrimaryOnly']}x")
        else:
            log(f"    (b) generated Java            :  skipped "
                f"({task2.get('generatedJavaNote', 'unavailable')})")
        log(f"    (c) fork digest slice         : "
            f"{task2['forkDigestSlice']['tokens']:>10,} tok")
        log(f"        ratio a/c = {task2['ratioSourceFiles']}x"
            + (f"   ratio b/c = {task2['ratioGeneratedJava']}x" if gj else ""))
    log("")
    log("  TASK 3  answering Q1-Q4")
    log(f"    (a) read the corpus yourself  : {m_base.tokens:>10,} tok")
    log(f"    (b) ask the index             : {m_answers.tokens:>10,} tok")
    log(f"        ratio a/b = {task3['ratio']}x  (the index parsed the corpus "
        f"once, in {metrics['parseMs']} ms, to be able to answer)")
    if "skipped" not in task4:
        log("")
        log(f"  TASK 4  walk {task4['rootType']}")
        log(f"    via report in {task4['reportFile']}, from {task4['reportFrom']}")
        log(f"    {task4['hops']} hops, depth {task4['maxDepth']}/"
            f"{task4['depthCap']}, {task4['declarationsTouched']} declarations, "
            f"{task4['filesCrossed']} files crossed")
        log(f"    (a) whole source files ({task4['baselineSourceFiles']['files']:>3}): "
            f"{task4['baselineSourceFiles']['tokens']:>10,} tok")
        log(f"    (b) the walk result           : "
            f"{task4['forkWalk']['tokens']:>10,} tok"
            f"   ratio a/b = {task4['ratio']}x")
    if "skipped" not in task5:
        c5 = task5["counts"]
        log("")
        log(f"  TASK 5  break down {task5['function']}")
        log(f"    {c5['distinctCallees']} distinct callees "
            f"({c5['calleesResolved']} resolved, {c5['calleesUnresolved']} not), "
            f"{c5['distinctAttributesRead']} attribute names read, "
            f"{c5['conditions']} conditions, "
            f"{c5['distinctDeclarationsTouched']} declarations touched")
        log(f"    (a) whole source files ({task5['baselineSourceFiles']['files']:>3}): "
            f"{task5['baselineSourceFiles']['tokens']:>10,} tok")
        log(f"    (b) the breakdown             : "
            f"{task5['forkBreakdown']['tokens']:>10,} tok"
            f"   ratio a/b = {task5['ratio']}x")
    if "skipped" not in task6:
        log("")
        log(f"  TASK 6  impact of changing {task6['type']} -> {task6['attribute']}")
        log(f"    {task6['directReferences']} direct, "
            f"{task6['transitiveReferences']} transitive references, depth "
            f"{task6['depthReached']}/{task6['depthCap']}, "
            f"{task6['filesAffected']} files affected")
        log(f"    (a) the whole corpus          : "
            f"{task6['baselineWholeCorpus']['tokens']:>10,} tok")
        log(f"    (b) the impact report         : "
            f"{task6['forkImpactReport']['tokens']:>10,} tok"
            f"   ratio a/b = {task6['ratio']}x")
    log("")
    log(f"  parse {metrics['parseMs']} ms | index {metrics['indexMs']} ms | "
        f"digest {digest_ms} ms | tokenize {metrics['tokenizeMs']} ms | "
        f"wall {wall_ms} ms")
    if peak is not None:
        log(f"  peak RSS          : {peak} MiB")
    log("====================================================================")

    if args.receipt:
        write_receipt(Path(args.receipt), args, started_at, duration_ms, metrics)
        log(f"[ai] receipt written to {args.receipt}")

    demo("done", metrics=metrics)
    return 0


_SIBLING_LANES: Dict[str, Dict[str, Any]] = {}


def load_sibling_lanes(receipts_dir: Path) -> None:
    """Read the plus-jvm / legacy-emf receipts so their figures are QUOTED.

    Every cross-lane number in the notes comes from here. If a receipt is not
    on disk the comparison sentence says so and quotes nothing - a figure this
    run did not read is a figure it does not state.
    """
    for key, fname in (("plus-jvm", "analytics.plus-jvm.json"),
                       ("legacy-emf", "analytics.legacy-emf.json")):
        p = receipts_dir / fname
        if not p.is_file():
            continue
        try:
            m = json.loads(p.read_text(encoding="utf-8")).get("metrics", {})
        except Exception:
            continue
        _SIBLING_LANES[key] = {
            "startupMs": m.get("startupMs"), "parseMs": m.get("parseMs"),
            "resolveMs": m.get("resolveMs"), "wallMs": m.get("wallMs"),
            "peakRssMb": m.get("peakRssMb"), "files": m.get("files"),
            "source": fname,
        }


def _sibling_lane_sentence(metrics: Dict[str, Any]) -> str:
    pj = _SIBLING_LANES.get("plus-jvm")
    le = _SIBLING_LANES.get("legacy-emf")
    if not pj and not le:
        return ("The sibling analytics receipts were not present in this "
                "workspace, so no cross-lane figure is quoted here - read "
                "demo/receipts/analytics.plus-jvm.json and "
                "analytics.legacy-emf.json for the comparison.")
    bits: List[str] = []
    mine = metrics.get("parseMs") or 0
    if pj and pj.get("parseMs"):
        times = round(mine / pj["parseMs"], 1) if pj["parseMs"] else None
        bits.append(f"The plus-jvm lane's receipt ({pj['source']}) records "
                    f"{pj['parseMs']} ms to parse the same {pj.get('files')} "
                    f"files - {times}x quicker than this run - for "
                    f"{pj.get('wallMs')} ms of wall clock.")
    if le:
        bits.append(f"The legacy-emf lane ({le['source']}) parses in "
                    f"{le.get('parseMs')} ms, but then pays "
                    f"{le.get('resolveMs')} ms in EcoreUtil.resolveAll and a "
                    f"{le.get('peakRssMb')} MiB peak before the first question "
                    f"can be asked at all, for {le.get('wallMs')} ms of wall "
                    f"clock.")
    bits.append(f"This lane's peak was {metrics.get('peakRssMb')} MiB.")
    return " ".join(bits)


def _capability_notes(metrics: Dict[str, Any]) -> str:
    """The point of tasks 4-6, stated without spin."""
    t4 = metrics.get("task4") or {}
    t5 = metrics.get("task5") or {}
    t6 = metrics.get("task6") or {}
    c5 = t5.get("counts") or {}
    ccr = t5.get("corpusCalleeResolution") or {}
    out = [
        "THE CAPABILITY POINT (tasks 4-6). Tasks 1-3 measure how big the model "
        "is in different representations. Tasks 4-6 measure what an agent can "
        "DO with it. Walking a report down to its leaf fields, breaking a "
        "function into its inputs, callees and conditions, and answering "
        "'what breaks if I change this type' are not text operations - they "
        "are graph operations, and an agent can only perform them "
        "programmatically if something has already parsed the model and built "
        "an index. Under the legacy toolchain that something is a JVM with "
        "Xtext and EMF, and the model is not queryable until a global "
        "EcoreUtil.resolveAll has finished. This lane did all of it in bare "
        "CPython: no JVM, no Xtext, no EMF, one pure-Python dependency, "
        "because the fork's front end is a plain ANTLR4 grammar with no "
        "embedded actions and no semantic predicates, so the SAME grammar "
        "files that drive the Java engine generate a Python parser.",
        "THE COST, WITHOUT SPIN: the pure-Python ANTLR runtime parsed the "
        f"corpus in {metrics.get('parseMs')} ms. The JVM parses the same "
        "corpus far faster. " + _sibling_lane_sentence(metrics)
        + " The parse is paid ONCE per process; every walk, breakdown and "
        f"impact query below then ran in milliseconds against the in-memory "
        f"index (task 4 {(metrics.get('task4') or {}).get('walkMs')} ms, "
        f"task 5 {(metrics.get('task5') or {}).get('breakdownMs')} ms, "
        f"task 6 {(metrics.get('task6') or {}).get('impactMs')} ms). The "
        "claim is about REACHABILITY - that the model is programmatically "
        "walkable with no JVM in the chain - not about the Python parser "
        "being fast.",
    ]
    if t4 and "skipped" not in t4:
        out.append(
            f"TASK 4 walked {t4.get('rootType')}, reached from the report in "
            f"{t4.get('reportFile')} ({t4.get('hops')} hops, depth "
            f"{t4.get('maxDepth')} against a cap of {t4.get('depthCap')}, "
            f"{t4.get('declarationsTouched')} declarations, "
            f"{t4.get('filesCrossed')} files crossed, {t4.get('leaves')} "
            f"leaves, {t4.get('cyclesStopped')} branches stopped on a cycle, "
            f"{t4.get('depthCapped')} stopped at the depth cap). The cycle "
            "guard is not decoration: the CDM type graph is cyclic and an "
            "unguarded walk does not terminate, so the depth cap is a real "
            "bound on the answer and the trace records every place it bit. "
            "The report was chosen as the MEDIAN hop count among "
            f"{t4.get('candidates')} candidate reports "
            f"({t4.get('selectionPercentile')}th percentile, range "
            f"{t4.get('hopsMin')}-{t4.get('hopsMax')}), not the largest.")
    if t5 and "skipped" not in t5:
        out.append(
            f"TASK 5 broke down {t5.get('function')}: {c5.get('inputs')} "
            f"inputs, {c5.get('outputs')} output, "
            f"{c5.get('distinctCallees')} distinct callees over "
            f"{c5.get('calleeSites')} call sites, "
            f"{c5.get('distinctAttributesRead')} distinct attribute names read "
            f"over {c5.get('attributeReadSites')} sites, "
            f"{c5.get('conditions')} conditions, {c5.get('aliases')} aliases, "
            f"{c5.get('operations')} assignments. WHAT WAS AND WAS NOT "
            "EXTRACTED, precisely: the parser records a call as a "
            "`qualifiedName` inside a #FunctionCallExpr, not as a binding, so "
            "callees are matched by NAME against the declaration index - for "
            f"this function {c5.get('calleesResolved')} resolved and "
            f"{c5.get('calleesUnresolved')} did not resolve to any function, "
            "rule or library function, and unresolved names are listed in the "
            "breakdown rather than dropped. Across the WHOLE corpus that "
            f"method resolves {ccr.get('resolved')} of "
            f"{ccr.get('distinctCalleeNames')} distinct callee names with "
            f"{ccr.get('unresolved')} unresolved - but the match kinds are "
            f"{json.dumps(ccr.get('byMatchKind'))}, and the "
            f"{ccr.get('guessedFirstOfSeveral')} `first` ones are GUESSES "
            "among several same-named candidates. A clean 'zero unresolved' "
            "must not be read as a clean 'zero wrong'; guessed edges are "
            "marked `?` in the task-6 report rather than presented as facts. "
            "Attributes read are the "
            "validID of each #FeatureCallExpr / #DeepFeatureCallExpr: a NAME "
            "with NO receiver type attached. This lane performs no type "
            "inference, so it cannot say WHICH type an attribute was read "
            "from, only that a feature of that name was read. Conditions, "
            "aliases and set/add operations are read from their own grammar "
            "productions and are exact. The expression's shape, its operators "
            "and its logic are NOT extracted at all. The function was chosen "
            "as the MEDIAN body-complexity score among "
            f"{t5.get('candidates')} functions that call something "
            f"({t5.get('selectionPercentile')}th percentile, score "
            f"{t5.get('scoreChosen')} in a range of {t5.get('scoreMin')}-"
            f"{t5.get('scoreMax')}).")
    if t6 and "skipped" not in t6:
        out.append(
            f"TASK 6 asked what changing `{t6.get('attribute')}` on "
            f"{t6.get('type')} affects: {t6.get('directReferences')} direct "
            f"and {t6.get('transitiveReferences')} transitive references "
            f"across {t6.get('filesAffected')} files, depth "
            f"{t6.get('depthReached')} against a cap of {t6.get('depthCap')}. "
            f"That answer covers {t6.get('shareOfAllDeclarationsPct')}% of "
            "every declaration in the corpus, which is why the ratio here is "
            "modest and should be: a genuinely central type reaches most of "
            "the model, and saying so is the answer. "
            "The baseline is the WHOLE corpus and that is the honest one: "
            "without an index an agent cannot know it has missed a reference "
            "without reading everything. GRANULARITY, stated plainly: the "
            "edges are TYPE-level - an edge means a declaration names this "
            "type in an attribute, an extends, a choice option, a type-alias "
            "target, a rule's `from`, or a report's `from`/`with type`. "
            "Expression-level reads are NOT edges, because the parser sees a "
            "feature call as a name with no receiver type; separately, "
            f"{t6.get('attributeNameMentionsInExpressions')} feature-call "
            f"sites in the corpus use the NAME `{t6.get('attribute')}`, and "
            "that figure is a name count, not a resolved reference. The type "
            "was chosen by HIGHEST direct in-degree among "
            f"{t6.get('candidates')} data types "
            f"({t6.get('selectionPercentile')}th percentile) - the least "
            "favourable choice available, because the most-referenced type "
            "produces the largest answer and therefore the smallest ratio.")
    return " ".join(out) + " "


def interpreter_label() -> str:
    """The interpreter that is running, as the receipt's command names it, without an absolute
    local path: relative to the working directory when it lies at most two levels above it (the
    documented `../work/venv/...` form from demo/nojvm/), otherwise its file name alone."""
    exe = sys.executable or "python"
    try:
        rel = os.path.relpath(exe)
    except ValueError:  # another drive on Windows
        rel = ""
    if not rel or rel.startswith(os.path.join("..", "..", "..")):
        rel = os.path.basename(exe)
    return f'"{rel}"' if " " in rel else rel


def write_receipt(path: Path, args: argparse.Namespace, started_at: str,
                  duration_ms: int, metrics: Dict[str, Any]) -> None:
    if args.command:
        command = args.command
    else:
        command = interpreter_label() + " " + " ".join(
            (f'"{a}"' if (" " in a or ";" in a) else a) for a in sys.argv)

    t1 = metrics["task1"]
    t2 = metrics["task2"]
    t3 = metrics["task3"]
    java = t2.get("baselineGeneratedJava") if isinstance(t2, dict) else None
    bs = (t2.get("bandStats") or {}) if isinstance(t2, dict) else {}
    ctr = (t2.get("closureTypeRefs") or {}) if isinstance(t2, dict) else {}

    notes = (
        "No JVM anywhere in this run: the corpus is parsed by the ANTLR4 "
        "Python3 parser generated from the fork's own grammars, and every "
        f"token count is an EXACT tiktoken count under the {metrics['encoding']} "
        f"encoding (tiktoken {metrics['aux']['tiktokenVersion']}), never an "
        "estimate. Characters and UTF-8 bytes are reported beside every token "
        "count so no claim rests on the tokenizer alone. "
        "WHAT THE DIGEST KEEPS: namespace, declaration kind and name, "
        "supertype, every attribute's name / declared type (as written) / "
        "cardinality, the [annotation qualifier] pairs on declarations and on "
        "attributes, enum values, choice options, the function input/output "
        "split, the reporting-rule input type, and the header line of every "
        "other declaration kind. "
        "WHAT THE DIGEST DROPS, deliberately and exhaustively: descriptions "
        "(<\"...\">), [docReference ...] blocks, synonyms and mappings "
        "(including the whole body of a synonym source and of a rule source), "
        "comments, and EVERY expression body - conditions, aliases, "
        "operations, post-conditions and reporting-rule logic. It is "
        "information-preserving for STRUCTURAL questions (what exists, what it "
        "is called, what it extends, what it holds, with what cardinality and "
        "what metadata) and it is NOT a substitute for the source when the "
        "question is about behaviour, mappings or documentation. "
        "TASK 1 compares the same population on both sides - every parsed file "
        "against every parsed declaration, builtins included. The baseline is "
        "the files concatenated with a single newline and NO added filename "
        "headers, which is the smallest honest form of 'paste the model'; "
        "adding per-file headers would raise the baseline and flatter the "
        f"ratio. Result: {t1['baselineSource']['tokens']} -> "
        f"{t1['forkDigest']['tokens']} tokens = {t1['ratio']}x. "
        "The obvious objection to that ratio - 'you just deleted the "
        "documentation' - is answered by measurement rather than by assertion: "
        "stripping descriptions, docReferences, synonyms/mappings and comments "
        "out of the SOURCE (located by parse-tree context class and by comment "
        "token type, never by matching text) leaves "
        f"{t1['attribution']['sourceWithoutDocs']['tokens']} tokens, so "
        f"{t1['attribution']['tokensThatWereDocumentation']} tokens "
        f"({t1['attribution']['documentationShareOfBaselinePct']}% of the "
        "baseline) were prose and mapping, and the remaining "
        f"{t1['attribution']['tokensFromStructuralReEncoding']} tokens of the "
        "saving come from re-encoding what is left. Structure against "
        "structure, the digest is "
        f"{t1['attribution']['ratioVsSourceWithoutDocs']}x smaller than the "
        "de-prosed source. That stripped text is an analytical artifact, not a "
        "representation anyone would paste. "
        "TASK 2 fixes one type and takes the transitive closure over "
        "supertypes and attribute types, stopping at the builtin "
        "(com.rosetta*) namespaces, which are ungenerated and need no "
        f"context. Root {t2.get('type')}, closure {t2.get('closureDecls')} "
        f"declarations across {t2.get('closureFiles')} source files. "
        "(a) is the WHOLE text of each of those files, because a reader "
        "without a toolchain does not know which lines matter; (c) is the same "
        "digest restricted to the closure. "
        "The root was not hand-picked: it is the largest closure inside the "
        f"band, drr. preferred. {bs.get('candidates')} data types have an "
        "in-band closure, and on one shared byte instrument (whole source "
        "files over summed rendered declarations, applied identically to every "
        "candidate and to the chosen root) the chosen root scores "
        f"{bs.get('chosenByteRatio')} against a band median of "
        f"{bs.get('byteRatioMedian')} and a maximum of "
        f"{bs.get('byteRatioMax')} - it sits at the "
        f"{bs.get('chosenPercentile')}th percentile, so the selection rule is "
        "not flattering the result. "
        "Type references are resolved exactly as queries.py resolves `extends` "
        "(qualified name, then unique simple name, then same-namespace, then "
        f"same-file, then first). Over the chosen root's own closure that was "
        f"{ctr.get('resolutions')} resolutions with {ctr.get('unresolved')} "
        f"unresolved and {ctr.get('ambiguous')} falling back to the first "
        "candidate; this lane does not read `import` statements, so an "
        "ambiguous simple name is a real (counted) approximation. "
        + (f"(b) is the generated Java for the same declarations, "
           f"{java['files']} files read from the mode-1 tree: the type's own "
           f"class plus its meta/ and validation/ companions, attributed by "
           f"LONGEST declaration-name prefix so a file like "
           f"PartyRoleValidator.java is charged to PartyRole and not to Party "
           f"({java['prefixOvermatchAvoided']} files re-attributed that way). "
           f"metafields/ and functions/ companions are NOT included, which "
           f"makes that baseline smaller than a full paste would be. Because "
           f"'nobody would paste the validators' is a fair objection, the "
           f"narrowest defensible Java baseline is reported beside it: each "
           f"declaration's OWN class and nothing else, "
           f"{(t2.get('baselineGeneratedJavaPrimaryOnly') or {}).get('files')} "
           f"files / "
           f"{(t2.get('baselineGeneratedJavaPrimaryOnly') or {}).get('tokens')} "
           f"tokens = {t2.get('ratioGeneratedJavaPrimaryOnly')}x against the "
           f"digest slice. Quote that number, not the wider one, if the "
           f"question is only about shape. "
           if java else
           "(b) was skipped: " + str(t2.get("generatedJavaNote", "unavailable")) + ". ")
        + "TASK 3 is the one large ratio and it must be read for what it is: "
        "it compares READING THE CORPUS YOURSELF against ASKING AN INDEX THAT "
        "HAS ALREADY PARSED IT. The index earned that answer by parsing all "
        f"{metrics['filesParsed']} files in {metrics['parseMs']} ms in this "
        "same process; the ratio is a statement about where the work happens, "
        "not about free information. It also excludes the question text "
        "itself, and it presumes the four questions are the ones asked - a "
        "fifth question needs no new parse but does need new query code. "
        f"Answer set: {json.dumps(t3['answers'], separators=(',', ':'))}. "
        "The Q1-Q4 values are produced by the same queries.py the "
        "analytics.nojvm lane uses, over an index built by the same extractor, "
        "so they are directly comparable with that receipt. "
        + _capability_notes(metrics)
        + "SCOPE, stated plainly: the digest is emitted by THIS harness from the "
        "parse trees; it is not an artifact the engine ships today. What is "
        "being measured is what the fork's parser makes possible in a bare "
        "CPython process, not a released feature. "
        "This receipt uses section \"ai\", which is a new section beyond the "
        "six enumerated in CONTRACTS.md section 2."
    )
    receipt = {
        "id": args.id,
        "section": "ai",
        "title": "LLM context cost by representation (no JVM)",
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
