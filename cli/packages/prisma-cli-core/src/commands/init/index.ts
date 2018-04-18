import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import * as npmRun from 'npm-run'
const debug = require('debug')('init')
import * as spawn from 'cross-spawn'
import getGraphQLCliBin from '../../utils/getGraphQLCliBin'
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
    boilerplate: flags.string({
      char: 'b',
      description:
        'Full URL or repo shorthand (e.g. `owner/repo`) to boilerplate GitHub repository',
    }),
  }

  async run() {
    const dirName = this.args!.dirName
    if (dirName) {
      const newDefinitionDir = path.join(process.cwd(), dirName + '/')
      this.config.definitionDir = newDefinitionDir
      fs.mkdirpSync(newDefinitionDir)
    } else {
      this.config.definitionDir = process.cwd()
    }

    await this.runInit()
  }

  async runInit() {
    const files = fs.readdirSync(this.config.definitionDir)
    // the .prismarc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (
      files.length > 0 &&
      (files.includes('prisma.yml') ||
        files.includes('datamodel.graphql') ||
        files.includes('docker-compose.yml'))
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

    const endpointDialog = new EndpointDialog(
      this.out,
      this.client,
      this.env,
      this.config,
    )
    const results = await endpointDialog.getEndpoint()
    this.out.up(3)

    fs.copySync(
      path.join(__dirname, 'boilerplate', 'prisma.yml'),
      path.join(this.config.definitionDir, 'prisma.yml'),
    )
    fs.copySync(
      path.join(__dirname, 'boilerplate', 'datamodel.graphql'),
      path.join(this.config.definitionDir, 'datamodel.graphql'),
    )
    if (results.cluster!.local) {
      fs.copySync(
        path.join(__dirname, 'boilerplate', 'docker-compose.yml'),
        path.join(this.config.definitionDir, 'docker-compose.yml'),
      )
    }
    let relativeDir = path.relative(process.cwd(), this.config.definitionDir)
    relativeDir = relativeDir.length === 0 ? '.' : relativeDir

    const definitionPath = path.join(this.config.definitionDir, 'prisma.yml')
    const prismaYml = fs.readFileSync(definitionPath, 'utf-8')

    const newPrismaYml = prismaYml.replace('ENDPOINT', results.endpoint)
    fs.writeFileSync(definitionPath, newPrismaYml)

    const dir = this.args!.dirName
    const dirString = dir
      ? `Open the new folder via ${chalk.cyan(`$ cd ${dir}`)}. `
      : ``

    const deployString = results.cluster!.local
      ? `Run ${chalk.cyan('docker-compose up -d')}. Then you `
      : `You now `

    this.out.log(`\
Created ${
      results.cluster!.local ? 3 : 2
    } new files:                                                                          

  ${chalk.cyan('prisma.yml')}           Prisma service definition
  ${chalk.cyan(
    'datamodel.graphql',
  )}    GraphQL SDL-based datamodel (foundation for database)
  ${
    results.cluster!.local
      ? `${chalk.cyan('docker-compose.yml')}   Docker configuration file`
      : ''
  }

${dirString}${deployString}can run ${chalk.cyan(
      '$ prisma deploy',
    )} to deploy your database service.`)
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
