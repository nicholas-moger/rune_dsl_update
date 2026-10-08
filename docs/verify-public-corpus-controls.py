"""Failure controls for docs/verify-public-corpus.py: prove each classification can fire and that
`rawBytesIdentical` is exact byte equality. Runs on synthetic files in a temporary directory; touches
no corpus, manifest or baseline. Exit 0 when every control behaves; exit 1 otherwise.
Usage: python3 docs/verify-public-corpus-controls.py
"""
import importlib.util
import sys
import tempfile
from pathlib import Path


def load_verifier():
    spec = importlib.util.spec_from_file_location(
        'verifier', Path(__file__).resolve().parent / 'verify-public-corpus.py')
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main():
    sys.dont_write_bytecode = True
    v = load_verifier()
    retained = b'/** A B */\r\npackage x;\r\n'        # the retained (CRLF) form of an order-dependent file
    witnessed_alt = b'/** B A */\r\npackage x;\r\n'   # a second form an independent released-generator run produced
    unwitnessed = b'/** C */\r\npackage x;\r\n'       # a form no released-generator run produced
    plain = b'class Plain {}\r\n'                     # a deterministic file
    lf = lambda b: b.replace(b'\r\n', b'\n')
    expected = {'order.java': (v.sha(retained), v.sha(lf(retained))),
                'plain.java': (v.sha(plain), v.sha(lf(plain)))}
    witnessed = {'order.java': [(0, v.sha(retained), v.sha(lf(retained)), 'R', True),
                                (1, v.sha(witnessed_alt), v.sha(lf(witnessed_alt)), 'W1', False)]}
    cases = [
        ('retained bytes', {'order.java': retained, 'plain.java': plain},
         lambda r: r['identical'] == 2 and r['rawBytesIdentical'] and r['passed']),
        ('LF-converted retained file', {'order.java': lf(retained), 'plain.java': plain},
         lambda r: r['identical'] == 1 and r['lineEndingOnly'] == 1
         and not r['rawBytesIdentical'] and r['passed']),
        ('witnessed alternative ordering', {'order.java': witnessed_alt, 'plain.java': plain},
         lambda r: len(r['acceptedOrdering']) == 1 and not r['acceptedOrdering'][0]['lineEndingDiffers']
         and not r['rawBytesIdentical'] and r['passed']),
        ('LF form of the witnessed alternative', {'order.java': lf(witnessed_alt), 'plain.java': plain},
         lambda r: len(r['acceptedOrdering']) == 1 and r['acceptedOrdering'][0]['lineEndingDiffers']
         and not r['rawBytesIdentical'] and r['passed']),
        ('unwitnessed ordering', {'order.java': unwitnessed, 'plain.java': plain},
         lambda r: r['unverifiedOrdering'] == ['order.java'] and not r['rawBytesIdentical']
         and not r['passed']),
        ('changed deterministic content', {'order.java': retained, 'plain.java': plain + b'//\r\n'},
         lambda r: r['different'] == ['plain.java'] and not r['rawBytesIdentical'] and not r['passed']),
        ('missing file', {'order.java': retained},
         lambda r: r['missing'] == ['plain.java'] and not r['passed']),
        ('extra file', {'order.java': retained, 'plain.java': plain, 'stray.java': plain},
         lambda r: r['extra'] == ['stray.java'] and r['identical'] == 2
         and not r['rawBytesIdentical'] and not r['passed']),
    ]
    failures = 0
    with tempfile.TemporaryDirectory() as tmp:
        base = Path(tmp)
        for label, files, check in cases:
            for name in set(expected) | set(files):
                target = base / name
                if name in files:
                    target.write_bytes(files[name])
                elif target.exists():
                    target.unlink()
            result = v.compare(base, expected, sorted(files), witnessed)
            ok = check(result)
            failures += not ok
            print(('PASS ' if ok else 'FAIL ') + label
                  + ': identical=%d lineEndingOnly=%d accepted=%d unverified=%d different=%d missing=%d '
                    'extra=%d rawBytesIdentical=%s passed=%s'
                  % (result['identical'], result['lineEndingOnly'], len(result['acceptedOrdering']),
                     len(result['unverifiedOrdering']), len(result['different']), len(result['missing']),
                     len(result['extra']), result['rawBytesIdentical'], result['passed']))
    print('controls failed:', failures)
    return 1 if failures else 0


if __name__ == '__main__':
    sys.exit(main())
