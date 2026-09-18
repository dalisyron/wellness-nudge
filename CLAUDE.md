# CLAUDE.md — wellness-nudge

wellness-nudge is an edge microservice (mim) for the mimOE platform.

## mimOE Runtime Constraints

This is NOT a Node.js project. mimOE runs a lightweight ES5-based JavaScript runtime with critical differences:

- **Stateless execution** — each HTTP request gets an isolated mim instance; all in-memory state is destroyed after response is sent
- **No Node.js built-ins** — `fs`, `path`, `http`, `net`, `crypto`, `child_process`, `process`, `__dirname` are all unavailable
- **No browser globals** — `fetch`, `XMLHttpRequest`, `localStorage` do not exist
- **No `setInterval`** — only `setTimeout` within execution quota; timers terminate when request ends
- **No direct sockets or WebSocket** — outbound HTTP/HTTPS only via `context.http`
- **30-second timeout** — requests exceeding this return 504
- **Storage is key-value only** — `context.storage` provides synchronous KV operations with tag support; no SQL or relational queries

### Available Platform APIs

| API | Purpose |
|-----|---------|
| `context.http` | Outbound HTTP/HTTPS requests |
| `context.storage` | Persistent key-value storage (survives across requests) |
| `context.env` | Environment variables from deployment config |
| `context.info` | Node metadata (device name, OS) |

### Persistence Pattern

Since state is destroyed between requests, any data that must persist must be written to `context.storage`.

## Project Structure

```
src/
  index.js                    # Entry point — initializes swagger middleware
  polyfills.js                # Targeted core-js polyfills for Duktape runtime
  controllers/
    nudgeController.js        # POST /nudge
    historyController.js      # GET /history, PUT /nudges/{id}/feedback, POST /nudges/{id}/clear
    tipsController.js         # GET /tips
    healthController.js       # GET /healthcheck
  processors/
    nudgeProcessor.js         # Sanitize metrics, classify, prompt the model, clean + store the nudge
    nudgePrompt.js            # Prompt construction (rounded plain-language metrics, computed summary, rotating worked examples)
    nudgeText.js              # Post-processing of raw model output
    classifier.js             # Keyword goal classifier (+ LLM variant for batch use)
    tipsProcessor.js          # Groups helpful nudges by recent focus category
  lib/
    mimikContext.js           # mimOE context caching (lazy singleton)
    aiClient.js               # Thin client over the local OpenAI-compatible inference API (mILM on Android)
    storage.js                # context.storage persistence for nudge records
  securityHandlers/
    bearer.js                 # Bearer token validation
config/
  swagger.yml                 # OpenAPI 2.0 spec — single source of truth for endpoints
  default.yml                 # Build/package configuration
  start-example.json          # Example deployment env vars
scripts/
  init.sh                     # One-time setup — install, build, set up mimOE
  start-mimoe.sh              # Start local mimOE instance
  deploy.sh                   # Deploy mim to running mimOE
android/                      # The Android app (Kotlin, Jetpack Compose) that embeds mimOE and bundles this mim
deploy/                       # Packaged .tar for deployment (also copied to android/app/src/main/assets/mims/)
```
## Architecture

**MVC-like pattern:** swagger.yml routes -> controllers -> processors -> domain logic

- Routes are defined declaratively in `config/swagger.yml` and mapped to controller handlers
- Swagger middleware is auto-generated at build time via `@mimik/swagger-mw-codegen`
- The `@mimik/edge-ms-helper` library initializes the mim and wires routing
- Security handler validates Bearer token before controller is invoked

## API Endpoints

Base path: `/wellness-nudge/v1`

| Method | Path | Handler | Auth | Description |
|--------|------|---------|------|-------------|
| GET | `/healthcheck` | `healthController.getHealth` | none | Liveness probe |
| POST | `/nudge` | `nudgeController.createNudge` | Bearer `API_KEY` | Generates, classifies and stores a nudge grounded in the supplied metrics |
| GET | `/history` | `historyController.listHistory` | Bearer `API_KEY` | Newest-first stored nudges (`limit` 1–100) |
| PUT | `/nudges/{id}/feedback` | `historyController.updateFeedback` | Bearer `API_KEY` | `{"helpful": "yes" \| "no" \| "unset"}` |
| POST | `/nudges/{id}/clear` | `historyController.deleteNudge` | Bearer `API_KEY` | Deletes a nudge (POST: `mw.delete` doesn't register on Duktape) |
| GET | `/tips` | `tipsController.getTips` | Bearer `API_KEY` | Helpful nudges grouped by recent focus category |

`/nudge` request body: JSON with any subset of `sleepHours`, `deepSleepPct`, `remSleepPct`, `restingHR`, `hrvMs`, `stepsYesterday`, `userGoal`. At least one is required.

`/nudge` response: JSON with `id`, `ts`, `nudge`, `category`, `userGoal`, `model`, `latencyMs`, `finishReason`, `usage`, `metricsUsed`.

Prompt changes are validated against a rubric (cites a real input number, invents none, two sentences, one concrete action) on the same GGUF under llama.cpp, then on the phone. Keep `temperature` 0.2 and `max_tokens` 80 unless re-validated.

## Quick Start

```bash
./scripts/init.sh          # Install deps, build, set up mimOE
./scripts/start-mimoe.sh   # Start mimOE (runs in foreground)
./scripts/deploy.sh        # Deploy mim (in another terminal)
```

## Build & Deploy

```bash
npm install
npm run build      # prebuild (codegen swagger-mw) + webpack bundle
npm run package    # creates deploy/wellness-nudge-v1-1.0.0.tar
```

- Webpack targets ES5 (web target) with Babel transpilation and Terser minification
- Polyfills are in `src/polyfills.js` — add new polyfills there, not in webpack config
- `useBuiltIns: false` is critical — prevents Babel from expanding targeted imports into 800+ modules
- Output is a single minified `build/index.js`
- The `.tar` archive in `deploy/` is uploaded to mimOE via MCM

## Required Environment Variables

Set via the mim deployment `env` config:

| Variable | Default | Description |
|----------|---------|-------------|
| `API_KEY` | (none) | Bearer token clients must send to `/nudge`. If unset, auth is bypassed (dev only). |
| `INFERENCE_API_KEY` | `1234` | Bearer token used by this mim when calling the local inference endpoint. |
| `INFERENCE_URL_PATH` | `/mimik-ai/openai/v1/chat/completions` | Inference path. The Android app sets `/{clientId}/milm/v1/chat/completions` (mILM). |
| `INFERENCE_MODEL` | `smollm2-360m` | Model ID for the inference call. Must already be provisioned in the local Model Registry. |

## Code Conventions

- ESLint with max line length 180 chars
- **Use `const` and `let`, not `var`** — webpack + Babel transpile ES6+ down to ES5 for the Duktape runtime, so modern syntax is safe in source files. Use `const` by default; use `let` only when reassignment is needed.
- **Prefer arrow functions** — use arrow functions (`=>`) for all functions including callbacks, closures, and module-level functions. Exception: only use traditional `function` when `this` binding or hoisting is specifically needed.
- Authentication is handled by the mim, not the runtime — see `securityHandlers/bearer.js`
- Use `context.storage` for all persistence; never rely on in-memory state across requests
- Use `context.http` for all outbound HTTP calls; never use `fetch` or Node HTTP modules
- Do not manually `JSON.stringify` request bodies for `context.http` — it auto-converts objects to JSON
