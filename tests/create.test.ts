import test from 'ava'
import TestResolver from '../src/resolvers/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool-create')
import createCommand from '../src/commands/create'
import {
  mockSchema, mockedCreateProjectResponse, systemAPIEndpoint,
  graphcoolProjectFileName
} from '../src/utils/constants'
import 'isomorphic-fetch'
import {readProjectIdFromSchemaFile} from '../src/utils/file'

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding project creation and verify project info is stored in in ./project.graphcool
 */
test('Succeeding project creation', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const name = 'My Project'
  const schema = mockSchema
  const schemaUrl = 'myproject.schema'
  const props = { name, schemaUrl }

  const storage = {}
  const resolver = new TestResolver(storage)
  resolver.write(schemaUrl, mockSchema)

  await t.notThrows(
    createCommand(props, resolver)
  )

  const expectedProjectFileContent = `# @project abcdefghi\n# @version 0.1\n\ntype Tweet {\n  id: ID!\n  createdAt: DateTime!\n  updatedAt: DateTime!\n  text: String!\n}`
  t.is(resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromSchemaFile(resolver), 'abcdefghi')

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

