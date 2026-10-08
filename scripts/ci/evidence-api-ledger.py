"""v3.1 phase C, C2a — the evidence-API retirement ledger and its gate.

THE PROPERTY. C2 retires class-(b) evidence scans — code that reconstructs a
semantic fact from EMITTED artifacts (rendered Java text, the emitted-imports
refs set, an emitted binding's Java type) instead of reading it from the typed
front-end. The retirement needs a denominator, and the programme plan (rev-5
R5-1) requires that denominator to be produced by a committed, re-runnable
definition — never quoted from an uncommitted probe (the 921 lesson: 22% of
that figure was comment prose). This script IS that committed definition.

THE MECHANISM — a freeze, not a grep count (plan § 3 Layer 2). The same APIs
carry legitimate traffic: at least 367 of the ~440 `.getRefs()` code lines are
refs PROPAGATION (an expression carrying its import set forward), which C2
keeps forever; a bare may-only-fall count on the API would red on correct new
work. So the gate joins the LIVE candidate enumeration against a committed
per-site ledger (`evidence-api-ledger.tsv`, next to this script):

  * a live candidate with no ledger row FAILS (every new evidence-API site
    must be classified at introduction time);
  * a ledger row with no live candidate FAILS (stale rows cannot outlive the
    code, the bidirectional-freeze pattern from ResolutionConformanceTest);
  * a ledger classification that contradicts a DEFINITIONAL shape rule FAILS
    (the committed judgement cannot overrule shapes that are their own
    classification; ADVISORY shapes only pre-fill --emit — see the shape-rule
    block below for the strength split and the measured counterexample that
    forced it);
  * the ledger's pinned probe-site count must equal the computed one EXACTLY —
    burn-down (and any deliberate rise, plan § 6) is a recorded pin move in
    the same commit as the code that causes it, never silent drift.

THE CANDIDATE NETS (kinds, most-specific first; one row per line, the first
matching net claims it):

  wrapper-scan          uniqueMetaWrapperForValueType — the E3 core: recovered
                        an element's meta-wrapper type by scanning emitted refs
                        (the helper, its five call sites and its declaration —
                        the six wrapper-scan rows — retired at PR #613; the net
                        stays as the tripwire against its return).
  overlay-read          boundImplicitReceiverType / thenArgRefFor — receiver-
                        kind overlays: read an emitted binding (JavaExpression)
                        from render scope; probes recover cardinality/shape
                        from its Java type, renders legitimately emit it.
  refs-read             .getRefs() — reads of the emitted-imports set.
  rendered-string-test  .contains/.startsWith/.endsWith/.matches/.indexOf —
                        string predicates; probes test rendered Java text,
                        excluded rows test model data, collections, or manage
                        pure text assembly (see CLASSES below).

CLASSES. PROBE = the site recovers a semantic fact (cardinality, wrapper-ness,
meta-ness, evaluated-ness, multi-ness) from an emitted artifact and branches
lowering on it — C2's retirement target, the pin's population. PROPAGATION =
refs carried forward by construction (union / addAll / bare argument position /
return) — KEPT-BY-CLASS, never burn-down debt. EXCLUDED = the net matched but
the site is not an evidence read (model-data string test, collection
membership, or render mechanics: indentation, import-collision checks,
identifier escaping — text assembly that recovers no semantic fact); EXCLUDED
rows carry a mandatory note naming the reason.

WHAT IS DELIBERATELY NOT NETTED (scope honesty, recorded here because the
ledger's completeness claim is only as good as its nets): `getExpressionType()`
(253 sites) is a dual-use render surface — emitters legitimately read the Java
type to spell casts and generics — and netting it would drown the ledger in
render-legitimate EXCLUDED rows; its class-(b) uses are reached through the
overlay-read net (the scope-binding chokepoint) and the C2c discrepancy
census. `chainProvesMulti` / CARDINALITY.compute re-derive facts from the
RESOLVED AST, not from emitted artifacts — per-seat re-derivation retired by
C2b's typed layer, not by this ledger. `workspace()` reads the semantic model
(RWorkspace), which is the legitimate input. C3a's unattributed-tail
exhaustion is the safety net for anything these nets miss.

ALSO HELD HERE: the § 6 facet-LABEL tripwire (the convention lint the plan
scheduled at C0; the probe-site pin above is the real invariant, the label pin
is the convention tripwire) — `facet <name>` comment mentions per module,
distinct and total, pinned exactly. And a POSITIVE CONTROL: every occurrence
of the phrase "render-truth" in the scanned trees must sit in a comment — the
plan measured that bucket at ZERO code sites, so one appearing in code means
the comment mask is broken or the phrase leaked into an identifier.

Usage:
    py -3 scripts/ci/evidence-api-ledger.py               # gate (exit 1 on failure)
    py -3 scripts/ci/evidence-api-ledger.py --list        # live candidates with line numbers
    py -3 scripts/ci/evidence-api-ledger.py --emit        # ledger rows for unledgered candidates
    py -3 scripts/ci/evidence-api-ledger.py --list --context 3   # triage aid
    py -3 scripts/ci/evidence-api-ledger.py --root PATH   # self-test harness hook
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_ROOT = os.path.dirname(os.path.dirname(HERE))

# The emitter trees the candidate nets scan. rune-parser is Layer 1/2 (the
# authority the emitters must consume) — its surface is C1's conformance gate,
# not this ledger; it appears only in LABEL_MODULES for the facet tripwire.
NET_MODULES = ["rune-java-generator", "rune-ir", "rune-ir-java",
               "rune-ir-java-optimised"]
LABEL_MODULES = NET_MODULES + ["rune-parser"]
SRC = os.path.join("src", "main", "java")

LEDGER_BASENAME = "evidence-api-ledger.tsv"

CLASSES = {"PROBE", "PROPAGATION", "EXCLUDED"}
DISPOSITIONS = {"PROBE": {"PENDING", "JUSTIFIED-KEPT"},
                "PROPAGATION": {"KEPT-BY-CLASS"},
                "EXCLUDED": {"N-A"}}
NOTE_REQUIRED = {"EXCLUDED"}  # and JUSTIFIED-KEPT probes, checked separately

# Kind nets, in claim order (first match on a line wins). OUTSIDE the net, by
# design (PR #624 round-2 spec SF-5 / round-3 N-1): `.containsKey(`, `.lastIndexOf(`,
# `.substring(` and the collection-membership reads — a `0 PENDING` verdict is
# scoped to the five rendered-string tests below, never to every string read.
# Matched against the
# COMMENT-AND-STRING-MASKED source, so a `.contains(` inside generated-Java
# text in a string literal, or a `.getRefs()` in prose, nets nothing — while
# the string-literal ARGUMENT of a live predicate call stays part of the
# snippet for classification.
NETS = [
    ("wrapper-scan", re.compile(r"uniqueMetaWrapperForValueType")),
    ("overlay-read", re.compile(r"boundImplicitReceiverType|thenArgRefFor\s*\(")),
    ("refs-read", re.compile(r"\.getRefs\s*\(\s*\)")),
    ("rendered-string-test",
     re.compile(r"\.(?:contains|startsWith|endsWith|matches|indexOf)\s*\(")),
]

# Shape rules — the mechanical half of classification, in TWO strengths:
#
#   * DEFINITIONAL — the shape IS the classification: union/addAll/return/
#     bare-argument-position getRefs are propagation by construction, and
#     uniqueMetaWrapperForValueType / boundImplicitReceiverType are probes by
#     their own contracts. A committed ledger row may never contradict these.
#   * ADVISORY — the shape SUGGESTS a probe (iterating or querying the refs
#     set) but what the code does with the result decides: the tree's sole
#     `.getRefs().isEmpty()` carrier is a guard around an idempotent refs
#     UNION (propagation maintenance, FunctionGenerator), while its sole
#     for-iteration carrier recovers the arg ITEM TYPE from a unique
#     non-Mapper refs witness (a probe, FunctionExpressionRenderer). Advisory
#     verdicts pre-fill --emit and are refinable in the committed ledger.
#
# The first cut enforced the advisory shapes too; verifying each rule at its
# live carriers found the isEmpty counterexample above (LAW 60 applied to the
# instrument: a rule nobody has seen fail at ground truth is not yet a rule).
REFS_PROBE_ADVISORY = re.compile(
    r"\.getRefs\s*\(\s*\)\s*\.\s*(?:contains|stream|iterator|forEach|isEmpty|size)"
    r"|for\s*\([^)]*:\s*[\w.]+\.getRefs\s*\(\s*\)\s*\)")
REFS_PROPAGATION = re.compile(
    r"\bunion\s*\("                                  # HandlerHelper.union(e.getRefs(), ...)
    r"|\.addAll\s*\(\s*[\w.]*getRefs"                # imports.addAll(e.getRefs())
    r"|return\s+[\w.]+\.getRefs\s*\(\s*\)\s*;"       # return expression.getRefs();
    r"|getRefs\s*\(\s*\)\s*,\s*[\w.]+\.getStaticWildcardImports"  # the arg pair
    r"|new\s+\w+[^;]*getRefs")                       # constructor argument
# A line that IS a bare argument-position read — nothing but the read and list
# punctuation (the dominant multi-line constructor-argument pattern).
REFS_BARE_ARG = re.compile(r"^[\w.<>\[\]() ]*[\w.]+\.getRefs\s*\(\s*\)\s*[,)]+;?$")
# boundImplicitReceiverType exists to recover receiver kind from an emitted
# binding — every mention is probe-side. thenArgRefFor is dual-use (renders
# legitimately emit the bound reference) and stays manual.
OVERLAY_PROBE = re.compile(r"boundImplicitReceiverType")

RENDER_TRUTH = re.compile(r"render[- ]truth", re.IGNORECASE)
FACET_LABEL = re.compile(r"\bfacet ([A-Za-z_][A-Za-z0-9_]*)")

QUOTES = ('"', "'")
LINE_FEED = "\n"
BACKSLASH = "\\"


def masks(src):
    """(code_mask, comment_mask): code = outside comments AND string/char
    literals; comment = inside // or /* */ regions. Same walk as the
    refusal-propagation lint's code_mask — DataRuleGenerator emits Java text
    (predicates included) inside string literals, and a net that cannot tell
    generated text from generator code would ledger phantom sites."""
    code = bytearray(b"\x01" * len(src))
    comment = bytearray(len(src))
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c in QUOTES:
            quote, start = c, i
            i += 1
            while i < n:
                if src[i] == BACKSLASH:
                    i += 2
                    continue
                if src[i] == quote:
                    i += 1
                    break
                if src[i] == LINE_FEED:
                    break
                i += 1
            for k in range(start, min(i, n)):
                code[k] = 0
            continue
        if c == "/" and i + 1 < n and src[i + 1] in ("/", "*"):
            if src[i + 1] == "/":
                j = src.find(LINE_FEED, i)
                j = n if j < 0 else j
            else:
                j = src.find("*/", i + 2)
                j = n if j < 0 else j + 2
            for k in range(i, j):
                code[k] = 0
                comment[k] = 1
            i = j
            continue
        i += 1
    return code, comment


def norm(line):
    """Whitespace-normalised snippet: the drift-stable ledger key. Survives
    reindentation and line-number churn; an edit to the site's actual text is
    a NEW site, which correctly re-triggers classification."""
    return " ".join(line.split())


def auto_class(kind, snippet):
    """-> (verdict, definitional) — the shape verdict for a candidate line,
    or (None, False) when only the committed ledger can decide. Definitional
    verdicts may not be contradicted by the ledger; advisory ones pre-fill
    --emit and the ledger's judgement wins."""
    if kind == "wrapper-scan":
        return "PROBE", True
    if kind == "overlay-read":
        if OVERLAY_PROBE.search(snippet):
            return "PROBE", True
        return None, False
    if kind == "refs-read":
        if REFS_PROPAGATION.search(snippet) or REFS_BARE_ARG.match(snippet):
            return "PROPAGATION", True
        if REFS_PROBE_ADVISORY.search(snippet):
            return "PROBE", False
        return None, False
    return None, False  # rendered-string-test: always a committed judgement


