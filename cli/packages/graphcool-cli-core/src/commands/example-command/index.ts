import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class ExampleCommand extends Command {
  static topic = 'example'
  static command = 'command'
  static description = 'example command'
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Target name'
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    let {target} = this.flags

    const {id} = await this.env.getTarget(target)

    // continue
  }
}
