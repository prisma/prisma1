import test from 'ava'
import TestResolver from '../src/system/TestResolver'
import authCommand from '../src/commands/auth'
import {authEndpoint, graphcoolConfigFilePath, systemAPIEndpoint} from '../src/utils/constants'
import TestAuthServer from '../src/api/TestAuthServer'
import {testToken, testSeparator} from './mock_data/mockData'
import {SystemEnvironment} from '../src/types'
import TestOut from '../src/system/TestOut'
import 'isomorphic-fetch'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

/*
 Tests:
- Succeeding auth without token
- Succeeding auth with existing token
 */

test.afterEach(() => {
  fetchMock.reset()
})

const description1 = 'Succeeding auth without token'
test(description1, async t => {
  const command1 = '$ graphcool auth'
  const separator = testSeparator(description1, command1)
  console.log(separator)

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
  const env = testEnvironment({})
  const authServer = new TestAuthServer()

  // authenticate
  await t.notThrows(
    authCommand({}, env, authServer)
  )

  // verify result
  const {token} = JSON.parse(env.resolver.read(graphcoolConfigFilePath))
  t.is(token, testToken)
})

const description2 = 'Succeeding auth with existing token'
test(description2, async t => {
  const command2 = `$ graphcool auth -t ${testToken}`
  const separator = testSeparator(description2, command2)
  console.log(separator)

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
  const env = testEnvironment({})
  env.resolver.write(graphcoolConfigFilePath, testToken)
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
