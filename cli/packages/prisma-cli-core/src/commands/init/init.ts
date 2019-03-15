import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import { EndpointDialog } from '../../utils/EndpointDialog'
import { isDockerComposeInstalled } from '../../utils/dockerComposeInstalled'
import { spawnSync } from 'npm-run'
import { spawnSync as nativeSpawnSync } from 'child_process'
import * as figures from 'figures'

export default class Init extends Command {
  static topic = 'init'
  static description = 'Initialize a new service'

  static args = [
    {
      name: 'dirName',
      description: 'Folder to initialize in (optional)',
    },
  ]

  static flags: Flags = {
    endpoint: flags.string({
      char: 'e',
      description: 'Initial service endpoint (optional)',
      required: false,
    }),

    /**
     * Temporary flag needed to test Datamodel v2
     */
    ['prototype']: flags.boolean({
      description:
        'Output Datamodel v2. Note: This is a temporary flag for debugging',
    }),
  }

  async run() {
    const dirName = this.args!.dirName
    if (dirName) {
      const newDefinitionDir = path.join(this.config.cwd, dirName + '/')
      this.config.definitionDir = newDefinitionDir
    } else {
      this.config.definitionDir = this.config.cwd
    }

    const endpoint = this.flags!.endpoint
    await this.runInit({ endpoint })
  }

