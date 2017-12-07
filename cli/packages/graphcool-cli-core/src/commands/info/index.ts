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
    }),
  }
  async run() {
    const { stage } = this.flags

    await this.definition.load(this.env, this.flags)
    const clusterName = this.definition.getStage(stage, true)
    const stageName = stage || this.definition.rawStages.default
    const cluster = this.env.clusterByName(clusterName!, true)!
    const serviceName = this.definition.definition!.service

    this.out.log(`\
Service Name: ${chalk.bold(serviceName)}

Stage: ${chalk.bold(stageName)} (deployed in ${chalk.bold(cluster.name)})

Endpoints:
HTTP        ${cluster.getApiEndpoint(serviceName, stageName)}
`)
  }
}
