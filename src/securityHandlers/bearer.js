const { ForbiddenError } = require('@mimik/edge-ms-helper/error-helper');

const ApiKey = (req, definition, apiKey, next) => {
  const expectedKey = req.context && req.context.env && req.context.env.API_KEY;

  if (!expectedKey) {
    next();
    return;
  }

  const token = apiKey && apiKey.toLowerCase().startsWith('bearer ')
    ? apiKey.slice(7)
    : apiKey;

  if (!token || token !== expectedKey) {
    next(new ForbiddenError('Forbidden: invalid API key'));
    return;
  }

  next();
};

module.exports = ApiKey;
