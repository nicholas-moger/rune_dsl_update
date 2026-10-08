#!/usr/bin/env python3
"""japicmp_to_entries.py — emit proposed catalogue entries from japicmp XML.

Honours D19 (local) part 1: writes ONLY under target/byte-diff-catalogue/_proposed/
relative to the current working directory (which CI sets to the repo root,
and tests set to a controlled tmpdir).

The output path is parsed as a PurePath and validated by:
1. Must be RELATIVE — absolute paths (with leading drive or '/') are rejected.
2. Must contain no '..' parts — traversal is rejected.
3. Must START with the canonical 3-part prefix ('target', 'byte-diff-catalogue',
   '_proposed') — substring matches anywhere else in the path are rejected
   (e.g., 'docs/foo/target/byte-diff-catalogue/_proposed/x.json' is invalid
   because parts[0] is 'docs', not 'target').

Anything else exits 4. The check is tighter than a substring search and
cannot be bypassed by paths that merely contain the canonical prefix as a
suffix or fragment.

Usage:
    japicmp_to_entries.py <japicmp-xml-path> <output-json-relative-path>

Failure modes (each non-zero exit):
    2 — wrong argv count
    3 — japicmp xml not found
    4 — D19 (local) part 1 violation (path not relative + under target/byte-diff-catalogue/_proposed/)
    5 — XML parse error
"""
from __future__ import annotations

import datetime
import hashlib
import json
import pathlib
import sys
import xml.etree.ElementTree as ET


def main(argv: list[str]) -> int:
    if len(argv) != 3:
        print("usage: japicmp_to_entries.py <japicmp-xml> <out-json>", file=sys.stderr)
        return 2
    in_path = pathlib.Path(argv[1])
    out_path = pathlib.Path(argv[2])

    if not in_path.is_file():
        print(f"error: japicmp xml not found: {in_path}", file=sys.stderr)
        return 3

    # D19 (local) part 1 enforcement: parts-based, not substring-based.
    # The path must (a) be RELATIVE, (b) contain no '..' traversal,
    # (c) START with the canonical 3-part prefix
    # ('target', 'byte-diff-catalogue', '_proposed'). Resolution to a real
    # filesystem path happens via cwd; CI sets cwd to repo root, tests set
    # cwd to a tmpdir. Either way the policy bounds *where* the proposal
    # can land relative to the working tree.
    out_pure = pathlib.PurePath(argv[2])
    expected_prefix = ("target", "byte-diff-catalogue", "_proposed")
    parts = out_pure.parts
    reason = None
    if out_pure.is_absolute():
        reason = "must be relative (no leading drive or '/')"
    elif any(p == ".." for p in parts):
        reason = "must not contain '..' traversal"
    elif len(parts) <= len(expected_prefix) or tuple(parts[:3]) != expected_prefix:
        reason = (
            "must start with parts " + str(list(expected_prefix))
            + " followed by a filename"
        )
    if reason is not None:
        print(
            "error: D19 (local) part 1 violation: " + reason
            + " (got: " + argv[2] + ")",
            file=sys.stderr,
        )
        return 4
    # The pre-existing pathlib.Path object below is used for the actual write.
    # Re-derive it as relative-to-cwd so all subsequent operations see the
    # same path the policy validated.
    out_path = pathlib.Path(out_pure)

    try:
        tree = ET.parse(str(in_path))
    except ET.ParseError as e:
        print(
            f"::error file={in_path}::japicmp_to_entries: XML parse error: {e}",
            file=sys.stderr,
        )
        return 5

    root = tree.getroot()
    candidates: list[dict] = []
    # japicmp's <class> elements live under <classes>. Each carries
    # `binaryCompatible` and `changeStatus` attributes. We treat any
    # binaryCompatible="false" entry as a candidate (semantic by §8.1).
    for cls in root.findall(".//class"):
        if cls.get("binaryCompatible", "true").lower() != "false":
            continue
        change_status = cls.get("changeStatus", "UNKNOWN")
        kind_map = {
            "REMOVED": "CLASS_REMOVED",
            "MODIFIED": "CLASS_MODIFIED",
            "NEW": "CLASS_NEW",
        }
        kind = kind_map.get(change_status, f"CLASS_{change_status}")
        symbol = cls.get("fullyQualifiedName", "<unknown>")
        candidates.append({
            "japicmp_change_type": kind,
            "element_kind": "type",
            "symbol": symbol,
            "proposed_classification": "semantic",
            "proposed_status": "active",
            "rationale_template": (
                f"japicmp flagged {kind} on {symbol}. Triage required: "
                "(a) intentional fork break — promote to status=accepted with D-entry citation; "
                "(b) regression — block merge until resolved; "
                "(c) upstream bug we expose — keep status=active with linked_issue."
            ),
        })

    out_path.parent.mkdir(parents=True, exist_ok=True)
    report_sha = hashlib.sha256(in_path.read_bytes()).hexdigest()
    # Proposer output is a workflow artefact (NOT committed) so a wallclock
    # timestamp is fine here — reproducibility constraint applies only to
    # committed index.json. generated_utc is per spec section 3 item 6.
    generated_utc = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")
    output = {
        "source": "japicmp",
        "generated_utc": generated_utc,
        "japicmp_report_sha": report_sha,
        "japicmp_report_path": str(in_path),
        "candidate_count": len(candidates),
        "candidates": candidates,
    }
    with out_path.open("w", encoding="utf-8") as f:
        json.dump(output, f, indent=2, sort_keys=False)
        f.write("\n")
    print(f"wrote {out_path} ({len(candidates)} candidates)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
