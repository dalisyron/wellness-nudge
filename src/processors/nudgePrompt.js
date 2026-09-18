// Prompt construction for the nudge writer (SmolLM2-360M via mILM).
//
// A 360M model needs a lot of help, so the work is split: code turns the raw
// metrics into rounded, plain-language lines plus a computed "Summary"
// (short sleep, low HRV, ...), and one worked example matched to the goal
// category shows the model the exact shape of a good nudge. Stand-in numbers
// in the example come from the same band as the request so the wording
// agrees, and the example only mentions the fields the request has.
//
// Duktape (ES5.1) safe: no toLocaleString grouping, padStart, includes,
// Object.assign, lookbehind or named groups. Babel handles const, arrow
// functions and template literals.

const SYSTEM_PROMPT = [
  'You are a warm wellness coach. Write one short nudge for the person, based on their data and goal.',
  'Rules:',
  '- Two sentences, under 45 words in total.',
  '- Sentence 1: repeat one or two of their numbers exactly as given and say what they mean.',
  '- Sentence 2: suggest one small, specific thing to do today that fits their goal.',
  '- Only use numbers from their data. Never change a number.',
  '- No medical advice, no lists, no greeting.',
  'Reply with only the nudge.',
].join('\n');

const FIELDS = ['sleepHours', 'deepSleepPct', 'remSleepPct', 'restingHR', 'hrvMs', 'stepsYesterday'];

// ---------------------------------------------------------------------------
// Numbers. The Android sliders are Kotlin Floats sent as Doubles, so values arrive as e.g.
// 5.699999809265137. Round BEFORE formatting and before deriving words, so text and words agree.
// 0, negative, empty or non-numeric values are treated as "no data" and their line is omitted.
const toNumber = (v) => {
  if (v === undefined || v === null || v === '') return null;
  const n = Number(v);
  if (isNaN(n) || !isFinite(n) || n <= 0) return null;
  return n;
};

const round1 = (n) => Math.round(n * 10) / 10;

// 5 -> "5", 5.7 -> "5.7"
const fmtHours = (n) => {
  const r = round1(n);
  return r % 1 === 0 ? String(r) : r.toFixed(1);
};

// 3200 -> "3,200" (manual grouping: Duktape's toLocaleString does not group digits)
const fmtThousands = (n) => {
  const s = String(Math.round(n));
  let out = '';
  for (let i = 0; i < s.length; i += 1) {
    if (i > 0 && (s.length - i) % 3 === 0) out += ',';
    out += s.charAt(i);
  }
  return out;
};

const cleanGoal = (g) => {
  if (g === undefined || g === null) return '';
  const s = String(g).replace(/\s+/g, ' ').trim();
  return s.length > 150 ? s.slice(0, 150).trim() : s;
};

const capitalize = (s) => s.charAt(0).toUpperCase() + s.slice(1);

// Plain-language reading of the numbers, computed in code (a 360M model cannot tell whether
// 28 ms is a low HRV). General wellness heuristics, not clinical thresholds.
const sleepBand = (h) => {
  if (h < 6) return 'short';
  if (h < 7) return 'slightly short';
  return 'good';
};
const hrvLevel = (v) => {
  if (v < 35) return 'low';
  if (v < 50) return 'average';
  return 'good';
};
const stepsBand = (s) => {
  if (s < 5000) return 'quiet';
  if (s < 10000) return 'moderately active';
  if (s < 15000) return 'active';
  return 'very active';
};

