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

    await this.client.login()
  }
}
