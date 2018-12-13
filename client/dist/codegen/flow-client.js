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
var utils_1 = require("../utils");
var prettier = require("prettier");
var codeComment_1 = require("../utils/codeComment");
var FlowGenerator = /** @class */ (function (_super) {
    __extends(FlowGenerator, _super);
    function FlowGenerator() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.genericsDelimiter = ':';
        _this.lineBreakDelimiter = ',';
        _this.partialType = '$Shape';
        _this.exportPrisma = false;
        _this.prismaInterface = 'PrismaInterface';
        return _this;
    }
    FlowGenerator.prototype.renderImports = function () {
        return "/**\n * @flow\n */\n\n" + codeComment_1.codeComment + "\n\nimport type { GraphQLSchema, DocumentNode } from 'graphql'\nimport type { BasePrismaOptions as BPOType, Options } from 'prisma-client-lib'\nimport { makePrismaClientClass, Model } from 'prisma-client-lib'\nimport { typeDefs } from './prisma-schema'\n\ntype NodePromise = Promise<Node>";
    };
    FlowGenerator.prototype.renderClientConstructor = function () {
        return "export type ClientConstructor<T> = (options?: BPOType) => T\n";
    };
    FlowGenerator.prototype.format = function (code, options) {
        if (options === void 0) { options = {}; }
        return prettier.format(code, __assign({}, options, { parser: 'flow' }));
    };
    FlowGenerator.prototype.renderAtLeastOne = function () {
        // TODO: as soon as flow has a clean solution for at least one, implement it here
        return "export type AtLeastOne<T> = $Shape<T>";
    };
    FlowGenerator.prototype.renderGraphQL = function () {
        return "$graphql: <T: mixed>(query: string, variables?: {[key: string]: mixed}) => Promise<T>;";
    };
    FlowGenerator.prototype.renderInputListType = function (type) {
        return type + "[]";
    };
    FlowGenerator.prototype.renderExists = function () {
        var queryType = this.schema.getQueryType();
        if (queryType) {
            return "" + utils_1.getExistsFlowTypes(queryType);
        }
        return '';
    };
    FlowGenerator.prototype.renderExports = function (options) {
        var args = this.renderPrismaClassArgs(options);
        return "export const Prisma: ClientConstructor<PrismaInterface> = makePrismaClientClass(" + args + ")\n\nexport const prisma = new Prisma()";
    };
    return FlowGenerator;
}(typescript_client_1.TypescriptGenerator));
exports.FlowGenerator = FlowGenerator;
//# sourceMappingURL=flow-client.js.map