// ---------------------------------------------------------------------------
// User message. Every field is optional; lines for missing fields are omitted.
//
//   Sleep last night: 5 hours (11% deep, 14% REM)
//   Resting heart rate: 70 bpm
//   HRV: 28 ms
//   Steps yesterday: 3,200
//   Summary: short sleep, low HRV, quiet day yesterday.
//   Goal: Sleep better tonight
//
//   Write my nudge.
//
// With no metrics at all (goal only) the data lines are replaced by 'No health data shared today.'
const buildUserMessage = (metrics) => {
  const m = metrics || {};
  const sleep = toNumber(m.sleepHours);
  const deep = toNumber(m.deepSleepPct);
  const rem = toNumber(m.remSleepPct);
  const rhr = toNumber(m.restingHR);
  const hrv = toNumber(m.hrvMs);
  const steps = toNumber(m.stepsYesterday);
  const goal = cleanGoal(m.userGoal);

  const lines = [];
  const stages = [];
  if (deep !== null) stages.push(`${Math.round(deep)}% deep`);
  if (rem !== null) stages.push(`${Math.round(rem)}% REM`);
  if (sleep !== null) {
    lines.push(`Sleep last night: ${fmtHours(sleep)} hours${stages.length ? ` (${stages.join(', ')})` : ''}`);
  } else if (stages.length) {
    lines.push(`Sleep last night: ${stages.join(', ')}`);
  }
  if (rhr !== null) lines.push(`Resting heart rate: ${Math.round(rhr)} bpm`);
  if (hrv !== null) lines.push(`HRV: ${Math.round(hrv)} ms`);
  if (steps !== null) lines.push(`Steps yesterday: ${fmtThousands(steps)}`);

  const summary = [];
  if (sleep !== null) summary.push(`${sleepBand(round1(sleep))} sleep`);
  if (hrv !== null) summary.push(`${hrvLevel(Math.round(hrv))} HRV`);
  if (steps !== null) summary.push(`${stepsBand(Math.round(steps))} day yesterday`);
  if (summary.length) lines.push(`Summary: ${summary.join(', ')}.`);
  if (!lines.length) lines.push('No health data shared today.');

  lines.push(goal ? `Goal: ${goal}` : 'Goal: none given, pick the most helpful focus.');
  lines.push('');
  lines.push('Write my nudge.');
  return lines.join('\n');
};

