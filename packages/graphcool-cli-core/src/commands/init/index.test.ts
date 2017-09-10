import * as nock from 'nock'
import {Config} from 'graphcool-cli-engine'
import { init } from '../test/mock-requests'
import Init from './'

const config = new Config()

beforeEach(() => {

  nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
    .post('/system', {"query":"{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }"})
    .reply(200, {"data":{"viewer":{"user":{"id":"cj7ektagf09sw0112cxeyoqjr","email":"test-ci@graph.cool"}}}}, [ 'Content-Type',
      'application/json',
      'Content-Length',
      '92',
      'Connection',
      'close',
      'Date',
      'Sun, 10 Sep 2017 10:22:19 GMT',
      'Request-Id',
      'eu-west-1:system:cj7elbq2g0a3k0112ll6pf08a',
      'Server',
      'akka-http/10.0.8',
      'X-Cache',
      'Miss from cloudfront',
      'Via',
      '1.1 940b367f846b05ee5d0f25268ff80731.cloudfront.net (CloudFront)',
      'X-Amz-Cf-Id',
      '6mN5TqA5XBtigjfGrizJn5kz4PtUsNu9uh7ZO42KoNzod9bBsLbt1w==' ]);


  nock('http://dynamodb.eu-west-1.amazonaws.com')
    .get(/.*/)
    .query(/.*/)
    .reply(404, "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>", [ 'x-amzn-RequestId',
      'FH92GQJKCOQC71E9QHCEV73S9VVV4KQNSO5AEMVJF66Q9ASUAAJG',
      'x-amz-crc32',
      '2548615100',
      'Content-Length',
      '272',
      'Date',
      'Sun, 10 Sep 2017 10:22:22 GMT' ]);


  nock('http://dynamodb.us-west-2.amazonaws.com', {"encodedQueryParams":true})
    .get(/.*/)
    .query(/.*/)
    .reply(404, "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>", [ 'x-amzn-RequestId',
      'TDT7H8DP0HE2NNPLSL29EKTG4JVV4KQNSO5AEMVJF66Q9ASUAAJG',
      'x-amz-crc32',
      '2548615100',
      'Content-Length',
      '272',
      'Date',
      'Sun, 10 Sep 2017 10:22:22 GMT' ]);


  nock('http://dynamodb.ap-northeast-1.amazonaws.com', {"encodedQueryParams":true})
    .get(/.*/)
    .query(/.*/)
    .reply(404, "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n<head>\n  <title>Page Not Found</title>\n</head>\n<body>Page Not Found</body>\n</html>", [ 'x-amzn-RequestId',
      '1CQFB6E46J1AD2N7KA7VTPTD37VV4KQNSO5AEMVJF66Q9ASUAAJG',
      'x-amz-crc32',
      '2548615100',
      'Content-Length',
      '272',
      'Date',
      'Sun, 10 Sep 2017 10:22:22 GMT' ]);

  nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
    .post('/system', {"query":"      mutation addProject($name: String!, $alias: String, $region: Region) {\n        addProject(input: {\n          name: $name,\n          alias: $alias,\n          region: $region,\n          clientMutationId: \"static\"\n        }) {\n          project {\n            ...RemoteProject\n          }\n        }\n      }\n      \n  fragment RemoteProject on Project {\n    id\n    name\n    schema\n    alias\n    region\n    projectDefinitionWithFileContent\n  }\n\n      ","variables":{"name":"Shellcrest Cub","region":"EU_WEST_1"}})
    .reply(200, ["1f8b080000000000000394534d4f023110fd2ba56744574d8c3d6944132ec6048807d790ba1da0da6d97b61b2364ffbbd32ee0579744b209ed7b339d796fda0d15dc73ca36940bf160cd2b143eecaaafa5e6255046c74b50aab0e03cb9a95f687f173284b9d4d24ba31fa55fde490537467bd0984b37b92624a7a511b5029753469e2214c0706c40729ad3fe0e2cdad416f71f15384606c7713158585e2d572acff5bcd6452888e4a6c17d05b694ce4504b747c420c2430423a1a181052e9204eae11e92545d892e4a8082bfd4d4814d568a44ba52a4d29522b5af648df113f30641f3d33302df4c9bcbadb95b6f03f6dbb4bda151019165a5a044a71db937023033a46edd9f601823636fa55ef45a22362fae3d23435c4c64092d210523a3618f5c4937d572554344c3687f1ee0008ff07bec57bc936b8c1f69df06b77e24aad556a58f089700bfa82ff8d6a5ef7f323afa68f0c6469f9bf0d73c2382af812bc91d65ba56aa4fa5c0cb5fbc5e807af197eb137eb63ec9b2d3ecbc3ccdea778dd1ae584289cfeee048ba26d2a52429e4cf380e4ee3e730ba3ce81e059aa30fcfe1bfcda71b088e5b58e043410f6fa7b3c7dbf16496d1067f9f000000ffff0300b18b0c6ad2040000"], [ 'Content-Type',
      'application/json',
      'Transfer-Encoding',
      'chunked',
      'Connection',
      'close',
      'Vary',
      'Accept-Encoding',
      'Date',
      'Sun, 10 Sep 2017 10:22:25 GMT',
      'Request-Id',
      'eu-west-1:system:cj7elbt9c0a3l0112uxxjj8d3',
      'Server',
      'akka-http/10.0.8',
      'Content-Encoding',
      'gzip',
      'X-Cache',
      'Miss from cloudfront',
      'Via',
      '1.1 0bf7ab276e9275ac14471a0d2b33bfd0.cloudfront.net (CloudFront)',
      'X-Amz-Cf-Id',
      'ZcSyzHnRSiRlmn8tgwsHnt8tpR2RG5uD5WAZI0P7xNhMPzJcK1AG7A==' ]);


  nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
    .post('/system', {"query":"      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ","variables":{"projectId":"cj7elbt9z0a3z011214m21uwn","force":true,"isDryRun":false,"config":"{\"modules\":[{\"name\":\"\",\"content\":\"\\ntypes: ./types.graphql\\nfunctions: {}\\npermissions:\\n- isEnabled: true\\n  operation: File.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.delete\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.delete\\n  authenticated: false\\nrootTokens: []\\n\",\"files\":{\"./types.graphql\":\"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}\\n\"}}]}"}})
    .reply(200, {"data":{"push":{"migrationMessages":[],"errors":[],"project":{"id":"cj7elbt9z0a3z011214m21uwn","name":"Shellcrest Cub","alias":null,"projectDefinitionWithFileContent":"{\n  \"modules\": [{\n    \"name\": \"\",\n    \"content\": \"types: ./types.graphql\\nfunctions: {}\\npermissions:\\n- operation: File.read\\n- operation: File.create\\n- operation: File.update\\n- operation: File.delete\\n- operation: User.read\\n- operation: User.create\\n- operation: User.update\\n- operation: User.delete\\nrootTokens: []\\n\",\n    \"files\": {\n      \"./types.graphql\": \"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}\"\n    }\n  }]\n}"}}}}, [ 'Content-Type',
      'application/json',
      'Content-Length',
      '918',
      'Connection',
      'close',
      'Date',
      'Sun, 10 Sep 2017 10:22:26 GMT',
      'Request-Id',
      'eu-west-1:system:cj7elbuya0a5h0112bnoi0dj8',
      'Server',
      'akka-http/10.0.8',
      'X-Cache',
      'Miss from cloudfront',
      'Via',
      '1.1 973544984500f17f202d338274a94acc.cloudfront.net (CloudFront)',
      'X-Amz-Cf-Id',
      'LK9xxtmZvFJPJT7vvNW2IXfs_zAILtLgwJZDKeZaqYVXWgwZIYej2A==' ]);
})

describe('deploy', () => {
  test('fake', async () => {
    expect(1).toBe(1)
    const result = await Init.mock('-t', 'blank', '--name', 'Shellcrest Cub')
    expect(result.out.stdout.output).toMatchSnapshot()
  })
})