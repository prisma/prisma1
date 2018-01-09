import { Command, flags, Flags } from 'graphcool-cli-engine'
import Docker from '../local/Docker'
import { NoClusterSetError } from '../../errors/NoClusterSetError'
import { ClusterNotFoundError } from '../../errors/ClusterNotFoundError'
import { Cluster } from 'graphcool-yml'
import chalk from 'chalk'

export default class ClusterRemove extends Command {
  static topic = 'cluster'
  static command = 'remove'
  static description = 'Remove Cluster'
  static group = 'cluster'
  async run() {
    await this.definition.load(this.flags)

    const cluster = await this.getCluster()
    await this.askForConfirmation(`cluster ${cluster}`)

    this.out.action.start(`Removing cluster ${cluster} from ~/.graphcoolrc`)
    this.env.removeCluster(cluster)
    this.env.saveGlobalRC()
    this.out.action.stop()
  }
  async getCluster(): Promise<string> {
    const choices = this.env.clusters.filter(c => c.local).map(c => ({
      name: `${chalk.bold(c.name)} (${c.baseUrl})`,
      value: c.name,
    }))

    if (choices.length === 0) {
      throw new Error('There are no clusters to delete')
    }

    const question = {
      name: 'cluster',
      type: 'list',
      message: `Please choose the cluster you want to delete`,
      choices,
      pageSize: 9,
    }

    const { cluster } = await this.out.prompt(question)

    return cluster
  }

  private async askForConfirmation(cluster: string) {
    const confirmationQuestion = {
      name: 'confirmation',
      type: 'input',
      message: `Are you sure that you want to delete ${cluster}? y/N`,
      default: 'n',
    }
    const { confirmation }: { confirmation: string } = await this.out.prompt(
      confirmationQuestion,
    )
    if (confirmation.toLowerCase().startsWith('n')) {
      this.out.exit(0)
    }
  }
}
