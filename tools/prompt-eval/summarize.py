"""Summarize JSONL logs written by run.py. Each stored nudge is re-scored with the current checker, so
older logs stay comparable after a rubric change.

usage: python3 summarize.py LOG.jsonl [LOG.jsonl ...] [--by input|variant|set] [--show] [--fails]

  rubric     cites a real input number, invents none, 1-2 sentences / 12-50 words, no forbidden pattern
  +semantic  the rubric and no hard semantic flag (wrong reading of the numbers, goal contradiction, ...)
  cites / numbers / length / form   share of nudges passing each part of the rubric
"""
import argparse
import json
import statistics
from collections import Counter, OrderedDict

import checker

ROW = '%-8s %-12s %-13s %4s %4s %s| %4s %6s %7s %6s %6s | %7s %9s | %s'


def load(paths):
    recs = []
    for path in paths:
        with open(path, encoding='utf-8') as f:
            recs += [json.loads(line) for line in f if line.strip()]
    return recs


def pct(k, n):
    return '%.1f%%' % (100.0 * k / n) if n else '-'


def rate(scores, test):
    return pct(sum(1 for s in scores if test(s)), len(scores))


def report(recs, by=None, show=False, fails=False):
    """One row per prompt / backend / model / sampling (and per `by` value), then why nudges failed."""
    groups = OrderedDict()
    for r in recs:
        key = (r['prompt'], r['backend'], r['model'], r['temperature'], r['max_tokens'], r.get(by) if by else '')
        groups.setdefault(key, []).append(r)

    def col(v):  # the --by column, only when grouping
        return '%-19s ' % str(v)[:19] if by else ''

    hdr = ROW % ('prompt', 'backend', 'model', 'temp', 'maxT', col(by), 'n', 'cites', 'numbers', 'length', 'form',
                 'rubric', '+semantic', 'latency (median)')
    print(hdr)
    print('-' * len(hdr))
    failures = []
    for key, rs in groups.items():
        ok = [r for r in rs if r['ok']]
        for r in ok:
            r['_score'] = checker.score(r['text'], r['metrics'])
        scores = [r['_score'] for r in ok]
        lat = [r['latency_s'] for r in ok if r['latency_s'] is not None]
        print(ROW % (key[0], key[1], key[2][:13], '%.2f' % key[3], key[4], col(key[5]), len(ok),
                     rate(scores, lambda s: s['check']['cites']), rate(scores, lambda s: s['check']['numbers_ok']),
                     rate(scores, lambda s: s['check']['length_ok']), rate(scores, lambda s: s['check']['forbidden_ok']),
                     rate(scores, lambda s: s['rubric']), rate(scores, lambda s: s['semantic']),
                     '%.2f s' % statistics.median(lat) if lat else '-')
              + ('   (%d failed calls)' % (len(rs) - len(ok)) if len(rs) != len(ok) else ''))
        why = Counter(w for s in scores for w in s['why'])
        if why:
            failures.append((key, why))

    print('\nwhy nudges failed:' + ('' if failures else ' none'))
    for key, why in failures:
        print('  %-40s %s' % (' '.join(str(k) for k in key if k != ''), ', '.join('%s %d' % kv for kv in why.most_common())))

    if show:
        for key, rs in groups.items():
            print('\n=== %s' % ' / '.join(str(k) for k in key if k != ''))
            for r in sorted((r for r in rs if r['ok']), key=lambda r: (r['input'], str(r['variant']), r['run'])):
                s = r['_score']
                if fails and s['semantic']:
                    continue
                verdict = 'PASS' if s['semantic'] else 'FAIL (%s)' % ', '.join(s['why'])
                print('%-19s v%s r%d %s\n    %s' % (r['input'], '-' if r['variant'] is None else r['variant'], r['run'],
                                                  verdict, r['text']))


def main():
    ap = argparse.ArgumentParser(description='Summarize run.py logs.')
    ap.add_argument('logs', nargs='+')
    ap.add_argument('--by', choices=['input', 'variant', 'set'], help='one row per input, variant or input set')
    ap.add_argument('--show', action='store_true', help='print every nudge with its verdict')
    ap.add_argument('--fails', action='store_true', help='with --show: only nudges that fail')
    a = ap.parse_args()
    report(load(a.logs), a.by, a.show, a.fails)


if __name__ == '__main__':
    main()