// ---------------------------------------------------------------------------
// One worked example per goal category (keys = classifier.js categories), sent as a user/assistant
// turn before the real request. The example is MATCHED to the request so the 360M model can mirror it:
//   - its sleep / HRV / steps are replaced by a stand-in value from the SAME band as the request
//     (so the summary words and "low/average/good" agree),
//   - it only contains the fields the request has (the model never sees a metric it could copy),
//   - its opening clause names the same fields, steps first after a very active day (15,000+),
//   - fitness and weight-loss switch to an "easy" variant after short sleep (<6 h) or low HRV (<35 ms).
// Stand-in numbers are distinct from the demo scenarios, so a copied number would be caught.
const EXAMPLES = {
  'improve-sleep': {
    metrics: { sleepHours: 5.3, deepSleepPct: 12, remSleepPct: 15, restingHR: 71, hrvMs: 30, stepsYesterday: 3600, userGoal: 'Get better sleep' },
    meaning: 'so tonight is for winding down early',
    action: 'Put your phone on the charger outside your bedroom 30 minutes before bed.',
  },
  'reduce-fatigue': {
    metrics: { sleepHours: 5.9, deepSleepPct: 14, remSleepPct: 16, restingHR: 72, hrvMs: 27, stepsYesterday: 4300, userGoal: 'Have more energy at work' },
    meaning: 'so go easy on yourself today',
    action: 'Step outside for a gentle 10-minute walk after lunch to wake your body up.',
  },
  'reduce-stress': {
    metrics: { sleepHours: 6.4, deepSleepPct: 16, remSleepPct: 19, restingHR: 65, hrvMs: 37, stepsYesterday: 6100, userGoal: 'Stay calm during a busy day' },
    meaning: 'so your body could use some calm today',
    action: 'Before your busiest hour, take five slow breaths, making each exhale longer than the inhale.',
  },
  'improve-fitness': {
    metrics: { sleepHours: 8.1, deepSleepPct: 21, remSleepPct: 24, restingHR: 54, hrvMs: 71, stepsYesterday: 12600, userGoal: 'Get fitter for ski season' },
    meaning: 'so your body is ready for a bit more today',
    action: 'Add 10 extra minutes to your usual workout at a steady pace.',
    easyMeaning: 'so keep today\'s training easy',
    easyAction: 'Swap any hard session for an easy 20-minute jog or walk.',
  },
  'improve-recovery': {
    metrics: { sleepHours: 7.4, deepSleepPct: 19, remSleepPct: 21, restingHR: 62, hrvMs: 46, stepsYesterday: 19800, userGoal: 'Recover from yesterday\'s trail run' },
    meaning: 'so today is for easy recovery',
    action: 'Keep it light with a relaxed 20-minute walk instead of a hard workout.',
  },
  'improve-mood': {
    metrics: { sleepHours: 6.2, deepSleepPct: 15, remSleepPct: 17, restingHR: 66, hrvMs: 38, stepsYesterday: 4600, userGoal: 'Feel more upbeat today' },
    meaning: 'so be gentle with yourself today',
    action: 'Get 15 minutes of daylight on a walk outside this morning.',
  },
  'weight-loss': {
    metrics: { sleepHours: 7.1, deepSleepPct: 18, remSleepPct: 20, restingHR: 60, hrvMs: 52, stepsYesterday: 6900, userGoal: 'Lose a few pounds by spring' },
    meaning: 'so you have energy to build on today',
    action: 'Add a brisk 15-minute walk after dinner tonight.',
    easyMeaning: 'so be gentle with yourself today',
    easyAction: 'Take a relaxed 15-minute walk after dinner tonight.',
  },
  'improve-appetite': {
    metrics: { sleepHours: 6.6, deepSleepPct: 16, remSleepPct: 19, restingHR: 64, hrvMs: 42, stepsYesterday: 5800, userGoal: 'Eat a proper breakfast' },
    meaning: 'so steady fuel will help today',
    action: 'Have a simple breakfast like yogurt with fruit within an hour of waking.',
  },
  other: {
    metrics: { sleepHours: 6.9, deepSleepPct: 15, remSleepPct: 18, restingHR: 63, hrvMs: 47, stepsYesterday: 7300, userGoal: 'Cut back on sugary snacks' },
    meaning: 'so small changes can go a long way today',
    action: 'Swap your afternoon cookie for a piece of fruit and a glass of water.',
  },
  wakeUp: {
    metrics: { sleepHours: 5.9, deepSleepPct: 13, remSleepPct: 16, restingHR: 70, hrvMs: 30, stepsYesterday: 4700, userGoal: 'Wake up with more energy' },
    meaning: 'so your body will need help waking up tomorrow',
    action: 'Open the curtains and get 5 minutes of daylight as soon as you get up.',
  },
  noGoal: {
    metrics: { sleepHours: 6.7, deepSleepPct: 17, remSleepPct: 19, restingHR: 62, hrvMs: 45, stepsYesterday: 6300 },
    meaning: 'so today is a good day for steady habits',
    action: 'Take a 10-minute walk after lunch to keep your energy even.',
  },
};

const SLEEP_STANDIN = { short: 5.3, 'slightly short': 6.6, good: 7.6 };
const HRV_STANDIN = { low: 30, average: 42, good: 61 };
const STEPS_STANDIN = { quiet: 3600, 'moderately active': 7300, active: 12600, 'very active': 19800 };

const WAKE_RX = /\bwak(e|es|ing)\b/i;
const WAKE_CATEGORIES = { 'improve-sleep': true, 'reduce-fatigue': true, other: true };

const pickExample = (goal, category) => {
  if (!goal) return EXAMPLES.noGoal;
  if (WAKE_CATEGORIES[category] && WAKE_RX.test(goal)) return EXAMPLES.wakeUp;
  return EXAMPLES[category] || EXAMPLES.other;
};

