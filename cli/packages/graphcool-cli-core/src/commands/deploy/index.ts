import { Command, flags, Flags } from 'graphcool-cli-engine'
import chalk from 'chalk'
import * as sillyName from 'sillyname'
import { ServiceDoesntExistError } from '../../errors/ServiceDoesntExistError'
import { emptyDefinition } from './emptyDefinition'
import * as chokidar from 'chokidar'
import * as inquirer from 'graphcool-inquirer'
import * as path from 'path'
import * as fs from 'fs-extra'
const debug = require('debug')('deploy')

export default class Deploy extends Command {
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

${chalk.gray('-')} Deploy local changes to a specific stage
  ${chalk.green('$ graphcool deploy --stage production')}
    
${chalk.gray(
    '-',
  )} Deploy local changes from default service file accepting potential data loss caused by schema changes
  ${chalk.green('$ graphcool deploy --force --env production')}
  `
  static flags: Flags = {
    stage: flags.string({
      char: 't',
      description: 'Local stage, ID or alias of service to deploy',
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
      description: 'Force interactive mode to select the cluster',
    }),
    default: flags.boolean({
      char: 'D',
      description: 'Set specified stage as default',
    }),
    'dry-run': flags.boolean({
      char: 'd',
      description: 'Perform a dry-run of the deployment',
    }),
    json: flags.boolean({
      char: 'j',
      description: 'Json Output',
    }),
  }
  private deploying: boolean = false
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
    // stage can be both key or value of the `stages` object in the .graphcoolrc
    // so either "my-stage" or "shared-eu-west-1/asdf"
    let showedDialog = false
    let stageName
    let stage
    let cluster
    const foundTarget = await this.env.getTargetWithName(
      process.env.GRAPHCOOL_TARGET || this.flags.stage,
    )
    // load the definition already so we're able to detect missing package.json / node_modules
    // if it is a existing project,

    if (interactive) {
      foundTarget.stageName = null
      foundTarget.stage = null
    }

    this.definition.checkNodeModules(Boolean(foundTarget.stage))

    if (
      interactive ||
      (!newServiceCluster && !foundTarget.stage) ||
      (newServiceName && !newServiceCluster)
    ) {
      cluster = await this.clusterSelection()
      showedDialog = true
      this.env.setActiveCluster(cluster)
      this.env.saveLocalRC()
      if (
        cluster === 'local' &&
        (!this.env.rc.clusters || !this.env.rc.clusters!.local)
      ) {
        this.out.log(`You chose the cluster ${chalk.bold(
          'local',
        )}, but don't have docker initialized, yet.
Please run ${chalk.green(
          '$ graphcool local up',
        )} to get a local Graphcool cluster.
`)
        this.out.exit(1)
      }
    }

    if (
      newServiceName ||
      interactive ||
      (!foundTarget.stageName && !foundTarget.stage)
    ) {
      stageName = this.flags.stage
      if (!stageName) {
        stageName = await this.stageNameSelector(
          this.env.getDefaultTargetName(cluster),
        )
        showedDialog = true
      }
    }

    if (!stageName && foundTarget.stageName) {
      stageName = foundTarget.stageName
    }

    if (!stage && foundTarget.stage) {
      stage = foundTarget.stage
    }

    if ((!newServiceName && !foundTarget.stage) || interactive) {
      newServiceName = await this.serviceNameSelector(
        path.basename(this.config.definitionDir),
      )
      showedDialog = true
    }

    if (showedDialog) {
      this.out.up(3)
    }

    // await this.auth.ensureAuth()
    await this.definition.load({
      ...this.flags,
      stage: stageName,
      cluster,
    })

    let projectId
    let projectIsNew = false

    cluster = cluster ? cluster : stage ? stage.cluster : this.env.activeCluster
    const isLocal = !this.env.isSharedCluster(cluster)

    if (!stage) {
      // if a specific service has been provided, check for its existence
      if (stage) {
        this.out.error(new ServiceDoesntExistError(stage))
      }

      const region = this.env.getRegionFromCluster(cluster)

      // otherwise create a new project
      const newProject = await this.createProject(newServiceName)
      projectId = newProject.projectId
      projectIsNew = true

      // add environment
      await this.env.setLocalTarget(stageName, `${cluster}/${projectId}`)

      if (!this.env.default || useDefault) {
        this.env.setLocalDefaultTarget(stageName)
      }

      this.env.saveLocalRC()
    } else {
      projectId = stage.id
    }

    // best guess for "project name"
    const projectName = newServiceName || stageName

    //     const info = await this.client.fetchProjectInfo(projectId)

    //     if (!info.isEjected) {
    //       this.out.error(`Your service ${info.name} (${
    //         info.id
    //       }) is not yet upgraded.
    // Please go to the console and upgrade it:
    // https://console.graph.cool/${encodeURIComponent(info.name)}/settings/general`)
    //     }

    await this.deploy(
      projectIsNew,
      stageName,
      projectId,
      isLocal,
      force,
      projectName,
      cluster,
    )

    if (watch) {
      this.out.log('Watching for change...')
      chokidar
        .watch(this.config.definitionDir, { ignoreInitial: true })
        .on('all', () => {
          setImmediate(async () => {
            if (!this.deploying) {
              await this.definition.load(this.flags)
              await this.deploy(
                projectIsNew,
                stageName,
                projectId!,
                isLocal,
                force,
                projectName,
                cluster,
              )
              this.out.log('Watching for change...')
            }
          })
        })
    }
  }

  private async createProject(
    name: string,
  ): Promise<{
    projectId: string
  }> {
    // create project
    const createdProject = await this.client.createProject(name)

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
    stageName: string,
    projectId: string,
    isLocal: boolean,
    force: boolean,
    projectName: string | null,
    cluster: string,
  ): Promise<void> {
    this.deploying = true
    const localNote = isLocal ? ' locally' : ''
    const before = Date.now()
    this.out.action.start(
      projectIsNew
        ? `Deploying${localNote}`
        : `Deploying to ${chalk.bold(cluster)} with stage ${chalk.bold(
            stageName || `${cluster}/${projectId}`,
          )}${localNote}`,
    )

    const migrationResult = await this.client.deploy(
      projectId,
      this.definition.getTypes(),
    )
    this.out.action.stop(this.prettyTime(Date.now() - before))

    // no action required
    this.deploying = false
    this.printEndpoints(projectId)
  }

  private printEndpoints(projectId) {
    this.out.log(`Here are your GraphQL Endpoints:

  ${chalk.bold('API:')}        ${this.env.simpleEndpoint(projectId)}`)
    // ${chalk.bold('Relay API:')}         ${this.env.relayEndpoint(projectId)}
    //   ${chalk.bold('Subscriptions API:')} ${this.env.subscriptionEndpoint(
    //       projectId,
    //     )}
  }

  private async clusterSelection(): Promise<string> {
    const localClusters = Object.keys(this.env.rc.clusters || {}).map(
      clusterName => {
        return {
          value: clusterName,
          name: clusterName,
        }
      },
    )
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
        new inquirer.Separator(chalk.bold('Custom clusters (local/private):')),
      ].concat(localClusters),
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

  private async stageNameSelector(defaultName: string): Promise<string> {
    const question = {
      name: 'stage',
      type: 'input',
      message: 'Please choose the stage name',
      default: defaultName,
    }

    const { stage } = await this.out.prompt(question)

    return stage
  }

  private async dryRun() {
    const { stage } = this.flags

    await this.definition.load(this.flags)
    await this.auth.ensureAuth()

    const { id } = await this.env.getTarget(stage)
    const stageName = stage || 'default'

    this.out.action.start(
      `Getting diff for ${chalk.bold(id)} with stage ${chalk.bold(stageName)}.`,
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
          `Identical service definition for service ${chalk.bold(
            id,
          )} in env ${chalk.bold(stageName)}, no action required.\n`,
        )
        return
      }

      if (migrationResult.migrationMessages.length > 0) {
        this.out.log(
          chalk.blue(
            `Your service ${chalk.bold(id)} of env ${chalk.bold(
              stageName,
            )} has the following changes:`,
          ),
        )

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
    } catch (e) {
      this.out.action.stop()
      this.out.error(e)
    }
  }
}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
