import {AuthServer, TokenValidationResult} from '../types'
import cuid = require('cuid')
import open = require('open')
const debug = require('debug')('graphcool')

export class GraphcoolAuthServer implements AuthServer {

  async getAuthToken(): Promise<string> {
    const apiEndpoint = 'https://cli-auth-api.graph.cool'
    const cliToken = cuid()

    await fetch(`${apiEndpoint}/create`, {
      method: 'post',
      body: JSON.stringify({ cliToken }),
    })

    const frontend = 'https://cli-auth.graph.cool'
    open(`${frontend}/?token=${cliToken}`)

    while (true) {
      const result = await fetch(`${apiEndpoint}/${cliToken}`)
      const json = await result.json()
      debug(`Auth token JSON: ${JSON.stringify(json)}`)
      const {authToken} = json
      debug(`Received auth token: ${authToken}`)
      return authToken as string
    }

  }

  async validateAuthToken(token: string): Promise<TokenValidationResult> {

    const authQuery = `{
      viewer {
        user {
          id
        }
      }
    }`

    try {
      await fetch('https://api.graph.cool/system', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
        body: authQuery
      })
    }
    catch(e) {
      return 'invalid'
    }

    return 'valid'
  }


}
