// Bridge from the Python harness to the mim's own prompt code, so the evaluation always measures
// what /nudge sends and what the app shows. Reads a JSON array on stdin, writes a JSON array:
//
//   node bridge.js messages  [{ metrics, variant, prompt }]   ->  [{ category, messages }]
//   node bridge.js clean     [{ raw, finishReason, metrics }]  ->  ["cleaned nudge", ...]
//
// messages: category from the keyword classifier, messages from buildMessages, as in
//   nudgeProcessor.createNudge. `variant` forces the worked example (0, 1, 2); null lets the clock
//   pick, as production does. prompt 'baseline' builds the pre-rewrite prompt instead.
// clean: the production cleanText applied to the raw model output.
const classifier = require('../../src/processors/classifier');
const { buildMessages } = require('../../src/processors/nudgePrompt');
const { cleanText } = require('../../src/processors/nudgeText');
const baseline = require('./baselinePrompt');

const OPS = {
  messages: (reqs) => Promise.all(reqs.map((r) => {
    const metrics = r.metrics || {};
    return classifier.classify({ userGoal: metrics.userGoal || '' })
      .catch(() => 'other')
      .then((category) => ({
        category,
        messages: r.prompt === 'baseline' ? baseline.buildMessages(metrics) : buildMessages(metrics, category, r.variant),
      }));
  })),
  clean: (reqs) => Promise.resolve(reqs.map((r) => cleanText(r.raw, r.finishReason, r.metrics))),
};

const op = OPS[process.argv[2]];
if (!op) {
  process.stderr.write('usage: node bridge.js messages|clean < requests.json\n');
  process.exit(2);
}

let input = '';
process.stdin.setEncoding('utf8');
process.stdin.on('data', (chunk) => {
  input += chunk;
});
process.stdin.on('end', () => {
  Promise.resolve()
    .then(() => op(JSON.parse(input)))
    .then((out) => process.stdout.write(JSON.stringify(out)))
    .catch((err) => {
      process.stderr.write(`${err.stack || err}\n`);
      process.exit(1);
    });
});
