let context = null;

module.exports = {
  setContext: (newContext) => {
    context = newContext;
  },
  getContext: () => (context ? Promise.resolve(context) : Promise.reject(new Error('context is not initialized'))),
};
