const response = require('@mimik/edge-ms-helper/response-helper');

const getHealth = (req, res) => {
  response.sendResult({
    data: {
      status: 'ok',
      mim: 'wellness-nudge',
      version: '1.0.0',
    },
  }, 200, res);
};

module.exports = { getHealth };
