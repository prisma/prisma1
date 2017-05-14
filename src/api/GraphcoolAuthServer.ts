import { AuthServer, AuthTrigger } from '../types'
import { systemAPIEndpoint, authEndpoint, authUIEndpoint } from '../utils/constants'
import 'isomorphic-fetch'
import cuid = require('cuid')
import open = require('open')
const debug = require('debug')('graphcool')

export class GraphcoolAuthServer implements AuthServer {

  private authTrigger: AuthTrigger

  constructor(authTrigger: AuthTrigger) {
    this.authTrigger = authTrigger
  }

  async requestAuthToken(): Promise<string> {
    const cliToken = cuid()

    await fetch(`${authEndpoint}/create`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({cliToken}),
    })

    // TODO adjust
    open(`${authUIEndpoint}?cliToken=${cliToken}&authTrigger=${this.authTrigger}`)

    while (true) {
      const url = `${authEndpoint}/${cliToken}`
      const result = await fetch(url)

      const json = await result.json()
      const {authToken} = json
      if (authToken) {
        return authToken as string
      }
    }
  }

  async validateAuthToken(token: string) {

    const authQuery = `{
      viewer {
        user {
          id
          email
        }
      }
    }`

    try {
      const result = await fetch(systemAPIEndpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({query: authQuery})
      })
      const json = await result.json()

      if (!json.data.viewer.user || !json.data.viewer.user.email || json.errors) {
        return undefined
      }

      return json.data.viewer.user.email
    }
    catch (e) {
      return undefined
    }
  }

}
