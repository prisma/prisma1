import { Command, flags, Flags, Cluster } from 'graphcool-cli-engine'
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
      description: 'Local stage to deploy to',
      defaultValue: 'dev',
    }),
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes',
    }),
    watch: flags.boolean({
      char: 'w',
      description: 'Watch for changes',
    }),
    'new-service-cluster': flags.string({
      char: 'c',
      description: 'Name of the Cluster to deploy to',
    }),
    // alias: flags.string({
    //   char: 'a',
    //   description: 'Service alias',
    // }),
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
    const { force, watch, interactive } = this.flags
    const newServiceClusterName = this.flags['new-service-cluster']
    // const dryRun = this.flags['dry-run']
    const stageName = this.flags.stage

    // if (dryRun) {
    //   return this.dryRun()
    // }

    if (newServiceClusterName) {
      const newServiceCluster = this.env.clusterByName(newServiceClusterName)
      if (!newServiceCluster) {
        this.out.error(
          `You provided 'new-service-cluster' ${chalk.bold(
            newServiceClusterName,
          )}, but it doesn't exist. Please check your global ~/.graphcoolrc`,
        )
      } else {
        this.env.setActiveCluster(newServiceCluster)
      }
    }

    await this.definition.load(this.env, this.flags)
    let clusterName = this.definition.getStage(stageName)
    const serviceIsNew = !!clusterName
    if (!clusterName) {
      clusterName = newServiceClusterName || (await this.clusterSelection())
    }

    const cluster = this.env.clusterByName(clusterName!)
    if (!cluster) {
      this.out.error(`Cluster ${clusterName} could not be found.`)
    }

    await this.deploy(
      serviceIsNew,
      stageName,
      this.definition.definition!.service,
      cluster!,
      force,
    )

    if (watch) {
      this.out.log('Watching for change...')
      chokidar
        .watch(this.config.definitionDir, { ignoreInitial: true })
        .on('all', () => {
          setImmediate(async () => {
            if (!this.deploying) {
              await this.definition.load(this.env, this.flags)
              await this.deploy(
                serviceIsNew,
                stageName,
                this.definition.definition!.service,
                cluster!,
                force,
              )
              this.out.log('Watching for change...')
            }
          })
        })
    }

    if (serviceIsNew) {
      this.definition.setStage(stageName, cluster!.name)
      this.definition.save()
      this.out.log(`Added stage ${stageName} to graphcool.yml`)
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
    const output =
      time > 1000 ? (Math.round(time / 100) / 10).toFixed(1) + 's' : time + 'ms'
    return chalk.cyan(output)
  }

  private async deploy(
    serviceIsNew: boolean,
    stageName: string,
    serviceName: string,
    cluster: Cluster,
    force: boolean,
  ): Promise<void> {
    this.deploying = true
    const localNote = cluster.local ? ' locally' : ''
    const before = Date.now()
    this.out.action.start(
      serviceIsNew
        ? `Deploying${localNote}`
        : `Deploying to ${chalk.bold(cluster.name)} with stage ${chalk.bold(
            stageName,
          )}${localNote}`,
    )

    const migrationResult = await this.client.deploy(
      serviceName,
      stageName,
      this.definition.typesString!,
    )
    this.out.action.stop(this.prettyTime(Date.now() - before))

    // no action required
    this.deploying = false
    this.printEndpoints(cluster, serviceName, stageName)
  }

  private printEndpoints(
    cluster: Cluster,
    serviceName: string,
    stageName: string,
  ) {
    this.out.log(`Here are your GraphQL Endpoints:

  ${chalk.bold('API:')}        ${cluster.getApiEndpoint(
      serviceName,
      stageName,
    )}`)
  }

  private async clusterSelection(): Promise<string> {
    const localClusters = this.env.clusters
      .filter(c => c.local)
      .map(clusterName => {
        return {
          value: clusterName,
          name: clusterName,
        }
      })
    const question = {
      name: 'cluster',
      type: 'list',
      message: 'Please choose the cluster you want to deploy to',
      choices: [
        new inquirer.Separator(chalk.bold('Shared Clusters:')),
        new inquirer.Separator('shared-eu-west-1 (coming soon)'),
        {
          value: 'shared-eu-west-1',
          name: 'shared-eu-west-1',
        },
        // {
        //   value: 'shared-ap-northeast-1',
        //   name: 'shared-ap-northeast-1',
        // },
        // {
        //   value: 'shared-us-west-2',
        //   name: 'shared-us-west-2',
        // },
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

  // private async dryRun() {
  //   const { stage } = this.flags

  //   await this.definition.load(this.env, this.flags)
  //   // await this.auth.ensureAuth()

  //   const stageName = stage || 'default'

  //   this.out.action.start(
  //     `Getting diff for ${chalk.bold(id)} with stage ${chalk.bold(stageName)}.`,
  //   )

  //   try {
  //     const migrationResult = await this.client.push(
  //       id,
  //       false,
  //       true,
  //       this.definition.definition!,
  //     )
  //     this.out.action.stop()

  //     // no action required
  //     if (
  //       (!migrationResult.migrationMessages ||
  //         migrationResult.migrationMessages.length === 0) &&
  //       (!migrationResult.errors || migrationResult.errors.length === 0)
  //     ) {
  //       this.out.log(
  //         `Identical service definition for service ${chalk.bold(
  //           id,
  //         )} in env ${chalk.bold(stageName)}, no action required.\n`,
  //       )
  //       return
  //     }

  //     if (migrationResult.migrationMessages.length > 0) {
  //       this.out.log(
  //         chalk.blue(
  //           `Your service ${chalk.bold(id)} of env ${chalk.bold(
  //             stageName,
  //           )} has the following changes:`,
  //         ),
  //       )

  //       this.out.migration.printMessages(migrationResult.migrationMessages)
  //       this.definition.set(migrationResult.projectDefinition)
  //     }

  //     if (migrationResult.errors.length > 0) {
  //       this.out.log(
  //         chalk.rgb(244, 157, 65)(
  //           `There are issues with the new service definition:`,
  //         ),
  //       )
  //       this.out.migration.printErrors(migrationResult.errors)
  //       this.out.log('')
  //       process.exitCode = 1
  //     }

  //     if (
  //       migrationResult.errors &&
  //       migrationResult.errors.length > 0 &&
  //       migrationResult.errors[0].description.includes(`destructive changes`)
  //     ) {
  //       // potentially destructive changes
  //       this.out.log(
  //         `Your changes might result in data loss.
  //           Use ${chalk.cyan(
  //             `\`graphcool deploy --force\``,
  //           )} if you know what you're doing!\n`,
  //       )
  //       process.exitCode = 1
  //     }
  //   } catch (e) {
  //     this.out.action.stop()
  //     this.out.error(e)
  //   }
  // }
}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
