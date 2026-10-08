#!/usr/bin/env python3
# Shared helpers for the chaos-expander lab instruments (the cq review's
# SF-10: one TSV loader, one failure mode, every error naming file+line).
import io


def load_tsv(path, ncols, name):
    """Load a #-commented TSV whose data rows carry exactly ncols columns.
    Returns [(lineno, [col, ...])]. A malformed row fails LOUD with the file,
    the line number, and the contract it broke - never a bare unpack error."""
    rows = []
    for lineno, ln in enumerate(
            io.open(path, encoding='utf-8').read().split('\n'), start=1):
        # Strip line terminators ONLY - a trailing space in a match substring
        # is content, not noise (the cq review's NIT-6).
        ln = ln.rstrip('\r\n')
        if not ln or ln.startswith('#'):
            continue
        cols = ln.split('\t')
        if len(cols) != ncols:
            raise SystemExit(
                '%s:%d: %s row has %d columns, the contract is %d: %r'
                % (path, lineno, name, len(cols), ncols, ln))
        rows.append((lineno, cols))
    return rows
