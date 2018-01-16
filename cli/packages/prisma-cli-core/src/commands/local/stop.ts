import { Command, flags, Flags } from 'prisma-cli-engine'
import Docker from './Docker'
import { prettyTime } from '../../util'

export default class Stop extends Command {
  static topic = 'local'
  static command = 'stop'
  static description = 'Stop local development cluster'
  static group = 'local'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the cluster instance',
      defaultValue: 'local',
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config, this.env, this.flags.name)
    this.out.action.start('Booting local development cluster')
    const before = Date.now()
    await docker.stop()
    this.out.action.stop(prettyTime(Date.now() - before))
  }
}
