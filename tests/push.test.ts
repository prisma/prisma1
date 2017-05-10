import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import 'isomorphic-fetch'
import pushCommand from '../src/commands/push'
import {systemAPIEndpoint, graphcoolProjectFileName, graphcoolConfigFilePath} from '../src/utils/constants'
import { mockedPushSchemaResponse, mockProjectFile2, mockProjectFile3 } from './mock_data/mockData'
import { TestSystemEnvironment } from '../src/types'
import TestOut from '../src/system/TestOut'
import {modifiedTwitterSchemaJSONFriendly} from './mock_data/schemas'

const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
Tests:
- Succeeding schema migration with default project file
- Succeeding schema migration without specified project file (fallback to default)
- Succeeding schema migration with renamed project file
*/

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})

test('Succeeding schema migration with default project file', async t => {

  // fetchMock.mock(function(url, options){
  //   console.log(`Call fetch on ${url} with options: ${JSON.stringify(options)}`)
  //   return true
  // })
  t.pass()

  // configure HTTP mocks
  // fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))

  // // dummy migration data
  // const env = testEnvironment({})
  // env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
  // env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
  // const props = { force: false , projectFilePath: graphcoolProjectFileName}
  //
  // env.out.prefix((t as any)._test.title, `$ graphcool push -p ${graphcoolProjectFileName}`)
  //
  // await t.notThrows(
  //   pushCommand(props, env)
  // )
  //
  // const expectedProjectFileContent = mockProjectFile3
  // const result = env.resolver.read(`./${graphcoolProjectFileName}`)
  //
  // t.is(result, expectedProjectFileContent)
})

// test('Succeeding schema migration without specified project file (fallback to default)', async t => {
//   // configure HTTP mocks
//   fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))
//
//   // dummy migration data
//   const env = testEnvironment({})
//   env.resolver.write(graphcoolProjectFileName, mockProjectFile2)
//   env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
//   const props = { force: false }
//
//   env.out.prefix((t as any)._test.title, `$ graphcool push`)
//
//   await t.notThrows(
//     pushCommand(props, env)
//   )
//
//   const expectedProjectFileContent = mockProjectFile3
//   const result = env.resolver.read(`./${graphcoolProjectFileName}`)
//
//   t.is(result, expectedProjectFileContent)
// })
//
// test('Succeeding schema migration with renamed project file', async t => {
//   // configure HTTP mocks
//   fetchMock.post(systemAPIEndpoint, JSON.parse(mockedPushSchemaResponse))
//
//   // dummy migration data
//   const projectFile = 'example.graphcool'
//   const env = testEnvironment({})
//   env.resolver.write(projectFile, mockProjectFile2)
//   env.resolver.write(graphcoolConfigFilePath, '{"token": ""}')
//   const props = { force: false }
//
//   env.out.prefix((t as any)._test.title, `$ graphcool push`)
//
//   await t.notThrows(
//     pushCommand(props, env)
//   )
//
//   const expectedProjectFileContent = mockProjectFile3
//   const result = env.resolver.read(graphcoolProjectFileName)
//
//   t.is(result, expectedProjectFileContent)
// })

function testEnvironment(storage: any): TestSystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut(),
  }
}
