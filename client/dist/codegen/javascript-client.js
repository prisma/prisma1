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
var typescript_client_1 = require("./typescript-client");
var prettier = require("prettier");
var JavascriptGenerator = /** @class */ (function (_super) {
    __extends(JavascriptGenerator, _super);
    function JavascriptGenerator(options) {
        return _super.call(this, options) || this;
    }
    JavascriptGenerator.prototype.format = function (code, options) {
        if (options === void 0) { options = {}; }
        return prettier.format(code, __assign({}, options, { parser: 'babylon' }));
    };
    JavascriptGenerator.prototype.renderJavascript = function (options) {
        var args = this.renderPrismaClassArgs(options);
        return this.format("\"use strict\";\nObject.defineProperty(exports, \"__esModule\", { value: true });\nvar prisma_lib_1 = require(\"prisma-client-lib\");\nvar typeDefs = require(\"./prisma-schema\").typeDefs\n\n" + this.renderModels() + "\nexports.Prisma = prisma_lib_1.makePrismaClientClass(" + args + ");\nexports.prisma = new exports.Prisma();\n");
    };
    JavascriptGenerator.prototype.renderModels = function () {
        var models = this.internalTypes
            .map(function (i) { return "{\n    name: '" + i.name + "',\n    embedded: " + i.isEmbedded + "\n  }"; })
            .join(',\n');
        return "var models = [" + models + "]";
    };
    return JavascriptGenerator;
}(typescript_client_1.TypescriptGenerator));
exports.JavascriptGenerator = JavascriptGenerator;
//# sourceMappingURL=javascript-client.js.map