import { Command, flags, Flags } from 'prisma-cli-engine'
import Docker from './Docker'

export default class Logs extends Command {
  static topic = 'local'
  static command = 'logs'
  static description = 'Print cluster logs'
  static group = 'local'
  static hidden = true
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the cluster instance',
      defaultValue: 'local',
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config, this.env, this.flags.name)
    await docker.init()
    await docker.logs()
  }
}
