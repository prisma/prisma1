import test from 'ava'
import TestResolver from '../src/system/TestResolver'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
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
  fetchMock.restore()
})

/*
Tests:
- Succeeding schema migration with project.graphcool as project file
- Succeeding schema migration as dry run
- Succeeding schema migration without argument for project file (fallback to default)
- Succeeding schema migration with renamed project file
 */

test('Succeeding schema migration with project.graphcool as project file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { isDryRun: false , projectFilePath: graphcoolProjectFileName}

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})

test('Succeeding schema migration as dry run', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { isDryRun: true , projectFile: graphcoolProjectFileName}

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile2
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)
})

test('Succeeding schema migration without argument for project file (fallback to default)', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const env = testEnvironment({})
  env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { isDryRun: false }

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(`./${graphcoolProjectFileName}`)

  t.is(result, expectedProjectFileContent)
})

test('Succeeding schema migration with renamed project file', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // dummy migration data
  const projectFile = 'example.graphcool'
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { isDryRun: false }

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)
})



function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
