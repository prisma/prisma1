import {AuthServer} from '../types'
import {testToken} from '../../tests/mock_data/mockData'

export default class TestAuthServer implements AuthServer {

  requestAuthToken(): Promise<string> {
    return new Promise((resolve, reject) => resolve(testToken))
  }

  async validateAuthToken(token: string) {
    return Promise.resolve('johnny@graph.cool')
  }

}
