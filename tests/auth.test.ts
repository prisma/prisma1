import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import authCommand from '../src/commands/auth'
import 'isomorphic-fetch'
import {authEndpoint, graphcoolConfigFilePath, systemAPIEndpoint} from '../src/utils/constants'
import TestAuthServer from '../src/api/TestAuthServer'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')
import {testToken} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'

test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding authentication and verify
 * the token is stored in ~/.graphcool
 */
test('Succeeding auth without token', async t => {
  // configure HTTP mocks
  fetchMock.post(`${authEndpoint}/create`, {})
  fetchMock.get(`${authEndpoint}/*`, {
    authToken: testToken
  })
  fetchMock.post(`${systemAPIEndpoint}`, {
    data: {
      viewer: {
        user: {
          id: "abcdefghik"
        }
      }
    }
  })

  // configure auth dependencies
  const props = { token: undefined }
  const env = testEnvironment({})
  const authServer = new TestAuthServer()

  // authenticate
  await t.notThrows(
    authCommand(props, env, authServer)
  )

  // verify result
  const {token} = JSON.parse(env.resolver.read(graphcoolConfigFilePath))
  t.is(token, testToken)
})

/*
 * Test succeeding authentication with existing token
 * and verify the correct token is still stored in ~/.graphcool
 */
test('Succeeding auth with existing token', async t => {
  // configure HTTP mocks
  fetchMock.post(`${systemAPIEndpoint}`, {
    data: {
      viewer: {
        user: {
          id: "abcdefghik"
        }
      }
    }
  })

  // configure auth dependencies
  const props = { token: testToken }
  const env = testEnvironment({ graphcoolConfigFilePath: testToken })
  const authServer = new TestAuthServer()

  // authenticate
  await t.notThrows(
    authCommand(props, env, authServer)
  )

  // verify result
  const {token} = JSON.parse(env.resolver.read(graphcoolConfigFilePath))
  t.is(token, testToken)
})

function testEnvironment(storage: any): SystemEnvironment {
  return {
    resolver: new TestResolver(storage),
    out: new TestOut()
  }
}
