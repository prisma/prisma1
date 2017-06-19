import {AuthServer} from '../../src/types'
import {testToken} from '../fixtures/mock_data'

export default class TestAuthServer implements AuthServer {

  requestAuthToken(): Promise<string> {
    return new Promise((resolve, reject) => resolve(testToken))
  }

  async validateAuthToken(token: string) {
    return Promise.resolve('johnny@graph.cool')
  }

}
