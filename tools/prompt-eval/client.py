"""Minimal chat client (stdlib only) for llama.cpp's llama-server or the phone's mILM.

Both speak the OpenAI chat completions API; they differ in path and auth:
  llama-server  POST <base>/v1/chat/completions                    no auth
  mILM          POST <base>/<client id>/milm/v1/chat/completions   Authorization: Bearer <key>

Environment (run.py's --base-url and --model override the first two):
  EVAL_BASE_URL   server root, default http://127.0.0.1:8080 (llama-server's default port)
  EVAL_MODEL      model id sent with each request, default smollm2-360m
  MILM_CLIENT_ID  the app's mimik client ID; set it to talk to mILM
  MILM_API_KEY    bearer token, sent when set
The client ID and the key are never printed or logged; errors are scrubbed of both.
"""
import json
import os
import time
import urllib.error
import urllib.request

BASE_URL = os.environ.get('EVAL_BASE_URL', 'http://127.0.0.1:8080')
MODEL = os.environ.get('EVAL_MODEL', 'smollm2-360m')


def _client_id():
    return os.environ.get('MILM_CLIENT_ID', '').strip()


def _api_key():
    return os.environ.get('MILM_API_KEY', '').strip()


def backend():
    return 'milm' if _client_id() else 'llama-server'


def _scrub(s):
    for secret in (_client_id(), _api_key()):
        if secret:
            s = s.replace(secret, '<redacted>')
    return s


def _chat_url(base_url):
    base = base_url.rstrip('/')
    return '%s/%s/milm/v1/chat/completions' % (base, _client_id()) if _client_id() else base + '/v1/chat/completions'


def wait_until_up(base_url, max_wait=300):
    """True once the server answers. llama-server's /health says 503 while the model loads, so keep
    polling through that; a connection error fails at once."""
    url = base_url.rstrip('/') + ('/' if _client_id() else '/health')
    deadline = time.time() + max_wait
    while True:
        try:
            with urllib.request.urlopen(url, timeout=10):
                return True
        except urllib.error.HTTPError as e:
            if e.code != 503:
                return True          # any other HTTP answer means the server is up
            if time.time() > deadline:
                return False
        except Exception:
            return False
        time.sleep(3)


def chat(messages, base_url, model, temperature, max_tokens, timeout=90, retries=2):
    """One non-streamed completion: ok, content, finish_reason, usage, model, latency_s, error."""
    body = json.dumps({'model': model, 'messages': messages, 'temperature': temperature,
                       'max_tokens': max_tokens}).encode('utf-8')
    headers = {'Content-Type': 'application/json'}
    if _api_key():
        headers['Authorization'] = 'Bearer ' + _api_key()
    error = None
    for attempt in range(retries + 1):
        if attempt:
            time.sleep(2 * attempt)
        req = urllib.request.Request(_chat_url(base_url), data=body, headers=headers, method='POST')
        t0 = time.time()
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                data = json.loads(resp.read().decode('utf-8', 'replace'))
            choice = (data.get('choices') or [{}])[0]
            # llama-server names the model by its file path; keep just the file name
            served = _scrub(os.path.basename(str(data.get('model') or ''))) or None
            return {'ok': True, 'content': (choice.get('message') or {}).get('content') or '',
                    'finish_reason': choice.get('finish_reason'), 'usage': data.get('usage'),
                    'model': served, 'latency_s': round(time.time() - t0, 3), 'error': None}
        except urllib.error.HTTPError as e:
            try:
                detail = e.read().decode('utf-8', 'replace')[:300]
            except Exception:
                detail = ''
            error = 'HTTP %s %s' % (e.code, detail)
            if 400 <= e.code < 500 and e.code not in (408, 429):
                break                # a wrong path or key will not fix itself
        except Exception as e:  # connection refused, timeout, bad JSON
            error = '%s: %s' % (type(e).__name__, e)
    return {'ok': False, 'content': '', 'finish_reason': None, 'usage': None, 'model': None,
            'latency_s': None, 'error': _scrub(error)}