def java_files(root, module):
    tree = os.path.join(root, module, SRC)
    for dirpath, _, names in os.walk(tree):
        for name in sorted(names):
            if name.endswith(".java"):
                yield os.path.join(dirpath, name)


def scan(root):
    """-> (candidates, labels, truth_violations)
    candidates: {(relfile, kind, snippet): [line, ...]}
    labels: {module: (distinct, mentions)}
    truth_violations: [(relfile, line)] — "render-truth" outside a comment."""
    candidates = {}
    truth_violations = []
    for module in NET_MODULES:
        for path in java_files(root, module):
            with open(path, encoding="utf-8", errors="replace") as fh:
                src = fh.read()
            code, comment = masks(src)
            rel = os.path.relpath(path, root).replace("\\", "/")
            lines = src.split(LINE_FEED)
            offsets, pos = [], 0
            for line in lines:
                offsets.append(pos)
                pos += len(line) + 1
            for lineno, line in enumerate(lines, 1):
                base = offsets[lineno - 1]
                claimed = False
                for kind, net in NETS:
                    if claimed:
                        break
                    for m in net.finditer(line):
                        if code[base + m.start()]:
                            key = (rel, kind, norm(line))
                            candidates.setdefault(key, []).append(lineno)
                            claimed = True
                            break
                for m in RENDER_TRUTH.finditer(line):
                    if not comment[base + m.start()]:
                        truth_violations.append((rel, lineno))
    labels = {}
    for module in LABEL_MODULES:
        distinct, mentions = set(), 0
        for path in java_files(root, module):
            with open(path, encoding="utf-8", errors="replace") as fh:
                src = fh.read()
            _, comment = masks(src)
            for m in FACET_LABEL.finditer(src):
                if comment[m.start()]:
                    distinct.add(m.group(1))
                    mentions += 1
        labels[module] = (len(distinct), mentions)
    return candidates, labels, truth_violations


