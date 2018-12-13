"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var graphql_1 = require("graphql");
var javascript_client_1 = require("./javascript-client");
var ava_1 = require("ava");
var typeDefs = fs.readFileSync(path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'), 'utf-8');
ava_1.test('typescript definition generator', function (t) {
    var schema = graphql_1.buildSchema(typeDefs);
    var generator = new javascript_client_1.JavascriptGenerator({
        schema: schema,
        internalTypes: [],
    });
    var javascript = generator.renderJavascript();
    t.snapshot(javascript);
});
//# sourceMappingURL=javascript-client.test.js.map