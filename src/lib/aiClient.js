// Thin wrapper around the local AI Foundation inference API.
// Uses context.http (the mimOE-provided HTTP client) because Node's fetch
// and http modules are not available in the Duktape runtime.
//
// All requests target the local mimOE node, on its dynamically-assigned
// HTTP port (read from context.info), so this works whether mimOE is on
// 8083 (default) or anything else.
//
// The inference path differs by host platform:
//   - Desktop mimOE serves the AI Foundation addon at /mimik-ai/openai/v1/...
//   - Android mimOE serves mILM at /{clientId}/milm/v1/...
// Set INFERENCE_URL_PATH at deploy time to override the default.

const mimikContext = require('./mimikContext');

const DEFAULT_INFERENCE_PATH = '/mimik-ai/openai/v1/chat/completions';
const DEFAULT_MODEL = 'smollm2-360m';
const DEFAULT_TEMPERATURE = 0.4;
const DEFAULT_MAX_TOKENS = 200;

const chat = (messages, options = {}) => mimikContext.getContext().then((context) => new Promise((resolve, reject) => {
  const port = context.info && context.info.httpPort;
  if (!port) {
    reject(Object.assign(new Error('mimOE httpPort missing from context.info'), { status: 500 }));
    return;
  }

  const env = context.env || {};
  const apiKey = env.INFERENCE_API_KEY || '1234';
  const model = options.model || env.INFERENCE_MODEL || DEFAULT_MODEL;
  const temperature = options.temperature !== undefined ? options.temperature : DEFAULT_TEMPERATURE;
  const maxTokens = options.maxTokens || DEFAULT_MAX_TOKENS;
  const inferencePath = env.INFERENCE_URL_PATH || DEFAULT_INFERENCE_PATH;

  context.http.request({
    type: 'POST',
    url: `http://127.0.0.1:${port}${inferencePath}`,
    headers: { 'Content-Type': 'application/json' },
    authorization: `Bearer ${apiKey}`,
    data: {
      model,
      temperature,
      max_tokens: maxTokens,
      messages,
    },
    success: (result) => {
      try {
        const parsed = typeof result.data === 'string' ? JSON.parse(result.data) : result.data;
        resolve(parsed);
      } catch (parseErr) {
        reject(Object.assign(new Error(`Failed to parse inference response: ${parseErr.message}`), { status: 502 }));
      }
    },
    error: (err) => {
      const message = (err && err.content) || (err && err.message) || 'Inference request failed';
      const status = (err && err.status) || 502;
      reject(Object.assign(new Error(message), { status }));
    },
  });
}));

module.exports = { chat };
