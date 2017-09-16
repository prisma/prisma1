import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  EnvironmentConfig,
  ProjectDefinition,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import * as figures from 'figures'
import { ProjectDoesntExistError } from '../../errors/ProjectDoesntExistError'

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
  }
  static mockDefinition: ProjectDefinition
  static mockEnv: EnvironmentConfig
  async run() {
    const { env, project, force } = this.flags

    if (Deploy.mockDefinition) {
      this.definition.set(Deploy.mockDefinition)
    }
    if (Deploy.mockEnv) {
      this.env.env = Deploy.mockEnv
    }
    await this.auth.ensureAuth()
    await this.definition.load()
    // temoprary ugly solution
    this.definition.injectEnvironment()

    const { projectId, envName } = await this.env.getEnvironment({
      project,
      env,
    })

    if (!projectId) {
      if (project) {
        this.out.error(new ProjectDoesntExistError(project))
      }

      if (env) {
        this.out.error(new EnvDoesntExistError(env))
      }

      this.out.error(
        `Please provide either a default environment, a project or an environment you want to deploy to.`,
      )
    } else {
      this.out.action.start(
        `Deploying to project ${chalk.bold(
          projectId,
        )} with local environment ${chalk.bold(envName)}.`,
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
  }
}
