"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var Client_1 = require("./Client");
exports.Client = Client_1.Client;
var Generator_1 = require("./codegen/Generator");
exports.Generator = Generator_1.Generator;
var javascript_client_1 = require("./codegen/javascript-client");
exports.JavascriptGenerator = javascript_client_1.JavascriptGenerator;
var typescript_client_1 = require("./codegen/typescript-client");
exports.TypescriptGenerator = typescript_client_1.TypescriptGenerator;
var typescript_definitions_1 = require("./codegen/typescript-definitions");
exports.TypescriptDefinitionsGenerator = typescript_definitions_1.TypescriptDefinitionsGenerator;
var go_client_1 = require("./codegen/go-client");
exports.GoGenerator = go_client_1.GoGenerator;
var flow_client_1 = require("./codegen/flow-client");
exports.FlowGenerator = flow_client_1.FlowGenerator;
var makePrismaClientClass_1 = require("./makePrismaClientClass");
exports.makePrismaClientClass = makePrismaClientClass_1.makePrismaClientClass;
//# sourceMappingURL=index.js.map