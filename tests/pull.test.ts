import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import 'isomorphic-fetch'
import pullCommand from '../src/commands/pull'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {
  mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3,
  mockedInvalidSessionPushResponse
} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'

test.afterEach(() => {
  debug(`Reset fetchMock!`)
  fetchMock.reset()
})

/*
 Tests:

 */

// test('Succeeding schema migration with project.graphcool as project file', async t => {
//
//   // configure HTTP mocks
//   fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))
//
//   // dummy migration data
//   const env = testEnvironment({})
//   env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
//   env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
//   const props = { isDryRun: false , projectFilePath: graphcoolProjectFileName}
//
//
//   const expectedProjectFileContent = mockProjectFile3
//   const result = env.resolver.read(`./${graphcoolProjectFileName}`)
//
//   t.is(result, expectedProjectFileContent)
// })
//
// function testEnvironment(storage: any): SystemEnvironment {
//   return {
//     resolver: new TestResolver(storage),
//     out: new TestOut()
//   }
// }
