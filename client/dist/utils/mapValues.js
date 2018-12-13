"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
function mapValues(obj, cb) {
    var newObj = {};
    Object.entries(obj).forEach(function (_a) {
        var key = _a[0], value = _a[1];
        newObj[key] = cb(key, value);
    });
    return newObj;
}
exports.mapValues = mapValues;
//# sourceMappingURL=mapValues.js.map