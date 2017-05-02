import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {
  mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3, testSeparator,
} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
Tests:
- Succeeding schema migration with default project file
- Succeeding schema migration as dry run
- Succeeding schema migration without specified project file (fallback to default)
- Succeeding schema migration with renamed project file
 */

test.afterEach(() => {
  fetchMock.restore()
})

const description1 = 'Succeeding schema migration with default project file'
test(description1, async t => {
  const command1 = `$ graphcool push -p ${graphcoolProjectFileName}`
  const separator = testSeparator(description1, command1)
  console.log(separator)

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

const description2 = 'Succeeding schema migration as dry run'
test(description2, async t => {
  const command2 = `$ graphcool push -p ${graphcoolProjectFileName} -d`
  const separator = testSeparator(description2, command2)
  console.log(separator)

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

const description3 = 'Succeeding schema migration without specified project file (fallback to default)'
test(description3, async t => {
  const command3 = `$ graphcool push`
  const separator = testSeparator(description3, command3)
  console.log(separator)

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

const description4 = 'Succeeding schema migration with renamed project file'
test(description4, async t => {
  const command4 = `$ graphcool push`
  const separator = testSeparator(description4, command4)
  console.log(separator)

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
