import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import createCommand from '../src/commands/create'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName, graphcoolConfigFilePath
} from '../src/utils/constants'
import {mockSchema1, mockedCreateProjectResponse} from '../mock_data/mockData'
import 'isomorphic-fetch'
import {readProjectIdFromProjectFile} from '../src/utils/file'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'

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
  const schema = mockSchema1
  const schemaUrl = './myproject.schema'
  const props = { name, schemaUrl }

  const storage = {}
  storage[schemaUrl] = mockSchema1
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const env = testEnvironment(storage)

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = `# projectId: abcdefghi\n# version: 1\n\ntype Tweet {\n  id: ID!\n  createdAt: DateTime!\n  updatedAt: DateTime!\n  text: String!\n}`
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver), 'abcdefghi')

})


function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
