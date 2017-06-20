import test from 'ava'
import { testEnvironment } from './helpers/test_environment'
import exportCommand from '../src/commands/export'
import { systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath } from '../src/utils/constants'
import TestOut from './helpers/test_out'
import { mockedExportResponse } from './fixtures/mock_data'
import { mockedPullProjectFile1 } from './fixtures/mock_data'
import 'isomorphic-fetch'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
 - Export without project file but passing project ID as argument
 - Export with project file
 - Export with multiple project files
 */

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})

test('Export with project file', async t => {
  const projectFile = 'example.graphcool'

  // configure HTTP mocks
  const mockedResponse = JSON.parse(mockedExportResponse)
  fetchMock.post(systemAPIEndpoint, mockedResponse)

  // dummy export data
  const env = testEnvironment({})
  env.resolver.write(projectFile, mockedPullProjectFile1)
  env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  const props = {projectFile}

  env.out.prefix((t as any)._test.title, `$ graphcool export -p ${projectFile}`)

  await t.notThrows(
    exportCommand(props, env)
  )
})

test('Export with multiple project files', async t => {
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
  const props = {}

  env.out.prefix((t as any)._test.title, `$ graphcool export`)

  await t.throws(
    exportCommand(props, env)
  )
})
