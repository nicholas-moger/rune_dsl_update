#!/usr/bin/env python3
# The chaos-corpus expander (v3.2 PR-1; the charter's § 2 machinery).
#
# HYBRID authoring (charter D-1): hand-written seed models per construct
# family live in seeds/; this script generates the placement/order/import/
# namespace/encoding variants from them. Expansion is OAT (one-factor-at-a-
# time) plus the enumerated pairwise rows in interactions.tsv - NEVER a
# cross-product (charter § 2.3).
#
# THE NO-REGEX-ON-STRUCTURED-CONTENT LAW (CLAUDE.md engineering standards):
# this script NEVER parses or pattern-matches Rune grammar. Seeds are treated
# as sequences of authored BLOCKS delimited by whole-line literal markers
# ('//== ' lines - our own metadata, not grammar), plus THREE content-blind
# literal touches: two exact header lines rewritten wholesale -
#     namespace chaos.<family>.seed
#     version "0.0.0"
# - and the a5uni transform's first-occurrence '<"' doc-string-opener
# injection (a positional literal replace that reads no structure; if a seed
# has no doc string the probe rides a comment line instead).
# Everything structural about a seed (its element blocks, the helper block an
# import-style variant relocates, the collision blocks) is DECLARED by the
# seed author in markers; if a marker lies about its content, the L1 oracle
# admission fails - the format is self-checking downstream.
#
# SEED FILE FORMAT (committed definition - the LAW-57 shape):
#   //== header
#   namespace chaos.<family>.seed        <- exactly this line (rewritten)
#   version "0.0.0"                      <- exactly this line (rewritten/dropped)
#   <optional imports the seed always needs - kept verbatim>
#   //== helper <Name>                   <- 0 or 1; the A2 relocation target;
#   <declarations>                          block id == its declared name
#   //== element <Name>                  <- 1..MAX_ELEMENT_BLOCKS; the ordered
#   <declarations>                          reorderable content
#   //== collision <Name> kind=<k>       <- 0..N; dormant unless an A6/
#   <declarations>                          interaction variant activates one
#
# OUTPUT (deterministic, sorted, byte-stable):
#   <out>/chaos-<family>-<variant>[-p<N>].rosetta
#   namespace chaos.<family>.<variant>[.p<N>]
#   - and, for the A7 order-of-load axis alone, a FILE NAME that sorts before
#     or after every sibling: aaa-chaos-<family>-a7first.rosetta and
#     zz-chaos-<family>-a7last.rosetta (the namespace keeps the chaos.<family>
#     law; SINK_NAME_PREFIXES is the writer's own-output manifest).
#
# GENERATION 2 (v3.2 seat 10, D49 - chaos-1.1.0): the three fresh axes A7 /
# A8 / A9 and the A1xA6 interaction row are content-blind exactly like the
# six of 1.0.0 - a file rename, a block relocation, a leading-whitespace
# rewrite, a comment wrap. Collision-block kinds type/choice/meta/enum/fn are
# A6's rivals (in `.rival`); kind=pkg is A8's rival TYPE (in `.functions`).
# The 22 gen-1 seeds are unedited but for s04's dormant kind=pkg block and
# expand byte-identical (asserted by --check-against over the retired cell
# at the swap).
# The default sink is target/chaos-expander/work/ (charter § 2.1) - this
# script NEVER writes under test-corpus/. The cell is a verified COPY of the
# sink, landed in PR-2.
#
# CAPS (asserted, fail-loud - charter § 2.3):
#   TOTAL generated files  <= 1000
#   element blocks / seed  <= 6   (the emitting-element cap's proxy at the
#                                  .rosetta grain; the golden-count budget is
#                                  measured at pin time and ratified at review)
import io
import os
import sys
import shutil
import hashlib
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from chaoslib import load_tsv  # noqa: E402

LAB = os.path.dirname(os.path.abspath(__file__))
SEEDS_DIR = os.path.join(LAB, 'seeds')
AXES_TSV = os.path.join(LAB, 'axes.tsv')
INTERACTIONS_TSV = os.path.join(LAB, 'interactions.tsv')
DEFAULT_OUT = os.path.join(LAB, '..', '..', 'target', 'chaos-expander', 'work')

TOTAL_FILE_CAP = 1000
MAX_ELEMENT_BLOCKS = 6

