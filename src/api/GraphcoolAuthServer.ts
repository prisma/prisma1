import {AuthServer, TokenValidationResult} from '../types'
import cuid = require('cuid')
import open = require('open')
import {systemAPIEndpoint, authEndpoint} from '../utils/constants'
const debug = require('debug')('graphcool')

export class GraphcoolAuthServer implements AuthServer {

  async requestAuthToken(): Promise<string> {
    const cliToken = cuid()
    // const apiEndpoint = 'https://cli-auth-api.graph.cool'

    debug(`Fetching from '${authEndpoint}/create' with token: ${cliToken}`)

    try {
      await fetch(`${authEndpoint}/create`, {
        method: 'post',
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ cliToken }),
      })
    }
    catch(e) {
      debug(`Error when trying to authenticate: ${JSON.stringify(e)}`)
    }

    const frontend = 'https://cli-auth.graph.cool'
    open(`${frontend}/?token=${cliToken}`)

    debug(`Waiting for auth token...\n`)

    while (true) {
      const result = await fetch(`${authEndpoint}/${cliToken}`)

      const json = await result.json()
      const {authToken} = json
      debug(`Received auth token: ${authToken}\n`)

      if (authToken) {
        return authToken as string
      }

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
      await fetch(systemAPIEndpoint, {
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
