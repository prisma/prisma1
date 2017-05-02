import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import exportCommand from '../src/commands/export'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import {

} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'
import {mockedExportResponse} from './mock_data/mockData'
import {mockedPullProjectFile1} from './mock_data/mockData'
import 'isomorphic-fetch'
import {testSeparator} from './mock_data/mockData'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
- Export without project file but passing project ID as argument
- Export with project file
- Export with multiple project files
 */

test.afterEach(() => {
  fetchMock.restore()
})

const description1 = 'Export without project file but passing project ID as argument'
test(description1, async t => {
  const sourceProjectId = "cj26898xqm9tz0126n34d64ey"

  const command1 = `$ graphcool export -s ${sourceProjectId}`
  const separator = testSeparator(description1, command1)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedExportResponse)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy export data
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { sourceProjectId }

  await t.notThrows(
    exportCommand(props, env)
  )
})

const description2 = 'Export with project file'
test(description2, async t => {
  const projectFile = 'example.graphcool'

  const command2 = `$ graphcool export -p ${projectFile}`
  const separator = testSeparator(description2, command2)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedExportResponse)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy export data
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockedPullProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { projectFile }

  await t.notThrows(
    exportCommand(props, env)
  )
})

const description3 = 'Export with multiple project files'
test(description3, async t => {
  const command3 = `$ graphcool export`
  const separator = testSeparator(description3, command3)
  console.log(separator)

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedExportResponse)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy export data
  const env = testEnvironment({})
  const projectFile1 = 'example.graphcool'
  const projectFile2 = graphcoolProjectFileName
  env.resolver.write(projectFile1, mockedPullProjectFile1)
  env.resolver.write(projectFile2, mockedPullProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = { }

  await t.throws(
    exportCommand(props, env)
  )
})

function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
