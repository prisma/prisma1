"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.interleave = function (strings, interpolations) {
    return interpolations.reduce(function (array, interp, i) {
        return array.concat(interp, strings[i + 1]);
    }, [strings[0]]);
};
//# sourceMappingURL=interleave.js.map