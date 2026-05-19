// Tips: aggregate the user's recent focus categories and surface the past
// nudges they themselves marked as helpful in those categories. Read-only,
// no LLM calls — runs entirely off stored categories from prior /nudge calls.
//
// Strategy:
//   1. Look at the last RECENT_WINDOW nudges to determine current focus
//      (the categories the user has been asking about lately).
//   2. For each focus category that's NOT "other", pull up to PER_CATEGORY
//      past helpful nudges in that category.
//   3. Only include categories where the user actually has helpful
//      feedback — no helpful past = no tip card.
//   4. Order tip cards by how recently the user asked about that category
//      (most recent focus first).

const storage = require('../lib/storage');

const RECENT_WINDOW = 20;
const PER_CATEGORY = 3;

// Human-readable mapping for category slugs. Used in the tip card intro.
const HUMAN_LABEL = {
  'improve-sleep': 'improving sleep',
  'weight-loss': 'weight loss',
  'improve-appetite': 'eating well',
  'improve-mood': 'lifting your mood',
  'reduce-stress': 'reducing stress',
  'improve-fitness': 'training and fitness',
  'improve-recovery': 'recovery',
  'reduce-fatigue': 'fighting fatigue',
};

const humanLabel = (category) => HUMAN_LABEL[category] || category;

const buildIntro = (category) => {
  const label = humanLabel(category);
  return `Your recent nudges suggest you've been focused on ${label}. Here are a few past nudges that you marked as helpful for this goal.`;
};

const slimNudge = (n) => ({
  id: n.id,
  ts: n.ts,
  nudge: n.nudge,
});

const buildTips = () => storage.listNudges(RECENT_WINDOW).then(({ items }) => {
  // Determine focus categories from the recent window. Track first-seen
  // timestamp per category so we can sort by recency of last focus.
  const focus = {}; // category -> { lastTs, count }
  (items || []).forEach((item) => {
    const cat = item && item.category;
    if (!cat || cat === 'other') return;
    if (!focus[cat]) {
      focus[cat] = { lastTs: item.ts, count: 1 };
    } else {
      focus[cat].count += 1;
      if (item.ts > focus[cat].lastTs) focus[cat].lastTs = item.ts;
    }
  });

  const categories = Object.keys(focus);
  if (categories.length === 0) {
    return {
      items: [],
      totalRecent: (items || []).length,
      generatedAt: Date.now(),
    };
  }

  // For each focus category, fetch past helpful nudges in that category.
  // Build promises sequentially via reduce — Promise.all + array destructuring
  // doesn't play well with Duktape's polyfilled Promise.
  const startSeed = Promise.resolve([]);
  return categories.reduce((acc, category) => acc.then((cards) => storage.findHelpfulByCategory(category, PER_CATEGORY)
    .catch(() => [])
    .then((helpful) => {
      if (!helpful || helpful.length === 0) return cards;
      cards.push({
        category,
        categoryLabel: humanLabel(category),
        intro: buildIntro(category),
        recentMentions: focus[category].count,
        lastFocusedAt: focus[category].lastTs,
        helpfulNudges: helpful.map(slimNudge),
      });
      return cards;
    })), startSeed).then((cards) => {
    // Most recently focused first.
    cards.sort((a, b) => b.lastFocusedAt - a.lastFocusedAt);
    return {
      items: cards,
      totalRecent: (items || []).length,
      generatedAt: Date.now(),
    };
  });
});

module.exports = { buildTips };
