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

export interface Service {
  project: {
    name: string
    stage: string
  }
  cluster: Cluster
}

export default class InfoCommand extends Command {
  static topic = 'info'
  static description = 'Display service information (endpoints, cluster, ...)'
  static group = 'general'
  static flags: Flags = {
    json: flags.boolean({
      char: 'j',
      description: 'Json Output',
    }),
    current: flags.boolean({
      char: 'c',
      description: 'Only show info for current service',
    }),
  }
  async run() {
    const { json } = this.flags
    await this.definition.load(this.flags)
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage
    const workspace = this.definition.getWorkspace()

    // if (current) {
    const clusterName = this.definition.getClusterName()
    if (!clusterName) {
      throw new Error(
        `No cluster set. Please set the "cluster" property in your graphcool.yml`,
      )
    }
    const cluster = this.definition.getCluster()
    if (!cluster) {
      throw new Error(`Cluster ${clusterName} could not be found in global ~/.graphcoolrc.
Please make sure it contains the cluster. You can create a local cluster using 'gc local start'`)
    }
    if (!json) {
      this.out.log(`Service Name: ${chalk.bold(serviceName)}`)
    }
    this.out.log(
      this.printStage(
        serviceName,
        stage,
        cluster,
        workspace || undefined,
        json,
      ),
    )
    // } else {
    //   let services: Service[] = []

    //   for (const cluster of this.env.clusters) {
    //     this.env.setActiveCluster(cluster)
    //     await this.client.initClusterClient(
    //       cluster,
    //       workspace!,
    //       serviceName,
    //       stage,
    //     )
    //     const projects = await this.client.listProjects()
    //     const filteredProjects = projects.filter(p => p.name === serviceName)
    //     services = services.concat(
    //       filteredProjects.map(project => ({
    //         project,
    //         cluster,
    //       })),
    //     )
    //   }

    //   this.out.log(this.printStages(serviceName, services, json))
    // }
  }

  printStage(
    name: string,
    stage: string,
    cluster: Cluster,
    workspace?: string,
    printJson: boolean = false,
  ) {
    if (printJson) {
      return JSON.stringify(
        {
          name,
          stage,
          cluster: cluster.name,
          workspace,
          httpEndpoint: cluster.getApiEndpoint(name, stage, workspace),
          wsEndpoint: cluster.getWSEndpoint(name, stage, workspace),
        },
        null,
        2,
      )
    }
    return `
  ${chalk.bold(stage)} (cluster: ${chalk.bold(`\`${cluster.name}\``)})

    HTTP:       ${cluster.getApiEndpoint(name, stage, workspace)}
    Websocket:  ${cluster.getWSEndpoint(name, stage, workspace)}`
  }

  printStages(
    serviceName: string,
    services: Service[],
    printJson: boolean = false,
  ) {
    if (!printJson) {
      this.out.log(`\
Service Name: ${chalk.bold(serviceName)}

Stages:
${services
        .map(s =>
          this.printStage(
            s.project.name,
            s.project.stage,
            s.cluster,
            undefined,
          ),
        )
        .join('\n\n')}
  `)
    } else {
      return JSON.stringify(
        services.map(s =>
          JSON.parse(
            this.printStage(
              s.project.name,
              s.project.stage,
              s.cluster,
              undefined,
              true,
            ),
          ),
        ),
        null,
        2,
      )
    }
  }
}
