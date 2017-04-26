
import {GraphcoolAuthServer} from '../api/GraphcoolAuthServer'
import {AuthServer, TokenValidationResult} from '../types'
import {testToken} from '../../mock_data/mockData'

export default class TestAuthServer extends GraphcoolAuthServer implements AuthServer {

  requestAuthToken(): Promise<string> {
    return new Promise((resolve, reject) => resolve(testToken))
  }

  async validateAuthToken(token: string): Promise<TokenValidationResult> {
    return super.validateAuthToken(token)
  }
}



