import * as nock from 'nock'

export default () => {
  nock('http://localhost:60000', { encodedQueryParams: true })
    .post('/system', {
      query:
        '{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }',
    })
    .reply(
      200,
      {
        data: {
          viewer: {
            user: { id: 'cj8a01dsu000001235iaui1hb', email: 'test@test.org' },
          },
        },
      },
      [
        'Server',
        'nginx/1.13.3',
        'Date',
        'Tue, 03 Oct 2017 11:52:30 GMT',
        'Content-Type',
        'application/json',
        'Content-Length',
        '87',
        'Connection',
        'close',
        'Request-Id',
        'eu-west-1:system:cj8bjoa94001f0183pcbd7lgh',
      ],
    )

  nock('http://dynamodb.eu-west-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoaij0000diyvrcp3at31' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        '1VDNI2RUJUS7SA01GS3M03B1QNVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:29 GMT',
      ],
    )

  nock('http://dynamodb.eu-west-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoaij0000diyvrcp3at31' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        '7E5HCJNJ14UBSR2TK805NES54JVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:29 GMT',
      ],
    )

  nock('http://dynamodb.us-west-2.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoaio0002diyvzkjko3yg' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'J3R8QTGQF8KEU687FP1CSIJNNBVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:29 GMT',
      ],
    )

  nock('http://dynamodb.us-west-2.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoaio0002diyvzkjko3yg' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'GP7FA4ESKFJ43791ILE0RVJQTRVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:30 GMT',
      ],
    )

  nock('http://dynamodb.ap-northeast-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoain0001diyv5beeq9ux' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'QF4T0HCE6MM53MBO6CVHSB9S0RVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:29 GMT',
      ],
    )

  nock('http://dynamodb.ap-northeast-1.amazonaws.com:80', {
    encodedQueryParams: true,
  })
    .get('/ping')
    .query({ x: 'cj8bjoain0001diyv5beeq9ux' })
    .reply(
      404,
      '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">\n<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>',
      [
        'x-amzn-RequestId',
        'JS8KEQQP50ATSEBVV28RK2QNHBVV4KQNSO5AEMVJF66Q9ASUAAJG',
        'x-amz-crc32',
        '2548615100',
        'Content-Length',
        '272',
        'Date',
        'Tue, 03 Oct 2017 11:52:30 GMT',
      ],
    )

  const addProjectPayload = {
    query:
      '      mutation addProject($name: String!, $alias: String, $region: Region, $config: String) {\n        addProject(input: {\n          name: $name,\n          alias: $alias,\n          region: $region,\n          clientMutationId: "static"\n          config: $config\n        }) {\n          project {\n            ...RemoteProject\n          }\n        }\n      }\n      \n  fragment RemoteProject on Project {\n    id\n    name\n    schema\n    alias\n    region\n    projectDefinitionWithFileContent\n  }\n\n      ',
    variables: {
      name: 'Moorrib Crusher',
      region: 'EU_WEST_1',
      config:
        '{"modules":[{"name":"","content":"types: ./types.graphql\\n","files":{"./types.graphql":""}}]}',
    },
  }
  nock('http://localhost:60000', { encodedQueryParams: true })
    .post('/system', data => data.query === addProjectPayload.query)
    .reply(
      200,
      {
        data: {
          addProject: {
            project: {
              name: 'Moorrib Crusher',
              projectDefinitionWithFileContent:
                '{\n  "modules": [{\n    "name": "",\n    "content": "types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n",\n    "files": {\n      "./types.graphql": ""\n    }\n  }]\n}',
              alias: null,
              id: 'cj8bjoc2w001h01830u1caec2',
              schema: '',
              region: 'EU_WEST_1',
            },
          },
        },
      },
      [
        'Server',
        'nginx/1.13.3',
        'Date',
        'Tue, 03 Oct 2017 11:52:35 GMT',
        'Content-Type',
        'application/json',
        'Content-Length',
        '387',
        'Connection',
        'close',
        'Request-Id',
        'eu-west-1:system:cj8bjobva001g0183ki39i4l7',
      ],
    )

  const pushPayload = {
    query:
      '      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ',
    variables: {
      projectId: 'cj8bjoc2w001h01830u1caec2',
      isDryRun: false,
      config:
        '{"modules":[{"name":"","content":"types: ./types.graphql\\npermissions:\\n  - operation: \'*\'\\nrootTokens: []\\nmodules: {}\\n","files":{"./types.graphql":"# The following types define the data model of the example app\\n# based on which the GraphQL API is generated\\n\\n# All types need to have the three fields id, updatedAt and createdAt like this:\\n\\ntype User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}\\n\\n\\n# Prisma has one special type, the File type:\\n# Uncommenting this type will automatically enable the File API\\n# Read more here:\\n# https://www.graph.cool/docs/reference/api/file-management-eer4wiang0\\n\\n# type File implements Node {\\n#   contentType: String!\\n#   createdAt: DateTime!\\n#   id: ID! @isUnique\\n#   name: String!\\n#   secret: String! @isUnique\\n#   size: Int!\\n#   updatedAt: DateTime!\\n#   url: String! @isUnique\\n# }\\n"}}]}',
    },
  }
  nock('http://localhost:60000', { encodedQueryParams: true })
    .post('/system', data => pushPayload.query === data.query)
    .reply(
      200,
      {
        data: {
          push: {
            migrationMessages: [
              {
                name: 'User',
                description: 'A new type with the name `User` is created.',
                subDescriptions: [
                  {
                    type: 'Field',
                    action: 'Create',
                    name: 'createdAt',
                    description:
                      'A new field with the name `createdAt` and type `DateTime!` is created.',
                  },
                  {
                    type: 'Field',
                    action: 'Create',
                    name: 'updatedAt',
                    description:
                      'A new field with the name `updatedAt` and type `DateTime!` is created.',
                  },
                ],
                type: 'Type',
                action: 'Create',
              },
              {
                name: 'Wildcard Permission',
                description:
                  'The wildcard permission for all operations is added.',
                subDescriptions: [],
                type: 'permission',
                action: 'Create',
              },
            ],
            errors: [],
            project: {
              id: 'cj8bjoc2w001h01830u1caec2',
              name: 'Moorrib Crusher',
              alias: null,
              projectDefinitionWithFileContent:
                '{\n  "modules": [{\n    "name": "",\n    "content": "types: ./types.graphql\\nfunctions: {}\\npermissions: []\\nrootTokens: []\\n",\n    "files": {\n      "./types.graphql": "type User implements Node {\\n  id: ID! @isUnique\\n  createdAt: DateTime!\\n  updatedAt: DateTime!\\n}"\n    }\n  }]\n}',
            },
          },
        },
      },
      [
        'Server',
        'nginx/1.13.3',
        'Date',
        'Tue, 03 Oct 2017 11:52:37 GMT',
        'Content-Type',
        'application/json',
        'Content-Length',
        '1049',
        'Connection',
        'close',
        'Request-Id',
        'eu-west-1:system:cj8bjoedj00210183hjs4eelg',
      ],
    )
}
