#!/usr/bin/env python3
# The L1 admission gate (charter § 4 layer 1).
#
# Reads an xtext-oracle dump and the EXPECTED census
# (expectations/oracle-diagnostics.tsv, FOUR columns:
#   file <TAB> reason-code <TAB> message-substring <TAB> rows
# where rows is a kind-count multiset like "LINKERR=1,VALIDERR=1", or '-'
# on an ORACLE-CLEAN row) and passes IFF:
#   (1) every VALIDERR/LINKERR dump row is CLAIMED by exactly one census row
#       (its file's row whose substring matches; two census rows of one file
#       both matching a dump row is a census defect and refuses);
#   (2) every census row's claimed rows equal its PINNED kind-count multiset
#       EXACTLY (the cq review's MF-1: existence-matching let up to 25 of 48
#       rows heal silently; LAW 73 - pin the multiset, not the presence);
#   (3) every ORACLE-CLEAN row ('-') sees ZERO error rows for its file;
#   (4) every reason code is on the deliberate-by-design allow-list;
#   (5) the dump header is present, names oracle 9.83.0 (the ORACLE LAW -
#       a dump from another oracle build admits nothing), and carries a
#       positive record count;
#   (6) every dump row kind is on the known-kind allow-list (an unknown
#       kind, e.g. a future PARSEERR, refuses instead of passing unseen).
# SEVERITY SCOPE (charter § 4): ERROR-severity facts only; WARNINGs are
# censused by warnings-census.py.
# Exit 0 = admitted; 1 = refused; 2 = usage; 3 = the gate itself failed to
# run (a crashed run is NOT a refusal and NEVER leaves a stale receipt - the
# receipt is stamped RUNNING at start and rewritten on every exit path).
#
# usage: l1-gate.py <dump.tsv> [--receipt <out.txt>]
import io
import os
import sys
import traceback

# Dump diagnostics carry raw non-ASCII (the BOM refusals quote U+FEFF); a
# cp1252 console must never turn a genuine REFUSAL into a reported gate
# crash (the cq verification's N-1).
if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

LAB = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, LAB)
from chaoslib import load_tsv  # noqa: E402

EXPECTATIONS = os.path.join(LAB, 'expectations', 'oracle-diagnostics.tsv')
ORACLE_VERSION = '9.83.0'
KNOWN_KINDS = {'XREF', 'VALIDERR', 'LINKERR'}
ERROR_KINDS = {'VALIDERR', 'LINKERR'}
ADMISSIBLE_CODES = {
    'BOM-REFUSAL',          # upstream's lexer rejects a UTF-8 BOM (refusal parity)
    'COLLISION-CAPTURE',    # upstream's own recorded resolution answer (E2 class)
    'ORACLE-CLEAN',         # a recorded upstream-CLEAN verdict ('-' rows, S15 class)
    'REFUSAL-PARITY',       # chaos-1.1.0 (D49): the base-only s9x band - the seed
                            # INTENDS the released plugin's refusal (grammar /
                            # linker / validator) and pins the fork's own verdict
                            # per file in fork-diagnostics.tsv; the BOM-REFUSAL
                            # precedent at the validator grain
}


def parse_rowspec(spec, where):
    counts = {}
    for part in spec.split(','):
        if '=' not in part:
            raise SystemExit('%s: bad rows spec %r (KIND=N,...)' % (where, spec))
        kind, n = part.split('=', 1)
        if kind not in ERROR_KINDS or not n.isdigit() or int(n) <= 0:
            raise SystemExit('%s: bad rows spec %r' % (where, spec))
        counts[kind] = counts.get(kind, 0) + int(n)
    return counts


