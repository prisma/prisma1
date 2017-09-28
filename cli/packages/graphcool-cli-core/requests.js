
nock('https://api.graph.cool:443', {"encodedQueryParams":true})
  .post('/system', {"query":"{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }"})
  .reply(200, {"data":{"viewer":{"user":{"id":"cj84c8sxb14ze0180pxb30q9r","email":"cli-tests@graph.cool"}}}}, [ 'Content-Type',
  'application/json',
  'Content-Length',
  '94',
  'Connection',
  'close',
  'Date',
  'Thu, 28 Sep 2017 15:05:14 GMT',
  'Request-Id',
  'eu-west-1:system:cj84lcw2y04dm0184n5sfgzeo',
  'Server',
  'akka-http/10.0.8',
  'X-Cache',
  'Miss from cloudfront',
  'Via',
  '1.1 e83e50311ef72f016908ba58ad2e6bd8.cloudfront.net (CloudFront)',
  'X-Amz-Cf-Id',
  'tKQ0OF_cDuk0L1XfzB1G5UmW-Ik6ydcjTKTyUwnfzK3YTuAitp53Kg==' ]);


nock('https://api.graph.cool:443', {"encodedQueryParams":true})
  .post('/system', {"query":"      mutation addProject($name: String!, $alias: String, $region: Region, $config: String) {\n        addProject(input: {\n          name: $name,\n          alias: $alias,\n          region: $region,\n          clientMutationId: \"static\"\n          config: $config\n        }) {\n          project {\n            ...RemoteProject\n          }\n        }\n      }\n      \n  fragment RemoteProject on Project {\n    id\n    name\n    schema\n    alias\n    region\n    projectDefinitionWithFileContent\n  }\n\n      ","variables":{"name":"Shellcrest Cub","region":"EU_WEST_1","config":"{\"modules\":[{\"name\":\"\",\"content\":\"# Welcome to Graphcool!\\n#\\n# This file is the main config file for your Graphcool Project.\\n# It's very minimal at this point and uses default values.\\n# We've included a hello world function here.\\n# Just uncomment it and run `graphcool deploy`\\n#\\n# Check out some examples:\\n#    github.com/graphcool/examples\\n#\\n# Happy Coding!\\n\\n\\n# GraphQL types\\ntypes: ./types.graphql\\n\\n\\n# uncomment this:\\n\\n# functions:\\n#   hello:\\n#     handler:\\n#       code:\\n#         src: ./code/hello.js\\n#     type: resolver\\n#     schema: ./code/hello.graphql\\n\\n \\n# Graphcool modules\\nmodules: {}\\n\\n\\n# Model/Relation permissions\\npermissions:\\n- operation: \\\"*\\\"\\n\\n  \\n# Permanent Auth Token / Root Tokens\\nrootTokens: []\\n\\n\",\"files\":{\"./types.graphql\":\"# This file contains the GraphQL Types\\n\\n# All types need to have the three fields id, updatedAt and createdAt like this:\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\\n\\n\\n# Graphcool has one special type, the File type:\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n\",\"./code/hello.js\":\"module.exports = event => {\\n  return {\\n    data: {\\n      message: `Hello ${event.data.name || 'World'}`\\n    }\\n  }\\n}\",\"./code/hello.graphql\":\"type HelloPayload {\\n  message: String!\\n}\\n\\nextend type Query {\\n  hello(name: String): HelloPayload\\n}\\n\"}}]}"}})
  .reply(200, {"data":{"addProject":{"project":{"name":"Shellcrest Cub","projectDefinitionWithFileContent":"{\n  \"modules\": [{\n    \"name\": \"\",\n    \"content\": \"types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n\",\n    \"files\": {\n      \"./types.graphql\": \"type User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\"\n    }\n  }]\n}","alias":null,"id":"cj84lcwd004gc0150aci9s8ow","schema":"type User implements Node {\n  id: ID! @isUnique\n  createdAt: DateTime!\n  updatedAt: DateTime!\n}","region":"EU_WEST_1"}}}}, [ 'Content-Type',
  'application/json',
  'Content-Length',
  '588',
  'Connection',
  'close',
  'Date',
  'Thu, 28 Sep 2017 15:05:15 GMT',
  'Request-Id',
  'eu-west-1:system:cj84lcwch04gb01507xcw6cb3',
  'Server',
  'akka-http/10.0.8',
  'X-Cache',
  'Miss from cloudfront',
  'Via',
  '1.1 c53e057b6a5ef86fdf09ac1341f7b843.cloudfront.net (CloudFront)',
  'X-Amz-Cf-Id',
  'oG73NUlLFwn37vQz8MID0grFEuk5wE5vlFVKTKHa9F77LQ3mKLpkyg==' ]);
