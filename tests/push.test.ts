import test from 'ava'
import TestResolver from '../src/resolvers/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3} from '../mock_data/mockData'

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding schema migration and verify updated project info is stored in in ./project.graphcool
 */
test('Succeeding schema migration', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const storage = {}
  storage[`./${graphcoolProjectFileName}`] = mockProjectFile2
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const resolver = new TestResolver(storage)
  const props = {isDryRun: false}

  await t.notThrows(
    pushCommand(props, resolver)
  )

  const expectedProjectFileContent = mockProjectFile3
  t.is(resolver.read(`./${graphcoolProjectFileName}`), expectedProjectFileContent)

})

/*
 * Test succeeding schema migration (dry run) and verify project info is not updated ./project.graphcool
 */
// test('Succeeding schema migration (dry)', async t => {
//
//   // configure HTTP mocks
//   fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))
//
//   // dummy migration data
//   const storage = {}
//   storage[graphcoolProjectFileName] = mockProjectFile2
//   const resolver = new TestResolver(storage)
//   const props = {isDryRun: true}
//
//   await pushCommand(props, resolver)
//   const expectedProjectFileContent = mockProjectFile2
//
//   t.is(resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
//
// })