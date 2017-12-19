import {
  Command,
  flags,
  Flags,
  AuthenticateCustomerPayload,
} from 'graphcool-cli-engine'
import { Cluster } from 'graphcool-yml'
import Docker from './Docker'
import chalk from 'chalk'
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

    const {
      envVars: { MASTER_TOKEN, PORT, FUNCTIONS_PORT },
      hostName,
    } = await docker.up()

    this.out.log('')
    this.out.action.start(
      'Waiting for Graphcool to initialize. This can take several minutes',
    )
    const cluster =
      this.env.clusterByName(name) ||
      new Cluster(name, `http://${hostName}:${PORT}`, '')
    await this.client.waitForLocalDocker(cluster.getDeployEndpoint())

    this.out.action.stop()

    if (!this.env.clusterByName(name)) {
      debug('Setting cluster', cluster)
      this.env.addCluster(cluster)
      this.env.saveGlobalRC()
      this.out.log(
        `\nSuccess! Added local cluster ${chalk.bold(`\`${name}\``)} to ${
          this.config.globalConfigPath
        }\n`,
      )
    } else {
      this.out.log(
        `\nSuccess! Cluster ${name} already exists and is up-to-date.`,
      )
    }

    const showInit = !this.definition.definition
    this.out.log(`To get started, execute

${showInit && `  ${chalk.green('$ graphcool init')}\n`}  ${chalk.green(
      '$ graphcool deploy',
    )}
`)
  }
}
