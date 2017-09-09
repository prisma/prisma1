import {Command, flags, Flags, EnvDoesntExistError} from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import * as figures from 'figures'
import {ProjectDoesntExistError} from '../errors/ProjectDoesntExistError'

export default class Deploy extends Command {
  static topic = 'deploy'
  static description = 'Deploy project definition changes'
  static help = `
  
  ${chalk.blue('Examples:')}
      
${chalk.gray('-')} Deploy local changes from graphcool.yml to the default project environment.
  ${chalk.blue('$ graphcool deploy')}

${chalk.gray('-')} Deploy local changes to a specific environment
  ${chalk.blue('$ graphcool deploy --env production')}
    
${chalk.gray('-')} Deploy local changes from default project file accepting potential data loss caused by schema changes
  ${chalk.blue('$ graphcool deploy --force --env production')}
  `
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Project environment to be deployed'
    }),
    project: flags.string({
      char: 'p',
      description: 'ID or alias of  project to deploy'
    }),
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes'
    }),
  }
  async run() {
    const {env, project, force} = this.flags

    await this.definition.load()
    await this.auth.ensureAuth()

    const {projectId, envName} = await this.env.getEnvironment({project, env})

    if (!projectId) {
      if (project) {
        this.out.error(new ProjectDoesntExistError(project))
      }

      if (env) {
        this.out.error(new EnvDoesntExistError(env))
      }

      this.out.error(`Please provide either a default environment, a project or an environment you want to deploy to.`)
    } else {
      this.out.action.start(`Deploying to project ${chalk.bold(projectId)} with local environment ${chalk.bold(envName)}.`)

      try {

        const migrationResult  = await this.client.push(projectId, force, true, this.definition.definition!)
        this.out.action.stop()

        // no action required
        if ((!migrationResult.migrationMessages || migrationResult.migrationMessages.length === 0) && (!migrationResult.errors || migrationResult.errors.length === 0)) {
          this.out.log(`${chalk.green(figures.tick)} Identical project definition for project ${chalk.bold(projectId)} in env ${chalk.bold(envName)}, no action required.`)
          return
        } else if (migrationResult.migrationMessages.length > 0 && migrationResult.errors.length === 0) {
          this.out.log(
            `${chalk.green(figures.tick)} Your project ${chalk.bold(projectId)} of env ${chalk.bold(envName)} was successfully updated.
            Here are the changes: \n`)

            this.out.migration.printMessages(migrationResult.migrationMessages)
            this.definition.set(migrationResult.projectDefinition)
            await this.definition.saveTypes()
        }

        // can't do migration because of issues with schema
        else if (migrationResult.migrationMessages.length === 0 && migrationResult.errors.length > 0) {
          this.out.log(`There are issues with the new project definition:\n`)
          this.out.migration.printErrors(migrationResult.errors)
          this.out.log(`\n`)
        }

        // potentially destructive changes
        else if (migrationResult.errors[0].description.includes(`destructive changes`)) {
          this.out.log(
            `Your changes might result in data loss.
            Review your changes with ${chalk.cyan(`\`graphcool status\``)} or use ${chalk.cyan(`\`graphcool deploy --force\``)} if you know what you're doing!`
          )
        }
      } catch (e) {
        this.out.action.stop()
        this.out.error(e)
      }
    }
  }
}
