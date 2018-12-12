"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = function (d, b) {
        extendStatics = Object.setPrototypeOf ||
            ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
            function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
        return extendStatics(d, b);
    }
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
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
var Client_1 = require("./Client");
function makePrismaClientClass(_a) {
    var typeDefs = _a.typeDefs, endpoint = _a.endpoint, secret = _a.secret, models = _a.models;
    return /** @class */ (function (_super) {
        __extends(Client, _super);
        function Client(options) {
            return _super.call(this, __assign({ typeDefs: typeDefs, endpoint: endpoint, secret: secret, models: models }, options)) || this;
        }
        return Client;
    }(Client_1.Client));
}
exports.makePrismaClientClass = makePrismaClientClass;
//# sourceMappingURL=makePrismaClientClass.js.map