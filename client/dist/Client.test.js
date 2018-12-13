"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
var ava_1 = require("ava");
var Client_1 = require("./Client");
var graphql_1 = require("graphql");
ava_1.test('related type', function (t) {
    var typeDefs = "\n    type Query {\n      user: User\n    }\n\n    type User {\n      id: ID!\n      posts: [Post!]!\n    }\n\n    type Post {\n      content: String!\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'User',
        },
        {
            embedded: false,
            name: 'Post',
        },
    ];
    var endpoint = 'http://localhost;4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.user();
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
ava_1.test('deep related type', function (t) {
    var typeDefs = "\n    type Query {\n      user: User\n    }\n\n    type User {\n      id: ID!\n      posts: [Post!]!\n    }\n\n    type Post {\n      content: String!\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'User',
        },
        {
            embedded: false,
            name: 'Post',
        },
    ];
    var endpoint = 'http://localhost;4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.user().posts();
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
ava_1.test('embedded type', function (t) {
    var typeDefs = "\n    type Query {\n      user: User\n    }\n\n    type User {\n      id: ID!\n      posts: [Post!]!\n    }\n\n    type Post {\n      content: String!\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'User',
        },
        {
            embedded: true,
            name: 'Post',
        },
    ];
    var endpoint = 'http://localhost;4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.user();
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
ava_1.test('nested mbedded type', function (t) {
    var typeDefs = "\n    type Query {\n      user: User\n    }\n\n    type User {\n      id: ID!\n      posts: [Post!]!\n    }\n\n    type Post {\n      content: String!\n      meta: PostMeta\n    }\n\n    type PostMeta {\n      meta: String!\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'User',
        },
        {
            embedded: true,
            name: 'Post',
        },
        {
            embedded: true,
            name: 'PostMeta',
        },
    ];
    var endpoint = 'http://localhost;4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.user();
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
ava_1.test('top level args', function (t) {
    var typeDefs = "\n    type Query {\n      post(where: PostInput!): Post\n    }\n\n    input PostInput {\n      id: ID!\n    }\n\n    type Post {\n      id: ID!\n      title: String!\n      content: String!\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'Post',
        },
    ];
    var endpoint = 'http://localhost:4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.post({ id: 'test' });
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
ava_1.test('nested args', function (t) {
    var typeDefs = "\n    type Query {\n      user: User\n      post(where: PostInput!): Post\n    }\n\n    input PostInput {\n      author: AuthorInput!\n    }\n\n    input AuthorInput {\n      firstName: String!\n      lastName: String!\n    }\n\n    type User {\n      id: ID!\n      post: Post\n    }\n\n    type Post {\n      id: ID!\n      title: String!\n      content: String!\n      user: User\n    }\n  ";
    var models = [
        {
            embedded: false,
            name: 'User',
        },
        {
            embedded: false,
            name: 'Post',
        },
    ];
    var endpoint = 'http://localhost;4466';
    var client = new Client_1.Client({
        typeDefs: typeDefs,
        endpoint: endpoint,
        models: models,
    });
    client.post({
        author: {
            firstName: 'Lydia',
            lastName: 'Hallie',
        },
    });
    var document = client.getDocumentForInstructions(Object.keys(client._currentInstructions)[0]);
    t.snapshot(graphql_1.print(document));
});
//# sourceMappingURL=Client.test.js.map