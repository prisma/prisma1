import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {
  mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3,
  mockedPullProjectResponse1
} from './mock_data/mockData'
import { TestSystemEnvironment } from '../src/types'
import TestOut from '../src/system/TestOut'
import {modifiedTwitterSchemaJSONFriendly} from './mock_data/schemas'

const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
Tests:
- Succeeding schema migration using --force with default project file
- Succeeding schema migration using --force without specified project file (fallback to default)
- Succeeding schema migration using --force with renamed project file
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
  env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true , projectFilePath: graphcoolProjectFileName}

  env.out.prefix((t as any)._test.title, `$ graphcool push ${graphcoolProjectFileName} --force`)

  await t.notThrows(
    pushCommand(props, env)
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
  env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: false }

  env.out.prefix((t as any)._test.title, `$ graphcool push --force`)

  await t.notThrows(
    pushCommand(props, env)
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
  env.resolver.write(projectFile, mockProjectFile2)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: false }

  env.out.prefix((t as any)._test.title, `$ graphcool push ${projectFile}`)

  await t.notThrows(
    pushCommand(props, env)
  )

  const expectedProjectFileContent = mockProjectFile3
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)
})

function testEnvironment(storage: any): TestSystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut(),
  }
}
