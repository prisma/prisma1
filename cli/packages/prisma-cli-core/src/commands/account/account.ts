import { Command } from 'prisma-cli-engine'
import chalk from 'chalk'

export default class Account extends Command {
  static topic = 'account'
  static description = 'Display account information'
  static group = 'platform'
  async run() {
    try {
      const account = await this.client.getAccount()

      this.out.log(`\
Email: ${account!.login[0].email}
Name:  ${account!.name}`)
    } catch (e) {
      this.out.log(
        `Currently not logged in. Run ${chalk.cyan('prisma login')} to login.`,
      )
    }
  }
}
