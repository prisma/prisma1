import { Command, flags, Flags } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class Start extends Command {
  static topic = 'local'
  static command = 'start'
  static description = 'Start an already initialized local Graphcool instance'
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
    await docker.start()
  }
}
