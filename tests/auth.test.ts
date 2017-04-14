import test from 'ava'
import {TestResolver} from '../src/utils/file'
import * as os from 'os'
import * as path from 'path'
import authCommand from '../src/commands/auth'
import 'isomorphic-fetch'
import {GraphcoolAuthServer} from '../src/commands/auth'
import {TokenValidationResult, AuthServer} from '../src/types'
const fetchMock = require('fetch-mock')
const debug = require('debug')('graphcool')

const TEST_TOKEN = 'abcdefghijklmnopqrstuvwxyz'
const apiEndpoint = 'https://cli-auth-api.graph.cool'

const configFilePath = path.join(os.homedir(), '.graphcool')


test.afterEach(() => {
  fetchMock.reset()
})

/*
 * Test succeeding authentication and verify the token is stored in <home>/.graphcool
 */
test('Succeeding auth', async t => {
  // configure HTTP mocks
  fetchMock.post(`${apiEndpoint}/create`, {})
  fetchMock.get(`${apiEndpoint}/*`, {
    authToken: TEST_TOKEN
  })

  // configure auth dependencies
  const resolver = new TestResolver({})
  const props = { token: undefined }
  const authServer = new TestAuthServer()

  // start authentication
  await t.notThrows(
    authCommand(props, resolver, authServer)
  )

  // verify result
  const {token} = JSON.parse(resolver.read(configFilePath))
  t.is(token, TEST_TOKEN)
})




export class TestAuthServer extends GraphcoolAuthServer implements AuthServer {

  getAuthToken(): Promise<string> {
    return new Promise((resolve, reject) => resolve(TEST_TOKEN))
  }

  async validateAuthToken(token: string): Promise<TokenValidationResult> {
    return super.validateAuthToken(token)
  }
}



