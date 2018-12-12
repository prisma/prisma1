"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var graphql_1 = require("graphql");
var flow_client_1 = require("./flow-client");
var ava_1 = require("ava");
var typeDefs = fs.readFileSync(path.join(__dirname, '../../src/codegen/fixtures/schema.graphql'), 'utf-8');
ava_1.test('flow generator', function (t) {
    try {
        var schema = graphql_1.buildSchema(typeDefs);
        var generator = new flow_client_1.FlowGenerator({
            schema: schema,
            internalTypes: [],
        });
        var result = generator.render();
        t.snapshot(result);
    }
    catch (e) {
        console.log(e.codeFrame);
    }
});
//# sourceMappingURL=flow-client.test.js.map