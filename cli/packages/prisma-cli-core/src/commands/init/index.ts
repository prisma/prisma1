import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import { EndpointDialog } from '../../utils/EndpointDialog'
import { isDockerComposeInstalled } from '../../utils/dockerComposeInstalled'

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
  }

  async run() {
    const dirName = this.args!.dirName
    if (dirName) {
      const newDefinitionDir = path.join(this.config.cwd, dirName + '/')
      this.config.definitionDir = newDefinitionDir
      fs.mkdirpSync(newDefinitionDir)
    } else {
      this.config.definitionDir = this.config.cwd
    }

    const endpoint = this.flags!.endpoint
    await this.runInit({ endpoint })
  }

  async runInit({ endpoint }) {
    const files = fs.readdirSync(this.config.definitionDir)
    // the .prismarc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
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

    if (endpoint) {
      // TODO: Revert the codepath for pkg bundling after this is resolved https://github.com/zeit/pkg/issues/420
      const isPackaged = fs.existsSync('/snapshot')
      if (isPackaged) {
        fs.writeFileSync(
          path.join(this.config.definitionDir, 'datamodel.prisma'), 
          fs.readFileSync(path.join(__dirname, 'boilerplate', 'datamodel.prisma'))
        )
        fs.writeFileSync(
          path.join(this.config.definitionDir, 'prisma.yml'), 
          fs.readFileSync(path.join(__dirname, 'boilerplate', 'prisma.yml'))
        )
      } else {
        fs.copySync(
          path.join(__dirname, 'boilerplate', 'datamodel.prisma'),
          path.join(this.config.definitionDir, 'datamodel.prisma'),
        )
        fs.copySync(
          path.join(__dirname, 'boilerplate', 'prisma.yml'),
          path.join(this.config.definitionDir, 'prisma.yml'),
        )
      }

      const endpointDefinitionPath = path.join(
        this.config.definitionDir,
        'prisma.yml',
      )
      const endpointPrismaYml = fs.readFileSync(endpointDefinitionPath, 'utf-8')

      const newEndpointPrismaYml = endpointPrismaYml.replace(
        'ENDPOINT',
        endpoint,
      )
      fs.writeFileSync(endpointDefinitionPath, newEndpointPrismaYml)

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

    const endpointDialog = new EndpointDialog({
      out: this.out,
      client: this.client,
      env: this.env,
      config: this.config,
      definition: this.definition,
      shouldAskForGenerator: true,
    })

    const results = await endpointDialog.getEndpoint()

    let prismaYmlString = `endpoint: ENDPOINT
datamodel: datamodel.prisma`

    if (results.generator && results.generator !== 'no-generation') {
      prismaYmlString += this.getGeneratorConfig(results.generator)
    }

    fs.writeFileSync(
      path.join(this.config.definitionDir, 'prisma.yml'),
      prismaYmlString,
    )
    fs.writeFileSync(
      path.join(this.config.definitionDir, 'datamodel.prisma'),
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

    const definitionPath = path.join(this.config.definitionDir, 'prisma.yml')
    const prismaYml = fs.readFileSync(definitionPath, 'utf-8')

    const newPrismaYml = prismaYml.replace('ENDPOINT', results.endpoint)
    fs.writeFileSync(definitionPath, newPrismaYml)

    const dir = this.args!.dirName
    const dirString = dir
      ? `Open the new folder via ${chalk.cyan(`$ cd ${dir}`)}.\n`
      : ``

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

    const dockerComposeInstalled = await isDockerComposeInstalled()
    if (!dockerComposeInstalled) {
      this.out.log(
        `\nTo install docker-compose, please follow this link: ${chalk.cyan(
          'https://docs.docker.com/compose/install/',
        )}`,
      )
    }
  }
  getGeneratorConfig(generator: string) {
    return `\n\ngenerate:
  - generator: ${generator}
    output: ./generated/prisma

hooks:
  post-deploy:
    - prisma generate`
  }
}
