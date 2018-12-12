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
var __awaiter = (this && this.__awaiter) || function (thisArg, _arguments, P, generator) {
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : new P(function (resolve) { resolve(result.value); }).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
};
var __generator = (this && this.__generator) || function (thisArg, body) {
    var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
    return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
    function verb(n) { return function (v) { return step([n, v]); }; }
    function step(op) {
        if (f) throw new TypeError("Generator is already executing.");
        while (_) try {
            if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [op[0] & 2, t.value];
            switch (op[0]) {
                case 0: case 1: t = op; break;
                case 4: _.label++; return { value: op[1], done: false };
                case 5: _.label++; y = op[1]; op = [0]; continue;
                case 7: op = _.ops.pop(); _.trys.pop(); continue;
                default:
                    if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                    if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                    if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                    if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                    if (t[2]) _.ops.pop();
                    _.trys.pop(); continue;
            }
            op = body.call(thisArg, _);
        } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
        if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
    }
};
Object.defineProperty(exports, "__esModule", { value: true });
var ts = require("typescript");
var path = require("path");
var fs = require("fs");
var graphql_1 = require("graphql");
var typescript_client_1 = require("../typescript-client");
var flow_client_1 = require("../flow-client");
var child_process_1 = require("child_process");
var codeComment_1 = require("../../utils/codeComment");
var flow = require('flow-bin');
var TestTypescriptGenerator = /** @class */ (function (_super) {
    __extends(TestTypescriptGenerator, _super);
    function TestTypescriptGenerator() {
        return _super !== null && _super.apply(this, arguments) || this;
    }
    TestTypescriptGenerator.prototype.renderImports = function () {
        return codeComment_1.codeComment + "\n\nimport { DocumentNode, GraphQLSchema  } from 'graphql'\nimport { makePrismaClientClass } from '../../makePrismaClientClass'\nimport { BaseClientOptions, Model } from '../../types'\nimport { typeDefs } from './prisma-schema'";
    };
    return TestTypescriptGenerator;
}(typescript_client_1.TypescriptGenerator));
function compile(fileNames, options) {
    var program = ts.createProgram(fileNames, options);
    var emitResult = program.emit();
    var allDiagnostics = ts
        .getPreEmitDiagnostics(program)
        .concat(emitResult.diagnostics);
    allDiagnostics.forEach(function (diagnostic) {
        if (diagnostic.file) {
            var _a = diagnostic.file.getLineAndCharacterOfPosition(diagnostic.start), line = _a.line, character = _a.character;
            var message = ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n');
            console.log(diagnostic.file.fileName + " (" + (line + 1) + "," + (character +
                1) + "): " + message);
        }
        else {
            console.log("" + ts.flattenDiagnosticMessageText(diagnostic.messageText, '\n'));
        }
    });
    return emitResult.emitSkipped ? 1 : 0;
}
function testTSCompilation(typeDefs) {
    return __awaiter(this, void 0, void 0, function () {
        var schema, generator, file, artifactsPath, filePath, func;
        return __generator(this, function (_a) {
            switch (_a.label) {
                case 0:
                    schema = graphql_1.buildSchema(typeDefs);
                    generator = new TestTypescriptGenerator({
                        schema: schema,
                        internalTypes: [],
                    });
                    file = generator
                        .render({ endpoint: '"http://localhost:4466"' })
                        .toString();
                    artifactsPath = path.join(__dirname, '..', 'artifacts');
                    if (!fs.existsSync(artifactsPath)) {
                        fs.mkdirSync(artifactsPath);
                    }
                    filePath = path.join(__dirname, '..', 'artifacts', 'generated_ts.ts');
                    return [4 /*yield*/, fs.writeFileSync(filePath, file)
                        // TODO: Remove ugly way to ignore TS import error
                    ];
                case 1:
                    _a.sent();
                    func = "export const typeDefs = ''";
                    return [4 /*yield*/, fs.writeFileSync(path.join(artifactsPath, 'prisma-schema.ts'), func)];
                case 2:
                    _a.sent();
                    return [2 /*return*/, compile([filePath], {
                            noEmitOnError: true,
                            noImplicitAny: true,
                            skipLibCheck: true,
                            target: ts.ScriptTarget.ESNext,
                            module: ts.ModuleKind.CommonJS,
                        })];
            }
        });
    });
}
exports.testTSCompilation = testTSCompilation;
function testFlowCompilation(typeDefs) {
    return __awaiter(this, void 0, void 0, function () {
        var schema, generator, file, artifactsPath, filePath, flowConfig, configFilePath, stdout;
        return __generator(this, function (_a) {
            switch (_a.label) {
                case 0:
                    schema = graphql_1.buildSchema(typeDefs);
                    generator = new flow_client_1.FlowGenerator({
                        schema: schema,
                        internalTypes: [],
                    });
                    file = generator.render().toString();
                    artifactsPath = path.join(__dirname, '..', 'artifacts');
                    if (!fs.existsSync(artifactsPath)) {
                        fs.mkdirSync(artifactsPath);
                    }
                    filePath = path.join(artifactsPath, 'generated_flow.js');
                    return [4 /*yield*/, fs.writeFileSync(filePath, file)];
                case 1:
                    _a.sent();
                    flowConfig = " [ignore]\n [libs]\n [lints]\n [include] " + artifactsPath + " \n [strict]";
                    configFilePath = path.join(__dirname, '..', 'artifacts', '.flowconfig');
                    return [4 /*yield*/, fs.writeFileSync(configFilePath, flowConfig)];
                case 2:
                    _a.sent();
                    return [4 /*yield*/, new Promise(function (resolve) {
                            return child_process_1.execFile(flow, ['check', configFilePath], function (_err, stdout) {
                                resolve(stdout);
                            });
                        })];
                case 3:
                    stdout = _a.sent();
                    return [2 /*return*/, stdout.length];
            }
        });
    });
}
exports.testFlowCompilation = testFlowCompilation;
//# sourceMappingURL=compile.js.map