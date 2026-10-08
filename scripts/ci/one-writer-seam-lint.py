#!/usr/bin/env python3
"""one-writer-seam-lint.py — the mechanical guard of FunctionGenerator's ONE writer seam.

v3.2 seat 3 (PR #624, the round-2 code-quality review's N-6; hardened at round 3, SF-1 / N-7, at
round 4, MF-1 / SF-1 / SF-2, at round 5, MF-1 / SF-1 / SF-2 / N-4 .. N-7, and at round 6, cq MF-1 /
N-1 .. N-3 + spec N-5): `FunctionGenerator.emit`
is the only place a function file may land in the output map, because the seam's two belts
(FUNCTION_PATH_COLLISION on a second same-path writer; the cross-kind clobber) and the accounting pass
(FUNCTION_NOT_EMITTED) all reason from it. The javadoc states the law; this lint keeps it true over the
base class AND every subclass:
  * FunctionGenerator: exactly ONE `output.put(` in the class, and it must sit inside `emit` — the
    method's extent found by BRACE MATCHING from its declaration over the MASKED source: string, char
    AND text-block literals and line / block comments blanked, the block-comment and text-block states
    carried across lines (round 4: a `'{'` char literal inside `emit` had defeated the matcher and made
    it PASS a writer anywhere in the file; round 5: a text block holding `{` did the same). An extent
    the masked source cannot decide — braces that never close, a block comment or a text block still
    open at the end of the file, a string or char literal still open at the end of its LINE (round 6:
    javac closes a text block at the FIRST three quotes, measured at javac 21, so `\"\"\"\"` leaves a `"`
    that opens a literal javac rejects as unclosed — the masker had blanked the rest of that line in
    silence), or a unicode escape spelling a delimiter (`\\u0022`, `\\u007b`, ...: JLS 3.3 translates
    it BEFORE lexing; this lint reads the source untranslated) — is itself a FAIL, never a guess;
  * every subclass — DISCOVERED, not declared (round 4): each `<root>/<module>/src/main/java/**/*.java`
    (the modules ONE level under the repo root — the fork's Maven layout; a nested module tree is
    outside the walk) whose masked source carries the token `extends FunctionGenerator` — must contain
    NO `output.put(` at all. The two known subclasses (IRFunctionGenerator, OptimisedFunctionGenerator)
    must be among the found, or the discovery itself is broken. Only DIRECT subclasses are found: a
    class extending a subclass is invisible to the token.

Trivial literal matching on fixed Java sources — the no-regex law's exception (a token count, not
structural analysis of language content) — and therefore SPELLING-BOUND: the guard sees `output.put(`
and `extends FunctionGenerator` as written; rename the parameter, write through `out.putAll(` or
`merge(`, extend a subclass instead of the base, or spell either token with a unicode escape (`o\u0075tput.put(`
compiles to the same call; the round-6 belt refuses only escapes spelling the characters the MASKER keys on,
not an identifier's letters — round-6 cq re-verification N-2), and the guard lapses. That residue is the
price of a lint that never parses. A file the walk cannot read is a FAIL in the lint's own voice, not a traceback.

Usage: no arguments lints the tree. `--source <path>` replaces the base file, `--subclass <path>`
(repeatable) replaces the discovered subclass list, `--root <dir>` replaces the tree the discovery
walks, `--known <a.java,b.java>` replaces the known-subclass names the discovery must find (checked for
ANY root when given; for the repo root alone otherwise; an EMPTY list is a usage error, never a silently
skipped check — round-6 cq N-2) — the `.test` companion's hooks. Exit 0 when the law holds; 1 with the
offending lines (the RAW source line beside its number — round-6 cq N-1) otherwise; 2 on a usage error.
Every line the lint prints is ASCII: a console that cannot encode a dash must not turn a FAIL into a
traceback (round-6 cq N-3).
"""
import io
import os
import sys

REPO = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
BASE = os.path.join(REPO, "rune-java-generator", "src", "main", "java", "com", "regnosys", "rosetta",
                    "generator", "java", "function", "FunctionGenerator.java")
KNOWN_SUBCLASSES = ("IRFunctionGenerator.java", "OptimisedFunctionGenerator.java")
NEEDLE = "output.put("
SEAM = "private void emit(Map<String, String> output, String filePath, String code, RFunction func) {"
EXTENDS = "extends FunctionGenerator"
IDENT = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_$")
TEXT_BLOCK = '"""'
# the characters the masker keys on: a unicode escape spelling one of them (JLS 3.3 translates `\\uXXXX` before
# lexing; the escape may carry several `u`s) makes the untranslated source undecidable here (round-6 spec N-5)
DELIMITER_CODE_POINTS = {0x22: '"', 0x27: "'", 0x5C: "\\", 0x2F: "/", 0x2A: "*", 0x7B: "{", 0x7D: "}", 0x0A: "LF", 0x0D: "CR"}
HEX = set("0123456789abcdefABCDEF")


class Unreadable(Exception):
    """A file the lint cannot read — reported as a FAIL, never a traceback (round-5 cq N-6)."""


