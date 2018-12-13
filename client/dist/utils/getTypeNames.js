"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
// TODO: Remove the same function from typescript-generator and use this one instead. 
function getTypeNames(ast) {
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
}
exports.getTypeNames = getTypeNames;
//# sourceMappingURL=getTypeNames.js.map