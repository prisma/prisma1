import test from 'ava'
import { testEnvironment } from './helpers/test_environment'
import authCommand from '../src/commands/auth'
import { authEndpoint, graphcoolConfigFilePath, systemAPIEndpoint } from '../src/utils/constants'
import TestAuthServer from './helpers/test_auth_server'
import { testToken } from './fixtures/mock_data'
import TestOut from './helpers/test_out'
import 'isomorphic-fetch'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
 - Succeeding auth without token
 - Succeeding auth with existing token
 */

test.afterEach(() => {
  new TestOut().write('\n')
  fetchMock.restore()
})

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
          id: 'abcdefghik'
        }
      }
    }
  })

  // configure auth dependencies
  const env = testEnvironment({})
  const authServer = new TestAuthServer()

  env.out.prefix((t as any)._test.title, '$ graphcool auth')

  // authenticate
  await t.notThrows(
    authCommand({}, env, authServer)
  )

  // verify result
  const {token} = JSON.parse(env.resolver.read(graphcoolConfigFilePath))
  t.is(token, testToken)
})

test('Succeeding auth with existing token', async t => {

  // configure HTTP mocks
  fetchMock.post(`${systemAPIEndpoint}`, {
    data: {
      viewer: {
        user: {
          id: 'abcdefghik'
        }
      }
    }
  })

  // configure auth dependencies
  const props = {token: testToken}
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, testToken)
  const authServer = new TestAuthServer()

  env.out.prefix((t as any)._test.title, `$ graphcool auth -t ${testToken}`)

  // authenticate
  await t.notThrows(
    authCommand(props, env, authServer)
  )

  // verify result
  const {token} = JSON.parse(env.resolver.read(graphcoolConfigFilePath))
  t.is(token, testToken)
})