def mask_line(line, in_block, in_text):
    """(masked, in_block, in_text, open_literal): the line with its string / char / text-block literals
    and its comments blanked out; the block-comment and text-block states are carried across lines by
    the caller; `open_literal` is True when a string or char literal never closes on its line - javac
    rejects that, so the masker must not guess where it ends (round-6 cq MF-1). A text block's body is
    DATA: braces, `//`, `output.put(` inside it are never code."""
    out = []
    i = 0
    n = len(line)
    while i < n:
        if in_block:
            j = line.find("*/", i)
            if j < 0:
                return "".join(out), True, False, False
            i = j + 2
            in_block = False
            continue
        if in_text:
            closed = False
            while i < n:
                if line[i] == "\\":          # `\"""` does not close the block
                    i += 2
                    continue
                if line.startswith(TEXT_BLOCK, i):   # javac closes at the FIRST three quotes (measured); a fourth
                    i += 3                           # opens a literal below, which never closes: a FAIL, not a guess
                    closed = True
                    break
                i += 1
            if not closed:
                return "".join(out), False, True, False
            in_text = False
            continue
        if line.startswith(TEXT_BLOCK, i):   # a text block opens (Java 15+; this tree compiles at 21)
            in_text = True
            i += 3
            continue
        c = line[i]
        if c == '"' or c == "'":
            quote = c
            i += 1
            while i < n and line[i] != quote:
                i += 2 if line[i] == "\\" else 1
            if i >= n:                           # the literal never closes on its line (round-6 cq MF-1)
                return "".join(out), in_block, in_text, True
            i += 1
            continue
        if line.startswith("//", i):
            break
        if line.startswith("/*", i):
            in_block = True
            i += 2
            continue
        out.append(c)
        i += 1
    return "".join(out), in_block, in_text, False


def delimiter_escape(line):
    """The first unicode escape on the line that spells a delimiter character, as written, or None. JLS 3.3:
    a `\\` opens an escape when the run of backslashes it ends is ODD in length (`\\\\u0022` is an escaped
    backslash followed by text), one or more `u`, four hex digits - translated by javac BEFORE lexing, read
    untranslated here, so its presence makes the masked source undecidable (round-6 spec N-5)."""
    i = 0
    n = len(line)
    while i < n:
        if line[i] != "\\":
            i += 1
            continue
        j = i
        while j < n and line[j] == "\\":
            j += 1
        if (j - i) % 2 == 1 and j < n and line[j] == "u":
            k = j
            while k < n and line[k] == "u":
                k += 1
            if k + 4 <= n and all(ch in HEX for ch in line[k:k + 4]) and int(line[k:k + 4], 16) in DELIMITER_CODE_POINTS:
                return line[j - 1:k + 4]
        i = j
    return None


def masked_lines(text, name):
    """(masked lines, problem): the source masked line by line; `problem` names what makes the masked
    source undecidable - a unicode escape spelling a delimiter, a string or char literal still open at
    the end of its line, a block comment or a text block still open at the end of the file (the first
    found, in reading order) - None when every literal and comment closes where javac closes it."""
    masked = []
    in_block = in_text = False
    problem = None
    for k, line in enumerate(text.split("\n")):
        escape = delimiter_escape(line)
        if escape is not None and problem is None:
            problem = ("a unicode escape spelling a delimiter (%s = %s) at line %d of %s - javac translates it before lexing"
                       " (JLS 3.3), this lint reads the source untranslated" % (escape, DELIMITER_CODE_POINTS[int(escape[-4:], 16)], k + 1, name))
        m, in_block, in_text, open_literal = mask_line(line, in_block, in_text)
        if open_literal and problem is None:
            problem = "a string or char literal opened at line %d of %s never closes on its line" % (k + 1, name)
        masked.append(m)
    if problem is None and in_block:
        problem = "a block comment is still open at the end of %s" % name
    if problem is None and in_text:
        problem = "a text block is still open at the end of %s" % name
    return masked, problem


def hits_in(masked, raw):
    """(line number, the RAW source line) of every needle in the MASKED source - the text a reader can find
    in the file, the verdict from the masked copy (round-6 cq N-1)."""
    return [(i + 1, raw[i].strip()) for i, l in enumerate(masked) if NEEDLE in l]


def method_extent(masked, decl_index):
    """(first, last) 1-based line numbers of the method declared at decl_index (0-based), by brace
    matching over the masked source; (first, None) when the braces never close."""
    depth = 0
    seen = False
    for j in range(decl_index, len(masked)):
        for c in masked[j]:
            if c == "{":
                depth += 1
                seen = True
            elif c == "}":
                depth -= 1
                if seen and depth == 0:
                    return decl_index + 1, j + 1
    return decl_index + 1, None


def read(path):
    try:
        with io.open(path, encoding="utf-8") as f:
            return f.read()
    except (OSError, UnicodeDecodeError) as e:
        raise Unreadable("cannot read %s: %s" % (path, e))


def undecidable(problem):
    return problem + " - the masked source is undecidable"


