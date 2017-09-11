import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'

export default class ExampleCommand extends Command {
  static topic = 'example'
  static command = 'command'
  static description = 'example command'
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
    let {env} = this.flags

    env = env || this.env.env.default

    const {projectId} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else {
      // execute the command
    }
  }
}