NS_SEED_PREFIX = 'namespace chaos.'
NS_SEED_SUFFIX = '.seed'
VERSION_SEED_LINE = 'version "0.0.0"'
VERSION_OUT_LINE = 'version "1.0.0"'
MARKER = '//== '


class SeedFormatError(Exception):
    pass


class Seed(object):
    def __init__(self, family, header_lines, helper, elements, collisions):
        self.family = family              # e.g. 's01'
        self.header_lines = header_lines  # lines AFTER ns+version, verbatim
        self.helper = helper              # (name, [lines]) or None
        self.elements = elements          # ordered [(name, [lines])]
        self.collisions = collisions      # {name: (kind, [lines])}


def parse_seed(path):
    """Split a seed into marker-delimited blocks. Whole-line literal marker
    matching only - no grammar analysis (see the law note above)."""
    fam = os.path.basename(path)
    assert fam.startswith('seed-') and fam.endswith('.rosetta'), path
    family = fam[len('seed-'):-len('.rosetta')]
    text = io.open(path, encoding='utf-8', newline='').read()
    if '\r' in text:
        raise SeedFormatError('%s: seeds must be LF-only (A5 owns encodings)' % fam)
    lines = text.split('\n')

    blocks = []   # (marker_line, [content lines])
    cur = None
    for ln in lines:
        if ln.startswith(MARKER):
            cur = (ln[len(MARKER):].strip(), [])
            blocks.append(cur)
        else:
            if cur is None:
                raise SeedFormatError('%s: content before the first marker' % fam)
            cur[1].append(ln)

    if not blocks or blocks[0][0] != 'header':
        raise SeedFormatError('%s: first block must be //== header' % fam)

    header_raw = blocks[0][1]
    ns_line = NS_SEED_PREFIX + family + NS_SEED_SUFFIX
    if not header_raw or header_raw[0] != ns_line:
        raise SeedFormatError('%s: header line 1 must be exactly %r' % (fam, ns_line))
    if len(header_raw) < 2 or header_raw[1] != VERSION_SEED_LINE:
        raise SeedFormatError('%s: header line 2 must be exactly %r' % (fam, VERSION_SEED_LINE))
    header_rest = header_raw[2:]

    helper = None
    elements = []
    collisions = {}
    for tag, content in blocks[1:]:
        parts = tag.split()
        kind = parts[0]
        if kind == 'helper':
            if helper is not None:
                raise SeedFormatError('%s: at most one helper block' % fam)
            if len(parts) != 2:
                raise SeedFormatError('%s: helper marker needs exactly a name' % fam)
            helper = (parts[1], content)
        elif kind == 'element':
            if len(parts) != 2:
                raise SeedFormatError('%s: element marker needs exactly a name' % fam)
            elements.append((parts[1], content))
        elif kind == 'collision':
            if len(parts) != 3 or not parts[2].startswith('kind='):
                raise SeedFormatError('%s: collision marker: //== collision <Name> kind=<k>' % fam)
            if parts[1] in collisions:
                raise SeedFormatError('%s: duplicate collision block name %r'
                                      % (fam, parts[1]))
            ckind = parts[2][len('kind='):]
            if ckind not in A6_KINDS and ckind != A8_KIND:
                raise SeedFormatError('%s: collision kind %r is not one an axis '
                                      'activates (%s)' % (fam, ckind,
                                                          A6_KINDS + (A8_KIND,)))
            collisions[parts[1]] = (ckind, content)
        else:
            raise SeedFormatError('%s: unknown marker %r' % (fam, tag))

    if not elements:
        raise SeedFormatError('%s: at least one element block required' % fam)
    if len(elements) > MAX_ELEMENT_BLOCKS:
        raise SeedFormatError('%s: %d element blocks > cap %d'
                              % (fam, len(elements), MAX_ELEMENT_BLOCKS))
    return Seed(family, header_rest, helper, elements, collisions)


KNOWN_AXES = {'A1', 'A2', 'A3', 'A4', 'A5', 'A6', 'A7', 'A8', 'A9'}
# Variants this code can emit per axis per seed - axes.tsv's cap column is
# VALIDATED against these (the cq review's SF-2: an SOT the code never reads
# cannot drift-detect the code).
EMITTED_VARIANTS = {'A1': 4, 'A2': 4, 'A3': 3, 'A4': 2, 'A5': 4, 'A6': 3,
                    'A7': 2, 'A8': 1, 'A9': 2}
