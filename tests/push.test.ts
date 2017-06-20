import test from 'ava'
import { testEnvironment } from './helpers/test_environment'
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import statusCommand from '../src/commands/status'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {
  mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3,
  mockedPullProjectResponse1, mockedPushSchemaResponseError, mockModifiedProjectFile1, mockedPushSchema1ResponseError
} from './fixtures/mock_data'
import TestOut from './helpers/test_out'

const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
Tests:
- Succeeding schema migration using --force with default project file
- Succeeding schema migration using --force without specified project file (fallback to default)
- Succeeding schema migration using --force with renamed project file
- Failing schema migration because of multiple project files
- Schema migration displaying data loss
- Status with potential data loss
*/

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})

test('Succeeding schema migration using --force with default project file', async t => {

  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPullProjectResponse1))
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockModifiedProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true , projectFilePath: graphcoolProjectFileName}

  env.out.prefix((t as any)._test.title, `$ graphcool push ${graphcoolProjectFileName} --force`)

  await t.notThrows(
    push(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})

test('Succeeding schema migration using --force without specified project file (fallback to default)', async t => {
  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPullProjectResponse1))
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockModifiedProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool push --force`)

  await t.notThrows(
    push(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})

test('Succeeding schema migration using --force with renamed project file', async t => {
  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPullProjectResponse1))
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const projectFile = 'example.graphcool'
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockModifiedProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool push ${projectFile}`)

  await t.notThrows(
    push(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(projectFile)

  t.is(result, expectedProjectFileContent)
})

test('Failing schema migration because of multiple project files', async t => {
  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPullProjectResponse1))
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const projectFile1 = 'example.graphcool'
  const projectFile2 = 'project.graphcool'
  const env = testEnvironment({})
  env.resolver.write(projectFile1, mockProjectFile2)
  env.resolver.write(projectFile2, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool push`)

  await t.throws(
    push(props, env)
  )

  t.is(env.resolver.read(projectFile1), mockProjectFile2)
  t.is(env.resolver.read(projectFile2), mockProjectFile2)
})

test('Schema migration displaying data loss', async t => {
  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPullProjectResponse1))
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchema1ResponseError))

  // dummy migration data
  const projectFile = 'project.graphcool'
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockModifiedProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool push`)

  await t.notThrows(
    push(props, env)
  )

  t.is(env.resolver.read(projectFile), mockModifiedProjectFile1)
})

test('Status with potential data loss', async t => {
  // configure HTTP mocks
  fetchMock.once(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponseError))

  // dummy migration data
  const projectFile = 'project.graphcool'
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { }

  env.out.prefix((t as any)._test.title, `$ graphcool status`)

  await t.notThrows(
    statusCommand(props, env)
  )

  t.is(env.resolver.read(projectFile), mockProjectFile2)
})

async function push(props, env) {
  try {
    await pushCommand(props, env)
  }
  catch (e) {
    env.out.onError(e)
    throw e
  }
}
