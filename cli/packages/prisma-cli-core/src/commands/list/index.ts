import { Command } from 'prisma-cli-engine'
import { table, getBorderCharacters } from 'table'
const debug = require('debug')('command')
import chalk from 'chalk'

export interface Project {
  name: string
  stage: string
  cluster: string
}

export default class List extends Command {
  static topic = 'list'
  static description = 'List all deployed services'
  static group = 'general'
  static aliases = ['ls']
  async run() {
    let projects: Project[] = []

    for (const cluster of this.env.clusters.filter(c => c.local)) {
      try {
        await this.client.initClusterClient(
          cluster,
          this.definition.getWorkspace() || '*',
          '*',
          '*',
        )
        this.env.setActiveCluster(cluster)
        debug('listing projects')
        const clusterProjects = await this.client.listProjects()
        const mappedClusterProjects = clusterProjects.map(p => ({
          ...p,
          cluster: cluster.name,
        }))
        projects = [...projects, ...mappedClusterProjects]
      } catch (e) {
        debug(e)
      }
    }

    let gotCloud = false

    try {
      if (this.env.cloudSessionKey) {
        const services = await this.client.getCloudServices()
        const mappedServices = services
          .filter(s => s.cluster)
          .map(s => ({
            name: s.name,
            stage: s.stage,
            cluster: s.cluster.name,
          }))

        projects = [...projects, ...mappedServices]
        gotCloud = true
      }
    } catch (e) {
      //
    }

    this.printProjects(projects, gotCloud)
  }

  printProjects(projects: Project[], gotCloud: boolean) {
    if (projects.length === 0) {
      this.out.log('No deployed service found')
    } else {
      const mapped = projects.map(p => ({
        'Service Name': p.name,
        Stage: p.stage,
        Server: p.cluster,
      }))
      this.out.table(mapped)
    }

    if (!gotCloud) {
      this.out.log('')
      this.out.warn(`This does not include your services deployed in the cloud.
In order to see them, please run ${chalk.bold.green('prisma login')}`)
    }
  }
}
