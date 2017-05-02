import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import createCommand from '../src/commands/init'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName, graphcoolConfigFilePath
} from '../src/utils/constants'
import {
  mockedCreateProjectResponse, mockProjectFile1, mockedCreateProjectResponseWithAlias,
  mockProjectFileWithAlias1, testSeparator
} from './mock_data/mockData'
import {simpleTwitterSchema} from './mock_data/schemas'
import 'isomorphic-fetch'
import {readProjectIdFromProjectFile, readVersionFromProjectFile} from '../src/utils/file'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

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

const description1 = 'Succeeding project creation with local schema file'
test(description1, async t => {
  const name = 'MyProject'
  const localSchemaFile = './myproject.graphql'

  const command1 = `$ graphcool init -f ${localSchemaFile} -n ${name}`
  const separator = testSeparator(description1, command1)
  console.log(separator)

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
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


const description2 = 'Succeeding project creation with remote schema file'
test(description2, async t => {
  const name = 'MyProject'
  const remoteSchemaUrl = 'https://graphqlbin/project.graphql'

  const command2 = `$ graphcool init -u ${remoteSchemaUrl} -n ${name}`
  const separator = testSeparator(description2, command2)
  console.log(separator)

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  const schema = simpleTwitterSchema
  fetchMock.get(remoteSchemaUrl, schema)

  // create dummy project data
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

const description3 = 'Succeeding project creation with local file and different output path'
test(description3, async t => {
  const name = 'MyProject'
  const localSchemaFile = './myproject.graphql'
  const outputPath = '/Desktop/example.graphcool'

  const command3 = `$ graphcool init -f ${localSchemaFile} -n ${name} -o ${outputPath}`
  const separator = testSeparator(description3, command3)
  console.log(separator)

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
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


const description4 = 'Succeeding project creation because with invalid output path, falls back to default'
test(description4, async t => {
  const name = 'MyProject'
  const localSchemaFile = './myproject.graphql'
  const outputPath = '/Desktop/example.graphql'

  const command4 = `$ graphcool init -f ${localSchemaFile} -n ${name} -o ${outputPath}`
  const separator = testSeparator(description4, command4)
  console.log(separator)

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
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


const description5 = 'Succeeding project creation with alias'
test(description5, async t => {
  const name = 'MyProject'
  const localSchemaFile = './myproject.graphql'
  const alias = 'example'

  const command5 = `$ graphcool init -f ${localSchemaFile} -n ${name} -a ${alias}`
  const separator = testSeparator(description5, command5)
  console.log(separator)

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponseWithAlias))

  // create dummy project data
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
