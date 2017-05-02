import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import createCommand from '../src/commands/init'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName, graphcoolConfigFilePath
} from '../src/utils/constants'
import {
  mockedCreateProjectResponse, mockProjectFile1, mockedCreateProjectResponseWithAlias,
  mockProjectFileWithAlias1
} from './mock_data/mockData'
import {simpleTwitterSchema} from './mock_data/schemas'
import 'isomorphic-fetch'
import {readProjectIdFromProjectFile, readVersionFromProjectFile} from '../src/utils/file'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'

/*
 Tests:
 - Succeeding project creation with local schema file
 - Succeeding project creation with remote schema file
 - Succeeding project creation with local file and different output path
 - Succeeding project creation because with invalid output path, falls back to default
 - Succeeding project creation with alias
 */

test.afterEach(() => {
  fetchMock.restore()
})

test('Succeeding project creation with local schema file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const name = 'My Project'
  const localSchemaFile = './myproject.graphql'
  const props = { name, localSchemaFile }

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(localSchemaFile, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')})

test('Succeeding project creation with remote schema file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  const remoteSchemaUrl = 'https://graphqlbin/project.graphql'
  const schema = simpleTwitterSchema
  fetchMock.get(remoteSchemaUrl, schema)

  // create dummy project data
  const name = 'MyProject'
  const props = { name, remoteSchemaUrl }

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')
})

test('Succeeding project creation with local file and different output path', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const name = 'My Project'
  const localSchemaFile = './myproject.graphql'
  const outputPath = '/Desktop/example.graphcool'
  const props = { name, localSchemaFile, outputPath }

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(localSchemaFile, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(outputPath), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, outputPath), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, outputPath), '1')
})

test('Succeeding project creation because with invalid output path, falls back to default', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const name = 'My Project'
  const localSchemaFile = './myproject.graphql'
  const outputPath = '/Desktop/example.graphql'
  const props = { name, localSchemaFile, outputPath }

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(localSchemaFile, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
})

test('Succeeding project creation with alias', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponseWithAlias))

  // create dummy project data
  const name = 'My Project'
  const localSchemaFile = './myproject.graphql'
  const alias = 'example'
  const props = { name, alias, localSchemaFile }

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(localSchemaFile, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  await t.notThrows(
    createCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFileWithAlias1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), alias)
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')
})


function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
