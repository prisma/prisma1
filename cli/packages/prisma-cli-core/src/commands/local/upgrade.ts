import { Command, Flags, flags } from 'prisma-cli-engine'
import Docker from './Docker'
import { prettyTime } from '../../util'
import { Cluster } from 'prisma-yml'

export default class UpgradeLocal extends Command {
  static topic = 'local'
  static command = 'upgrade'
  static description = 'Upgrade to the latest (or specific) cluster version'
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
    let before = Date.now()
    this.out.action.start('Pulling latest docker image')
    await docker.pull()
    this.out.action.stop(prettyTime(Date.now() - before))
    before = Date.now()
    this.out.action.start('Booting local development cluster')
    await docker.up()
    this.out.action.stop(prettyTime(Date.now() - before))

    docker.saveCluster()
  }
}
