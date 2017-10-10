import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import * as sillyName from 'sillyname'
import { ProjectDoesntExistError } from '../../errors/ServiceDoesntExistError'
import { emptyDefinition } from './emptyDefinition'
import * as chokidar from 'chokidar'
import { Target } from '../../../../graphcool-cli-engine/src/types/rc'

export default class Deploy extends Command {
  private deploying: boolean = false
  static topic = 'deploy'
  static description = 'Deploy project definition changes'
  static help = `
  
  ${chalk.green.bold('Examples:')}
      
${chalk.gray(
    '-',
  )} Deploy local changes from graphcool.yml to the default service environment.
  ${chalk.green('$ graphcool deploy')}

${chalk.gray('-')} Deploy local changes to a specific environment
  ${chalk.green('$ graphcool deploy --env production')}
    
${chalk.gray(
    '-',
  )} Deploy local changes from default service file accepting potential data loss caused by schema changes
  ${chalk.green('$ graphcool deploy --force --env production')}
  `
  static flags: Flags = {
    target: flags.string({
      char: 't',
      description: 'Local target, ID or alias of service to deploy',
    }),
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes',
    }),
    watch: flags.boolean({
      char: 'w',
      description: 'Watch for changes',
    }),
    'new-service': flags.string({
      char: 'n',
      description: 'Name of the new Service',
    }),
    'new-service-cluster': flags.string({
      char: 'c',
      description: 'Name of the Cluster to deploy to',
    }),
    alias: flags.string({
      char: 'a',
      description: 'Service alias',
    }),
  }
  async run() {
    const { force, watch, alias } = this.flags
    const newServiceName = this.flags['new-service']
    const newServiceCluster = this.flags['new-service-cluster']
    // target can be both key or value of the `targets` object in the .graphcoolrc
    // so either "my-target" or "shared-eu-west-1/asdf"
    let targetName
    let target
    let cluster
    if (newServiceName) {
      if (newServiceCluster) {
        this.env.setActiveCluster(newServiceCluster)
      }
      cluster = this.env.activeCluster
      targetName = this.flags.target || this.env.getDefaultTargetName(cluster)
    } else {
      const foundTarget = await this.env.getTargetWithName(this.flags.target)
      if (foundTarget) {
        targetName = foundTarget.targetName
        target = foundTarget.target
      }
    }

    await this.auth.ensureAuth()
    await this.definition.load(this.flags)
    // temporary ugly solution
    this.definition.injectEnvironment()

    let projectId
    let projectIsNew = false


    cluster = cluster ? cluster : (target ? target.cluster : this.env.activeCluster)
    const isLocal = !this.env.isSharedCluster(cluster)

    if (!target) {
      // if a specific project has been provided, check for its existence
      if (target) {
        this.out.error(new ProjectDoesntExistError(target))
      }

      const region = this.env.getRegionFromCluster(cluster)

      // otherwise create a new project
      const newProject = await this.createProject(isLocal, newServiceName, alias, region)
      projectId = newProject.projectId
      projectIsNew = true

      // add environment
      await this.env.setLocalTarget(targetName, `${cluster}/${projectId}`)

      if (!this.env.default) {
        this.env.setLocalDefaultTarget(targetName)
      }

      this.env.saveLocalRC()
    }

    // best guess for "project name"
    const projectName = newServiceName || targetName

    await this.deploy(projectIsNew, targetName, projectId, isLocal, force, projectName)

    if (watch) {
      this.out.log('Watching for change...')
      chokidar.watch(this.config.definitionDir, {ignoreInitial: true}).on('all', () => {
        setImmediate(async () => {
          if (!this.deploying) {
            await this.definition.load(this.flags)
            await this.deploy(projectIsNew, targetName, projectId!, isLocal, force, projectName)
            this.out.log('Watching for change...')
          }
        })
      })
    }
  }

  private async createProject(isLocal: boolean, name?: string, alias?: string, region?: string): Promise<{
    projectId: string
  }> {

    const localNote =
      isLocal
        ? ' locally'
        : ''

    const projectName = name || sillyName()

    this.out.log('')
    const projectMessage = `Creating project ${chalk.bold(name)}${localNote}`
    this.out.action.start(projectMessage)

    // create project
    const createdProject = await this.client.createProject(
      projectName,
      emptyDefinition,
      alias,
      region,
    )

    this.out.action.stop()

    return {
      projectId: createdProject.id,
    }
  }

  private async deploy(
    projectIsNew: boolean,
    targetName: string,
    projectId: string,
    isLocal: boolean,
    force: boolean,
    projectName: string | null,
  ): Promise<void> {
    this.deploying = true
    const localNote =
        isLocal
        ? ' locally'
        : ''
    this.out.action.start(
      projectIsNew
        ? `Deploying${localNote}`
        : `Deploying to ${chalk.bold(
            projectName,
          )} with env ${chalk.bold(targetName)}${localNote}`,
    )

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
        `Everything up-to-date.`,
      )
      this.deploying = false
      return
    }

    if (migrationResult.migrationMessages.length > 0) {
      if (projectIsNew) {
        this.out.log('\nSuccess! Created the following project:\n')
      } else {
        const updateText =
          migrationResult.errors.length > 0
            ? `${chalk.red('Error!')} Here are the potential changes:`
            : `${chalk.green('Success!')} Here is what changed:`
        this.out.log(updateText)
      }

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
    this.deploying = false
  }
}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
