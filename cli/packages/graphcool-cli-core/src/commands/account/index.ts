import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class Account extends Command {
  static topic = 'account'
  static description = 'Get info about the current account'
  async run() {
    await this.auth.ensureAuth()
    const account = await this.client.getAccount()

    this.out.log(`\
Email: ${account.email}
Name: ${account.name}`)
  }
}
