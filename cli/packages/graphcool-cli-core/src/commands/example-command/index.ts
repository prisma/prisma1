import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidTargetError'

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

    const foundTarget = await this.env.getTarget(target)

    if (!foundTarget) {
      this.out.error(new InvalidProjectError())
    } else {
      // execute the command
    }
  }
}
