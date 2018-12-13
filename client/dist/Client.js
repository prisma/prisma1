"use strict";
var __makeTemplateObject = (this && this.__makeTemplateObject) || function (cooked, raw) {
    if (Object.defineProperty) { Object.defineProperty(cooked, "raw", { value: raw }); } else { cooked.raw = raw; }
    return cooked;
};
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
var __rest = (this && this.__rest) || function (s, e) {
    var t = {};
    for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p) && e.indexOf(p) < 0)
        t[p] = s[p];
    if (s != null && typeof Object.getOwnPropertySymbols === "function")
        for (var i = 0, p = Object.getOwnPropertySymbols(s); i < p.length; i++) if (e.indexOf(p[i]) < 0)
            t[p[i]] = s[p[i]];
    return t;
};
Object.defineProperty(exports, "__esModule", { value: true });
var graphql_1 = require("graphql");
var mapAsyncIterator_1 = require("./utils/mapAsyncIterator");
var mapValues_1 = require("./utils/mapValues");
var graphql_tag_1 = require("graphql-tag");
var utils_1 = require("./utils");
var log = require('debug')('binding');
var jsonwebtoken_1 = require("jsonwebtoken");
var http_link_dataloader_1 = require("http-link-dataloader");
var subscriptions_transport_ws_1 = require("subscriptions-transport-ws");
var observableToAsyncIterable_1 = require("./utils/observableToAsyncIterable");
var WS = require("ws");
// to make the TS compiler happy
var instructionId = 0;
var Client = /** @class */ (function () {
    function Client(_a) {
        var typeDefs = _a.typeDefs, endpoint = _a.endpoint, secret = _a.secret, debug = _a.debug, models = _a.models;
        var _this = this;
        this._currentInstructions = {};
        this._models = [];
        this._promises = {};
        this.processInstructionsOnce = function (id) {
            if (!_this._promises[id]) {
                _this._promises[id] = _this.processInstructions(id);
            }
            return _this._promises[id];
        };
        this.processInstructions = function (id) { return __awaiter(_this, void 0, void 0, function () {
            var instructions, variables, document, operation, query, result;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        log('process instructions');
                        instructions = this._currentInstructions[id];
                        variables = this.generateSelections(instructions).variables;
                        document = this.getDocumentForInstructions(id);
                        operation = this.getOperation(instructions);
                        if (this.debug) {
                            console.log("\nQuery:");
                            query = graphql_1.print(document);
                            console.log(query);
                            if (variables && Object.keys(variables).length > 0) {
                                console.log('Variables:');
                                console.log(JSON.stringify(variables));
                            }
                        }
                        log('printed / before');
                        return [4 /*yield*/, this.execute(operation, document, variables)];
                    case 1:
                        result = _a.sent();
                        log('executed');
                        if (operation === 'subscription') {
                            return [2 /*return*/, this.mapSubscriptionPayload(result, instructions)];
                        }
                        return [2 /*return*/, this.extractPayload(result, instructions)];
                }
            });
        }); };
        this.then = function (id, resolve, reject) { return __awaiter(_this, void 0, void 0, function () {
            var result, e_1;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        _a.trys.push([0, 2, , 3]);
                        return [4 /*yield*/, this.processInstructionsOnce(id)];
                    case 1:
                        result = _a.sent();
                        this._currentInstructions[id] = [];
                        if (typeof resolve === 'function') {
                            return [2 /*return*/, resolve(result)];
                        }
                        return [3 /*break*/, 3];
                    case 2:
                        e_1 = _a.sent();
                        this._currentInstructions[id] = [];
                        if (typeof reject === 'function') {
                            return [2 /*return*/, reject(e_1)];
                        }
                        return [3 /*break*/, 3];
                    case 3: return [2 /*return*/, result];
                }
            });
        }); };
        this.catch = function (id, reject) { return __awaiter(_this, void 0, void 0, function () {
            var e_2;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        _a.trys.push([0, 2, , 3]);
                        return [4 /*yield*/, this.processInstructionsOnce(id)];
                    case 1: return [2 /*return*/, _a.sent()];
                    case 2:
                        e_2 = _a.sent();
                        this._currentInstructions[id] = [];
                        return [2 /*return*/, reject(e_2)];
                    case 3: return [2 /*return*/];
                }
            });
        }); };
        this.debug = debug;
        this.schema = graphql_1.buildSchema(typeDefs);
        this._endpoint = endpoint;
        this._secret = secret;
        this._models = models;
        this.buildMethods();
        var token = secret ? jsonwebtoken_1.sign({}, secret) : undefined;
        this.$graphql = this.buildGraphQL();
        this.$exists = this.buildExists();
        this._token = token;
        this._client = new http_link_dataloader_1.BatchedGraphQLClient(endpoint, {
            headers: token
                ? {
                    Authorization: "Bearer " + token,
                }
                : {},
        });
        this._subscriptionClient = new subscriptions_transport_ws_1.SubscriptionClient(endpoint.replace(/^http/, 'ws'), {
            connectionParams: {
                Authorization: "Bearer " + token,
            },
            inactivityTimeout: 60000,
            lazy: true,
        }, WS);
    }
    Client.prototype.getOperation = function (instructions) {
        return instructions[0].typeName.toLowerCase();
    };
    Client.prototype.getDocumentForInstructions = function (id) {
        log('process instructions');
        var instructions = this._currentInstructions[id];
        var ast = this.generateSelections(instructions).ast;
        log('generated selections');
        var variableDefinitions = ast.variableDefinitions, restAst = __rest(ast, ["variableDefinitions"]);
        var operation = this.getOperation(instructions);
        return {
            kind: graphql_1.Kind.DOCUMENT,
            definitions: [
                {
                    kind: graphql_1.Kind.OPERATION_DEFINITION,
                    operation: operation,
                    directives: [],
                    variableDefinitions: variableDefinitions,
                    selectionSet: {
                        kind: graphql_1.Kind.SELECTION_SET,
                        selections: [restAst],
                    },
                },
            ],
        };
    };
    Client.prototype.mapSubscriptionPayload = function (result, instructions) {
        var _this = this;
        return mapAsyncIterator_1.default(result, function (res) {
            var extracted = _this.extractPayload(res, instructions);
            return extracted;
        });
    };
    Client.prototype.extractPayload = function (result, instructions) {
        var pointer = result;
        var count = 0;
        while (pointer &&
            typeof pointer === 'object' &&
            !Array.isArray(pointer) &&
            count < instructions.length) {
            pointer = pointer[Object.keys(pointer)[0]];
            count++;
        }
        log('unpack it');
        return pointer;
    };
    Client.prototype.execute = function (operation, document, variables) {
        var query = graphql_1.print(document);
        if (operation === 'subscription') {
            var subscription = this._subscriptionClient.request({
                query: query,
                variables: variables,
            });
            return Promise.resolve(observableToAsyncIterable_1.observableToAsyncIterable(subscription));
        }
        return this._client.request(query, variables);
    };
    Client.prototype.generateSelections = function (instructions) {
        var _this = this;
        var variableDefinitions = [];
        var variables = {};
        var variableCounter = {};
        var ast = instructions.reduceRight(function (acc, instruction, index) {
            var args = [];
            if (instruction.args && Object.keys(instruction.args).length > 0) {
                Object.entries(instruction.args).forEach(function (_a) {
                    var name = _a[0], value = _a[1];
                    var variableName;
                    if (typeof variableCounter[name] === 'undefined') {
                        variableName = name;
                        variableCounter[name] = 0;
                    }
                    else {
                        variableCounter[name]++;
                        variableName = name + "_" + variableCounter[name];
                    }
                    variables[variableName] = value;
                    var inputArg = instruction.field.args.find(function (arg) { return arg.name === name; });
                    if (!inputArg) {
                        throw new Error("Could not find argument " + name + " for type " + _this.getTypeName(instruction.field.type));
                    }
                    variableDefinitions.push({
                        kind: graphql_1.Kind.VARIABLE_DEFINITION,
                        variable: {
                            kind: graphql_1.Kind.VARIABLE,
                            name: {
                                kind: graphql_1.Kind.NAME,
                                value: variableName,
                            },
                        },
                        type: inputArg.astNode.type,
                    });
                    args.push({
                        kind: graphql_1.Kind.ARGUMENT,
                        name: {
                            kind: graphql_1.Kind.NAME,
                            value: name,
                        },
                        value: {
                            kind: graphql_1.Kind.VARIABLE,
                            name: {
                                kind: 'Name',
                                value: variableName,
                            },
                        },
                    });
                });
            }
            var node = {
                kind: graphql_1.Kind.FIELD,
                name: {
                    kind: graphql_1.Kind.NAME,
                    value: instruction.fieldName,
                },
                arguments: args,
                directives: [],
                selectionSet: {
                    kind: graphql_1.Kind.SELECTION_SET,
                    selections: [],
                },
            };
            var type = _this.getDeepType(instruction.field.type);
            if (index === instructions.length - 1 &&
                type instanceof graphql_1.GraphQLObjectType) {
                if (instruction.fragment) {
                    if (typeof instruction.fragment === 'string') {
                        instruction.fragment = graphql_tag_1.default(templateObject_1 || (templateObject_1 = __makeTemplateObject(["\n              ", "\n            "], ["\n              ", "\n            "])), instruction.fragment);
                    }
                    node.selectionSet = node = {
                        kind: graphql_1.Kind.FIELD,
                        name: {
                            kind: graphql_1.Kind.NAME,
                            value: instruction.fieldName,
                        },
                        arguments: args,
                        directives: [],
                        selectionSet: instruction.fragment.definitions[0].selectionSet,
                    };
                }
                else {
                    var rootTypeName = _this.getDeepType(instructions[0].field.type).name;
                    node = _this.getFieldAst({
                        field: instruction.field,
                        fieldName: instruction.fieldName,
                        isRelayConnection: _this.isConnectionTypeName(rootTypeName),
                        isSubscription: instructions[0].typeName === 'Subscription',
                        args: args,
                    });
                }
            }
            if (acc) {
                node.selectionSet.selections.push(acc);
            }
            return node;
        }, null);
        return {
            ast: __assign({}, ast, { variableDefinitions: variableDefinitions }),
            variables: variables,
        };
    };
    Client.prototype.isScalar = function (field) {
        var fieldType = this.getDeepType(field.type);
        return (fieldType instanceof graphql_1.GraphQLScalarType ||
            fieldType instanceof graphql_1.GraphQLEnumType);
    };
    Client.prototype.isEmbedded = function (field) {
        var model = this._models.find(function (m) { return m.name === field.type.name; });
        return model && model.embedded;
    };
    Client.prototype.isConnectionTypeName = function (typeName) {
        return typeName.endsWith('Connection') && typeName !== 'Connection';
    };
    Client.prototype.getFieldAst = function (_a) {
        var _this = this;
        var field = _a.field, fieldName = _a.fieldName, isRelayConnection = _a.isRelayConnection, isSubscription = _a.isSubscription, args = _a.args;
        var node = {
            kind: graphql_1.Kind.FIELD,
            name: {
                kind: graphql_1.Kind.NAME,
                value: fieldName,
            },
            arguments: args,
            directives: [],
        };
        if (this.isScalar(field)) {
            return node;
        }
        node.selectionSet = {
            kind: graphql_1.Kind.SELECTION_SET,
            selections: [],
        };
        var type = this.getDeepType(field.type);
        node.selectionSet.selections = Object.entries(type.getFields())
            .filter(function (_a) {
            var subField = _a[1];
            var isScalar = _this.isScalar(subField);
            if (isScalar) {
                return true;
            }
            var fieldType = _this.getDeepType(subField.type);
            if (isRelayConnection) {
                if (subField.name === 'pageInfo' && fieldType.name === 'PageInfo') {
                    return true;
                }
                if (subField.name === 'edges' && fieldType.name.endsWith('Edge')) {
                    return true;
                }
                if (subField.name === 'node' &&
                    fieldName === 'edges' &&
                    type.name.endsWith('Edge')) {
                    return true;
                }
                return false;
            }
            if (isSubscription) {
                if (['previousValues', 'node'].includes(subField.name)) {
                    return true;
                }
                return false;
            }
            var model = _this._models && _this._models.find(function (m) { return m.name === fieldType.name; });
            var embedded = model && model.embedded;
            return embedded;
        })
            .map(function (_a) {
            var fieldName = _a[0], field = _a[1];
            return _this.getFieldAst({
                field: field,
                fieldName: fieldName,
                isRelayConnection: isRelayConnection,
                isSubscription: isSubscription,
                args: [],
            });
        });
        return node;
    };
    Client.prototype.buildMethods = function () {
        this._types = this.getTypes();
        Object.assign(this, this._types.Query);
        Object.assign(this, this._types.Mutation);
        this.$subscribe = this._types.Subscription;
    };
    Client.prototype.getTypes = function () {
        var _this = this;
        var typeMap = this.schema.getTypeMap();
        var types = Object.entries(typeMap)
            .map(function (_a) {
            var name = _a[0], type = _a[1];
            var _b;
            var value = (_b = {
                    then: _this.then,
                    catch: _this.catch
                },
                _b[Symbol.toStringTag] = 'Promise',
                _b);
            if (type instanceof graphql_1.GraphQLObjectType) {
                var fieldsArray = Object.entries(type.getFields()).concat([
                    ["$fragment", null],
                ]);
                value = __assign({}, value, fieldsArray
                    .map(function (_a) {
                    var fieldName = _a[0], field = _a[1];
                    return {
                        key: fieldName,
                        value: function (args, arg2) {
                            var id = typeof args === 'number' ? args : ++instructionId;
                            var realArgs = typeof args === 'number' ? arg2 : args;
                            _this._currentInstructions[id] =
                                _this._currentInstructions[id] || [];
                            if (fieldName === '$fragment') {
                                var currentInstructions = _this._currentInstructions[id];
                                currentInstructions[currentInstructions.length - 1].fragment = arg2;
                                return mapValues_1.mapValues(value, function (key, v) {
                                    if (typeof v === 'function') {
                                        return v.bind(_this, id);
                                    }
                                    return v;
                                });
                            }
                            else {
                                if (_this._currentInstructions[id].length === 0) {
                                    if (name === 'Mutation') {
                                        if (fieldName.startsWith('create')) {
                                            realArgs = { data: realArgs };
                                        }
                                        if (fieldName.startsWith('delete')) {
                                            realArgs = { where: realArgs };
                                        }
                                    }
                                    else if (name === 'Query' ||
                                        name === 'Subscription') {
                                        if (field.args.length === 1) {
                                            realArgs = { where: realArgs };
                                        }
                                    }
                                }
                                _this._currentInstructions[id].push({
                                    fieldName: fieldName,
                                    args: realArgs,
                                    field: field,
                                    typeName: type.name,
                                });
                                var typeName = _this.getTypeName(field.type);
                                // this is black magic. what we do here: bind both .then, .catch and all resolvers to `id`
                                return mapValues_1.mapValues(_this._types[typeName], function (key, value) {
                                    if (typeof value === 'function') {
                                        return value.bind(_this, id);
                                    }
                                    return value;
                                });
                            }
                        },
                    };
                })
                    .reduce(reduceKeyValue, {}));
            }
            return {
                key: name,
                value: value,
            };
        })
            .reduce(reduceKeyValue, {});
        return types;
    };
    Client.prototype.getTypeName = function (type) {
        if (type.ofType) {
            return this.getDeepType(type.ofType);
        }
        return type.name;
    };
    Client.prototype.getDeepType = function (type) {
        if (type.ofType) {
            return this.getDeepType(type.ofType);
        }
        return type;
    };
    Client.prototype.buildGraphQL = function () {
        var _this = this;
        return function (query, variables) {
            return _this._client.request(query, variables);
        };
    };
    Client.prototype.buildExists = function () {
        var _this = this;
        var queryType = this.schema.getQueryType();
        if (!queryType) {
            return {};
        }
        if (queryType) {
            var types = utils_1.getTypesAndWhere(queryType);
            return types.reduce(function (acc, _a) {
                var type = _a.type, pluralFieldName = _a.pluralFieldName;
                var _b;
                var firstLetterLowercaseTypeName = type[0].toLowerCase() + type.slice(1);
                return __assign({}, acc, (_b = {}, _b[firstLetterLowercaseTypeName] = function (args) {
                    // TODO: when the fragment api is there, only add one field
                    return _this[pluralFieldName]({ where: args }).then(function (res) {
                        return res.length > 0;
                    });
                }, _b));
            }, {});
        }
        return {};
    };
    return Client;
}());
exports.Client = Client;
var reduceKeyValue = function (acc, curr) {
    var _a;
    return (__assign({}, acc, (_a = {}, _a[curr.key] = curr.value, _a)));
};
var templateObject_1;
//# sourceMappingURL=Client.js.map