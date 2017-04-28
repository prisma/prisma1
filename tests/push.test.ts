import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool-t')
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
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
 * Test succeeding schema migration and verify updated project info is stored in in ./project2.graphcool
 */
test('Succeeding schema migration', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const storage = {}
  storage[`./${graphcoolProjectFileName}`] = mockProjectFile2
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const env = testEnvironment(storage)
  const props = { isDryRun: false , projectFilePath: graphcoolProjectFileName}

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})

/*
 * Test succeeding schema migration (dry run) and verify project info is not updated ./project2.graphcool
 */
test('Succeeding schema migration (dry)', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const storage = {}
  storage[`./${graphcoolProjectFileName}`] = mockProjectFile2
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const env = testEnvironment(storage)
  const props = { isDryRun: true , projectFilePath: graphcoolProjectFileName}

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile2
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})


/*
 * Test failing schema migration because of invalid session ./project2.graphcool
 */
// test('Failing schema migration because of lacking permissions (No valid session)', async t => {
//
//   // configure HTTP mocks
//   fetchMock.post(systemAPIEndpoint, JSON.parse(mockedInvalidSessionPushResponse))
//
//   // dummy migration data
//   const storage = {}
//   storage[`./${graphcoolProjectFileName}`] = mockProjectFile2
//   storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
//   const env = testEnvironment(storage)
//   const props = { isDryRun: false , projectFilePath: graphcoolProjectFileName}
//
//   await t.notThrows(
//     pushCommand(props, env)
//   )
//
//   const expectedProjectFileContent = mockProjectFile2
//   const result = env.resolver.read(`./${graphcoolProjectFileName}`)
//
//   t.is(result, expectedProjectFileContent)
//
// })

function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
