import {
  Command,
  flags,
  Flags,
  DeployPayload,
  Config,
} from 'graphcool-cli-engine'
import { Cluster } from 'graphcool-yml'
import chalk from 'chalk'
import { ServiceDoesntExistError } from '../../errors/ServiceDoesntExistError'
import { emptyDefinition } from './emptyDefinition'
import * as chokidar from 'chokidar'
import * as inquirer from 'graphcool-inquirer'
import * as path from 'path'
import * as fs from 'fs-extra'
import { getGraphQLConfig } from 'graphql-config'
import { fetchAndPrintSchema } from './printSchema'
import Up from '../local/up'
import { Seeder } from '../seed/Seeder'
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
  ${chalk.green('$ graphcool deploy --force --stage production')}
  `
  static flags: Flags = {
    stage: flags.string({
      char: 's',
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
    'no-seed': flags.boolean({
      description: 'Disable seed on initial service deploy',
    }),
    json: flags.boolean({
      char: 'j',
      description: 'Json Output',
    }),
    dotenv: flags.string({
      description: 'Path to .env file to inject env vars',
    }),
  }
  private deploying: boolean = false
  private showedLines: number = 0
  async run() {
    debug('run')
    const { force, watch, interactive, dotenv, stage } = this.flags
    const newServiceClusterName = this.flags['new-service-cluster']
    const dryRun = this.flags['dry-run']

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

    if (dotenv && !fs.pathExistsSync(path.join(this.config.cwd, dotenv))) {
      this.out.error(`--dotenv path '${dotenv}' does not exist`)
    }

    await this.definition.load(this.flags, dotenv)
    const serviceName = this.definition.definition!.service
    let cluster = await this.client.getCluster(serviceName, stage)

    const serviceIsNew = !cluster

    if (!cluster) {
      const clusterName =
        (!interactive && newServiceClusterName) ||
        (await this.clusterSelection(stage))
      cluster = this.env.clusterByName(clusterName)!
      if (!cluster) {
        if (clusterName === 'local') {
          await Up.run(new Config({ mock: false, argv: [] }))
          await this.env.load(this.flags)
          cluster = this.env.clusterByName('local')!
          this.env.setActiveCluster(cluster)
        } else {
          this.out.error(`Cluster ${clusterName} could not be found.`)
        }
      }
    }
    this.env.setActiveCluster(cluster)

    if (this.showedLines > 0) {
      this.out.up(this.showedLines)
    }

    if (!await this.projectExists(serviceName, stage)) {
      await this.addProject(serviceName, stage)
    }

    await this.deploy(stage, serviceName, cluster!, force, dryRun)

    if (this.definition.definition!.seed && !this.flags['no-seed']) {
      const seeder = new Seeder(
        this.definition,
        this.client,
        this.out,
        this.config,
      )
      this.out.log(`Seeding Data...`)
      await seeder.seed(serviceName, stage)
    }

    if (watch) {
      this.out.log('Watching for change...')
      chokidar
        .watch(this.config.definitionDir, { ignoreInitial: true })
        .on('all', () => {
          setImmediate(async () => {
            if (!this.deploying) {
              await this.definition.load(this.flags)
              await this.deploy(
                stage,
                this.definition.definition!.service,
                cluster!,
                force,
                dryRun,
              )
              this.out.log('Watching for change...')
            }
          })
        })
    }
  }

  private async projectExists(name: string, stage: string): Promise<boolean> {
    const projects = await this.client.listProjects()
    return Boolean(projects.find(p => p.name === name && p.stage === stage))
  }

  private async addProject(name: string, stage: string): Promise<void> {
    this.out.action.start(`Creating stage ${stage} for service ${name}`)
    const createdProject = await this.client.addProject(
      name,
      stage,
      this.definition.secrets,
    )
    this.out.action.stop()
  }

  private prettyTime(time: number): string {
    const output =
      time > 1000 ? (Math.round(time / 100) / 10).toFixed(1) + 's' : time + 'ms'
    return chalk.cyan(output)
  }

  private async deploy(
    stageName: string,
    serviceName: string,
    cluster: Cluster,
    force: boolean,
    dryRun: boolean,
  ): Promise<void> {
    this.deploying = true
    const localNote = cluster.local ? ' locally' : ''
    let before = Date.now()

    const b = s => `\`${chalk.bold(s)}\``

    const verb = dryRun ? 'Performing dry run for' : 'Deploying'

    this.out.action.start(
      `${verb} service ${b(serviceName)} to stage ${b(
        stageName,
      )} on cluster ${b(cluster.name)}`,
    )

    const migrationResult: DeployPayload = await this.client.deploy(
      serviceName,
      stageName,
      this.definition.typesString!,
      dryRun,
      this.definition.secrets,
    )
    this.out.action.stop(this.prettyTime(Date.now() - before))
    this.printResult(migrationResult)

    if (migrationResult.migration.revision > 0 && !dryRun) {
      before = Date.now()
      this.out.action.start(`Applying changes`)
      await this.client.waitForMigration(
        serviceName,
        stageName,
        migrationResult.migration.revision,
      )
      this.out.action.stop(this.prettyTime(Date.now() - before))
    }
    // TODO move up to if statement after testing done
    await this.generateSchema(serviceName, stageName)

    // no action required
    this.deploying = false
    if (migrationResult.migration.steps.length > 0) {
      this.printEndpoints(cluster, serviceName, stageName)
    }
  }

  private async generateSchema(serviceName: string, stageName: string) {
    const schemaPath =
      this.definition.definition!.schema || this.getSchemaPathFromConfig()
    if (schemaPath) {
      const schemaDir = path.dirname(schemaPath)
      fs.mkdirpSync(schemaDir)
      const token = this.definition.getToken(serviceName, stageName)
      const schemaString = await fetchAndPrintSchema(
        this.client,
        serviceName,
        stageName,
        token,
      )
      fs.writeFileSync(schemaPath, schemaString)
    }
  }

  private getSchemaPathFromConfig(): string | null {
    try {
      const config = getGraphQLConfig()
      if (config) {
        const schemaPath = config.config.schemaPath
        if (schemaPath) {
          return schemaPath
        }
        const projects = config.getProjects()
        if (projects) {
          const foundProjectName = Object.keys(projects).find(projectName => {
            const project = projects[projectName]
            if (
              ['graphcool', 'database', 'db'].includes(projectName) &&
              project.schemaPath
            ) {
              return true
            }
            return false
          })
          if (foundProjectName) {
            const foundProject = projects[foundProjectName]
            return foundProject.schemaPath
          }
        }
      }
    } catch (e) {
      //
    }

    return null
  }

  private printResult(payload: DeployPayload) {
    if (payload.errors && payload.errors.length > 0) {
      this.out.log(`${chalk.bold.red('Errors:')}`)
      this.out.migration.printErrors(payload.errors)
      this.out.log('')
      return
    }

    if (payload.migration.steps.length === 0) {
      this.out.log('Service is already up to date.')
      return
    }

    if (payload.migration.steps.length > 0) {
      // this.out.migrati
      this.out.log('\n' + chalk.bold('Changes:'))
      this.out.migration.printMessages(payload.migration.steps)
      this.out.log('')
    }
  }

  private printEndpoints(
    cluster: Cluster,
    serviceName: string,
    stageName: string,
  ) {
    this.out.log(`\n${chalk.bold('Your GraphQL database endpoint is live:')}

  ${chalk.bold('HTTP:')}  ${cluster.getApiEndpoint(serviceName, stageName)}\n`)
  }

  private async clusterSelection(stage: string): Promise<string> {
    const localClusters = this.env.clusters.filter(c => c.local).map(c => {
      return {
        value: c.name,
        name: c.name,
      }
    })
    if (localClusters.length === 0) {
      localClusters.push({ value: 'local', name: 'local' })
    }
    const question = {
      name: 'cluster',
      type: 'list',
      message: `Please choose the cluster you want to deploy "${stage}" to`,
      choices: [
        new inquirer.Separator(chalk.bold('Shared Clusters:')),
        {
          value: 'shared-public-demo',
          name: 'shared-public-demo',
        },
        new inquirer.Separator('                     '),
        new inquirer.Separator(chalk.bold('Custom clusters (local/private):')),
      ].concat(localClusters),
      pageSize: 8,
    }

    const { cluster } = await this.out.prompt(question)
    this.showedLines += 2

    return cluster
  }

  private async stageNameSelector(defaultName: string): Promise<string> {
    const question = {
      name: 'stage',
      type: 'input',
      message: 'Please choose the stage name',
      default: defaultName,
    }

    const { stage } = await this.out.prompt(question)

    this.showedLines += 1

    return stage
  }
}

export function isValidProjectName(projectName: string): boolean {
  return /^[A-Z](.*)/.test(projectName)
}
