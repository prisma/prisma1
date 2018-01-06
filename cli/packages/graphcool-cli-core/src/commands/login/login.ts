import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
const debug = require('debug')('login')

export default class Login extends Command {
  static topic = 'login'
  static description = 'Login or signup to the Graphcool Cloud'
  static flags: Flags = {
    key: flags.string({
      char: 'k',
      description: 'Cloud session key',
    }),
  }
  async run() {
    const { key } = this.flags

    const secret = await this.client.requestCloudToken()

    const url = `${this.config.consoleEndpoint}/cli-auth?secret=${secret}`

    this.out.log(`Opening ${url} in the browser\n`)

    opn(url)

    this.out.action.start(`Authenticating`)

    let token
    while (!token) {
      const cloud = await this.client.cloudTokenRequest(secret)
      if (cloud.token) {
        token = cloud.token
      }
      await new Promise(r => setTimeout(r, 1000))
    }

    this.env.databaseRC.cloudSessionKey = token
    this.env.saveGlobalRC()
    debug(`Saved token ${token}`)

    this.out.action.stop()
  }
}
