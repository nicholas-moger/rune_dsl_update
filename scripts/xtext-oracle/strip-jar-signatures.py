#!/usr/bin/env python3
"""Strip JAR signature files from the oracle's copied dependencies.

Seven separately-signed Eclipse platform jars contribute classes to the package
``org.eclipse.core.runtime``; the JVM refuses to load a package whose classes
carry mismatched signer information, and Guice dies before parsing anything::

    SecurityException: class "org.eclipse.core.runtime.OperationCanceledException"'s
    signer information does not match signer information of other classes in the
    same package

The jars are stripped IN PLACE, in our own ``target/lib`` copy — never in the
local Maven repository.

Why this is a script and not a shell one-liner: the obvious ``zip -d`` is not
available on every box this repo builds on, and when it is missing the loop
that used it failed silently and then wrote a "done" marker. The oracle then
came up unusable in a way that looked like a classpath problem. This exits
non-zero and says which jars it could not fix.
"""
import sys
import zipfile
from pathlib import Path

SIGNATURE_SUFFIXES = (".SF", ".RSA", ".DSA", ".EC")


def is_signature_entry(name: str) -> bool:
    upper = name.upper()
    return upper.startswith("META-INF/") and upper.endswith(SIGNATURE_SUFFIXES)


def strip(jar: Path) -> bool:
    """Rewrite *jar* without its signature entries. True if it changed."""
    temporary = jar.with_suffix(jar.suffix + ".stripped")
    with zipfile.ZipFile(jar) as archive:
        entries = archive.infolist()
        if not any(is_signature_entry(e.filename) for e in entries):
            return False
        # A failure part-way through must not leave a half-written *.jar.stripped
        # behind in target/lib. It would not be picked up — neither this script's
        # `*.jar` glob nor Java's `lib/*` classpath wildcard matches it — but the
        # run that leaves it is the run that already failed, and the next person
        # to look in that directory should not have to work out whether a stray
        # temp file matters (Copilot R18, PR #566).
        try:
            _write_stripped(temporary, archive, entries)
        except BaseException:
            temporary.unlink(missing_ok=True)
            raise
    # Both archives are closed by here: Windows will not replace an open file.
    temporary.replace(jar)
    return True


def _write_stripped(temporary: Path, archive: zipfile.ZipFile, entries) -> None:
    """Write every non-signature entry of *archive* into a new jar at *temporary*."""
    with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as out:
        for info in entries:
            if is_signature_entry(info.filename):
                continue
            # ONE payload in memory at a time. This used to build the whole
            # `kept` list first, holding every entry of the jar at once for
            # no reason — bounded here at ~9 MB by the largest dependency,
            # so never a live problem, but not a habit to keep
            # (Copilot R12, PR #566).
            #
            # Preserve the original compression per entry. STORED means the
            # entry is held UNCOMPRESSED (not, as this said before, "already
            # compressed"); passing the original ZipInfo carries its
            # compress_type through unchanged either way, and the comment is
            # corrected so a future edit does not "fix" the wrong thing.
            out.writestr(info, archive.read(info.filename))


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print("usage: strip-jar-signatures.py <lib-dir>", file=sys.stderr)
        return 2
    lib = Path(argv[1])
    if not lib.is_dir():
        print(f"FATAL: {lib} is not a directory", file=sys.stderr)
        return 3

    changed = 0
    failed = []
    for jar in sorted(lib.glob("*.jar")):
        try:
            if strip(jar):
                changed += 1
        except Exception as exc:  # noqa: BLE001 - report every jar, fail at the end
            failed.append(f"{jar.name}: {exc}")

    if failed:
        print("FATAL: could not strip signatures from:", file=sys.stderr)
        for line in failed:
            print("  " + line, file=sys.stderr)
        return 4

    print(f"stripped signatures from {changed} jar(s)", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
