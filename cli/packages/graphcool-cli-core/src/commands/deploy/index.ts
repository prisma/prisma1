import { Command, flags, Flags } from 'graphcool-cli-engine'
import chalk from 'chalk'
import * as sillyName from 'sillyname'
import { ServiceDoesntExistError } from '../../errors/ServiceDoesntExistError'
import { emptyDefinition } from './emptyDefinition'
import * as chokidar from 'chokidar'
import * as inquirer from 'graphcool-inquirer'
import * as path from 'path'
import * as fs from 'fs-extra'
import Bundler from './Bundler/Bundler'
const debug = require('debug')('deploy')

export default class Deploy extends Command {
  private deploying: boolean = false
  static topic = 'deploy'
  static description = 'Deploy service changes (or new service)'
  static group = 'general'
  static allowAnyFlags = true
  static help = `
  
  ${chalk.green.bold('Examples:')}
      
${chalk.gray(
    '-',
  )} Deploy local changes from graphcool.yml to the default service environment.
  ${chalk.green('$ graphcool deploy')}

${chalk.gray('-')} Deploy local changes to a specific target
  ${chalk.green('$ graphcool deploy --target production')}
    
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
    interactive: flags.boolean({
      char: 'i',
      description: 'Force interactive mode to select the cluster'
    }),
    default: flags.boolean({
      char: 'd',
      description: 'Set specified target as default'
    }),
    'dry-run': flags.boolean({
      char: 'D',
      description: 'Perform a dry-run of the deployment'
    }),
    json: flags.boolean({
      char: 'j',
      description: 'Json Output'
    })
  }
  async run() {
    debug('run')
    const { force, watch, alias, interactive } = this.flags
    const useDefault = this.flags.default
    let newServiceName = this.flags['new-service']
    const newServiceCluster = this.flags['new-service-cluster']
    const dryRun = this.flags['dry-run']

    if (dryRun) {
      return this.dryRun()
    }

    if (newServiceCluster) {
      this.env.setActiveCluster(newServiceCluster)
    }
    // target can be both key or value of the `targets` object in the .graphcoolrc
    // so either "my-target" or "shared-eu-west-1/asdf"
    let showedDialog = false
    let targetName
    let target
    let cluster
    const foundTarget = await this.env.getTargetWithName(process.env.GRAPHCOOL_TARGET || this.flags.target)
    // load the definition already so we're able to detect missing package.json / node_modules
    // if it is a existing project,

    if (interactive) {
      foundTarget.targetName = null
      foundTarget.target = null
    }

    this.definition.checkNodeModules(Boolean(foundTarget.target))

    if (interactive || (!newServiceCluster && !foundTarget.target) || (newServiceName && !newServiceCluster)) {
      cluster = await this.clusterSelection()
      showedDialog = true
      this.env.setActiveCluster(cluster)
      this.env.saveLocalRC()
      if (cluster === 'local' && (!this.env.rc.clusters || !this.env.rc.clusters!.local)) {
        this.out.log(`You chose the cluster ${chalk.bold('local')}, but don't have docker initialized, yet.
Please run ${chalk.green('$ graphcool local up')} to get a local Graphcool cluster.
`)
        this.out.exit(1)
      }
    }

    if (newServiceName || interactive || (!foundTarget.targetName && !foundTarget.target)) {
      targetName = this.flags.target
      if (!targetName) {
        targetName = await this.targetNameSelector(this.env.getDefaultTargetName(cluster))
        showedDialog = true
      }
    }

    if (!targetName && foundTarget.targetName) {
      targetName = foundTarget.targetName
    }

    if (!target && foundTarget.target) {
      target = foundTarget.target
    }

    if ((!newServiceName && !foundTarget.target) || interactive) {
      newServiceName = await this.serviceNameSelector(path.basename(this.config.definitionDir))
      showedDialog = true
    }

    if (showedDialog) {
      this.out.up(3)
    }

    await this.auth.ensureAuth()
    await this.definition.load({
      ...this.flags,
      target: targetName,
      cluster,
    })

    let projectId
    let projectIsNew = false

    cluster = cluster ? cluster : (target ? target.cluster : this.env.activeCluster)
    const isLocal = !this.env.isSharedCluster(cluster)

    if (!target) {
      // if a specific service has been provided, check for its existence
      if (target) {
        this.out.error(new ServiceDoesntExistError(target))
      }

      const region = this.env.getRegionFromCluster(cluster)

      // otherwise create a new project
      const newProject = await this.createProject(isLocal, cluster, newServiceName, alias, region)
      projectId = newProject.projectId
      projectIsNew = true

      // add environment
      await this.env.setLocalTarget(targetName, `${cluster}/${projectId}`)

      if (!this.env.default || useDefault) {
        this.env.setLocalDefaultTarget(targetName)
      }

      this.env.saveLocalRC()
    } else {
      projectId = target.id
    }

    // best guess for "project name"
    const projectName = newServiceName || targetName

    const info = await this.client.fetchProjectInfo(projectId)

    if (!info.isEjected) {
      this.out.error(`Your service ${info.name} (${info.id}) is not yet upgraded.
Please go to the console and upgrade it:
https://console.graph.cool/${encodeURIComponent(info.name)}/settings/general`)
    }

    await this.deploy(projectIsNew, targetName, projectId, isLocal, force, projectName, cluster)

    if (watch) {
      this.out.log('Watching for change...')
      chokidar.watch(this.config.definitionDir, {ignoreInitial: true}).on('all', () => {
        setImmediate(async () => {
          if (!this.deploying) {
            await this.definition.load(this.flags)
            await this.deploy(projectIsNew, targetName, projectId!, isLocal, force, projectName, cluster)
            this.out.log('Watching for change...')
          }
        })
      })
    }
  }

  private async createProject(isLocal: boolean, cluster: string, name: string, alias?: string, region?: string): Promise<{
    projectId: string
  }> {

    const localNote =
      isLocal
        ? ' locally'
        : ''

    this.out.log('')
    const projectMessage = `Creating service ${chalk.bold(name)}${localNote} in cluster ${cluster}`
    this.out.action.start(projectMessage)

    // create project
    const createdProject = await this.client.createProject(
      name,
      emptyDefinition,
      alias,
      region,
    )

    this.out.action.stop()

    return {
      projectId: createdProject.id,
    }
  }

  private prettyTime(time: number): string {
    let output = ''
    if (time > 1000) {
      output = (Math.round(time / 100) / 10).toFixed(1) + 's'
    } else {
      output = time + 'ms'
    }
    return chalk.cyan(output)
  }

  private async deploy(
    projectIsNew: boolean,
    targetName: string,
    projectId: string,
    isLocal: boolean,
    force: boolean,
    projectName: string | null,
    cluster: string
  ): Promise<void> {
    // bundle and add externalFiles
    debug('bundling')
    let before = Date.now()
    if (this.definition.definition!.modules[0].definition!.functions) {
      const bundler = new Bundler(this, projectId)
      const externalFiles = await bundler.bundle()
      bundler.cleanBuild()
      this.definition.definition!.modules[0].externalFiles = externalFiles
      Object.keys(externalFiles).forEach(key => delete this.definition.definition!.modules[0].files[key])
    }
    this.out.action.stop(this.prettyTime(Date.now() - before))
    debug('bundled')

    this.deploying = true
    const localNote =
        isLocal
        ? ' locally'
        : ''
    before = Date.now()
    this.out.action.start(
      projectIsNew
        ? `Deploying${localNote}`
        : `Deploying to ${chalk.bold(
            cluster,
          )} with target ${chalk.bold(targetName || `${cluster}/${projectId}`)}${localNote}`,
    )

    const migrationResult = await this.client.push(
      projectId,
      force,
      false,
      this.definition.definition!,
    )
    this.out.action.stop(this.prettyTime(Date.now() - before))

    // no action required
    if (
      (!migrationResult.migrationMessages ||
        migrationResult.migrationMessages.length === 0) &&
      (!migrationResult.errors || migrationResult.errors.length === 0)
    ) {
      this.out.log(
        `Everything up-to-date.`,
      )
      this.printEndpoints(projectId)
      this.deploying = false
      return
    }

    if (migrationResult.migrationMessages.length > 0) {
      if (projectIsNew) {
        this.out.log('\nSuccess! Created the following service:')
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
          `There are issues with the new service definition:`,
        ),
      )
      this.out.migration.printErrors(migrationResult.errors)
      this.out.log('')
      process.exitCode = 1
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
      process.exitCode = 1
    }
    this.deploying = false
    this.printEndpoints(projectId)
  }

  private printEndpoints(projectId) {
    this.out.log(`Here are your GraphQL Endpoints:

  ${chalk.bold('Simple API:')}        ${this.env.simpleEndpoint(projectId)}
  ${chalk.bold('Relay API:')}         ${this.env.relayEndpoint(projectId)}
  ${chalk.bold('Subscriptions API:')} ${this.env.subscriptionEndpoint(projectId)}`)
  }

  private async clusterSelection(): Promise<string> {
    const question = {
      name: 'cluster',
      type: 'list',
      message: 'Please choose the cluster you want to deploy to',
      choices: [
        new inquirer.Separator(chalk.bold('Shared Clusters:')),
        {
          value: 'shared-eu-west-1',
          name: 'shared-eu-west-1',
        },
        {
          value: 'shared-ap-northeast-1',
          name: 'shared-ap-northeast-1',
        },
        {
          value: 'shared-us-west-2',
          name: 'shared-us-west-2',
        },
        new inquirer.Separator('                     '),
        new inquirer.Separator(chalk.bold('Local (docker):')),
        {
          value: 'local',
          name: 'local',
        },
      ],
      pageSize: 8,
    }

    const { cluster } = await this.out.prompt(question)

    return cluster
  }

  private async serviceNameSelector(defaultName: string): Promise<string> {
    const question = {
      name: 'service',
      type: 'input',
      message: 'Please choose the service name',
      default: defaultName,
    }

    const { service } = await this.out.prompt(question)

    return service
  }

  private async targetNameSelector(defaultName: string): Promise<string> {
    const question = {
      name: 'target',
      type: 'input',
      message: 'Please choose the target name',
      default: defaultName,
    }

    const { target } = await this.out.prompt(question)

    return target
  }

  private async dryRun() {
    const {target} = this.flags

    await this.definition.load(this.flags)
    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(target)
    const targetName = target || 'default'

    this.out.action.start(
      `Getting diff for ${chalk.bold(id)} with target ${chalk.bold(
        targetName,
      )}.`,
    )

    try {
      const migrationResult = await this.client.push(
        id,
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
            id,
          )} in env ${chalk.bold(targetName)}, no action required.\n`,
        )
        return
      }

      if (migrationResult.migrationMessages.length > 0) {
        this.out.log(
          chalk.blue(
            `Your project ${chalk.bold(id)} of env ${chalk.bold(
              targetName,
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
        process.exitCode = 1
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
        process.exitCode = 1
      }
    } catch (e) {
      this.out.action.stop()
      this.out.error(e)
    }
  }

}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
