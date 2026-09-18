const aiClient = require('../lib/aiClient');
const storage = require('../lib/storage');
const classifier = require('./classifier');

// humanLabel moved to processors/tipsProcessor.js — used to be needed for
// the inline "byTheWay" subtitle on /nudge; that's now a dedicated /tips
// endpoint, so keep the mapping with the consumer.

const SYSTEM_PROMPT = [
  'You are a careful wellness coach speaking to one person.',
  'You will receive a JSON object describing their recent biometric data.',
  '',
  'Produce a single short, actionable nudge in 1 to 3 sentences tailored to that data.',
  '',
  'Rules:',
  '- Do NOT invent numbers that are not in the input.',
  '- Do NOT diagnose or make any medical claims; this is a wellness tool, not a medical device.',
  '- Reference at least one specific signal from the input (sleep duration, sleep quality, HRV, resting heart rate, or activity) when it is informative.',
  '- Be concrete: suggest one small thing the person could try today.',
  '- Tone: warm, plain language, no jargon.',
  '',
  'Reply with only the nudge text. No JSON, no preamble, no headings.',
].join('\n');

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

const buildUserMessage = (metrics) => [
  'Recent biometric data (JSON):',
  JSON.stringify(metrics, null, 2),
  '',
  'Write the nudge now.',
].join('\n');

const createNudge = ({ metrics, model, temperature, maxTokens }) => {
  const clean = sanitizeMetrics(metrics);
  const userGoal = clean.userGoal || '';

  // Keyword classifier returns synchronously. Tip lookup and any LLM-backed
  // reclassification live behind /tips — they don't block the live nudge.
  return classifier.classify({ userGoal })
    .catch(() => 'other')
    .then((category) => {
      const messages = [
        { role: 'system', content: SYSTEM_PROMPT },
        { role: 'user', content: buildUserMessage(clean) },
      ];
      // Wall-clock time of the on-device inference call only (excludes the
      // client's HTTP hop), so the app can show "generated in 4.2 s".
      const startedAt = Date.now();
      return aiClient.chat(messages, { model, temperature, maxTokens })
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

      // Clean up the model response:
      // 1. Strip any `<|...|>` special tokens (loading_model, processing_prompt,
      //    eos markers) that leak from mILM's streaming layer into non-streamed
      //    output — Android mILM seems to inline its progress markers.
      // 2. Strip `<br />` separators those markers leave behind.
      // 3. Strip outer quotes models sometimes add.
      const cleanText = (s) => s
        .replace(/<\|[^|]*\|>\s*\d+%/g, '')
        .replace(/<\|[^|]*\|>/g, '')
        .replace(/<br\s*\/?>/g, '')
        .trim()
        .replace(/^["']+|["']+$/g, '')
        .trim();
      const nudgeText = cleanText(text);
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
