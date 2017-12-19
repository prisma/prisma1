import {
  Command,
  Flags,
  flags,
  ProjectInfo,
  Output,
  Project,
} from 'graphcool-cli-engine'
import chalk from 'chalk'

export default class InfoCommand extends Command {
  static topic = 'info'
  static description = 'Display service information (endpoints, cluster, ...)'
  static group = 'general'
  static flags: Flags = {
    stage: flags.string({
      char: 's',
      description: 'Stage name to get the info for',
      defaultValue: 'dev',
    }),
  }
  async run() {
    const { stage } = this.flags

    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const cluster = await this.client.getClusterSafe(serviceName, stage)

    this.out.log(`\
Service Name: ${chalk.bold(serviceName)}

Stage: ${chalk.bold(stage)} (deployed in ${chalk.bold(cluster.name)})

Endpoints:
HTTP        ${cluster!.getApiEndpoint(serviceName, stage)}
`)
  }
}
