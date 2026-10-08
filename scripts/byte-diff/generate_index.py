#!/usr/bin/env python3
"""generate_index.py — emit docs/byte-diff-catalogue/index.json.

Walks docs/byte-diff-catalogue/<corpus>/<version>/*.json (excluding
index.json and schema.json), validates filename matches each entry's
entry_id field, sorts by entry_id, and writes the aggregate index.

Idempotent. Reproducible (no timestamps in output) so the
index-regen-check CI job can diff committed copy against fresh
regeneration byte-for-byte.

Usage:
    generate_index.py            # writes docs/byte-diff-catalogue/index.json
    generate_index.py --stdout   # writes JSON to stdout (CI mode)

Failure modes (each non-zero exit):
    2 — catalogue root not found
    3 — entry missing entry_id field
    4 — filename != entry_id
    5 — duplicate entry_id across two files
"""
from __future__ import annotations

import json
import pathlib
import subprocess
import sys


def repo_root() -> pathlib.Path:
    """Resolve repo root via git rev-parse. Returns clean exit code on failure
    instead of letting CalledProcessError / FileNotFoundError propagate as a
    raw stack trace — so callers running outside a git worktree see a useful
    error message and exit 2 (catalogue-root-not-found semantics).
    """
    try:
        out = subprocess.check_output(
            ["git", "rev-parse", "--show-toplevel"],
            text=True,
            stderr=subprocess.PIPE,
        ).strip()
    except (subprocess.CalledProcessError, FileNotFoundError) as e:
        print(
            f"error: not in a git worktree (git rev-parse failed): {e}",
            file=sys.stderr,
        )
        sys.exit(2)
    return pathlib.Path(out)


def main(argv: list[str]) -> int:
    root = repo_root() / "docs" / "byte-diff-catalogue"
    if not root.is_dir():
        print(f"error: catalogue root not found: {root}", file=sys.stderr)
        return 2

    entry_files = sorted(
        p for p in root.rglob("*.json")
        if p.name not in {"index.json", "schema.json"}
    )

    entries: list[dict] = []
    seen_ids: dict[str, pathlib.Path] = {}
    for path in entry_files:
        with path.open(encoding="utf-8") as f:
            entry = json.load(f)
        eid = entry.get("entry_id")
        if not eid:
            print(f"error: entry missing entry_id: {path}", file=sys.stderr)
            return 3
        if path.stem != eid:
            print(
                f"error: filename ({path.stem}) != entry_id ({eid}): {path}",
                file=sys.stderr,
            )
            return 4
        if eid in seen_ids:
            print(
                f"error: duplicate entry_id {eid}: {seen_ids[eid]} and {path}",
                file=sys.stderr,
            )
            return 5
        seen_ids[eid] = path
        entries.append(entry)

    entries.sort(key=lambda e: e["entry_id"])

    index = {
        "entry_count": len(entries),
        "entries": entries,
    }

    if "--stdout" in argv:
        json.dump(index, sys.stdout, indent=2, sort_keys=False)
        sys.stdout.write("\n")
    else:
        out_path = root / "index.json"
        with out_path.open("w", encoding="utf-8") as f:
            json.dump(index, f, indent=2, sort_keys=False)
            f.write("\n")
        print(f"wrote {out_path} ({len(entries)} entries)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