# The collision-block kinds each activating axis owns (D49): A6 places a
# rival of these kinds in `<ns>.rival`; A8 places a kind=pkg rival TYPE in
# `<ns>.functions`. A block of an unknown kind refuses at parse time.
A6_KINDS = ('type', 'choice', 'meta', 'enum', 'fn')
A8_KIND = 'pkg'
# The writer's own-output manifest (the sink law): every name this script
# emits starts with one of these, so the sink clear can tell its own files
# from a stranger's (the cq review's MF-2) even after the A7 renames.
SINK_NAME_PREFIXES = ('chaos-', 'aaa-chaos-', 'zz-chaos-')


# ---------------------------------------------------------------------------
# Variant assembly

def render(ns, header_rest, body_blocks, version_line=VERSION_OUT_LINE,
           extra_imports=()):
    # Order law (grammar: Import* precedes RosettaQualifiableConfiguration*):
    # expander imports go FIRST, then the seed's header tail - so a seed may
    # end its header with imports of its own followed by `isProduct root ...;`
    # lines and stay grammatical in every variant.
    out = ['namespace ' + ns]
    if version_line is not None:
        out.append(version_line)
    for imp in extra_imports:
        out.append(imp)
    out.extend(header_rest)
    for _name, content in body_blocks:
        out.extend(content)
    text = '\n'.join(out)
    if not text.endswith('\n'):
        text += '\n'
    return text


def variant_files(seed, variant, blocks=None, version_line=VERSION_OUT_LINE,
                  parts=None):
    """One un-split variant (or pre-split parts) -> [(relname, ns, text)]."""
    base_ns = 'chaos.%s.%s' % (seed.family, variant)
    if parts is None:
        body = blocks if blocks is not None else list(seed.elements)
        if seed.helper is not None:
            body = [seed.helper] + body
        fn = 'chaos-%s-%s.rosetta' % (seed.family, variant)
        return [(fn, base_ns, render(base_ns, seed.header_lines, body,
                                     version_line))]
    files = []
    all_ns = ['%s.p%d' % (base_ns, i + 1) for i in range(len(parts))]
    for i, part_blocks in enumerate(parts):
        ns = all_ns[i]
        imports = ['import %s.*' % other for other in all_ns if other != ns]
        fn = 'chaos-%s-%s-p%d.rosetta' % (seed.family, variant, i + 1)
        files.append((fn, ns, render(ns, seed.header_lines, part_blocks,
                                     version_line, extra_imports=imports)))
    return files


# --- A1: declaration-order permutations (6 chosen orders, never N!) --------

