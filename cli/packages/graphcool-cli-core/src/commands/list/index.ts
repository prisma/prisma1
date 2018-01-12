import { Command } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'
const debug = require('debug')('command')

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

    await this.definition.load(this.flags)

    for (const cluster of this.env.clusters.filter(c => c.local)) {
      await this.client.initClusterClient(
        cluster,
        this.definition.getWorkspace() || '*',
        '*',
        '*',
      )
      try {
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

    const services = await this.client.getCloudServices()
    const mappedServices = services.map(s => ({
      name: s.name,
      stage: s.stage,
      cluster: s.cluster.name,
    }))

    projects = [...projects, ...mappedServices]

    this.printProjects(projects)
  }

  printProjects(projects: Project[]) {
    if (projects.length === 0) {
      this.out.log('No deployed service found')
    } else {
      const mapped = projects.map(p => ({
        'Service Name': p.name,
        Stage: p.stage,
        Cluster: p.cluster,
      }))
      this.out.table(mapped)
    }
  }
}
