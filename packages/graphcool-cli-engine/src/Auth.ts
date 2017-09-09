import { Config } from './Config'
import { Output } from './Output/index'
import 'isomorphic-fetch'
import * as cuid from 'cuid'
import * as opn from 'opn'
import { AuthTrigger } from './types'

export class Auth {
  out: Output
  config: Config
  authTrigger: AuthTrigger = 'auth'

  constructor(out: Output, config: Config) {
    this.out = out
    this.config = config
  }

  setAuthTrigger(authTrigger: AuthTrigger) {
    this.authTrigger = authTrigger
  }

  async ensureAuth() {
    const token = this.config.token || await this.requestAuthToken()

    const valid = await this.validateAuthToken(token)
    if (!valid) {
      this.config.setToken(null)
      this.config.saveToken()
      this.out.error(`Received invalid token. Please try ${this.out.color.bold('graphcool auth')} again to get a valid token.`)
      this.out.exit(1)
    }

    return true
  }

  async setToken(token: string) {
    const valid = await this.validateAuthToken(token)
    if (valid) {
      this.config.setToken(token)
    } else {
      this.config.setToken(null)
      this.config.saveToken()
      this.out.error(`You provided an invalid token. You can run ${this.out.color.bold('graphcool auth')} to receive a valid auth token`)
      this.out.exit(1)
    }
  }

  async requestAuthToken(): Promise<string> {
    const cliToken = cuid()

    await fetch(`${this.config.authEndpoint}/create`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({cliToken}),
    })

    // TODO adjust
    opn(`${this.config.authUIEndpoint}?cliToken=${cliToken}&authTrigger=${this.authTrigger}`)

    while (true) {
      const url = `${this.config.authEndpoint}/${cliToken}`
      const result = await fetch(url)

      const json = await result.json()
      const {authToken} = json
      if (authToken) {
        return authToken as string
      }
    }
  }

  async validateAuthToken(token: string): Promise<string | null> {
    const authQuery = `{
      viewer {
        user {
          id
          email
        }
      }
    }`

    try {
      const result = await fetch(this.config.systemAPIEndpoint, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({query: authQuery})
      })
      const json = await result.json()

      if (!json.data.viewer.user || !json.data.viewer.user.email || json.errors) {
        return null
      }

      return json.data.viewer.user.email
    }
    catch (e) {
      return null
    }
  }
}