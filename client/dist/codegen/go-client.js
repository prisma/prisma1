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
Object.defineProperty(exports, "__esModule", { value: true });
var Generator_1 = require("./Generator");
var upperCamelCase = require("uppercamelcase");
var getTypeNames_1 = require("../utils/getTypeNames");
var goCase = function (s) {
    var cased = upperCamelCase(s);
    return cased.startsWith('Id') ? "ID" + cased.slice(2) : cased;
};
var whereArgs = 7;
var GoGenerator = /** @class */ (function (_super) {
    __extends(GoGenerator, _super);
    function GoGenerator() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        // Tracks which types we've already printed.
        // At the moment, it only tracks FooParamsExec types.
        _this.printedTypes = {};
        _this.scalarMapping = {
            Int: 'int32',
            String: 'string',
            ID: 'string',
            Float: 'float32',
            Boolean: 'bool',
            DateTime: 'string',
            Json: 'map[string]interface{}',
            Long: 'int64',
        };
        _this.graphqlRenderers = {
            GraphQLUnionType: function (type) { return ""; },
            GraphQLObjectType: function (type) {
                var fieldMap = type.getFields();
                if (type.name === "BatchPayload") {
                    return "";
                }
                if (type.name.startsWith("Aggregate")) {
                    // We're merging all Aggregate types into a single type
                    return "";
                }
                return "\n        type " + type.name + "Exec struct {\n          exec *prisma.Exec\n        }\n\n        " + Object.keys(fieldMap)
                    .filter(function (key) {
                    var field = fieldMap[key];
                    var _a = _this.extractFieldLikeType(field), isScalar = _a.isScalar, isEnum = _a.isEnum;
                    return !isScalar && !isEnum;
                })
                    .map(function (key) {
                    // XXX this code is responsible for things like
                    // previousValues, pageInfo, aggregate, edges, and relations.
                    // It should probably be specialised like the rest of our code generation.
                    var field = fieldMap[key];
                    var args = field.args;
                    var _a = _this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName, isList = _a.isList;
                    var sTyp = "";
                    var meth = goCase(field.name) + "ParamsExec";
                    // TODO(dh): This type (FooParamsExec) is redundant.
                    // If we have a relation article.authors -> [User],
                    // then we can reuse UsersParams.
                    // The only reason we can't do it right now
                    // is because we don't have the base type's plural name available
                    // (and appending a single s doesn't work for names like Mouse)
                    if (!_this.printedTypes[meth] && field.args.length > 0) {
                        _this.printedTypes[meth] = true;
                        sTyp = "\n                type " + meth + " struct {\n                  " + args
                            .map(function (arg) { return goCase(arg.name) + " " + _this.goTypeName(_this.extractFieldLikeType(arg)); }).join('\n') + "\n                }";
                    }
                    if (field.args.length !== 0 && field.args.length !== whereArgs) {
                        throw new Error("unexpected argument count " + field.args.length);
                    }
                    if (field.args.length === whereArgs && !isList) {
                        throw new Error("looks like a getMany query but doesn't return an array");
                    }
                    if (field.args.length > 0) {
                        return sTyp + ("\n                func (instance *" + type.name + "Exec) " + goCase(field.name) + "(params *" + goCase(field.name) + "ParamsExec) *" + goCase(typeName) + "ExecArray {\n                  var wparams *prisma.WhereParams\n                  if params != nil {\n                    wparams = &prisma.WhereParams{\n                      Where: params.Where,\n                      OrderBy: (*string)(params.OrderBy),\n                      Skip: params.Skip,\n                      After: params.After,\n                      Before: params.Before,\n                      First: params.First,\n                      Last: params.Last,\n                    }\n                  }\n\n                  ret := instance.exec.Client.GetMany(\n                    instance.exec,\n                    wparams,\n                    [3]string{\"" + field.args[0].type + "\", \"" + field.args[1].type + "\", \"" + typeName + "\"},\n                    \"" + field.name + "\",\n                    []string{" + typeFields.join(',') + "})\n\n                  return &" + goCase(typeName) + "ExecArray{ret}\n                }");
                    }
                    else {
                        if (type.name.endsWith("Connection") && field.name === "aggregate") {
                            return sTyp + ("\n                  func (instance *" + type.name + "Exec) " + goCase(field.name) + "(ctx context.Context) (Aggregate, error) {\n                    ret := instance.exec.Client.GetOne(\n                      instance.exec,\n                      nil,\n                      [2]string{\"\", \"" + typeName + "\"},\n                      \"" + field.name + "\",\n                      []string{" + typeFields.join(',') + "})\n\n                    var v Aggregate\n                    _, err := ret.Exec(ctx, &v)\n                    return v, err\n                  }");
                        }
                        return sTyp + ("\n                func (instance *" + type.name + "Exec) " + goCase(field.name) + "() *" + goCase(typeName) + "Exec {\n                  ret := instance.exec.Client.GetOne(\n                    instance.exec,\n                    nil,\n                    [2]string{\"\", \"" + typeName + "\"},\n                    \"" + field.name + "\",\n                    []string{" + typeFields.join(',') + "})\n\n                  return &" + goCase(typeName) + "Exec{ret}\n                }");
                    }
                }).join('\n') + "\n\n          func (instance " + type.name + "Exec) Exec(ctx context.Context) (*" + type.name + ", error) {\n            var v " + type.name + "\n            ok, err := instance.exec.Exec(ctx, &v)\n            if err != nil {\n              return nil, err\n            }\n            if !ok {\n              return nil, ErrNoResult\n            }\n            return &v, nil\n          }\n\n          func (instance " + type.name + "Exec) Exists(ctx context.Context) (bool, error) {\n            return instance.exec.Exists(ctx)\n          }\n\n          type " + type.name + "ExecArray struct {\n            exec *prisma.Exec\n          }\n\n          func (instance " + type.name + "ExecArray) Exec(ctx context.Context) ([]" + type.name + ", error) {\n            var v []" + type.name + "\n            err := instance.exec.ExecArray(ctx, &v)\n            return v, err\n          }\n\n        type " + type.name + " struct {\n          " + Object.keys(fieldMap)
                    .filter(function (key) {
                    var field = fieldMap[key];
                    var isScalar = _this.extractFieldLikeType(field).isScalar;
                    return isScalar;
                })
                    .map(function (key) {
                    var field = fieldMap[key];
                    var fieldType = _this.extractFieldLikeType(field);
                    return goCase(field.name) + " " + _this.goTypeName(fieldType) + " " + _this.goStructTag(field);
                })
                    .join('\n') + "\n        }";
            },
            GraphQLInterfaceType: function (type) {
                if (type.name === "Node") {
                    // Don't emit code relating to generic node fetching
                    return "";
                }
                var fieldMap = type.getFields();
                return "\n      type " + goCase(type.name) + "Exec struct {\n        exec *prisma.Exec\n      }\n\n      type " + goCase(type.name) + " interface {\n        " + Object.keys(fieldMap).map(function (key) {
                    var field = fieldMap[key];
                    var typeName = _this.extractFieldLikeType(field).typeName;
                    return goCase(field.name) + "() " + (_this.scalarMapping[typeName] ||
                        typeName);
                }) + "\n      }";
            },
            GraphQLInputObjectType: function (type) {
                var fieldMap = type.getFields();
                return "\n      type " + type.name + " struct {\n        " + Object.keys(fieldMap)
                    .map(function (key) {
                    var field = fieldMap[key];
                    var fieldType = _this.extractFieldLikeType(field);
                    var typ = _this.goTypeName(fieldType);
                    return goCase(field.name) + " " + typ + " " + _this.goStructTag(field);
                }).join('\n') + "\n          }";
            },
            GraphQLScalarType: function (type) { return ""; },
            GraphQLIDType: function (type) { return ""; },
            GraphQLEnumType: function (type) {
                var enumValues = type.getValues();
                var typ = goCase(type.name);
                return "\n        type " + typ + " string\n        const (\n          " + enumValues
                    .map(function (v) { return "" + typ + goCase(v.name) + " " + typ + " = \"" + v.name + "\""; })
                    .join('\n') + "\n          )";
            },
        };
        _this.graphqlTypeRenderersForQuery = {
            GraphQLScalarType: function (type) {
                return "";
            },
            GraphQLObjectType: function (type) {
                var typeFields = type.getFields();
                return "" + Object.keys(typeFields)
                    .map(function (key) {
                    var field = typeFields[key];
                    var isScalar = _this.extractFieldLikeType(field).isScalar;
                    return isScalar ? "" + field.name : "";
                })
                    .join('\n');
            },
            GraphQLInterfaceType: function (type) {
                return "";
            },
            GraphQLUnionType: function (type) {
                return "";
            },
            GraphQLEnumType: function (type) {
                return "";
            },
            GraphQLInputObjectType: function (type) {
                var typeFields = type.getFields();
                return "" + Object.keys(typeFields)
                    .map(function (key) {
                    var field = typeFields[key];
                    return "" + field.name;
                })
                    .join('\n');
            },
        };
        return _this;
    }
    GoGenerator.prototype.goTypeName = function (fieldType) {
        var typ;
        if (fieldType.isEnum) {
            typ = goCase(fieldType.typeName);
        }
        else {
            typ = this.scalarMapping[fieldType.typeName] || fieldType.typeName;
        }
        if (fieldType.isList) {
            typ = "[]" + typ;
        }
        else if (!fieldType.isNonNull) {
            typ = "*" + typ;
        }
        return typ;
    };
    GoGenerator.prototype.shouldOmitEmpty = function (fieldType) {
        return !fieldType.isNonNull;
    };
    GoGenerator.prototype.goStructTag = function (field) {
        var s = "`json:\"" + field.name;
        if (this.shouldOmitEmpty(this.extractFieldLikeType(field))) {
            s += ",omitempty";
        }
        s += "\"`";
        return s;
    };
    GoGenerator.prototype.extractFieldLikeType = function (field) {
        var _this = this;
        var deepTypeName = this.getDeepType(field.type);
        var deepType = this.schema.getType(deepTypeName);
        var isScalar = deepType.constructor.name === 'GraphQLScalarType';
        var isEnum = deepType.constructor.name === 'GraphQLEnumType';
        var isInput = deepType.constructor.name === 'GraphQLInputObjectType';
        var isList = field.type.toString().indexOf('[') === 0 &&
            field.type.toString().indexOf(']') > -1;
        var isNonNull = field.type.toString().indexOf('!') > -1 && field.type.toString().indexOf('!]') === -1;
        var fieldMap = null;
        if (deepType.constructor.name === 'GraphQLObjectType') {
            fieldMap = deepType.getFields();
        }
        if (deepType.constructor.name === 'GraphQLInputObjectType') {
            fieldMap = deepType.getFields();
        }
        var fields = Boolean(fieldMap)
            ? Object.keys(fieldMap)
                .filter(function (key) {
                var field = fieldMap[key];
                return (_this.getDeepType(field.type).constructor.name ===
                    'GraphQLScalarType');
            })
                .map(function (key) { return "\"" + fieldMap[key].name + "\""; })
            : [];
        return {
            name: field.name,
            typeName: deepTypeName.toString(),
            type: deepType,
            typeFields: fields,
            args: field.args,
            isScalar: isScalar,
            isEnum: isEnum,
            isList: isList,
            isNonNull: isNonNull,
            isInput: isInput,
        };
    };
    GoGenerator.prototype.getDeepType = function (type) {
        if (type.ofType) {
            return this.getDeepType(type.ofType);
        }
        return type;
    };
    GoGenerator.prototype.opUpdateMany = function (field) {
        var param = this.paramsType(field, "updateMany");
        return param.code + ("\n      func (client *Client) " + goCase(field.name) + " (params " + param.type + ") *BatchPayloadExec {\n        exec := client.Client.UpdateMany(\n          prisma.UpdateParams{\n            Data: params.Data,\n            Where: params.Where,\n          },\n          [2]string{\"" + field.args[0].type + "\", \"" + field.args[1].type + "\"},\n          \"" + field.name + "\")\n        return &BatchPayloadExec{exec}\n      }");
    };
    GoGenerator.prototype.opUpdate = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        var param = this.paramsType(field, "update");
        return param.code + ("\n      func (client *Client) " + goCase(field.name) + " (params " + param.type + ") *" + goCase(typeName) + "Exec {\n        ret := client.Client.Update(\n                 prisma.UpdateParams{\n                   Data: params.Data,\n                   Where: params.Where,\n                 },\n                 [3]string{\"" + field.args[0].type + "\", \"" + field.args[1].type + "\", \"" + typeName + "\"},\n                 \"" + field.name + "\",\n                 []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "Exec{ret}\n      }");
    };
    GoGenerator.prototype.opDeleteMany = function (field) {
        return "\n      func (client *Client) " + goCase(field.name) + " (params *" + this.getDeepType(field.args[0].type) + ") *BatchPayloadExec {\n        exec := client.Client.DeleteMany(params, \"" + field.args[0].type + "\", \"" + field.name + "\")\n        return &BatchPayloadExec{exec}\n      }";
    };
    GoGenerator.prototype.opDelete = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        return "\n      func (client *Client) " + goCase(field.name) + " (params " + this.getDeepType(field.args[0].type) + ") *" + goCase(typeName) + "Exec {\n        ret := client.Client.Delete(\n          params,\n          [2]string{\"" + field.args[0].type + "\", \"" + typeName + "\"},\n          \"" + field.name + "\",\n          []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "Exec{ret}\n      }";
    };
    GoGenerator.prototype.opGetOne = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        return "\n      func (client *Client) " + goCase(field.name) + " (params " + this.getDeepType(field.args[0].type) + ") *" + goCase(typeName) + "Exec {\n        ret := client.Client.GetOne(\n          nil,\n          params,\n          [2]string{\"" + field.args[0].type + "\", \"" + typeName + "\"},\n          \"" + field.name + "\",\n          []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "Exec{ret}\n      }";
    };
    GoGenerator.prototype.opGetMany = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        var param = this.paramsType(field);
        return param.code + ("\n      func (client *Client) " + goCase(field.name) + " (params *" + param.type + ") *" + goCase(typeName) + "ExecArray {\n        var wparams *prisma.WhereParams\n        if params != nil {\n          wparams = &prisma.WhereParams{\n            Where: params.Where,\n            OrderBy: (*string)(params.OrderBy),\n            Skip: params.Skip,\n            After: params.After,\n            Before: params.Before,\n            First: params.First,\n            Last: params.Last,\n          }\n        }\n\n        ret := client.Client.GetMany(\n          nil,\n          wparams,\n          [3]string{\"" + field.args[0].type + "\", \"" + field.args[1].type + "\", \"" + typeName + "\"},\n          \"" + field.name + "\",\n          []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "ExecArray{ret}\n      }");
    };
    GoGenerator.prototype.opGetConnection = function (field) {
        // TODO(dh): Connections are not yet implemented
        var typeName = this.extractFieldLikeType(field).typeName;
        var param = this.paramsType(field);
        return param.code + ("\n      func (client *Client) " + goCase(field.name) + " (params *" + param.type + ") (" + goCase(typeName) + "Exec) {\n        panic(\"not implemented\")\n      }");
    };
    GoGenerator.prototype.opCreate = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        return "\n      func (client *Client) " + goCase(field.name) + " (params " + this.getDeepType(field.args[0].type) + ") *" + goCase(typeName) + "Exec {\n        ret := client.Client.Create(\n          params,\n          [2]string{\"" + field.args[0].type + "\", \"" + typeName + "\"},\n          \"" + field.name + "\",\n          []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "Exec{ret}\n      }";
    };
    GoGenerator.prototype.opUpsert = function (field) {
        var _a = this.extractFieldLikeType(field), typeFields = _a.typeFields, typeName = _a.typeName;
        var param = this.paramsType(field, "upsert");
        return param.code + ("\n      func (client *Client) " + goCase(field.name) + " (params " + param.type + ") *" + goCase(typeName) + "Exec {\n        uparams := &prisma.UpsertParams{\n          Where:  params.Where,\n          Create: params.Create,\n          Update: params.Update,\n        }\n        ret := client.Client.Upsert(\n          uparams,\n          [4]string{\"" + field.args[0].type + "\", \"" + field.args[1].type + "\", \"" + field.args[2].type + "\",\"" + typeName + "\"},\n          \"" + field.name + "\",\n          []string{" + typeFields.join(',') + "})\n\n        return &" + goCase(typeName) + "Exec{ret}\n      }");
    };
    GoGenerator.prototype.paramsType = function (field, verb) {
        var _this = this;
        var type = goCase(field.name) + "Params";
        if (verb) {
            // Mangle the name from <verb><noun>Params to <noun><verb>Params.
            // When the noun is in its plural form, turn it into its singular form.
            var arg = field.args.find(function (arg) { return arg.name === "where"; });
            if (!arg) {
                throw new Error("couldn't find expected 'where' argument");
            }
            var match = arg.type.toString().match("^(.+)Where(?:Unique)?Input!?$");
            if (match === null) {
                throw new Error("couldn't determine type name");
            }
            type = match[1] + goCase(verb) + "Params";
        }
        var code = "\n      type " + type + " struct {\n        " + field.args
            .map(function (arg) {
            var fieldType = _this.extractFieldLikeType(arg);
            var typ = _this.goTypeName(fieldType);
            return goCase(arg.name) + " " + typ + " " + _this.goStructTag(arg);
        })
            .join('\n') + "\n      }";
        return { code: code, type: type };
    };
    GoGenerator.prototype.printOperation = function (fields, operation, options) {
        var _this = this;
        return Object.keys(fields)
            .map(function (key) {
            var field = fields[key];
            var isList = _this.extractFieldLikeType(field).isList;
            // FIXME(dh): This is brittle. A model may conceivably be named "Many",
            // in which case updateMany would be updating a single instance of Many.
            // The same issue applies to many other prefixes.
            if (operation === "mutation") {
                if (field.name.startsWith("updateMany")) {
                    return _this.opUpdateMany(field);
                }
                if (field.name.startsWith("update")) {
                    return _this.opUpdate(field);
                }
                if (field.name.startsWith("deleteMany")) {
                    return _this.opDeleteMany(field);
                }
                if (field.name.startsWith("delete")) {
                    return _this.opDelete(field);
                }
                if (field.name.startsWith("create")) {
                    return _this.opCreate(field);
                }
                if (field.name.startsWith("upsert")) {
                    return _this.opUpsert(field);
                }
                throw new Error("unsupported mutation operation on field " + field.name);
            }
            if (operation === "query") {
                if (!isList && field.args.length === 1 && field.name !== "node") {
                    return _this.opGetOne(field);
                }
                if (isList && field.args.length === whereArgs) {
                    return _this.opGetMany(field);
                }
                if (!isList && field.args.length === whereArgs && field.name.endsWith("Connection")) {
                    return _this.opGetConnection(field);
                }
                if (field.name === "node") {
                    // Don't emit generic Node fetching
                    return "";
                }
                throw new Error("unsupported query operation on field " + field.name);
            }
            throw new Error("unsupported operation " + operation);
        })
            .join('\n');
    };
    GoGenerator.prototype.printEndpoint = function (options) {
        if (options.endpoint.startsWith('process.env')) {
            // Find a better way to generate Go env construct
            var envVariable = ("" + options.endpoint
                .replace('process.env[', '')
                .replace(']', ''))
                .replace("'", '')
                .replace("'", '');
            return "os.Getenv(\"" + envVariable + "\")";
        }
        else {
            return "\"" + options.endpoint.replace("'", '').replace("'", '') + "\"";
        }
    };
    GoGenerator.prototype.printSecret = function (options) {
        if (!options.secret) {
            return "\"\"";
        }
        else {
            if (options.secret.startsWith('${process.env')) {
                // Find a better way to generate Go env construct
                var envVariable = ("" + options.secret
                    .replace('${process.env[', '')
                    .replace(']}', ''))
                    .replace("'", '')
                    .replace("'", '');
                return "os.Getenv(\"" + envVariable + "\")";
            }
            else {
                return "\"" + options.secret.replace("'", '').replace("'", '') + "\"";
            }
        }
    };
    GoGenerator.prototype.render = function (options) {
        var _this = this;
        var typeNames = getTypeNames_1.getTypeNames(this.schema);
        var typeMap = this.schema.getTypeMap();
        var queryType = this.schema.getQueryType();
        var queryFields = queryType.getFields();
        var mutationType = this.schema.getMutationType();
        var mutationFields = mutationType.getFields();
        // Code in fixed shouldn't contain any dynamic content.
        // It could equally live in its own file
        // to which generated code gets appened.
        var fixed = "\n    // Code generated by Prisma CLI (https://github.com/prisma/prisma). DO NOT EDIT.\n\npackage prisma\n\nimport (\n\t\"context\"\n  \"errors\"\n\n\t\"github.com/prisma/prisma-client-lib-go\"\n\n\t\"github.com/machinebox/graphql\"\n)\n\nvar ErrNoResult = errors.New(\"query returned no result\")\n\nfunc Str(v string) *string { return &v }\nfunc Int32(v int32) *int32 { return &v }\nfunc Bool(v bool) *bool    { return &v }\n\ntype BatchPayloadExec struct {\n\texec *prisma.BatchPayloadExec\n}\n\nfunc (exec *BatchPayloadExec) Exec(ctx context.Context) (BatchPayload, error) {\n\tbp, err := exec.exec.Exec(ctx)\n    return BatchPayload(bp), err\n}\n\ntype BatchPayload struct {\n\tCount int64 `json:\"count\"`\n}\n\ntype Aggregate struct {\n\tCount int64 `json:\"count\"`\n}\n\ntype Client struct {\n\tClient *prisma.Client\n}\n\ntype Options struct {\n  Endpoint  string\n  Secret    string\n}\n\nfunc New(options *Options, opts ...graphql.ClientOption) *Client {\n  endpoint := DefaultEndpoint\n  secret   := Secret\n\tif options != nil {\n    endpoint = options.Endpoint\n    secret = options.Secret\n\t}\n\treturn &Client{\n\t\tClient: prisma.New(endpoint, secret, opts...),\n\t}\n}\n\nfunc (client *Client) GraphQL(ctx context.Context, query string, variables map[string]interface{}) (map[string]interface{}, error) {\n\treturn client.Client.GraphQL(ctx, query, variables)\n}\n";
        // Dynamic contains the parts of the generated code that are dynamically generated.
        var dynamic = "\n\nvar DefaultEndpoint = " + this.printEndpoint(options) + "\nvar Secret          = " + this.printSecret(options) + "\n\n" + this.printOperation(queryFields, 'query', options) + "\n\n" + this.printOperation(mutationFields, 'mutation', options) + "\n\n" + typeNames
            .map(function (key) {
            var type = typeMap[key];
            return _this.graphqlRenderers[type.constructor.name]
                ? _this.graphqlRenderers[type.constructor.name](type)
                : "// No GraphQL Renderer for Type " + type.name + " of type " + type.constructor.name;
        })
            .join('\n') + "\n        ";
        return fixed + dynamic;
    };
    return GoGenerator;
}(Generator_1.Generator));
exports.GoGenerator = GoGenerator;
//# sourceMappingURL=go-client.js.map