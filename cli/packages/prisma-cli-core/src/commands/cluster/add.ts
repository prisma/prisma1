import { Command, flags, Flags } from 'prisma-cli-engine'
import chalk from 'chalk'
import { Cluster } from 'prisma-yml'
const debug = require('debug')('add')

export default class ClusterAdd extends Command {
  static topic = 'cluster'
  static command = 'add'
  static description = 'Add an existing cluster'
  static group = 'cluster'
  static deprecated = true
  async run() {
    const endpoint = await this.endpointSelector()
    const clusterSecret = await this.clusterSecretSelector()
    const name = await this.nameSelector()

    const cluster = new Cluster(
      this.out,
      name,
      endpoint,
      clusterSecret
        .trim()
        .replace(/\\r/g, '\r')
        .replace(/\\n/g, '\n'),
    )
    debug('Saving cluster', cluster)
    this.env.addCluster(cluster)
    this.env.saveGlobalRC()

    this.out.log(
      `Your cluster is set up and has been saved to ~/.prisma/config.yml. Try ${chalk.green(
        'prisma deploy',
      )} and choose ${chalk.green(name)} to deploy to your new cluster.`,
    )
  }

  private async nameSelector(): Promise<string> {
    const question = {
      name: 'name',
      type: 'input',
      message: 'Please provide a name for your cluster',
      defaultValue: 'cluster',
    }

    const { name } = await this.out.prompt(question)

    return name
  }

  private async endpointSelector(): Promise<string> {
    const question = {
      name: 'endpoint',
      type: 'input',
      message: 'Please provide the cluster endpoint',
    }

    const { endpoint } = await this.out.prompt(question)

    return endpoint
  }

  private async clusterSecretSelector(): Promise<string> {
    const question = {
      name: 'clusterSecret',
      type: 'input',
      message: 'Please provide the cluster secret',
    }

    const { clusterSecret } = await this.out.prompt(question)

    return clusterSecret
  }
}
