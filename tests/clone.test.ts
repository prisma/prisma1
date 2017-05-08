import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import cloneCommand from '../src/commands/clone'
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
} from './mock_data/mockData'
import 'isomorphic-fetch'
import { readProjectIdFromProjectFile,
  readVersionFromProjectFile
} from '../src/utils/file'
import { TestSystemEnvironment } from '../src/types'
import TestOut from '../src/system/TestOut'

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
  fetchMock.restore()
})

test('Succeeding project clone with specific name', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const clonedProjectName = `Clone of MyProject`
  const props = {name: clonedProjectName}

  // prepare environment
  const env = testEnvironment({})
  const projectFilePath = graphcoolProjectFileName
  env.resolver.write(projectFilePath, mockProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool clone -n "Clone of MyProject"`)

  await t.notThrows(
    cloneCommand(props, env)
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
  const clonedProjectName = `Clone of MyProject`
  const outputPath = `myclone.graphcool`
  const props = {name: clonedProjectName, outputPath}

  // prepare environment
  const env = testEnvironment({})
  const projectFilePath = graphcoolProjectFileName
  env.resolver.write(projectFilePath, mockProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool clone -n "Clone of MyProject" -o ${outputPath}`)

  await t.notThrows(
    cloneCommand(props, env)
  )

  const expectedProjectFileContent = clonedMockProjectFile1
  const clonedProjectFileName = outputPath
  t.is(env.resolver.read(clonedProjectFileName), expectedProjectFileContent)
  t.is(readProjectIdFromProjectFile(env.resolver, clonedProjectFileName), 'nmlkjihgfedcba')
  t.is(readVersionFromProjectFile(env.resolver, clonedProjectFileName), '1')
})

test('Succeeding project clone with specific name and project ID', async t => {

  // configure HTTP mocks
  fetchMock.post(systemAPIEndpoint, JSON.parse(mockedClonedProjectResponse))

  // create dummy project data
  const clonedProjectName = `Clone of MyProject`
  const sourceProjectId = `abcdefghijklmn`
  const props = {name: clonedProjectName, sourceProjectId}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')

  env.out.prefix((t as any)._test.title, `$ graphcool clone -n "Clone of MyProject" -s ${sourceProjectId}`)

  await t.notThrows(
    cloneCommand(props, env)
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
  const clonedProjectName = `Clone of MyProject`
  const projectFile = `example.graphcool`
  const props = {name: clonedProjectName, projectFile}

  // prepare environment
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": "abcdefgh"}')
  env.resolver.write(projectFile, mockProjectFile1)

  env.out.prefix((t as any)._test.title, `$ graphcool clone -n "Clone of MyProject" -p ${projectFile}`)

  await t.notThrows(
    cloneCommand(props, env)
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

  env.out.prefix((t as any)._test.title, `$ graphcool clone -n "Clone of MyProject"`)

  await t.throws(
    clone(props, env)
  )

})


async function clone(props, env) {
  try {
    await cloneCommand(props, env)
  }
  catch (e) {
    env.out.onError(e)
    throw e
  }
}

function testEnvironment(storage: any): TestSystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut(),
  }
}

