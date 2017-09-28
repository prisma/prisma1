import { Config } from './Config'
import { Output } from './Output/index'
import 'isomorphic-fetch'
import * as cuid from 'cuid'
import * as opn from 'opn'
import { AuthTrigger } from './types'
import { Client } from './Client/Client'
import { GraphQLClient } from 'graphql-request'
import * as chalk from 'chalk'
const debug = require('debug')('auth')

export class Auth {
  out: Output
  config: Config
  authTrigger: AuthTrigger = 'auth'
  client: Client

  constructor(out: Output, config: Config, client: Client) {
    this.out = out
    this.config = config
    this.client = client
  }

  setAuthTrigger(authTrigger: AuthTrigger) {
    this.authTrigger = authTrigger
  }

  async ensureAuth() {
    const token = this.config.token || (await this.requestAuthToken())

    const valid = await this.validateAuthToken(token)
    if (!valid) {
      this.out.error(
        `Received invalid token. Please try ${this.out.color.bold(
          'graphcool auth',
        )} again to get a valid token.`,
      )
      this.out.exit(1)
    }

    this.config.setToken(token)
    this.config.saveToken()
    this.client.updateClient()

    // return if we already had a token
    return !!this.config.token
  }

  async setToken(token: string) {
    const valid = await this.validateAuthToken(token)
    if (valid) {
      this.config.setToken(token)
    } else {
      this.config.setToken(null)
      this.config.saveToken()
      this.out.error(
        `You provided an invalid token. You can run ${this.out.color.bold(
          'graphcool auth',
        )} to receive a valid auth token`,
      )
      this.out.exit(1)
    }
  }

  async requestAuthToken(): Promise<string> {
    const cliToken = cuid()
    const url = `${this.config
      .authUIEndpoint}?cliToken=${cliToken}&authTrigger=${this.authTrigger}`
    this.out.log(`Auth URL: ${chalk.underline(url)}`)
    this.out.action.start(`Authenticating`)

    await fetch(`${this.config.authEndpoint}/create`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ cliToken }),
    })

    opn(url)

    while (true) {
      const endpointUrl = `${this.config.authEndpoint}/${cliToken}`
      const result = await fetch(endpointUrl)

      const json = await result.json()
      const { authToken } = json
      if (authToken) {
        this.out.action.stop()
        return authToken as string
      }
      await new Promise(r => setTimeout(r, 500))
    }
  }

  async validateAuthToken(token: string): Promise<string | null> {
    debug('requesting', this.config.systemAPIEndpoint)
    debug('token', token)
    const client = new GraphQLClient(this.config.systemAPIEndpoint, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    })

    const authQuery = `{
      viewer {
        user {
          id
          email
        }
      }
    }`

    const result = await client.request<{
      viewer: { user: { email: string } }
    }>(authQuery)

    if (!result.viewer.user || !result.viewer.user.email) {
      return null
    }

    return result.viewer.user.email
  }
}
