// Targeted core-js polyfills for mimOE Duktape runtime.
// Only import the specific modules this mim actually needs.
// With useBuiltIns: false, Babel will NOT expand these into 800+ modules.
//
// Promise           — async/await (transpiled by Babel) + direct usage
// Array.find        — used by @mimik/edge-ms-helper (getRouting)
// Array.from        — used by Object.entries polyfill chain
// Array.includes    — used by swagger middleware (content-type detection)
// String.includes   — used by swagger middleware (content-type detection)
// String.startsWith — used by securityHandlers/bearer.js (auth header parsing)
// Object.assign     — used by @mimik/edge-ms-helper (edge-pollyfill.js)
// Object.entries    — used by processors (context info mapping)
// Object.values     — used by processors and helpers
// Map / Set         — used by swagger middleware internals
// Symbol.iterator   — used by for...of loops (transpiled by Babel)
require('core-js/modules/es.promise');
require('core-js/modules/es.promise.finally');
require('core-js/modules/es.array.find');
require('core-js/modules/es.array.from');
require('core-js/modules/es.array.includes');
require('core-js/modules/es.string.includes');
require('core-js/modules/es.string.starts-with');
require('core-js/modules/es.object.assign');
require('core-js/modules/es.object.entries');
require('core-js/modules/es.object.values');
require('core-js/modules/es.map');
require('core-js/modules/es.set');
require('core-js/modules/es.symbol.iterator');