  async runInit({ endpoint }) {
    let files: string[] = []
    try {
      files = fs.readdirSync(this.config.definitionDir)
    } catch(e) {
      if (this.config.debug) {
        this.out.log(`prisma init workflow called without existing directory.`)
        this.out.log(e)
      }
    }
    // the .prismarc must be allowed for the docker version to be functioning
    if (
      files.length > 0 &&
      (files.includes('prisma.yml') || files.includes('datamodel.prisma'))
    ) {
      this.out.log(`
The directory ${chalk.cyan(
        this.config.definitionDir,
      )} contains files that could conflict:

${files.map(f => `  ${f}`).join('\n')}

Either try using a new directory name, or remove the files listed above.
      `)
      this.out.exit(1)
    }

    /**
     * If there is a predefined endpoint provided as a cli arg
     */

    if (endpoint) {
      fs.mkdirpSync(this.config.definitionDir)

      const datamodelBoilerplatePath =
        this.definition.definition &&
        this.definition.definition.databaseType === 'document'
          ? path.join(__dirname, 'boilerplate', 'datamodel-mongo.prisma')
          : path.join(__dirname, 'boilerplate', 'datamodel.prisma')
      fs.writeFileSync(
        path.join(this.config.definitionDir, 'datamodel.prisma'),
        fs.readFileSync(datamodelBoilerplatePath),
      )

      /**
       * Write prisma.yml
       */
      const prismaYml = `\
endpoint: ${endpoint}
datamodel: datamodel.prisma
`
      fs.writeFileSync(
        path.join(this.config.definitionDir, 'prisma.yml'),
        prismaYml,
      )

      const endpointSteps: string[] = []

      const endpointDir = this.args!.dirName
      if (endpointDir) {
        endpointSteps.push(`Open folder: ${chalk.cyan(`cd ${endpointDir}`)}`)
      }

      endpointSteps.push(
        `Deploy your Prisma service: ${chalk.cyan('prisma deploy')}`,
      )

      const endpointCreatedFiles = [
        `  ${chalk.cyan('prisma.yml')}           Prisma service definition`,
        `  ${chalk.cyan(
          'datamodel.prisma',
        )}    GraphQL SDL-based datamodel (foundation for database)`,
      ]

      this.out.log(`
${chalk.bold(
        `Created ${
          endpointCreatedFiles.length
        } new files:                                                                          `,
      )}

${endpointCreatedFiles.join('\n')}

${chalk.bold('Next steps:')}

${endpointSteps.map((step, index) => `  ${index + 1}. ${step}`).join('\n')}`)

      this.out.exit(0)
    }

    await this.env.fetchClusters()

    const endpointDialog = new EndpointDialog({
      out: this.out,
      client: this.client,
      env: this.env,
      config: this.config,
      definition: this.definition,
      shouldAskForGenerator: true,
      prototype: this.flags.prototype,
    })

    const results = await endpointDialog.getEndpoint()

    const databaseTypeString =
      results.database && results.database.type === 'mongo'
        ? '\ndatabaseType: document'
        : ''
    let prismaYmlString = `endpoint: ${results.endpoint}
datamodel: datamodel.prisma${databaseTypeString}`

    if (results.generator && results.generator !== 'no-generation') {
      prismaYmlString += this.getGeneratorConfig(results.generator)
    }

    fs.mkdirpSync(this.config.definitionDir)
    fs.writeFileSync(
      path.join(this.config.definitionDir, 'prisma.yml'),
      prismaYmlString,
    )
    fs.writeFileSync(
      path.join(this.config.definitionDir, `datamodel.prisma`),
      results.datamodel,
    )
    if (results.cluster!.local && results.writeDockerComposeYml) {
      fs.writeFileSync(
        path.join(this.config.definitionDir, 'docker-compose.yml'),
        results.dockerComposeYml,
      )
    }
    if (results.managementSecret) {
      fs.writeFileSync(
        path.join(this.config.definitionDir, '.env'),
        `PRISMA_MANAGEMENT_API_SECRET=${results.managementSecret}`,
      )
    }

    const dir = this.args!.dirName

    const isLocal = results.cluster!.local && results.writeDockerComposeYml

    const steps: string[] = []

    if (dir) {
      steps.push(`Open folder: ${chalk.cyan(`cd ${dir}`)}`)
    }

    if (results.cluster!.local && results.writeDockerComposeYml) {
      steps.push(
        `Start your Prisma server: ${chalk.cyan('docker-compose up -d')}`,
      )
    }

    steps.push(`Deploy your Prisma service: ${chalk.cyan('prisma deploy')}`)

    if (results.database && results.database.alreadyData) {
      steps.push(
        `Read more about introspection:\n     http://bit.ly/prisma-introspection`,
      )
    } else if (
      (results.database && !results.database.alreadyData) ||
      results.newDatabase
    ) {
      steps.push(
        `Read more about Prisma server:\n     http://bit.ly/prisma-server-overview`,
      )
    } else {
      steps.push(
        `Read more about deploying services:\n     http://bit.ly/prisma-deploy-services`,
      )
    }

    const createdFiles = [
      `  ${chalk.cyan('prisma.yml')}           Prisma service definition`,
      `  ${chalk.cyan(
        'datamodel.prisma',
      )}    GraphQL SDL-based datamodel (foundation for database)`,
    ]

    if (isLocal) {
      createdFiles.push(
        `  ${chalk.cyan('docker-compose.yml')}   Docker configuration file`,
      )
    }

    if (results.managementSecret) {
      createdFiles.push(
        `  ${chalk.cyan(
          '.env',
        )}                 Env file including PRISMA_API_MANAGEMENT_SECRET`,
      )
    }

    this.out.log(`
${chalk.bold(
      `Created ${
        createdFiles.length
      } new files:                                                                          `,
    )}

${createdFiles.join('\n')}

${chalk.bold('Next steps:')}

${steps.map((step, index) => `  ${index + 1}. ${step}`).join('\n')}`)

    if (results.generator && results.generator !== 'no-generation') {
      try {
        process.chdir(this.config.definitionDir)
      } catch (err) {
        this.out.log(chalk.red(err))
      }
      const isPackaged = fs.existsSync('/snapshot')
      const spawnPath = isPackaged ? nativeSpawnSync : spawnSync
      const child = spawnPath('prisma', ['generate'])
      const stderr = child.stderr && child.stderr.toString()
      if (stderr && stderr.length > 0) {
        this.out.log(chalk.red(stderr))
      }
      const stdout = child.stdout && child.stdout.toString()
      if (stdout && stdout.length > 0) {
        this.out.log(chalk.white(stdout))
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
      prismaYmlString += this.getGeneratorConfig(results.generator)
    }

    if (isLocal) {
      const dockerComposeInstalled = await isDockerComposeInstalled()
      if (!dockerComposeInstalled) {
        this.out.log(
          `\nTo install docker-compose, please follow this link: ${chalk.cyan(
            'https://docs.docker.com/compose/install/',
          )}`,
        )
      }
    }
  }

  getGeneratorConfig(generator: string) {
    return `\n\ngenerate:
  - generator: ${generator}
    output: ./generated/prisma-client/`
  }
}
