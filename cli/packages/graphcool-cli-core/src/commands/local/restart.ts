import { Command, flags, Flags } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class Restart extends Command {
  static topic = 'local'
  static command = 'restart'
  static description = 'Restart local development cluster'
  static group = 'local'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the new instance',
      defaultValue: 'local'
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config, this.env, this.flags.name)
    await docker.restart()
  }
}
