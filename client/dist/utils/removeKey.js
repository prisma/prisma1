"use strict";
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
Object.defineProperty(exports, "__esModule", { value: true });
function omitDeep(obj, key) {
    if (typeof obj === 'object') {
        return Object.keys(obj).reduce(function (acc, curr) {
            var _a;
            if (curr === key) {
                return acc;
            }
            return __assign({}, acc, (_a = {}, _a[curr] = omitDeep(obj[curr], key), _a));
        }, {});
    }
    return obj;
}
exports.omitDeep = omitDeep;
//# sourceMappingURL=removeKey.js.map