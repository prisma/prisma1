import { Command, flags, Flags } from 'graphcool-cli-engine'

export default class ExampleCommand extends Command {
  static topic = 'example'
  static command = 'command'
  static description = 'example command'
  static flags: Flags = {
    stage: flags.string({
      char: 't',
      description: 'Target name',
    }),
  }
  async run() {
    let {} = this.flags

    // const { id } = await this.env.getTarget(stage)

    // continue
  }
}