def load_ledger(path):
    """-> (rows {(file, kind, snippet): (cls, disposition, count, note)},
    pins {name: value}, errors [str])"""
    rows, pins, errors = {}, {}, []
    if not os.path.isfile(path):
        return rows, pins, ["ledger file missing: " + path]
    with open(path, encoding="utf-8") as fh:
        for lineno, raw in enumerate(fh, 1):
            # \r-tolerant: a CRLF checkout must not smuggle \r into the last
            # field (the #567 line-endings-are-part-of-the-instrument lesson;
            # the .gitattributes LF pin is the first line of defence).
            line = raw.rstrip("\r\n")
            if not line.strip():
                continue
            if line.startswith("#"):
                m = re.match(r"#\s*pin\s+([\w-]+)\s*=\s*(\S+)", line)
                if m:
                    pins[m.group(1)] = m.group(2)
                continue
            parts = line.split("\t")
            if len(parts) < 6:
                errors.append("ledger:%d: expected >=6 tab-separated fields, "
                              "got %d" % (lineno, len(parts)))
                continue
            file_, kind, cls, disposition, count, snippet = parts[:6]
            note = parts[6] if len(parts) > 6 else ""
            if cls not in CLASSES:
                errors.append("ledger:%d: class %r is not one of %s (an "
                              "UNCLASSIFIED emit-placeholder must be resolved "
                              "before commit)" % (lineno, cls, sorted(CLASSES)))
                continue
            if disposition not in DISPOSITIONS[cls]:
                errors.append("ledger:%d: disposition %r invalid for %s "
                              "(allowed: %s)" % (lineno, disposition, cls,
                                                 sorted(DISPOSITIONS[cls])))
            if cls in NOTE_REQUIRED and not note.strip():
                errors.append("ledger:%d: %s row needs a note naming the "
                              "reason" % (lineno, cls))
            if cls == "PROBE" and disposition == "JUSTIFIED-KEPT" \
                    and not note.strip():
                errors.append("ledger:%d: JUSTIFIED-KEPT needs the written "
                              "justification in the note" % lineno)
            try:
                count = int(count)
            except ValueError:
                errors.append("ledger:%d: count %r is not an integer"
                              % (lineno, count))
                continue
            key = (file_, kind, snippet)
            if key in rows:
                errors.append("ledger:%d: duplicate row for %s" % (lineno, key))
                continue
            rows[key] = (cls, disposition, count, note)
    return rows, pins, errors


