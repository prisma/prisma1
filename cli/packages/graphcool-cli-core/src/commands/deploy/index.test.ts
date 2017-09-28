import * as nock from 'nock'
import Deploy from './'
import { changedDefaultDefinition } from '../../examples'
import { mockEnv } from 'graphcool-cli-engine'

afterAll(() => {
  nock.cleanAll()
})

describe('deploy', () => {
  test('without change', async () => {
    const scope = nock('https://api.graph.cool:443', {
      encodedQueryParams: true,
    })
      .post('/system', {
        query:
          '{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }',
      })
      .reply(
        200,
        {
          data: {
            viewer: {
              user: {
                id: 'cj84c8sxb14ze0180pxb30q9r',
                email: 'cli-tests@graph.cool',
              },
            },
          },
        },
        [
          'Content-Type',
          'application/json',
          'Content-Length',
          '94',
          'Connection',
          'close',
          'Date',
          'Thu, 28 Sep 2017 11:41:06 GMT',
          'Request-Id',
          'eu-west-1:system:cj84e2d9s19i90120hvkd39lw',
          'Server',
          'akka-http/10.0.8',
          'X-Cache',
          'Miss from cloudfront',
          'Via',
          '1.1 cdefd7498c238ce7b5d291eb3d7f28f9.cloudfront.net (CloudFront)',
          'X-Amz-Cf-Id',
          'ojrrq0aFbvEOMzgJ63HFD3L6HQ22Ir8HzHYh4LVHUi7VkTKAHWRTxg==',
        ],
      )

    scope
      .post('/system', {
        query:
          '      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ',
        variables: {
          projectId: 'cj84dopd3197p01200l5sb9fs',
          isDryRun: false,
          config:
            '{"modules":[{"name":"","content":"# Welcome to Graphcool!\\n#\\n# This file is the main config file for your Graphcool Project.\\n# It\'s very minimal at this point and uses default values.\\n# We\'ve included a hello world function here.\\n# Just uncomment it and run `graphcool deploy`\\n#\\n# Check out some examples:\\n#    github.com/graphcool/examples\\n#\\n# Happy Coding!\\n\\n\\n# GraphQL types\\ntypes: ./types.graphql\\n\\n\\n# uncomment this:\\n\\n# functions:\\n#   hello:\\n#     handler:\\n#       code:\\n#         src: ./code/hello.js\\n#     type: resolver\\n#     schema: ./code/hello.graphql\\n\\n \\n# Graphcool modules\\nmodules: {}\\n\\n\\n# Model/Relation permissions\\npermissions:\\n- operation: \\"*\\"\\n\\n  \\n# Permanent Auth Token / Root Tokens\\nrootTokens: []\\n\\n","files":{"./types.graphql":"# This file contains the GraphQL Types\\n\\n# All types need to have the three fields id, updatedAt and createdAt like this:\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\\n\\n\\n# Graphcool has one special type, the File type:\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n","./code/hello.js":"module.exports = event => {\\n  return {\\n    data: {\\n      message: `Hello ${event.data.name || \'World\'}`\\n    }\\n  }\\n}","./code/hello.graphql":"type HelloPayload {\\n  message: String!\\n}\\n\\nextend type Query {\\n  hello(name: String): HelloPayload\\n}\\n"}}]}',
        },
      })
      .reply(
        200,
        {
          data: {
            push: {
              migrationMessages: [],
              errors: [],
              project: {
                id: 'cj84dopd3197p01200l5sb9fs',
                name: 'Shellcrest Cub',
                alias: null,
                projectDefinitionWithFileContent:
                  '{\n  "modules": [{\n    "name": "",\n    "content": "types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n",\n    "files": {\n      "./types.graphql": "type User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}"\n    }\n  }]\n}',
              },
            },
          },
        },
        [
          'Content-Type',
          'application/json',
          'Content-Length',
          '485',
          'Connection',
          'close',
          'Date',
          'Thu, 28 Sep 2017 11:41:06 GMT',
          'Request-Id',
          'eu-west-1:system:cj84e2de5029j0154vmf95046',
          'Server',
          'akka-http/10.0.8',
          'X-Cache',
          'Miss from cloudfront',
          'Via',
          '1.1 a6cbd015f961a0db7be2b1cd285e26d9.cloudfront.net (CloudFront)',
          'X-Amz-Cf-Id',
          '2L-mJfqGK0CX5ct2XRJ-J_4K1pYMb8Z3CZlWfhBrN6BofdeG5P5e7Q==',
        ],
      )

    const result = await Deploy.mock({mockEnv})
    expect(result.out.stdout.output).toMatchSnapshot()
    scope.done()
  })

  test('with change', async () => {
    nock('https://api.graph.cool:443', { encodedQueryParams: true })
      .post('/system', {
        query:
          '{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }',
      })
      .reply(
        200,
        {
          data: {
            viewer: {
              user: {
                id: 'cj84c8sxb14ze0180pxb30q9r',
                email: 'cli-tests@graph.cool',
              },
            },
          },
        },
        [
          'Content-Type',
          'application/json',
          'Content-Length',
          '94',
          'Connection',
          'close',
          'Date',
          'Thu, 28 Sep 2017 11:47:33 GMT',
          'Request-Id',
          'eu-west-1:system:cj84eao900rw00116ett3sluc',
          'Server',
          'akka-http/10.0.8',
          'X-Cache',
          'Miss from cloudfront',
          'Via',
          '1.1 3f146fa6bc6607097fc0d9bc7e6d4947.cloudfront.net (CloudFront)',
          'X-Amz-Cf-Id',
          '_0Q6Je6TliSoF3mVWkdZNz-k5QvBPPZR0bga4kn0yYYTVGXH9SDXVg==',
        ],
      )

    nock('https://api.graph.cool:443', { encodedQueryParams: true })
      .post('/system', {
        query:
          '      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ',
        variables: {
          projectId: 'cj84dopd3197p01200l5sb9fs',
          isDryRun: false,
          config:
            '{"modules":[{"name":"","content":"# This is the changed default definition, used in tests\\n#\\n# This file is the main config file for your Graphcool Project.\\n# It\'s very minimal at this point and uses default values.\\n# We\'ve included a hello world function here.\\n# Just uncomment it and run `graphcool deploy`\\n#\\n# Check out some examples:\\n#    github.com/graphcool/examples\\n#\\n# Happy Coding!\\n\\n\\n# GraphQL types\\ntypes: ./types.graphql\\n\\n\\n# uncomment this:\\n\\n# functions:\\n#   hello:\\n#     handler:\\n#       code:\\n#         src: ./code/hello.js\\n#     type: resolver\\n#     schema: ./code/hello.graphql\\n\\n \\n# Graphcool modules\\nmodules: {}\\n\\n\\n# Model/Relation permissions\\npermissions:\\n- operation: \\"*\\"\\n\\n  \\n# Permanent Auth Token / Root Tokens\\nrootTokens: []\\n\\n","files":{"./types.graphql":"# This file contains the GraphQL Types\\n\\n# All types need to have the three fields id, updatedAt and createdAt like this:\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\\n\\n\\n# Graphcool has one special type, the File type:\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n\\ntype Post implements Node {\\n  id: ID! @isUnique\\n  title: String\\n}\\n","./code/hello.js":"module.exports = event => {\\n  return {\\n    data: {\\n      message: `Hello ${event.data.name || \'World\'}`\\n    }\\n  }\\n}","./code/hello.graphql":"type HelloPayload {\\n  message: String!\\n}\\n\\nextend type Query {\\n  hello(name: String): HelloPayload\\n}\\n"}}]}',
        },
      })
      .reply(
        200,
        {
          data: {
            push: {
              migrationMessages: [
                {
                  name: 'Post',
                  description: 'A new type with the name `Post` is created.',
                  subDescriptions: [
                    {
                      type: 'Field',
                      action: 'Create',
                      name: 'title',
                      description:
                        'A new field with the name `title` and type `String` is created.',
                    },
                  ],
                  type: 'Type',
                  action: 'Create',
                },
              ],
              errors: [],
              project: {
                id: 'cj84dopd3197p01200l5sb9fs',
                name: 'Shellcrest Cub',
                alias: null,
                projectDefinitionWithFileContent:
                  '{\n  "modules": [{\n    "name": "",\n    "content": "types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n",\n    "files": {\n      "./types.graphql": "type Post implements Node {\\n  id: ID! @isUnique\\n  title: String\\n}\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}"\n    }\n  }]\n}',
              },
            },
          },
        },
        [
          'Content-Type',
          'application/json',
          'Content-Length',
          '819',
          'Connection',
          'close',
          'Date',
          'Thu, 28 Sep 2017 11:47:34 GMT',
          'Request-Id',
          'eu-west-1:system:cj84eaodb19n701208kfypei3',
          'Server',
          'akka-http/10.0.8',
          'X-Cache',
          'Miss from cloudfront',
          'Via',
          '1.1 6eea7c9b83576b73ff12f8e9ff2ad318.cloudfront.net (CloudFront)',
          'X-Amz-Cf-Id',
          'M-FTdUKUGTug7DBoOpGp-Qr59RvgmPkl8n_FP6NxMRs0VbWhfPE5qQ==',
        ],
      )

    const result = await Deploy.mock({
      mockDefinition: changedDefaultDefinition,
      mockEnv,
    })
    expect(result.out.stdout.output).toMatchSnapshot()
  })
})
