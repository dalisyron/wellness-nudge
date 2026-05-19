const response = require('@mimik/edge-ms-helper/response-helper');

const storage = require('../lib/storage');

const FEEDBACK_VALUES = { yes: true, no: true, unset: true };

// swagger-mw-codegen sometimes wraps parameter values as { value: X } and
// sometimes exposes them directly. Normalize for path/query params.
const extractParam = (req, name) => {
  const params = (req.swagger && req.swagger.params) || {};
  const p = params[name];
  if (p && typeof p === 'object' && 'value' in p) return p.value;
  return p;
};

const extractBody = (req) => {
  const body = extractParam(req, 'body');
  if (body !== undefined && body !== null) return body;
  if (req.body) {
    if (typeof req.body === 'string') {
      try {
        return JSON.parse(req.body);
      } catch (_) {
        return null;
      }
    }
    return req.body;
  }
  return null;
};

const clampLimit = (raw) => {
  const n = Number(raw);
  if (!Number.isFinite(n) || n <= 0) return 20;
  if (n > 100) return 100;
  return Math.floor(n);
};

const listHistory = (req, res) => {
  const limit = clampLimit(extractParam(req, 'limit'));
  storage.listNudges(limit)
    .then(({ items, total }) => {
      response.sendResult({ data: { items, total } }, 200, res);
    })
    .catch((err) => {
      const status = err.status || err.statusCode || 500;
      response.sendError(err, res, status);
    });
};

const updateFeedback = (req, res) => {
  const id = extractParam(req, 'id');
  const body = extractBody(req);

  if (!id || typeof id !== 'string') {
    response.sendError(new Error('id is required'), res, 400);
    return;
  }
  if (!body || typeof body !== 'object') {
    response.sendError(new Error('Request body must be a JSON object'), res, 400);
    return;
  }
  const helpful = body.helpful;
  if (!FEEDBACK_VALUES[helpful]) {
    response.sendError(new Error('helpful must be one of: yes, no, unset'), res, 400);
    return;
  }

  storage.updateFeedback(id, helpful)
    .then((nudge) => {
      response.sendResult({ data: nudge }, 200, res);
    })
    .catch((err) => {
      const status = err.status || err.statusCode || 500;
      response.sendError(err, res, status);
    });
};

const deleteNudge = (req, res) => {
  const id = extractParam(req, 'id');
  if (!id || typeof id !== 'string') {
    response.sendError(new Error('id is required'), res, 400);
    return;
  }
  storage.deleteNudge(id)
    .then(() => {
      // 204 No Content on success.
      res.statusCode = 204;
      res.end();
    })
    .catch((err) => {
      const status = err.status || err.statusCode || 500;
      response.sendError(err, res, status);
    });
};

module.exports = {
  listHistory,
  updateFeedback,
  deleteNudge,
};
