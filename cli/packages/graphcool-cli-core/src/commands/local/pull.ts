import { Command, Flags, flags } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class PullLocal extends Command {
  static topic = 'local'
  static command = 'pull'
  static description = 'Download latest (or specific) framework cluster version'
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
    await docker.pull()
  }
}
