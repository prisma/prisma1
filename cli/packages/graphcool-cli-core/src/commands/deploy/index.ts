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
import * as childProcess from 'child_process'
import { getBinPath } from './getbin'
import * as semver from 'semver'
const debug = require('debug')('deploy')
import * as Raven from 'raven'

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
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes',
    }),
    watch: flags.boolean({
      char: 'w',
      description: 'Watch for changes',
    }),
    interactive: flags.boolean({
      char: 'i',
      description: 'Force interactive mode to select the cluster',
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
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  private deploying: boolean = false
  private showedLines: number = 0
  async run() {
    debug('run')
    const { force, watch, interactive } = this.flags
    const envFile = this.flags['env-file']
    const dryRun = this.flags['dry-run']

    if (envFile && !fs.pathExistsSync(path.join(this.config.cwd, envFile))) {
      this.out.error(`--env-file path '${envFile}' does not exist`)
    }

    await this.definition.load(this.flags, envFile)
    if (!this.definition.definition) {
      throw new Error(
        `Couldn’t find \`graphcool.yml\` file. Are you in the right directory?`,
      )
    }
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    let cluster = this.definition.getCluster()
    if (!cluster && this.definition.definition.cluster) {
      cluster = await this.localUp()
    }
    let gotCluster = false

    if (!cluster) {
      cluster = await this.getCluster(serviceName, stage)
      gotCluster = true
    }

    if (cluster) {
      this.env.setActiveCluster(cluster)
    } else {
      throw new Error(
        `No cluster set. Please set the "cluster" property in your graphcool.yml`,
      )
    }

    if (this.showedLines > 0) {
      this.out.up(this.showedLines)
    }

    if (gotCluster) {
      this.out.log(
        `Added ${chalk.bold(`cluster: ${cluster!.name}`)} to graphcool.yml`,
      )
    }

    await this.checkVersions(cluster!)

    let projectNew = false
    if (!await this.projectExists(serviceName, stage)) {
      await this.addProject(serviceName, stage)
      projectNew = true
    }

    await this.deploy(stage, serviceName, cluster!, force, dryRun, projectNew)

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
                false,
              )
              this.out.log('Watching for change...')
            }
          })
        })
    }
  }

  private async checkVersions(cluster: Cluster) {
    const clusterVersion = await cluster!.getVersion()
    if (clusterVersion) {
      const gcSemverRegex = /(\d{1,2}\.\d{1,2}\.?\d{0,2})-?\w*(\d{1,2}\.\d{1,2}\.?\d{0,2})/
      const clusterMatch = clusterVersion.match(gcSemverRegex)
      const cliMatch = this.config.version.match(gcSemverRegex)
      const localNote = cluster.local
        ? `Please use ${chalk.green.bold(
            'graphcool local upgrade',
          )} to get the latest cluster.`
        : `Please update your graphcool cli to the latest version ${chalk.green.bold(
            'npm install -g graphcool',
          )}`
      const error = new Error(
        `The CLI version (${
          this.config.version
        }) and cluster version (${clusterVersion}) of cluster ${
          cluster.name
        } do not match. ${localNote}`,
      )
      if (clusterMatch && clusterMatch[1] && cliMatch && cliMatch[1]) {
        const mainSatisfied = semver.satisfies(
          cliMatch[1],
          `~${clusterMatch[1]}`,
        )
        if (mainSatisfied) {
          if (clusterMatch[2] && cliMatch[2]) {
            const secondarySatisfied = semver.satisfies(
              cliMatch[2],
              `~${clusterMatch[2]}`,
            )
            if (!secondarySatisfied) {
              throw error
            }
          }
        } else {
          throw error
        }
      }
    }
  }

  private async localUp(): Promise<Cluster> {
    await Up.run(new Config({ mock: false, argv: [] }))
    await this.env.load(this.flags)
    const cluster = this.env.clusterByName('local')!
    this.env.setActiveCluster(cluster)
    return cluster
  }

  private async projectExists(name: string, stage: string): Promise<boolean> {
    try {
      return Boolean(await this.client.getProject(name, stage))
    } catch (e) {
      return false
    }
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
    projectNew: boolean,
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
    this.out.log(chalk.bold(`\nHooks:\n`))
    if (
      this.definition.definition!.seed &&
      !this.flags['no-seed'] &&
      projectNew
    ) {
      await this.seed(projectNew, serviceName, stageName)
    }
    await this.generateSchema(serviceName, stageName)
    await this.graphqlPrepare()

    // no action required
    this.deploying = false
    if (migrationResult.migration.steps.length > 0) {
      this.printEndpoints(cluster, serviceName, stageName)
    }
  }

  private async seed(
    projectNew: boolean,
    serviceName: string,
    stageName: string,
  ) {
    const seeder = new Seeder(
      this.definition,
      this.client,
      this.out,
      this.config,
    )
    const before = Date.now()
    const from =
      this.definition.definition!.seed &&
      this.definition.definition!.seed!.import
        ? ` from \`${this.definition.definition!.seed!.import}\``
        : ''
    this.out.action.start(`Importing seed dataset${from}`)
    await seeder.seed(serviceName, stageName)
    this.out.action.stop(this.prettyTime(Date.now() - before))
  }

  private async generateSchema(serviceName: string, stageName: string) {
    const schemaPath =
      this.definition.definition!.schema || this.getSchemaPathFromConfig()
    if (schemaPath) {
      const schemaDir = path.dirname(schemaPath)
      fs.mkdirpSync(schemaDir)
      const token = this.definition.getToken(serviceName, stageName)
      const before = Date.now()
      this.out.action.start(`Writing database schema to \`${schemaPath}\` `)
      const schemaString = await fetchAndPrintSchema(
        this.client,
        serviceName,
        stageName,
        token,
      )
      fs.writeFileSync(schemaPath, schemaString)
      this.out.action.stop(this.prettyTime(Date.now() - before))
    }
  }

  private async graphqlPrepare() {
    const graphqlBin = await getBinPath('graphql')
    if (graphqlBin) {
      try {
        await this.runCmd(`graphql prepare`)
      } catch (e) {
        //
      }
    }
  }

  private runCmd(cmd: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.out.log(`Running ${chalk.cyan(`$ ${cmd}`)}...`)
      const args = cmd.split(/\s/g)
      const child = childProcess.spawn(args[0], args.slice(1), {
        cwd: this.config.cwd,
      })
      child.stdout.on('data', data => {
        this.out.log(data.toString())
      })
      let errorShown = false
      child.stderr.on('data', data => {
        if (!errorShown) {
          this.out.log(chalk.bold.red(`\nError:`))
          errorShown = true
        }
        this.out.log(data.toString())
      })
      child.on('error', err => {
        this.out.error(err)
      })
      child.on('close', code => {
        if (code !== 0) {
          reject(code)
        } else {
          resolve()
        }
      })
    })
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

  private async getCluster(
    serviceName: string,
    stage: string,
  ): Promise<Cluster | undefined> {
    const name = await this.clusterSelection(serviceName, stage)
    await this.definition.addCluster(name, this.flags)
    return this.env.clusterByName(name)
  }

  private async clusterSelection(
    serviceName: string,
    stage: string,
  ): Promise<string> {
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
      message: `Please choose the cluster you want to deploy "${serviceName}@${stage}" to`,
      choices: [
        new inquirer.Separator(chalk.bold('Custom clusters (local/private):')),
        ...localClusters,
        new inquirer.Separator('                     '),
        new inquirer.Separator(chalk.bold('Shared Clusters:')),
        {
          value: 'shared-public-demo',
          name: 'shared-public-demo',
        },
      ],
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
