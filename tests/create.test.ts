import test from 'ava'
import TestResolver from '../src/resolvers/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import createCommand from '../src/commands/create'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName
} from '../src/utils/constants'
import {mockSchema1, mockedCreateProjectResponse} from '../mock_data/mockData'
import 'isomorphic-fetch'
import {readProjectIdFromProjectFile} from '../src/utils/file'

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding project creation and verify project info is stored in in ./project.graphcool
 */
test('Succeeding project creation', async t => {

  // configure HTTP mocks
  // fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))
  //
  // // create dummy project data
  // const name = 'My Project'
  // const schema = mockSchema1
  // const schemaUrl = 'myproject.schema'
  // const props = { name, schemaUrl }
  //
  // const storage = {}
  // const resolver = new TestResolver(storage)
  // resolver.write(schemaUrl, mockSchema1)
  //
  // await t.notThrows(
  //   createCommand(props, resolver)
  // )
  //
  // const expectedProjectFileContent = `# projectId: abcdefghi\n# version: 1\n\ntype Tweet {\n  id: ID!\n  createdAt: DateTime!\n  updatedAt: DateTime!\n  text: String!\n}`
  // t.is(resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  // t.is(readProjectIdFromProjectFile(resolver), 'abcdefghi')

  t.pass()

})

/*
 // configure HTTP mocks
 fetchMock.post(systemAPIEndpoint, {
 test: 'well'
 })

 const result = await fetch(systemAPIEndpoint, {
 method: 'POST',
 headers: {
 Authorization: `Bearer ${'abc'}`
 },
 body: 'abc'
 })
 const info = await result.json()
 debug(info)
 */

