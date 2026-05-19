const response = require('@mimik/edge-ms-helper/response-helper');

const nudgeProcessor = require('../processors/nudgeProcessor');

// swagger-mw-codegen may expose the body parameter as either the value
// directly or as { value: ... }. Normalize.
const extractBody = (req) => {
  const params = (req.swagger && req.swagger.params) || {};
  const bodyParam = params.body;
  if (bodyParam && typeof bodyParam === 'object' && 'value' in bodyParam) {
    return bodyParam.value;
  }
  if (bodyParam !== undefined) {
    return bodyParam;
  }
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

const createNudge = (req, res) => {
  const metrics = extractBody(req);

  nudgeProcessor
    .createNudge({ metrics })
    .then((data) => response.sendResult({ data }, 200, res))
    .catch((err) => {
      const status = err.status || err.statusCode || 500;
      response.sendError(err, res, status);
    });
};

module.exports = {
  createNudge,
};
