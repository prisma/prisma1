import { Command } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'

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
    for (const cluster of this.env.clusters) {
      this.env.setActiveCluster(cluster)
      const clusterProjects = await this.client.listProjects()
      const mappedClusterProjects = clusterProjects.map(p => ({
        ...p,
        cluster: cluster.name,
      }))
      projects = [...projects, ...mappedClusterProjects]
    }

    this.printProjects(projects)
  }

  printProjects(projects: Project[]) {
    this.out.table(projects)
  }
}
