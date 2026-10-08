#!/usr/bin/env python3
# The L1 WARNING census (charter § 4 severity scope; the spec review's MF-3).
#
# The admission gate (l1-gate.py) adjudicates ERROR-severity facts - the
# dump's VALIDERR/LINKERR rows. Upstream WARNINGs never enter the dump (the
# oracle prints them to its log), so this instrument censuses the LOG:
# every WARNING line must belong to a DISPOSITIONED class in
# expectations/oracle-warnings.tsv, and every dispositioned class must fire.
# An undispositioned warning class fails loud - "clean" is never allowed to
# narrow silently again.
#
# Classification is literal-substring on LOG LINES (shell-output grain - the
# engineering standard's explicit non-structured exception); the one
# normalisation collapses per-namespace "Unused import <ns>" lines into the
# single class their disposition covers.
#
# usage: warnings-census.py <oracle-run.log> [--receipt <out.txt>]
import io
import os
import sys

LAB = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, LAB)
from chaoslib import load_tsv  # noqa: E402

EXPECTATIONS = os.path.join(LAB, 'expectations', 'oracle-warnings.tsv')


def load_classes():
    return [tuple(cols) for _ln, cols in
            load_tsv(EXPECTATIONS, 3, 'warning class')]


def main(argv):
    args = list(argv[1:])
    receipt_path = None
    if '--receipt' in args:
        i = args.index('--receipt')
        receipt_path = args[i + 1]
        del args[i:i + 2]
    if len(args) != 1:
        print('usage: warnings-census.py <oracle-run.log> [--receipt <out.txt>]')
        return 2

    # BOTH warning spellings the oracle's output carries (the cq review's
    # SF-9: 'WARNING <validator message>' from the validation leg AND
    # '[WARN] <logger message>' from the oracle's logging framework - a
    # framework-level warning must not be invisible).
    warnings = []
    for ln in io.open(args[0], encoding='utf-8', errors='replace').read().split('\n'):
        stripped = ln.strip()
        if stripped.startswith('WARNING '):
            warnings.append(stripped[len('WARNING '):])
        elif stripped.startswith('[WARN] '):
            warnings.append(stripped[len('[WARN] '):])

    classes = load_classes()
    counts = {cls: 0 for cls, _s, _d in classes}
    undispositioned = {}
    for w in warnings:
        matched = False
        for cls, substring, _d in classes:
            if substring in w:
                counts[cls] += 1
                matched = True
                break
        if not matched:
            undispositioned[w] = undispositioned.get(w, 0) + 1

    out = []

    def say(msg):
        out.append(msg)
        print(msg)

    say('warning census over %s: %d WARNING lines' % (os.path.basename(args[0]), len(warnings)))
    for cls, _s, disposition in classes:
        say('  %-28s %5d  %s' % (cls, counts[cls], disposition))

    code = 0
    silent = [cls for cls, n in counts.items() if n == 0]
    if undispositioned:
        code = 1
        say('WARNINGS REFUSED: %d undispositioned class(es):' % len(undispositioned))
        for w, n in sorted(undispositioned.items(), key=lambda kv: -kv[1])[:20]:
            say('  %5d  %s' % (n, w))
    if silent:
        code = 1
        say('WARNINGS REFUSED: dispositioned class(es) that no longer fire '
            '(re-ratify): %s' % silent)
    if code == 0:
        say('WARNINGS CENSUSED: every class dispositioned, every disposition live.')
    if receipt_path:
        with io.open(receipt_path, 'w', encoding='utf-8', newline='\n') as f:
            f.write('\n'.join(out) + '\nEXIT=%d\n' % code)
    return code


if __name__ == '__main__':
    sys.exit(main(sys.argv))
