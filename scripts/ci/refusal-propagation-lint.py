"""v3.1 phase C, C0 item 1 — the refusal-propagation lint.

THE PROPERTY. A refusal (SilentDegradation.Refusal) must reach
JavaClassGenerator#generateClasses, which attaches the target path and reports it
as a generation error. The generator's emission path is full of catch-alls —
`catch (Exception | RuntimeException | Throwable)` — and several do not merely
swallow: FunctionGenerator substitutes a `/* TODO: expression compilation error
... */` comment for the expression and carries on. A refusal caught by one of
those becomes exactly the silent breakage the refusal contract exists to end.

So: EVERY catch-all in rune-java-generator/src/main must rethrow a Refusal as its
first act. This lint fails the build when one does not.

Wiring it here rather than trusting review is LAW 57 (a gate that is not wired is
not a gate) applied to the mechanism that makes every other C0 gate meaningful.

Usage:
    py -3 scripts/ci/refusal-propagation-lint.py            # lint (exit 1 on failure)
    py -3 scripts/ci/refusal-propagation-lint.py --list     # list every catch-all found
"""
import os
import re
import sys

ROOT = os.path.join(
    os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))),
    "rune-java-generator", "src", "main", "java")

# A catch clause whose parameter type is broad enough to intercept a Refusal.
#
# MULTI-CATCH COUNTS. `Refusal extends RuntimeException`, so
# `catch (ClassNotFoundException | LinkageError | RuntimeException e)` intercepts one
# exactly as surely as `catch (RuntimeException e)` does. The first version of this
# pattern matched a SINGLE type only and therefore could not see any multi-catch —
# and the tree has three of them in src/main, so the guarantee this lint publishes had
# a hole in it from the day it was written (Copilot R2 suppressed note, PR #566).
# The clause matches ANY type list; BROAD_TYPES then decides whether it is broad
# enough to matter, so a narrow multi-catch is correctly ignored.
CATCH_ALL = re.compile(
    r"catch\s*\(\s*(?:final\s+)?([\w.]+(?:\s*\|\s*[\w.]+)*)\s+(\w+)\s*\)")

# The types that can intercept a Refusal. Matched per alternative of the type list.
BROAD_TYPES = {"Exception", "RuntimeException", "Throwable",
               "java.lang.Exception", "java.lang.RuntimeException", "java.lang.Throwable"}


def intercepts_refusal(type_list):
    """True when any alternative of a catch clause's type list can hold a Refusal."""
    return any(alt.strip() in BROAD_TYPES for alt in type_list.split("|"))

# The guard: an explicit rethrow of the Refusal type as the block's first statement.
GUARD = re.compile(
    r"if\s*\(\s*\w+\s+instanceof\s+(?:SilentDegradation\.)?Refusal(?:\s+\w+)?\s*\)"
    r"\s*(?:\{\s*)?throw\b")

# How far past the catch clause to look for the guard: it must be the FIRST statement,
# so the window is deliberately small — a guard buried after other work is not a guard.
# Wide enough to clear the explanatory comment the guard is written with.
GUARD_WINDOW_CHARS = 500

# The declaring file itself defines Refusal and needs no guard.
EXEMPT_BASENAMES = {"SilentDegradation.java"}

# A catch-all preceded, in the SAME try, by an explicit `catch (GenerationException ...)`
# clause is already terminal for refusals: the earlier clause claims them. That is the
# shape of JavaClassGenerator#generateClasses, the per-element boundary this whole
# mechanism delivers refusals TO.
PRIOR_GE_CLAUSE = re.compile(r"catch\s*\(\s*(?:final\s+)?GenerationException\s+\w+\s*\)")
PRIOR_WINDOW_CHARS = 600

