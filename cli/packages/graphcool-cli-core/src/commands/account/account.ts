import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class Account extends Command {
  static topic = 'account'
  static description = 'Display account information'
  static group = 'platform'
  async run() {
    await this.client.ensureAuth()
    const account = await this.client.getAccount()

    this.out.log(`\
Email: ${account!.login[0].email}
Name:  ${account!.name}`)
  }
}
