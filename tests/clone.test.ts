import test from 'ava'
import { testEnvironment } from './helpers/test_environment'
import initCommand from '../src/commands/init'
import {
  systemAPIEndpoint,
  graphcoolProjectFileName,
  graphcoolConfigFilePath,
  graphcoolCloneProjectFileName
} from '../src/utils/constants'
import {
  mockedClonedProjectResponse,
  mockProjectFile1,
  clonedMockProjectFile1
} from './fixtures/mock_data'
import 'isomorphic-fetch'
import { readProjectIdFromProjectFile,
  readVersionFromProjectFile
} from '../src/utils/file'
import TestOut from './helpers/test_out'

const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
- Succeeding project clone with specific name
- Succeeding project clone with specific name and different output path
- Succeeding project clone with specific name and project ID
- Succeeding project clone with specific name, project file and output path
- Failing because no project ID provided (no project file in current directory)
 */

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})


test('Succeeding project clone with specific name', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const copyProjectId = 'abcdefghiklmn'
  const clonedProjectName = `Clone of MyProject`
  const props = {
    name: clonedProjectName,
    copyProjectId,

  }

  // prepare environment
  const env = testEnvironment({})
  const projectFilePath = graphcoolProjectFileName
  env.resolver.write(projectFilePath, mockProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -n "Clone of MyProject" -c ${copyProjectId} `)

  await t.notThrows(
    init(props, env)
  )

  const expectedProjectFileContent = clonedMockProjectFile1
  const clonedProjectFileName = graphcoolCloneProjectFileName(projectFilePath)
  t.is(env.resolver.read(clonedProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, clonedProjectFileName), 'nmlkjihgfedcba')
  t.is(readVersionFromProjectFile(env.resolver, clonedProjectFileName), '1')
})

test('Succeeding project clone with specific name and different output path', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const copyProjectId = 'abcdefghiklmn'
  const clonedProjectName = `Clone of MyProject`
  const outputPath = `myclone.graphcool`
  const props = {name: clonedProjectName, outputPath, copyProjectId}

  // prepare environment
  const env = testEnvironment({})
  const projectFilePath = graphcoolProjectFileName
  env.resolver.write(projectFilePath, mockProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -n "Clone of MyProject" -o ${outputPath} -c ${copyProjectId} `)

  await t.notThrows(
    init(props, env)
  )

  const expectedProjectFileContent = clonedMockProjectFile1
  const clonedProjectFileName = outputPath
  t.is(env.resolver.read(clonedProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, clonedProjectFileName), 'nmlkjihgfedcba')
  t.is(readVersionFromProjectFile(env.resolver, clonedProjectFileName), '1')
})

test('Succeeding project clone with specific name', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const clonedProjectName = `Clone of MyProject`
  const copyProjectId = `abcdefghijklmn`
  const props = {name: clonedProjectName, copyProjectId}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -n "Clone of MyProject" -c ${copyProjectId}`)

  await t.notThrows(
    init(props, env)
  )

  const expectedProjectFileContent = clonedMockProjectFile1
  const clonedProjectFileName = graphcoolCloneProjectFileName(graphcoolProjectFileName)
  t.is(env.resolver.read(clonedProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, clonedProjectFileName), 'nmlkjihgfedcba')
  t.is(readVersionFromProjectFile(env.resolver, clonedProjectFileName), '1')

})

test('Succeeding project clone with specific name, project file and output path', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const copyProjectId = `abcdefghijklmn`
  const clonedProjectName = `Clone of MyProject`
  const projectFile = `example.graphcool`
  const props = {name: clonedProjectName, projectFile, copyProjectId}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')
  env.resolver.write(projectFile, mockProjectFile1)

  env.out.prefix((t as any)._test.title, `$ graphcool init -n "Clone of MyProject" -c ${copyProjectId} -p ${projectFile}`)

  await t.notThrows(
    init(props, env)
  )

  // make sure the original project file wasn't overridden
  t.is(env.resolver.read(projectFile), mockProjectFile1)

  const expectedProjectFileContent = clonedMockProjectFile1
  const clonedProjectFileName = graphcoolCloneProjectFileName(projectFile)
  t.is(env.resolver.read(clonedProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, clonedProjectFileName), 'nmlkjihgfedcba')
  t.is(readVersionFromProjectFile(env.resolver, clonedProjectFileName), '1')

})

test('Failing because no project ID provided (no project file in current directory)', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const clonedProjectName = `Clone of MyProject`
  const props = {name: clonedProjectName}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool init -n "Clone of MyProject"`)

  await t.throws(
    init(props, env)
  )

})

async function init(props, env) {
  try {
    await initCommand(props, env)
  }
  catch (e) {
    env.out.onError(e)
    throw e
  }
}
