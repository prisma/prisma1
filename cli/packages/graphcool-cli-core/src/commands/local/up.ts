import {
  Command,
  flags,
  Flags,
  AuthenticateCustomerPayload,
} from 'graphcool-cli-engine'
import { Cluster } from 'graphcool-yml'
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

    await docker.init()

    const { envVars: { MASTER_TOKEN, PORT, FUNCTIONS_PORT }, hostName } = docker
    const cluster =
      this.env.clusterByName(name) ||
      new Cluster(name, `http://${hostName}:${PORT}`, '')

    if (!this.env.clusterByName(name)) {
      debug('Setting cluster', cluster)
      this.env.addCluster(cluster)
      this.env.saveGlobalRC()
      // this.out.log(
      //   `\nSuccess! Added local cluster ${chalk.bold(`\`${name}\``)} to ${
      //     this.config.globalConfigPath
      //   }\n`,
      // )
    }

    this.out.action.start('Booting local development cluster')
    await docker.up()

    const before = Date.now()
    await this.client.waitForLocalDocker(cluster.getDeployEndpoint())

    this.out.action.stop(prettyTime(Date.now() - before))

    const showInit = !this.definition.definition
    //     this.out.log(`To get started, execute

    // ${showInit && `  ${chalk.green('$ graphcool init')}\n`}  ${chalk.green(
    //       '$ graphcool deploy',
    //     )}
    // `)
  }
}
