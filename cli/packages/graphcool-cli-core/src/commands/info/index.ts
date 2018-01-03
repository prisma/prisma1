import {
  Command,
  Flags,
  flags,
  ProjectInfo,
  Output,
  Project,
} from 'graphcool-cli-engine'
import chalk from 'chalk'
import { Cluster } from 'graphcool-yml'

export default class InfoCommand extends Command {
  static topic = 'info'
  static description = 'Display service information (endpoints, cluster, ...)'
  static group = 'general'
  async run() {
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    let services: any[] = []

    for (const cluster of this.env.clusters) {
      this.env.setActiveCluster(cluster)
      const projects = await this.client.listProjects()
      const filteredProjects = projects.filter(p => p.name === serviceName)
      services = services.concat(
        filteredProjects.map(project => ({
          project,
          cluster,
        })),
      )
    }

    this.out.log(`\
Service Name: ${chalk.bold(serviceName)}

Stages:
${services
      .map(s => this.printStage(serviceName, s.project.stage, s.cluster))
      .join('\n\n')}
`)
  }

  printStage(name: string, stage: string, cluster: Cluster) {
    return `
  ${chalk.bold(stage)} (cluster: ${chalk.bold(`\`${cluster.name}\``)})

    HTTP:       ${cluster.getApiEndpoint(name, stage)}
    Websocket:  ${cluster.getWSEndpoint(name, stage)}`
  }
}
