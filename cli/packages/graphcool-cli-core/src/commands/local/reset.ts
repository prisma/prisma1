import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'

export default class Reset extends Command {
  static topic = 'local'
  static command = 'reset'
  static description = 'Reset the local project'
  static flags: Flags = {
    data: flags.boolean({
      char: 'd',
      description: 'Reset only the data of the project',
    }),
    schema: flags.boolean({
      char: 's',
      description: 'Reset only the schema of the project',
    }),
    env: flags.string({
      char: 'e',
      description: 'Environment to reset',
    }),
  }
  async run() {
    this.config.setLocal()
    let {env} = this.flags
    const {schema, data} = this.flags

    env = env || this.env.env.default

    const {projectId} = await this.env.getEnvironment({env})

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else {
      if (!schema && !data) {
        // noop
      }
    }
  }
}