def run(dump_path, say):
    lines = io.open(dump_path, encoding='utf-8').read().split('\n')
    if not lines or not lines[0].startswith('# rune-xtext-resolution-oracle'):
        say('L1 REFUSED: dump header missing - not a completed oracle run')
        return 1
    if (' %s ' % ORACLE_VERSION) not in lines[0] + ' ':
        say('L1 REFUSED: the dump header does not name oracle %s (THE ORACLE '
            'LAW - a census ratified for one oracle admits nothing from '
            'another): %r' % (ORACLE_VERSION, lines[0]))
        return 1
    try:
        records = int(lines[0].split('records=')[1].split()[0])
    except (IndexError, ValueError):
        say('L1 REFUSED: dump header carries no record count')
        return 1
    if records <= 0:
        say('L1 REFUSED: zero records - an empty dump is a failed boot, not a clean corpus')
        return 1

    exp = []
    for lineno, cols in load_tsv(EXPECTATIONS, 4, 'EXPECTED census'):
        f, code, sub, rowspec = cols
        if code not in ADMISSIBLE_CODES:
            say('L1 REFUSED: inadmissible reason code %r at line %d (the § 4 bar)'
                % (code, lineno))
            return 1
        if (sub == '-') != (rowspec == '-'):
            say("L1 REFUSED: line %d mixes '-' and a non-'-' column" % lineno)
            return 1
        pinned = None if sub == '-' else parse_rowspec(
            rowspec, '%s:%d' % (EXPECTATIONS, lineno))
        exp.append((f, code, sub, pinned))

    err_rows_by_file = {}
    err_rows = 0
    unknown_kinds = set()
    for ln in lines[1:]:
        if not ln:
            continue
        cols = ln.split('\t')
        if cols[0] not in KNOWN_KINDS:
            unknown_kinds.add(cols[0])
            continue
        if cols[0] not in ERROR_KINDS:
            continue
        err_rows += 1
        # Basename keying: the sink is FLAT (the expander enforces unique
        # names); if a nested layout ever feeds this gate, revisit the key.
        fname = cols[1].replace('\\', '/').split('/')[-1]
        err_rows_by_file.setdefault(fname, []).append((cols[0], cols[-1]))

    if unknown_kinds:
        say('L1 REFUSED: unknown dump row kind(s) %s - the gate adjudicates '
            'only %s and refuses what it cannot see'
            % (sorted(unknown_kinds), sorted(KNOWN_KINDS)))
        return 1

    problems = []
    claimed_counts = {}   # (file, sub) -> {kind: n}
    for fname, rows_here in sorted(err_rows_by_file.items()):
        specs = [(sub, pinned) for (f, _c, sub, pinned) in exp
                 if f == fname and sub != '-']
        for kind, msg in rows_here:
            claimants = [sub for sub, _p in specs if sub in msg]
            if not claimants:
                problems.append('UNEXPECTED\t%s\t%s\t%s' % (kind, fname, msg))
            elif len(claimants) > 1:
                problems.append('AMBIGUOUS\t%s\t%s claimed by %d census rows %s'
                                % (fname, msg, len(claimants), claimants))
            else:
                key = (fname, claimants[0])
                claimed_counts.setdefault(key, {})
                claimed_counts[key][kind] = claimed_counts[key].get(kind, 0) + 1

    for f, code, sub, pinned in exp:
        if sub == '-':
            if f in err_rows_by_file:
                problems.append('CLEAN-VIOLATED\t%s\t%s' % (f, err_rows_by_file[f][0]))
            continue
        got = claimed_counts.get((f, sub), {})
        if got != pinned:
            problems.append('ROWS-MOVED\t%s\t%r: pinned %s, measured %s '
                            '(a silent heal or growth is a verdict change - '
                            're-ratify the census)' % (f, sub, pinned, got))

    if problems:
        say('L1 REFUSED: %d problem(s):' % len(problems))
        for p in problems[:60]:
            say('  ' + p)
        if len(problems) > 60:
            say('  ... and %d more' % (len(problems) - 60))
        return 1
    say('L1 ADMITTED: records=%d, error rows=%d, every row claimed by exactly '
        'one census row, every census row at its pinned kind-count multiset '
        '(%d rows over %d files incl. clean-verdict rows).'
        % (records, err_rows, len(exp), len({f for f, _c, _s, _p in exp})))
    return 0


def main(argv):
    args = list(argv[1:])
    receipt_path = None
    if '--receipt' in args:
        i = args.index('--receipt')
        if i + 1 >= len(args):
            print('usage: l1-gate.py <dump.tsv> [--receipt <out.txt>]')
            return 2
        receipt_path = args[i + 1]
        del args[i:i + 2]
    if len(args) != 1:
        print('usage: l1-gate.py <dump.tsv> [--receipt <out.txt>]')
        return 2

    out_lines = []

    def say(msg):
        out_lines.append(msg)
        print(msg)

    def write_receipt(code):
        if receipt_path:
            with io.open(receipt_path, 'w', encoding='utf-8', newline='\n') as f:
                f.write('\n'.join(out_lines) + '\nEXIT=%d\n' % code)

    # Stamp the receipt FIRST (the cq review's MF-4): a crashed run must
    # never leave the previous run's ADMITTED standing.
    if receipt_path:
        with io.open(receipt_path, 'w', encoding='utf-8', newline='\n') as f:
            f.write('RUNNING - no verdict yet\nEXIT=3\n')
    try:
        code = run(args[0], say)
    except SystemExit as e:
        say(str(e))
        write_receipt(3)
        return 3
    except Exception:
        say('L1 GATE CRASHED (exit 3 - not a refusal, not a measurement):')
        for tl in traceback.format_exc().split('\n'):
            say('  ' + tl)
        write_receipt(3)
        return 3
    write_receipt(code)
    return code


if __name__ == '__main__':
    sys.exit(main(sys.argv))
