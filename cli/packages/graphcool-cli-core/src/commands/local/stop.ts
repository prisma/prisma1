import { Command, flags, Flags } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class Stop extends Command {
  static topic = 'local'
  static command = 'stop'
  static description = 'Stop an already initialized local Graphcool instance'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the new instance',
      defaultValue: 'dev'
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config, this.env, this.flags.name)
    await docker.stop()
  }
}
