const { init } = require('@mimik/edge-ms-helper/init-helper');

const swaggerMiddleware = require('../build/wellness-nudge-swagger-mw');
const mimikContext = require('./lib/mimikContext');

mimikModule.exports = (context, req, res) => {
  mimikContext.setContext(context);

  const initFunction = init(swaggerMiddleware);
  return initFunction(context, req, res);
};
