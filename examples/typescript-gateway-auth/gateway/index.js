"use strict";
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
            if (f = 1, y && (t = y[op[0] & 2 ? "return" : op[0] ? "throw" : "next"]) && !(t = t.call(y, op[1])).done) return t;
            if (y = 0, t) op = [0, t.value];
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
var express = require("express");
var cors = require("cors");
var bodyParser = require("body-parser");
var apollo_server_express_1 = require("apollo-server-express");
var graphql_transform_schema_1 = require("graphql-transform-schema");
var graphql_tools_1 = require("graphql-tools");
var apollo_link_http_1 = require("apollo-link-http");
var node_fetch_1 = require("node-fetch");
var middleware_1 = require("graphql-playground/middleware");
var graphcool_lib_1 = require("graphcool-lib");
var signup_1 = require("./signup");
var authenticate_1 = require("./authenticate");
var loggedInUser_1 = require("./loggedInUser");
var jwt = require("jsonwebtoken");
// constants
exports.JWT_SECRET = 'oohahjeiqu4oyaiqueecho2ei';
// export const ROOT_TOKEN = '__ROOT_TOKEN__' // eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MTAwNjQwMzYsImNsaWVudElkIjoiY2l3ajBkc2V1MGY0bDAxMjJ1NDdzcXE1bSIsInByb2plY3RJZCI6ImNqOXBvN2M4dDFncHUwMTQ1cmtpOGxmdnciLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqOXBwNHpsNjJvNXkwMTQ1emh4aXdwZ2gifQ.VFem0gkCIRFDjMv71z1zH1BCyXJ26rPua679LtBu2vg
// const SERVICE_ID = '__SERVICE_ID__' // cj9po7c8t1gpu0145rki8lfvw
exports.ROOT_TOKEN = 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MTAwNjQwMzYsImNsaWVudElkIjoiY2l3ajBkc2V1MGY0bDAxMjJ1NDdzcXE1bSIsInByb2plY3RJZCI6ImNqOXBvN2M4dDFncHUwMTQ1cmtpOGxmdnciLCJwZXJtYW5lbnRBdXRoVG9rZW5JZCI6ImNqOXBwNHpsNjJvNXkwMTQ1emh4aXdwZ2gifQ.VFem0gkCIRFDjMv71z1zH1BCyXJ26rPua679LtBu2vg';
var SERVICE_ID = 'cj9po7c8t1gpu0145rki8lfvw';
// Graphcool service
var graphcool = new graphcool_lib_1.default(SERVICE_ID, {
    token: exports.ROOT_TOKEN
});
function run() {
    return __awaiter(this, void 0, void 0, function () {
        var _this = this;
        var endpoint, link, graphcoolSchema, _a, _b, graphcolSchemaWithAuthCheck, authSchema, mergedSchemas, schema, app;
        return __generator(this, function (_c) {
            switch (_c.label) {
                case 0:
                    endpoint = "https://api.graph.cool/simple/v1/" + SERVICE_ID;
                    link = new apollo_link_http_1.HttpLink({ uri: endpoint, fetchOptions: { fetch: node_fetch_1.default } });
                    _a = graphql_tools_1.makeRemoteExecutableSchema;
                    _b = {};
                    return [4 /*yield*/, graphql_tools_1.introspectSchema(link)];
                case 1:
                    graphcoolSchema = _a.apply(void 0, [(_b.schema = _c.sent(),
                            _b.link = link,
                            _b)]);
                    graphcolSchemaWithAuthCheck = graphql_transform_schema_1.transformSchema(graphcoolSchema, {
                        Mutation: {
                            '*': function (_a) {
                                var args = _a.args, resolve = _a.resolve;
                                console.log("send mutation with args: " + JSON.stringify(args));
                                return resolve(args);
                            }
                        },
                    });
                    authSchema = "\n    extend type Query {\n      loggedInUser: User\n    }\n  \n    extend type Mutation {\n      signupUser(email: String!, password: String!, name: String!): AuthenticationPayload\n      authenticateUser(email: String!, password: String!): AuthenticationPayload\n    }\n    \n    type AuthenticationPayload {\n      user: User!\n      token: String!\n    }\n  ";
                    mergedSchemas = graphql_tools_1.mergeSchemas({
                        schemas: [graphcolSchemaWithAuthCheck, authSchema],
                        resolvers: function (mergeInfo) { return ({
                            Query: {
                                loggedInUser: function (parent, args, context) { return __awaiter(_this, void 0, void 0, function () {
                                    var token, loggedInUserResult, userResult;
                                    return __generator(this, function (_a) {
                                        switch (_a.label) {
                                            case 0:
                                                token = context.req.get('Authorization').replace('Bearer ', '');
                                                return [4 /*yield*/, loggedInUser_1.default(graphcool, token)
                                                    // NOTE: User should be retrieved using `mergeInfo.delegate` but there's a bug so
                                                    // for now we're using a custom function to retrieve user data: `getUserWithId`
                                                    // const userResult = await mergeInfo.delegate('query', 'User', { id: authenticateResult.id }, context, info)
                                                ];
                                            case 1:
                                                loggedInUserResult = _a.sent();
                                                return [4 /*yield*/, getUserWithId(graphcool.api(), loggedInUserResult.id)];
                                            case 2:
                                                userResult = _a.sent();
                                                return [2 /*return*/, userResult.User];
                                        }
                                    });
                                }); }
                            },
                            Mutation: {
                                signupUser: function (parent, args) { return __awaiter(_this, void 0, void 0, function () {
                                    var signupResult, userResult;
                                    return __generator(this, function (_a) {
                                        switch (_a.label) {
                                            case 0: return [4 /*yield*/, signup_1.default(graphcool, args)
                                                // NOTE: User should be retrieved using `mergeInfo.delegate` but there's a bug so
                                                // for now we're using a custom function to retrieve user data: `getUserWithId`
                                                // const userResult = await mergeInfo.delegate('query', 'User', { id: signupResult.id }, context, info)
                                            ];
                                            case 1:
                                                signupResult = _a.sent();
                                                return [4 /*yield*/, getUserWithId(graphcool.api(), signupResult.id)];
                                            case 2:
                                                userResult = _a.sent();
                                                return [2 /*return*/, {
                                                        user: userResult.User,
                                                        token: signupResult.token
                                                    }];
                                        }
                                    });
                                }); },
                                authenticateUser: function (parent, args) { return __awaiter(_this, void 0, void 0, function () {
                                    var authenticateResult, userResult;
                                    return __generator(this, function (_a) {
                                        switch (_a.label) {
                                            case 0: return [4 /*yield*/, authenticate_1.default(graphcool, args)
                                                // NOTE: User should be retrieved using `mergeInfo.delegate` but there's a bug so
                                                // for now we're using a custom function to retrieve user data: `getUserWithId`
                                                // const userResult = await mergeInfo.delegate('query', 'User', { id: authenticateResult.id }, context, info)
                                            ];
                                            case 1:
                                                authenticateResult = _a.sent();
                                                return [4 /*yield*/, getUserWithId(graphcool.api(), authenticateResult.id)];
                                            case 2:
                                                userResult = _a.sent();
                                                return [2 /*return*/, {
                                                        user: userResult.User,
                                                        token: authenticateResult.token
                                                    }];
                                        }
                                    });
                                }); }
                            }
                        }); }
                    });
                    schema = graphql_transform_schema_1.transformSchema(mergedSchemas, {
                        Mutation: {
                            'createUser': false
                        }
                    });
                    app = express();
                    app.use('/graphql', cors(), bodyParser.json(), apollo_server_express_1.graphqlExpress(function (req) { return ({ schema: schema, context: { req: req } }); }));
                    app.use('/playground', middleware_1.express({ endpoint: '/graphql' }));
                    app.listen(5433, function () { return console.log('Server running. Open http://localhost:5433/playground to run queries.'); });
                    return [2 /*return*/];
            }
        });
    });
}
function tokenValid(token) {
    var userId = jwt.verify(token, exports.JWT_SECRET).userId;
    return userId !== null;
}
exports.tokenValid = tokenValid;
// helper function until `mergeInfo.delegate` is fixed
function getUserWithId(client, id) {
    return __awaiter(this, void 0, void 0, function () {
        var query, variables;
        return __generator(this, function (_a) {
            query = "\n    query ($id: ID!) {\n      User(id: $id) {\n        id\n        name\n        email\n        password\n        posts {\n          id\n          title\n        }\n      }\n    }\n  ";
            variables = {
                id: id,
            };
            return [2 /*return*/, client.request(query, variables)];
        });
    });
}
run().catch(console.error.bind(console));
//# sourceMappingURL=index.js.map