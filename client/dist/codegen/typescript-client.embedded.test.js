"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var fs = require("fs");
var path = require("path");
var graphql_1 = require("graphql");
var typescript_client_1 = require("./typescript-client");
var ava_1 = require("ava");
var prisma_generate_schema_1 = require("prisma-generate-schema");
var prisma_datamodel_1 = require("prisma-datamodel");
var typeDefs = fs.readFileSync(path.join(__dirname, '../../src/codegen/fixtures/embedded.graphql'), 'utf-8');
var datamodel = "\ntype User {\n  id: ID! @unique\n  name: String!\n  address: Address\n}\n\ntype Address @embedded {\n  location: String\n}\n";
ava_1.test('typescript generator - embedded', function (t) {
    var schema = graphql_1.buildSchema(typeDefs);
    var generator = new typescript_client_1.TypescriptGenerator({
        schema: schema,
        internalTypes: prisma_generate_schema_1.parseInternalTypes(datamodel, prisma_datamodel_1.DatabaseType.postgres).types,
    });
    var result = generator.render();
    t.snapshot(result);
});
//# sourceMappingURL=typescript-client.embedded.test.js.map