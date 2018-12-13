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
Object.defineProperty(exports, "__esModule", { value: true });
var graphql_1 = require("graphql");
var Generator_1 = require("./Generator");
var utils_1 = require("../utils");
var flatten = require("lodash.flatten");
var prettier = require("prettier");
var codeComment_1 = require("../utils/codeComment");
var TypescriptGenerator = /** @class */ (function (_super) {
    __extends(TypescriptGenerator, _super);
    function TypescriptGenerator() {
        var _this = _super !== null && _super.apply(this, arguments) || this;
        _this.genericsDelimiter = '=';
        _this.lineBreakDelimiter = '';
        _this.partialType = 'Partial';
        _this.prismaInterface = 'Prisma';
        _this.exportPrisma = true;
        _this.scalarMapping = {
            Int: 'number',
            String: 'string',
            ID: 'string | number',
            Float: 'number',
            Boolean: 'boolean',
            DateTimeInput: 'Date | string',
            DateTimeOutput: 'string',
            Json: 'any',
        };
        _this.graphqlRenderers = {
            GraphQLUnionType: function (type) {
                return _this.renderDescription(type.description) + "export type " + type.name + " = " + type
                    .getTypes()
                    .map(function (t) { return t.name; })
                    .join(' | ');
            },
            GraphQLObjectType: function (type) {
                return (_this.renderInterfaceOrObject(type, true) +
                    '\n\n' +
                    _this.renderInterfaceOrObject(type, false) +
                    '\n\n' +
                    _this.renderInterfaceOrObject(type, false, true));
            },
            GraphQLInterfaceType: function (type) { return _this.renderInterfaceOrObject(type); },
            GraphQLInputObjectType: function (type) {
                var fieldDefinition = Object.keys(type.getFields())
                    .map(function (f) {
                    var field = type.getFields()[f];
                    var isOptional = !graphql_1.isNonNullType(field.type);
                    return "  " + _this.renderFieldName(field, false) + (isOptional ? '?' : '') + ": " + _this.renderInputFieldType(field.type);
                })
                    .join(_this.lineBreakDelimiter + "\n");
                var interfaces = [];
                if (type instanceof graphql_1.GraphQLObjectType) {
                    interfaces = type.getInterfaces();
                }
                return _this.renderInterfaceWrapper(type.name, type.description, interfaces, fieldDefinition);
            },
            GraphQLScalarType: function (type) {
                if (type.name === 'ID') {
                    return _this.graphqlRenderers.GraphQLIDType(type);
                }
                return (type.description
                    ? "/*\n" + type.description + "\n*/\n"
                    : '') + "export type " + type.name + " = " + (_this.scalarMapping[type.name] || 'string');
            },
            GraphQLIDType: function (type) {
                return (type.description
                    ? "/*\n" + type.description + "\n*/\n"
                    : '') + "export type " + type.name + "_Input = " + (_this.scalarMapping[type.name] ||
                    'string') + "\nexport type " + type.name + "_Output = string";
            },
            GraphQLEnumType: function (type) {
                if (type.name === 'PrismaDatabase') {
                    return "";
                }
                return _this.renderDescription(type.description) + "export type " + type.name + " = " + type
                    .getValues()
                    .map(function (e) { return "  '" + e.name + "'"; })
                    .join(' |\n');
            },
        };
        return _this;
    }
    TypescriptGenerator.prototype.format = function (code, options) {
        if (options === void 0) { options = {}; }
        return prettier.format(code, __assign({}, options, { parser: 'typescript' }));
    };
    TypescriptGenerator.prototype.renderAtLeastOne = function () {
        return "export type AtLeastOne<T, U = {[K in keyof T]: Pick<T, K> }> = Partial<T> & U[keyof U]";
    };
    TypescriptGenerator.prototype.renderModels = function () {
        var models = this.internalTypes
            .map(function (i) { return "{\n    name: '" + i.name + "',\n    embedded: " + i.isEmbedded + "\n  }"; })
            .join(',\n');
        return "export const models: Model[] = [" + models + "]";
    };
    TypescriptGenerator.prototype.render = function (options) {
        var queries = this.renderQueries();
        var mutations = this.renderMutations();
        return this.format(this.compile(templateObject_1 || (templateObject_1 = __makeTemplateObject(["", "\n\n", "\n\nexport interface Exists {\n", "\n}\n\nexport interface Node {}\n\nexport type FragmentableArray<T> = Promise<Array<T>> & Fragmentable\n\nexport interface Fragmentable {\n  $fragment<T>(fragment: string | DocumentNode): Promise<T>\n}\n\n", " interface ", " {\n  $exists: Exists;\n  $graphql: <T ", " any>(query: string, variables?: {[key: string]: any}) => Promise<T>;\n\n  /**\n   * Queries\n  */\n\n", "", "\n\n  /**\n   * Mutations\n  */\n\n", "", "\n\n\n  /**\n   * Subscriptions\n  */\n\n  $subscribe: Subscription;\n\n}\n\nexport interface Subscription {\n", "\n}\n\n", "\n\n/**\n * Types\n*/\n\n", "\n\n/**\n * Model Metadata\n*/\n\n", "\n\n/**\n * Type Defs\n*/\n\n", "\n"], ["\\\n", "\n\n", "\n\nexport interface Exists {\\n", "\\n}\n\nexport interface Node {}\n\nexport type FragmentableArray<T> = Promise<Array<T>> & Fragmentable\n\nexport interface Fragmentable {\n  $fragment<T>(fragment: string | DocumentNode): Promise<T>\n}\n\n", " interface ", " {\n  $exists: Exists;\n  $graphql: <T ",
            " any>(query: string, variables?: {[key: string]: any}) => Promise<T>;\n\n  /**\n   * Queries\n  */\n\n", "", "\n\n  /**\n   * Mutations\n  */\n\n", "", "\n\n\n  /**\n   * Subscriptions\n  */\n\n  $subscribe: Subscription;\n\n}\n\nexport interface Subscription {\n", "\n}\n\n", "\n\n/**\n * Types\n*/\n\n", "\n\n/**\n * Model Metadata\n*/\n\n", "\n\n/**\n * Type Defs\n*/\n\n", "\n"])), this.renderImports(), this.renderAtLeastOne(), this.renderExists(), this.exportPrisma ? 'export' : '', this.prismaInterface, this.genericsDelimiter, queries, queries.length > 0 ? ';' : '', mutations, mutations.length > 0 ? ';' : '', this.renderSubscriptions(), this.renderClientConstructor, this.renderTypes(), this.renderModels(), this.renderExports(options)));
    };
    TypescriptGenerator.prototype.renderClientConstructor = function () {
        return "export interface ClientConstructor<T> {\n  new(options?: BaseClientOptions): T\n}";
    };
    TypescriptGenerator.prototype.renderImports = function () {
        return codeComment_1.codeComment + "\n\nimport { DocumentNode, GraphQLSchema } from 'graphql'\nimport { makePrismaClientClass, BaseClientOptions, Model } from 'prisma-client-lib'\nimport { typeDefs } from './prisma-schema'";
    };
    TypescriptGenerator.prototype.renderPrismaClassArgs = function (options) {
        var endpointString = '';
        var secretString = '';
        if (options) {
            if (options.endpoint) {
                endpointString = options.endpoint
                    ? ", endpoint: " + options.endpoint
                    : '';
            }
            if (options.secret) {
                secretString = options.secret ? ", secret: " + options.secret : '';
            }
        }
        return "{typeDefs, models" + endpointString + secretString + "}";
    };
    TypescriptGenerator.prototype.renderExports = function (options) {
        var args = this.renderPrismaClassArgs(options);
        return "export const Prisma = makePrismaClientClass<ClientConstructor<" + this.prismaInterface + ">>(" + args + ")\nexport const prisma = new Prisma()";
    };
    TypescriptGenerator.prototype.renderTypedefs = function () {
        return ('export const typeDefs = /* GraphQL */ `' +
            graphql_1.printSchema(this.schema).replace(/`/g, '\\`') +
            '`');
    };
    TypescriptGenerator.prototype.renderExists = function () {
        var queryType = this.schema.getQueryType();
        if (queryType) {
            return "" + utils_1.getExistsTypes(queryType);
        }
        return '';
    };
    TypescriptGenerator.prototype.renderQueries = function () {
        var queryType = this.schema.getQueryType();
        if (!queryType) {
            return '';
        }
        return this.renderMainMethodFields('query', queryType.getFields(), false);
    };
    TypescriptGenerator.prototype.renderMutations = function () {
        var mutationType = this.schema.getMutationType();
        if (!mutationType) {
            return '';
        }
        return this.renderMainMethodFields('mutation', mutationType.getFields(), true);
    };
    TypescriptGenerator.prototype.renderSubscriptions = function () {
        var queryType = this.schema.getSubscriptionType();
        if (!queryType) {
            return '';
        }
        return this.renderMainMethodFields('subscription', queryType.getFields(), false);
    };
    TypescriptGenerator.prototype.getTypeNames = function () {
        var ast = this.schema;
        // Create types
        return Object.keys(ast.getTypeMap())
            .filter(function (typeName) { return !typeName.startsWith('__'); })
            .filter(function (typeName) { return typeName !== ast.getQueryType().name; })
            .filter(function (typeName) {
            return ast.getMutationType()
                ? typeName !== ast.getMutationType().name
                : true;
        })
            .filter(function (typeName) {
            return ast.getSubscriptionType()
                ? typeName !== ast.getSubscriptionType().name
                : true;
        })
            .sort(function (a, b) {
            return ast.getType(a).constructor.name <
                ast.getType(b).constructor.name
                ? -1
                : 1;
        });
    };
    TypescriptGenerator.prototype.renderTypes = function () {
        var _this = this;
        var typeNames = this.getTypeNames();
        return flatten(typeNames.map(function (typeName) {
            var forbiddenTypeNames = ['then', 'catch'];
            if (forbiddenTypeNames.includes(typeName)) {
                throw new Error("Cannot use " + typeName + " as a type name as it is reserved.");
            }
            var type = _this.schema.getTypeMap()[typeName];
            if (typeName === 'DateTime') {
                return [
                    _this.graphqlRenderers.GraphQLScalarType({
                        name: 'DateTimeInput',
                        description: 'DateTime scalar input type, allowing Date',
                    }),
                    _this.graphqlRenderers.GraphQLScalarType({
                        name: 'DateTimeOutput',
                        description: 'DateTime scalar output type, which is always a string',
                    }),
                ];
            }
            return _this.graphqlRenderers[type.constructor.name]
                ? _this.graphqlRenderers[type.constructor.name](type)
                : null;
        })).join('\n\n');
    };
    TypescriptGenerator.prototype.renderArgs = function (field, isMutation, isTopLevel) {
        var _this = this;
        if (isMutation === void 0) { isMutation = false; }
        if (isTopLevel === void 0) { isTopLevel = false; }
        var args = field.args;
        var hasArgs = args.length > 0;
        var allOptional = args.reduce(function (acc, curr) {
            if (!acc) {
                return false;
            }
            return !graphql_1.isNonNullType(curr.type);
        }, true);
        // hard-coded for Prisma ease-of-use
        if (isMutation && field.name.startsWith('create')) {
            return "data" + (allOptional ? '?' : '') + ": " + this.renderInputFieldTypeHelper(args[0], isMutation);
        }
        else if ((isMutation && field.name.startsWith('delete')) || // either it's a delete mutation
            (!isMutation &&
                isTopLevel &&
                args.length === 1 &&
                (graphql_1.isObjectType(field.type) || graphql_1.isObjectType(field.type.ofType))) // or a top-level single query
        ) {
            return "where" + (allOptional ? '?' : '') + ": " + this.renderInputFieldTypeHelper(args[0], isMutation);
        }
        return "args" + (allOptional ? '?' : '') + ": {" + (hasArgs ? ' ' : '') + args
            .map(function (a) {
            return "" + _this.renderFieldName(a, false) + (graphql_1.isNonNullType(a.type) ? '' : '?') + ": " + _this.renderInputFieldTypeHelper(a, isMutation);
        })
            .join(', ') + (args.length > 0 ? ' ' : '') + "}";
    };
    TypescriptGenerator.prototype.renderInputFieldTypeHelper = function (field, isMutation) {
        return this.renderFieldType({
            field: field,
            node: false,
            input: true,
            partial: false,
            renderFunction: false,
            isMutation: isMutation,
            operation: false,
            embedded: false,
        });
    };
    TypescriptGenerator.prototype.renderMainMethodFields = function (operation, fields, isMutation) {
        var _this = this;
        if (isMutation === void 0) { isMutation = false; }
        return Object.keys(fields)
            .filter(function (f) {
            var field = fields[f];
            return !(field.name === 'executeRaw' && isMutation);
        })
            .map(function (f) {
            var field = fields[f];
            return "    " + field.name + ": (" + _this.renderArgs(field, isMutation, true) + ") => " + _this.renderFieldType({
                field: field,
                node: false,
                input: false,
                partial: false,
                renderFunction: false,
                isMutation: isMutation,
                isSubscription: operation === 'subscription',
                operation: true,
                embedded: false,
            });
        })
            .join(';\n');
    };
    TypescriptGenerator.prototype.getDeepType = function (type) {
        if (type.ofType) {
            return this.getDeepType(type.ofType);
        }
        return type;
    };
    TypescriptGenerator.prototype.getInternalTypeName = function (type) {
        var deepType = this.getDeepType(type);
        var name = String(deepType);
        return name === 'ID' ? 'ID_Output' : name;
    };
    TypescriptGenerator.prototype.getPayloadType = function (operation) {
        if (operation === 'subscription') {
            return "Promise<AsyncIterator<T>>";
        }
        return "Promise<T>";
    };
    TypescriptGenerator.prototype.isEmbeddedType = function (type) {
        var internalType = this.internalTypes.find(function (i) { return i.name === type.name; });
        if (internalType && internalType.isEmbedded) {
            return true;
        }
        return false;
    };
    TypescriptGenerator.prototype.renderInterfaceOrObject = function (type, node, subscription) {
        var _this = this;
        if (node === void 0) { node = true; }
        if (subscription === void 0) { subscription = false; }
        var fields = type.getFields();
        if (node && this.isConnectionType(type)) {
            return this.renderConnectionType(type);
        }
        if (node && this.isSubscriptionType(type)) {
            return this.renderSubscriptionType(type);
        }
        var fieldDefinition = Object.keys(fields)
            .filter(function (f) {
            var deepType = _this.getDeepType(fields[f].type);
            return node
                ? !graphql_1.isObjectType(deepType) || _this.isEmbeddedType(deepType)
                : true;
        })
            .map(function (f) {
            var field = fields[f];
            var deepType = _this.getDeepType(fields[f].type);
            var embedded = _this.isEmbeddedType(deepType);
            return "  " + _this.renderFieldName(field, node) + ": " + _this.renderFieldType({
                field: field,
                node: node,
                input: false,
                partial: false,
                renderFunction: true,
                isMutation: false,
                isSubscription: subscription,
                operation: false,
                embedded: embedded,
            });
        })
            .join(this.lineBreakDelimiter + "\n");
        var interfaces = [];
        if (type instanceof graphql_1.GraphQLObjectType) {
            interfaces = type.getInterfaces();
        }
        return this.renderInterfaceWrapper("" + type.name + (node ? '' : ''), type.description, interfaces, fieldDefinition, !node, subscription);
    };
    TypescriptGenerator.prototype.renderFieldName = function (field, node) {
        if (!node) {
            return "" + field.name;
        }
        return "" + field.name + (graphql_1.isNonNullType(field.type) ? '' : '?');
    };
    TypescriptGenerator.prototype.wrapType = function (type, subscription, isArray) {
        if (subscription === void 0) { subscription = false; }
        if (isArray === void 0) { isArray = false; }
        if (subscription) {
            return "Promise<AsyncIterator<" + type + ">>";
        }
        if (isArray) {
            return "FragmentableArray<" + type + ">";
        }
        return "Promise<" + type + ">";
    };
    TypescriptGenerator.prototype.renderFieldType = function (_a) {
        var field = _a.field, node = _a.node, input = _a.input, partial = _a.partial, renderFunction = _a.renderFunction, _b = _a.isMutation, isMutation = _b === void 0 ? false : _b, _c = _a.isSubscription, isSubscription = _c === void 0 ? false : _c, _d = _a.operation, operation = _d === void 0 ? false : _d, _e = _a.embedded, embedded = _e === void 0 ? false : _e;
        var type = field.type;
        var deepType = this.getDeepType(type);
        var isList = graphql_1.isListType(type) || graphql_1.isListType(type.ofType);
        var isOptional = !(graphql_1.isNonNullType(type) || graphql_1.isNonNullType(type.ofType));
        var isScalar = graphql_1.isScalarType(deepType) || graphql_1.isEnumType(deepType);
        var isInput = field.astNode.kind === 'InputValueDefinition';
        // const isObject = isObjectType(deepType)
        var typeString = this.getInternalTypeName(type);
        if (typeString === 'DateTime') {
            if (isInput) {
                typeString += 'Input';
            }
            else {
                typeString += 'Output';
            }
        }
        var addSubscription = !partial && isSubscription && !isScalar;
        if (operation &&
            !node &&
            !isInput &&
            !isList &&
            !isScalar &&
            !addSubscription) {
            return typeString === 'Node' ? "Node" : typeString + "Promise";
        }
        if ((node || isList) && !isScalar && !addSubscription) {
            typeString += "";
        }
        if (addSubscription) {
            typeString += 'Subscription';
        }
        if (isScalar && !isInput) {
            if (isList) {
                typeString += "[]";
            }
            if (node) {
                return typeString;
            }
            else {
                return "(" + (field.args && field.args.length > 0
                    ? this.renderArgs(field, isMutation)
                    : '') + ") => " + this.wrapType(typeString, isSubscription);
            }
        }
        if ((isList || node) && isOptional) {
            typeString += ' | null';
        }
        if (isList) {
            if (isScalar) {
                return typeString + "[]";
            }
            else {
                if (renderFunction) {
                    return "<T " + this.genericsDelimiter + " " + this.wrapType("" + typeString, isSubscription, true) + "> (" + (field.args && field.args.length > 0
                        ? this.renderArgs(field, isMutation, false)
                        : '') + ") => T";
                }
                else {
                    return this.wrapType(typeString, isSubscription, true);
                }
            }
        }
        if (partial) {
            typeString = this.partialType + "<" + typeString + ">";
        }
        if (embedded && node) {
            return typeString;
        }
        if (node && (!isInput || isScalar)) {
            return this.wrapType("" + typeString, isSubscription);
        }
        if (isInput || !renderFunction) {
            return typeString;
        }
        var promiseString = !isList && !isScalar && !isSubscription ? 'Promise' : '';
        return "<T " + this.genericsDelimiter + " " + typeString + promiseString + ">(" + (field.args && field.args.length > 0
            ? this.renderArgs(field, isMutation, false)
            : '') + ") => T";
    };
    TypescriptGenerator.prototype.renderInputFieldType = function (type) {
        if (graphql_1.isNonNullType(type)) {
            return this.renderInputFieldType(type.ofType);
        }
        if (graphql_1.isListType(type)) {
            var inputType = this.renderInputFieldType(type.ofType);
            if (inputType === 'DateTime') {
                inputType += 'Input';
            }
            return this.renderInputListType(inputType);
        }
        var name = type.name;
        if (name === 'DateTime') {
            name += 'Input';
        }
        return "" + name + (type.name === 'ID' ? '_Input' : '');
    };
    TypescriptGenerator.prototype.renderInputListType = function (type) {
        return type + "[] | " + type;
    };
    TypescriptGenerator.prototype.renderTypeWrapper = function (typeName, typeDescription, fieldDefinition) {
        return this.renderDescription(typeDescription) + "export type " + typeName + " = {\n" + fieldDefinition + "\n}";
    };
    TypescriptGenerator.prototype.renderInterfaceWrapper = function (typeName, typeDescription, interfaces, fieldDefinition, promise, subscription) {
        var actualInterfaces = promise
            ? [
                {
                    name: subscription
                        ? "Promise<AsyncIterator<" + typeName + ">>"
                        : "Promise<" + typeName + ">",
                },
                {
                    name: 'Fragmentable',
                },
            ].concat(interfaces)
            : interfaces;
        return "" + this.renderDescription(typeDescription) + (
        // TODO: Find a better solution than the hacky replace to remove ? from inside AtLeastOne
        typeName.includes('WhereUniqueInput')
            ? "export type " + typeName + " = AtLeastOne<{\n        " + fieldDefinition.replace('?:', ':') + "\n      }>"
            : "export interface " + typeName + (typeName === 'Node' ? 'Node' : '') + (promise && !subscription ? 'Promise' : '') + (subscription ? 'Subscription' : '') + (actualInterfaces.length > 0
                ? " extends " + actualInterfaces.map(function (i) { return i.name; }).join(', ')
                : '') + " {\n      " + fieldDefinition + "\n      }");
    };
    TypescriptGenerator.prototype.renderDescription = function (description) {
        return "" + (description
            ? "/*\n" + description.split('\n').map(function (l) { return " * " + l + "\n"; }) + "\n */\n"
            : '');
    };
    TypescriptGenerator.prototype.isConnectionType = function (type) {
        if (!(type instanceof graphql_1.GraphQLObjectType)) {
            return false;
        }
        var fields = type.getFields();
        if (type.name.endsWith('Connection') && type.name !== 'Connection') {
            return !Object.keys(fields).some(function (f) { return ['pageInfo', 'aggregate', 'edges'].includes(f) === false; });
        }
        if (type.name.endsWith('Edge') && type.name !== 'Edge') {
            return !Object.keys(fields).some(function (f) { return ['cursor', 'node'].includes(f) === false; });
        }
        return false;
    };
    TypescriptGenerator.prototype.renderConnectionType = function (type) {
        var _this = this;
        var fields = type.getFields();
        var fieldDefinition = [];
        var connectionFieldsType = {
            pageInfo: function (fieldType) { return fieldType; },
            edges: function (fieldType) { return fieldType + "[]"; },
        };
        if (type.name.endsWith('Connection')) {
            fieldDefinition = Object.keys(fields)
                .filter(function (f) { return f !== 'aggregate'; })
                .map(function (f) {
                var field = fields[f];
                var deepType = _this.getDeepType(fields[f].type);
                return "  " + _this.renderFieldName(field, false) + ": " + connectionFieldsType[field.name](deepType.name);
            });
        }
        else {
            // else if type.name is typeEdge
            fieldDefinition = Object.keys(fields).map(function (f) {
                var field = fields[f];
                var deepType = _this.getDeepType(fields[f].type);
                return "  " + _this.renderFieldName(field, false) + ": " + deepType.name;
            });
        }
        return this.renderInterfaceWrapper("" + type.name, type.description, [], fieldDefinition.join(this.lineBreakDelimiter + "\n"), false, false);
    };
    TypescriptGenerator.prototype.isSubscriptionType = function (type) {
        if (!(type instanceof graphql_1.GraphQLObjectType)) {
            return false;
        }
        var fields = type.getFields();
        if (type.name.endsWith('SubscriptionPayload') &&
            type.name !== 'SubscriptionPayload') {
            return !Object.keys(fields).some(function (f) {
                return ['mutation', 'node', 'updatedFields', 'previousValues'].includes(f) === false;
            });
        }
        return false;
    };
    TypescriptGenerator.prototype.renderSubscriptionType = function (type) {
        var _this = this;
        var fields = type.getFields();
        var fieldsType = {
            mutation: function (fieldType) { return fieldType; },
            node: function (fieldType) { return fieldType; },
            previousValues: function (fieldType) { return fieldType; },
            updatedFields: function (fieldType) { return fieldType + "[]"; },
        };
        var fieldDefinition = Object.keys(fields)
            .map(function (f) {
            var field = fields[f];
            var deepType = _this.getDeepType(fields[f].type);
            return "  " + _this.renderFieldName(field, false) + ": " + fieldsType[field.name](deepType.name);
        })
            .join(this.lineBreakDelimiter + "\n");
        return this.renderInterfaceWrapper("" + type.name, type.description, [], fieldDefinition, false, false);
    };
    return TypescriptGenerator;
}(Generator_1.Generator));
exports.TypescriptGenerator = TypescriptGenerator;
var templateObject_1;
//# sourceMappingURL=typescript-client.js.map