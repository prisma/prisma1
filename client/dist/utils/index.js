"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var graphql_1 = require("graphql");
function isScalar(t) {
    if (t instanceof graphql_1.GraphQLScalarType || t instanceof graphql_1.GraphQLEnumType) {
        return true;
    }
    if (t instanceof graphql_1.GraphQLObjectType ||
        t instanceof graphql_1.GraphQLInterfaceType ||
        t instanceof graphql_1.GraphQLUnionType ||
        t instanceof graphql_1.GraphQLList) {
        return false;
    }
    var nnt = t;
    if (nnt instanceof graphql_1.GraphQLNonNull) {
        if (nnt.ofType instanceof graphql_1.GraphQLScalarType ||
            nnt.ofType instanceof graphql_1.GraphQLEnumType) {
            return true;
        }
    }
    return false;
}
exports.isScalar = isScalar;
function getTypeForRootFieldName(rootFieldName, operation, schema) {
    if (operation === 'mutation' && !schema.getMutationType()) {
        throw new Error("Schema doesn't have mutation type");
    }
    if (operation === 'subscription' && !schema.getSubscriptionType()) {
        throw new Error("Schema doesn't have subscription type");
    }
    var rootType = {
        query: function () { return schema.getQueryType(); },
        mutation: function () { return schema.getMutationType(); },
        subscription: function () { return schema.getSubscriptionType(); },
    }[operation]() || undefined;
    var rootField = rootType.getFields()[rootFieldName];
    if (!rootField) {
        throw new Error("No such root field found: " + rootFieldName);
    }
    return rootField.type;
}
exports.getTypeForRootFieldName = getTypeForRootFieldName;
function printDocumentFromInfo(info) {
    var fragments = Object.keys(info.fragments).map(function (fragment) { return info.fragments[fragment]; });
    var doc = {
        kind: 'Document',
        definitions: [
            {
                kind: 'OperationDefinition',
                operation: 'query',
                selectionSet: info.fieldNodes[0].selectionSet,
            }
        ].concat(fragments),
    };
    return graphql_1.print(doc);
}
exports.printDocumentFromInfo = printDocumentFromInfo;
function lowerCaseFirst(str) {
    return str[0].toLowerCase() + str.slice(1);
}
function getExistsTypes(queryType) {
    var types = getTypesAndWhere(queryType);
    return types
        .map(function (_a) {
        var type = _a.type, where = _a.where;
        return "  " + lowerCaseFirst(type) + ": (where?: " + where + ") => Promise<boolean>";
    })
        .join('\n');
}
exports.getExistsTypes = getExistsTypes;
function getExistsFlowTypes(queryType) {
    var types = getTypesAndWhere(queryType);
    return types
        .map(function (_a) {
        var type = _a.type, where = _a.where;
        return lowerCaseFirst(type) + "(where?: " + where + "): Promise<boolean>;";
    })
        .join('\n');
}
exports.getExistsFlowTypes = getExistsFlowTypes;
function getTypesAndWhere(queryType) {
    var fields = queryType.getFields();
    var listFields = Object.keys(fields).reduce(function (acc, field) {
        var deepType = getDeepListType(fields[field]);
        if (deepType) {
            acc.push({ field: fields[field], deepType: deepType });
        }
        return acc;
    }, []);
    return listFields.map(function (_a) {
        var field = _a.field, deepType = _a.deepType;
        return ({
            type: deepType.name,
            pluralFieldName: field.name,
            where: getWhere(field),
        });
    });
}
exports.getTypesAndWhere = getTypesAndWhere;
function getWhere(field) {
    return field.args.find(function (a) { return a.name === 'where'; })
        .type.name;
}
exports.getWhere = getWhere;
function getDeepListType(field) {
    var type = field.type;
    if (graphql_1.isListType(type)) {
        return type.ofType;
    }
    if (graphql_1.isWrappingType(type) && graphql_1.isListType(type.ofType)) {
        return type.ofType.ofType;
    }
    return null;
}
exports.getDeepListType = getDeepListType;
//# sourceMappingURL=index.js.map