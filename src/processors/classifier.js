// Classifies a nudge request into one of nine wellness-goal categories.
// Strategy: ask the LLM, then validate. Small models (SmolLM2-360M) don't
// reliably honor a constrained label list, so we layer a fuzzy keyword
// fallback that scans both the LLM output and the user's goal text. Final
// fallback is 'other'.
//
// The category is used at storage time (tags the saved nudge) and at lookup
// time (filter for past helpful nudges in the same category).

const aiClient = require('../lib/aiClient');

const CATEGORIES = [
  'improve-sleep',
  'weight-loss',
  'improve-appetite',
  'improve-mood',
  'reduce-stress',
  'improve-fitness',
  'improve-recovery',
  'reduce-fatigue',
  'other',
];

const ALLOWED = {};
CATEGORIES.forEach((c) => {
  ALLOWED[c] = true;
});

// Fuzzy keyword map. Order matters — more specific categories first so they
// outrank generic ones (e.g. "tired sleep" should hit improve-sleep, not
// reduce-fatigue; "muscle sore" should hit improve-recovery, not fitness).
const FUZZY_KEYWORDS = [
  { category: 'improve-sleep', keywords: ['sleep', 'bedtime', 'insomnia', 'rested', 'wake up', 'wake-up'] },
  { category: 'improve-recovery', keywords: ['recover', 'recovery', 'sore', 'soreness', 'muscle', 'rest day'] },
  { category: 'reduce-fatigue', keywords: ['fatigue', 'tired', 'exhaust', 'drained', 'energy', 'sluggish'] },
  { category: 'reduce-stress', keywords: ['stress', 'anxiety', 'anxious', 'tense', 'overwhelm', 'calm', 'relax', 'nervous'] },
  { category: 'improve-mood', keywords: ['mood', 'feel down', 'feeling down', 'feel-down', 'depress', 'sad', 'unhappy', 'cheer'] },
  {
    category: 'improve-fitness',
    keywords: [
      'fitness', 'train', 'training', 'run', 'running', 'jog',
      'exercise', 'workout', 'strength', 'cardio', 'endurance',
      'marathon', '10k', '5k', 'hike', 'hiking', 'move more', 'more active',
    ],
  },
  { category: 'weight-loss', keywords: ['weight', 'lose pound', 'lose weight', 'fat loss', 'slim', 'thinner', 'lose 10', 'lose 5'] },
  { category: 'improve-appetite', keywords: ['appetite', 'hunger', 'hungry', 'eat more', 'eating', 'meal'] },
];

const fuzzyMatch = (text) => {
  const lower = (text || '').toLowerCase();
  if (!lower) return null;
  // First pass: literal category name in the text (e.g. LLM emits the label)
  for (let i = 0; i < CATEGORIES.length; i += 1) {
    if (CATEGORIES[i] !== 'other' && lower.indexOf(CATEGORIES[i]) !== -1) {
      return CATEGORIES[i];
    }
  }
  // Second pass: keyword-based
  for (let i = 0; i < FUZZY_KEYWORDS.length; i += 1) {
    const entry = FUZZY_KEYWORDS[i];
    for (let j = 0; j < entry.keywords.length; j += 1) {
      if (lower.indexOf(entry.keywords[j]) !== -1) {
        return entry.category;
      }
    }
  }
  return null;
};

// Qwen3 uses a reasoning ("<think>...") mode by default. The /no_think
// directive disables it so the model emits the label directly.
const SYSTEM_PROMPT = [
  '/no_think You classify wellness coaching requests into exactly ONE category label.',
  '',
  'Allowed labels (and only these — no others):',
  '- improve-sleep',
  '- weight-loss',
  '- improve-appetite',
  '- improve-mood',
  '- reduce-stress',
  '- improve-fitness',
  '- improve-recovery',
  '- reduce-fatigue',
  '- other',
  '',
  'Rules:',
  '- Reply with ONLY the label.',
  '- No explanation, no punctuation, no quotes, no extra words.',
  '- If the user goal does not clearly match a specific label, reply "other".',
  '',
  'Examples:',
  'Input: goal="sleep better", metrics={"sleepHours":5}',
  'Output: improve-sleep',
  '',
  'Input: goal="lose 10 pounds", metrics={"stepsYesterday":2000}',
  'Output: weight-loss',
  '',
  'Input: goal="I feel constantly stressed at work", metrics={}',
  'Output: reduce-stress',
  '',
  'Input: goal="recover from a hard training week", metrics={"hrvMs":32}',
  'Output: improve-recovery',
  '',
  'Input: goal="train for a marathon", metrics={"stepsYesterday":12000}',
  'Output: improve-fitness',
  '',
  'Input: goal="I am always tired", metrics={"sleepHours":6}',
  'Output: reduce-fatigue',
  '',
  'Input: goal="get over a breakup", metrics={}',
  'Output: improve-mood',
  '',
  'Input: goal="learn to play piano"',
  'Output: other',
].join('\n');

const buildUserMessage = (userGoal, metrics) => {
  const goalText = (userGoal && String(userGoal).trim()) || '(no goal stated)';
  const metricsText = (metrics && Object.keys(metrics).length > 0)
    ? JSON.stringify(metrics)
    : '{}';
  return `Input: goal="${goalText}", metrics=${metricsText}\nOutput:`;
};

// Synchronous keyword classification — used on the live /nudge path.
// Returns the matched category or "other" for unmatched goals.
//
// We deliberately do NOT fall through to an LLM call here: mILM on Android
// serves one inference at a time, so any extra LLM call on the live path
// (classifier) runs concurrently with the nudge generator and one of them
// loses, surfacing as a 502 to the caller. Keep the live path fast.
const classify = ({ userGoal }) => Promise.resolve(fuzzyMatch(userGoal) || 'other');

// LLM-backed classifier. Kept around for batch / background use cases such
// as re-classifying older "other" records, but NOT called from /nudge.
const classifyWithLLM = ({ userGoal, metrics }) => {
  const fromGoal = fuzzyMatch(userGoal);
  if (fromGoal) return Promise.resolve(fromGoal);

  const messages = [
    { role: 'system', content: SYSTEM_PROMPT },
    { role: 'user', content: buildUserMessage(userGoal, metrics) },
  ];
  return aiClient.chat(messages, { model: 'qwen3-1.7b', temperature: 0, maxTokens: 64 }).then((completion) => {
    const choice = completion && completion.choices && completion.choices[0];
    let raw = (choice && choice.message && choice.message.content) || '';
    // Qwen3 may emit a <think>...</think> block even with /no_think.
    // Take everything after the last </think>.
    const closeIdx = raw.lastIndexOf('</think>');
    if (closeIdx !== -1) {
      raw = raw.slice(closeIdx + '</think>'.length);
    }
    const cleaned = raw
      .trim()
      .toLowerCase()
      .replace(/^["'`]+|["'`.,!?]+$/g, '')
      .split(/[\s\n]/)[0];

    if (ALLOWED[cleaned] && cleaned !== 'other') return cleaned;
    const fromLlm = fuzzyMatch(raw);
    if (fromLlm) return fromLlm;
    if (cleaned === 'other') return 'other';
    return 'other';
  }).catch(() => 'other');
};

module.exports = {
  CATEGORIES,
  classify,
  classifyWithLLM,
};
