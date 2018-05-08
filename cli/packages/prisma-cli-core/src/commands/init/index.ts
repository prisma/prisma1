import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import * as npmRun from 'npm-run'
const debug = require('debug')('init')
import * as spawn from 'cross-spawn'
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
      const newDefinitionDir = path.join(this.config.cwd, dirName + '/')
      this.config.definitionDir = newDefinitionDir
      fs.mkdirpSync(newDefinitionDir)
    } else {
      this.config.definitionDir = this.config.cwd
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

    fs.copySync(
      path.join(__dirname, 'boilerplate', 'prisma.yml'),
      path.join(this.config.definitionDir, 'prisma.yml'),
    )
    fs.writeFileSync(
      path.join(this.config.definitionDir, 'datamodel.graphql'),
      results.datamodel,
    )
    if (results.cluster!.local && results.writeDockerComposeYml) {
      fs.writeFileSync(
        path.join(this.config.definitionDir, 'docker-compose.yml'),
        results.dockerComposeYml,
      )
    }
    let relativeDir = path.relative(this.config.cwd, this.config.definitionDir)
    relativeDir = relativeDir.length === 0 ? '.' : relativeDir

    const definitionPath = path.join(this.config.definitionDir, 'prisma.yml')
    const prismaYml = fs.readFileSync(definitionPath, 'utf-8')

    const newPrismaYml = prismaYml.replace('ENDPOINT', results.endpoint)
    fs.writeFileSync(definitionPath, newPrismaYml)

    const dir = this.args!.dirName
    const dirString = dir
      ? `Open the new folder via ${chalk.cyan(`$ cd ${dir}`)}.\n`
      : ``

    const isLocal = results.cluster!.local && results.writeDockerComposeYml
    const dbType = results.database ? results.database.type : ''
    const beautifulDbTypesMap = {
      mysql: 'MySQL',
      postgres: 'PostgreSQL',
    }
    const beautifulDbType = beautifulDbTypesMap[dbType] || ''

    const steps: string[] = []

    if (dir) {
      steps.push(`Open folder: ${chalk.cyan('cd example')}`)
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

    this.out.log(`
${chalk.bold(
      `Created ${
        results.cluster!.local ? 3 : 2
      } new files:                                                                          `,
    )}

  ${chalk.cyan('prisma.yml')}           Prisma service definition
  ${chalk.cyan(
    'datamodel.graphql',
  )}    GraphQL SDL-based datamodel (foundation for database)
  ${
    isLocal
      ? `${chalk.cyan('docker-compose.yml')}   Docker configuration file`
      : ''
  }

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
}
