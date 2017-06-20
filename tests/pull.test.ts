import test from 'ava'
import { testEnvironment } from './helpers/test_environment'
import pullCommand from '../src/commands/pull'
import { systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath } from '../src/utils/constants'
import {
  mockedPullProjectResponse1, mockedPullProjectFile1, mockedPullProjectFile2, mockedPullProjectResponse2,
  mockedPullProjectFileWithAlias1, mockedPullProjectResponseWithAlias1,
  mockedPullProjectResponseWithAlias2, mockedPullProjectFileWithAlias2,
} from './fixtures/mock_data'
import TestOut from './helpers/test_out'
import 'isomorphic-fetch'

const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
 - Pull without project file but passing project ID as argument
 - Pull without project file (defaulting to project.graphcoool)
 - Pull without project file (defaulting to project.graphcoool) updating version from 1 to 2
 - Pull without project file but passing project ID as argument, result has an alias
 - Pull without project file (defaulting to project.graphcoool) result has an alias, updating version from 1 to 2
 - Pull with specific project file, updating version from 1 to 2
 - Pull without project file (defaulting to project.graphcoool) and specify output path
 - Pull without project files but with multiple project files in current directory
 */

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})

test('Pull without project file but passing project ID as argument', async t => {
  const sourceProjectId = 'cj26898xqm9tz0126n34d64ey'

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true, sourceProjectId}

  env.out.prefix((t as any)._test.title, `$ graphcool pull -s ${sourceProjectId}`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

test('Pull without project file (defaulting to project.graphcoool)', async t => {
  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool pull`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)
})

test('Pull without project file (defaulting to project.graphcoool) updating version from 1 to 2', async t => {
  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)
  debug(`Mocked response: ${JSON.stringify(mockedResponse)}`)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool pull`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile2
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

test('Pull without project file but passing project ID as argument, result has an alias', async t => {
  const sourceProjectId = 'cj26898xqm9tz0126n34d64ey'

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponseWithAlias1)
  debug(`Mock response: ${JSON.stringify(mockedResponse)}`)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { force: true, sourceProjectId}

  env.out.prefix((t as any)._test.title, `$ graphcool pull -s ${sourceProjectId}`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFileWithAlias1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

test('Pull without project file (defaulting to project.graphcoool) result has an alias, updating version from 1 to 2', async t => {
  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponseWithAlias2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool pull`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFileWithAlias2
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

test('Pull with specific project file, updating version from 1 to 2', async t => {
  const projectFile = '/Desktop/example.graphcool'

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(projectFile, mockedPullProjectFile1)
  const props = { force: true, projectFile}

  env.out.prefix((t as any)._test.title, `$ graphcool pull -p ${projectFile}`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile2
  const result = env.resolver.read(projectFile)

  t.is(result, expectedProjectFileContent)
})

test('Pull without project file (defaulting to project.graphcoool) and specify output path', async t => {
  const outputPath = '/Desktop/example.graphcool'

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { force: true, outputPath}

  env.out.prefix((t as any)._test.title, `$ graphcool pull -o ${outputPath}`)

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(outputPath)

  t.is(result, expectedProjectFileContent)
})

test('Pull without project files but with multiple project files in current directory', async t => {
  const projectFile1 = 'example.graphcool'
  const projectFile2 = graphcoolProjectFileName
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(projectFile1, mockedPullProjectFile1)
  env.resolver.write(projectFile2, mockedPullProjectFile1)
  const props = { force: true }

  env.out.prefix((t as any)._test.title, `$ graphcool pull`)


  await t.throws(
    pull(props, env)
  )

  const expectedProjectFileContent1 = mockedPullProjectFile1
  const expectedProjectFileContent2 = mockedPullProjectFile1
  const result1 = env.resolver.read(projectFile1)
  const result2 = env.resolver.read(projectFile2)
  t.is(result1, expectedProjectFileContent1)
  t.is(result2, expectedProjectFileContent2)
})

async function pull(props, env) {
  try {
    await pullCommand(props, env)
  }
  catch (e) {
    env.out.onError(e)
    throw e
  }
}