const isLowRecovery = (m) => {
  const sleep = toNumber(m.sleepHours);
  const hrv = toNumber(m.hrvMs);
  return (sleep !== null && round1(sleep) < 6) || (hrv !== null && Math.round(hrv) < 35);
};

const LEAD_ORDER = ['sleepHours', 'hrvMs', 'stepsYesterday', 'restingHR', 'deepSleepPct', 'remSleepPct'];

const leadPhrase = (key, v) => {
  if (key === 'sleepHours') return `you slept ${round1(v) < 6 ? 'only ' : ''}${fmtHours(v)} hours`;
  if (key === 'hrvMs') return `your HRV is ${hrvLevel(Math.round(v))} at ${Math.round(v)} ms`;
  if (key === 'stepsYesterday') return `you walked ${fmtThousands(v)} steps yesterday`;
  if (key === 'restingHR') return `your resting heart rate is ${Math.round(v)} bpm`;
  if (key === 'deepSleepPct') return `${Math.round(v)}% of your sleep was deep sleep`;
  return `${Math.round(v)}% of your sleep was REM sleep`;
};

// e.g. "You slept only 5.3 hours and your HRV is low at 30 ms"
const buildLead = (exampleMetrics, request) => {
  let keys = LEAD_ORDER.filter((k) => toNumber(exampleMetrics[k]) !== null);
  const reqSteps = toNumber(request.stepsYesterday);
  if (reqSteps !== null && Math.round(reqSteps) >= 15000) {
    keys = ['stepsYesterday'].concat(keys.filter((k) => k !== 'stepsYesterday'));
  }
  const parts = keys.slice(0, 2).map((k) => leadPhrase(k, toNumber(exampleMetrics[k])));
  if (parts.length === 2 && parts[0].indexOf('you ') === 0 && parts[1].indexOf('you ') === 0) {
    parts[1] = parts[1].slice(4);
  }
  return parts.length ? capitalize(parts.join(' and ')) : '';
};

const buildExample = (metrics, category) => {
  const m = metrics || {};
  const ex = pickExample(cleanGoal(m.userGoal), category);
  const useEasy = ex.easyMeaning && isLowRecovery(m);
  const meaning = useEasy ? ex.easyMeaning : ex.meaning;
  const action = useEasy ? ex.easyAction : ex.action;

  const sleep = toNumber(m.sleepHours);
  const hrv = toNumber(m.hrvMs);
  const steps = toNumber(m.stepsYesterday);
  const standIn = {};
  if (sleep !== null) standIn.sleepHours = SLEEP_STANDIN[sleepBand(round1(sleep))];
  if (hrv !== null) standIn.hrvMs = HRV_STANDIN[hrvLevel(Math.round(hrv))];
  if (steps !== null) standIn.stepsYesterday = STEPS_STANDIN[stepsBand(Math.round(steps))];

  const masked = {};
  FIELDS.forEach((k) => {
    if (toNumber(m[k]) !== null) masked[k] = standIn[k] !== undefined ? standIn[k] : ex.metrics[k];
  });
  if (ex.metrics.userGoal) masked.userGoal = ex.metrics.userGoal;

  const lead = buildLead(masked, m);
  const nudge = lead
    ? `${lead}, ${meaning}. ${action}`
    : `${capitalize(meaning.replace(/^so /, ''))}. ${action}`;
  return { user: buildUserMessage(masked), nudge };
};

const buildMessages = (metrics, category) => {
  const ex = buildExample(metrics, category);
  return [
    { role: 'system', content: SYSTEM_PROMPT },
    { role: 'user', content: ex.user },
    { role: 'assistant', content: ex.nudge },
    { role: 'user', content: buildUserMessage(metrics) },
  ];
};

module.exports = {
  SYSTEM_PROMPT, EXAMPLES, buildUserMessage, buildMessages, fmtHours, fmtThousands,
};
