# wellness-nudge

An edge microservice (mim) for the [mimOE](https://mimik.com/) platform that
generates short, personalized wellness nudges from on-device biometric
metrics (sleep, HRV, resting HR, steps, etc.) using a small local LLM.

A companion Android app (`android/`) collects metrics and renders nudges
on-device. The whole stack — inference, prompt assembly, history, and the
mim itself — runs locally on the phone via mimOE.

## Components

| Path | What it is |
|------|------------|
| `src/`, `config/`, `scripts/` | The mim: a swagger-driven microservice that runs inside mimOE |
| `android/` | Android client app that calls the local mim |
| `deploy/` | Packaged `.tar` ready to upload to a mimOE node |

## API

Base path: `/wellness-nudge/v1`

- `GET /healthcheck` — liveness probe
- `POST /nudge` — accepts biometric metrics, returns a generated nudge
  grounded in those metrics. Bearer-auth'd by `API_KEY`.

See [`CLAUDE.md`](./CLAUDE.md) for the full architecture, runtime
constraints, env vars, and conventions.

## Quick start

```bash
./scripts/init.sh          # install deps, build, set up mimOE
./scripts/start-mimoe.sh   # start mimOE (foreground)
./scripts/deploy.sh        # deploy the mim (in another terminal)
```

## Build

```bash
npm install
npm run build      # codegen swagger middleware + webpack bundle
npm run package    # produces deploy/wellness-nudge-v1-1.0.0.tar
```

The runtime is the ES5-based Duktape engine inside mimOE — not Node.js.
Webpack + Babel transpile modern JS down to ES5; targeted polyfills live
in `src/polyfills.js`.
