import { Command, flags, Flags, DeployPayload, Config } from 'prisma-cli-engine'
import { Cluster } from 'prisma-yml'
import chalk from 'chalk'
import { ServiceDoesntExistError } from '../../errors/ServiceDoesntExistError'
import { emptyDefinition } from './emptyDefinition'
import * as chokidar from 'chokidar'
import * as inquirer from 'inquirer'
import * as path from 'path'
import * as fs from 'fs-extra'
import { fetchAndPrintSchema } from './printSchema'
import Up from '../local/up'
import { Seeder } from '../seed/Seeder'
import * as childProcess from 'child_process'
import { getBinPath } from './getbin'
import * as semver from 'semver'
const debug = require('debug')('deploy')
import { prettyTime, concatName } from '../../util'
import { spawn } from '../../spawn'
import * as sillyname from 'sillyname'
import { getSchemaPathFromConfig } from './getSchemaPathFromConfig'
import { getGraphQLConfig } from 'graphql-config'
import * as findUp from 'find-up'

export default class Deploy extends Command {
  static topic = 'deploy'
  static description = 'Deploy service changes (or new service)'
  static group = 'general'
  static allowAnyFlags = true
  static help = `
  
  ${chalk.green.bold('Examples:')}
      
${chalk.gray(
    '-',
  )} Deploy local changes from prisma.yml to the default service environment.
  ${chalk.green('$ prisma deploy')}

${chalk.gray('-')} Deploy local changes to a specific stage
  ${chalk.green('$ prisma deploy --stage production')}
    
${chalk.gray(
    '-',
  )} Deploy local changes from default service file accepting potential data loss caused by schema changes
  ${chalk.green('$ prisma deploy --force --stage production')}
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
  private showedHooks: boolean = false
  async run() {
    debug('run')
    const { force, watch, interactive } = this.flags
    const envFile = this.flags['env-file']
    const dryRun = this.flags['dry-run']

    if (envFile && !fs.pathExistsSync(path.join(this.config.cwd, envFile))) {
      await this.out.error(`--env-file path '${envFile}' does not exist`)
    }

    await this.definition.load(this.flags, envFile)
    if (!this.definition.definition) {
      throw new Error(
        `Couldn’t find \`prisma.yml\` file. Are you in the right directory?`,
      )
    }
    const serviceName = this.definition.definition!.service
    const stage = this.definition.definition!.stage

    let cluster = this.definition.getCluster()
    const isOnline = cluster ? await cluster.isOnline() : false
    if (
      this.definition.definition.cluster === 'local' &&
      (!cluster || !isOnline)
    ) {
      cluster = await this.localUp()
    } else if (!isOnline && cluster && cluster.local) {
      cluster = await this.localUp()
    }
    let gotCluster = false

    if (!cluster) {
      if (this.definition.definition!.hasOwnProperty('cluster')) {
        const value = this.definition.rawJson.cluster
        const envNotice = value.toLowerCase().includes('env')
          ? `Make sure that the env var ${value} is set correctly or remove the cluster property from the prisma.yml and execute deploy again to get a new cluster.`
          : ''
        throw new Error(
          `Property cluster in prisma.yml is provided, but provided value ${value} could not be resolved. ${envNotice}`,
        )
      }
      const clusterWorkspace = await this.getCluster(serviceName, stage)
      cluster = clusterWorkspace.cluster
      gotCluster = true
    }

    if (cluster) {
      this.env.setActiveCluster(cluster)
    } else {
      throw new Error(
        `No cluster set. Please set the "cluster" property in your prisma.yml`,
      )
    }

    if (this.showedLines > 0) {
      this.out.up(this.showedLines)
    }

    if (gotCluster) {
      this.out.log(
        `Added ${chalk.bold(`cluster: ${cluster!.name}`)} to prisma.yml`,
      )
    }

    if (cluster && !cluster.local) {
      const workspace = this.definition.getWorkspace()
      if (
        workspace &&
        !workspace.startsWith('public-') &&
        (!this.env.cloudSessionKey || this.env.cloudSessionKey === '')
      ) {
        await this.client.login()
        cluster.clusterSecret = this.env.cloudSessionKey
      }
    }

    await this.client.initClusterClient(
      cluster,
      this.definition.getWorkspace() || '*',
      serviceName,
      stage,
    )

    await this.checkVersions(cluster!)

    let projectNew = false
    if (
      !await this.projectExists(
        serviceName,
        stage,
        this.definition.getWorkspace(),
      )
    ) {
      await this.addProject(serviceName, stage, this.definition.getWorkspace())
      projectNew = true
    }

    await this.deploy(
      stage,
      serviceName,
      cluster!,
      this.definition.definition!.cluster!,
      force,
      dryRun,
      projectNew,
      this.definition.getWorkspace(),
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
                stage,
                this.definition.definition!.service,
                cluster!,
                this.definition.definition!.cluster!,
                force,
                dryRun,
                false,
                this.definition.getWorkspace(),
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
            'prisma local upgrade',
          )} to get the latest cluster.`
        : `Please update your prisma cli to the latest version ${chalk.green.bold(
            'npm install -g prisma',
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
          // disable minor check in beta
          // if (clusterMatch[2] && cliMatch[2]) {
          //   const secondarySatisfied = semver.satisfies(
          //     cliMatch[2],
          //     `~${clusterMatch[2]}`,
          //   )
          //   if (!secondarySatisfied) {
          //     throw error
          //   }
          // }
        } else {
          throw error
        }
      }
    }
  }

  private getSillyName() {
    return `${slugify(sillyname()).split('-')[0]}-${Math.round(
      Math.random() * 1000,
    )}`
  }

  private getPublicName() {
    return `public-${this.getSillyName()}`
  }

  private async localUp(): Promise<Cluster> {
    await Up.run(new Config({ mock: false, argv: [] }))
    await this.env.load(this.flags)
    const cluster = this.env.clusterByName('local')!
    this.env.setActiveCluster(cluster)
    return cluster
  }

  private async projectExists(
    name: string,
    stage: string,
    workspace: string | null,
  ): Promise<boolean> {
    try {
      return Boolean(
        await this.client.getProject(concatName(name, workspace), stage),
      )
    } catch (e) {
      return false
    }
  }

  private async addProject(
    name: string,
    stage: string,
    workspace: string | null,
  ): Promise<void> {
    this.out.action.start(`Creating stage ${stage} for service ${name}`)
    const createdProject = await this.client.addProject(
      concatName(name, workspace),
      stage,
      this.definition.secrets,
    )
    this.out.action.stop()
  }

  private async deploy(
    stageName: string,
    serviceName: string,
    cluster: Cluster,
    completeClusterName: string,
    force: boolean,
    dryRun: boolean,
    projectNew: boolean,
    workspace: string | null,
  ): Promise<void> {
    this.deploying = true
    const localNote = cluster.local ? ' locally' : ''
    let before = Date.now()

    const b = s => `\`${chalk.bold(s)}\``

    const verb = dryRun ? 'Performing dry run for' : 'Deploying'

    this.out.action.start(
      `${verb} service ${b(serviceName)} to stage ${b(
        stageName,
      )} on cluster ${b(completeClusterName)}`,
    )

    const migrationResult: DeployPayload = await this.client.deploy(
      concatName(serviceName, workspace),
      stageName,
      this.definition.typesString!,
      dryRun,
      this.definition.getSubscriptions(),
      this.definition.secrets,
    )
    this.out.action.stop(prettyTime(Date.now() - before))
    this.printResult(migrationResult)

    if (
      migrationResult.migration &&
      migrationResult.migration.revision > 0 &&
      !dryRun
    ) {
      before = Date.now()
      this.out.action.start(
        `Applying changes`,
        this.getProgress(0, migrationResult.migration.steps.length),
      )
      let done = false
      while (!done) {
        const revision = migrationResult.migration.revision
        const migration = await this.client.getMigration(
          concatName(serviceName, workspace),
          stageName,
        )

        if (migration.errors && migration.errors.length > 0) {
          await this.out.error(migration.errors.join('\n'))
        }

        if (migration.applied === migrationResult.migration.steps.length) {
          done = true
        }
        this.out.action.status = this.getProgress(
          migration.applied,
          migrationResult.migration.steps.length,
        )
        await new Promise(r => setTimeout(r, 500))
      }

      this.out.action.stop(prettyTime(Date.now() - before))
    }
    // TODO move up to if statement after testing done
    if (migrationResult.migration) {
      if (
        this.definition.definition!.seed &&
        !this.flags['no-seed'] &&
        projectNew
      ) {
        this.printHooks()
        await this.seed(
          projectNew,
          serviceName,
          stageName,
          this.definition.getWorkspace(),
        )
      }

      // no action required
      this.deploying = false
      if (migrationResult.migration) {
        this.printEndpoints(
          cluster,
          serviceName,
          stageName,
          this.definition.getWorkspace() || undefined,
        )
      }
    }

    const schemaChanged = await this.generateSchema(serviceName, stageName)
    await this.graphqlPrepare()
  }

  private printHooks() {
    if (!this.showedHooks) {
      this.out.log(chalk.bold(`\nHooks:`))
      this.showedHooks = true
    }
  }

  private getProgress(applied: number, of: number) {
    return this.out.color.prisma(`(${applied}/${of})`)
  }

  private async seed(
    projectNew: boolean,
    serviceName: string,
    stageName: string,
    workspace: string | null,
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
    await seeder.seed(concatName(serviceName, workspace), stageName)
    this.out.action.stop(prettyTime(Date.now() - before))
  }

  /**
   * Returns true if there was a change
   */
  private async generateSchema(
    serviceName: string,
    stageName: string,
  ): Promise<boolean> {
    const schemaPath =
      getSchemaPathFromConfig() || this.definition.definition!.schema
    if (schemaPath) {
      this.printHooks()
      const schemaDir = path.dirname(schemaPath)
      fs.mkdirpSync(schemaDir)
      const token = this.definition.getToken(serviceName, stageName)
      const before = Date.now()
      this.out.action.start(`Checking, if schema file changed`)
      const schemaString = await fetchAndPrintSchema(
        this.client,
        concatName(serviceName, this.definition.getWorkspace()),
        stageName,
        token,
      )
      this.out.action.stop(prettyTime(Date.now() - before))
      const oldSchemaString = fs.pathExistsSync(schemaPath)
        ? fs.readFileSync(schemaPath, 'utf-8')
        : null
      if (schemaString !== oldSchemaString) {
        const beforeWrite = Date.now()
        this.out.action.start(`Writing database schema to \`${schemaPath}\` `)
        fs.writeFileSync(schemaPath, schemaString)
        this.out.action.stop(prettyTime(Date.now() - beforeWrite))
      }
    }

    return false
  }

  private async graphqlPrepare() {
    let dir
    try {
      dir = this.config.findConfigDir()
    } catch (e) {
      //
    }
    if (dir) {
      const graphqlBin = await getBinPath('graphql')
      if (graphqlBin) {
        this.out.log(`Running ${chalk.cyan(`$ graphql prepare`)}...`)
        try {
          const oldCwd = process.cwd()
          const configDir = this.config.findConfigDir()
          if (configDir) {
            process.chdir(configDir)
          }
          await spawn(`graphql`, ['prepare'])
          process.chdir(oldCwd)
        } catch (e) {
          this.out.warn(e)
        }
      }
    }
  }

  private printResult(payload: DeployPayload) {
    if (payload.errors && payload.errors.length > 0) {
      this.out.log(`${chalk.bold.red('Errors:')}`)
      this.out.migration.printErrors(payload.errors)
      this.out.log('')
      this.out.exit(1)
    }

    if (!payload.migration || payload.migration.steps.length === 0) {
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
    workspace?: string,
  ) {
    this.out.log(`\n${chalk.bold('Your GraphQL database endpoint is live:')}

  ${chalk.bold('HTTP:')}  ${cluster.getApiEndpoint(
      serviceName,
      stageName,
      workspace,
    )}
  ${chalk.bold('WS:')}    ${cluster.getWSEndpoint(
      serviceName,
      stageName,
      workspace,
    )}
`)
  }

  private async getCluster(
    serviceName: string,
    stage: string,
  ): Promise<{ cluster?: Cluster; workspace?: string }> {
    const loggedIn = await this.client.isAuthenticated()
    let workspaceClusterCombination = await this.clusterSelection(
      serviceName,
      stage,
      loggedIn,
    )
    const splitted = workspaceClusterCombination.split('/')
    let workspace = splitted.length > 1 ? splitted[0] : null
    const clusterName = splitted.slice(-1)[0]
    const exists = this.env.clusterByName(clusterName)

    // in this case it's a public cluster and we need to generate a workspace name
    if (!loggedIn && exists && !exists.local) {
      workspace = this.getPublicName()
      debug(`silly name`, workspace)
      workspaceClusterCombination = `${workspace}/${clusterName}`
    }

    if (!exists) {
      if (clusterName === 'local') {
        await this.localUp()
      } else {
        await this.out.error(`Could not find selected cluster ${clusterName}`)
      }
    }
    await this.definition.addCluster(workspaceClusterCombination, this.flags)

    return {
      cluster: this.env.clusterByName(clusterName) || undefined,
      workspace: workspace || undefined,
    }
  }

  private async clusterSelection(
    serviceName: string,
    stage: string,
    loggedIn: boolean,
  ): Promise<string> {
    debug({ loggedIn })

    const choices = loggedIn
      ? await this.getLoggedInChoices()
      : this.getPublicChoices()

    const question = {
      name: 'cluster',
      type: 'list',
      message: `Please choose the cluster you want to deploy "${serviceName}@${stage}" to`,
      choices,
      pageSize: 9,
    }

    const { cluster } = await this.out.prompt(question)
    this.showedLines += 2

    if (cluster === 'login') {
      await this.client.login()
      return this.clusterSelection(serviceName, stage, true)
    }

    return cluster
  }

  private getLocalClusterChoices(): string[][] {
    const clusters = this.env.clusters.filter(c => c.local).map(c => c.name)
    if (clusters.length === 0) {
      clusters.push('local')
    }

    return clusters.map(c => [c, 'Local cluster (requires Docker)'])
  }

  private async getLoggedInChoices(): Promise<any[]> {
    const localChoices = this.getLocalClusterChoices()
    const workspaces = await this.client.getWorkspaces()
    const clusters = this.env.clusters.filter(
      c => !c.local && c.name !== 'shared-public-demo',
    )
    const combinations: string[][] = []
    workspaces.forEach(workspace => {
      clusters.forEach(cluster => {
        combinations.push([
          `${workspace.slug}/${cluster.name}`,
          'Free development cluster (hosted on Prisma Cloud)',
        ])
      })
    })

    const allCombinations = [...combinations, ...localChoices]

    return [
      ...this.convertChoices(allCombinations),
      new inquirer.Separator('                     '),
      new inquirer.Separator(
        chalk.dim(
          `You can learn more about deployment in the docs: http://bit.ly/prisma-graphql-deployment`,
        ),
      ),
    ]
  }

  private convertChoices(
    choices: string[][],
  ): Array<{ value: string; name: string }> {
    const padded = this.out.printPadded(choices, 0, 6).split('\n')
    return padded.map((name, index) => ({
      name,
      value: choices[index][0],
    }))
  }

  private getPublicChoices(): any[] {
    const publicChoices = [
      [
        'prisma-eu1',
        'Public development cluster (hosted in EU on Prisma Cloud)',
      ],
      [
        'prisma-us1',
        'Public development cluster (hosted in US on Prisma Cloud)',
      ],
    ]
    const allCombinations = [...publicChoices, ...this.getLocalClusterChoices()]

    return [
      ...this.convertChoices(allCombinations),
      new inquirer.Separator('                     '),
      {
        value: 'login',
        name: 'Log in or create new account on Prisma Cloud',
      },
      new inquirer.Separator('                     '),
      new inquirer.Separator(
        chalk.dim(
          `Note: When not logged in, service deployments to Prisma Cloud expire after 7 days.`,
        ),
      ),
      new inquirer.Separator(
        chalk.dim(
          `You can learn more about deployment in the docs: http://bit.ly/prisma-graphql-deployment`,
        ),
      ),
      new inquirer.Separator('                     '),
    ]
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

function slugify(text) {
  return text
    .toString()
    .toLowerCase()
    .replace(/\s+/g, '-') // Replace spaces with -
    .replace(/[^\w\-]+/g, '') // Remove all non-word chars
    .replace(/\-\-+/g, '-') // Replace multiple - with single -
    .replace(/^-+/, '') // Trim - from start of text
    .replace(/-+$/, '') // Trim - from end of text
}
