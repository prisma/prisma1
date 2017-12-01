import { Command, flags, Flags } from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class Clusters extends Command {
  static topic = 'clusters'
  static description = 'Manage your private clusters'
  static flags: Flags = {
    connect: flags.boolean({
      char: 'c',
      description: 'Target name',
      required: true,
    }),
  }
  async run() {
    await this.auth.ensureAuth()

    const endpoint = await this.endpointSelector()
    const masterToken = await this.masterTokenSelector()
    const name = await this.nameSelector()

    this.out.action.start('Connecting to cluster')
    const { token } = await this.client.authenticateCustomer(
      endpoint + '/system',
      masterToken,
    )

    this.env.setGlobalCluster(name, {
      clusterSecret: token,
      faasHost: endpoint,
      host: endpoint,
    })
    this.env.saveGlobalRC()

    this.out.action.stop()
    this.out.log(
      `Your cluster is set up. Try ${chalk.green(
        'graphcool deploy --interactive',
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

  private async masterTokenSelector(): Promise<string> {
    const question = {
      name: 'masterToken',
      type: 'input',
      message: 'Please provide the master token',
    }

    const { masterToken } = await this.out.prompt(question)

    return masterToken
  }
}
