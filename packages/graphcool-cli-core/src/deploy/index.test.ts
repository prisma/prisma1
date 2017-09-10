import * as nock from 'nock'
import {Config} from 'graphcool-cli-engine'
import { init } from '../test/mock-requests'
import Deploy from './'
import { changedDefaultDefinition, defaultDefinition } from '../examples'

const config = new Config()
describe('deploy', () => {
  test('without change', async () => {

    nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }"})
      .reply(200, {"data":{"viewer":{"user":{"id":"cj7ektagf09sw0112cxeyoqjr","email":"test-ci@graph.cool"}}}}, [ 'Content-Type',
        'application/json',
        'Content-Length',
        '92',
        'Connection',
        'close',
        'Date',
        'Sun, 10 Sep 2017 11:00:06 GMT',
        'Request-Id',
        'eu-west-1:system:cj7emob0n0ai50112657j1x3h',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 926c5f53581f4e2717deb4e0fac4efc6.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        '8ld-M4A-gszxxWbO0fr2LipWQf4Pa6fuwcTteK_z0lwn0kIkOP5bgQ==' ]);


    nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ","variables":{"projectId":"cj7elbt9z0a3z011214m21uwn","isDryRun":false,"config":"{\"modules\":[{\"name\":\"\",\"content\":\"\\ntypes: ./types.graphql\\nfunctions: {}\\npermissions:\\n- isEnabled: true\\n  operation: File.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.delete\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.delete\\n  authenticated: false\\nrootTokens: []\\n\",\"files\":{\"./types.graphql\":\"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}\\n\"}}]}"}})
      .reply(200, {"data":{"push":{"migrationMessages":[],"errors":[],"project":{"id":"cj7elbt9z0a3z011214m21uwn","name":"Shellcrest Cub","alias":null,"projectDefinitionWithFileContent":"{\n  \"modules\": [{\n    \"name\": \"\",\n    \"content\": \"types: ./types.graphql\\nfunctions: {}\\npermissions:\\n- operation: File.read\\n- operation: File.create\\n- operation: File.update\\n- operation: File.delete\\n- operation: User.read\\n- operation: User.create\\n- operation: User.update\\n- operation: User.delete\\nrootTokens: []\\n\",\n    \"files\": {\n      \"./types.graphql\": \"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}\"\n    }\n  }]\n}"}}}}, [ 'Content-Type',
        'application/json',
        'Content-Length',
        '918',
        'Connection',
        'close',
        'Date',
        'Sun, 10 Sep 2017 11:00:07 GMT',
        'Request-Id',
        'eu-west-1:system:cj7emobp90ai60112qe9jo689',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 b451ce1932d9b97c4ef54f2f37ecb931.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        '-fkEiv0LsvSXlLfLDXUPYv6duhBRFbQpsxETOP8aVW6NXQzk78xI6A==' ]);

    Deploy.mockDefinition = defaultDefinition
    Deploy.mockEnv = {
      default: 'dev',
      environments: {
        dev: 'cj7elbt9z0a3z011214m21uwn'
      }
    }
    const result = await Deploy.mock()
    expect(result.out.stdout.output).toMatchSnapshot()

    nock.cleanAll()
  })

  test('with change', async () => {

    nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"{\n      viewer {\n        user {\n          id\n          email\n        }\n      }\n    }"})
      .reply(200, {"data":{"viewer":{"user":{"id":"cj7ektagf09sw0112cxeyoqjr","email":"test-ci@graph.cool"}}}}, [ 'Content-Type',
        'application/json',
        'Content-Length',
        '92',
        'Connection',
        'close',
        'Date',
        'Sun, 10 Sep 2017 11:11:27 GMT',
        'Request-Id',
        'eu-west-1:system:cj7en2wxc0am20112d9ldfa02',
        'Server',
        'akka-http/10.0.8',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 e482e2c19d6e57adc72e19f731c7bf44.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'dVPKelegAj38sDAcZ8cVuTCgWbNgB27_dG7zv9RUO5enmwXE9wbYhg==' ]);


    nock('https://dev.api.graph.cool:443', {"encodedQueryParams":true})
      .post('/system', {"query":"      mutation($projectId: String!, $force: Boolean, $isDryRun: Boolean!, $config: String!) {\n        push(input: {\n          projectId: $projectId\n          force: $force\n          isDryRun: $isDryRun\n          config: $config\n          version: 1\n        }) {\n          migrationMessages {\n            type\n            action\n            name\n            description\n            subDescriptions {\n              type\n              action\n              name\n              description\n            }\n          }\n          errors {\n            description\n            type\n            field\n          }\n          project {\n            id\n            name\n            alias\n            projectDefinitionWithFileContent\n          }\n        }\n      }\n    ","variables":{"projectId":"cj7elbt9z0a3z011214m21uwn","isDryRun":false,"config":"{\"modules\":[{\"name\":\"\",\"content\":\"\\ntypes: ./types.graphql\\nfunctions: {}\\npermissions:\\n- isEnabled: true\\n  operation: File.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: File.delete\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.read\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.create\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.update\\n  authenticated: false\\n- isEnabled: true\\n  operation: User.delete\\n  authenticated: false\\nrootTokens: []\\n\",\"files\":{\"./types.graphql\":\"type File implements Node {\\n  contentType: String!\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  name: String!\\n  secret: String! @isUnique\\n  size: Int!\\n  updatedAt: DateTime!\\n  url: String! @isUnique\\n}\\n\\ntype User implements Node {\\n  createdAt: DateTime!\\n  id: ID! @isUnique\\n  updatedAt: DateTime!\\n}\\n\\ntype Post {\\n  imageUrl: String!\\n}\\n\"}}]}"}})
      .reply(200, ["1f8b0800000000000003a454df6bdb3010fe57ae7aceb2a61b8cf96925a1d087964113f61007e25897449d2cb9924c5842fef7dd494edaa6f6c3983148febebbfbee173e08598442640751377ecb67a536ae08ca9a07f4bed8a017d9fc204c51a1c8c44feb83180889be74aa662b026fc1e00ec29f1a61a7c216c21681ed61c9e64b501e4a87454039245fdfac26afee293afb52a03b855a924951b691c7d18d90565e5594d0cce99e14d6ec7f99c3c96709859129cbe55370ca6caedea7765c0c4e894cf9f898075ba073d671d674af9d7dc63270d79424b3f2f91bea55f8bebf2ebeecaf47a39bd1d7ea66d4eccc6b054f5bd49a247d8071b3620dad0a0a671aadcf0127b85646b1f62faae54e691c5b13d0909238e406201795958d469f8b0ce61162903518c9452e0627b04cae09e7fa7c06c3cff132a449d7db179de766dd98582c9187237dd7e82ae57d44e8f3135842e25a64c0090da923b29348fdeca49a5af65112357ea4787f3a9522d1ad14a96ea548752bcd3cba4ea548742b45aa5b29526725676d98dadfc8dd9d2f0878339eb56ac7d84e91b1cbf19c47177b05aaaa355634530f8f562279b26b3b675edd0cda054f445af0db90c1842e5355612294cce07e72053f949f19f5d260447989de07f04821c219bbb0f76a4ff6f72624e3d48f0eb5c6e9ee10bc6ef4c6fab86f7df5fd5b193d79bc15e375e8113bfd34feab93bd298834ea231fc70521e248cf5f000000ffff0300b2e62f5b8b050000"], [ 'Content-Type',
        'application/json',
        'Transfer-Encoding',
        'chunked',
        'Connection',
        'close',
        'Vary',
        'Accept-Encoding',
        'Date',
        'Sun, 10 Sep 2017 11:11:28 GMT',
        'Request-Id',
        'eu-west-1:system:cj7en2x2k0am30112muvlj98k',
        'Server',
        'akka-http/10.0.8',
        'Content-Encoding',
        'gzip',
        'X-Cache',
        'Miss from cloudfront',
        'Via',
        '1.1 926c5f53581f4e2717deb4e0fac4efc6.cloudfront.net (CloudFront)',
        'X-Amz-Cf-Id',
        'FZwO6vcREexdulKnOHzTgTeR9I0R4yY4z85031kkFPOKSZXqDEpfHQ==' ]);

    Deploy.mockDefinition = changedDefaultDefinition
    Deploy.mockEnv = {
      default: 'dev',
      environments: {
        dev: 'cj7elbt9z0a3z011214m21uwn'
      }
    }
    const result = await Deploy.mock()
    expect(result.out.stdout.output).toMatchSnapshot()

    nock.cleanAll()
  })

})