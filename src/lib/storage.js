// Persistence layer for nudges. Wraps context.storage in Promise-returning
// helpers so callers (controllers, processors) have a uniform async surface.
//
// Schema (one storage entry per nudge, tag 'nudge'):
//   key:   nudge_<ts>          e.g. "nudge_1747700000000"
//   value: JSON string:
//     {
//       id:        "nudge_<ts>",
//       ts:        <unix ms>,
//       metrics:   { ... },
//       userGoal:  string,
//       category:  one of 9 wellness categories,
//       nudge:     generated nudge text,
//       model:     model id,
//       latencyMs: on-device inference time in ms (absent on older records),
//       helpful:   "unset" | "yes" | "no"
//     }

const mimikContext = require('./mimikContext');

const TAG = 'nudge';

const getStorage = () => mimikContext.getContext().then((ctx) => ctx.storage);

const newId = (ts) => `nudge_${ts}`;

const saveNudge = (nudge) => getStorage().then((storage) => {
  storage.setItemWithTag(nudge.id, JSON.stringify(nudge), TAG);
  return nudge;
});

const getNudge = (id) => getStorage().then((storage) => {
  const raw = storage.getItem(id);
  return raw ? JSON.parse(raw) : null;
});

const parseItems = (items) => {
  if (!items) return [];
  if (typeof items === 'string') {
    try {
      return JSON.parse(items) || [];
    } catch (_) {
      return [];
    }
  }
  if (Array.isArray(items)) return items;
  return [];
};

const listNudges = (limit) => getStorage().then((storage) => new Promise((resolve, reject) => {
  try {
    storage.getJsonItemsPaginated(
      0,
      limit,
      (items, totalCount) => {
        resolve({ items: parseItems(items), total: totalCount || 0 });
      },
      {
        tag: TAG,
        orderBy: [{ jsonPath: '$.ts', order: -1 }],
      },
    );
  } catch (e) {
    reject(e);
  }
}));

const findHelpfulByCategory = (category, limit) => getStorage().then((storage) => new Promise((resolve, reject) => {
  try {
    storage.getJsonItemsPaginated(
      0,
      limit,
      (items) => {
        resolve(parseItems(items));
      },
      {
        tag: TAG,
        orderBy: [{ jsonPath: '$.ts', order: -1 }],
        filters: [
          { jsonPath: '$.category', comparisonOperator: '=', value: category },
          { jsonPath: '$.helpful', comparisonOperator: '=', value: 'yes' },
        ],
      },
    );
  } catch (e) {
    reject(e);
  }
}));

const updateFeedback = (id, helpful) => getStorage().then((storage) => {
  const raw = storage.getItem(id);
  if (!raw) {
    const err = new Error(`Nudge '${id}' not found`);
    err.status = 404;
    throw err;
  }
  const nudge = JSON.parse(raw);
  nudge.helpful = helpful;
  storage.setItemWithTag(id, JSON.stringify(nudge), TAG);
  return nudge;
});

// Remove a nudge entirely. 404 if it doesn't exist.
const deleteNudge = (id) => getStorage().then((storage) => {
  const raw = storage.getItem(id);
  if (!raw) {
    const err = new Error(`Nudge '${id}' not found`);
    err.status = 404;
    throw err;
  }
  storage.removeItem(id);
});

module.exports = {
  newId,
  saveNudge,
  getNudge,
  listNudges,
  findHelpfulByCategory,
  updateFeedback,
  deleteNudge,
};
