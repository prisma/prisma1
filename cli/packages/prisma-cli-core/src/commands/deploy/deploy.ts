import { Command, flags, Flags, DeployPayload, Config } from 'prisma-cli-engine'
import { Cluster } from 'prisma-yml'
import chalk from 'chalk'
import * as inquirer from 'inquirer'
import * as path from 'path'
import * as fs from 'fs-extra'
import { fetchAndPrintSchema } from './printSchema'
import { Seeder } from '../seed/Seeder'
const debug = require('debug')('deploy')
import { prettyTime, concatName, defaultDockerCompose } from '../../util'
import * as sillyname from 'sillyname'
import { getSchemaPathFromConfig } from './getSchemaPathFromConfig'
import { EndpointDialog } from '../../utils/EndpointDialog'
import { spawnSync } from 'npm-run'
import { spawnSync as nativeSpawnSync } from 'child_process'
import * as figures from 'figures'
import { SchemaError } from 'prisma-cli-engine/dist/Client/types'

export default class Deploy extends Command {
  static topic = 'deploy'
  static description = 'Deploy service changes (or new service)'
  static group = 'general'
  static help = `
  
  ${chalk.green.bold('Examples:')}
      
${chalk.gray(
    '-',
  )} Deploy local changes from prisma.yml to the default service environment.
  ${chalk.green('$ prisma deploy')}
    
${chalk.gray(
    '-',
  )} Deploy local changes from default service file accepting potential data loss caused by schema changes
  ${chalk.green('$ prisma deploy --force')}
  `
  static flags: Flags = {
    force: flags.boolean({
      char: 'f',
      description: 'Accept data loss caused by schema changes',
    }),
    new: flags.boolean({
      char: 'n',
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
    'no-migrate': flags.boolean({
      description: 'Disable migrations. Prisma 1.26 and above needed',
    }),
    ['env-file']: flags.string({
      description: 'Path to .env file to inject env vars',
      char: 'e',
    }),
  }
  private deploying: boolean = false
  private showedHooks: boolean = false
  private loggedIn: boolean = false
  async run() {
    /**
     * Get Args
     */
    const { force } = this.flags
    const interactive = this.flags.new // new is a reserved keyword, so we use interactive instead
    const envFile = this.flags['env-file']
    const dryRun = this.flags['dry-run']
    const noMigrate = this.flags['no-migrate']

    if (envFile && !fs.pathExistsSync(path.join(this.config.cwd, envFile))) {
      await this.out.error(`--env-file path '${envFile}' does not exist`)
    }

    /**
     * Get prisma.yml content
     */
    await this.definition.load(this.flags, envFile)

    if (!this.definition.definition!.datamodel) {
      await this.out.error(
        `The property ${chalk.bold('datamodel')} is missing in your prisma.yml`,
      )
    }

    let serviceName = this.definition.service!
    let stage = this.definition.stage!

    /**
     * If no endpoint or service provided, ask for it
     */
    let workspace: string | undefined | null = this.definition.getWorkspace()
    let cluster
    let dockerComposeYml = defaultDockerCompose
    if (!serviceName || !stage || interactive) {
      const endpointDialog = new EndpointDialog({
        out: this.out,
        client: this.client,
        env: this.env,
        config: this.config,
        definition: this.definition,
        shouldAskForGenerator: false,
      })
      const results = await endpointDialog.getEndpoint()
      cluster = results.cluster
      workspace = results.workspace
      serviceName = results.service
      stage = results.stage
      dockerComposeYml = results.dockerComposeYml
      this.definition.replaceEndpoint(results.endpoint)
      // Reload definition because we are changing the yml file
      await this.definition.load(this.flags, envFile)
      this.out.log(
        `\nWritten endpoint \`${chalk.bold(
          results.endpoint,
        )}\` to prisma.yml\n`,
      )
    } else {
      cluster = this.definition.getCluster(false)
    }

    if (cluster && cluster.local && !(await cluster.isOnline())) {
      throw new Error(
        `Could not connect to server at ${
          cluster.baseUrl
        }. Please check if your server is running.`,
      )
    }

    /**
     * Abort when no cluster is set
     */
    if (cluster) {
      this.env.setActiveCluster(cluster)
    } else {
      throw new Error(`Cluster ${cluster} does not exist.`)
    }

    /**
     * Make sure we're logged in when a non-public cluster has been chosen
     */
    if (cluster && !cluster.local && cluster.isPrivate) {
      if (!workspace) {
        workspace = this.definition.getWorkspace()
      }
      if (
        workspace &&
        !workspace.startsWith('public-') &&
        !process.env.PRISMA_MANAGEMENT_API_SECRET &&
        (!this.env.cloudSessionKey || this.env.cloudSessionKey === '')
      ) {
        await this.client.login()
        cluster.clusterSecret = this.env.cloudSessionKey
      }
    }

    await this.client.initClusterClient(cluster, serviceName, stage, workspace!)

    let projectNew = false
    debug('checking if project exists')
    if (!(await this.projectExists(cluster, serviceName, stage, workspace!))) {
      debug('adding project')
      await this.addProject(cluster, serviceName, stage, workspace!)
      projectNew = true
    }

    await this.deploy(
      stage,
      serviceName,
      cluster,
      cluster.name,
      force,
      dryRun,
      projectNew,
      workspace!,
      noMigrate,
    )
  }

  private getSillyName() {
    return `${slugify(sillyname()).split('-')[0]}-${Math.round(
      Math.random() * 1000,
    )}`
  }

  private getPublicName() {
    return `public-${this.getSillyName()}`
  }

  private async projectExists(
    cluster: Cluster,
    name: string,
    stage: string,
    workspace: string | null,
  ): Promise<boolean> {
    try {
      return Boolean(
        await this.client.getProject(
          concatName(cluster, name, workspace),
          stage,
        ),
      )
    } catch (e) {
      return false
    }
  }

  private async addProject(
    cluster: Cluster,
    name: string,
    stage: string,
    workspace: string | null,
  ): Promise<void> {
    this.out.action.start(`Creating stage ${stage} for service ${name}`)
    const createdProject = await this.client.addProject(
      concatName(cluster, name, workspace),
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
    noMigrate: boolean,
  ): Promise<void> {
    this.deploying = true
    let before = Date.now()

    const b = s => `\`${chalk.bold(s)}\``

    const verb = dryRun ? 'Performing dry run for' : 'Deploying'

    this.out.action.start(
      `${verb} service ${b(serviceName)} to stage ${b(stageName)} to server ${b(
        completeClusterName,
      )}`,
    )

    const migrationResult: DeployPayload = await this.client.deploy(
      concatName(cluster, serviceName, workspace),
      stageName,
      this.definition.typesString!,
      dryRun,
      this.definition.getSubscriptions(),
      this.definition.secrets,
      force,
      noMigrate,
    )
    this.out.action.stop(prettyTime(Date.now() - before))
    this.printResult(migrationResult, force, dryRun)

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
          concatName(cluster, serviceName, workspace),
          stageName,
        )

        if (migration.errors && migration.errors.length > 0) {
          this.out.action.stop(prettyTime(Date.now() - before))
          throw new Error(
            `The Migration failed and has not been performed. This is very likely not a transient issue.\n` +
              migration.errors.join('\n'),
          )
        }

        /**
         * Read more here about the different deployment statuses https://github.com/prisma/prisma/issues/3326
         */
        if (
          migration.applied === migrationResult.migration.steps.length ||
          ['SUCCESS', 'ROLLBACK_SUCCESS', 'ROLLBACK_FAILURE'].includes(
            migration.status,
          )
        ) {
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

    const hooks = this.definition.getHooks('post-deploy')
    if (hooks.length > 0) {
      this.out.log(`\n${chalk.bold('post-deploy')}:`)
    }
    for (const hook of hooks) {
      const splittedHook = hook.split(' ')
      this.out.action.start(`Running ${chalk.cyan(hook)}`)
      const isPackaged = fs.existsSync('/snapshot')
      const spawnPath = isPackaged ? nativeSpawnSync : spawnSync
      const child = spawnPath(splittedHook[0], splittedHook.slice(1))
      const stderr = child.stderr && child.stderr.toString().trim()
      if (stderr && stderr.length > 0) {
        this.out.log(chalk.red(stderr))
      }
      const stdout = child.stdout && child.stdout.toString().trim()
      if (stdout && stdout.length > 0) {
        this.out.log(stdout)
      }
      const { status, error } = child
      if (error || status !== 0) {
        if (error) {
          this.out.log(chalk.red(error.message))
        }
        this.out.action.stop(chalk.red(figures.cross))
      } else {
        this.out.action.stop()
      }
    }

    if (migrationResult.migration) {
      if (
        this.definition.definition!.seed &&
        !this.flags['no-seed'] &&
        projectNew
      ) {
        this.printHooks()
        await this.seed(
          cluster,
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
    cluster: Cluster,
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
    const seedSource =
      this.definition.definition!.seed!.import ||
      this.definition.definition!.seed!.run
    if (!seedSource) {
      this.out.log(
        chalk.yellow(
          'Invalid seed property in `prisma.yml`. Please use `import` or `run` under the `seed` property. Follow the docs for more info: http://bit.ly/prisma-seed-optional',
        ),
      )
    } else {
      this.out.action.start(`Seeding based on ${chalk.bold(seedSource!)}`)
    }
    await seeder.seed(concatName(cluster, serviceName, workspace), stageName)
    this.out.action.stop(prettyTime(Date.now() - before))
  }

  /**
   * Returns true if there was a change
   */
  private async generateSchema(
    cluster: Cluster,
    serviceName: string,
    stageName: string,
  ): Promise<boolean> {
    const schemaPath = getSchemaPathFromConfig()
    if (schemaPath) {
      this.printHooks()
      const schemaDir = path.dirname(schemaPath)
      fs.mkdirpSync(schemaDir)
      const token = this.definition.getToken(serviceName, stageName)
      const before = Date.now()
      this.out.action.start(`Checking, if schema file changed`)
      const schemaString = await fetchAndPrintSchema(
        this.client,
        concatName(cluster, serviceName, this.definition.getWorkspace()),
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
        return true
      }
    }

    return false
  }

  private addCustomHelpMessage(errors: SchemaError[]) {
    const hasIdError = errors.find(e =>
      e.description.includes(
        'must be marked as the id field with the `@id` directive.',
      ),
    )
    if (hasIdError) {
      this.out.log(
        chalk.bold(
          '\nNote: The Prisma server you are deploying against runs with the new datamodel v2, which requires the @id directive.',
        ),
      )
    }
  }

  private printResult(payload: DeployPayload, force: boolean, dryRun: boolean) {
    if (payload.errors && payload.errors.length > 0) {
      this.out.log(chalk.bold.red('\nErrors:'))
      this.out.migration.printErrors(payload.errors)
      this.addCustomHelpMessage(payload.errors)
      this.out.log(
        '\nDeployment canceled. Please fix the above errors to continue deploying.',
      )
      this.out.log(
        'Read more about deployment errors here: https://bit.ly/prisma-force-flag',
      )

      this.out.exit(1)
    }

    const steps =
      payload.steps || (payload.migration && payload.migration.steps) || []

    if (
      steps.length === 0 &&
      (!payload.warnings || payload.warnings.length === 0)
    ) {
      if (dryRun) {
        this.out.log('There are no changes.')
      } else {
        this.out.log('Service is already up to date.')
      }
      return
    }

    if (steps.length > 0) {
      const areChangesPotential =
        dryRun || (payload.warnings && payload.warnings.length > 0)
      this.out.log(
        '\n' +
          chalk.bold(areChangesPotential ? 'Potential changes:' : 'Changes:'),
      )
      this.out.migration.printMessages(steps)
      this.out.log('')
    }

    if (payload.warnings && payload.warnings.length > 0) {
      this.out.log(`${chalk.bold.yellow('\nWarnings:')}`)
      this.out.migration.printWarnings(payload.warnings)

      if (force) {
        this.out.log('\nIgnoring warnings because you provided --force.')
      } else {
        this.out.log(
          `\nIf you want to ignore the warnings, please deploy with the --force flag: ${chalk.cyan(
            '$ prisma deploy --force',
          )}`,
        )
        this.out.log(
          'Read more about deployment warnings here: https://bit.ly/prisma-force-flag',
        )
        this.out.exit(1)
      }
    }
  }

  private printEndpoints(
    cluster: Cluster,
    serviceName: string,
    stageName: string,
    workspace?: string,
  ) {
    this.out.log(`\n${chalk.bold(
      'Your Prisma GraphQL database endpoint is live:',
    )}

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

  private getCloudClusters(): Cluster[] {
    return this.env.clusters.filter(c => c.shared || c.isPrivate)
  }

  private async clusterSelection(loggedIn: boolean): Promise<string> {
    debug({ loggedIn })

    const choices = loggedIn
      ? await this.getLoggedInChoices()
      : this.getPublicChoices()

    const question = {
      name: 'cluster',
      type: 'list',
      message: `Please choose the cluster you want to deploy to`,
      choices,
      pageSize: 9,
    }

    const { cluster } = await this.out.prompt(question)

    if (cluster === 'login') {
      await this.client.login()
      this.loggedIn = true
      return this.clusterSelection(true)
    }

    return cluster
  }

  private getLocalClusterChoices(): string[][] {
    return [['local', 'Local cluster (requires Docker)']]
  }

  private async getLoggedInChoices(): Promise<any[]> {
    const localChoices = this.getLocalClusterChoices()
    const combinations: string[][] = []
    const remoteClusters = this.env.clusters.filter(
      c => c.shared || c.isPrivate,
    )

    remoteClusters.forEach(cluster => {
      const label = this.env.sharedClusters.includes(cluster.name)
        ? 'Free development cluster (hosted on Prisma Cloud)'
        : 'Private Prisma Cluster'
      combinations.push([`${cluster.workspaceSlug}/${cluster.name}`, label])
    })

    const allCombinations = [...combinations, ...localChoices]

    return [
      new inquirer.Separator('                     '),
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
