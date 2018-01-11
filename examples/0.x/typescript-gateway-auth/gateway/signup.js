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
var bcrypt = require("bcryptjs");
var validator = require("validator");
var jwt = require("jsonwebtoken");
var index_1 = require("./index");
var SALT_ROUNDS = 10;
function signup(graphcool, input) {
    return __awaiter(this, void 0, void 0, function () {
        var email, password, name, userExists, hash, userId, token, e_1;
        return __generator(this, function (_a) {
            switch (_a.label) {
                case 0:
                    _a.trys.push([0, 5, , 6]);
                    email = input.email, password = input.password, name = input.name;
                    if (!validator.isEmail(email)) {
                        return [2 /*return*/, { error: 'Not a valid email' }];
                    }
                    return [4 /*yield*/, getUserWithEmail(graphcool.api(), email)
                            .then(function (data) {
                            return data.User !== null;
                        })];
                case 1:
                    userExists = _a.sent();
                    if (userExists) {
                        return [2 /*return*/, { error: 'Email already in use' }];
                    }
                    return [4 /*yield*/, bcrypt.hash(password, SALT_ROUNDS)
                        // create new user
                    ];
                case 2:
                    hash = _a.sent();
                    return [4 /*yield*/, createGraphcoolUser(graphcool.api(), email, hash, name)
                        // generate node token for new User node
                    ];
                case 3:
                    userId = _a.sent();
                    return [4 /*yield*/, jwt.sign({ userId: userId }, index_1.JWT_SECRET)];
                case 4:
                    token = _a.sent();
                    return [2 /*return*/, { id: userId, token: token }];
                case 5:
                    e_1 = _a.sent();
                    console.log(e_1);
                    return [2 /*return*/, { error: 'An unexpected error occurred during signup' }];
                case 6: return [2 /*return*/];
            }
        });
    });
}
exports.default = signup;
function getUserWithEmail(client, email) {
    return __awaiter(this, void 0, void 0, function () {
        var query, variables;
        return __generator(this, function (_a) {
            query = "\n    query ($email: String!) {\n      User(email: $email) {\n        id\n      }\n    }\n  ";
            variables = {
                email: email,
            };
            return [2 /*return*/, client.request(query, variables)];
        });
    });
}
function createGraphcoolUser(api, email, password, name) {
    return __awaiter(this, void 0, void 0, function () {
        var mutation, variables;
        return __generator(this, function (_a) {
            mutation = "\n    mutation ($email: String!, $password: String!, $name: String!) {\n      createUser(\n        email: $email,\n        password: $password,\n        name: $name\n      ) {\n        id\n      }\n    }\n  ";
            variables = {
                email: email,
                password: password,
                name: name
            };
            return [2 /*return*/, api.request(mutation, variables)
                    .then(function (data) { return data.createUser.id; })];
        });
    });
}
//# sourceMappingURL=signup.js.map