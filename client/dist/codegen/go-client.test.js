"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var graphql_1 = require("graphql");
var go_client_1 = require("./go-client");
var ava_1 = require("ava");
var typeDefs = fs.readFileSync(path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'), 'utf-8');
ava_1.test('go generator', function (t) {
    var schema = graphql_1.buildSchema(typeDefs);
    var generator = new go_client_1.GoGenerator({
        schema: schema,
        internalTypes: [],
    });
    var result = generator.render({
        endpoint: 'http://localhost:4466/test/test',
    });
    t.snapshot(result);
});
//# sourceMappingURL=go-client.test.js.map