import {
  Command,
  flags,
  Flags,
  EnvDoesntExistError,
  EnvironmentConfig,
  ProjectDefinition,
} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { ProjectDoesntExistError } from '../../errors/ProjectDoesntExistError'

export default class Diff extends Command {
  static topic = 'diff'
  static description = 'Get the diff of the local and remote project definition'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Project environment to be deployed',
    }),
    project: flags.string({
      char: 'p',
      description: 'ID or alias of  project to deploy',
    }),
  }
  static mockDefinition: ProjectDefinition
  static mockEnv: EnvironmentConfig

  async run() {
    const { env, project, force } = this.flags

    if (Diff.mockDefinition) {
      this.definition.set(Diff.mockDefinition)
    }
    if (Diff.mockEnv) {
      this.env.env = Diff.mockEnv
    }
    await this.definition.load()
    await this.auth.ensureAuth()
    // temporary ugly solution
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
        `Getting diff for ${chalk.bold(
          projectId,
        )} with env${chalk.bold(envName)}.`,
      )

      try {
        const migrationResult = await this.client.push(
          projectId,
          false,
          true,
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
          this.out.log(
            chalk.blue(
              `Your project ${chalk.bold(projectId)} of env ${chalk.bold(
                envName,
              )} has the following changes:`,
            ),
          )

          this.out.migration.printMessages(migrationResult.migrationMessages)
          this.definition.set(migrationResult.projectDefinition)
        }

        if (migrationResult.errors.length > 0) {
          this.out.log(
            chalk.rgb(244, 157, 65)(
              `There are issues with the new project definition:`,
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
