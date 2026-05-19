const response = require('@mimik/edge-ms-helper/response-helper');

const tipsProcessor = require('../processors/tipsProcessor');

const getTips = (req, res) => {
  tipsProcessor.buildTips()
    .then((data) => response.sendResult({ data }, 200, res))
    .catch((err) => {
      const status = err.status || err.statusCode || 500;
      response.sendError(err, res, status);
    });
};

module.exports = { getTips };
