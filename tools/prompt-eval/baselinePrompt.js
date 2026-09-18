// The /nudge prompt before the rewrite (src/processors/nudgeProcessor.js at 791875a^), frozen here so
// the harness can re-measure the baseline: generic rules plus the request body as raw JSON.
// The app's sliders were Kotlin Floats sent as Doubles, so 5.7 arrived as 5.699999809265137;
// Math.fround reproduces that for the three slider fields.

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
const FLOAT_FIELDS = { sleepHours: true, deepSleepPct: true, remSleepPct: true };

const buildUserMessage = (metrics) => [
  'Recent biometric data (JSON):',
  JSON.stringify(metrics, null, 2),
  '',
  'Write the nudge now.',
].join('\n');

const buildMessages = (metrics) => {
  const body = {};
  KNOWN_FIELDS.forEach((k) => {
    const v = (metrics || {})[k];
    if (v !== undefined && v !== null) body[k] = FLOAT_FIELDS[k] && typeof v === 'number' ? Math.fround(v) : v;
  });
  return [
    { role: 'system', content: SYSTEM_PROMPT },
    { role: 'user', content: buildUserMessage(body) },
  ];
};

module.exports = { buildMessages };
