import {
  Command,
  flags,
  Flags,
  AuthenticateCustomerPayload,
} from 'prisma-cli-engine'
import { Cluster } from 'prisma-yml'
import Docker from './Docker'
import chalk from 'chalk'
import { prettyTime } from '../../util'
const debug = require('debug')('up')

export default class Up extends Command {
  static topic = 'local'
  static command = 'start'
  static description = 'Start local development cluster'
  static group = 'local'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the cluster instance',
      defaultValue: 'local',
    }),
  }
  async run() {
    const { name } = this.flags

    const docker = new Docker(this.out, this.config, this.env, name)

    this.out.action.start('Booting local development cluster')

    await docker.init()

    const cluster = docker.saveCluster()

    await docker.up()

    const before = Date.now()
    await this.client.waitForLocalDocker(cluster.getDeployEndpoint())

    this.out.action.stop(prettyTime(Date.now() - before))

    const showInit = !this.definition.definition
    //     this.out.log(`To get started, execute

    // ${showInit && `  ${chalk.green('$ prisma init')}\n`}  ${chalk.green(
    //       '$ prisma deploy',
    //     )}
    // `)
  }
}
