import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  Project,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'

export default class Delete extends Command {
  static topic = 'delete'
  static description = 'example command'
  static hidden = true
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name to delete',
    }),
    project: flags.string({
      char: 'p',
      description: 'Project Id to delete',
    }),
  }
  async run() {
    const { project, env } = this.flags

    let projectId = project

    if (env) {
      const projectEnv = await this.env.getEnvironment({ env })
      if (!projectEnv.projectId) {
        this.out.error(new EnvDoesntExistError(env))
      }
      projectId = projectEnv.projectId
    }

    if (projectId) {
      this.out.action.start(`Deleting project ${projectId}`)
      await this.client.deleteProjects([projectId])
      this.env.deleteIfExist([projectId])
      env.save()
      this.out.action.stop()
    } else {
      const projects = await this.client.fetchProjects()

      const question = {
        name: 'projectsToDelete',
        type: 'checkbox',
        message: 'Select projects to delete',
        choices: projects.map(p => ({
          name: prettyProject(p),
          value: p,
        })),
        pageSize: Math.min(process.stdout.rows!, projects.length) - 2,
      }

      const {projectsToDelete}: {projectsToDelete: Project[]} = await this.out.prompt(question)

      if (projectsToDelete.length === 0) {
        this.out.log(`You didn't select any project to delete, so none will be deleted`)
        this.out.exit(0)
      }

      const prettyProjects = projectsToDelete.map(prettyProject).join(', ')

      this.out.log('')
      this.out.action.start(`${chalk.red.bold(`Deleting project${projectsToDelete.length > 1 ? 's': ''}`)} ${prettyProjects}`)
      await this.client.deleteProjects(projectsToDelete.map(p => p.id))
      this.out.action.stop()
    }
  }
}

const prettyProject = p => `${chalk.bold(p.name)} (${p.id})`
