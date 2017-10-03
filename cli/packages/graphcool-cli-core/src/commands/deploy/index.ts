import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  EnvironmentConfig,
  ProjectDefinition,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import * as sillyName from 'sillyname'
import { ProjectDoesntExistError } from '../../errors/ProjectDoesntExistError'
import { emptyDefinition } from './emptyDefinition'

export default class Deploy extends Command {
  static topic = 'deploy'
  static description = 'Deploy project definition changes'
  static help = `
  
  ${chalk.green.bold('Examples:')}
      
${chalk.gray(
    '-',
  )} Deploy local changes from graphcool.yml to the default project environment.
  ${chalk.green('$ graphcool deploy')}

${chalk.gray('-')} Deploy local changes to a specific environment
  ${chalk.green('$ graphcool deploy --env production')}
    
${chalk.gray(
    '-',
  )} Deploy local changes from default project file accepting potential data loss caused by schema changes
  ${chalk.green('$ graphcool deploy --force --env production')}
  `
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Project environment to be deployed',
    }),
    project: flags.string({
      char: 'p',
      description: 'ID or alias of  project to deploy',
    }),
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes',
    }),
    name: flags.string({
      char: 'n',
      description: 'Project name',
    }),
    alias: flags.string({
      char: 'a',
      description: 'Project alias',
    }),
  }
  async run() {
    const { project, force } = this.flags

    let {env} = this.flags
    env = env || this.env.env.default


    await this.auth.ensureAuth()
    await this.definition.load()
    // temporary ugly solution
    this.definition.injectEnvironment()

    const envResult = await this.env.getEnvironment({
      project,
      env,
    })

    let {projectId, envName} = envResult

    let projectName = projectId
    let projectIsNew = false

    if (!projectId) {
      // if a specific project has been provided, check for its existence
      if (project) {
        this.out.error(new ProjectDoesntExistError(project))
      }

      // otherwise create a new project
      const newProject = await this.createProject()
      projectId = newProject.projectId
      envName = newProject.envName
      projectIsNew = true
    }

    this.out.log('')
    const localNote = (this.env.env && this.env.isDockerEnv(this.env.env.environments[env])) ? ' locally' : ''
    this.out.action.start(
      projectIsNew ? `Deploying${localNote}` :
      `Deploying to project ${chalk.bold(
        projectName,
      )} with environment ${chalk.bold(envName)}${localNote}.`,
    )

    try {
      const migrationResult = await this.client.push(
        projectId,
        force,
        false,
        this.definition.definition!,
      )
      this.out.action.stop()

      // no action required
      if (
        (!migrationResult.migrationMessages ||
          migrationResult.migrationMessages.length === 0) &&
        (!migrationResult.errors || migrationResult.errors.length === 0)
      ) {
        this.out.log(
          `Identical project definition for project ${chalk.bold(
            projectId,
          )} in env ${chalk.bold(envName)}, no action required.\n`,
        )
        return
      }

      if (migrationResult.migrationMessages.length > 0) {
        if (projectIsNew) {
          this.out.log('\nSuccess! Created the following project:\n')
        } else {
          const updateText =
            migrationResult.errors.length > 0
              ? ` has changes:`
              : ` was successfully updated.\nHere are the changes:`
          this.out.log(
            chalk.blue(
              `\nYour project ${chalk.bold(projectId)} of env ${chalk.bold(
                envName,
              )}${updateText}\n`,
            ),
          )
        }

        this.out.migration.printMessages(migrationResult.migrationMessages)
        this.definition.set(migrationResult.projectDefinition)
      }

      if (migrationResult.errors.length > 0) {
        this.out.log(
          chalk.rgb(244, 157, 65)(
            `\nThere are issues with the new project definition:`,
          ),
        )
        this.out.migration.printErrors(migrationResult.errors)
        this.out.log('')
      }

      if (
        migrationResult.errors &&
        migrationResult.errors.length > 0 &&
        migrationResult.errors[0].description.includes(`destructive changes`)
      ) {
        // potentially destructive changes
        this.out.log(
          `Your changes might result in data loss.
          Use ${chalk.cyan(
            `\`graphcool deploy --force\``,
          )} if you know what you're doing!\n`,
        )
      }
    } catch (e) {
      this.out.action.stop()
      this.out.error(e)
    }

  }

  private async createProject(): Promise<{envName: string, projectId: string}> {
    const {alias, region} = this.flags
    let {name, env} = this.flags

    env = env || this.getEnvName()
    name = name || sillyName()

    this.out.log('')
    const localNote = (this.env.env && this.env.isDockerEnv(this.env.env.environments[env])) ? ' locally' : ''
    const projectMessage = `Creating project ${chalk.bold(name)}${localNote}`
    this.out.action.start(projectMessage)

    // create project
    const createdProject = await this.client.createProject(
      name,
      emptyDefinition,
      alias,
      region,
    )

    // add environment
    await this.env.set(env, createdProject.id)

    if (!this.env.default) {
      this.env.setDefault(env)
    }

    this.env.save()

    this.out.action.stop()

    return {
      projectId: createdProject.id,
      envName: env,
    }
  }

  private getEnvName() {
    if (this.env.default && this.env.isDockerEnv(this.env.default)) {
      return this.env.env.default
    }

    return 'dev'
  }
}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
