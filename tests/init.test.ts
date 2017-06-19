import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import { Config } from '../src/utils/config'
import initCommand from '../src/commands/init'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName, graphcoolConfigFilePath
} from '../src/utils/constants'
import {
  mockedCreateProjectResponse, mockProjectFile1, mockedCreateProjectResponseWithAlias,
  mockProjectFileWithAlias1,
} from './fixtures/mock_data'
import { simpleTwitterSchema } from './fixtures/schemas'
import 'isomorphic-fetch'
import { readProjectIdFromProjectFile, readVersionFromProjectFile } from '../src/utils/file'
import { TestSystemEnvironment } from '../src/types'
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
  new TestOut().write('\n')
  fetchMock.restore()
})

test('Succeeding project creation with local schema file', async t => {
  const name = 'MyProject'
  const schemaUrl = './myproject.graphql'

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const props = {name, schemaUrl}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(schemaUrl, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -s ${schemaUrl} -n ${name}`)

  await t.notThrows(
    initCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')
})

test('Succeeding project creation with remote schema file', async t => {
  const name = 'MyProject'
  const schemaUrl = 'https://graphqlbin/project.graphql'

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  const schema = simpleTwitterSchema
  fetchMock.get(schemaUrl, schema)

  // create dummy project data
  const props = {name, schemaUrl}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -s ${schemaUrl} -n ${name}`)

  await t.notThrows(
    initCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')
})

test('Succeeding project creation with local file and different output path', async t => {
  const name = 'MyProject'
  const schemaUrl = './myproject.graphql'
  const outputPath = '/Desktop/example.graphcool'

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const props = {name, schemaUrl, outputPath}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(schemaUrl, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -s ${schemaUrl} -n ${name} -o ${outputPath}`)

  await t.notThrows(
    initCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(outputPath), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, outputPath), 'abcdefghijklmn')
  t.is(readVersionFromProjectFile(env.resolver, outputPath), '1')
})

test('Succeeding project creation with invalid output path, falls back to default', async t => {
  const name = 'MyProject'
  const schemaUrl = './myproject.graphql'
  const outputPath = '/Desktop/example.graphql'

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponse))

  // create dummy project data
  const props = {name, schemaUrl, outputPath}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(schemaUrl, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -s ${schemaUrl} -n ${name} -o ${outputPath}`)

  await t.notThrows(
    initCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
})

test('Succeeding project creation with alias', async t => {
  const name = 'MyProject'
  const schemaUrl = './myproject.graphql'
  const alias = 'example'

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedCreateProjectResponseWithAlias))

  // create dummy project data
  const props = {name, alias, schemaUrl}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(schemaUrl, simpleTwitterSchema)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -s ${schemaUrl} -n ${name} -a ${alias}`)

  await t.notThrows(
    initCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFileWithAlias1
  t.is(env.resolver.read(graphcoolProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, graphcoolProjectFileName), alias)
  t.is(readVersionFromProjectFile(env.resolver, graphcoolProjectFileName), '1')
})

function testEnvironment(storage: any): TestSystemEnvironment {
  const resolver = new TestResolver(storage)
  const config = new Config(resolver)

  return {
    resolver: resolver,
    out: new TestOut(),
    config: config
  }
}
