import * as nock from 'nock'
import Deploy from './'
import { changedDefaultDefinition } from '../../examples'
import { mockDefinition, Config } from 'graphcool-cli-engine'
import default_definition from './nocks/default_definition'
import local_instance from './nocks/local_instance'

afterAll(() => {
  nock.cleanAll()
})

const mockEnv = {
  default: 'dev',
  environments: {
    dev: 'cj8be5ct201is0140cq7qp23b',
  },
}

// default: local-dev
// environments:
//   local-dev:
// token: >-
//   eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDcwMjM3NzMsImNsaWVudElkIjoiY2o4YTAxZHN1MDAwMDAxMjM1aWF1aTFoYiJ9.WscmbACu0HqPEDSk_U66TNOskGddmt2plJAew6XCyNw
// host: 'http://localhost:60000'
// projectId: cj8bjoc2w001h01830u1caec2
//

const localMockEnv = {
  default: 'local-dev',
  environments: {
    'local-dev': {
      token: 'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpYXQiOjE1MDcwMjM3NzMsImNsaWVudElkIjoiY2o4YTAxZHN1MDAwMDAxMjM1aWF1aTFoYiJ9.WscmbACu0HqPEDSk_U66TNOskGddmt2plJAew6XCyNw',
      host: 'http://localhost:60000',
      projectId: null
    }
  }
}

describe('deploy', () => {
  test('from empty to default definition', async () => {
    default_definition()
    const result = await Deploy.mock({mockEnv, mockDefinition})
    expect(result.out.stdout.output).toMatchSnapshot()
  })

  test('to local instance', async () => {
    local_instance()
    const result = await Deploy.mock({
      mockDefinition: changedDefaultDefinition,
      mockEnv: localMockEnv,
    })
    expect(result.out.stdout.output).toMatchSnapshot()
  })
})
