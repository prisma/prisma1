import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import createCommand from '../src/commands/init'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName, graphcoolConfigFilePath
} from '../src/utils/constants'
import {mockSchema1, mockedCreateProjectResponse, mockProjectFile1} from '../mock_data/mockData'
import 'isomorphic-fetch'
import {readProjectIdFromProjectFile} from '../src/utils/file'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'

/**
 Options:
 -u, --url <schema-url>    Url to a GraphQL schema
 -f, --file <schema-file>  Local GraphQL schema file
 -n, --name <name>         Project name
 -a, --alias <alias>       Project alias
 -r, --region <region>     AWS Region (default: us-west-2)
 -h, --help                Output usage information

 Note: This command will create a ${chalk.bold('project.graphcool')} config file in the current directory.
*/

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding project creation and verify project info is stored in in ./project.graphcool
 */
test('Succeeding project creation with local schema file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const name = 'My Project'
  const schema = mockSchema1
  const localSchemaFile = './myproject.schema'
  const props = { name, localSchemaFile }

  const storage = {}
  storage[localSchemaFile] = mockSchema1
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const env = testEnvironment(storage)

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver), 'abcdefghijklmn')
})

test('Succeeding project creation with remote schema file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))
  const remoteSchemaUrl = 'https://graphqlbin/project.schema'
  const schema = mockSchema1
  fetchMock.get(remoteSchemaUrl, schema)

  // create dummy project data
  const name = 'MyProject'
  const props = { name, remoteSchemaUrl }

  const storage = {}
  storage[graphcoolConfigFilePath] = '{"token": "abcdefgh"}'
  const env = testEnvironment(storage)

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver), 'abcdefghijklmn')
})

function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
