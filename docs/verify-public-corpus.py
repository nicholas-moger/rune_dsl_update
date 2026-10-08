"""Read-only check of public clone pins and the retained CDM source/golden hashes.

Run after independent upstream reference generation. It writes no source, golden,
manifest or baseline. Every file is classified from its RAW bytes first:

  identical          raw bytes equal the retained reference (cdm-5.38.0.sha256)
  lineEndingOnly     raw bytes differ, CRLF->LF normalised bytes equal the retained content
                     (the reference Java was generated on Windows; expected on an LF platform)
  acceptedOrdering   a file whose content depends on the generating machine's enumeration order
                     and whose bytes equal a form that an independent released-generator run
                     produced (cdm-5.38.0.witnessed-variants.tsv); `lineEndingDiffers` says
                     whether only its line endings differ from that witnessed form
  unverifiedOrdering an enumeration-order-dependent file whose bytes match NO witnessed form:
                     not accepted; compare it by hand (see docs/CORPUS-9.83.md)
  different          content differs (a finding to investigate; never normalised away)
  missing / extra    population differences

`rawBytesIdentical` is true only when EVERY common file is `identical` (no accepted ordering,
no line-ending difference) and the populations match. `passed` requires no missing, extra,
different or unverifiedOrdering file; accepted orderings and line-ending-only differences are
reported, not hidden. Usage: python3 docs/verify-public-corpus.py [--root <repository root>]
"""
import hashlib
import json
from pathlib import Path, PurePosixPath
import subprocess
import sys


def git(directory, *args):
    return subprocess.check_output(['git', '-C', str(directory), *args])


def valid_digest(digest):
    return len(digest) == 64 and all(c in '0123456789abcdef' for c in digest)


def read_manifest(path):
    expected = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        name, raw, normalised = line.split('\t')
        key = PurePosixPath(name)
        if key.is_absolute() or '..' in key.parts or name in expected:
            raise ValueError(f'Invalid or duplicate manifest path: {name}')
        if not (valid_digest(raw) and valid_digest(normalised)):
            raise ValueError(f'Invalid SHA-256: {name}')
        expected[name] = (raw, normalised)
    if not expected:
        raise ValueError('Empty expected population')
    return expected


def read_witnessed(path, expected):
    """path -> list of (variant, raw, normalised, witnesses, isRetained); each path must be in the
    manifest and its retained form must appear exactly once with the manifest's hashes."""
    variants = {}
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        name, index, raw, normalised, witnesses, retained = line.split('\t')
        if name not in expected:
            raise ValueError(f'Witnessed-variant path not in manifest: {name}')
        if not (valid_digest(raw) and valid_digest(normalised)) or not witnesses:
            raise ValueError(f'Invalid witnessed-variant row: {name}')
        variants.setdefault(name, []).append((int(index), raw, normalised, witnesses, retained == 'true'))
    for name, rows in variants.items():
        retained = [r for r in rows if r[4]]
        if len(retained) != 1 or (retained[0][1], retained[0][2]) != expected[name]:
            raise ValueError(f'Witnessed-variant list does not carry the retained reference exactly once: {name}')
    return variants


def sha(data):
    return hashlib.sha256(data).hexdigest()


def compare(base, expected, actual, witnessed):
    missing = sorted(set(expected) - set(actual))
    extra = sorted(set(actual) - set(expected))
    identical, line_ending_only, different = [], [], []
    accepted, unverified = [], []
    for name in sorted(set(expected) & set(actual)):
        data = (base / name).read_bytes()
        raw = sha(data)
        normalised = sha(data.replace(b'\r\n', b'\n'))
        if raw == expected[name][0]:
            identical.append(name)
        elif normalised == expected[name][1]:
            line_ending_only.append(name)
        elif name in witnessed:
            match = next((r for r in witnessed[name] if r[1] == raw), None)
            eol = False
            if match is None:
                match = next((r for r in witnessed[name] if r[2] == normalised), None)
                eol = match is not None
            if match is None:
                unverified.append(name)
            else:
                accepted.append({'path': name, 'variant': match[0], 'witnesses': match[3],
                                 'lineEndingDiffers': eol})
        else:
            different.append(name)
    common = len(set(expected) & set(actual))
    passed = bool(expected) and not (missing or extra or different or unverified)
    return {'expected': len(expected), 'observed': len(actual),
            'identical': len(identical), 'lineEndingOnly': len(line_ending_only),
            'lineEndingOnlyFirst': line_ending_only[:5],
            'acceptedOrdering': accepted, 'unverifiedOrdering': unverified,
            'missing': missing, 'extra': extra, 'different': different,
            'rawBytesIdentical': not (missing or extra) and common > 0 and len(identical) == common,
            'passed': passed}


