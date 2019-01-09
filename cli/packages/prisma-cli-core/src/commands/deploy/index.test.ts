import Deploy from './deploy'
import { changedDefaultDefinition } from '../../examples'
import { mockDefinition, Config } from 'prisma-cli-engine'
import default_definition from './nocks/default_definition'
import local_instance from './nocks/local_instance'
import { MockGraphQLClient } from '../../test/mock-client'

jest.mock('graphql-request')
const GraphQLClient = require('graphql-request').GraphQLClient
GraphQLClient.mockImplementation(MockGraphQLClient)

const mockEnv = {
  stages: {
    default: 'dev',
    dev: 'cj8be5ct201is0140cq7qp23b',
  },
}

const localMockEnv = {
  clusters: {
    default: 'local',
    local: {
      token:
        'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDcwMjM3NzMsImNsaWVudElkIjoiY2o4YTAxZHN1MDAwMDAxMjM1aWF1aTFoYiJ9.WscmbACu0HqPEDSk_U66TNOskGddmt2plJAew6XCyNw',
      host: 'http://localhost:4466',
    },
  },
}

describe.skip('deploy', () => {
  test('from empty to default definition', async () => {
    default_definition()
    const result = await Deploy.mock({ mockEnv, mockDefinition })
    expect(result.out.stdout.output).toMatchSnapshot()
  })

  test.skip('to local instance', async () => {
    local_instance()
    const result = await Deploy.mock(
      {
        mockDefinition: changedDefaultDefinition,
        mockEnv: localMockEnv,
      },
      '-n',
      'servicename2',
      '-c',
      'local',
    )
    expect(result.out.stdout.output).toMatchSnapshot()
  })
})
