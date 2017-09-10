import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class ExampleCommand extends Command {
  static topic = 'example'
  static command = 'command'
  static description = 'example command'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to set'
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to set'
    }),
  }
  async run() {
    this.out.log('Running example command', this.flags)
  }
}