def main(argv):
    docs = Path(__file__).resolve().parent
    root = docs.parent
    if len(argv) == 3 and argv[1] == '--root':
        root = Path(argv[2]).resolve()
    elif len(argv) != 1:
        raise ValueError('usage: verify-public-corpus.py [--root <repository root>]')
    lock = json.loads((docs / 'corpus-lock.json').read_text(encoding='utf-8'))
    for item in lock['inputs']:
        directory = root / item['directory']
        top = Path(git(directory, 'rev-parse', '--show-toplevel').decode().strip()).resolve()
        if top != directory.resolve():
            raise ValueError(f"Not an independent clone: {item['directory']}")
        if git(directory, 'rev-parse', 'HEAD').decode().strip() != item['commit']:
            raise ValueError(f"Wrong source commit: {item['name']}")
        # Includes staged and unstaged tracked changes; generated untracked output is allowed.
        if git(directory, 'diff', 'HEAD', '--name-only').strip():
            raise ValueError(f"Tracked inputs changed: {item['name']}")
        tracked = set(git(directory, 'ls-files', '--', item['sourceRoot']).decode().splitlines())
        observed = {p.relative_to(directory).as_posix()
                    for p in (directory / item['sourceRoot']).rglob('*.rosetta') if p.is_file()}
        if not observed or observed - tracked:
            raise ValueError(f"Empty or untracked Rune inputs: {item['name']}")
        for name, expected_hash in item.get('gitBlobSha256', {}).items():
            digest = hashlib.sha256(git(directory, 'show', 'HEAD:' + name)).hexdigest()
            if digest != expected_hash:
                raise ValueError(f'Wrong pinned Git blob: {name}')

    expected = read_manifest(docs / 'evidence/cdm-5.38.0.sha256')
    witnessed = read_witnessed(docs / 'evidence/cdm-5.38.0.witnessed-variants.tsv', expected)
    corpus = root / 'test-corpus'
    try:
        cdm = next(item for item in lock['inputs'] if item['name'] == 'cdm')
    except (LookupError, StopIteration, TypeError) as exc:
        raise ValueError(f'The corpus lock has no usable cdm input: {exc!r}')
    cell = root / cdm['directory']
    actual = []
    # Check the declared source/golden roots, not Maven's target copies or unrelated files.
    for relative, suffix in [(cdm['sourceRoot'], '.rosetta'), (cdm['goldenRoot'], '.java')]:
        actual.extend(p.relative_to(corpus).as_posix()
                      for p in (cell / relative).rglob('*' + suffix) if p.is_file())
    result = compare(corpus, expected, actual, witnessed)
    result['scope'] = ('CDM 5.38.0: 87 source files (Git blob bytes at the pinned commit) + 3,950 independent '
                       'reference Java files (retained Windows reference); raw bytes first; accepted orderings '
                       'are only forms witnessed by independent released-generator runs')
    print(json.dumps(result, indent=2))
    return 0 if result['passed'] else 1


if __name__ == '__main__':
    try:
        sys.exit(main(sys.argv))
    except (OSError, ValueError, subprocess.CalledProcessError) as error:
        print(f'Public corpus verification failed: {error}', file=sys.stderr)
        sys.exit(2)
