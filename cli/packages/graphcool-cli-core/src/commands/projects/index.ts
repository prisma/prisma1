import {Command, flags, Flags} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import {table, getBorderCharacters} from 'table'

export default class Projects extends Command {
  static topic = 'projects'
  static description = 'List all projects'
  async run() {
    await this.auth.ensureAuth()
    const projects = await this.client.fetchProjects()

    const currentProjectId = this.env.default ? this.env.default : null

    const data = projects.map(project => {
      const isCurrentProject = currentProjectId !== null && (currentProjectId === project.id || currentProjectId === project.alias)
      const envName = Object.keys(this.env.env.environments).find(key => this.env.env.environments[key] === project.id) || ''
      return [isCurrentProject ? '*' : ' ', `${project.alias || project.id}   `, project.name, project.region, envName]
    })

    const tableHeader = [
      ['', chalk.bold('Project ID'), chalk.bold('Project Name'), chalk.bold('Region'), chalk.bold('Environment')],
    ]

    const output = table(tableHeader.concat(data), {
      border: getBorderCharacters('void'),
      columnDefault: {
        paddingLeft: '0',
        paddingRight: '2',
      },
      drawHorizontalLine: () => false,
    }).trimRight()

    this.out.log(output)
  }
}
