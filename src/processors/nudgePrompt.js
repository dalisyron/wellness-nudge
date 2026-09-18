// Prompt construction for the nudge writer (SmolLM2-360M via mILM).
//
// A 360M model needs a lot of help, so the work is split: code turns the raw
// metrics into rounded, plain-language lines plus a computed "Summary"
// (short sleep, low HRV, ...), and one worked example matched to the goal
// category shows the model the exact shape of a good nudge. Stand-in numbers
// in the example come from the same band as the request so the wording
// agrees, and the example only mentions the fields the request has. Each
// category has several example answers; the one used rotates per call, so
// "Try another" usually gets a genuinely different suggestion.
//
// Duktape (ES5.1) safe: no toLocaleString grouping, padStart, includes,
// Object.assign, lookbehind or named groups. Babel handles const, arrow
// functions and template literals.
//
// Usage: buildMessages(clean, category) picks the variant from the clock;
// tests pass an explicit variantIndex.

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
const NO_DATA_OPENER = 'No numbers today, so start with one simple step.';

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
//   - fitness and weight-loss use easyVariants after short sleep (<6 h) or low HRV (<35 ms).
// Each category has 3 meaning+action variants ('other' has 2, see below). The model mirrors the example's
// action closely, so the variant (picked per call from the clock, or injected) is what makes "Try another"
// give a different nudge. A variant may override the example goal.
// 'other' is the classifier's catch-all, so its real goals are arbitrary: of 10 candidate actions tested, only
// these 2 passed both the automatic checks and a manual goal-fit read (the rest got copied into unrelated goals
// or turned "drink less coffee" into coffee advice). Both keep the same meaning line on purpose: it is the one
// that makes the model adapt the action to the user's goal instead of copying the example.
// Stand-in numbers are distinct from the demo scenarios, so a copied number would be caught.
const EXAMPLES = {
  'improve-sleep': {
    metrics: { sleepHours: 5.3, deepSleepPct: 12, remSleepPct: 15, restingHR: 71, hrvMs: 30, stepsYesterday: 3600, userGoal: 'Get better sleep' },
    variants: [
      { meaning: 'so tonight is for winding down early', action: 'Put your phone on the charger outside your bedroom 30 minutes before bed.' },
      { meaning: 'so tonight is for a cool, dark bedroom', action: 'Open a window or turn down the heat so your bedroom is cool by bedtime.' },
      { meaning: 'so give your body a calm evening', action: 'Take a warm shower about an hour before bed to help you wind down.' },
    ],
  },
  'reduce-fatigue': {
    metrics: { sleepHours: 5.9, deepSleepPct: 14, remSleepPct: 16, restingHR: 72, hrvMs: 27, stepsYesterday: 4300, userGoal: 'Have more energy at work' },
    variants: [
      { meaning: 'so go easy on yourself today', action: 'Step outside for a gentle 10-minute walk after lunch to wake your body up.' },
      { meaning: 'so small energy boosts will help today', action: 'Drink a glass of water every time you finish a task this afternoon.' },
      { meaning: 'so a little daylight will help today', action: 'Spend 10 minutes by a bright window or outside when the afternoon slump hits.' },
    ],
  },
  'reduce-stress': {
    metrics: { sleepHours: 6.4, deepSleepPct: 16, remSleepPct: 19, restingHR: 65, hrvMs: 37, stepsYesterday: 6100, userGoal: 'Stay calm during a busy day' },
    variants: [
      { meaning: 'so your body could use some calm today', action: 'Before your busiest hour, take five slow breaths, making each exhale longer than the inhale.' },
      { meaning: 'so give yourself a real break today', action: 'Take your lunch break away from your desk, without your phone.' },
      { meaning: 'so give your mind some room today', action: 'Spend five minutes writing down whatever is on your mind.' },
    ],
  },
  'improve-fitness': {
    metrics: { sleepHours: 8.1, deepSleepPct: 21, remSleepPct: 24, restingHR: 54, hrvMs: 71, stepsYesterday: 12600, userGoal: 'Get fitter for ski season' },
    variants: [
      { meaning: 'so your body is ready for a bit more today', action: 'Add 10 extra minutes to your usual workout at a steady pace.' },
      { meaning: 'so consistency is the goal today', action: 'Put your next workout in your calendar right after today\'s session.' },
      { meaning: 'so you can push the pace a little today', action: 'Pick up the pace for the last five minutes of your workout.' },
    ],
    easyVariants: [
      { meaning: 'so today calls for an easy session', action: 'Make today an easy 20-minute jog or walk instead of a hard session.' },
      { meaning: 'so today is for gentle movement', action: 'Take a relaxed walk today and save hard training for a fresher day.' },
      { meaning: 'so let your body catch up today', action: 'Do half your usual workout today at a pace easy enough to chat.' },
    ],
  },
  'improve-recovery': {
    metrics: { sleepHours: 7.4, deepSleepPct: 19, remSleepPct: 21, restingHR: 62, hrvMs: 46, stepsYesterday: 19800, userGoal: 'Recover from yesterday\'s trail run' },
    variants: [
      { meaning: 'so today is for easy recovery', action: 'Keep it light with a relaxed 20-minute walk instead of a hard workout.' },
      { meaning: 'so today is for gentle recovery', action: 'Do 10 minutes of gentle stretching for your legs and hips this evening.' },
      { meaning: 'so make recovery the focus today', action: 'Go to bed 30 minutes earlier tonight to help your muscles repair.' },
    ],
  },
  'improve-mood': {
    metrics: { sleepHours: 6.2, deepSleepPct: 15, remSleepPct: 17, restingHR: 66, hrvMs: 38, stepsYesterday: 4600, userGoal: 'Feel more upbeat today' },
    variants: [
      { meaning: 'so be gentle with yourself today', action: 'Get 15 minutes of daylight on a walk outside this morning.' },
      { meaning: 'so a little connection could lift you today', action: 'Send a quick message to a friend you have not talked to in a while.' },
      { meaning: 'so notice the small wins today', action: 'Write down three small things that went well this week.' },
    ],
  },
  'weight-loss': {
    metrics: { sleepHours: 7.1, deepSleepPct: 18, remSleepPct: 20, restingHR: 60, hrvMs: 52, stepsYesterday: 6900, userGoal: 'Lose a few pounds by spring' },
    variants: [
      { meaning: 'so you have energy to build on today', action: 'Add a brisk 15-minute walk after dinner tonight.' },
      { meaning: 'so small food swaps can help today', action: 'Fill half your plate with vegetables at dinner tonight.' },
      { meaning: 'so keep today simple and steady', action: 'Drink a glass of water before each meal today.' },
    ],
    easyVariants: [
      { meaning: 'so be gentle with yourself today', action: 'Take a relaxed 15-minute walk after dinner tonight.' },
      { meaning: 'so focus on food rather than hard exercise today', action: 'Fill half your plate with vegetables at dinner tonight.' },
      { meaning: 'so rest is part of the plan today', action: 'Go to bed 30 minutes earlier tonight to help curb tomorrow\'s cravings.' },
    ],
  },
  'improve-appetite': {
    metrics: { sleepHours: 6.6, deepSleepPct: 16, remSleepPct: 19, restingHR: 64, hrvMs: 42, stepsYesterday: 5800, userGoal: 'Eat a proper breakfast' },
    variants: [
      { meaning: 'so steady fuel will help today', action: 'Have a simple breakfast like yogurt with fruit within an hour of waking.' },
      { meaning: 'so small, regular bites will help today', action: 'Keep a banana or a handful of nuts nearby for an easy mid-morning snack.' },
      { meaning: 'so make eating easy today', action: 'Blend a smoothie with milk, a banana and oats if a full meal feels like too much.' },
    ],
  },
  other: {
    metrics: { sleepHours: 6.9, deepSleepPct: 15, remSleepPct: 18, restingHR: 63, hrvMs: 47, stepsYesterday: 7300, userGoal: 'Cut back on sugary snacks' },
    variants: [
      { meaning: 'so small changes can go a long way today', action: 'Swap your afternoon cookie for a piece of fruit and a glass of water.', goal: 'Cut back on sugary snacks' },
      { meaning: 'so small changes can go a long way today', action: 'Swap your after-dinner dessert for a cup of herbal tea.', goal: 'Eat fewer sweets at night' },
    ],
  },
  wakeUp: {
    metrics: { sleepHours: 5.9, deepSleepPct: 13, remSleepPct: 16, restingHR: 70, hrvMs: 30, stepsYesterday: 4700, userGoal: 'Wake up with more energy' },
    variants: [
      { meaning: 'so your body will need help waking up tomorrow', action: 'Open the curtains and get 5 minutes of daylight as soon as you get up.' },
      { meaning: 'so tonight sets up tomorrow morning', action: 'Go to bed 30 minutes earlier tonight so your alarm feels easier.' },
      { meaning: 'so let morning light help you tomorrow', action: 'Leave your curtains slightly open tonight so morning light can wake you gently.' },
    ],
  },
  noGoal: {
    metrics: { sleepHours: 6.7, deepSleepPct: 17, remSleepPct: 19, restingHR: 62, hrvMs: 45, stepsYesterday: 6300 },
    variants: [
      { meaning: 'so today is a good day for steady habits', action: 'Take a 10-minute walk after lunch to keep your energy even.' },
      { meaning: 'so keep today simple', action: 'Drink a glass of water with each meal today.' },
      { meaning: 'so small breaks will help today', action: 'Take a two-minute stretch break at the top of every hour.' },
    ],
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

// Explicit index (tests, or a client-supplied counter) or the current second, modulo the pool size.
const pickVariant = (pool, variantIndex) => {
  const raw = (typeof variantIndex === 'number' && isFinite(variantIndex))
    ? Math.floor(variantIndex)
    : Math.floor(Date.now() / 1000);
  return pool[((raw % pool.length) + pool.length) % pool.length];
};

const buildExample = (metrics, category, variantIndex) => {
  const m = metrics || {};
  const ex = pickExample(cleanGoal(m.userGoal), category);
  const pool = (ex.easyVariants && isLowRecovery(m)) ? ex.easyVariants : ex.variants;
  const variant = pickVariant(pool, variantIndex);
  const meaning = variant.meaning;
  const action = variant.action;

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
  const exampleGoal = variant.goal || ex.metrics.userGoal;
  if (exampleGoal) masked.userGoal = exampleGoal;

  const lead = buildLead(masked, m);
  let nudge;
  if (lead) {
    nudge = `${lead}, ${meaning}. ${action}`;
  } else if (exampleGoal) {
    // goal only (no metrics): honest opener, no claims about the body, then the action
    nudge = `${NO_DATA_OPENER} ${action}`;
  } else {
    nudge = `${capitalize(meaning.replace(/^so /, ''))}. ${action}`;
  }
  return { user: buildUserMessage(masked), nudge };
};

// variantIndex is optional: omit it in production (clock-based pick), pass 0/1/2 in tests.
const buildMessages = (metrics, category, variantIndex) => {
  const ex = buildExample(metrics, category, variantIndex);
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
