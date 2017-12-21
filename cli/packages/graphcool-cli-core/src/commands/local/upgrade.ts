import { Command, Flags, flags } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class UpgradeLocal extends Command {
  static topic = 'local'
  static command = 'upgrade'
  static description = 'Upgrade to the latest (or specific) cluster version'
  static group = 'local'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the cluster instance',
      defaultValue: 'local'
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config, this.env, this.flags.name)
    await docker.pull()
    await docker.up()
  }
}