def a1_orders(n):
    # FOUR chosen orders (the spec review's MF-1 budget trim: each A1 variant
    # re-emits its seed's FULL golden set, so A1 is the most golden-expensive
    # axis; four orders keep the #413 class measured at ~2/3 the cost - the
    # charter cap is a ceiling of 6, not a target).
    idx = list(range(n))
    orders = [
        list(reversed(idx)),                       # reverse
        idx[1:] + idx[:1],                         # rotate-1
        idx[n // 2:] + idx[:n // 2],               # rotate-half
        idx[0::2] + idx[1::2],                     # evens-then-odds
    ]
    seen, out = set(), []
    for o in orders:
        t = tuple(o)
        if t != tuple(idx) and t not in seen:
            seen.add(t)
            out.append(o)
    return out[:4]


def apply_a1(seed):
    out = []
    for k, order in enumerate(a1_orders(len(seed.elements))):
        blocks = [seed.elements[i] for i in order]
        out.append(('a1o%d' % (k + 1), {'blocks': blocks}))
    return out


# --- A2: import styles (needs the helper block; 4 styles) ------------------

def apply_a2(seed):
    if seed.helper is None:
        return []
    hname = seed.helper[0]
    # 'as' aliasing is upstream-legal ONLY on wildcard imports ("as" statement
    # can only be used with wildcard imports - the l1-run1 finding), and an
    # aliased wildcard does not expose bare names, so a2alias carries a
    # qualified import beside the aliased wildcard.
    styles = [
        ('a2qual', ['import {h}.{n}']),
        ('a2wild', ['import {h}.*']),
        ('a2alias', ['import {h}.* as {n}Ns', 'import {h}.{n}']),
        ('a2dangle', ['import {h}.*',
                      'import chaos.%s.a2dangle.unused.*' % seed.family]),
    ]
    out = []
    for vid, imps in styles:
        out.append((vid, {'import_style': imps, 'helper_name': hname}))
    return out


def a2_files(seed, vid, spec):
    base_ns = 'chaos.%s.%s' % (seed.family, vid)
    helper_ns = base_ns + '.h'
    hname = spec['helper_name']
    imports = [t.format(h=helper_ns, n=hname) for t in spec['import_style']]
    # Auxiliary files render with an EMPTY header tail: a helper/dangling
    # namespace must be fully self-contained (a seed's header may carry
    # qualifiable-config lines that resolve only beside its elements).
    main_fn = 'chaos-%s-%s.rosetta' % (seed.family, vid)
    files = [(main_fn, base_ns,
              render(base_ns, seed.header_lines, list(seed.elements),
                     VERSION_OUT_LINE, extra_imports=imports)),
             ('chaos-%s-%s-h.rosetta' % (seed.family, vid), helper_ns,
              render(helper_ns, [], [seed.helper]))]
    if vid == 'a2dangle':
        dang_ns = base_ns + '.unused'
        files.append(('chaos-%s-%s-u.rosetta' % (seed.family, vid), dang_ns,
                      render(dang_ns, [],
                             [('Unused', ['type %sUnusedT:' % hname,
                                          '    stub string (0..1)'])])))
    return files


# --- A3: namespace splits of identical content (3 shapes) ------------------

def balanced_parts(items, k):
    """k contiguous non-empty parts with balanced sizes (the cq review's
    SF-1: the old ceil-split silently DROPPED a3third for 4-block seeds)."""
    n = len(items)
    if n < k:
        return None
    base, extra = divmod(n, k)
    parts, i = [], 0
    for j in range(k):
        size = base + (1 if j < extra else 0)
        parts.append(items[i:i + size])
        i += size
    return parts


def a3_parts(seed):
    els = list(seed.elements)
    if seed.helper is not None:
        els = [seed.helper] + els
    n = len(els)
    shapes = []
    if n >= 2:
        shapes.append(('a3half', balanced_parts(els, 2)))
        # For n == 2, half and hub would be byte-identical bodies in
        # different namespaces (a free golden cost, no new measurement) -
        # hub therefore requires n >= 3, stated here rather than discovered.
    if n >= 3:
        thirds = balanced_parts(els, 3)
        if thirds is None:
            raise SeedFormatError(
                '%s: a3third declared applicable but unbuildable at %d blocks'
                % (seed.family, n))
        shapes.append(('a3third', thirds))
        shapes.append(('a3hub', [els[:1], els[1:]]))
    return shapes


# --- A4: version declaration variants (2) ----------------------------------

def apply_a4(seed):
    return [('a4none', {'version_line': None}),
            ('a4snap', {'version_line': 'version "1.0.0-SNAPSHOT"'})]


# --- A5: line endings + non-ASCII (byte-level; 4) --------------------------

def a5_transform(vid, text):
    if vid == 'a5crlf':
        return text.replace('\n', '\r\n').encode('utf-8')
    if vid == 'a5bom':
        return b'\xef\xbb\xbf' + text.encode('utf-8')
    if vid == 'a5mixed':
        lines = text.split('\n')
        out = []
        for i, ln in enumerate(lines[:-1]):
            out.append(ln + ('\r\n' if i % 2 == 0 else '\n'))
        out.append(lines[-1])
        return ''.join(out).encode('utf-8')
    if vid == 'a5uni':
        # Non-ASCII in STRINGS/doc strings/comments ONLY (identifiers must
        # stay [a-zA-Z_][a-zA-Z0-9_]* - the lexer's ID rule). The appended
        # probe decls put non-ASCII into a LABEL and a displayName too - the
        # v3.1 F2 class (emitted literals outside 0x20-0x7e). The injected
        # strings are NFC in this source file; the carrier text is NOT
        # normalised (the cq review's NIT-8: whole-text NFC would make this
        # variant silently differ from its siblings on a future non-ASCII
        # seed).
        marked = text.replace('<"', '<"éüµ – ', 1)
        if marked == text:
            marked = '// chaos non-ascii probe: éüµ – 世界\n' + text
        if not marked.endswith('\n'):
            marked += '\n'
        marked += (
            'type A5UniProbe: <"Non-ASCII literal probe – müßig 試験.">\n'
            '    p string (0..1)\n'
            '        [label as "µ–label §7"]\n'
            'enum A5UniProbeEnum: <"displayName probe.">\n'
            '    V displayName "µ–„display“"\n')
        return marked.encode('utf-8')
    raise AssertionError(vid)


A5_IDS = ['a5crlf', 'a5bom', 'a5mixed', 'a5uni']


# --- A6: cross-namespace collision injection (S16/S17 only; <=3) -----------

def a6_blocks(seed):
    """The A6-owned collision blocks (name, kind, content), sorted by name -
    the kind=pkg block is A8's and never a `.rival` (D49)."""
    kinds = [k for k, _c in seed.collisions.values()]
    if len(kinds) != len(set(kinds)):
        raise SeedFormatError(
            '%s: two collision blocks share a kind= (their a6<kind> variant '
            'ids would collide)' % seed.family)
    return [(name, seed.collisions[name][0], seed.collisions[name][1])
            for name in sorted(seed.collisions)
            if seed.collisions[name][0] in A6_KINDS]


def apply_a6(seed):
    out = []
    for name, kind, content in a6_blocks(seed)[:3]:
        out.append(('a6%s' % kind, {'collision': (name, content)}))
    return out


def a6_files(seed, vid, spec):
    base_ns = 'chaos.%s.%s' % (seed.family, vid)
    col_ns = base_ns + '.rival'
    name, content = spec['collision']
    body = list(seed.elements)
    if seed.helper is not None:
        body = [seed.helper] + body
    return [
        ('chaos-%s-%s.rosetta' % (seed.family, vid), base_ns,
         render(base_ns, seed.header_lines, body, VERSION_OUT_LINE,
                extra_imports=['import %s.*' % col_ns])),
        ('chaos-%s-%s-r.rosetta' % (seed.family, vid), col_ns,
         render(col_ns, [], [(name, content)])),
    ]


# --- A7: order-of-load (2; the F10 / seat-4 first-wins class) --------------
# Upstream's first-wins qualifiable-root resolution follows the resource
# PATH (three seat-4 probes; lexical order INFERRED, never measured against
# a name that sorts first). Two slim carriers whose FILE NAMES sort before
# and after every sibling; the namespace keeps the chaos.<family> law.

A7_VARIANTS = [('a7first', 'aaa-'), ('a7last', 'zz-')]


def a7_files(seed):
    files = []
    for vid, prefix in A7_VARIANTS:
        ns = 'chaos.%s.%s' % (seed.family, vid)
        body = [seed.elements[0]]
        if seed.helper is not None:
            body = [seed.helper] + body
        files.append(('%schaos-%s-%s.rosetta' % (prefix, seed.family, vid), ns,
                      render(ns, seed.header_lines, body, VERSION_OUT_LINE)))
    return files


# --- A8: output-package-shaped namespace (1; the seat-3 writer-seam class) --
# The seed's content in chaos.<family>.a8pkg beside its kind=pkg collision
# block - a TYPE named like one of the seed's functions - placed in
# chaos.<family>.a8pkg.functions and wildcard-imported, so the POJO generator
# and the function generator address ONE Java path (the writer seam
# `FUNCTION_PATH_COLLISION` witnessed by a seeded map alone at #624).

def a8_files(seed):
    pkg = [(name, c) for name, (k, c) in sorted(seed.collisions.items())
           if k == A8_KIND]
    if len(pkg) != 1:
        raise SeedFormatError('%s: A8 needs exactly one kind=pkg collision '
                              'block, found %d' % (seed.family, len(pkg)))
    name, content = pkg[0]
    base_ns = 'chaos.%s.a8pkg' % seed.family
    fn_ns = base_ns + '.functions'
    body = list(seed.elements)
    if seed.helper is not None:
        body = [seed.helper] + body
    return [
        ('chaos-%s-a8pkg.rosetta' % seed.family, base_ns,
         render(base_ns, seed.header_lines, body, VERSION_OUT_LINE,
                extra_imports=['import %s.*' % fn_ns])),
        ('chaos-%s-a8pkg-f.rosetta' % seed.family, fn_ns,
         render(fn_ns, [], [(name, content)])),
    ]


# --- A9: lexer trivia (2; the L2 offset key + the seat-8 column anchors) ----
# Content-blind on WHITESPACE and COMMENTS only: a9tabs rewrites every
# leading run of four spaces to a tab (column arithmetic under tabs);
# a9comment wraps the text in hidden-channel tokens - a block-comment banner
# and a line comment BEFORE `namespace` (where the BOM was refused) plus a
# line-comment trailer on every non-empty line (every offset after the first
# shifts). Slim carrier, like A4 / A5.

A9_IDS = ['a9tabs', 'a9comment']


def a9_transform(vid, text):
    lines = text.split('\n')
    if vid == 'a9tabs':
        out = []
        for ln in lines:
            n = len(ln) - len(ln.lstrip(' '))
            out.append('\t' * (n // 4) + ' ' * (n % 4) + ln[n:])
        return '\n'.join(out)
    if vid == 'a9comment':
        out = ['/* chaos a9: a block-comment banner before the first token */',
               '// chaos a9: a line comment before namespace']
        for ln in lines:
            out.append(ln + ' // a9' if ln else ln)
        return '\n'.join(out)
    raise AssertionError(vid)


# ---------------------------------------------------------------------------

def expand_seed(seed, axes_for):
    """All files for one seed: base + OAT deltas per applicable axis."""
    files = []
    files += variant_files(seed, 'base')

    if 'A1' in axes_for:
        for vid, spec in apply_a1(seed):
            files += variant_files(seed, vid, blocks=spec['blocks'])
    if 'A2' in axes_for:
        for vid, spec in apply_a2(seed):
            files += a2_files(seed, vid, spec)
    if 'A3' in axes_for:
        for vid, parts in a3_parts(seed):
            files += variant_files(seed, vid, parts=parts)
    if 'A4' in axes_for:
        # SLIM CARRIER (the MF-1 golden-budget trim): A4 probes the VERSION
        # DECLARATION (N4 - a header/resolution fact), so the variant carries
        # only the helper + the first element block, not the full seed. The
        # full content rides base/A1/A2/A3; a full-content re-emission here
        # bought goldens without buying a new measurement.
        slim = [seed.elements[0]]
        for vid, spec in apply_a4(seed):
            files += variant_files(seed, vid, blocks=slim,
                                   version_line=spec['version_line'])
    if 'A6' in axes_for:
        for vid, spec in apply_a6(seed):
            files += a6_files(seed, vid, spec)
    if 'A7' in axes_for:
        files += a7_files(seed)
    if 'A8' in axes_for:
        files += a8_files(seed)

    out = [(fn, text.encode('utf-8')) for fn, _ns, text in files]

    # A5 last: byte transforms of a SLIM carrier (helper + first element -
    # the MF-1 trim: A5 probes ENCODING/EOL handling, a lexer-level fact a
    # slim carrier proves; the a5uni probe decls ride on top regardless).
    if 'A5' in axes_for:
        _fn, _ns, slim_text = variant_files(
            seed, 'base', blocks=[seed.elements[0]])[0]
        for vid in A5_IDS:
            ns = 'chaos.%s.%s' % (seed.family, vid)
            text = slim_text.replace(
                'namespace chaos.%s.base' % seed.family, 'namespace ' + ns, 1)
            out.append(('chaos-%s-%s.rosetta' % (seed.family, vid),
                        a5_transform(vid, text)))
    if 'A9' in axes_for:
        _fn, _ns, slim_text = variant_files(
            seed, 'base', blocks=[seed.elements[0]])[0]
        for vid in A9_IDS:
            ns = 'chaos.%s.%s' % (seed.family, vid)
            text = slim_text.replace(
                'namespace chaos.%s.base' % seed.family, 'namespace ' + ns, 1)
            out.append(('chaos-%s-%s.rosetta' % (seed.family, vid),
                        a9_transform(vid, text).encode('utf-8')))
    return out


def expand_interactions(seeds_by_family, rows):
    """interactions.tsv rows: pair<TAB>families<TAB>note. Each pair is an
    enumerated, motivated combination (charter § 2.3)."""
    files = []
    for row in rows:
        pair, fams = row[0], row[1].split(',')
        for fam in fams:
            seed = seeds_by_family.get(fam)
            if seed is None:
                raise SeedFormatError('interactions.tsv names unknown family %r' % fam)
            if pair == 'A1xA3':
                orders = a1_orders(len(seed.elements))
                if not orders:
                    continue
                perm = [seed.elements[i] for i in orders[0]]
                permuted = Seed(seed.family, seed.header_lines, seed.helper,
                                perm, seed.collisions)
                shapes = a3_parts(permuted)
                if shapes:
                    vid = 'x13' + shapes[0][0][2:]
                    for fn, ns, text in variant_files(permuted, vid,
                                                      parts=shapes[0][1]):
                        files.append((fn, text.encode('utf-8')))
            elif pair == 'A2xA6':
                if seed.helper is None or not a6_blocks(seed):
                    raise SeedFormatError('%s: A2xA6 needs helper + collision' % fam)
                name, kind, content = a6_blocks(seed)[0]
                base_ns = 'chaos.%s.x26%s' % (seed.family, kind)
                col_ns = base_ns + '.rival'
                body = [seed.helper] + list(seed.elements)
                files.append((
                    'chaos-%s-x26%s.rosetta' % (seed.family, kind),
                    render(base_ns, seed.header_lines, body, VERSION_OUT_LINE,
                           extra_imports=['import %s.%s' % (col_ns, name)]
                           ).encode('utf-8')))
                files.append((
                    'chaos-%s-x26%s-r.rosetta' % (seed.family, kind),
                    render(col_ns, [], [(name, content)]).encode('utf-8')))
            elif pair in ('A3xA6', 'A1xA6'):
                # A3xA6: the split with the rival LAST in p2 (1.0.0 - the D48
                # capture). A1xA6 (D49): the SAME split with the rival FIRST in
                # p2 - is upstream's in-file capture of the E2 class
                # order-dependent? (measured with the rival last alone)
                if not a6_blocks(seed):
                    raise SeedFormatError('%s: %s needs a collision block' % (fam, pair))
                name, kind, content = a6_blocks(seed)[0]
                els = list(seed.elements)
                if seed.helper is not None:
                    els = [seed.helper] + els
                half = (len(els) + 1) // 2
                tag = 'x36' if pair == 'A3xA6' else 'x16'
                base_ns = 'chaos.%s.%s%s' % (seed.family, tag, kind)
                ns1, ns2 = base_ns + '.p1', base_ns + '.p2'
                p2 = (els[half:] + [(name, content)] if pair == 'A3xA6'
                      else [(name, content)] + els[half:])
                files.append((
                    'chaos-%s-%s%s-p1.rosetta' % (seed.family, tag, kind),
                    render(ns1, seed.header_lines, els[:half], VERSION_OUT_LINE,
                           extra_imports=['import %s.*' % ns2]).encode('utf-8')))
                files.append((
                    'chaos-%s-%s%s-p2.rosetta' % (seed.family, tag, kind),
                    render(ns2, seed.header_lines, p2, VERSION_OUT_LINE,
                           extra_imports=['import %s.*' % ns1]).encode('utf-8')))
            else:
                raise SeedFormatError('unknown interaction pair %r' % pair)
    return files


def expand_all():
    seeds = []
    for fn in sorted(os.listdir(SEEDS_DIR)):
        if fn.endswith('.rosetta'):
            seeds.append(parse_seed(os.path.join(SEEDS_DIR, fn)))
    seeds_by_family = dict((s.family, s) for s in seeds)

    # axes.tsv is the SOT and the code must drift-detect it (SF-2): unknown
    # axis ids, unknown families, and a cap below what this code emits all
    # refuse; a cap ABOVE it is a ceiling (the charter's grain).
    applicability = {}
    for lineno, row in load_tsv(AXES_TSV, 4, 'axis'):
        axis, fams, cap = row[0], row[2], row[3]
        if axis not in KNOWN_AXES:
            raise SeedFormatError('axes.tsv:%d: unknown axis %r (this code '
                                  'emits %s)' % (lineno, axis, sorted(KNOWN_AXES)))
        if not cap.isdigit() or int(cap) < EMITTED_VARIANTS[axis]:
            raise SeedFormatError(
                'axes.tsv:%d: cap %s below the %d variants this code emits '
                'for %s - shrink the code with the cap, never past it'
                % (lineno, cap, EMITTED_VARIANTS[axis], axis))
        if fams == '*':
            applicability[axis] = None
        else:
            fam_set = set(fams.split(','))
            unknown = fam_set - set(seeds_by_family)
            if unknown:
                raise SeedFormatError('axes.tsv:%d: unknown families %s'
                                      % (lineno, sorted(unknown)))
            applicability[axis] = fam_set
    missing = KNOWN_AXES - set(applicability)
    if missing:
        raise SeedFormatError('axes.tsv: axes missing entirely: %s'
                              % sorted(missing))

    all_files = []
    for seed in seeds:
        axes_for = set()
        for axis, fams in applicability.items():
            if fams is None or seed.family in fams:
                axes_for.add(axis)
        all_files += expand_seed(seed, axes_for)

    all_files += expand_interactions(seeds_by_family,
                                     [cols for _ln, cols in
                                      load_tsv(INTERACTIONS_TSV, 3, 'interaction')])

    names = [fn for fn, _b in all_files]
    if len(names) != len(set(names)):
        dupes = sorted({n for n in names if names.count(n) > 1})
        raise SeedFormatError(
            'duplicate output names (two variants claim one file - a seed or '
            'axis edit collided): %s' % dupes[:10])
    if len(all_files) > TOTAL_FILE_CAP:
        raise SeedFormatError(
            'CAP EXCEEDED: %d files > %d - an axis or interaction edit '
            'outgrew the charter budget; shrink it, never raise the cap '
            'silently' % (len(all_files), TOTAL_FILE_CAP))
    return sorted(all_files), len(seeds)


def write_out(files, out_dir):
    # The green-tree law is STRUCTURAL, not a default (the spec review's
    # SF-1): this script must never write - and never rmtree - under
    # test-corpus/ or the frozen baseline, whatever --out says. And the
    # sink it DOES clear must look like a sink (the cq review's MF-2: an
    # off-by-one --out must not delete logs/, goldens, or anything this
    # script did not itself write).
    norm = os.path.abspath(out_dir).replace('\\', '/')
    for banned in ('/test-corpus/', '/corpus-baseline-9.83/'):
        if banned in norm + '/':
            raise SeedFormatError(
                'REFUSED: the expander never writes under %s (%s)'
                % (banned.strip('/'), out_dir))
    if os.path.isdir(out_dir):
        strangers = [n for n in sorted(os.listdir(out_dir))
                     if not (n.startswith(SINK_NAME_PREFIXES)
                             and n.endswith('.rosetta'))]
        if strangers:
            raise SeedFormatError(
                'REFUSED: %s holds non-sink content %s - this writer clears '
                'only directories it wrote (%s *.rosetta files only)'
                % (out_dir, strangers[:5], '/'.join(SINK_NAME_PREFIXES)))
        shutil.rmtree(out_dir)
    os.makedirs(out_dir)
    for fn, data in files:
        with open(os.path.join(out_dir, fn), 'wb') as f:
            f.write(data)


def digest(files):
    h = hashlib.sha256()
    for fn, data in files:
        h.update(fn.encode('utf-8'))
        h.update(b'\x00')
        h.update(data)
        h.update(b'\x00')
    return h.hexdigest()[:16]


def main(argv):
    out_dir = DEFAULT_OUT
    check_against = None
    i = 1
    while i < len(argv):
        if argv[i] in ('--out', '--check-against') and i + 1 < len(argv):
            if argv[i] == '--out':
                out_dir = argv[i + 1]
            else:
                check_against = argv[i + 1]
            i += 2
        else:
            raise SystemExit('usage: expand.py [--out DIR] [--check-against DIR]')

    files, n_seeds = expand_all()
    d = digest(files)

    if check_against is not None:
        tmp = tempfile.mkdtemp(prefix='chaos-expand-check-')
        try:
            write_out(files, tmp)
            # Compare .rosetta files only (SF-6): the committed cell may
            # legitimately hold a README/.gitattributes beside the content.
            ref = sorted(n for n in os.listdir(check_against)
                         if n.endswith('.rosetta'))
            got = sorted(n for n in os.listdir(tmp) if n.endswith('.rosetta'))
            if ref != got:
                only_ref = sorted(set(ref) - set(got))
                only_got = sorted(set(got) - set(ref))
                print('CHECK FAIL: file lists differ (%d vs %d); only in '
                      'target: %s; only regenerated: %s'
                      % (len(ref), len(got), only_ref[:10], only_got[:10]))
                return 1
            for fn in ref:
                a = open(os.path.join(check_against, fn), 'rb').read()
                b = open(os.path.join(tmp, fn), 'rb').read()
                if a != b:
                    print('CHECK FAIL: %s differs' % fn)
                    return 1
            print('CHECK OK: %d files byte-identical | digest %s' % (len(ref), d))
            return 0
        finally:
            shutil.rmtree(tmp, ignore_errors=True)

    write_out(files, out_dir)
    print('expanded: %d seeds -> %d files | digest %s | out %s'
          % (n_seeds, len(files), d, out_dir))
    return 0


if __name__ == '__main__':
    sys.exit(main(sys.argv))
