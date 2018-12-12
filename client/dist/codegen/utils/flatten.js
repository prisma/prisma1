"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var flatten = function (chunks, executionContext) {
    return chunks.reduce(function (ruleSet, chunk) {
        /* Remove falsey values */
        if (chunk === undefined ||
            chunk === null ||
            chunk === false ||
            chunk === '') {
            return ruleSet;
        }
        /* Flatten ruleSet */
        if (Array.isArray(chunk)) {
            return ruleSet.concat(flatten(chunk, executionContext));
        }
        /* Either execute or defer the function */
        if (typeof chunk === 'function') {
            return executionContext
                ? ruleSet.concat.apply(ruleSet, flatten([chunk(executionContext)], executionContext)) : ruleSet.concat(chunk);
        }
        return ruleSet.concat(chunk.toString());
    }, []);
};
exports.default = flatten;
//# sourceMappingURL=flatten.js.map