import * as nock from 'nock'

export default () => {
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
              id: 'cilpevfu1000b0pl8wmdmz5dd',
              email: 'tim.suchanek@gmail.com',
            },
          },
        },
      },
      [
        'Content-Type',
        'application/json',
        'Content-Length',
        '96',
        'Connection',
        'close',
        'Date',
        'Tue, 03 Oct 2017 09:17:47 GMT',
        'Request-Id',
        'eu-west-1:system:cj8be5bam01ti0153b16ivp5u',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 279cefb37a6695aa72c905285954d61d.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'nkvRuEAh2vue7xw5u7Jl9a6r0DBEPMuIu3wszkzrKOWl2JOq2ssIfg==',
      ],
    )

  nock('http://dynamodb.eu-west-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bcp0000d6yvj0p6mbxd' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'MNKVHVIQC8IM3GS76MDDFKDUJ7VV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:47 GMT',
      ],
    )

  nock('http://dynamodb.eu-west-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bcp0000d6yvj0p6mbxd' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'B39P7HGNSQNJCTHI3LKNOKIOJBVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:46 GMT',
      ],
    )

  nock('http://dynamodb.us-west-2.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bd90002d6yv57jgjatk' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        '90SA8F5245Q2OT92JAJT640B6NVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:46 GMT',
      ],
    )

  nock('http://dynamodb.us-west-2.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bd90002d6yv57jgjatk' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        '681FFV061AJFG5PGF4H26CIMLRVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:46 GMT',
      ],
    )

  nock('http://dynamodb.ap-northeast-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bd50001d6yv1lv0b1f1' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'PODOGO1EDFIU9UF0NNH7R8MSL3VV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:47 GMT',
      ],
    )

  nock('http://dynamodb.ap-northeast-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8be5bd50001d6yv1lv0b1f1' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'PKFL474CANR8M1MHU080VFUNQ7VV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 09:17:48 GMT',
      ],
    )

  nock('https://api.graph.cool:443', { encodedQueryParams: true })
    .post('/system', {
      query:
        '      mutation addProject($name: String!, $alias: String, $region: Region, $config: String) {\n        addProject(input: {\n          name: $name,\n          alias: $alias,\n          region: $region,\n          clientMutationId: "static"\n          config: $config\n        }) {\n          project {\n            ...RemoteProject\n          }\n        }\n      }\n      \n  fragment RemoteProject on Project {\n    id\n    name\n    schema\n    alias\n    region\n    projectDefinitionWithFileContent\n  }\n\n      ',
      variables: {
        name: 'Tardonkey Bat',
        region: 'EU_WEST_1',
        config:
          '{"modules":[{"name":"","content":"types: ./types.graphql\\n","files":{"./types.graphql":""}}]}',
      },
    })
    .reply(
      200,
      {
        data: {
          addProject: {
            project: {
              name: 'Tardonkey Bat',
              projectDefinitionWithFileContent:
                '{\n  "modules": [{\n    "name": "",\n    "content": "types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n",\n    "files": {\n      "./types.graphql": ""\n    }\n  }]\n}',
              alias: null,
              id: 'cj8be5ct201is0140cq7qp23b',
              schema: '',
              region: 'EU_WEST_1',
            },
          },
        },
      },
      [
        'Content-Type',
        'application/json',
        'Content-Length',
        '385',
        'Connection',
        'close',
        'Date',
        'Tue, 03 Oct 2017 09:17:49 GMT',
        'Request-Id',
        'eu-west-1:system:cj8be5csi01ir0140u28gnmyn',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 def7631bb5a87451a530ea65c6733627.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'dZFwe36HAsJxwaZ_1Q1RgvpJsqEVxUZcT6wxutmnhbZCeTwQ2NDN3g==',
      ],
    )

  const pushPayload = {
    query:
      '      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ',
    variables: {
      projectId: 'cj8be5ct201is0140cq7qp23b',
      isDryRun: false,
      config:
        '{"modules":[{"name":"","content":"types: ./types.graphql\\npermissions:\\n  - operation: \'*\'\\nrootTokens: []\\nmodules: {}\\n","files":{"./types.graphql":"# The following types define the data model of the example app\\n# based on which the GraphQL API is generated\\n\\n# All types need to have the three fields id, updatedAt and createdAt like this:\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\\n\\n\\n# Prisma has one special type, the File type:\\n# Uncommenting this type will automatically enable the File API\\n# Read more here:\\n# https://www.graph.cool/docs/reference/api/file-management-eer4wiang0\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n"}}]}',
    },
  }
  nock('https://api.graph.cool:443', { encodedQueryParams: true })
    .post('/system', data => data.query === pushPayload.query)
    .reply(
      200,
      [
        '1f8b08000000000000039493c16edb300c865f85d539c8d26ec3069fd63528b0c3861d5cf410078862d1355b597224194511f8dd473aaed3a628d0f922fb177ff23345ed95d149ab6cafda2ed6b23674177422ef7e638cfa0ea3ca567be574832a533711839a2983b10cd44a148b97e0f011d2538bf048a9865423483c6c247c0314a10ca8139a397b63b75d1eed87ece2e544d784d670882ec7cc57838d95b1fc98e632bdc35049825388c9b401edcc8173b36429a706cf5ee3f5b38fc374adf97f98c9f41198f5ec192697e52d8be08e34b7644da98381bf181a8a51e24eb9f25a4e688c6ba738a87c006ded401305411bf3de611d99da9785de90711c86e0c3e86983bfc732c980919193bcffbec5af65ba589c535c9c7f5994bb6fbbf6e2f3f6d8df9c29bd7bc027f8a9a5c7da92e66caeb376cab7c48a1c49e95beef33559bcf22ea1e3426a5f38804235de741663a132580d928852429442156af62c9607eb411f7a91c1fcd3f032e73bd1d63b5b14aeeadcf0afbcb9eff9fbd80656566b5682f729f70f38092f6a5434b28c28a29dd698ea83dc1fa0a6b5d83058843fde203bc54a26835fcb33f841f1c6d1aec3419d663d8369a8868d69ee5e6ff4dc8081a397a55fb3a27a7efe010000ffff0300c26f6baf17040000',
      ],
      [
        'Content-Type',
        'application/json',
        'Transfer-Encoding',
        'chunked',
        'Connection',
        'close',
        'Vary',
        'Accept-Encoding',
        'Date',
        'Tue, 03 Oct 2017 09:17:50 GMT',
        'Request-Id',
        'eu-west-1:system:cj8be5dae01gi0161cbt4qrsh',
        'Server',
        'akka-http/10.0.8',
        'Content-Encoding',
        'gzip',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 d9823e657bbd8582e0bc0663df5ca353.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'FcGRKm8axMR-BFjk9CCZZAXo3h6Xcup4USoL6cDo2PSDys1gFMl2hw==',
      ],
    )
}