def gate(root, ledger_path):
    candidates, labels, truth_violations = scan(root)
    rows, pins, errors = load_ledger(ledger_path)

    for key, linenos in sorted(candidates.items()):
        rel, kind, snippet = key
        if key not in rows:
            errors.append("unledgered candidate: %s:%s  [%s]  %s"
                          % (rel, ",".join(map(str, linenos)), kind, snippet))
            continue
        cls, _, count, _ = rows[key]
        if count != len(linenos):
            errors.append("count drift: %s [%s] ledger=%d live=%d  %s"
                          % (rel, kind, count, len(linenos), snippet))
        verdict, definitional = auto_class(kind, snippet)
        if definitional and verdict is not None and cls != verdict:
            errors.append("classification contradicts definitional shape "
                          "rule: %s [%s] ledger=%s shape=%s  %s"
                          % (rel, kind, cls, verdict, snippet))
    for key in sorted(rows):
        if key not in candidates:
            errors.append("stale ledger row (site no longer in the tree — "
                          "remove the row and move the pin in this commit): "
                          "%s [%s]  %s" % key)

    probe_sites = sum(len(candidates[k]) for k in candidates
                      if k in rows and rows[k][0] == "PROBE")
    pin = pins.get("probe-sites")
    if pin is None:
        errors.append("ledger has no '# pin probe-sites=N' header")
    elif pin != str(probe_sites):
        errors.append("probe-site pin %s != computed %d — a burn-down (or a "
                      "deliberate, written-reason rise) moves the pin in the "
                      "same commit" % (pin, probe_sites))

    for module, (distinct, mentions) in sorted(labels.items()):
        want = pins.get("facet-labels-" + module)
        have = "%d/%d" % (distinct, mentions)
        if want is None:
            errors.append("ledger has no '# pin facet-labels-%s=D/M' header"
                          % module)
        elif want != have:
            errors.append("facet-label tripwire: %s pinned %s, tree has %s "
                          "(the convention count moved — re-pin deliberately "
                          "in this commit)" % (module, want, have))

    for rel, lineno in truth_violations:
        errors.append("positive control: 'render-truth' OUTSIDE a comment at "
                      "%s:%d — the phrase has zero legitimate code sites"
                      % (rel, lineno))
    return candidates, rows, probe_sites, errors


