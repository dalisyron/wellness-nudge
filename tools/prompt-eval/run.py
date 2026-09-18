"""Generate nudges for the evaluation inputs, score each one, and append every call to a JSONL log.

Messages and the output cleanup come from the mim's own code (src/processors) through bridge.js, so this
always measures what /nudge sends and what the app shows.

usage: python3 run.py [--set core|edge|demo|all] [--only ID,...] [--runs N] [--variants 0,1,2]
                      [--prompt final|baseline] [--temperature T] [--max-tokens N]
                      [--base-url URL] [--model ID] [--out FILE]
"""
import argparse
import json
import os
import subprocess
import sys
import time

import checker
import client
import inputs
import summarize

HERE = os.path.dirname(os.path.abspath(__file__))

# (temperature, max_tokens) each prompt shipped with: nudgeProcessor.js now, aiClient.js defaults before
SETTINGS = {'final': (0.2, 80), 'baseline': (0.4, 200)}


def bridge(op, items):
    """Run bridge.js on a list of requests and return its list of results."""
    p = subprocess.run(['node', os.path.join(HERE, 'bridge.js'), op], input=json.dumps(items),
                       capture_output=True, encoding='utf-8')
    if p.returncode != 0:
        sys.exit('bridge.js %s failed:\n%s' % (op, p.stderr))
    return json.loads(p.stdout)


def main():
    ap = argparse.ArgumentParser(description='Evaluate the nudge prompt against a chat endpoint.')
    ap.add_argument('--set', default='core', choices=sorted(inputs.SETS), help='input set (default core)')
    ap.add_argument('--only', help='comma-separated input ids instead of a set')
    ap.add_argument('--runs', type=int, default=1, help='calls per input and variant (default 1)')
    ap.add_argument('--variants', default='0,1,2', help='worked-example variants to force (default 0,1,2)')
    ap.add_argument('--prompt', default='final', choices=sorted(SETTINGS),
                    help='final = src/processors/nudgePrompt.js, baseline = the pre-rewrite prompt')
    ap.add_argument('--temperature', type=float, help='default: what the prompt shipped with (0.2 final, 0.4 baseline)')
    ap.add_argument('--max-tokens', type=int, help='default: what the prompt shipped with (80 final, 200 baseline)')
    ap.add_argument('--base-url', default=client.BASE_URL, help='server root (default $EVAL_BASE_URL or %(default)s)')
    ap.add_argument('--model', default=client.MODEL, help='model id (default $EVAL_MODEL or %(default)s)')
    ap.add_argument('--out', help='JSONL log to append to (default runs/<time>-<prompt>-<set>.jsonl)')
    a = ap.parse_args()

    if a.only:
        by_id = {t['id']: t for t in inputs.SETS['all']}
        unknown = [i for i in a.only.split(',') if i not in by_id]
        if unknown:
            sys.exit('unknown input ids: %s' % ', '.join(unknown))
        tests = [by_id[i] for i in a.only.split(',')]
    else:
        tests = inputs.SETS[a.set]
    # the baseline has no worked examples, so there is nothing to vary
    variants = [None] if a.prompt == 'baseline' else [int(v) for v in a.variants.split(',')]
    temperature = a.temperature if a.temperature is not None else SETTINGS[a.prompt][0]
    max_tokens = a.max_tokens or SETTINGS[a.prompt][1]
    out = a.out or os.path.join(HERE, 'runs', '%s-%s-%s.jsonl' % (
        time.strftime('%Y%m%d-%H%M%S'), a.prompt, 'only' if a.only else a.set))

    # every (input, variant) is built once; runs are the outer loop so any drift hits all inputs alike
    pairs = [(t, v) for t in tests for v in variants]
    built = bridge('messages', [{'metrics': inputs.metrics_of(t), 'variant': v, 'prompt': a.prompt} for t, v in pairs])
    jobs = [(run, t, v, b) for run in range(a.runs) for (t, v), b in zip(pairs, built)]

    if not client.wait_until_up(a.base_url):
        sys.exit('cannot reach %s: start llama-server (see README) or set --base-url / EVAL_BASE_URL' % a.base_url)
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    print('%d call%s: %s prompt, %s at %s, model %s, temperature %s, max_tokens %d' % (
        len(jobs), '' if len(jobs) == 1 else 's', a.prompt, client.backend(), a.base_url, a.model, temperature, max_tokens))

    recs, failed_in_a_row, width = [], 0, len(str(len(jobs)))
    for i, (run, t, variant, b) in enumerate(jobs, 1):
        m = inputs.metrics_of(t)
        res = client.chat(b['messages'], a.base_url, a.model, temperature, max_tokens)
        text = ''
        if res['ok']:
            text = bridge('clean', [{'raw': res['content'], 'finishReason': res['finish_reason'], 'metrics': m}])[0]
        s = checker.score(text, m)
        chk = s['check']
        rec = {
            'ts': time.strftime('%Y-%m-%dT%H:%M:%S'), 'prompt': a.prompt, 'backend': client.backend(),
            'base_url': a.base_url, 'model': a.model, 'temperature': temperature, 'max_tokens': max_tokens,
            'input': t['id'], 'set': inputs.SET_OF[t['id']], 'variant': variant, 'run': run,
            'category': b['category'], 'metrics': m, 'messages': b['messages'],
            'ok': res['ok'], 'error': res['error'], 'raw': res['content'], 'finish_reason': res['finish_reason'],
            'usage': res['usage'], 'served_model': res['model'], 'latency_s': res['latency_s'],
            'text': text, 'rubric_pass': s['rubric'], 'semantic_pass': s['semantic'], 'why': s['why'],
            'semantic_flags': s['semantic_flags'], 'soft_flags': chk['soft_flags'], 'n_words': chk['n_words'],
            'cited': [(c['n'], c['metric']) for c in chk['cited']],
            'bad_numbers': [(x['n'], x['kind']) for x in chk['bad_numbers']],
        }
        with open(out, 'a', encoding='utf-8') as f:
            f.write(json.dumps(rec, ensure_ascii=False) + '\n')
        recs.append(rec)

        if not res['ok']:
            failed_in_a_row += 1
            print('[%*d/%d] %s call failed: %s' % (width, i, len(jobs), t['id'], res['error']), flush=True)
            if failed_in_a_row == 3:
                sys.exit('3 calls in a row failed; stopping. Log so far: %s' % out)
            continue
        failed_in_a_row = 0
        verdict = 'PASS' if s['semantic'] else 'FAIL (%s)' % ', '.join(s['why'])
        print('[%*d/%d] %-19s v%s r%d %5.2fs %s | %s' % (width, i, len(jobs), t['id'], '-' if variant is None else variant,
                                                       run, res['latency_s'], verdict, text), flush=True)

    print()
    summarize.report(recs)
    print('\nlog: %s' % os.path.relpath(out))


if __name__ == '__main__':
    main()
