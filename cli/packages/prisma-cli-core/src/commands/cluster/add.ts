import { Command, flags, Flags } from 'prisma-cli-engine'
import chalk from 'chalk'
import { Cluster } from 'prisma-yml'
const debug = require('debug')('add')

export default class ClusterAdd extends Command {
  static topic = 'cluster'
  static command = 'add'
  static description = 'Add an existing cluster'
  static group = 'cluster'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Cluster name',
    }),
    endpoint: flags.string({
      char: 'e',
      description: 'Cluster endpoint',
    }),
    secret: flags.string({
      char: 's',
      description: 'Cluster secret',
    })
  }
  async run() {
    const nameFlag = this.flags['name']
    const endpointFlag = this.flags['endpoint']
    const secretFlag = this.flags['secret']

    let name
    let endpoint
    let secret

    if(nameFlag || endpointFlag || secretFlag){
      if(!endpointFlag || !nameFlag){
        throw new Error(
          `You must define flags with cluster name, endpoint and optional secret.`,
        )
      }

      endpoint = endpointFlag
      secret = secretFlag || ''
      name = nameFlag
    } else {
      endpoint = await this.endpointSelector()
      secret = await this.secretSelector()
      name = await this.nameSelector()
    }

    const cluster = new Cluster(this.out, name, endpoint, secret)
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

  private async secretSelector(): Promise<string> {
    const question = {
      name: 'secret',
      type: 'input',
      message: 'Please provide the cluster secret',
    }

    const { secret } = await this.out.prompt(question)

    return secret
  }
}