def main():
    # Snippets can carry any byte a source file does; a cp1252 Windows console
    # must degrade the glyph, never crash the gate mid-report.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")
    argv = sys.argv[1:]
    root = DEFAULT_ROOT
    if "--root" in argv:
        i = argv.index("--root")
        if i + 1 >= len(argv):
            print("--root requires a path", file=sys.stderr)
            return 2
        root = argv[i + 1]
    ledger_path = os.path.join(root, "scripts", "ci", LEDGER_BASENAME) \
        if root != DEFAULT_ROOT else os.path.join(HERE, LEDGER_BASENAME)

    if "--list" in argv or "--emit" in argv:
        candidates, labels, _ = scan(root)
        rows, _, _ = load_ledger(ledger_path)
        context = 0
        if "--context" in argv:
            i = argv.index("--context")
            context = int(argv[i + 1]) if i + 1 < len(argv) else 0
        emitted = 0
        for key, linenos in sorted(candidates.items()):
            rel, kind, snippet = key
            if "--emit" in argv:
                if key in rows:
                    continue
                cls = auto_class(kind, snippet)[0] or "UNCLASSIFIED"
                disposition = {"PROBE": "PENDING",
                               "PROPAGATION": "KEPT-BY-CLASS"}.get(cls, "N-A")
                print("\t".join([rel, kind, cls, disposition,
                                 str(len(linenos)), snippet, ""]))
                emitted += 1
            else:
                print("%s:%s  [%s]  %s"
                      % (rel, ",".join(map(str, linenos)), kind, snippet))
                if context:
                    path = os.path.join(root, rel)
                    with open(path, encoding="utf-8", errors="replace") as fh:
                        lines = fh.read().split(LINE_FEED)
                    for ln in linenos:
                        lo = max(0, ln - 1 - context)
                        hi = min(len(lines), ln + context)
                        for k in range(lo, hi):
                            print("      %5d| %s" % (k + 1, lines[k]))
                        print("      -----")
        if "--emit" in argv:
            print("%d unledgered candidate(s)" % emitted, file=sys.stderr)
        else:
            for module, (distinct, mentions) in sorted(labels.items()):
                print("facet-labels %s: %d distinct / %d mentions"
                      % (module, distinct, mentions))
        return 0

    candidates, rows, probe_sites, errors = gate(root, ledger_path)
    by_class = {}
    for k in candidates:
        if k in rows:
            by_class[rows[k][0]] = by_class.get(rows[k][0], 0) \
                + len(candidates[k])
    pending = sum(len(candidates[k]) for k in candidates if k in rows
                  and rows[k][0] == "PROBE" and rows[k][1] == "PENDING")
    print("evidence-api ledger: %d candidate sites (%s), probe pin %d, "
          "%d PENDING disposition"
          % (sum(len(v) for v in candidates.values()),
             ", ".join("%s=%d" % kv for kv in sorted(by_class.items())),
             probe_sites, pending))
    if errors:
        print()
        print("FAIL - %d finding(s):" % len(errors))
        print()
        for e in errors:
            print("  " + e)
        return 1
    print("OK - ledger and tree agree; the probe pin holds.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
