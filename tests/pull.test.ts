import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import pullCommand from '../src/commands/pull'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {
  mockedPullProjectResponse1, mockedPullProjectFile1, mockedPullProjectFile2, mockedPullProjectResponse2,
  mockedPullProjectFileWithAlias1, mockedPullProjectResponseWithAlias1,
  mockedPullProjectResponseWithAlias2, mockedPullProjectFileWithAlias2, testSeparator
} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'
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
  fetchMock.restore()
})

const description1 = 'Pull without project file but passing project ID as argument'
test(description1, async t => {
  const sourceProjectId = "cj26898xqm9tz0126n34d64ey"

  const command1 = `$ graphcool pull -s ${sourceProjectId}`
  const separator = testSeparator(description1, command1)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { sourceProjectId }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

const description2 = 'Pull without project file (defaulting to project.graphcoool)'
test(description2, async t => {
  const command2 = `$ graphcool pull`
  const separator = testSeparator(description2, command2)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)
})


const description3 = 'Pull without project file (defaulting to project.graphcoool) updating version from 1 to 2'
test(description3, async t => {
  const command3 = `$ graphcool pull`
  const separator = testSeparator(description3, command3)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)
  debug(`Mocked response: ${JSON.stringify(mockedResponse)}`)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile2
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

const description4 = 'Pull without project file but passing project ID as argument, result has an alias'
test(description4, async t => {
  const sourceProjectId = "cj26898xqm9tz0126n34d64ey"

  const command4 = `$ graphcool pull -s ${sourceProjectId}`
  const separator = testSeparator(description4, command4)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponseWithAlias1)
  debug(`Mock response: ${JSON.stringify(mockedResponse)}`)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { sourceProjectId }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFileWithAlias1
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

const description5 = 'Pull without project file (defaulting to project.graphcoool) result has an alias, updating version from 1 to 2'
test(description5, async t => {
  const command5 = `$ graphcool pull`
  const separator = testSeparator(description5, command5)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponseWithAlias2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFileWithAlias2
  const result = env.resolver.read(graphcoolProjectFileName)

  t.is(result, expectedProjectFileContent)

})

const description6 = 'Pull with specific project file, updating version from 1 to 2'
test(description6, async t => {
  const projectFile = '/Desktop/example.graphcool'

  const command6 = `$ graphcool pull -p ${projectFile}`
  const separator = testSeparator(description6, command6)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse2)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(projectFile, mockedPullProjectFile1)
  const props = { projectFile }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile2
  const result = env.resolver.read(projectFile)

  t.is(result, expectedProjectFileContent)
})

const description7 = 'Pull without project file (defaulting to project.graphcoool) and specify output path'
test(description7, async t => {
  const outputPath = '/Desktop/example.graphcool'

  const command7 = `$ graphcool pull -o ${outputPath}`
  const separator = testSeparator(description7, command7)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedPullProjectResponse1)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy pull data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(graphcoolProjectFileName, mockedPullProjectFile1)
  const props = { outputPath }

  await t.notThrows(
    pullCommand(props, env)
  )

  const expectedProjectFileContent = mockedPullProjectFile1
  const result = env.resolver.read(outputPath)

  t.is(result, expectedProjectFileContent)
})

const description8 = 'Pull without project files but with multiple project files in current directory'
test(description8, async t => {
  const command8 = `$ graphcool pull`
  const separator = testSeparator(description8, command8)
  console.log(separator)

  const projectFile1 = 'example.graphcool'
  const projectFile2 = graphcoolProjectFileName
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  env.resolver.write(projectFile1, mockedPullProjectFile1)
  env.resolver.write(projectFile2, mockedPullProjectFile1)
  const props = { }

  await t.throws(
    pullCommand(props, env)
  )

  const expectedProjectFileContent1 = mockedPullProjectFile1
  const expectedProjectFileContent2 = mockedPullProjectFile1
  const result1 = env.resolver.read(projectFile1)
  const result2 = env.resolver.read(projectFile2)
  t.is(result1, expectedProjectFileContent1)
  t.is(result2, expectedProjectFileContent2)
})


function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