def check_base(path):
    text = read(path)
    masked, problem = masked_lines(text, os.path.basename(path))
    problems = []
    hits = hits_in(masked, text.split("\n"))
    if problem:
        problems.append(undecidable(problem))
    seam_at = [i for i, l in enumerate(masked) if SEAM in l]
    if len(seam_at) != 1:
        problems.append("the seam `emit(...)` must be declared exactly once in %s (found %d)" % (os.path.basename(path), len(seam_at)))
    if len(hits) != 1:
        problems.append("expected exactly ONE `%s` in %s (found %d): %s" % (NEEDLE, os.path.basename(path), len(hits), hits))
    elif seam_at:
        first, last = method_extent(masked, seam_at[0])
        if last is None:
            problems.append("`emit`'s braces never close in the masked source (declared at line %d) - the extent is undecidable" % first)
        elif not (first < hits[0][0] <= last):
            problems.append("the one `%s` (line %d) is not inside `emit` (lines %d-%d)" % (NEEDLE, hits[0][0], first, last))
    return problems, hits


def check_subclass(path):
    text = read(path)
    masked, problem = masked_lines(text, os.path.basename(path))
    problems = []
    if problem:
        problems.append(undecidable(problem))
    hits = hits_in(masked, text.split("\n"))
    if hits:
        problems.append("a subclass writes the output map itself - %s: %s" % (os.path.basename(path), hits))
    return problems


def is_subclass_source(path):
    """(direct subclass?, problem): the token at an identifier boundary in the MASKED source (a text
    block or a comment quoting it does not count); an undecidable source is a problem, not a verdict."""
    if os.path.basename(path) == "FunctionGenerator.java":
        return False, None
    masked, problem = masked_lines(read(path), os.path.basename(path))
    if problem:
        return False, undecidable(problem)
    for m in masked:
        k = m.find(EXTENDS)
        while k >= 0:
            end = k + len(EXTENDS)
            if end == len(m) or m[end] not in IDENT:
                return True, None
            k = m.find(EXTENDS, end)
    return False, None


def discover_subclasses(root):
    """(found, problems): every direct subclass under <root>/<module>/src/main/java — the modules one
    level under the root, the fork's Maven layout; a nested module tree is outside the walk."""
    found, problems = [], []
    try:
        modules = sorted(os.listdir(root))
    except OSError as e:
        return found, ["cannot walk %s: %s" % (root, e)]
    for module in modules:
        src = os.path.join(root, module, "src", "main", "java")
        if not os.path.isdir(src):
            continue
        for dirpath, _, files in os.walk(src):
            for name in sorted(files):
                if not name.endswith(".java"):
                    continue
                p = os.path.join(dirpath, name)
                try:
                    is_sub, problem = is_subclass_source(p)
                except Unreadable as e:
                    problems.append(str(e))
                    continue
                if problem:
                    problems.append(problem)
                elif is_sub:
                    found.append(p)
    return found, problems


def same_path(a, b):
    return os.path.normcase(os.path.abspath(a)) == os.path.normcase(os.path.abspath(b))


def main(argv):
    base = BASE
    root = REPO
    overrides = []
    known = None
    i = 0
    while i < len(argv):
        flag = argv[i]
        if flag not in ("--source", "--subclass", "--root", "--known"):
            print("one-writer-seam-lint: unknown argument " + flag)
            return 2
        if i + 1 >= len(argv):
            print("one-writer-seam-lint: %s needs a value" % flag)
            return 2
        value = argv[i + 1]
        if flag == "--source":
            base = value
        elif flag == "--subclass":
            overrides.append(value)
        elif flag == "--root":
            root = value
        else:
            known = tuple(x for x in value.split(",") if x)
            if not known:                    # an empty list would silently skip the check (round-6 cq N-2)
                print("one-writer-seam-lint: --known needs at least one file name")
                return 2
        i += 2
    problems = []
    hits = []
    try:
        problems, hits = check_base(base)
    except Unreadable as e:
        problems.append(str(e))
    if overrides:
        subclasses = overrides
    else:
        subclasses, found_problems = discover_subclasses(root)
        problems += found_problems
        names = set(os.path.basename(p) for p in subclasses)
        if known is None and same_path(root, REPO):
            known = KNOWN_SUBCLASSES
        if known and not set(known) <= names:
            problems.append("the subclass discovery no longer finds the known subclasses %s (found %s)" % (tuple(known), sorted(names)))
        if not subclasses:
            problems.append("the subclass discovery found no `%s` under %s" % (EXTENDS, root))
    for sub in subclasses:
        try:
            problems += check_subclass(sub)
        except Unreadable as e:
            problems.append(str(e))
    if problems:
        for p in problems:
            print("one-writer-seam-lint: FAIL - " + p)
        return 1
    print("one-writer-seam-lint: OK - FunctionGenerator writes through emit() alone (line %d); %d subclass file(s) write nothing: %s"
          % (hits[0][0], len(subclasses), ", ".join(sorted(os.path.basename(p) for p in subclasses))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
