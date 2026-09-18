# Nudge prompt evaluation

Measures how often SmolLM2-360M, prompted the way `/nudge` prompts it, writes a nudge that passes an
automatic rubric. `bridge.js` builds every request with the mim's own `classifier.js` and
`nudgePrompt.js`, and cleans every reply with `nudgeText.js`. The harness therefore scores exactly
what `/nudge` sends and what the app shows, and there is no second copy of the prompt to drift. It
needs Python 3 and Node, and no packages.

## The rubric

`checker.py` passes a nudge when all of these hold:

- **It cites a real input number** as the prompt shows it (5.2 hours, 29 ms, 3,400 steps). A goal-only
  request has nothing to cite.
- **It invents none.** No number is wrong, pinned on the wrong metric, or about a metric the request
  doesn't have. Action numbers ("a 10-minute walk"), clock times and numbers from the goal ("10K")
  are fine, but a made-up percentage is not.
- **1–2 sentences, 12–50 words.**
- **No preamble, lists or medical wording.** That rules out "Here's your nudge" or "Sure", greetings,
  lists, markdown, a "Nudge:" label, medical terms (doctor, diagnosis, insomnia, medication,
  supplements, ...), leftover runtime tokens, AI-isms, echoed data labels ("HRV:"), extra lines and a
  cut-off ending.

The **semantic checks** are heuristics on top of that. They catch a wrong reading of the numbers
(upbeat after a short night or low HRV, or the reverse), a contradiction of a cut-back goal (coffee
advice for "drink less coffee"), a nap for a sleep goal, a long nap, garbled or repeated phrases, hard
training or "push harder" after short sleep or low HRV, an HRV word that contradicts the value, and
caffeine late in the day. `rubric` counts nudges that pass the rubric; `+semantic` counts those that
also trip none of these checks. Nudges are scored after the production `cleanText`, on the text the
app would show. The baseline is scored the same way.

## Files

| File | What it does |
|---|---|
| `run.py` | Runs a set of inputs against a server, scores every nudge, logs each call as a JSONL line, prints a summary |
| `summarize.py` | Re-scores logs with the current checker, with rates per prompt, input, variant or set, failure reasons and `--show` |
| `checker.py` | The rubric and the semantic checks |
| `inputs.py` | The core set (9 goals), edge cases (8: goal only, float noise, strings and zeros, single fields) and the 4 demo scenarios |
| `client.py` | Minimal OpenAI-compatible chat client for llama-server or mILM |
| `bridge.js` | Node bridge to the mim's classifier, prompt builder and `cleanText` |
| `baselinePrompt.js` | The prompt from before the rewrite, frozen so the baseline can be measured again |

## On a laptop: llama.cpp

mILM on the phone runs `smollm2-360m-instruct-q8_0.gguf`, and llama.cpp's `llama-server` runs the
same file. Get llama.cpp (`brew install llama.cpp`, or a release build from
github.com/ggml-org/llama.cpp), then:

```bash
curl -L -o smollm2-360m-instruct-q8_0.gguf \
  https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q8_0.gguf
llama-server -m smollm2-360m-instruct-q8_0.gguf --jinja --host 127.0.0.1 --port 8080 -c 4096 -np 1
```

`--jinja` applies the chat template stored in the GGUF, and `-np 1` serves one request at a time,
as mILM does. On Apple silicon the first start can take about a minute while Metal compiles its
shaders. `-ngl 0` runs on the CPU instead.

Then, from this directory:

```bash
python3 run.py --set demo --runs 2                      # 4 demo scenarios x 3 example variants x 2 runs
python3 run.py --set core --runs 5                      # 9 goals x 3 variants x 5 runs
python3 run.py --set edge --runs 5                      # goal only, float noise, strings and zeros, single fields
python3 run.py --set core --runs 10 --prompt baseline   # the prompt from before the rewrite
python3 summarize.py runs/*.jsonl --by set              # add --show --fails to read what failed
```

Each prompt runs with the sampling it shipped with unless `--temperature` or `--max-tokens` overrides
it. The current prompt gets temperature 0.2 and max_tokens 80 (from `nudgeProcessor.js`); the baseline
gets 0.4 and 200 (the old `aiClient` defaults). Each category has three worked-example variants, which
production rotates by the clock. `--variants` forces particular ones (default `0,1,2`), and `--only`
picks inputs by id. Logs go to `runs/`, which git ignores. Each line holds the request (metrics,
category, messages), the raw and cleaned reply, latency, token usage and the verdicts.

## On the phone: mILM through adb

The app must be running on a phone connected over USB. On first launch it deploys mILM and downloads
the model.

```bash
adb forward tcp:18083 tcp:8083             # mimOE listens on 8083 on the phone by default
export EVAL_BASE_URL=http://127.0.0.1:18083
export MILM_CLIENT_ID=...                  # your mimik client ID (the mimik.clientId the app is built with)
export MILM_API_KEY=...                    # the key the app gives mILM (setMilmApiKey in EdgeRuntime.kt)
python3 run.py --set demo --runs 1
```

When `MILM_CLIENT_ID` is set, requests go to `/<client id>/milm/v1/chat/completions`, with the key as
a bearer token. Both values are read only from the environment and are never printed or logged. Keep
them out of the repo. mILM serves one request at a time, so leave the app idle during a run.

## Results

These are logged runs on the llama.cpp bench (llama-server build 11026 on an Apple-silicon Mac, with
the same GGUF as the phone):

| Prompt | Inputs | Temperature / max_tokens | n | Rubric | +semantic |
|---|---|---|---:|---:|---:|
| Before the rewrite (rules + raw JSON) | core set, 10 runs each | 0.4 / 200 | 90 | 2% | 1% |
| Current (`src/processors/nudgePrompt.js`) | 83 inputs with metrics¹, variants 0–2 | 0.2 / 80 | 2,107 | 99.6% | 98.9% |
| Current | 10 goal-only requests, variants 0–2 | 0.2 / 80 | 192 | 100% | 99.5% |

¹ These inputs are the core set, the app's 30 sample days, edge cases, wake-up and "other" goals,
physiology probes and a supplementary set. 26 of the runs (T9_partial, "Move more today") were built
with an older copy of the keyword classifier, which sent that goal to the generic example. Production
now routes it to fitness. Without those runs, n is 2,081 and the rates are the same.

The lower temperature is not what made the difference. At the baseline's 0.4, an early version of the
rewrite (one fixed example per category) already passed 92% on the same core set (n = 90).

The pass rates come from the llama.cpp bench with the same GGUF the phone runs. The four demo
scenarios were spot-checked on a Pixel 9 Pro XL through mILM.

A re-run with this harness on the same build passed the rubric on 135/135 core-set runs, 166/168
edge-case runs (two made-up "10%" figures) and 48/48 demo runs. The baseline passed 2 of 90.
