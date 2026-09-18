const aiClient = require('../lib/aiClient');
const storage = require('../lib/storage');
const classifier = require('./classifier');
const { buildMessages } = require('./nudgePrompt');
const { cleanText } = require('./nudgeText');

// Tuned for SmolLM2-360M: at 0.2 the model keeps the numbers and the
// two-sentence shape; nudges are ~45 tokens, so 80 leaves headroom without
// letting a runaway reply eat the 30 s request budget.
const DEFAULT_TEMPERATURE = 0.2;
const DEFAULT_MAX_TOKENS = 80;

const KNOWN_FIELDS = ['sleepHours', 'deepSleepPct', 'remSleepPct', 'restingHR', 'hrvMs', 'stepsYesterday', 'userGoal'];

const sanitizeMetrics = (raw) => {
  if (!raw || typeof raw !== 'object') {
    const err = new Error('Request body must be a JSON object of biometric metrics.');
    err.status = 400;
    throw err;
  }
  const out = {};
  KNOWN_FIELDS.forEach((key) => {
    if (raw[key] !== undefined && raw[key] !== null) {
      out[key] = raw[key];
    }
  });
  if (Object.keys(out).length === 0) {
    const err = new Error(`At least one metric is required. Recognized fields: ${KNOWN_FIELDS.join(', ')}.`);
    err.status = 400;
    throw err;
  }
  return out;
};

const createNudge = ({ metrics, model, temperature, maxTokens }) => {
  const clean = sanitizeMetrics(metrics);
  const userGoal = clean.userGoal || '';

  // Keyword classifier returns synchronously. Tip lookup and any LLM-backed
  // reclassification live behind /tips — they don't block the live nudge.
  return classifier.classify({ userGoal })
    .catch(() => 'other')
    .then((category) => {
      const messages = buildMessages(clean, category);
      // Wall-clock time of the on-device inference call only (excludes the
      // client's HTTP hop), so the app can show "generated in 4.2 s".
      const startedAt = Date.now();
      return aiClient.chat(messages, {
        model,
        temperature: temperature !== undefined ? temperature : DEFAULT_TEMPERATURE,
        maxTokens: maxTokens || DEFAULT_MAX_TOKENS,
      })
        .then((completion) => ({ category, completion, latencyMs: Date.now() - startedAt }));
    })
    .then(({ category, completion, latencyMs }) => {
      const choice = completion && completion.choices && completion.choices[0];
      const text = choice && choice.message && choice.message.content;
      if (!text || !text.trim()) {
        const err = new Error('Empty response from inference service.');
        err.status = 502;
        throw err;
      }

      const nudgeText = cleanText(text, choice.finish_reason, clean);
      const ts = Date.now();
      const id = storage.newId(ts);

      const record = {
        id,
        ts,
        metrics: clean,
        userGoal,
        category,
        nudge: nudgeText,
        model: completion.model,
        latencyMs,
        helpful: 'unset',
      };

      // Best-effort save — never fail the user-facing response on a storage error.
      return storage.saveNudge(record)
        .catch(() => null)
        .then(() => ({
          id,
          ts,
          nudge: nudgeText,
          category,
          userGoal,
          model: completion.model,
          latencyMs,
          finishReason: choice.finish_reason,
          usage: completion.usage,
          metricsUsed: clean,
        }));
    });
};

module.exports = { createNudge };
