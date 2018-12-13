"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var flatten_1 = require("./utils/flatten");
var interleave_1 = require("./utils/interleave");
var Generator = /** @class */ (function () {
    function Generator(_a) {
        var schema = _a.schema, internalTypes = _a.internalTypes;
        this.internalTypes = [];
        this.internalTypes = internalTypes;
        this.schema = schema;
    }
    Generator.prototype.compile = function (strings) {
        var interpolations = [];
        for (var _i = 1; _i < arguments.length; _i++) {
            interpolations[_i - 1] = arguments[_i];
        }
        return flatten_1.default(interleave_1.interleave(strings, interpolations), this).join('');
    };
    return Generator;
}());
exports.Generator = Generator;
//# sourceMappingURL=Generator.js.map