# THE DISTINCTION THAT MATTERS. Two kinds of catch-all live in this tree:
#
#   * a BOUNDARY collects the failure into an error list and lets the rest of the
#     model generate — FunctionGenerator's per-function phases, JavaClassGenerator's
#     per-element loop. A refusal must be KEPT there: that IS its destination, and
#     rethrowing past it aborts the whole run instead of one element. (Learned the
#     hard way: guarding those two turned 7 refusals into 7 dead cells and cost the
#     matrix 20,715 identical files' worth of measurement.)
#   * a RECOVERY swallows and substitutes something — a null, a fallback shape, or
#     FunctionGenerator's `/* TODO: expression compilation error … */` comment. A
#     refusal must NEVER be recovered from; those are the blocks this lint guards.
#
# A block that adds to an error collection is a boundary. Everything else is recovery.
COLLECTS = re.compile(r"\b(?:errors|genErrors|errorList)\.add\(")
COLLECT_WINDOW_CHARS = 1200

QUOTES = ('"', "'")
LINE_FEED = "\n"
BACKSLASH = "\\"


def code_mask(src):
    """1 at offsets that are real CODE — outside comments and string/char literals.

    Necessary, not defensive: DataRuleGenerator EMITS `catch (Exception ex) {` as part
    of the Java it generates, inside a string literal. A lint that cannot tell generated
    text from its own code would demand a guard inside a string, and an auto-fixer
    following it would write one there. (It did, once. Hence this function.)
    """
    mask = bytearray(b"\x01" * len(src))
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
                mask[k] = 0
            continue
        if c == "/" and i + 1 < n and src[i + 1] in ("/", "*"):
            if src[i + 1] == "/":
                j = src.find(LINE_FEED, i)
                j = n if j < 0 else j
            else:
                j = src.find("*/", i + 2)
                j = n if j < 0 else j + 2
            for k in range(i, j):
                mask[k] = 0
            i = j
            continue
        i += 1
    return mask


def scan():
    findings, catch_alls = [], []
    for dirpath, _, names in os.walk(ROOT):
        for name in sorted(names):
            if not name.endswith(".java") or name in EXEMPT_BASENAMES:
                continue
            path = os.path.join(dirpath, name)
            with open(path, encoding="utf-8", errors="replace") as fh:
                src = fh.read()
            mask = code_mask(src)
            for m in CATCH_ALL.finditer(src):
                if not mask[m.start()]:
                    continue  # generated-code text inside a string literal, not our code
                if not intercepts_refusal(m.group(1)):
                    continue  # a narrow clause cannot hold a Refusal
                line = src.count(LINE_FEED, 0, m.start()) + 1
                rel = os.path.relpath(path, ROOT).replace("\\", "/")
                catch_alls.append((rel, line, m.group(1), m.group(2)))
                before = src[max(0, m.start() - PRIOR_WINDOW_CHARS):m.start()]
                if PRIOR_GE_CLAUSE.search(before):
                    continue  # the GenerationException clause above already claims refusals
                body = src[m.end():m.end() + COLLECT_WINDOW_CHARS]
                next_catch = body.find("catch (")
                if next_catch >= 0:
                    body = body[:next_catch]
                if COLLECTS.search(body):
                    continue  # a BOUNDARY: it collects the failure, which is where a refusal belongs
                if not GUARD.search(src[m.end():m.end() + GUARD_WINDOW_CHARS]):
                    findings.append((rel, line, m.group(1), m.group(2)))
    return catch_alls, findings


def main():
    catch_alls, findings = scan()
    if "--list" in sys.argv:
        for rel, line, kind, var in catch_alls:
            print(f"{rel}:{line}  catch ({kind} {var})")
        print()
        print(f"{len(catch_alls)} catch-all blocks")
        return 0

    print(f"refusal-propagation lint: {len(catch_alls)} catch-all blocks under "
          f"rune-java-generator/src/main")
    if findings:
        print()
        print(f"FAIL - {len(findings)} catch-all(s) can swallow a refusal:")
        print()
        for rel, line, kind, var in findings:
            print(f"  {rel}:{line}  catch ({kind} {var}) - add as its FIRST statement:")
            print(f"      if ({var} instanceof SilentDegradation.Refusal __refusal) "
                  f"throw __refusal;")
        print()
        print("A refusal must reach JavaClassGenerator#generateClasses, which attaches")
        print("the target path and reports it as a generation error. Swallowing one")
        print("restores exactly the silence the C0 refusal contract exists to end.")
        return 1
    print("OK - every catch-all rethrows a refusal.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
