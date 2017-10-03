import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  Project,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import * as inquirer from 'inquirer'
import {repeat} from 'lodash'

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
    force: flags.boolean({
      char: 'f',
      description: 'Force delete, without confirmation',
    }),
  }
  async run() {
    await this.auth.ensureAuth()
    const { project, env, force } = this.flags

    let projectId = project

    if (env) {
      const projectEnv = await this.env.getEnvironment({ env })
      if (!projectEnv.projectId) {
        this.out.error(new EnvDoesntExistError(env))
      }
      projectId = projectEnv.projectId
    }

    if (projectId) {
      if (!force) {
        await this.askForConfirmation(projectId)
      }
      this.out.action.start(`${chalk.bold.red('Deleting project')} ${projectId}`)
      await this.client.deleteProjects([projectId])
      this.env.deleteIfExist([projectId])
      this.env.save()
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
        })).concat(new inquirer.Separator(chalk.bold.green(repeat('-', 50)))),
        pageSize: Math.min(process.stdout.rows!, projects.length) - 2,
      }

      const {projectsToDelete}: {projectsToDelete: Project[]} = await this.out.prompt(question)
      const projectIdsToDelete = projectsToDelete.map(p => p.id)

      if (projectsToDelete.length === 0) {
        this.out.log(`You didn't select any project to delete, so none will be deleted`)
        this.out.exit(0)
      }
      const prettyProjects = projectsToDelete.map(prettyProject).join(', ')

      if (!force) {
        await this.askForConfirmation(prettyProjects)
      }

      this.out.log('')
      this.out.action.start(`${chalk.red.bold(`Deleting project${projectsToDelete.length > 1 ? 's': ''}`)} ${prettyProjects}`)
      await this.client.deleteProjects(projectIdsToDelete)
      this.env.deleteIfExist(projectIdsToDelete)
      this.env.save()
      this.out.action.stop()
    }
  }

  private async askForConfirmation(projects: string) {
    const confirmationQuestion = {
      name: 'confirmation',
      type: 'input',
      message: `Are you sure that you want to delete ${projects}? y/N`,
      default: 'n'
    }
    const {confirmation}: {confirmation: string} = await this.out.prompt(confirmationQuestion)
    if (confirmation.toLowerCase().startsWith('y')) {
      this.out.exit(0)
    }
  }
}

const prettyProject = p => `${chalk.bold(p.name)} (${p.id})`
