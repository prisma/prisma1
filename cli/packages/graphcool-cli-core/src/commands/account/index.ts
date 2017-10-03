import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class Info extends Command {
  static topic = 'info'
  static description = 'Get info about the current account'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to set',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
  }
}
